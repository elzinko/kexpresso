package kexpresso

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for text-domain helpers defined in [Text.kt].
 */
class TextTest {

    // ── word ──────────────────────────────────────────────────────────────────

    @Test
    fun `word matches alphanumeric sequences`() {
        val p = kexpresso { word() }
        assertTrue(p.matches("Espresso"))
        assertTrue(p.matches("Cappuccino42"))
        assertTrue(p.matches("123"))
    }

    @Test
    fun `word does not match empty string`() {
        val p = kexpresso { word() }
        assertFalse(p.matches(""))
    }

    @Test
    fun `word does not match hyphen or underscore`() {
        val p = kexpresso { word() }
        assertFalse(p.matches("cold-brew"))
        assertFalse(p.matches("cold_brew"))
    }

    // ── pseudo ────────────────────────────────────────────────────────────────

    @Test
    fun `pseudo matches alphanumeric with hyphens and underscores`() {
        val p = kexpresso { pseudo() }
        assertTrue(p.matches("cold-brew"))
        assertTrue(p.matches("cold_brew"))
        assertTrue(p.matches("Espresso"))
        assertTrue(p.matches("drink_2024"))
    }

    @Test
    fun `pseudo does not match empty string`() {
        val p = kexpresso { pseudo() }
        assertFalse(p.matches(""))
    }

    @Test
    fun `pseudo does not match spaces`() {
        val p = kexpresso { pseudo() }
        assertFalse(p.matches("cold brew"))
    }

    // ── email ─────────────────────────────────────────────────────────────────

    @Test
    fun `email matches typical email addresses`() {
        val p = kexpresso { email() }
        assertTrue(p.matches("barista@coffee.shop"))
        assertTrue(p.matches("user.name+tag@example.co.uk"))
        assertTrue(p.matches("espresso123@latte.com"))
    }

    @Test
    fun `email does not match missing at-sign`() {
        val p = kexpresso { email() }
        assertFalse(p.matches("notanemail.com"))
    }

    @Test
    fun `email does not match missing domain`() {
        val p = kexpresso { email() }
        assertFalse(p.matches("user@"))
    }

    @Test
    fun `email is found within a sentence`() {
        val p = kexpresso { email() }
        assertTrue(p.containsMatchIn("Contact us at barista@coffee.shop for reservations"))
    }

    @Test
    fun `email extracted via findAll from text`() {
        val p = kexpresso { email() }
        val emails = p.findAll("espresso@cafe.com and latte@bistro.org").map { it.value }.toList()
        assertTrue(emails.contains("espresso@cafe.com"))
        assertTrue(emails.contains("latte@bistro.org"))
    }

    // ── url ───────────────────────────────────────────────────────────────────

    @Test
    fun `url matches http and https URLs`() {
        val p = kexpresso { url() }
        assertTrue(p.containsMatchIn("http://example.com"))
        assertTrue(p.containsMatchIn("https://coffee.shop/menu?drink=espresso"))
    }

    @Test
    fun `url does not match plain domain without scheme`() {
        val p = kexpresso { url() }
        assertFalse(p.containsMatchIn("example.com"))
    }

    @Test
    fun `url found within longer text`() {
        val p = kexpresso { url() }
        assertTrue(p.containsMatchIn("Visit us at https://latte.cafe for more info"))
    }
}
