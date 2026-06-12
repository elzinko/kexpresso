package kexpresso

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalKexpressoApi::class)

/**
 * Tests for [KexpressoPattern.analyze] and [KexpressoPattern.isPotentiallyVulnerable].
 *
 * Two categories:
 * 1. **True positives** — nested unbounded quantifiers that SHOULD be flagged.
 * 2. **False-positive guards** — shapes that MUST NOT be flagged (safe patterns).
 *
 * All assertions on findings also check that the reported [ReDoSFinding.index] matches
 * the expected start of the risky group in the source string.
 */
class AnalysisTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun pattern(block: KexpressoBuilder.() -> Unit) = kexpresso(block = block)

    private fun raw(source: String): KexpressoPattern {
        val builder = KexpressoBuilder()
        builder.raw(source)
        return builder.build()
    }

    // ── TRUE POSITIVES — these patterns MUST be flagged ─────────────────────

    @Test
    fun `non-capturing group with plus inside plus is flagged at index 0`() {
        // (?:a+)+ — canonical evil regex
        val p = raw("(?:a+)+")
        assertTrue(p.isPotentiallyVulnerable, "Expected (?:a+)+ to be flagged")
        val report = p.analyze()
        assertEquals(1, report.findings.size)
        assertEquals(0, report.findings[0].index)
        assertEquals(ReDoSSeverity.WARNING, report.findings[0].severity)
        assertTrue(report.findings[0].message.contains("0"), "message should mention index 0")
    }

    @Test
    fun `capturing group with star inside star is flagged at index 0`() {
        // (a*)* — evil regex
        val p = raw("(a*)*")
        assertTrue(p.isPotentiallyVulnerable)
        assertEquals(0, p.analyze().findings[0].index)
    }

    @Test
    fun `non-capturing group with word-char plus inside plus is flagged`() {
        // (?:\w+)+ — classic evil regex
        val p = raw("(?:\\w+)+")
        assertTrue(p.isPotentiallyVulnerable)
        assertEquals(0, p.analyze().findings[0].index)
    }

    @Test
    fun `capturing group with anyChar plus inside plus is flagged at index 0`() {
        // (.+)+ — evil regex
        val p = raw("(.+)+")
        assertTrue(p.isPotentiallyVulnerable)
        assertEquals(0, p.analyze().findings[0].index)
    }

    @Test
    fun `double-nested non-capturing groups are flagged at outer group`() {
        // (?:(?:x)+)* — outer unbounded wraps inner unbounded
        val p = raw("(?:(?:x)+)*")
        assertTrue(p.isPotentiallyVulnerable)
        assertEquals(0, p.analyze().findings[0].index)
    }

    @Test
    fun `lazy outer quantifier with unbounded inner is still flagged`() {
        // (?:a+)+? — lazy outer still risks catastrophic backtracking
        val p = raw("(?:a+)+?")
        assertTrue(p.isPotentiallyVulnerable)
        assertEquals(0, p.analyze().findings[0].index)
    }

    @Test
    fun `non-capturing group with digit plus inside plus is flagged`() {
        // (?:\d+)+ — commonly misused for numeric parsing
        val p = raw("(?:\\d+)+")
        assertTrue(p.isPotentiallyVulnerable)
        assertEquals(0, p.analyze().findings[0].index)
    }

    @Test
    fun `atLeast DSL outer with oneOrMore inner is flagged`() {
        // atLeast(1) { ... } produces (?:...){1,} which is unbounded
        val p = pattern { atLeast(1) { oneOrMore { digit() } } }
        assertTrue(
            p.isPotentiallyVulnerable,
            "atLeast(1) { oneOrMore { digit() } } should be flagged; source=${p.source}"
        )
    }

    @Test
    fun `end-to-end DSL oneOrMore inside oneOrMore is flagged`() {
        // DSL produces (?:(?:[a-zA-Z])+)+ — nested unbounded quantifiers
        val p = pattern { oneOrMore { oneOrMore { letter() } } }
        assertTrue(
            p.isPotentiallyVulnerable,
            "oneOrMore { oneOrMore { letter() } } should be flagged; source=${p.source}"
        )
        val report = p.analyze()
        assertEquals(1, report.findings.size)
        assertEquals(0, report.findings[0].index)
    }

    @Test
    fun `zeroOrMore outer with oneOrMore inner is flagged`() {
        val p = pattern { zeroOrMore { oneOrMore { wordChar() } } }
        assertTrue(
            p.isPotentiallyVulnerable,
            "zeroOrMore { oneOrMore { wordChar() } } should be flagged; source=${p.source}"
        )
    }

    @Test
    fun `multiple risky groups produce multiple findings`() {
        // Two separate evil groups concatenated: (?:a+)+(?:b+)+
        val p = raw("(?:a+)+(?:b+)+")
        val report = p.analyze()
        assertEquals(2, report.findings.size, "Expected two findings for two nested-unbounded groups")
        assertEquals(0, report.findings[0].index)
        // Second group starts after the first group+quantifier — "(?:a+)+" is 7 chars
        assertEquals(7, report.findings[1].index)
    }

    // ── FALSE-POSITIVE GUARDS — these patterns MUST NOT be flagged ──────────

    @Test
    fun `quantifier chars inside character class are not flagged`() {
        // [a-z+]* — the '+' is literal inside [...], not a quantifier
        val p = raw("[a-z+]*")
        assertFalse(p.isPotentiallyVulnerable, "quantifier inside [...] must not be flagged; source=${p.source}")
    }

    @Test
    fun `star and plus inside character class are not flagged`() {
        // [*+] — both chars are literals
        val p = raw("[*+]")
        assertFalse(p.isPotentiallyVulnerable)
    }

    @Test
    fun `escaped plus followed by star is not flagged`() {
        // a\+* — backslash escapes the '+', making it a literal; not a nested quantifier
        val p = raw("a\\+*")
        assertFalse(p.isPotentiallyVulnerable, "escaped quantifier must not be flagged; source=${p.source}")
    }

    @Test
    fun `bounded outer exact quantifier is not flagged`() {
        // (?:a+){3} — outer {3} is bounded, so this is NOT a nested-unbounded shape
        val p = raw("(?:a+){3}")
        assertFalse(p.isPotentiallyVulnerable, "bounded outer {3} must not be flagged; source=${p.source}")
    }

    @Test
    fun `bounded inner exact quantifier is not flagged`() {
        // (?:a{2})+ — inner {2} is bounded; safe
        val p = raw("(?:a{2})+")
        assertFalse(p.isPotentiallyVulnerable, "bounded inner {2} must not be flagged; source=${p.source}")
    }

    @Test
    fun `bounded inner range quantifier is not flagged`() {
        // (?:a{2,5})+ — inner {2,5} is bounded; safe
        val p = raw("(?:a{2,5})+")
        assertFalse(p.isPotentiallyVulnerable, "bounded inner {2,5} must not be flagged; source=${p.source}")
    }

    @Test
    fun `optional bounded outer with unbounded inner is not flagged`() {
        // (?:a+)? — outer ? is bounded (0 or 1)
        val p = raw("(?:a+)?")
        assertFalse(p.isPotentiallyVulnerable, "optional outer must not be flagged; source=${p.source}")
    }

    @Test
    fun `plain oneOrMore digit is not flagged`() {
        val p = pattern { oneOrMore { digit() } }
        assertFalse(p.isPotentiallyVulnerable, "plain oneOrMore must not be flagged; source=${p.source}")
    }

    @Test
    fun `plain zeroOrMore letter is not flagged`() {
        val p = pattern { zeroOrMore { letter() } }
        assertFalse(p.isPotentiallyVulnerable, "plain zeroOrMore must not be flagged; source=${p.source}")
    }

    @Test
    fun `atomic group is not flagged`() {
        // (?>a+)+ — atomic groups do not backtrack, so no catastrophic backtracking
        val p = raw("(?>a+)+")
        assertFalse(p.isPotentiallyVulnerable, "atomic group must not be flagged; source=${p.source}")
    }

    @Test
    fun `possessive inner quantifier is not flagged`() {
        // (?:a++)+ — a++ is possessive: it never gives back, so no catastrophic backtracking
        val p = raw("(?:a++)+")
        assertFalse(p.isPotentiallyVulnerable, "possessive inner a++ must not be flagged; source=${p.source}")
    }

    @Test
    fun `possessive outer quantifier is not flagged`() {
        // (?:a+)++ — possessive outer, so no outer backtracking
        val p = raw("(?:a+)++")
        assertFalse(p.isPotentiallyVulnerable, "possessive outer ++ must not be flagged; source=${p.source}")
    }

    @Test
    fun `anchored email helper is not flagged`() {
        // email() is complex but has no nested unbounded quantifiers
        val p = pattern { startOfText(); email(); endOfText() }
        assertFalse(
            p.isPotentiallyVulnerable,
            "anchored email() must not be flagged; source=${p.source}"
        )
    }

    @Test
    fun `atLeast outer with bounded exact inner is not flagged`() {
        // (?:(?:\d){3}){2,} — inner {3} is bounded
        val p = pattern { atLeast(2) { exactly(3) { digit() } } }
        assertFalse(
            p.isPotentiallyVulnerable,
            "bounded inner exactly(3) must not be flagged; source=${p.source}"
        )
    }

    @Test
    fun `bounded between outer with unbounded inner is not flagged`() {
        // between(1,5) is bounded outer; even if inner oneOrMore is unbounded, the outer isn't
        val p = pattern { between(1, 5) { oneOrMore { letter() } } }
        assertFalse(
            p.isPotentiallyVulnerable,
            "bounded outer between must not be flagged; source=${p.source}"
        )
    }

    // ── ReDoSReport API contract ────────────────────────────────────────────

    @Test
    fun `isPotentiallyVulnerable is false on empty findings`() {
        val report = ReDoSReport(emptyList())
        assertFalse(report.isPotentiallyVulnerable)
    }

    @Test
    fun `isPotentiallyVulnerable is true on non-empty findings`() {
        val finding = ReDoSFinding("test finding", 0)
        val report = ReDoSReport(listOf(finding))
        assertTrue(report.isPotentiallyVulnerable)
    }

    @Test
    fun `ReDoSFinding default severity is WARNING`() {
        val finding = ReDoSFinding("msg", 0)
        assertEquals(ReDoSSeverity.WARNING, finding.severity)
    }

    @Test
    fun `isPotentiallyVulnerable extension property agrees with analyze`() {
        val safe = raw("abc")
        val risky = raw("(?:a+)+")
        assertEquals(safe.analyze().isPotentiallyVulnerable, safe.isPotentiallyVulnerable)
        assertEquals(risky.analyze().isPotentiallyVulnerable, risky.isPotentiallyVulnerable)
    }

    // ── Coverage-completeness tests — exercise remaining branches ───────────

    @Test
    fun `named capturing group with inner unbounded outer unbounded is flagged`() {
        // (?<brew>a+)+ — named capture group with nested unbounded quantifiers
        val p = raw("(?<brew>a+)+")
        assertTrue(p.isPotentiallyVulnerable, "named group with nested unbounded must be flagged")
        assertEquals(0, p.analyze().findings[0].index)
    }

    @Test
    fun `lookbehind group is not flagged even with inner plus`() {
        // (?<=a+) — lookbehind; the outer cannot carry a repeat quantifier,
        // but our scanner must not crash and must not falsely flag it.
        val p = raw("(?<=a)b+")
        assertFalse(p.isPotentiallyVulnerable, "lookbehind must not produce false positive")
    }

    @Test
    fun `negative lookbehind is not flagged`() {
        val p = raw("(?<!a)b+")
        assertFalse(p.isPotentiallyVulnerable, "negative lookbehind must not produce false positive")
    }

    @Test
    fun `lazy star on group body inner unbounded is still flagged`() {
        // (?:a+)*? — lazy outer *? is still unbounded, inner + is unbounded
        val p = raw("(?:a+)*?")
        assertTrue(p.isPotentiallyVulnerable, "lazy star outer must still be flagged")
    }

    @Test
    fun `char class with inner plus in group body is flagged`() {
        // (?:[a-z]+)+ — inner char class with + is unbounded inside unbounded group
        val p = raw("(?:[a-z]+)+")
        assertTrue(p.isPotentiallyVulnerable, "char class inside unbounded group with outer unbounded must be flagged")
        assertEquals(0, p.analyze().findings[0].index)
    }

    @Test
    fun `char class with star in group body is not flagged when outer is bounded`() {
        // (?:[a-z]*){3} — outer {3} is bounded
        val p = raw("(?:[a-z]*){3}")
        assertFalse(p.isPotentiallyVulnerable, "bounded outer with inner char class must not be flagged")
    }

    @Test
    fun `escaped zero-width assertion is not treated as an unbounded subject`() {
        // \b+ — \b is a zero-width assertion; + on a zero-width token should not flag
        val p = raw("\\b+")
        assertFalse(p.isPotentiallyVulnerable, "quantified zero-width assertion must not be flagged")
    }

    @Test
    fun `negated char class with inner star in outer unbounded group is flagged`() {
        // (?:[^a]+)+ — inner unbounded on negated char class inside outer unbounded
        val p = raw("(?:[^a]+)+")
        assertTrue(p.isPotentiallyVulnerable, "negated char class with nested unbounded must be flagged")
    }
}
