package kexpresso

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for writing-domain helpers defined in [Writing.kt].
 */
class WritingTest {

    // ── sentence ──────────────────────────────────────────────────────────────

    @Test
    fun `sentence matches single-word sentence`() {
        val p = kexpresso { sentence() }
        assertTrue(p.matches("Espresso!"))
        assertTrue(p.matches("Latte."))
        assertTrue(p.matches("Cappuccino?"))
    }

    @Test
    fun `sentence matches multi-word sentence`() {
        val p = kexpresso { sentence() }
        assertTrue(p.matches("Espresso is good."))
        // Note: sentence() requires the first word to have >=2 chars (one capital + one+ from word()),
        // so "I" alone as the first word does not match; use a longer first word.
        assertTrue(p.matches("Latte is delicious!"))
    }

    @Test
    fun `sentence requires capital first letter`() {
        val p = kexpresso { sentence() }
        assertFalse(p.matches("espresso is good."))
    }

    @Test
    fun `sentence requires end punctuation`() {
        val p = kexpresso { sentence() }
        assertFalse(p.matches("Espresso is good"))
    }

    @Test
    fun `sentence does not match empty string`() {
        val p = kexpresso { sentence() }
        assertFalse(p.matches(""))
    }

    @Test
    fun `sentence does not match multiple spaces between words`() {
        val p = kexpresso { sentence() }
        assertFalse(p.matches("Espresso  is  good."))
    }

    // ── paragraph ────────────────────────────────────────────────────────────

    @Test
    fun `paragraph matches single sentence`() {
        val p = kexpresso { paragraph() }
        assertTrue(p.matches("Espresso is perfect!"))
    }

    @Test
    fun `paragraph matches multiple sentences separated by space`() {
        val p = kexpresso { paragraph() }
        assertTrue(p.matches("Latte is smooth. Espresso is strong!"))
        assertTrue(p.matches("Cappuccino rocks. Latte is nice. Espresso is bold."))
    }

    @Test
    fun `paragraph requires capital first letter`() {
        val p = kexpresso { paragraph() }
        assertFalse(p.matches("espresso is good."))
    }

    @Test
    fun `paragraph requires terminal punctuation`() {
        val p = kexpresso { paragraph() }
        assertFalse(p.matches("Espresso is good"))
    }

    // ── end-to-end integration tests ──────────────────────────────────────────

    @Test
    fun `validate email end-to-end`() {
        val emailPattern = kexpresso {
            startOfText()
            email()
            endOfText()
        }
        assertTrue(emailPattern.matches("barista@coffee.shop"))
        assertFalse(emailPattern.matches("not-an-email"))
        assertFalse(emailPattern.matches("barista@coffee.shop extra"))
    }

    @Test
    fun `extract all words from coffee order via findAll`() {
        val wordPattern = kexpresso { word() }
        val order = "Espresso Latte Cappuccino"
        val drinks = wordPattern.findAll(order).map { it.value }.toList()
        assertTrue(drinks.contains("Espresso"))
        assertTrue(drinks.contains("Latte"))
        assertTrue(drinks.contains("Cappuccino"))
    }

    @Test
    fun `match full sentence end-to-end`() {
        val sentencePattern = kexpresso { sentence() }
        assertTrue(sentencePattern.matches("Espresso is perfect!"))
        assertFalse(sentencePattern.matches("espresso is lowercase."))
        assertFalse(sentencePattern.matches("No punctuation at end"))
    }

    @Test
    fun `match paragraph end-to-end`() {
        val paragraphPattern = kexpresso { paragraph() }
        assertTrue(paragraphPattern.matches("Latte is smooth. Espresso is bold!"))
        assertFalse(paragraphPattern.matches("latte is smooth. espresso is bold!"))
    }
}
