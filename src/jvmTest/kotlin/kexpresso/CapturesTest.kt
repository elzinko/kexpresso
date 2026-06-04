package kexpresso

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for the typed captures API ([Captures] / [MatchResult.captures]).
 *
 * Scenarios are coffee-flavoured where natural.
 */
class CapturesTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Builds a named-group pattern for ISO-date strings: `(?<year>\d{4})-(?<month>\d{2})-(?<day>\d{2})`. */
    private val isoDatePattern = kexpresso {
        capture("year") { exactly(4) { digit() } }
        literal("-")
        capture("month") { exactly(2) { digit() } }
        literal("-")
        capture("day") { exactly(2) { digit() } }
    }

    // ── end-to-end: ISO date parsed into Ints ────────────────────────────────

    @Test
    fun `end-to-end - iso date parts extracted as ints by name`() {
        val caps = isoDatePattern.find("2026-06-03")?.captures
        assertNotNull(caps)
        assertEquals(2026, caps.int("year"))
        assertEquals(6, caps.int("month"))
        assertEquals(3, caps.int("day"))
    }

    @Test
    fun `end-to-end - iso date parts extracted as strings by name`() {
        val caps = isoDatePattern.find("2026-06-03")?.captures
        assertNotNull(caps)
        assertEquals("2026", caps.string("year"))
        assertEquals("06", caps.string("month"))
        assertEquals("03", caps.string("day"))
    }

    @Test
    fun `end-to-end - price extracted as int by index`() {
        // Pattern for a price like "$42": index 0 = whole match, index 1 = the digits
        val pricePattern = kexpresso {
            literal("\$")
            capture { oneOrMore { digit() } }
        }
        val caps = pricePattern.find("\$42")?.captures
        assertNotNull(caps)
        assertEquals("\$42", caps.string(0))  // whole match
        assertEquals(42, caps.int(1))          // first capture group
    }

    // ── by-name: string ──────────────────────────────────────────────────────

    @Test
    fun `string by name - returns value when group matches`() {
        val pattern = kexpresso { capture("drink") { oneOrMore { letter() } } }
        val caps = pattern.find("Espresso")?.captures
        assertEquals("Espresso", caps?.string("drink"))
    }

    @Test
    fun `string by name - returns null for unknown group name`() {
        val pattern = kexpresso { capture("drink") { oneOrMore { letter() } } }
        val caps = pattern.find("Espresso")?.captures
        assertNull(caps?.string("unknown"))
    }

    // ── by-name: int ─────────────────────────────────────────────────────────

    @Test
    fun `int by name - parses valid integer`() {
        val pattern = kexpresso { capture("shots") { oneOrMore { digit() } } }
        val caps = pattern.find("3")?.captures
        assertEquals(3, caps?.int("shots"))
    }

    @Test
    fun `int by name - returns null for absent group`() {
        val pattern = kexpresso { capture("shots") { oneOrMore { digit() } } }
        val caps = pattern.find("3")?.captures
        assertNull(caps?.int("missing"))
    }

    @Test
    fun `int by name - returns null when value is not a number`() {
        val pattern = kexpresso { capture("label") { oneOrMore { letter() } } }
        val caps = pattern.find("Latte")?.captures
        assertNull(caps?.int("label"))
    }

    // ── by-name: long ────────────────────────────────────────────────────────

    @Test
    fun `long by name - parses valid long`() {
        val pattern = kexpresso { capture("ts") { oneOrMore { digit() } } }
        val caps = pattern.find("1748908800000")?.captures
        assertEquals(1748908800000L, caps?.long("ts"))
    }

    @Test
    fun `long by name - returns null for absent group`() {
        val pattern = kexpresso { capture("ts") { oneOrMore { digit() } } }
        val caps = pattern.find("123")?.captures
        assertNull(caps?.long("missing"))
    }

    @Test
    fun `long by name - returns null when value is not a long`() {
        val pattern = kexpresso { capture("label") { oneOrMore { letter() } } }
        val caps = pattern.find("Espresso")?.captures
        assertNull(caps?.long("label"))
    }

    // ── by-name: double ──────────────────────────────────────────────────────

    @Test
    fun `double by name - parses valid double`() {
        val pattern = kexpresso { capture("price") { raw("[0-9.]+") } }
        val caps = pattern.find("3.5")?.captures
        assertEquals(3.5, caps?.double("price"))
    }

    @Test
    fun `double by name - returns null for absent group`() {
        val pattern = kexpresso { capture("price") { raw("[0-9.]+") } }
        val caps = pattern.find("3.5")?.captures
        assertNull(caps?.double("missing"))
    }

    @Test
    fun `double by name - returns null when value is not a double`() {
        val pattern = kexpresso { capture("label") { oneOrMore { letter() } } }
        val caps = pattern.find("Latte")?.captures
        assertNull(caps?.double("label"))
    }

    // ── by-name: boolean ─────────────────────────────────────────────────────

    @Test
    fun `boolean by name - parses true`() {
        val pattern = kexpresso { capture("decaf") { raw("true|false") } }
        val caps = pattern.find("true")?.captures
        assertEquals(true, caps?.boolean("decaf"))
    }

    @Test
    fun `boolean by name - parses false`() {
        val pattern = kexpresso { capture("decaf") { raw("true|false") } }
        val caps = pattern.find("false")?.captures
        assertEquals(false, caps?.boolean("decaf"))
    }

    @Test
    fun `boolean by name - returns null for non-strict value`() {
        val pattern = kexpresso { capture("decaf") { oneOrMore { letter() } } }
        val caps = pattern.find("yes")?.captures
        assertNull(caps?.boolean("decaf"))
    }

    @Test
    fun `boolean by name - returns null for absent group`() {
        val pattern = kexpresso { capture("decaf") { raw("true|false") } }
        val caps = pattern.find("true")?.captures
        assertNull(caps?.boolean("missing"))
    }

    // ── by-index: string ─────────────────────────────────────────────────────

    @Test
    fun `string by index 0 - returns whole match`() {
        val pattern = kexpresso { capture { oneOrMore { letter() } } }
        val caps = pattern.find("Cappuccino")?.captures
        assertEquals("Cappuccino", caps?.string(0))
    }

    @Test
    fun `string by index 1 - returns first capturing group`() {
        val pattern = kexpresso { capture { oneOrMore { letter() } } }
        val caps = pattern.find("Cappuccino")?.captures
        assertEquals("Cappuccino", caps?.string(1))
    }

    @Test
    fun `string by index - returns null for out-of-range index`() {
        val pattern = kexpresso { capture { oneOrMore { letter() } } }
        val caps = pattern.find("Cappuccino")?.captures
        assertNull(caps?.string(99))
    }

    // ── by-index: int ────────────────────────────────────────────────────────

    @Test
    fun `int by index - parses valid integer`() {
        val pattern = kexpresso { capture { oneOrMore { digit() } } }
        val caps = pattern.find("42")?.captures
        assertEquals(42, caps?.int(1))
    }

    @Test
    fun `int by index - returns null for out-of-range`() {
        val pattern = kexpresso { capture { oneOrMore { digit() } } }
        val caps = pattern.find("42")?.captures
        assertNull(caps?.int(99))
    }

    @Test
    fun `int by index - returns null when value is not a number`() {
        val pattern = kexpresso { capture { oneOrMore { letter() } } }
        val caps = pattern.find("Latte")?.captures
        assertNull(caps?.int(1))
    }

    // ── by-index: long ───────────────────────────────────────────────────────

    @Test
    fun `long by index - parses valid long`() {
        val pattern = kexpresso { capture { oneOrMore { digit() } } }
        val caps = pattern.find("1748908800000")?.captures
        assertEquals(1748908800000L, caps?.long(1))
    }

    @Test
    fun `long by index - returns null for out-of-range`() {
        val pattern = kexpresso { capture { oneOrMore { digit() } } }
        val caps = pattern.find("123")?.captures
        assertNull(caps?.long(99))
    }

    @Test
    fun `long by index - returns null when value is not a long`() {
        val pattern = kexpresso { capture { oneOrMore { letter() } } }
        val caps = pattern.find("Latte")?.captures
        assertNull(caps?.long(1))
    }

    // ── by-index: double ─────────────────────────────────────────────────────

    @Test
    fun `double by index - parses valid double`() {
        val pattern = kexpresso { capture { raw("[0-9.]+") } }
        val caps = pattern.find("2.5")?.captures
        assertEquals(2.5, caps?.double(1))
    }

    @Test
    fun `double by index - returns null for out-of-range`() {
        val pattern = kexpresso { capture { raw("[0-9.]+") } }
        val caps = pattern.find("2.5")?.captures
        assertNull(caps?.double(99))
    }

    @Test
    fun `double by index - returns null when value is not a double`() {
        val pattern = kexpresso { capture { oneOrMore { letter() } } }
        val caps = pattern.find("Mocha")?.captures
        assertNull(caps?.double(1))
    }

    // ── by-index: boolean ────────────────────────────────────────────────────

    @Test
    fun `boolean by index - parses true`() {
        val pattern = kexpresso { capture { raw("true|false") } }
        val caps = pattern.find("true")?.captures
        assertEquals(true, caps?.boolean(1))
    }

    @Test
    fun `boolean by index - parses false`() {
        val pattern = kexpresso { capture { raw("true|false") } }
        val caps = pattern.find("false")?.captures
        assertEquals(false, caps?.boolean(1))
    }

    @Test
    fun `boolean by index - returns null for non-strict value`() {
        val pattern = kexpresso { capture { oneOrMore { letter() } } }
        val caps = pattern.find("yes")?.captures
        assertNull(caps?.boolean(1))
    }

    @Test
    fun `boolean by index - returns null for out-of-range`() {
        val pattern = kexpresso { capture { raw("true|false") } }
        val caps = pattern.find("true")?.captures
        assertNull(caps?.boolean(99))
    }

    // ── OrThrow: success ─────────────────────────────────────────────────────

    @Test
    fun `stringOrThrow - returns value when group matches`() {
        val caps = isoDatePattern.find("2026-06-03")?.captures
        assertNotNull(caps)
        assertEquals("2026", caps.stringOrThrow("year"))
    }

    @Test
    fun `intOrThrow - returns parsed int when group matches`() {
        val caps = isoDatePattern.find("2026-06-03")?.captures
        assertNotNull(caps)
        assertEquals(2026, caps.intOrThrow("year"))
    }

    @Test
    fun `longOrThrow - returns parsed long when group matches`() {
        val pattern = kexpresso { capture("ts") { oneOrMore { digit() } } }
        val caps = pattern.find("1748908800000")?.captures
        assertNotNull(caps)
        assertEquals(1748908800000L, caps.longOrThrow("ts"))
    }

    @Test
    fun `doubleOrThrow - returns parsed double when group matches`() {
        val pattern = kexpresso { capture("price") { raw("[0-9.]+") } }
        val caps = pattern.find("4.5")?.captures
        assertNotNull(caps)
        assertEquals(4.5, caps.doubleOrThrow("price"))
    }

    @Test
    fun `booleanOrThrow - returns true when group value is true`() {
        val pattern = kexpresso { capture("decaf") { raw("true|false") } }
        val caps = pattern.find("true")?.captures
        assertNotNull(caps)
        assertEquals(true, caps.booleanOrThrow("decaf"))
    }

    @Test
    fun `booleanOrThrow - returns false when group value is false`() {
        val pattern = kexpresso { capture("decaf") { raw("true|false") } }
        val caps = pattern.find("false")?.captures
        assertNotNull(caps)
        assertEquals(false, caps.booleanOrThrow("decaf"))
    }

    // ── OrThrow: failure on absent group ─────────────────────────────────────

    @Test
    fun `stringOrThrow - throws NoSuchElementException for absent group`() {
        val caps = isoDatePattern.find("2026-06-03")?.captures
        assertNotNull(caps)
        val ex = assertFailsWith<NoSuchElementException> { caps.stringOrThrow("missing") }
        assert(ex.message?.contains("missing") == true)
    }

    @Test
    fun `intOrThrow - throws NoSuchElementException for absent group`() {
        val caps = isoDatePattern.find("2026-06-03")?.captures
        assertNotNull(caps)
        assertFailsWith<NoSuchElementException> { caps.intOrThrow("missing") }
    }

    @Test
    fun `longOrThrow - throws NoSuchElementException for absent group`() {
        val caps = isoDatePattern.find("2026-06-03")?.captures
        assertNotNull(caps)
        assertFailsWith<NoSuchElementException> { caps.longOrThrow("missing") }
    }

    @Test
    fun `doubleOrThrow - throws NoSuchElementException for absent group`() {
        val caps = isoDatePattern.find("2026-06-03")?.captures
        assertNotNull(caps)
        assertFailsWith<NoSuchElementException> { caps.doubleOrThrow("missing") }
    }

    @Test
    fun `booleanOrThrow - throws NoSuchElementException for absent group`() {
        val caps = isoDatePattern.find("2026-06-03")?.captures
        assertNotNull(caps)
        assertFailsWith<NoSuchElementException> { caps.booleanOrThrow("missing") }
    }

    // ── OrThrow: failure on unparseable value ────────────────────────────────

    @Test
    fun `intOrThrow - throws NumberFormatException for non-integer value`() {
        val pattern = kexpresso { capture("label") { oneOrMore { letter() } } }
        val caps = pattern.find("Latte")?.captures
        assertNotNull(caps)
        val ex = assertFailsWith<NumberFormatException> { caps.intOrThrow("label") }
        assert(ex.message?.contains("label") == true)
        assert(ex.message?.contains("Latte") == true)
    }

    @Test
    fun `longOrThrow - throws NumberFormatException for non-long value`() {
        val pattern = kexpresso { capture("label") { oneOrMore { letter() } } }
        val caps = pattern.find("Espresso")?.captures
        assertNotNull(caps)
        val ex = assertFailsWith<NumberFormatException> { caps.longOrThrow("label") }
        assert(ex.message?.contains("label") == true)
        assert(ex.message?.contains("Espresso") == true)
    }

    @Test
    fun `doubleOrThrow - throws NumberFormatException for non-double value`() {
        val pattern = kexpresso { capture("label") { oneOrMore { letter() } } }
        val caps = pattern.find("Mocha")?.captures
        assertNotNull(caps)
        val ex = assertFailsWith<NumberFormatException> { caps.doubleOrThrow("label") }
        assert(ex.message?.contains("label") == true)
        assert(ex.message?.contains("Mocha") == true)
    }

    @Test
    fun `booleanOrThrow - throws IllegalArgumentException for non-boolean value`() {
        val pattern = kexpresso { capture("decaf") { oneOrMore { letter() } } }
        val caps = pattern.find("yes")?.captures
        assertNotNull(caps)
        val ex = assertFailsWith<IllegalArgumentException> { caps.booleanOrThrow("decaf") }
        assert(ex.message?.contains("decaf") == true)
        assert(ex.message?.contains("yes") == true)
    }

    // ── captures extension property ──────────────────────────────────────────

    @Test
    fun `captures extension property is available on MatchResult`() {
        val pattern = kexpresso { capture("roast") { oneOrMore { letter() } } }
        val result = pattern.find("Arabica")
        assertNotNull(result)
        // accessing .captures must not throw
        val caps: Captures = result.captures
        assertEquals("Arabica", caps.string("roast"))
    }

    // ── realistic: parse a coffee menu price line ────────────────────────────

    @Test
    fun `end-to-end - extract drink name and price from menu line`() {
        val menuPattern = kexpresso {
            capture("name") { oneOrMore { letter() } }
            literal(": \$")
            capture("dollars") { oneOrMore { digit() } }
            literal(".")
            capture("cents") { exactly(2) { digit() } }
        }
        val caps = menuPattern.find("Espresso: \$3.50")?.captures
        assertNotNull(caps)
        assertEquals("Espresso", caps.string("name"))
        assertEquals(3, caps.int("dollars"))
        assertEquals(50, caps.int("cents"))
    }
}
