package kexpresso

/**
 * Top-level entry point for building a [KexpressoPattern].
 *
 * Example:
 * ```kotlin
 * val pattern = kexpresso { startOfText(); capitalLetter(); oneOrMore { letter() }; endOfText() }
 * pattern.matches("Espresso") // true
 * ```
 *
 * @param options optional [RegexOption]s to apply to the compiled regex.
 * @param block builder block that assembles the pattern.
 * @return a compiled [KexpressoPattern].
 */
fun kexpresso(vararg options: RegexOption, block: KexpressoBuilder.() -> Unit): KexpressoPattern {
    val builder = KexpressoBuilder()
    builder.block()
    return builder.build(*options)
}

/**
 * Object-oriented entry point for building a [KexpressoPattern].
 *
 * Example:
 * ```kotlin
 * val pattern = Kexpresso.pattern { literal("Latte") }
 * pattern.matches("Latte") // true
 * ```
 */
object Kexpresso {
    /**
     * Builds a [KexpressoPattern] using the supplied [block].
     *
     * @param options optional [RegexOption]s applied to the compiled regex.
     * @param block builder block that assembles the pattern.
     * @return a compiled [KexpressoPattern].
     */
    fun pattern(vararg options: RegexOption, block: KexpressoBuilder.() -> Unit): KexpressoPattern {
        val builder = KexpressoBuilder()
        builder.block()
        return builder.build(*options)
    }
}

/**
 * Immutable compiled regex pattern produced by [kexpresso] or [Kexpresso.pattern].
 *
 * Example:
 * ```kotlin
 * val p = kexpresso { digit(); oneOrMore { letter() } }
 * p.matches("3Espresso")                  // true  (digit followed by letters, full match)
 * p.matches("Espresso")                   // false (no leading digit)
 * p.containsMatchIn("order 3Cappuccino now") // true  (the pattern occurs inside the text)
 * ```
 *
 * @property source the raw regex string.
 * @property regex the compiled [Regex].
 */
class KexpressoPattern internal constructor(
    val source: String,
    val regex: Regex,
    internal val ast: RegexNode,
) {
    /**
     * Public constructor that builds a pattern from a pre-rendered [source] and compiled [regex].
     *
     * The internal AST representation falls back to a single [Raw] node wrapping [source], so the
     * resulting pattern still renders and describes itself consistently.
     *
     * @param source the raw regex string.
     * @param regex the compiled [Regex].
     */
    constructor(source: String, regex: Regex) : this(source, regex, Raw(source))

    /** The set of [RegexOption]s used when compiling this pattern. */
    val options: Set<RegexOption> get() = regex.options

    /**
     * Returns a readable English description of this pattern, derived from its internal AST.
     *
     * The description is a deterministic, comma-joined phrase. It is meant for humans reading or
     * debugging a pattern, not for machine round-tripping.
     *
     * Example:
     * ```kotlin
     * kexpresso { startOfText(); oneOrMore { digit() }; endOfText() }.describe()
     * // "start of text, one or more of (a digit), end of text"
     * ```
     *
     * @return the English description.
     */
    fun describe(): String = ast.describe()

    /**
     * Returns true if the entire [input] matches this pattern.
     *
     * @param input the character sequence to test.
     */
    fun matches(input: CharSequence): Boolean = regex.matches(input)

    /**
     * Returns true if the pattern appears anywhere in [input].
     *
     * @param input the character sequence to search.
     */
    fun containsMatchIn(input: CharSequence): Boolean = regex.containsMatchIn(input)

    /**
     * Returns the first match of this pattern in [input], or null if none.
     *
     * @param input the character sequence to search.
     * @param startIndex the index to start searching from.
     */
    fun find(input: CharSequence, startIndex: Int = 0): MatchResult? = regex.find(input, startIndex)

    /**
     * Returns a sequence of all non-overlapping matches of this pattern in [input].
     *
     * @param input the character sequence to search.
     */
    fun findAll(input: CharSequence): Sequence<MatchResult> = regex.findAll(input)

    /**
     * Returns the underlying [Regex].
     */
    fun toRegex(): Regex = regex

    /**
     * Replaces the first occurrence of this pattern in [input] with [replacement].
     *
     * Group references in [replacement] use the `$1`, `$2`, `${name}` syntax
     * as supported by [Regex.replaceFirst].
     *
     * Example:
     * ```kotlin
     * val roast = kexpresso { oneOrMore { letter() } }
     * roast.replaceFirst("Espresso Latte", "Americano") // "Americano Latte"
     * ```
     *
     * @param input the character sequence to search.
     * @param replacement the replacement string.
     * @return the resulting string with the first match replaced.
     */
    fun replaceFirst(input: CharSequence, replacement: String): String =
        regex.replaceFirst(input.toString(), replacement)

    /**
     * Replaces all occurrences of this pattern in [input] with [replacement].
     *
     * Group references in [replacement] use the `$1`, `$2`, `${name}` syntax
     * as supported by [Regex.replace].
     *
     * Example:
     * ```kotlin
     * val roast = kexpresso { oneOrMore { letter() } }
     * roast.replaceAll("Espresso Latte", "Brew") // "Brew Brew"
     * ```
     *
     * @param input the character sequence to search.
     * @param replacement the replacement string.
     * @return the resulting string with all matches replaced.
     */
    fun replaceAll(input: CharSequence, replacement: String): String =
        regex.replace(input.toString(), replacement)

    /**
     * Replaces all occurrences of this pattern in [input] using the [transform] function.
     *
     * [transform] is called for each [MatchResult] and its return value is used as the
     * replacement text for that match.
     *
     * Example:
     * ```kotlin
     * val drink = kexpresso { oneOrMore { letter() } }
     * drink.replaceAll("espresso latte") { it.value.uppercase() } // "ESPRESSO LATTE"
     * ```
     *
     * @param input the character sequence to search.
     * @param transform function that maps each [MatchResult] to its replacement text.
     * @return the resulting string with all matches replaced by the transform output.
     */
    fun replaceAll(input: CharSequence, transform: (MatchResult) -> CharSequence): String =
        regex.replace(input, transform)

    /**
     * Splits [input] around matches of this pattern and returns the resulting list of strings.
     *
     * Example:
     * ```kotlin
     * val separator = kexpresso { literal(", ") }
     * separator.split("Espresso, Latte, Cappuccino") // ["Espresso", "Latte", "Cappuccino"]
     * ```
     *
     * @param input the character sequence to split.
     * @param limit the maximum number of resulting substrings. `0` (default) means no limit.
     * @return the list of substrings obtained by splitting [input].
     */
    fun split(input: CharSequence, limit: Int = 0): List<String> =
        regex.split(input, limit)

    /**
     * Attempts to match the entire [input] against this pattern, returning a [MatchResult]
     * on success or null if the input does not fully match.
     *
     * Unlike [find], the pattern must cover the whole input.
     *
     * Example:
     * ```kotlin
     * val order = kexpresso { oneOrMore { letter() } }
     * order.matchEntire("Espresso")?.value // "Espresso"
     * order.matchEntire("Espresso42")      // null (digits are not letters)
     * ```
     *
     * @param input the character sequence to match against.
     * @return a [MatchResult] if the entire [input] matches, otherwise null.
     */
    fun matchEntire(input: CharSequence): MatchResult? =
        regex.matchEntire(input)

    /** Returns the raw regex source string. */
    override fun toString(): String = source

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KexpressoPattern) return false
        return source == other.source && options == other.options
    }

    override fun hashCode(): Int = 31 * source.hashCode() + options.hashCode()
}

/**
 * Validates a regex group name and throws [IllegalArgumentException] if invalid.
 *
 * The JVM regex engine requires group names to start with a Latin letter and consist
 * of only Latin letters and ASCII digits — underscores are **not** accepted by
 * `java.util.regex.Pattern` at compile time.
 */
private fun requireValidGroupName(name: String) {
    require(name.matches(Regex("[a-zA-Z][a-zA-Z0-9]*"))) {
        "Invalid group name '$name': must start with a letter and contain only letters or digits " +
            "(the JVM regex engine does not allow underscores in group names)."
    }
}

/**
 * Fluent builder that assembles a regular-expression string from high-level primitives.
 *
 * Every method returns `this`, enabling a natural chaining style:
 * ```kotlin
 * val p = kexpresso {
 *     startOfText()
 *     capitalLetter()
 *     oneOrMore { letter() }
 *     endOfText()
 * }
 * p.matches("Espresso") // true
 * p.matches("espresso") // false
 * ```
 */
@Suppress("TooManyFunctions")
class KexpressoBuilder {

    private val nodes = mutableListOf<RegexNode>()

    // ── internal helpers ────────────────────────────────────────────────────

    /**
     * Appends [token] verbatim as a [Raw] node.
     *
     * This is the shim that lets the typed primitives and every domain/helper extension
     * (`Text.kt`, `Writing.kt`, `Domains.kt`) keep their external behaviour and signature
     * unchanged: each emits its raw regex fragment and the AST records it as [Raw].
     */
    internal fun append(token: String): KexpressoBuilder = add(Raw(token))

    /** Adds [node] to the accumulating sequence and returns this builder for chaining. */
    private fun add(node: RegexNode): KexpressoBuilder {
        nodes += node
        return this
    }

    /** Build the node for a child block as a [SequenceNode] without modifying this builder. */
    private fun childNode(block: KexpressoBuilder.() -> Unit): RegexNode {
        val child = KexpressoBuilder()
        child.block()
        return SequenceNode(child.nodes.toList())
    }

    // ── build ────────────────────────────────────────────────────────────────

    /**
     * Compiles the builder's current state into an immutable [KexpressoPattern].
     *
     * @param options optional [RegexOption]s applied to the resulting regex.
     */
    fun build(vararg options: RegexOption): KexpressoPattern {
        val root = SequenceNode(nodes.toList())
        val source = root.render()
        val regex = if (options.isEmpty()) Regex(source) else Regex(source, options.toSet())
        return KexpressoPattern(source, regex, root)
    }

    // ── primitives ───────────────────────────────────────────────────────────

    /**
     * Appends a regex-escaped literal string so that special characters
     * are matched verbatim.
     *
     * @param text the plain text to match.
     */
    fun literal(text: String): KexpressoBuilder = add(Literal(text))

    /**
     * Appends a single character, escaping it if it is a regex meta-character.
     *
     * @param c the character to match.
     */
    fun char(c: Char): KexpressoBuilder = add(Literal(c.toString()))

    /** Matches a decimal digit (`\d`). */
    fun digit(): KexpressoBuilder = add(Token("\\d", "a digit"))

    /** Matches a non-digit character (`\D`). */
    fun nonDigit(): KexpressoBuilder = add(Token("\\D", "a non-digit"))

    /** Matches any whitespace character (`\s`). */
    fun whitespace(): KexpressoBuilder = add(Token("\\s", "whitespace"))

    /**
     * Alias for [whitespace]; matches any whitespace character (`\s`).
     * Kept for backward compatibility.
     */
    fun space(): KexpressoBuilder = add(Token("\\s", "whitespace"))

    /** Matches any non-whitespace character (`\S`). */
    fun nonWhitespace(): KexpressoBuilder = add(Token("\\S", "a non-whitespace character"))

    /** Matches a word character (`\w`). */
    fun wordChar(): KexpressoBuilder = add(Token("\\w", "a word character"))

    /** Matches a non-word character (`\W`). */
    fun nonWordChar(): KexpressoBuilder = add(Token("\\W", "a non-word character"))

    /** Matches any character except newline (`.`). */
    fun anyChar(): KexpressoBuilder = add(Token(".", "any character"))

    /** Matches any ASCII letter `[a-zA-Z]`. */
    fun letter(): KexpressoBuilder = add(Token("[a-zA-Z]", "a letter"))

    /** Matches any ASCII uppercase letter `[A-Z]`. */
    fun capitalLetter(): KexpressoBuilder = add(Token("[A-Z]", "an uppercase letter"))

    /** Matches any sentence-ending punctuation `[.!?]`. */
    fun endPunctuation(): KexpressoBuilder = add(Token("[.!?]", "sentence-ending punctuation"))

    // ── character classes ────────────────────────────────────────────────────

    /**
     * Matches any one character in [chars] (`[...]`).
     * Special characters inside the class are escaped appropriately.
     *
     * @param chars the set of characters to match.
     */
    fun anyOf(chars: String): KexpressoBuilder {
        val escaped = chars.replace("\\", "\\\\")
            .replace("]", "\\]")
            .replace("^", "\\^")
            .replace("-", "\\-")
        return add(Token("[$escaped]", "any of \"$chars\""))
    }

    /**
     * Matches any one character NOT in [chars] (`[^...]`).
     *
     * @param chars the set of characters to exclude.
     */
    fun noneOf(chars: String): KexpressoBuilder {
        val escaped = chars.replace("\\", "\\\\")
            .replace("]", "\\]")
            .replace("^", "\\^")
            .replace("-", "\\-")
        return add(Token("[^$escaped]", "none of \"$chars\""))
    }

    /**
     * Matches any character in the range `[from-to]`.
     *
     * @param from the lower bound (inclusive).
     * @param to the upper bound (inclusive).
     */
    fun inRange(from: Char, to: Char): KexpressoBuilder =
        add(Token("[$from-$to]", "a character in range $from-$to"))

    // ── anchors ──────────────────────────────────────────────────────────────

    /** Anchors to the start of a line (`^`). */
    fun startOfLine(): KexpressoBuilder = add(Token("^", "start of line"))

    /** Anchors to the end of a line (`$`). */
    fun endOfLine(): KexpressoBuilder = add(Token("$", "end of line"))

    /** Anchors to the start of the entire input (`\A`). */
    fun startOfText(): KexpressoBuilder = add(Token("\\A", "start of text"))

    /** Anchors to the end of the entire input (`\z`). */
    fun endOfText(): KexpressoBuilder = add(Token("\\z", "end of text"))

    /** Matches a word boundary (`\b`). */
    fun wordBoundary(): KexpressoBuilder = add(Token("\\b", "a word boundary"))

    // ── completions ──────────────────────────────────────────────────────────

    /** Matches a non-word boundary (`\B`). */
    fun nonWordBoundary(): KexpressoBuilder = add(Token("\\B", "a non-word boundary"))

    /** Matches any lowercase ASCII letter (`[a-z]`). */
    fun lowercaseLetter(): KexpressoBuilder = add(Token("[a-z]", "a lowercase letter"))

    /** Matches any ASCII letter or digit (`[a-zA-Z0-9]`). */
    fun alphanumeric(): KexpressoBuilder = add(Token("[a-zA-Z0-9]", "an alphanumeric character"))

    /** Matches a horizontal tab character (`\t`). */
    fun tab(): KexpressoBuilder = add(Token("\\t", "a tab"))

    /** Matches a newline character (`\n`). */
    fun newline(): KexpressoBuilder = add(Token("\\n", "a newline"))

    /** Matches a carriage-return character (`\r`). */
    fun carriageReturn(): KexpressoBuilder = add(Token("\\r", "a carriage return"))

    // ── quantifiers ──────────────────────────────────────────────────────────

    /**
     * Makes the [block] pattern optional (`(?:...)?`).
     *
     * @param greedy when false, the quantifier is lazy (`??`).
     * @param block the pattern to make optional.
     */
    fun optional(greedy: Boolean = true, block: KexpressoBuilder.() -> Unit): KexpressoBuilder =
        add(Quantifier(childNode(block), QuantifierKind.Optional, greedy))

    /**
     * Repeats the [block] pattern zero or more times (`(?:...)*`).
     *
     * @param greedy when false, the quantifier is lazy (`*?`).
     * @param block the pattern to repeat.
     */
    fun zeroOrMore(greedy: Boolean = true, block: KexpressoBuilder.() -> Unit): KexpressoBuilder =
        add(Quantifier(childNode(block), QuantifierKind.ZeroOrMore, greedy))

    /**
     * Repeats the [block] pattern one or more times (`(?:...)+`).
     *
     * @param greedy when false, the quantifier is lazy (`+?`).
     * @param block the pattern to repeat.
     */
    fun oneOrMore(greedy: Boolean = true, block: KexpressoBuilder.() -> Unit): KexpressoBuilder =
        add(Quantifier(childNode(block), QuantifierKind.OneOrMore, greedy))

    /**
     * Repeats the [block] pattern exactly [n] times (`(?:...){n}`).
     *
     * @param n the exact repetition count.
     * @param block the pattern to repeat.
     */
    fun exactly(n: Int, block: KexpressoBuilder.() -> Unit): KexpressoBuilder =
        add(Quantifier(childNode(block), QuantifierKind.Exactly(n), greedy = true))

    /**
     * Repeats the [block] pattern at least [n] times (`(?:...){n,}`).
     *
     * @param n the minimum repetition count.
     * @param greedy when false, the quantifier is lazy.
     * @param block the pattern to repeat.
     */
    fun atLeast(n: Int, greedy: Boolean = true, block: KexpressoBuilder.() -> Unit): KexpressoBuilder =
        add(Quantifier(childNode(block), QuantifierKind.AtLeast(n), greedy))

    /**
     * Repeats the [block] pattern between [min] and [max] times (`(?:...){min,max}`).
     *
     * @param min the minimum repetition count.
     * @param max the maximum repetition count.
     * @param greedy when false, the quantifier is lazy.
     * @param block the pattern to repeat.
     */
    fun between(
        min: Int,
        max: Int,
        greedy: Boolean = true,
        block: KexpressoBuilder.() -> Unit,
    ): KexpressoBuilder =
        add(Quantifier(childNode(block), QuantifierKind.Between(min, max), greedy))

    // ── grouping & alternation ────────────────────────────────────────────────

    /**
     * Wraps the [block] pattern in a non-capturing group (`(?:...)`).
     *
     * @param block the pattern to group.
     */
    fun group(block: KexpressoBuilder.() -> Unit): KexpressoBuilder =
        add(Group(childNode(block), GroupKind.NonCapturing))

    /**
     * Wraps the [block] pattern in a capturing group (`(...)`).
     *
     * @param block the pattern to capture.
     */
    fun capture(block: KexpressoBuilder.() -> Unit): KexpressoBuilder =
        add(Group(childNode(block), GroupKind.Capturing))

    /**
     * Wraps the [block] pattern in a named capturing group (`(?<name>...)`).
     *
     * The [name] must start with a Latin letter and contain only Latin letters or ASCII digits.
     * Note: the JVM regex engine does **not** allow underscores in named group names.
     * An invalid name throws [IllegalArgumentException] immediately at the call site rather
     * than producing an obscure `PatternSyntaxException` later.
     *
     * @param name the capture group name.
     * @param block the pattern to capture.
     * @throws IllegalArgumentException if [name] is not a valid group name.
     */
    fun capture(name: String, block: KexpressoBuilder.() -> Unit): KexpressoBuilder {
        requireValidGroupName(name)
        return add(Group(childNode(block), GroupKind.Named(name)))
    }

    /**
     * Matches any one of the patterns produced by [blocks], joined by `|` and
     * wrapped in a non-capturing group (`(?:a|b|c)`).
     *
     * @param blocks the alternative patterns.
     */
    fun oneOf(vararg blocks: KexpressoBuilder.() -> Unit): KexpressoBuilder =
        add(Alternation(blocks.map { childNode(it) }))

    // ── lookarounds ──────────────────────────────────────────────────────────

    /**
     * Asserts that the current position is immediately followed by the pattern
     * produced by [block], without consuming any characters (positive lookahead `(?=...)`).
     *
     * Example:
     * ```kotlin
     * // Match digits only when followed by "ml"
     * val p = kexpresso { oneOrMore { digit() }; followedBy { literal("ml") } }
     * p.find("250ml")?.value // "250"
     * p.find("250g")         // null
     * ```
     *
     * @param block the pattern that must appear immediately after the current position.
     */
    fun followedBy(block: KexpressoBuilder.() -> Unit): KexpressoBuilder =
        add(Lookaround(childNode(block), LookaroundKind.FollowedBy))

    /**
     * Asserts that the current position is NOT immediately followed by the pattern
     * produced by [block], without consuming any characters (negative lookahead `(?!...)`).
     *
     * Example:
     * ```kotlin
     * // Match "Espresso" only when NOT followed by "Martini"
     * val p = kexpresso { literal("Espresso"); notFollowedBy { literal("Martini") } }
     * p.containsMatchIn("Espresso!")       // true
     * p.containsMatchIn("EspressoMartini") // false
     * ```
     *
     * @param block the pattern that must NOT appear immediately after the current position.
     */
    fun notFollowedBy(block: KexpressoBuilder.() -> Unit): KexpressoBuilder =
        add(Lookaround(childNode(block), LookaroundKind.NotFollowedBy))

    /**
     * Asserts that the current position is immediately preceded by the pattern
     * produced by [block], without consuming any characters (positive lookbehind `(?<=...)`).
     *
     * Example:
     * ```kotlin
     * // Match digits only when preceded by "$"
     * val p = kexpresso { precededBy { literal("\$") }; oneOrMore { digit() } }
     * p.find("Total: \$42")?.value // "42"
     * p.find("Total: 42")         // null
     * ```
     *
     * @param block the pattern that must appear immediately before the current position.
     */
    fun precededBy(block: KexpressoBuilder.() -> Unit): KexpressoBuilder =
        add(Lookaround(childNode(block), LookaroundKind.PrecededBy))

    /**
     * Asserts that the current position is NOT immediately preceded by the pattern
     * produced by [block], without consuming any characters (negative lookbehind `(?<!...)`).
     *
     * Example:
     * ```kotlin
     * // Match digits only when NOT preceded by "$"
     * val p = kexpresso { notPrecededBy { literal("\$") }; oneOrMore { digit() } }
     * p.find("Qty: 42")       // MatchResult("42")
     * p.find("Total: \$42")   // null (digits are preceded by "$")
     * ```
     *
     * @param block the pattern that must NOT appear immediately before the current position.
     */
    fun notPrecededBy(block: KexpressoBuilder.() -> Unit): KexpressoBuilder =
        add(Lookaround(childNode(block), LookaroundKind.NotPrecededBy))

    // ── composition & escape hatch ────────────────────────────────────────────

    /**
     * Appends [pattern] verbatim to the current pattern without any escaping or wrapping.
     *
     * **Warning:** the string is inserted as-is. Any regex metacharacters in [pattern]
     * will be interpreted by the regex engine. Use this only when you need to inject
     * a raw regex fragment that the DSL does not yet cover; prefer the typed DSL methods
     * whenever possible.
     *
     * Example:
     * ```kotlin
     * val p = kexpresso { raw("\\d{4}-\\d{2}-\\d{2}") }
     * p.matches("2026-06-03") // true
     * ```
     *
     * @param pattern the raw regex string to append verbatim.
     */
    fun raw(pattern: String): KexpressoBuilder = add(Raw(pattern))

    /**
     * Appends a numeric back-reference (`\n`) that matches the same text captured by
     * the [n]th capturing group.
     *
     * Example — detect repeated words:
     * ```kotlin
     * val repeated = kexpresso {
     *     capture { oneOrMore { wordChar() } }
     *     whitespace()
     *     backreference(1)
     * }
     * repeated.containsMatchIn("latte latte") // true
     * repeated.containsMatchIn("latte mocha") // false
     * ```
     *
     * @param n the 1-based index of the capturing group to reference. Must be >= 1.
     * @throws IllegalArgumentException if [n] is less than 1.
     */
    fun backreference(n: Int): KexpressoBuilder {
        require(n >= 1) { "Back-reference index must be >= 1, but was $n." }
        return add(Backreference("$n", "group $n"))
    }

    /**
     * Appends a named back-reference (`\k<name>`) that matches the same text captured
     * by the group named [name].
     *
     * The [name] must start with a Latin letter and contain only Latin letters or ASCII digits.
     * Note: the JVM regex engine does **not** allow underscores in group names.
     * An invalid name throws [IllegalArgumentException] immediately at the call site.
     *
     * Example — detect a repeated drink name:
     * ```kotlin
     * val repeated = kexpresso {
     *     capture("drink") { oneOrMore { letter() } }
     *     whitespace()
     *     backreference("drink")
     * }
     * repeated.containsMatchIn("Latte Latte") // true
     * repeated.containsMatchIn("Latte Mocha") // false
     * ```
     *
     * @param name the name of the capturing group to reference.
     * @throws IllegalArgumentException if [name] is not a valid group name.
     */
    fun backreference(name: String): KexpressoBuilder {
        requireValidGroupName(name)
        return add(Backreference("k<$name>", "group \"$name\""))
    }

    /**
     * Embeds the given [pattern] as a non-capturing group (`(?:...)`), preserving its
     * precedence and allowing it to be safely combined with quantifiers and other
     * sub-patterns.
     *
     * Use this to define a sub-pattern once and reuse it in multiple places.
     *
     * Example:
     * ```kotlin
     * val octet = kexpresso { between(1, 3) { digit() } }
     * val ip = kexpresso {
     *     include(octet)
     *     exactly(3) { char('.'); include(octet) }
     * }
     * ip.matches("192.168.1.1") // true
     * ```
     *
     * @param pattern the compiled [KexpressoPattern] to embed.
     */
    fun include(pattern: KexpressoPattern): KexpressoBuilder =
        add(Group(pattern.ast, GroupKind.NonCapturing))
}
