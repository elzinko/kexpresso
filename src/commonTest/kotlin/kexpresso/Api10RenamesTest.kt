package kexpresso

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Behavioural-equivalence tests for the four pre-1.0 renames:
 *
 * | Old (deprecated) | New (canonical) |
 * | ---------------- | --------------- |
 * | `pseudo()`       | `handle()`      |
 * | `capitalLetter()`| `uppercaseLetter()` |
 * | `space()`        | `whitespace()` (no rename, just deprecate) |
 *
 * The old names must keep matching identically until they are removed in 1.0;
 * the new names are the canonical replacements. These tests guarantee that
 * downstream users on the deprecated names see a compiler warning but never a
 * runtime regression.
 */
@Suppress("DEPRECATION")
class Api10RenamesTest {

    // ── handle() ↔ pseudo() ───────────────────────────────────────────────────

    @Test
    fun handle_matches_alphanumerics_underscores_and_hyphens() {
        val pattern = kexpresso { handle() }
        assertTrue(pattern.matches("cold-brew_2024"))
        assertTrue(pattern.matches("octo-cat"))
        assertTrue(pattern.matches("barista_42"))
        assertFalse(pattern.matches("has space"))
        assertFalse(pattern.matches("with.dot"))
    }

    @Test
    fun pseudo_remains_behaviourally_identical_to_handle() {
        val old = kexpresso { pseudo() }
        val new = kexpresso { handle() }
        assertEquals(old.source, new.source)
        for (input in listOf("cold-brew_2024", "octo-cat", "barista_42", "with.dot", "")) {
            assertEquals(new.matches(input), old.matches(input), "matches diverged on '$input'")
        }
    }

    // ── uppercaseLetter() ↔ capitalLetter() ──────────────────────────────────

    @Test
    fun uppercaseLetter_matches_a_to_z_uppercase() {
        val pattern = kexpresso { uppercaseLetter() }
        assertTrue(pattern.matches("A"))
        assertTrue(pattern.matches("M"))
        assertFalse(pattern.matches("a"))
        assertFalse(pattern.matches("1"))
        assertFalse(pattern.matches("é"))
    }

    @Test
    fun capitalLetter_remains_behaviourally_identical_to_uppercaseLetter() {
        val old = kexpresso { capitalLetter() }
        val new = kexpresso { uppercaseLetter() }
        assertEquals(old.source, new.source)
        for (input in listOf("A", "Z", "a", "1", "")) {
            assertEquals(new.matches(input), old.matches(input), "matches diverged on '$input'")
        }
    }

    // ── space() ↔ whitespace() ───────────────────────────────────────────────

    @Test
    fun space_remains_behaviourally_identical_to_whitespace() {
        val old = kexpresso { space() }
        val new = kexpresso { whitespace() }
        assertEquals(old.source, new.source)
        for (input in listOf(" ", "\t", "\n", "x", "")) {
            assertEquals(new.matches(input), old.matches(input), "matches diverged on '${input.toCharArray().contentToString()}'")
        }
    }

    // ── sentence() still works after switching internal calls ────────────────

    @Test
    fun sentence_still_matches_after_internal_rename() {
        val pattern = kexpresso { sentence() }
        assertTrue(pattern.matches("Espresso is perfect!"))
        assertTrue(pattern.matches("Latte."))
        assertTrue(pattern.matches("Why coffee?"))
        assertFalse(pattern.matches("espresso is perfect!")) // lowercase start
        assertFalse(pattern.matches("Espresso is perfect"))  // missing terminator
    }

    @Test
    fun paragraph_still_matches_after_internal_rename() {
        val pattern = kexpresso { paragraph() }
        assertTrue(pattern.matches("Latte is smooth. Espresso is strong!"))
        assertTrue(pattern.matches("Coffee."))
    }
}
