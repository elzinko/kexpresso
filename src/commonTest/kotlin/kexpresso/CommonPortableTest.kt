package kexpresso

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Portable test suite that runs on **every** Kotlin Multiplatform target (JVM + JS/IR).
 *
 * Why a separate file with camelCase names?
 * Kotlin/JS (IR) forbids spaces and most punctuation in declaration names, so the
 * backtick-with-spaces test names used by the rest of the suite cannot compile to JS.
 * The full backtick-named suite therefore lives in `jvmTest` (JVM-only), and this file
 * mirrors a solid, portable subset using JS-legal identifiers.
 *
 * Scope discipline:
 * - String-generation assertions (`source`, `describe()`, `toKexpressoCode()`) are fully portable.
 * - Runtime-matching assertions deliberately avoid JVM-only constructs that the ECMAScript
 *   engine cannot compile: `startOfText()`/`endOfText()` (`\A`/`\z`), lookbehind, atomic groups,
 *   and possessive quantifiers. Those are exercised JVM-only in `jvmTest`.
 */
class CommonPortableTest {

    private fun pattern(block: KexpressoBuilder.() -> Unit) = kexpresso(block = block)

    // ── source generation: primitives ─────────────────────────────────────────

    @Test
    fun digitSource() = assertEquals("\\d", pattern { digit() }.source)

    @Test
    fun letterAndDigitSource() = assertEquals("\\d[a-zA-Z]", pattern { digit(); letter() }.source)

    @Test
    fun completionTokenSources() {
        assertEquals("\\B", pattern { nonWordBoundary() }.source)
        assertEquals("[a-z]", pattern { lowercaseLetter() }.source)
        assertEquals("[a-zA-Z0-9]", pattern { alphanumeric() }.source)
        assertEquals("\\t", pattern { tab() }.source)
        assertEquals("\\n", pattern { newline() }.source)
        assertEquals("\\r", pattern { carriageReturn() }.source)
    }

    @Test
    fun anchorSources() {
        // Only ECMAScript-valid anchors are built here. `\A`/`\z` (startOfText/endOfText) are
        // JVM-only: building them eagerly compiles a Regex, which the JS engine rejects.
        // Their generation is asserted JVM-only in jvmTest's KexpressoBuilderTest.
        assertEquals("^", pattern { startOfLine() }.source)
        assertEquals("$", pattern { endOfLine() }.source)
        assertEquals("\\b", pattern { wordBoundary() }.source)
    }

    // ── source generation: portable literal escaper (replaces \Q…\E) ──────────

    @Test
    fun literalEscapesMetacharactersPerCharacter() {
        // Dot is a metacharacter → escaped; result is portable on JS (no \Q…\E).
        assertEquals("a\\.b", pattern { literal("a.b") }.source)
        // Hyphen is NOT a metacharacter outside a character class → verbatim.
        assertEquals("a-b", pattern { literal("a-b") }.source)
    }

    @Test
    fun literalEscapesAllMetacharacters() {
        // Each of these is backslash-escaped by the portable escaper.
        assertEquals(
            "\\.\\^\\$\\|\\?\\*\\+\\(\\)\\[\\]\\{\\}\\/",
            pattern { literal(".^\$|?*+()[]{}/") }.source,
        )
    }

    @Test
    fun charEscapesDot() = assertEquals("\\.", pattern { char('.') }.source)

    // ── source generation: quantifiers, groups, alternation ──────────────────

    @Test
    fun quantifierSources() {
        assertEquals("(?:\\d)+", pattern { oneOrMore { digit() } }.source)
        assertEquals("(?:\\d)*?", pattern { zeroOrMore(greedy = false) { digit() } }.source)
        assertEquals("(?:\\d){3}", pattern { exactly(3) { digit() } }.source)
        assertEquals("(?:[a-zA-Z]){2,4}", pattern { between(2, 4) { letter() } }.source)
        assertEquals("(?:\\d){2,}?", pattern { atLeast(2, greedy = false) { digit() } }.source)
    }

    @Test
    fun groupAndCaptureSources() {
        assertEquals("(?:[a-zA-Z])", pattern { group { letter() } }.source)
        assertEquals("(\\d)", pattern { capture { digit() } }.source)
        assertEquals("(?<year>\\d)", pattern { capture("year") { digit() } }.source)
    }

    @Test
    fun alternationAndLookaheadSources() {
        assertEquals("(?:[a-zA-Z]|\\d)", pattern { oneOf({ letter() }, { digit() }) }.source)
        assertEquals("(?=\\d)", pattern { followedBy { digit() } }.source)
        assertEquals("(?!\\d)", pattern { notFollowedBy { digit() } }.source)
    }

    @Test
    fun backreferenceSources() {
        assertEquals("(\\d)\\1", pattern { capture { digit() }; backreference(1) }.source)
        assertEquals(
            "(?<d>[a-zA-Z])\\k<d>",
            pattern { capture("d") { letter() }; backreference("d") }.source,
        )
    }

    @Test
    fun includeEmbedsNonCapturingGroup() {
        val octet = kexpresso { between(1, 3) { digit() } }
        assertEquals("(?:(?:\\d){1,3})", kexpresso { include(octet) }.source)
    }

    // ── describe() ─────────────────────────────────────────────────────────────

    @Test
    fun describePrimitivesWithLineAnchorsAndQuantifier() {
        // Uses portable ^…$ anchors (startOfText/endOfText `\A`/`\z` are JVM-only at runtime).
        val p = kexpresso { startOfLine(); oneOrMore { digit() }; endOfLine() }
        assertEquals("start of line, one or more of (a digit), end of line", p.describe())
    }

    @Test
    fun describeAlternationAndCaptures() {
        assertEquals(
            "one of (the literal \"cat\"; the literal \"dog\")",
            kexpresso { oneOf({ literal("cat") }, { literal("dog") }) }.describe(),
        )
        assertEquals(
            "a capture named \"year\" of (exactly 4 of (a digit))",
            kexpresso { capture("year") { exactly(4) { digit() } } }.describe(),
        )
    }

    @Test
    fun describeRawAndPublicConstructorFallBackToRaw() {
        assertEquals("raw regex `\\d{4}`", kexpresso { raw("\\d{4}") }.describe())
        assertEquals("raw regex `\\d+`", KexpressoPattern("\\d+", Regex("\\d+")).describe())
    }

    // ── toKexpressoCode() ──────────────────────────────────────────────────────

    @Test
    fun generateCodeForADate() {
        val code = Kexpresso.from("\\d{4}-\\d{2}-\\d{2}").toKexpressoCode()
        assertEquals(
            """
            |kexpresso {
            |    exactly(4) { digit() }
            |    literal("-")
            |    exactly(2) { digit() }
            |    literal("-")
            |    exactly(2) { digit() }
            |}
            """.trimMargin(),
            code,
        )
    }

    @Test
    fun generateCodeMapsFriendlyTokenTable() {
        // `\A`/`\z` are omitted: `from` compiles the regex verbatim, and the JS engine rejects
        // those anchors. The startOfText()/endOfText() codegen mapping is covered JVM-only.
        val code = Kexpresso.from("\\d\\D\\s\\S\\w\\W.\\b\\B\\t\\n\\r").toKexpressoCode()
        assertEquals(
            """
            |kexpresso {
            |    digit()
            |    nonDigit()
            |    whitespace()
            |    nonWhitespace()
            |    wordChar()
            |    nonWordChar()
            |    anyChar()
            |    wordBoundary()
            |    nonWordBoundary()
            |    tab()
            |    newline()
            |    carriageReturn()
            |}
            """.trimMargin(),
            code,
        )
    }

    // ── ReDoS analysis (pure string heuristic — fully portable) ───────────────

    @Test
    fun nestedUnboundedIsFlagged() {
        val p = kexpresso { oneOrMore { oneOrMore { letter() } } }
        assertTrue(p.isPotentiallyVulnerable)
        val report = p.analyze()
        assertEquals(1, report.findings.size)
        assertEquals(0, report.findings[0].index)
        assertEquals(ReDoSSeverity.WARNING, report.findings[0].severity)
    }

    @Test
    fun plainQuantifierIsNotFlagged() {
        assertFalse(kexpresso { oneOrMore { digit() } }.isPotentiallyVulnerable)
    }

    // ── portable runtime matching (ECMAScript-safe constructs only) ───────────

    @Test
    fun matchesLiteralAndDot() {
        assertTrue(pattern { literal("Espresso") }.matches("Espresso"))
        assertFalse(pattern { literal("Espresso") }.matches("espresso"))
        // Escaped dot matches a literal dot, not any char.
        assertTrue(pattern { literal("a.b") }.matches("a.b"))
        assertFalse(pattern { literal("a.b") }.matches("axb"))
    }

    @Test
    fun anchoredWordMatchesWithLineAnchors() {
        // Use ^…$ (portable) rather than \A…\z.
        val p = kexpresso { startOfLine(); oneOrMore { letter() }; endOfLine() }
        assertTrue(p.matches("Espresso"))
        assertFalse(p.matches("Espresso1"))
    }

    @Test
    fun quantifierMatching() {
        val p = kexpresso { startOfLine(); exactly(3) { digit() }; endOfLine() }
        assertTrue(p.matches("123"))
        assertFalse(p.matches("12"))
        assertFalse(p.matches("1234"))
    }

    @Test
    fun positiveLookaheadMatching() {
        val p = pattern { oneOrMore { digit() }; followedBy { literal("ml") } }
        assertEquals("250", p.find("250ml")?.value)
        assertNull(p.find("250g"))
    }

    @Test
    fun numericBackreferenceMatching() {
        val p = pattern { capture { oneOrMore { wordChar() } }; whitespace(); backreference(1) }
        assertTrue(p.containsMatchIn("latte latte"))
        assertFalse(p.containsMatchIn("latte mocha"))
    }

    @Test
    fun namedCaptureAndCapturesAccessor() {
        val p = kexpresso {
            capture("year") { exactly(4) { digit() } }
            literal("-")
            capture("month") { exactly(2) { digit() } }
        }
        val caps = p.find("2026-06")?.captures
        assertNotNull(caps)
        assertEquals(2026, caps.int("year"))
        assertEquals("06", caps.string("month"))
        assertNull(caps.string("missing"))
    }

    @Test
    fun capturesOrThrowVariants() {
        val p = kexpresso { capture("label") { oneOrMore { letter() } } }
        val caps = p.find("Latte")?.captures
        assertNotNull(caps)
        assertEquals("Latte", caps.stringOrThrow("label"))
        assertFailsWith<NoSuchElementException> { caps.stringOrThrow("missing") }
        assertFailsWith<NumberFormatException> { caps.intOrThrow("label") }
    }

    @Test
    fun ignoreCaseOptionApplies() {
        val p = kexpresso(RegexOption.IGNORE_CASE) { literal("espresso") }
        assertTrue(p.matches("ESPRESSO"))
        assertTrue(RegexOption.IGNORE_CASE in p.options)
    }

    @Test
    fun replaceAndSplitOperations() {
        val letters = kexpresso { oneOrMore { letter() } }
        assertEquals("Americano Latte", letters.replaceFirst("Espresso Latte", "Americano"))
        assertEquals("ESPRESSO LATTE", letters.replaceAll("espresso latte") { it.value.uppercase() })
        val sep = kexpresso { literal(", ") }
        assertEquals(listOf("a", "b", "c"), sep.split("a, b, c"))
    }

    // ── reverse: matching correctness is engine-exact and portable ────────────

    @Test
    fun fromMatchesIdenticallyToRawRegex() {
        val regex = "\\d{4}-\\d{2}-\\d{2}"
        val reference = Regex(regex)
        val reversed = Kexpresso.from(regex)
        for (input in listOf("2026-06-03", "2026-6-3", "not-a-date", "")) {
            assertEquals(reference.matches(input), reversed.matches(input))
        }
        // Source is preserved verbatim by `from`.
        assertEquals(regex, reversed.source)
    }

    @Test
    fun fromThrowsOnInvalidRegex() {
        // An invalid regex throws on every platform, but the concrete type differs: the JVM
        // throws java.util.regex.PatternSyntaxException (an IllegalArgumentException), while the
        // JS engine throws a SyntaxError that Kotlin/JS surfaces as a plain Throwable. The
        // portable contract is simply "it throws"; the JVM-specific type is asserted in jvmTest.
        assertFailsWith<Throwable> { Kexpresso.from("(unclosed") }
    }

    // ── name validation (pure Kotlin, portable) ───────────────────────────────

    @Test
    fun invalidGroupNamesThrow() {
        assertFailsWith<IllegalArgumentException> { pattern { capture("1bad") { digit() } } }
        assertFailsWith<IllegalArgumentException> { pattern { capture("bad name") { digit() } } }
        assertFailsWith<IllegalArgumentException> { pattern { backreference(0) } }
    }
}
