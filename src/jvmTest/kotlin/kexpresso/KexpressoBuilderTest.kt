package kexpresso

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [KexpressoBuilder] primitives, quantifiers, character classes,
 * anchors, and grouping/alternation constructs.
 */
class KexpressoBuilderTest {

    // ── helper ───────────────────────────────────────────────────────────────

    private fun pattern(block: KexpressoBuilder.() -> Unit) = kexpresso(block = block)

    // ── primitives ───────────────────────────────────────────────────────────

    @Test
    fun `literal matches exact text`() {
        val p = pattern { literal("Espresso") }
        assertTrue(p.matches("Espresso"))
        assertFalse(p.matches("espresso"))
        assertFalse(p.matches("Espressoo"))
    }

    @Test
    fun `literal escapes regex meta-characters`() {
        val p = pattern { literal("a.b") }
        assertTrue(p.matches("a.b"))
        assertFalse(p.matches("axb"))  // dot is literal, not wildcard
    }

    @Test
    fun `literal with special chars`() {
        val p = pattern { literal("price: \$5.00") }
        assertTrue(p.matches("price: \$5.00"))
        assertFalse(p.matches("price: 500"))
    }

    @Test
    fun `char matches single character`() {
        val p = pattern { char('E') }
        assertTrue(p.matches("E"))
        assertFalse(p.matches("e"))
    }

    @Test
    fun `char escapes dot`() {
        val p = pattern { char('.') }
        assertTrue(p.matches("."))
        assertFalse(p.matches("x"))
    }

    @Test
    fun `digit matches 0-9`() {
        val p = pattern { digit() }
        assertTrue(p.matches("0"))
        assertTrue(p.matches("7"))
        assertFalse(p.matches("a"))
    }

    @Test
    fun `nonDigit matches non-digit`() {
        val p = pattern { nonDigit() }
        assertTrue(p.matches("a"))
        assertFalse(p.matches("5"))
    }

    @Test
    fun `whitespace matches space and tab`() {
        val p = pattern { whitespace() }
        assertTrue(p.matches(" "))
        assertTrue(p.matches("\t"))
        assertFalse(p.matches("a"))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `space is deprecated alias for whitespace`() {
        val pw = pattern { whitespace() }
        val ps = pattern { space() }
        assertEquals(pw.source, ps.source)
    }

    @Test
    fun `nonWhitespace matches non-whitespace`() {
        val p = pattern { nonWhitespace() }
        assertTrue(p.matches("a"))
        assertFalse(p.matches(" "))
    }

    @Test
    fun `wordChar matches letters digits underscore`() {
        val p = pattern { wordChar() }
        assertTrue(p.matches("a"))
        assertTrue(p.matches("5"))
        assertTrue(p.matches("_"))
        assertFalse(p.matches("-"))
    }

    @Test
    fun `nonWordChar matches punctuation`() {
        val p = pattern { nonWordChar() }
        assertTrue(p.matches("-"))
        assertFalse(p.matches("a"))
    }

    @Test
    fun `anyChar matches any character`() {
        val p = pattern { anyChar() }
        assertTrue(p.matches("a"))
        assertTrue(p.matches("5"))
        assertTrue(p.matches("."))
    }

    @Test
    fun `letter matches ASCII letters only`() {
        val p = pattern { letter() }
        assertTrue(p.matches("a"))
        assertTrue(p.matches("Z"))
        assertFalse(p.matches("5"))
    }

    @Test
    fun `uppercaseLetter matches uppercase only`() {
        val p = pattern { uppercaseLetter() }
        assertTrue(p.matches("A"))
        assertTrue(p.matches("Z"))
        assertFalse(p.matches("a"))
        assertFalse(p.matches("5"))
    }

    @Test
    fun `endPunctuation matches dot exclamation question`() {
        val p = pattern { endPunctuation() }
        assertTrue(p.matches("."))
        assertTrue(p.matches("!"))
        assertTrue(p.matches("?"))
        assertFalse(p.matches(","))
    }

    // ── character classes ────────────────────────────────────────────────────

    @Test
    fun `anyOf matches characters in set`() {
        val p = pattern { anyOf("aeiou") }
        assertTrue(p.matches("a"))
        assertTrue(p.matches("e"))
        assertFalse(p.matches("b"))
    }

    @Test
    fun `anyOf escapes special chars inside class`() {
        val p = pattern { anyOf("]^-\\") }
        assertTrue(p.matches("]"))
        assertTrue(p.matches("^"))
        assertTrue(p.matches("-"))
        assertTrue(p.matches("\\"))
    }

    @Test
    fun `noneOf excludes characters`() {
        val p = pattern { noneOf("aeiou") }
        assertFalse(p.matches("a"))
        assertTrue(p.matches("b"))
    }

    @Test
    fun `inRange matches characters in range`() {
        val p = pattern { inRange('a', 'f') }
        assertTrue(p.matches("a"))
        assertTrue(p.matches("f"))
        assertFalse(p.matches("g"))
        assertFalse(p.matches("A"))
    }

    // ── anchors ──────────────────────────────────────────────────────────────

    @Test
    fun `startOfLine and endOfLine anchor to line boundaries`() {
        val p = kexpresso(RegexOption.MULTILINE) {
            startOfLine()
            literal("Espresso")
            endOfLine()
        }
        assertTrue(p.containsMatchIn("Espresso\nCappuccino"))
        assertFalse(p.containsMatchIn("PrefixEspresso"))
    }

    @Test
    fun `startOfText and endOfText anchor to full input`() {
        val p = pattern { startOfText(); literal("Espresso"); endOfText() }
        assertTrue(p.matches("Espresso"))
        assertFalse(p.containsMatchIn("PrefixEspresso"))
        assertFalse(p.containsMatchIn("EspressoSuffix"))
    }

    @Test
    fun `wordBoundary isolates whole words`() {
        val p = pattern { wordBoundary(); literal("Latte"); wordBoundary() }
        assertTrue(p.containsMatchIn("I love Latte coffee"))
        assertFalse(p.containsMatchIn("ILattecoffee"))
    }

    // ── quantifiers ──────────────────────────────────────────────────────────

    @Test
    fun `optional makes block optional`() {
        val p = pattern { literal("Espresso"); optional { literal("s") } }
        assertTrue(p.matches("Espresso"))
        assertTrue(p.matches("Espressos"))
        assertFalse(p.matches("Espressoss"))
    }

    @Test
    fun `optional lazy quantifier`() {
        val p = pattern { optional(greedy = false) { digit() } }
        assertTrue(p.matches(""))
        assertTrue(p.matches("5"))
    }

    @Test
    fun `zeroOrMore repeats zero or more times`() {
        val p = pattern { zeroOrMore { digit() } }
        assertTrue(p.matches(""))
        assertTrue(p.matches("123"))
        assertFalse(p.matches("12a"))
    }

    @Test
    fun `zeroOrMore lazy quantifier`() {
        val p = pattern { startOfText(); zeroOrMore(greedy = false) { digit() }; endOfText() }
        assertTrue(p.matches(""))
        assertTrue(p.matches("42"))
    }

    @Test
    fun `oneOrMore requires at least one`() {
        val p = pattern { oneOrMore { digit() } }
        assertFalse(p.matches(""))
        assertTrue(p.matches("1"))
        assertTrue(p.matches("123"))
    }

    @Test
    fun `oneOrMore lazy quantifier`() {
        val p = pattern { startOfText(); oneOrMore(greedy = false) { digit() }; endOfText() }
        assertTrue(p.matches("7"))
        assertTrue(p.matches("42"))
    }

    @Test
    fun `exactly repeats n times`() {
        val p = pattern { exactly(3) { digit() } }
        assertTrue(p.matches("123"))
        assertFalse(p.matches("12"))
        assertFalse(p.matches("1234"))
    }

    @Test
    fun `atLeast repeats at least n times`() {
        val p = pattern { atLeast(2) { digit() } }
        assertFalse(p.matches("1"))
        assertTrue(p.matches("12"))
        assertTrue(p.matches("1234567"))
    }

    @Test
    fun `atLeast lazy quantifier`() {
        val p = pattern { startOfText(); atLeast(1, greedy = false) { digit() }; endOfText() }
        assertTrue(p.matches("5"))
        assertTrue(p.matches("55"))
    }

    @Test
    fun `between repeats in range`() {
        val p = pattern { between(2, 4) { digit() } }
        assertFalse(p.matches("1"))
        assertTrue(p.matches("12"))
        assertTrue(p.matches("1234"))
        assertFalse(p.matches("12345"))
    }

    @Test
    fun `between lazy quantifier`() {
        val p = pattern { startOfText(); between(1, 3, greedy = false) { letter() }; endOfText() }
        assertTrue(p.matches("a"))
        assertTrue(p.matches("abc"))
        assertFalse(p.matches("abcd"))
    }

    // ── grouping & alternation ────────────────────────────────────────────────

    @Test
    fun `group wraps in non-capturing group`() {
        val p = pattern { group { literal("Espresso") }; literal("!") }
        assertTrue(p.matches("Espresso!"))
        assertFalse(p.matches("!"))
    }

    @Test
    fun `capture creates capturing group`() {
        val p = pattern { capture { literal("Espresso") } }
        val result = p.find("I love Espresso coffee")
        assertNotNull(result)
        assertEquals("Espresso", result.groupValues[1])
    }

    @Test
    fun `named capture creates named capturing group`() {
        // Use a pattern that anchors after the colon-space so we capture the drink name
        val p = pattern { literal(": "); capture("drink") { word() } }
        val result = p.find("Order: Cappuccino please")
        assertNotNull(result)
        assertEquals("Cappuccino", result.groups["drink"]?.value)
    }

    @Test
    fun `oneOf matches any alternative`() {
        val p = pattern { oneOf({ literal("Espresso") }, { literal("Latte") }, { literal("Cappuccino") }) }
        assertTrue(p.matches("Espresso"))
        assertTrue(p.matches("Latte"))
        assertTrue(p.matches("Cappuccino"))
        assertFalse(p.matches("Americano"))
    }

    @Test
    fun `oneOf with complex blocks`() {
        val p = pattern { oneOf({ digit() }, { letter() }) }
        assertTrue(p.matches("5"))
        assertTrue(p.matches("a"))
        assertFalse(p.matches("_"))
    }

    // ── lookarounds ──────────────────────────────────────────────────────────

    @Test
    fun `followedBy positive lookahead matches digits before ml`() {
        val p = pattern { oneOrMore { digit() }; followedBy { literal("ml") } }
        assertEquals("250", p.find("250ml")?.value)
        assertNull(p.find("250g"))
    }

    @Test
    fun `notFollowedBy negative lookahead matches Espresso not followed by Martini`() {
        val p = pattern { literal("Espresso"); notFollowedBy { literal("Martini") } }
        assertTrue(p.containsMatchIn("Espresso!"))
        assertFalse(p.containsMatchIn("EspressoMartini"))
    }

    @Test
    fun `precededBy positive lookbehind matches digits after dollar sign`() {
        val p = pattern { precededBy { literal("\$") }; oneOrMore { digit() } }
        assertEquals("42", p.find("Total: \$42")?.value)
        assertNull(p.find("Total: 42"))
    }

    @Test
    fun `notPrecededBy negative lookbehind matches digits not after dollar sign`() {
        // A single digit that is NOT preceded by "$" should match
        val p = pattern { notPrecededBy { literal("\$") }; digit() }
        assertNotNull(p.find("Qty: 5"))
        assertEquals("5", p.find("Qty: 5")?.value)
        // A single digit immediately preceded by "$" should NOT match
        assertNull(p.find("\$9"))
    }

    @Test
    fun `notFollowedBy source string is correct`() {
        val p = pattern { notFollowedBy { digit() } }
        assertEquals("(?!\\d)", p.source)
    }

    // ── build options ─────────────────────────────────────────────────────────

    @Test
    fun `build with IGNORE_CASE option`() {
        val p = kexpresso(RegexOption.IGNORE_CASE) { literal("Espresso") }
        assertTrue(p.matches("espresso"))
        assertTrue(p.matches("ESPRESSO"))
        assertTrue(p.matches("Espresso"))
    }

    @Test
    fun `build produces correct source string`() {
        val p = pattern { digit(); letter() }
        assertEquals("\\d[a-zA-Z]", p.source)
    }
}
