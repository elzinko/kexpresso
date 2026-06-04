package kexpresso

/**
 * Internal abstract-syntax-tree representation of a kexpresso pattern.
 *
 * Each [RegexNode] knows how to [render] itself back to the exact regex string the
 * [KexpressoBuilder] would have produced, and how to [describe] itself in readable English.
 * Rendering is the source of truth for the public `source` string and MUST remain
 * byte-for-byte identical to the legacy string-concatenation builder.
 *
 * The hierarchy is internal: it is an implementation detail behind the public DSL and
 * [KexpressoPattern.describe]. It is the "AST spine" that future introspection features
 * (analysis, diagrams, reverse) build upon.
 */
internal sealed interface RegexNode {
    /** Renders this node to its exact regex string. */
    fun render(): String

    /** Renders this node to a readable English description. */
    fun describe(): String
}

/**
 * An ordered concatenation of child nodes, rendered by joining each child's [render] output.
 *
 * This is the root node of every pattern and the body of every block-based construct.
 */
internal data class SequenceNode(val children: List<RegexNode>) : RegexNode {
    override fun render(): String = children.joinToString("") { it.render() }

    override fun describe(): String =
        children.filter { it.describe().isNotEmpty() }.joinToString(", ") { it.describe() }
}

/**
 * A leaf primitive, anchor, or character class whose regex form is fixed.
 *
 * @property regex the verbatim regex fragment (e.g. `\d`, `\A`, `[a-zA-Z]`).
 * @property description the human-readable phrase (e.g. `a digit`, `start of text`).
 */
internal data class Token(val regex: String, val description: String) : RegexNode {
    override fun render(): String = regex

    override fun describe(): String = description
}

/**
 * An escaped literal string. Renders by backslash-escaping every regex meta-character so the
 * text matches verbatim.
 *
 * The escaping is a hand-written, per-character escaper (rather than `Regex.escape`/`\Q…\E`,
 * which do not exist on Kotlin/JS) so the generated `source` is portable across the JVM
 * (PCRE) and JS (ECMAScript) regex engines. For example `a.b` renders as `a\.b`.
 *
 * @property text the plain text to match literally.
 */
internal data class Literal(val text: String) : RegexNode {
    override fun render(): String = escapeLiteral(text)

    override fun describe(): String = "the literal \"$text\""
}

/**
 * The set of regex meta-characters that [escapeLiteral] backslash-escapes.
 *
 * Includes `/` (harmless on the JVM, required so a literal `/` is safe inside a JS regex
 * literal) in addition to the standard PCRE/ECMAScript metacharacters.
 */
private val LITERAL_META_CHARACTERS: Set<Char> =
    setOf('\\', '.', '^', '$', '|', '?', '*', '+', '(', ')', '[', ']', '{', '}', '/')

// Note: '-' is intentionally NOT escaped — it is only special inside a character class,
// and these literals are never emitted inside one.

/**
 * Backslash-escapes every regex meta-character in [text] so the result matches [text] verbatim
 * on both the JVM (PCRE) and JS (ECMAScript) regex engines.
 *
 * This is a portable, common-source replacement for `Regex.escape()` / `\Q…\E`, which are
 * JVM-only.
 */
internal fun escapeLiteral(text: String): String = buildString(text.length) {
    for (c in text) {
        if (c in LITERAL_META_CHARACTERS) append('\\')
        append(c)
    }
}

/**
 * A raw regex fragment inserted verbatim, with no escaping or wrapping.
 *
 * Used by [KexpressoBuilder.raw], by every domain/helper extension that calls the internal
 * `append(token)` shim, and as the fallback node for the public
 * [KexpressoPattern] two-argument constructor.
 *
 * @property regex the raw regex string.
 */
internal data class Raw(val regex: String) : RegexNode {
    override fun render(): String = regex

    override fun describe(): String = "raw regex `$regex`"
}

/** The kinds of repetition a [Quantifier] can apply. */
internal sealed interface QuantifierKind {
    /** `?` — zero or one. */
    object Optional : QuantifierKind

    /** `*` — zero or more. */
    object ZeroOrMore : QuantifierKind

    /** `+` — one or more. */
    object OneOrMore : QuantifierKind

    /** `{n}` — exactly [n]. */
    data class Exactly(val n: Int) : QuantifierKind

    /** `{n,}` — at least [n]. */
    data class AtLeast(val n: Int) : QuantifierKind

    /** `{min,max}` — between [min] and [max]. */
    data class Between(val min: Int, val max: Int) : QuantifierKind
}

/**
 * A repetition applied to a non-capturing group around [child].
 *
 * Renders as `(?:` + child + `)` + symbol + optional lazy `?`, reproducing the legacy output
 * exactly. [QuantifierKind.Exactly] is always greedy (no lazy suffix is emitted by the DSL).
 *
 * @property child the wrapped pattern.
 * @property kind the repetition kind.
 * @property greedy when false, a lazy `?` suffix is appended.
 */
internal data class Quantifier(
    val child: RegexNode,
    val kind: QuantifierKind,
    val greedy: Boolean,
) : RegexNode {
    override fun render(): String {
        val symbol = when (kind) {
            is QuantifierKind.Optional -> "?"
            is QuantifierKind.ZeroOrMore -> "*"
            is QuantifierKind.OneOrMore -> "+"
            is QuantifierKind.Exactly -> "{${kind.n}}"
            is QuantifierKind.AtLeast -> "{${kind.n},}"
            is QuantifierKind.Between -> "{${kind.min},${kind.max}}"
        }
        val lazy = if (kind is QuantifierKind.Exactly || greedy) "" else "?"
        return "(?:${child.render()})$symbol$lazy"
    }

    override fun describe(): String {
        val phrase = when (kind) {
            is QuantifierKind.Optional -> "optionally"
            is QuantifierKind.ZeroOrMore -> "zero or more of"
            is QuantifierKind.OneOrMore -> "one or more of"
            is QuantifierKind.Exactly -> "exactly ${kind.n} of"
            is QuantifierKind.AtLeast -> "at least ${kind.n} of"
            is QuantifierKind.Between -> "between ${kind.min} and ${kind.max} of"
        }
        val lazy = if (kind !is QuantifierKind.Exactly && !greedy) " (lazy)" else ""
        return "$phrase (${child.describe()})$lazy"
    }
}

/** The kinds of grouping a [Group] can apply. */
internal sealed interface GroupKind {
    /** `(?:...)` — non-capturing group. */
    object NonCapturing : GroupKind

    /** `(...)` — capturing group. */
    object Capturing : GroupKind

    /** `(?<name>...)` — named capturing group. */
    data class Named(val name: String) : GroupKind
}

/**
 * A grouping construct wrapping [child].
 *
 * @property child the wrapped pattern.
 * @property kind the grouping kind.
 */
internal data class Group(val child: RegexNode, val kind: GroupKind) : RegexNode {
    override fun render(): String = when (kind) {
        is GroupKind.NonCapturing -> "(?:${child.render()})"
        is GroupKind.Capturing -> "(${child.render()})"
        is GroupKind.Named -> "(?<${kind.name}>${child.render()})"
    }

    override fun describe(): String = when (kind) {
        is GroupKind.NonCapturing -> "(${child.describe()})"
        is GroupKind.Capturing -> "a capture of (${child.describe()})"
        is GroupKind.Named -> "a capture named \"${kind.name}\" of (${child.describe()})"
    }
}

/**
 * An alternation of branches, rendered as `(?:a|b|c)`.
 *
 * @property branches the alternative patterns.
 */
internal data class Alternation(val branches: List<RegexNode>) : RegexNode {
    override fun render(): String = "(?:${branches.joinToString("|") { it.render() }})"

    override fun describe(): String =
        "one of (${branches.joinToString("; ") { it.describe() }})"
}

/** The kinds of lookaround a [Lookaround] can apply. */
internal sealed interface LookaroundKind {
    /** `(?=...)` — positive lookahead. */
    object FollowedBy : LookaroundKind

    /** `(?!...)` — negative lookahead. */
    object NotFollowedBy : LookaroundKind

    /** `(?<=...)` — positive lookbehind. */
    object PrecededBy : LookaroundKind

    /** `(?<!...)` — negative lookbehind. */
    object NotPrecededBy : LookaroundKind
}

/**
 * A zero-width lookaround assertion around [child].
 *
 * @property child the asserted pattern.
 * @property kind the lookaround kind.
 */
internal data class Lookaround(val child: RegexNode, val kind: LookaroundKind) : RegexNode {
    override fun render(): String = when (kind) {
        is LookaroundKind.FollowedBy -> "(?=${child.render()})"
        is LookaroundKind.NotFollowedBy -> "(?!${child.render()})"
        is LookaroundKind.PrecededBy -> "(?<=${child.render()})"
        is LookaroundKind.NotPrecededBy -> "(?<!${child.render()})"
    }

    override fun describe(): String = when (kind) {
        is LookaroundKind.FollowedBy -> "followed by (${child.describe()})"
        is LookaroundKind.NotFollowedBy -> "not followed by (${child.describe()})"
        is LookaroundKind.PrecededBy -> "preceded by (${child.describe()})"
        is LookaroundKind.NotPrecededBy -> "not preceded by (${child.describe()})"
    }
}

/**
 * A back-reference to a previously captured group, either numeric (`\n`) or named (`\k<name>`).
 *
 * @property reference the rendered reference body (`1`, `2`, … for numeric; `k<name>` for named).
 * @property label a readable label for the referenced group (e.g. `group 1`, `group "drink"`).
 */
internal data class Backreference(val reference: String, val label: String) : RegexNode {
    override fun render(): String = "\\$reference"

    override fun describe(): String = "a back-reference to $label"
}
