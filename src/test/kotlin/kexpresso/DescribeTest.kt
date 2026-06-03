package kexpresso

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [KexpressoPattern.describe] and the round-trip guarantee that the internal AST
 * renders byte-for-byte identical regex `source` strings.
 *
 * The describe-wording assertions lock the English output; the round-trip assertions make the
 * AST → regex contract explicit (duplicating the broader regression guarantee on purpose).
 */
class DescribeTest {

    // ── describe(): representative patterns ───────────────────────────────────

    @Test
    fun `describe primitives with anchors and quantifier`() {
        val p = kexpresso { startOfText(); oneOrMore { digit() }; endOfText() }
        assertEquals("start of text, one or more of (a digit), end of text", p.describe())
    }

    @Test
    fun `describe primitive plus zero-or-more`() {
        val p = kexpresso { capitalLetter(); zeroOrMore { letter() } }
        assertEquals("an uppercase letter, zero or more of (a letter)", p.describe())
    }

    @Test
    fun `describe literal escapes nothing in the prose`() {
        val p = kexpresso { literal("a-b") }
        assertEquals("the literal \"a-b\"", p.describe())
    }

    @Test
    fun `describe alternation`() {
        val p = kexpresso { oneOf({ literal("cat") }, { literal("dog") }) }
        assertEquals("one of (the literal \"cat\"; the literal \"dog\")", p.describe())
    }

    @Test
    fun `describe exactly quantifier`() {
        val p = kexpresso { exactly(3) { digit() } }
        assertEquals("exactly 3 of (a digit)", p.describe())
    }

    @Test
    fun `describe between quantifier`() {
        val p = kexpresso { between(2, 4) { letter() } }
        assertEquals("between 2 and 4 of (a letter)", p.describe())
    }

    @Test
    fun `describe lazy quantifier flags laziness`() {
        val p = kexpresso { oneOrMore(greedy = false) { digit() } }
        assertEquals("one or more of (a digit) (lazy)", p.describe())
    }

    @Test
    fun `describe optional quantifier`() {
        val p = kexpresso { optional { literal("s") } }
        assertEquals("optionally (the literal \"s\")", p.describe())
    }

    @Test
    fun `describe non-capturing group`() {
        val p = kexpresso { group { letter() } }
        assertEquals("(a letter)", p.describe())
    }

    @Test
    fun `describe capturing group`() {
        val p = kexpresso { capture { oneOrMore { digit() } } }
        assertEquals("a capture of (one or more of (a digit))", p.describe())
    }

    @Test
    fun `describe named capture`() {
        val p = kexpresso { capture("year") { exactly(4) { digit() } } }
        assertEquals("a capture named \"year\" of (exactly 4 of (a digit))", p.describe())
    }

    @Test
    fun `describe numeric backreference`() {
        val p = kexpresso { capture { digit() }; backreference(1) }
        assertEquals("a capture of (a digit), a back-reference to group 1", p.describe())
    }

    @Test
    fun `describe named backreference`() {
        val p = kexpresso { capture("drink") { letter() }; backreference("drink") }
        assertEquals(
            "a capture named \"drink\" of (a letter), a back-reference to group \"drink\"",
            p.describe(),
        )
    }

    @Test
    fun `describe positive lookahead`() {
        val p = kexpresso { oneOrMore { digit() }; followedBy { literal("ml") } }
        assertEquals("one or more of (a digit), followed by (the literal \"ml\")", p.describe())
    }

    @Test
    fun `describe negative lookbehind`() {
        val p = kexpresso { notPrecededBy { literal("$") }; digit() }
        assertEquals("not preceded by (the literal \"$\"), a digit", p.describe())
    }

    @Test
    fun `describe raw fragment`() {
        val p = kexpresso { raw("\\d{4}") }
        assertEquals("raw regex `\\d{4}`", p.describe())
    }

    @Test
    fun `describe domain helper degrades to raw gracefully`() {
        // Domain helpers use the append(token) shim, so they describe as raw regex.
        val p = kexpresso { email() }
        assertEquals(
            "raw regex `[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}`",
            p.describe(),
        )
    }

    @Test
    fun `describe of pattern built via public constructor falls back to raw`() {
        val p = KexpressoPattern("\\d+", Regex("\\d+"))
        assertEquals("raw regex `\\d+`", p.describe())
    }

    // ── round-trip: AST renders identical source ──────────────────────────────

    @Test
    fun `round-trip oneOrMore digit`() {
        assertEquals("(?:\\d)+", kexpresso { oneOrMore { digit() } }.source)
    }

    @Test
    fun `round-trip lazy zeroOrMore`() {
        assertEquals("(?:\\d)*?", kexpresso { zeroOrMore(greedy = false) { digit() } }.source)
    }

    @Test
    fun `round-trip exactly and between`() {
        assertEquals("(?:\\d){3}", kexpresso { exactly(3) { digit() } }.source)
        assertEquals("(?:[a-zA-Z]){2,4}", kexpresso { between(2, 4) { letter() } }.source)
    }

    @Test
    fun `round-trip atLeast lazy`() {
        assertEquals("(?:\\d){2,}?", kexpresso { atLeast(2, greedy = false) { digit() } }.source)
    }

    @Test
    fun `round-trip groups and named capture`() {
        assertEquals("(?:[a-zA-Z])", kexpresso { group { letter() } }.source)
        assertEquals("(\\d)", kexpresso { capture { digit() } }.source)
        assertEquals("(?<year>\\d)", kexpresso { capture("year") { digit() } }.source)
    }

    @Test
    fun `round-trip alternation`() {
        assertEquals(
            "(?:[a-zA-Z]|\\d)",
            kexpresso { oneOf({ letter() }, { digit() }) }.source,
        )
    }

    @Test
    fun `round-trip lookarounds`() {
        assertEquals("(?=\\d)", kexpresso { followedBy { digit() } }.source)
        assertEquals("(?!\\d)", kexpresso { notFollowedBy { digit() } }.source)
        assertEquals("(?<=[a-zA-Z])", kexpresso { precededBy { letter() } }.source)
        assertEquals("(?<![a-zA-Z])", kexpresso { notPrecededBy { letter() } }.source)
    }

    @Test
    fun `round-trip backreferences and literal escaping`() {
        assertEquals("(\\d)\\1", kexpresso { capture { digit() }; backreference(1) }.source)
        assertEquals(
            "(?<d>[a-zA-Z])\\k<d>",
            kexpresso { capture("d") { letter() }; backreference("d") }.source,
        )
        assertEquals("\\Qa-b\\E", kexpresso { literal("a-b") }.source)
    }

    @Test
    fun `round-trip include embeds non-capturing group`() {
        val octet = kexpresso { between(1, 3) { digit() } }
        assertEquals("(?:(?:\\d){1,3})", kexpresso { include(octet) }.source)
    }
}
