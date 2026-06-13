package kexpresso

/**
 * "Reverse" feature: read an existing raw regex string and turn it back into kexpresso.
 *
 * Two capabilities live here:
 * 1. [Kexpresso.from] — compile a raw regex (matching is **always exact**) and best-effort
 *    parse it into the internal [RegexNode] AST to power [KexpressoPattern.describe] and
 *    [KexpressoPattern.toKexpressoCode].
 * 2. [KexpressoPattern.toKexpressoCode] — emit compilable kexpresso DSL Kotlin source for ANY
 *    pattern (builder-made or `from`-parsed), enabling round-tripping.
 *
 * The parser is a small recursive-descent parser. It is intentionally **not** a complete PCRE
 * parser: it models the common constructs and honestly degrades anything it cannot model to a
 * [Raw] node, so the generated DSL stays compilable and the description stays meaningful.
 */

/**
 * Parses [regex] into a [KexpressoPattern] whose matching behaviour is **exactly** that of
 * `Regex(regex)`, and whose internal AST is a best-effort structural reconstruction.
 *
 * Matching correctness is guaranteed: the input is compiled verbatim and the original [regex]
 * string is used as the pattern's [KexpressoPattern.source], so
 * `Kexpresso.from(r).matches(x) == Regex(r).matches(x)` for every `r` and `x`. The AST is used
 * only for [KexpressoPattern.describe] and [KexpressoPattern.toKexpressoCode]; anything the
 * parser does not model falls back to a [Raw] node (which still renders and describes honestly).
 *
 * Example:
 * ```kotlin
 * Kexpresso.from("\\d{4}-\\d{2}-\\d{2}").describe()
 * // "exactly 4 of (a digit), the literal "-", exactly 2 of (a digit), the literal "-", …"
 * ```
 *
 * @param regex the raw regex string to read.
 * @return a [KexpressoPattern] with exact matching and a best-effort AST.
 * @throws IllegalArgumentException if [regex] is not a valid regular expression (on the JVM this
 *   is the more specific `java.util.regex.PatternSyntaxException`, which is a subtype).
 */
@ExperimentalKexpressoApi
fun Kexpresso.from(regex: String): KexpressoPattern {
    // Compile verbatim — this is the source of truth for matching and may throw on invalid input.
    val compiled = Regex(regex)
    // Best-effort structural parse purely for describe()/toKexpressoCode().
    val ast = RegexParser(regex).parse()
    // Keep the ORIGINAL string as `source` so rendering never alters match behaviour.
    return KexpressoPattern(regex, compiled, ast)
}

/**
 * Emits compilable kexpresso DSL Kotlin source for this pattern, derived from its internal AST.
 *
 * Works for any [KexpressoPattern] — built via the DSL or produced by [Kexpresso.from] — which
 * makes round-tripping possible. The output uses 4-space indentation, with nested blocks
 * indented one level deeper. Tokens the generator recognises map to their friendly DSL call
 * (e.g. `\d` → `digit()`); everything else degrades to `raw("…")`.
 *
 * Example:
 * ```kotlin
 * Kexpresso.from("\\d{4}-\\d{2}").toKexpressoCode()
 * // kexpresso {
 * //     exactly(4) { digit() }
 * //     literal("-")
 * //     exactly(2) { digit() }
 * // }
 * ```
 *
 * @return the generated kexpresso DSL source.
 */
@ExperimentalKexpressoApi
fun KexpressoPattern.toKexpressoCode(): String {
    val bodyLines = KexpressoCodeGenerator.render(ast)
    val indented = bodyLines.joinToString("") { "${KexpressoCodeGenerator.INDENT_UNIT}$it\n" }
    return "kexpresso {\n$indented}"
}

// ── Code generation ─────────────────────────────────────────────────────────────

/**
 * Walks a [RegexNode] AST and renders it to kexpresso DSL source lines (without leading indent;
 * the caller indents them). Each construct maps to its DSL equivalent.
 *
 * A block-bearing construct (quantifier, group, lookaround) whose body is exactly one line is
 * rendered compactly as `header { thatLine }`; otherwise the body is expanded across lines and
 * indented one level deeper. A [SequenceNode] flattens to one line per child. Tokens map via a
 * fixed lookup table and fall back to `raw("…")` when unknown.
 */
internal object KexpressoCodeGenerator {

    const val INDENT_UNIT: String = "    "

    /**
     * Lookup table mapping a fixed token regex fragment to the DSL call that produces it.
     * Anything absent from this table is emitted as `raw("<regex>")`.
     */
    private val TOKEN_CALLS: Map<String, String> = mapOf(
        "\\d" to "digit()",
        "\\D" to "nonDigit()",
        "\\s" to "whitespace()",
        "\\S" to "nonWhitespace()",
        "\\w" to "wordChar()",
        "\\W" to "nonWordChar()",
        "." to "anyChar()",
        "[a-zA-Z]" to "letter()",
        "[A-Z]" to "uppercaseLetter()",
        "[a-z]" to "lowercaseLetter()",
        "[a-zA-Z0-9]" to "alphanumeric()",
        "[.!?]" to "endPunctuation()",
        "\\A" to "startOfText()",
        "\\z" to "endOfText()",
        "^" to "startOfLine()",
        "$" to "endOfLine()",
        "\\b" to "wordBoundary()",
        "\\B" to "nonWordBoundary()",
        "\\t" to "tab()",
        "\\n" to "newline()",
        "\\r" to "carriageReturn()",
    )

    /** Renders [node] to a list of DSL source lines (no leading indentation). */
    fun render(node: RegexNode): List<String> = when (node) {
        is SequenceNode -> node.children.flatMap { render(it) }
        is Token -> listOf(tokenCall(node.regex))
        is Literal -> listOf("literal(\"${escapeForKotlin(node.text)}\")")
        is Raw -> listOf("raw(\"${escapeForKotlin(node.regex)}\")")
        is Quantifier -> block(quantifierHeader(node), node.child)
        is Group -> block(groupHeader(node.kind), node.child)
        is Lookaround -> block(lookaroundHeader(node.kind), node.child)
        is Alternation -> renderAlternation(node)
        is Backreference -> listOf(backreferenceCall(node))
    }

    /** Maps a token's fixed regex fragment to its DSL call, or `raw(...)` when unknown. */
    private fun tokenCall(regex: String): String =
        TOKEN_CALLS[regex] ?: "raw(\"${escapeForKotlin(regex)}\")"

    private fun quantifierHeader(node: Quantifier): String {
        // `greedy = false` is the first arg wherever the DSL supports it (everything but exactly).
        val lazyArg = if (!node.greedy && node.kind !is QuantifierKind.Exactly) "greedy = false" else ""
        return when (val kind = node.kind) {
            is QuantifierKind.Optional -> callWith("optional", lazyArg)
            is QuantifierKind.ZeroOrMore -> callWith("zeroOrMore", lazyArg)
            is QuantifierKind.OneOrMore -> callWith("oneOrMore", lazyArg)
            is QuantifierKind.Exactly -> callWith("exactly", "${kind.n}")
            is QuantifierKind.AtLeast -> callWith("atLeast", "${kind.n}", lazyArg)
            is QuantifierKind.Between -> callWith("between", "${kind.min}", "${kind.max}", lazyArg)
        }
    }

    private fun groupHeader(kind: GroupKind): String = when (kind) {
        is GroupKind.NonCapturing -> "group"
        is GroupKind.Capturing -> "capture"
        is GroupKind.Named -> "capture(\"${kind.name}\")"
    }

    private fun lookaroundHeader(kind: LookaroundKind): String = when (kind) {
        is LookaroundKind.FollowedBy -> "followedBy"
        is LookaroundKind.NotFollowedBy -> "notFollowedBy"
        is LookaroundKind.PrecededBy -> "precededBy"
        is LookaroundKind.NotPrecededBy -> "notPrecededBy"
    }

    /**
     * Renders `header { … }`. When the child body is a single line, the block is collapsed onto
     * one line (`header { line }`); otherwise the body is expanded and indented one level deeper.
     */
    private fun block(header: String, child: RegexNode): List<String> {
        val body = render(child)
        return if (body.size == 1) {
            listOf("$header { ${body[0]} }")
        } else {
            listOf("$header {") + body.map { "$INDENT_UNIT$it" } + "}"
        }
    }

    /**
     * Renders an alternation, always expanded:
     * ```
     * oneOf(
     *     { … },
     *     { … },
     * )
     * ```
     */
    private fun renderAlternation(node: Alternation): List<String> {
        val lines = mutableListOf("oneOf(")
        for (branch in node.branches) {
            lines += "$INDENT_UNIT{"
            render(branch).forEach { lines += "$INDENT_UNIT$INDENT_UNIT$it" }
            lines += "$INDENT_UNIT},"
        }
        lines += ")"
        return lines
    }

    private fun backreferenceCall(node: Backreference): String {
        // Numeric refs render as "1".."9"; named refs render as "k<name>".
        val ref = node.reference
        return if (ref.all { it.isDigit() }) {
            "backreference($ref)"
        } else {
            val name = ref.removePrefix("k<").removeSuffix(">")
            "backreference(\"$name\")"
        }
    }

    /**
     * Joins a DSL call [name] with its non-empty arguments, e.g. `between(1, 5, greedy = false)`.
     * When there are no arguments the bare name is returned (so block headers read `oneOrMore`,
     * not `oneOrMore()`).
     */
    private fun callWith(name: String, vararg args: String): String {
        val nonEmpty = args.filter { it.isNotEmpty() }
        return if (nonEmpty.isEmpty()) name else "$name(${nonEmpty.joinToString(", ")})"
    }

    /** Escapes `\` and `"` so [text] is a valid Kotlin string-literal body. */
    private fun escapeForKotlin(text: String): String =
        text.replace("\\", "\\\\").replace("\"", "\\\"")
}

// ── Parser ──────────────────────────────────────────────────────────────────────

/**
 * Recursive-descent parser turning a raw regex string into a best-effort [RegexNode] AST.
 *
 * Grammar (informally):
 * ```
 * alternation := sequence ('|' sequence)*
 * sequence    := quantified*
 * quantified  := atom quantifier?
 * atom        := group | charClass | escape | anchor | literalChar
 * ```
 *
 * The parser never throws on a valid regex: any fragment it cannot model cleanly degrades to a
 * [Raw] node holding the exact source text, preserving an honest description and compilable
 * generated code. It does not validate the regex — [Kexpresso.from] already compiled it.
 */
internal class RegexParser(private val src: String) {

    private var pos = 0

    /** Parses the whole input into a single [RegexNode]. */
    fun parse(): RegexNode = parseAlternation()

    // ── alternation ───────────────────────────────────────────────────────────

    /** Parses `a|b|c`. A single branch returns that branch directly (no [Alternation] wrapper). */
    private fun parseAlternation(): RegexNode {
        val branches = mutableListOf(parseSequence())
        while (peek() == '|') {
            pos++ // consume '|'
            branches += parseSequence()
        }
        return if (branches.size == 1) branches[0] else Alternation(branches)
    }

    // ── sequence ──────────────────────────────────────────────────────────────

    /** Parses a run of quantified atoms until end-of-input, `|`, or a closing `)`. */
    private fun parseSequence(): RegexNode {
        val nodes = mutableListOf<RegexNode>()
        while (pos < src.length && peek() != '|' && peek() != ')') {
            nodes += parseQuantified()
        }
        return mergeLiterals(nodes).let { merged ->
            if (merged.size == 1) merged[0] else SequenceNode(merged)
        }
    }

    /** Merges consecutive [Literal] nodes into one for cleaner output (e.g. `a`,`b`,`c` → `abc`). */
    private fun mergeLiterals(nodes: List<RegexNode>): List<RegexNode> {
        val out = mutableListOf<RegexNode>()
        for (node in nodes) {
            val last = out.lastOrNull()
            if (node is Literal && last is Literal) {
                out[out.size - 1] = Literal(last.text + node.text)
            } else {
                out += node
            }
        }
        return out
    }

    // ── quantified atom ───────────────────────────────────────────────────────

    /** Parses an atom and, if present, the quantifier that applies to it. */
    private fun parseQuantified(): RegexNode {
        val atomStart = pos
        val atom = parseAtom()
        return applyQuantifier(atom, atomStart)
    }

    /**
     * Reads an optional quantifier following [atom] (which spans `[atomStart, pos)`).
     * A possessive form (`*+`, `++`, `?+`, `{…}+`) or any quantifier we cannot model cleanly
     * degrades the whole `atom + quantifier` span to a single [Raw] node, preserving behaviour.
     */
    private fun applyQuantifier(atom: RegexNode, atomStart: Int): RegexNode {
        val kind = readQuantifierKind() ?: return atom
        val greedy = consumeLazyOrPossessive()
        if (greedy == Greediness.POSSESSIVE) {
            return Raw(src.substring(atomStart, pos)) // possessive — emit raw span verbatim
        }
        return Quantifier(atom, kind, greedy == Greediness.GREEDY)
    }

    /** Reads `*`, `+`, `?`, `{n}`, `{n,}`, `{n,m}` and advances past it; null if none present. */
    private fun readQuantifierKind(): QuantifierKind? = when (peek()) {
        '*' -> { pos++; QuantifierKind.ZeroOrMore }
        '+' -> { pos++; QuantifierKind.OneOrMore }
        '?' -> { pos++; QuantifierKind.Optional }
        '{' -> readCurlyQuantifier()
        else -> null
    }

    /** Parses a `{…}` quantifier; returns null (leaving [pos] untouched) if it is not a valid one. */
    private fun readCurlyQuantifier(): QuantifierKind? {
        val end = src.indexOf('}', pos + 1)
        if (end < 0) return null
        val inside = src.substring(pos + 1, end)
        val kind = curlyKindOf(inside) ?: return null
        pos = end + 1 // consume through '}'
        return kind
    }

    /** Interprets the body of a `{…}` quantifier (`n`, `n,`, or `n,m`) as a [QuantifierKind]. */
    private fun curlyKindOf(inside: String): QuantifierKind? {
        val comma = inside.indexOf(',')
        return when {
            comma < 0 ->
                inside.toIntOrNull()?.let { QuantifierKind.Exactly(it) }
            comma == inside.length - 1 ->
                inside.dropLast(1).toIntOrNull()?.let { QuantifierKind.AtLeast(it) }
            else -> {
                val min = inside.substring(0, comma).toIntOrNull()
                val max = inside.substring(comma + 1).toIntOrNull()
                if (min != null && max != null) QuantifierKind.Between(min, max) else null
            }
        }
    }

    /** Greediness of a quantifier suffix. */
    private enum class Greediness { GREEDY, LAZY, POSSESSIVE }

    /** Consumes an optional `?` (lazy) or `+` (possessive) suffix and reports the greediness. */
    private fun consumeLazyOrPossessive(): Greediness = when (peek()) {
        '?' -> { pos++; Greediness.LAZY }
        '+' -> { pos++; Greediness.POSSESSIVE }
        else -> Greediness.GREEDY
    }

    // ── atoms ─────────────────────────────────────────────────────────────────

    /** Dispatches to the right atom parser based on the current character, consuming one char each. */
    private fun parseAtom(): RegexNode = when (val c = peek()) {
        '(' -> parseGroup()
        '[' -> parseCharClass()
        '\\' -> parseEscape()
        '^' -> { pos++; Token("^", "start of line") }
        '$' -> { pos++; Token("$", "end of line") }
        '.' -> { pos++; Token(".", "any character") }
        else -> { pos++; Literal(c.toString()) } // a plain literal character
    }

    // ── groups & lookarounds ──────────────────────────────────────────────────

    /**
     * Parses a `(...)` construct. Recognises capturing, non-capturing, named, and the four
     * lookarounds; any other `(?...)` exotic form (atomic `(?>`, inline flags `(?i)`, etc.) is
     * consumed with balanced-paren tracking and emitted as a single [Raw] node.
     */
    private fun parseGroup(): RegexNode {
        val openStart = pos
        return when (val opener = classifyGroupOpener()) {
            is GroupOpener.Modeled -> finishModeledGroup(opener)
            is GroupOpener.Exotic -> Raw(consumeBalancedGroup(openStart))
        }
    }

    /** A modeled group wraps its parsed body; an exotic one is emitted raw. */
    private sealed interface GroupOpener {
        /** A group/lookaround we model; [wrap] turns the parsed body into the final node. */
        data class Modeled(val wrap: (RegexNode) -> RegexNode) : GroupOpener
        /** A group form we do not model and will emit verbatim. */
        object Exotic : GroupOpener
    }

    /**
     * Inspects the group opener at [pos] and advances past it, returning how to wrap the body.
     * Leaves [pos] at the first character of the group body for modeled groups; for exotic
     * groups it does not advance (the caller re-reads the whole group verbatim).
     */
    private fun classifyGroupOpener(): GroupOpener {
        if (!src.startsWith("(?", pos)) {
            pos++ // consume '(' — plain capturing group
            return GroupOpener.Modeled { Group(it, GroupKind.Capturing) }
        }
        return classifyExtendedOpener()
    }

    /** Classifies the `(?…` openers: `(?:`, lookarounds, `(?<name>`, or exotic. */
    private fun classifyExtendedOpener(): GroupOpener {
        val lookaround = lookaroundKindAt()
        if (lookaround != null) return lookaround
        return when {
            src.startsWith("(?:", pos) -> { pos += 3; GroupOpener.Modeled { Group(it, GroupKind.NonCapturing) } }
            src.startsWith("(?<", pos) -> classifyNamedGroup()
            else -> GroupOpener.Exotic
        }
    }

    /** Recognises the four lookaround openers, or null if the opener is not a lookaround. */
    private fun lookaroundKindAt(): GroupOpener.Modeled? {
        val kind = when {
            src.startsWith("(?=", pos) -> LookaroundKind.FollowedBy
            src.startsWith("(?!", pos) -> LookaroundKind.NotFollowedBy
            src.startsWith("(?<=", pos) -> LookaroundKind.PrecededBy
            src.startsWith("(?<!", pos) -> LookaroundKind.NotPrecededBy
            else -> return null
        }
        pos += if (kind == LookaroundKind.PrecededBy || kind == LookaroundKind.NotPrecededBy) 4 else 3
        return GroupOpener.Modeled { Lookaround(it, kind) }
    }

    /** Distinguishes a named group `(?<name>…)` from lookbehind (already handled) / exotic. */
    private fun classifyNamedGroup(): GroupOpener {
        val gt = src.indexOf('>', pos + 3)
        if (gt < 0) return GroupOpener.Exotic
        val name = src.substring(pos + 3, gt)
        // The JVM engine only allows letters/digits in names; anything else is exotic to us.
        if (name.isEmpty() || !name.all { it.isLetterOrDigit() }) return GroupOpener.Exotic
        pos = gt + 1
        return GroupOpener.Modeled { Group(it, GroupKind.Named(name)) }
    }

    /** Parses the body of a modeled group up to its `)` and applies the wrapper. */
    private fun finishModeledGroup(opener: GroupOpener.Modeled): RegexNode {
        val body = parseAlternation()
        if (peek() == ')') pos++ // consume ')'
        return opener.wrap(body)
    }

    /**
     * Consumes a whole group starting at [start] (`(`) using balanced-paren tracking that respects
     * escapes and character classes, and returns the verbatim substring. Used for exotic groups.
     */
    private fun consumeBalancedGroup(start: Int): String {
        var depth = 0
        var i = start
        while (i < src.length) {
            when {
                src[i] == '\\' -> i += 2
                src[i] == '[' -> i = skipCharClass(i)
                src[i] == '(' -> { depth++; i++ }
                src[i] == ')' -> { depth--; i++; if (depth == 0) break }
                else -> i++
            }
        }
        pos = minOf(i, src.length)
        return src.substring(start, pos)
    }

    // ── character classes ─────────────────────────────────────────────────────

    /**
     * Parses a `[...]` character class. A handful of well-known classes map to a friendly [Token]
     * (so codegen yields `letter()`, `uppercaseLetter()`, etc.); every other class becomes a
     * [Token] whose regex is the verbatim class — which codegen honestly renders as `raw("[…]")`.
     */
    private fun parseCharClass(): RegexNode {
        val end = skipCharClass(pos)
        val text = src.substring(pos, end)
        pos = end
        // Friendly descriptions for the few classes the DSL has dedicated helpers for.
        val description = when (text) {
            "[a-zA-Z]" -> "a letter"
            "[A-Z]" -> "an uppercase letter"
            "[a-z]" -> "a lowercase letter"
            "[a-zA-Z0-9]" -> "an alphanumeric character"
            "[.!?]" -> "sentence-ending punctuation"
            else -> "one of $text"
        }
        return Token(text, description)
    }

    /**
     * Given [i] pointing at `[`, returns the index just after the matching `]`, honouring `\]`
     * escapes and a leading `]`/`^]` being treated as a literal `]` by the regex engine.
     * Returns `src.length` if unterminated.
     */
    private fun skipCharClass(i: Int): Int {
        var j = i + 1
        if (j < src.length && src[j] == '^') j++ // negated class
        if (j < src.length && src[j] == ']') j++ // literal ']' as first member
        while (j < src.length) {
            when {
                src[j] == '\\' -> j += 2
                src[j] == ']' -> return j + 1
                else -> j++
            }
        }
        return src.length
    }

    // ── escapes ───────────────────────────────────────────────────────────────

    /**
     * Parses an escape sequence beginning at `\`. Predefined classes and anchors map to a [Token];
     * numeric (`\1`..`\9`) and named (`\k<name>`) back-references to [Backreference]; a lone
     * trailing `\` to [Raw]; and any other escaped metacharacter `\X` to a [Literal] of `X`.
     */
    private fun parseEscape(): RegexNode {
        // A lone trailing '\' cannot start an escape — emit it verbatim.
        if (pos + 1 >= src.length) {
            val raw = src.substring(pos); pos = src.length; return Raw(raw)
        }
        val c = src[pos + 1]
        return predefinedOrBackreference(c) ?: run {
            pos += 2 // any other \X is a literal X (e.g. \., \+, \()
            Literal(c.toString())
        }
    }

    /** Resolves `\d`-style tokens and numeric/named back-references; null for a plain `\X` literal. */
    private fun predefinedOrBackreference(c: Char): RegexNode? {
        PREDEFINED_ESCAPES["\\$c"]?.let { pos += 2; return it }
        if (c in '1'..'9') { pos += 2; return Backreference("$c", "group $c") }
        return namedBackreference(c)
    }

    /** Parses `\k<name>` as a named [Backreference]; null if the syntax does not match. */
    private fun namedBackreference(c: Char): RegexNode? {
        if (c != 'k' || !src.startsWith("\\k<", pos)) return null
        val gt = src.indexOf('>', pos + 3)
        if (gt < 0) return null
        val name = src.substring(pos + 3, gt)
        pos = gt + 1
        return Backreference("k<$name>", "group \"$name\"")
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Current character, or a space sentinel at end-of-input. The sentinel is safe because
     * callers only compare peek() against metacharacters, and every loop is additionally
     * bounded by `pos < src.length`.
     */
    private fun peek(): Char = if (pos < src.length) src[pos] else ' '

    private companion object {
        /** Fixed escape/anchor fragments and the [Token]s they map to. */
        val PREDEFINED_ESCAPES: Map<String, Token> = mapOf(
            "\\d" to Token("\\d", "a digit"),
            "\\D" to Token("\\D", "a non-digit"),
            "\\s" to Token("\\s", "whitespace"),
            "\\S" to Token("\\S", "a non-whitespace character"),
            "\\w" to Token("\\w", "a word character"),
            "\\W" to Token("\\W", "a non-word character"),
            "\\b" to Token("\\b", "a word boundary"),
            "\\B" to Token("\\B", "a non-word boundary"),
            "\\A" to Token("\\A", "start of text"),
            "\\z" to Token("\\z", "end of text"),
            "\\t" to Token("\\t", "a tab"),
            "\\n" to Token("\\n", "a newline"),
            "\\r" to Token("\\r", "a carriage return"),
        )
    }
}
