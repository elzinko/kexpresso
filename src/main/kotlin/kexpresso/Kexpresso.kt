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
class KexpressoPattern(
    val source: String,
    val regex: Regex,
) {
    /** The set of [RegexOption]s used when compiling this pattern. */
    val options: Set<RegexOption> get() = regex.options

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
     * Returns a [java.util.regex.Pattern] equivalent to this pattern.
     */
    fun toPattern(): java.util.regex.Pattern = regex.toPattern()

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

    private val sb = StringBuilder()

    // ── internal helpers ────────────────────────────────────────────────────

    /** Append [token] directly to the internal buffer. */
    internal fun append(token: String): KexpressoBuilder {
        sb.append(token)
        return this
    }

    /** Render a child block into a standalone string without modifying this builder. */
    private fun renderBlock(block: KexpressoBuilder.() -> Unit): String {
        val child = KexpressoBuilder()
        child.block()
        return child.sb.toString()
    }

    /** Wrap [inner] in a non-capturing group `(?:...)`. */
    private fun nonCaptureGroup(inner: String): String = "(?:$inner)"

    /** Append a lazy suffix `?` when [greedy] is false. */
    private fun lazySuffix(greedy: Boolean): String = if (greedy) "" else "?"

    // ── build ────────────────────────────────────────────────────────────────

    /**
     * Compiles the builder's current state into an immutable [KexpressoPattern].
     *
     * @param options optional [RegexOption]s applied to the resulting regex.
     */
    fun build(vararg options: RegexOption): KexpressoPattern {
        val source = sb.toString()
        val regex = if (options.isEmpty()) Regex(source) else Regex(source, options.toSet())
        return KexpressoPattern(source, regex)
    }

    // ── primitives ───────────────────────────────────────────────────────────

    /**
     * Appends a regex-escaped literal string so that special characters
     * are matched verbatim.
     *
     * @param text the plain text to match.
     */
    fun literal(text: String): KexpressoBuilder = append(Regex.escape(text))

    /**
     * Appends a single character, escaping it if it is a regex meta-character.
     *
     * @param c the character to match.
     */
    fun char(c: Char): KexpressoBuilder = append(Regex.escape(c.toString()))

    /** Matches a decimal digit (`\d`). */
    fun digit(): KexpressoBuilder = append("\\d")

    /** Matches a non-digit character (`\D`). */
    fun nonDigit(): KexpressoBuilder = append("\\D")

    /** Matches any whitespace character (`\s`). */
    fun whitespace(): KexpressoBuilder = append("\\s")

    /**
     * Alias for [whitespace]; matches any whitespace character (`\s`).
     * Kept for backward compatibility.
     */
    fun space(): KexpressoBuilder = append("\\s")

    /** Matches any non-whitespace character (`\S`). */
    fun nonWhitespace(): KexpressoBuilder = append("\\S")

    /** Matches a word character (`\w`). */
    fun wordChar(): KexpressoBuilder = append("\\w")

    /** Matches a non-word character (`\W`). */
    fun nonWordChar(): KexpressoBuilder = append("\\W")

    /** Matches any character except newline (`.`). */
    fun anyChar(): KexpressoBuilder = append(".")

    /** Matches any ASCII letter `[a-zA-Z]`. */
    fun letter(): KexpressoBuilder = append("[a-zA-Z]")

    /** Matches any ASCII uppercase letter `[A-Z]`. */
    fun capitalLetter(): KexpressoBuilder = append("[A-Z]")

    /** Matches any sentence-ending punctuation `[.!?]`. */
    fun endPunctuation(): KexpressoBuilder = append("[.!?]")

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
        return append("[$escaped]")
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
        return append("[^$escaped]")
    }

    /**
     * Matches any character in the range `[from-to]`.
     *
     * @param from the lower bound (inclusive).
     * @param to the upper bound (inclusive).
     */
    fun inRange(from: Char, to: Char): KexpressoBuilder = append("[$from-$to]")

    // ── anchors ──────────────────────────────────────────────────────────────

    /** Anchors to the start of a line (`^`). */
    fun startOfLine(): KexpressoBuilder = append("^")

    /** Anchors to the end of a line (`$`). */
    fun endOfLine(): KexpressoBuilder = append("$")

    /** Anchors to the start of the entire input (`\A`). */
    fun startOfText(): KexpressoBuilder = append("\\A")

    /** Anchors to the end of the entire input (`\z`). */
    fun endOfText(): KexpressoBuilder = append("\\z")

    /** Matches a word boundary (`\b`). */
    fun wordBoundary(): KexpressoBuilder = append("\\b")

    // ── quantifiers ──────────────────────────────────────────────────────────

    /**
     * Makes the [block] pattern optional (`(?:...)?`).
     *
     * @param greedy when false, the quantifier is lazy (`??`).
     * @param block the pattern to make optional.
     */
    fun optional(greedy: Boolean = true, block: KexpressoBuilder.() -> Unit): KexpressoBuilder {
        val inner = nonCaptureGroup(renderBlock(block))
        return append("$inner?${lazySuffix(greedy)}")
    }

    /**
     * Repeats the [block] pattern zero or more times (`(?:...)*`).
     *
     * @param greedy when false, the quantifier is lazy (`*?`).
     * @param block the pattern to repeat.
     */
    fun zeroOrMore(greedy: Boolean = true, block: KexpressoBuilder.() -> Unit): KexpressoBuilder {
        val inner = nonCaptureGroup(renderBlock(block))
        return append("$inner*${lazySuffix(greedy)}")
    }

    /**
     * Repeats the [block] pattern one or more times (`(?:...)+`).
     *
     * @param greedy when false, the quantifier is lazy (`+?`).
     * @param block the pattern to repeat.
     */
    fun oneOrMore(greedy: Boolean = true, block: KexpressoBuilder.() -> Unit): KexpressoBuilder {
        val inner = nonCaptureGroup(renderBlock(block))
        return append("$inner+${lazySuffix(greedy)}")
    }

    /**
     * Repeats the [block] pattern exactly [n] times (`(?:...){n}`).
     *
     * @param n the exact repetition count.
     * @param block the pattern to repeat.
     */
    fun exactly(n: Int, block: KexpressoBuilder.() -> Unit): KexpressoBuilder {
        val inner = nonCaptureGroup(renderBlock(block))
        return append("$inner{$n}")
    }

    /**
     * Repeats the [block] pattern at least [n] times (`(?:...){n,}`).
     *
     * @param n the minimum repetition count.
     * @param greedy when false, the quantifier is lazy.
     * @param block the pattern to repeat.
     */
    fun atLeast(n: Int, greedy: Boolean = true, block: KexpressoBuilder.() -> Unit): KexpressoBuilder {
        val inner = nonCaptureGroup(renderBlock(block))
        return append("$inner{$n,}${lazySuffix(greedy)}")
    }

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
    ): KexpressoBuilder {
        val inner = nonCaptureGroup(renderBlock(block))
        return append("$inner{$min,$max}${lazySuffix(greedy)}")
    }

    // ── grouping & alternation ────────────────────────────────────────────────

    /**
     * Wraps the [block] pattern in a non-capturing group (`(?:...)`).
     *
     * @param block the pattern to group.
     */
    fun group(block: KexpressoBuilder.() -> Unit): KexpressoBuilder {
        return append(nonCaptureGroup(renderBlock(block)))
    }

    /**
     * Wraps the [block] pattern in a capturing group (`(...)`).
     *
     * @param block the pattern to capture.
     */
    fun capture(block: KexpressoBuilder.() -> Unit): KexpressoBuilder {
        return append("(${renderBlock(block)})")
    }

    /**
     * Wraps the [block] pattern in a named capturing group (`(?<name>...)`).
     *
     * @param name the capture group name.
     * @param block the pattern to capture.
     */
    fun capture(name: String, block: KexpressoBuilder.() -> Unit): KexpressoBuilder {
        return append("(?<$name>${renderBlock(block)})")
    }

    /**
     * Matches any one of the patterns produced by [blocks], joined by `|` and
     * wrapped in a non-capturing group (`(?:a|b|c)`).
     *
     * @param blocks the alternative patterns.
     */
    fun oneOf(vararg blocks: KexpressoBuilder.() -> Unit): KexpressoBuilder {
        val alternatives = blocks.joinToString("|") { renderBlock(it) }
        return append(nonCaptureGroup(alternatives))
    }
}
