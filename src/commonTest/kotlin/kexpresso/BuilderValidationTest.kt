package kexpresso

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Argument validation on [KexpressoBuilder]: invalid arguments must fail fast at the
 * call site with [IllegalArgumentException], not later with an engine-specific regex
 * syntax error (which differs between the JVM and JS — `[^]`, for instance, is invalid
 * on the JVM but matches any character on JS).
 */
class BuilderValidationTest {

    // ── quantifier bounds ─────────────────────────────────────────────────────

    @Test
    fun exactlyRejectsNegativeCount() {
        assertFailsWith<IllegalArgumentException> { kexpresso { exactly(-1) { digit() } } }
    }

    @Test
    fun exactlyZeroIsValid() {
        val p = kexpresso { exactly(0) { digit() } }
        assertTrue(p.matches(""))
    }

    @Test
    fun atLeastRejectsNegativeCount() {
        assertFailsWith<IllegalArgumentException> { kexpresso { atLeast(-1) { digit() } } }
    }

    @Test
    fun betweenRejectsReversedBounds() {
        assertFailsWith<IllegalArgumentException> { kexpresso { between(3, 1) { digit() } } }
    }

    @Test
    fun betweenRejectsNegativeMin() {
        assertFailsWith<IllegalArgumentException> { kexpresso { between(-1, 2) { digit() } } }
    }

    @Test
    fun betweenWithEqualBoundsIsValid() {
        val p = kexpresso { between(2, 2) { digit() } }
        assertTrue(p.matches("42"))
    }

    // ── character classes ─────────────────────────────────────────────────────

    @Test
    fun anyOfRejectsEmptyCharacterSet() {
        assertFailsWith<IllegalArgumentException> { kexpresso { anyOf("") } }
    }

    @Test
    fun noneOfRejectsEmptyCharacterSet() {
        assertFailsWith<IllegalArgumentException> { kexpresso { noneOf("") } }
    }

    @Test
    fun inRangeRejectsReversedBounds() {
        assertFailsWith<IllegalArgumentException> { kexpresso { inRange('z', 'a') } }
    }

    @Test
    fun inRangeWithSingleCharRangeIsValid() {
        val p = kexpresso { inRange('a', 'a') }
        assertTrue(p.matches("a"))
    }

    // ── alternation ───────────────────────────────────────────────────────────

    @Test
    fun oneOfRejectsZeroAlternatives() {
        assertFailsWith<IllegalArgumentException> { kexpresso { oneOf() } }
    }

    @Test
    fun oneOfWithSingleAlternativeIsValid() {
        val p = kexpresso { oneOf({ digit() }) }
        assertTrue(p.matches("7"))
    }
}
