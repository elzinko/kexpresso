package kexpresso

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the [KexpressoPattern] result type: surface API, equality, and options.
 */
class KexpressoPatternTest {

    private fun coffeePattern() = kexpresso { oneOf({ literal("Espresso") }, { literal("Latte") }) }

    // ── source and toString ───────────────────────────────────────────────────

    @Test
    fun `source returns raw regex string`() {
        val p = kexpresso { digit() }
        assertEquals("\\d", p.source)
    }

    @Test
    fun `toString returns source`() {
        val p = kexpresso { letter() }
        assertEquals(p.source, p.toString())
    }

    // ── matches ───────────────────────────────────────────────────────────────

    @Test
    fun `matches returns true for full match`() {
        val p = coffeePattern()
        assertTrue(p.matches("Espresso"))
        assertTrue(p.matches("Latte"))
    }

    @Test
    fun `matches returns false for partial input`() {
        val p = coffeePattern()
        assertFalse(p.matches("EspressoExtra"))
    }

    // ── containsMatchIn ───────────────────────────────────────────────────────

    @Test
    fun `containsMatchIn finds match anywhere`() {
        val p = coffeePattern()
        assertTrue(p.containsMatchIn("I love Espresso coffee"))
        assertFalse(p.containsMatchIn("I love Americano"))
    }

    // ── find ──────────────────────────────────────────────────────────────────

    @Test
    fun `find returns first match`() {
        val p = kexpresso { oneOrMore { letter() } }
        val result = p.find("Espresso is good")
        assertNotNull(result)
        assertEquals("Espresso", result.value)
    }

    @Test
    fun `find returns null when no match`() {
        val p = kexpresso { digit() }
        assertNull(p.find("no digits here"))
    }

    @Test
    fun `find with startIndex skips earlier matches`() {
        val p = kexpresso { oneOrMore { letter() } }
        val result = p.find("Espresso Latte", startIndex = 9)
        assertNotNull(result)
        assertEquals("Latte", result.value)
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    fun `findAll returns all matches as sequence`() {
        val p = kexpresso { oneOrMore { letter() } }
        val matches = p.findAll("Espresso Latte Cappuccino").map { it.value }.toList()
        assertEquals(listOf("Espresso", "Latte", "Cappuccino"), matches)
    }

    @Test
    fun `findAll returns empty sequence when no matches`() {
        val p = kexpresso { digit() }
        val matches = p.findAll("no digits").toList()
        assertTrue(matches.isEmpty())
    }

    // ── toRegex and toPattern ─────────────────────────────────────────────────

    @Test
    fun `toRegex returns equivalent Regex`() {
        val p = kexpresso { literal("Cappuccino") }
        val regex = p.toRegex()
        assertTrue(regex.matches("Cappuccino"))
    }

    @Test
    fun `toPattern returns equivalent java Pattern`() {
        val p = kexpresso { literal("Macchiato") }
        val javaPattern = p.toPattern()
        assertTrue(javaPattern.matcher("Macchiato").matches())
    }

    // ── options ───────────────────────────────────────────────────────────────

    @Test
    fun `options set reflects supplied RegexOptions`() {
        val p = kexpresso(RegexOption.IGNORE_CASE, RegexOption.MULTILINE) { digit() }
        assertTrue(RegexOption.IGNORE_CASE in p.options)
        assertTrue(RegexOption.MULTILINE in p.options)
    }

    @Test
    fun `IGNORE_CASE option makes match case-insensitive`() {
        val p = kexpresso(RegexOption.IGNORE_CASE) { literal("espresso") }
        assertTrue(p.matches("ESPRESSO"))
        assertTrue(p.matches("Espresso"))
    }

    // ── equals and hashCode ───────────────────────────────────────────────────

    @Test
    fun `equal patterns with same source and options are equal`() {
        val p1 = kexpresso { digit() }
        val p2 = kexpresso { digit() }
        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
    }

    @Test
    fun `patterns with different sources are not equal`() {
        val p1 = kexpresso { digit() }
        val p2 = kexpresso { letter() }
        assertFalse(p1 == p2)
    }

    @Test
    fun `patterns with different options are not equal`() {
        val p1 = kexpresso { digit() }
        val p2 = kexpresso(RegexOption.IGNORE_CASE) { digit() }
        assertFalse(p1 == p2)
    }

    // ── object entry point ────────────────────────────────────────────────────

    @Test
    fun `Kexpresso object pattern entry point works`() {
        val p = Kexpresso.pattern { literal("Ristretto") }
        assertTrue(p.matches("Ristretto"))
        assertFalse(p.matches("ristretto"))
    }

    @Test
    fun `Kexpresso object pattern accepts options`() {
        val p = Kexpresso.pattern(RegexOption.IGNORE_CASE) { literal("Ristretto") }
        assertTrue(p.matches("ristretto"))
    }
}
