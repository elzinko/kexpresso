package kexpresso

import java.util.regex.PatternSyntaxException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for the reverse feature: [Kexpresso.from] and [KexpressoPattern.toKexpressoCode].
 *
 * The most important property is **matching correctness**: a pattern produced by
 * [Kexpresso.from] must match exactly the same inputs as a verbatim [Regex] of the same source,
 * including when the structural parse degrades to a `raw` fallback. The remaining tests lock the
 * `describe()` prose, the exact generated DSL source, the round-trip behaviour, and the
 * propagation of [PatternSyntaxException] on invalid input.
 */
class ReverseTest {

    // ── Matching correctness (most important) ─────────────────────────────────

    /** Asserts `Kexpresso.from(regex)` matches exactly as `Regex(regex)` for every input. */
    private fun assertSameMatching(regex: String, vararg inputs: String) {
        val reference = Regex(regex)
        val reversed = Kexpresso.from(regex)
        for (input in inputs) {
            assertEquals(
                reference.matches(input),
                reversed.matches(input),
                "matches() disagreement for /$regex/ on \"$input\"",
            )
            assertEquals(
                reference.containsMatchIn(input),
                reversed.containsMatchIn(input),
                "containsMatchIn() disagreement for /$regex/ on \"$input\"",
            )
        }
        // The original source string is preserved verbatim.
        assertEquals(regex, reversed.source)
    }

    @Test
    fun `date pattern matches identically`() {
        assertSameMatching(
            "\\d{4}-\\d{2}-\\d{2}",
            "2026-06-03", "2026-6-3", "not-a-date", "1999-12-31", "",
        )
    }

    @Test
    fun `anchored word pattern matches identically`() {
        assertSameMatching(
            "^[a-zA-Z]+$",
            "Espresso", "latte123", "", "ABC", "a b",
        )
    }

    @Test
    fun `alternation matches identically`() {
        assertSameMatching(
            "cat|dog",
            "cat", "dog", "cattle", "bird", "do",
        )
    }

    @Test
    fun `named capture with quantifier matches identically`() {
        assertSameMatching(
            "(?<year>\\d{4})-(?<month>\\d{2})",
            "2026-06", "26-6", "abcd-ef", "2026-067",
        )
    }

    @Test
    fun `escaped metacharacters and predefined classes match identically`() {
        assertSameMatching(
            "\\$\\d+\\.\\d{2}",
            "\$42.00", "\$5.5", "42.00", "\$100.99",
        )
    }

    @Test
    fun `possessive quantifier falls back to raw but still matches identically`() {
        // a++ is possessive; the parser degrades it to a raw node — matching must stay exact.
        assertSameMatching(
            "a++b",
            "aaab", "ab", "b", "aaa",
        )
        // Prove the structural fallback actually produced a raw() call.
        assertTrue(
            Kexpresso.from("a++b").toKexpressoCode().contains("raw(\"a++\")"),
            "expected possessive quantifier to degrade to raw()",
        )
    }

    @Test
    fun `inline-flag exotic group falls back to raw but still matches identically`() {
        // (?i) inline flags are not modeled; they degrade to raw while matching stays exact.
        assertSameMatching(
            "(?i:cat)dog",
            "CATdog", "catdog", "Catdog", "catDOG",
        )
    }

    // ── describe() ────────────────────────────────────────────────────────────

    @Test
    fun `describe a parsed date`() {
        val p = Kexpresso.from("\\d{4}-\\d{2}-\\d{2}")
        assertEquals(
            "exactly 4 of (a digit), the literal \"-\", exactly 2 of (a digit), " +
                "the literal \"-\", exactly 2 of (a digit)",
            p.describe(),
        )
    }

    @Test
    fun `describe an anchored alternation of letters`() {
        val p = Kexpresso.from("^(cat|dog)$")
        assertEquals(
            "start of line, a capture of (one of (the literal \"cat\"; the literal \"dog\")), " +
                "end of line",
            p.describe(),
        )
    }

    @Test
    fun `describe a named capture`() {
        val p = Kexpresso.from("(?<word>\\w+)")
        assertEquals(
            "a capture named \"word\" of (one or more of (a word character))",
            p.describe(),
        )
    }

    // ── toKexpressoCode(): exact generated source ─────────────────────────────

    @Test
    fun `generate code for a date`() {
        val code = Kexpresso.from("\\d{4}-\\d{2}-\\d{2}").toKexpressoCode()
        assertEquals(
            """
            |kexpresso {
            |    exactly(4) { digit() }
            |    literal("-")
            |    exactly(2) { digit() }
            |    literal("-")
            |    exactly(2) { digit() }
            |}
            """.trimMargin(),
            code,
        )
    }

    @Test
    fun `generate code for an anchored word`() {
        val code = Kexpresso.from("^[a-zA-Z]+$").toKexpressoCode()
        assertEquals(
            """
            |kexpresso {
            |    startOfLine()
            |    oneOrMore { letter() }
            |    endOfLine()
            |}
            """.trimMargin(),
            code,
        )
    }

    @Test
    fun `generate code for an alternation`() {
        val code = Kexpresso.from("cat|dog").toKexpressoCode()
        assertEquals(
            """
            |kexpresso {
            |    oneOf(
            |        {
            |            literal("cat")
            |        },
            |        {
            |            literal("dog")
            |        },
            |    )
            |}
            """.trimMargin(),
            code,
        )
    }

    @Test
    fun `generate code for a named capture`() {
        val code = Kexpresso.from("(?<year>\\d{4})").toKexpressoCode()
        assertEquals(
            """
            |kexpresso {
            |    capture("year") { exactly(4) { digit() } }
            |}
            """.trimMargin(),
            code,
        )
    }

    @Test
    fun `generate code with a raw fallback for a possessive quantifier`() {
        val code = Kexpresso.from("a++b").toKexpressoCode()
        assertEquals(
            """
            |kexpresso {
            |    raw("a++")
            |    literal("b")
            |}
            """.trimMargin(),
            code,
        )
    }

    @Test
    fun `generate code for a lazy quantifier emits greedy false`() {
        val code = Kexpresso.from("\\d+?").toKexpressoCode()
        assertEquals(
            """
            |kexpresso {
            |    oneOrMore(greedy = false) { digit() }
            |}
            """.trimMargin(),
            code,
        )
    }

    @Test
    fun `generate code for lookahead and backreference`() {
        val code = Kexpresso.from("(\\w+)\\s\\1(?=!)").toKexpressoCode()
        assertEquals(
            """
            |kexpresso {
            |    capture { oneOrMore { wordChar() } }
            |    whitespace()
            |    backreference(1)
            |    followedBy { literal("!") }
            |}
            """.trimMargin(),
            code,
        )
    }

    // ── Round-trip ────────────────────────────────────────────────────────────

    @Test
    fun `builder-made pattern generates the expected DSL`() {
        val p = kexpresso {
            startOfText()
            capture("name") { oneOrMore { letter() } }
            endOfText()
        }
        assertEquals(
            """
            |kexpresso {
            |    startOfText()
            |    capture("name") { oneOrMore { letter() } }
            |    endOfText()
            |}
            """.trimMargin(),
            p.toKexpressoCode(),
        )
    }

    @Test
    fun `builder-made pattern with raw and literal round-trips its source through from`() {
        val patterns = listOf(
            kexpresso { digit(); literal("-"); oneOrMore { letter() } },
            kexpresso { raw("\\d{4}-\\d{2}-\\d{2}") },
            kexpresso { oneOf({ literal("tea") }, { literal("coffee") }) },
        )
        for (p in patterns) {
            assertEquals(p.source, Kexpresso.from(p.source).source)
        }
    }

    @Test
    fun `named backreference generates the expected DSL`() {
        val code = Kexpresso.from("(?<drink>\\w+)\\s\\k<drink>").toKexpressoCode()
        assertEquals(
            """
            |kexpresso {
            |    capture("drink") { oneOrMore { wordChar() } }
            |    whitespace()
            |    backreference("drink")
            |}
            """.trimMargin(),
            code,
        )
    }

    // ── Error propagation ─────────────────────────────────────────────────────

    @Test
    fun `from propagates PatternSyntaxException on invalid regex`() {
        assertFailsWith<PatternSyntaxException> {
            Kexpresso.from("(unclosed")
        }
    }

    @Test
    fun `from propagates PatternSyntaxException on dangling quantifier`() {
        assertFailsWith<PatternSyntaxException> {
            Kexpresso.from("*abc")
        }
    }

    // ── Extra coverage: friendly token mappings and constructs ────────────────

    @Test
    fun `generate code maps the full friendly token table`() {
        val code = Kexpresso.from("\\d\\D\\s\\S\\w\\W.\\A\\z\\b\\B\\t\\n\\r").toKexpressoCode()
        assertEquals(
            """
            |kexpresso {
            |    digit()
            |    nonDigit()
            |    whitespace()
            |    nonWhitespace()
            |    wordChar()
            |    nonWordChar()
            |    anyChar()
            |    startOfText()
            |    endOfText()
            |    wordBoundary()
            |    nonWordBoundary()
            |    tab()
            |    newline()
            |    carriageReturn()
            |}
            """.trimMargin(),
            code,
        )
    }

    @Test
    fun `generate code maps friendly character classes`() {
        val code = Kexpresso.from("[a-z][A-Z][a-zA-Z0-9][.!?]").toKexpressoCode()
        assertEquals(
            """
            |kexpresso {
            |    lowercaseLetter()
            |    capitalLetter()
            |    alphanumeric()
            |    endPunctuation()
            |}
            """.trimMargin(),
            code,
        )
    }

    @Test
    fun `generate code for the remaining quantifier and group shapes`() {
        val code = Kexpresso.from("(?:a)*(b)?[0-9]{2,}(?!x)(?<!y)").toKexpressoCode()
        assertEquals(
            """
            |kexpresso {
            |    zeroOrMore { group { literal("a") } }
            |    optional { capture { literal("b") } }
            |    atLeast(2) { raw("[0-9]") }
            |    notFollowedBy { literal("x") }
            |    notPrecededBy { literal("y") }
            |}
            """.trimMargin(),
            code,
        )
    }

    @Test
    fun `between quantifier and preceded-by lookbehind generate expected DSL`() {
        val code = Kexpresso.from("(?<=\\$)\\d{1,3}").toKexpressoCode()
        assertEquals(
            """
            |kexpresso {
            |    precededBy { literal("${'$'}") }
            |    between(1, 3) { digit() }
            |}
            """.trimMargin(),
            code,
        )
    }

    @Test
    fun `unmodeled unicode class still matches identically`() {
        // \p{Alpha} is a Unicode class the parser does not model structurally; the parse degrades
        // gracefully (without crashing) while matching stays exact because the source is verbatim.
        assertSameMatching("\\p{Alpha}+", "abc", "123", "")
        // The generated code is still compilable kexpresso source (no exception thrown).
        assertTrue(Kexpresso.from("\\p{Alpha}+").toKexpressoCode().startsWith("kexpresso {"))
    }

    @Test
    fun `atomic exotic group falls back to raw and matches identically`() {
        // (?>a+) is an atomic group — an exotic form the parser emits verbatim as raw().
        assertSameMatching("(?>a+)b", "aaab", "ab", "b")
        assertTrue(
            Kexpresso.from("(?>a+)b").toKexpressoCode().contains("raw(\"(?>a+)\")"),
            "expected atomic group to degrade to raw()",
        )
    }

    @Test
    fun `lazy between quantifier emits greedy false`() {
        val code = Kexpresso.from("\\d{2,4}?").toKexpressoCode()
        assertEquals(
            """
            |kexpresso {
            |    between(2, 4, greedy = false) { digit() }
            |}
            """.trimMargin(),
            code,
        )
    }

    @Test
    fun `multi-element block body expands across multiple lines`() {
        // (?:\d\w)+ — the group body has two tokens, so the block cannot collapse to one line.
        val code = Kexpresso.from("(?:\\d\\w)+").toKexpressoCode()
        assertEquals(
            """
            |kexpresso {
            |    oneOrMore {
            |        group {
            |            digit()
            |            wordChar()
            |        }
            |    }
            |}
            """.trimMargin(),
            code,
        )
    }

    @Test
    fun `nested alternation inside a capture matches and generates expected DSL`() {
        assertSameMatching("(tea|coffee)!", "tea!", "coffee!", "milk!", "tea")
        val code = Kexpresso.from("(tea|coffee)!").toKexpressoCode()
        assertEquals(
            """
            |kexpresso {
            |    capture {
            |        oneOf(
            |            {
            |                literal("tea")
            |            },
            |            {
            |                literal("coffee")
            |            },
            |        )
            |    }
            |    literal("!")
            |}
            """.trimMargin(),
            code,
        )
    }

    // ── Parser degradation on edge fragments (direct, never-throw guarantee) ──

    @Test
    fun `parser degrades gracefully on an unterminated character class`() {
        // An unterminated '[' is invalid regex (so `from` would reject it), but the parser itself
        // must never throw — it consumes to end-of-input and emits the class fragment verbatim.
        val node = RegexParser("[abc").parse()
        assertEquals("[abc", node.render(), "unterminated class should render verbatim")
    }

    @Test
    fun `parser degrades gracefully on a trailing backslash`() {
        // A lone trailing '\' cannot start an escape; the parser emits it as a raw fragment and
        // must not throw. (The 'ab' prefix becomes a literal, which renders via Regex.escape.)
        val node = RegexParser("ab\\").parse()
        assertTrue(node.render().endsWith("\\"), "trailing backslash should be preserved verbatim")
    }
}
