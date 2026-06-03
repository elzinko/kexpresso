package kexpresso

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the 0.2.0 additions:
 * - DSL completions: [KexpressoBuilder.nonWordBoundary], [KexpressoBuilder.lowercaseLetter],
 *   [KexpressoBuilder.alphanumeric], [KexpressoBuilder.tab], [KexpressoBuilder.newline],
 *   [KexpressoBuilder.carriageReturn]
 * - Composition & escape hatch: [KexpressoBuilder.raw], [KexpressoBuilder.backreference],
 *   [KexpressoBuilder.include]
 * - Name validation on [KexpressoBuilder.capture] and [KexpressoBuilder.backreference]
 * - String operations on [KexpressoPattern]: [KexpressoPattern.replaceFirst],
 *   [KexpressoPattern.replaceAll], [KexpressoPattern.split], [KexpressoPattern.matchEntire]
 */
class KexpressoCompletionsTest {

    private fun pattern(block: KexpressoBuilder.() -> Unit) = kexpresso(block = block)

    // ── DSL completions — primitives / anchors ────────────────────────────────

    @Test
    fun `nonWordBoundary produces correct source`() {
        val p = pattern { nonWordBoundary() }
        assertEquals("\\B", p.source)
    }

    @Test
    fun `nonWordBoundary matches inside a word`() {
        // \B matches positions that are NOT at a word boundary.
        // In "latte", the position between 'l' and 'a' is inside a word → \B matches there.
        val p = pattern { literal("l"); nonWordBoundary(); literal("a") }
        assertTrue(p.containsMatchIn("latte"))
    }

    @Test
    fun `nonWordBoundary does not match at word boundary`() {
        // Between "latte" and " " (space) is a word boundary → \B should not match there.
        val p = pattern { literal("e"); nonWordBoundary(); literal(" ") }
        assertFalse(p.containsMatchIn("latte mocha"))
    }

    @Test
    fun `lowercaseLetter matches a-z`() {
        val p = pattern { lowercaseLetter() }
        assertTrue(p.matches("a"))
        assertTrue(p.matches("z"))
        assertFalse(p.matches("A"))
        assertFalse(p.matches("5"))
    }

    @Test
    fun `lowercaseLetter produces correct source`() {
        val p = pattern { lowercaseLetter() }
        assertEquals("[a-z]", p.source)
    }

    @Test
    fun `alphanumeric matches letters and digits`() {
        val p = pattern { alphanumeric() }
        assertTrue(p.matches("a"))
        assertTrue(p.matches("Z"))
        assertTrue(p.matches("5"))
        assertFalse(p.matches("_"))
        assertFalse(p.matches("-"))
    }

    @Test
    fun `alphanumeric produces correct source`() {
        val p = pattern { alphanumeric() }
        assertEquals("[a-zA-Z0-9]", p.source)
    }

    @Test
    fun `tab matches horizontal tab character`() {
        val p = pattern { tab() }
        assertTrue(p.matches("\t"))
        assertFalse(p.matches(" "))
    }

    @Test
    fun `tab produces correct source`() {
        val p = pattern { tab() }
        assertEquals("\\t", p.source)
    }

    @Test
    fun `newline matches newline character`() {
        val p = pattern { newline() }
        assertTrue(p.matches("\n"))
        assertFalse(p.matches(" "))
    }

    @Test
    fun `newline produces correct source`() {
        val p = pattern { newline() }
        assertEquals("\\n", p.source)
    }

    @Test
    fun `carriageReturn matches carriage-return character`() {
        val p = pattern { carriageReturn() }
        assertTrue(p.matches("\r"))
        assertFalse(p.matches("\n"))
    }

    @Test
    fun `carriageReturn produces correct source`() {
        val p = pattern { carriageReturn() }
        assertEquals("\\r", p.source)
    }

    // ── raw (escape hatch) ────────────────────────────────────────────────────

    @Test
    fun `raw inserts verbatim regex fragment`() {
        val p = pattern { raw("\\d{4}-\\d{2}-\\d{2}") }
        assertTrue(p.matches("2026-06-03"))
        assertFalse(p.matches("26-6-3"))
    }

    @Test
    fun `raw produces correct source`() {
        val p = pattern { raw("[abc]+") }
        assertEquals("[abc]+", p.source)
    }

    @Test
    fun `raw combined with typed methods`() {
        val p = pattern { raw("\\d+"); literal("-"); oneOrMore { letter() } }
        assertTrue(p.matches("250-ml"))
        assertFalse(p.matches("250"))
    }

    // ── backreference (numeric) ───────────────────────────────────────────────

    @Test
    fun `backreference by index produces correct source`() {
        val p = pattern { capture { digit() }; backreference(1) }
        assertEquals("(\\d)\\1", p.source)
    }

    @Test
    fun `backreference by index matches repeated digit`() {
        val p = pattern { capture { digit() }; backreference(1) }
        assertTrue(p.matches("55"))
        assertFalse(p.matches("56"))
    }

    @Test
    fun `backreference numeric detects repeated word`() {
        // Coffee-themed: detect a duplicated drink keyword
        val p = pattern { capture { oneOrMore { wordChar() } }; whitespace(); backreference(1) }
        assertTrue(p.containsMatchIn("latte latte"))
        assertFalse(p.containsMatchIn("latte mocha"))
    }

    @Test
    fun `backreference by index throws for zero`() {
        assertFailsWith<IllegalArgumentException> {
            pattern { backreference(0) }
        }
    }

    @Test
    fun `backreference by index throws for negative`() {
        assertFailsWith<IllegalArgumentException> {
            pattern { backreference(-1) }
        }
    }

    // ── backreference (named) ─────────────────────────────────────────────────

    @Test
    fun `backreference by name produces correct source`() {
        val p = pattern { capture("drink") { letter() }; backreference("drink") }
        assertEquals("(?<drink>[a-zA-Z])\\k<drink>", p.source)
    }

    @Test
    fun `backreference by name matches repeated drink`() {
        val p = pattern {
            capture("drink") { oneOrMore { letter() } }
            whitespace()
            backreference("drink")
        }
        assertTrue(p.containsMatchIn("Latte Latte"))
        assertFalse(p.containsMatchIn("Latte Mocha"))
    }

    @Test
    fun `backreference by name throws for invalid name starting with digit`() {
        assertFailsWith<IllegalArgumentException> {
            pattern { backreference("1drink") }
        }
    }

    @Test
    fun `backreference by name throws for name with space`() {
        assertFailsWith<IllegalArgumentException> {
            pattern { backreference("my drink") }
        }
    }

    @Test
    fun `backreference by name throws for empty name`() {
        assertFailsWith<IllegalArgumentException> {
            pattern { backreference("") }
        }
    }

    // ── capture name validation ───────────────────────────────────────────────

    @Test
    fun `capture with invalid name starting with digit throws`() {
        assertFailsWith<IllegalArgumentException> {
            pattern { capture("1bad") { digit() } }
        }
    }

    @Test
    fun `capture with name containing space throws`() {
        assertFailsWith<IllegalArgumentException> {
            pattern { capture("bad name") { digit() } }
        }
    }

    @Test
    fun `capture with valid name including digit succeeds`() {
        // Note: JVM regex does not allow underscores in named group names
        val p = pattern { capture("drink1") { letter() } }
        assertEquals("(?<drink1>[a-zA-Z])", p.source)
    }

    @Test
    fun `capture with name containing underscore throws due to JVM limitation`() {
        assertFailsWith<IllegalArgumentException> {
            pattern { capture("drink_1") { letter() } }
        }
    }

    // ── include (composition) ─────────────────────────────────────────────────

    @Test
    fun `include wraps sub-pattern in non-capturing group`() {
        val sub = kexpresso { digit() }
        val p = pattern { include(sub) }
        assertEquals("(?:\\d)", p.source)
    }

    @Test
    fun `include embeds sub-pattern correctly`() {
        val octet = kexpresso { between(1, 3) { digit() } }
        val ip = kexpresso {
            include(octet)
            exactly(3) { char('.'); include(octet) }
        }
        assertTrue(ip.matches("192.168.1.1"))
        assertTrue(ip.matches("10.0.0.1"))
        assertFalse(ip.matches("192.168.1"))
    }

    @Test
    fun `include allows sub-pattern reuse`() {
        val word = kexpresso { oneOrMore { letter() } }
        val p = kexpresso { include(word); whitespace(); include(word) }
        assertTrue(p.matches("Espresso Latte"))
        assertFalse(p.matches("Espresso"))
    }

    // ── KexpressoPattern string operations ────────────────────────────────────

    @Test
    fun `replaceFirst replaces only the first match`() {
        val p = kexpresso { oneOrMore { letter() } }
        val result = p.replaceFirst("Espresso Latte Cappuccino", "Americano")
        assertEquals("Americano Latte Cappuccino", result)
    }

    @Test
    fun `replaceFirst with no match returns original string`() {
        val p = kexpresso { digit() }
        val result = p.replaceFirst("Espresso", "X")
        assertEquals("Espresso", result)
    }

    @Test
    fun `replaceAll with string replacement replaces all matches`() {
        val p = kexpresso { oneOrMore { letter() } }
        val result = p.replaceAll("Espresso Latte Cappuccino", "Brew")
        assertEquals("Brew Brew Brew", result)
    }

    @Test
    fun `replaceAll with transform uppercases every drink`() {
        val p = kexpresso { oneOrMore { letter() } }
        val result = p.replaceAll("espresso latte") { it.value.uppercase() }
        assertEquals("ESPRESSO LATTE", result)
    }

    @Test
    fun `replaceAll with transform can prefix match values`() {
        val p = kexpresso { oneOrMore { digit() } }
        val result = p.replaceAll("1 shot, 2 pumps") { "x${it.value}" }
        assertEquals("x1 shot, x2 pumps", result)
    }

    @Test
    fun `split splits on separator pattern`() {
        val p = kexpresso { literal(", ") }
        val parts = p.split("Espresso, Latte, Cappuccino")
        assertEquals(listOf("Espresso", "Latte", "Cappuccino"), parts)
    }

    @Test
    fun `split with limit caps the result`() {
        val p = kexpresso { literal(", ") }
        val parts = p.split("Espresso, Latte, Cappuccino", limit = 2)
        assertEquals(listOf("Espresso", "Latte, Cappuccino"), parts)
    }

    @Test
    fun `split with no match returns single-element list`() {
        val p = kexpresso { literal("; ") }
        val parts = p.split("Espresso")
        assertEquals(listOf("Espresso"), parts)
    }

    @Test
    fun `matchEntire returns match result for full match`() {
        val p = kexpresso { oneOrMore { letter() } }
        val result = p.matchEntire("Espresso")
        assertNotNull(result)
        assertEquals("Espresso", result.value)
    }

    @Test
    fun `matchEntire returns null for partial string`() {
        val p = kexpresso { oneOrMore { letter() } }
        assertNull(p.matchEntire("Espresso42"))
    }

    @Test
    fun `matchEntire returns null when no match`() {
        val p = kexpresso { digit() }
        assertNull(p.matchEntire("Espresso"))
    }
}
