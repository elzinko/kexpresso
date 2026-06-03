package kexpresso

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * Tests for domain helpers defined in [Domains.kt].
 */
class DomainsTest {

    // ── ipv4 ──────────────────────────────────────────────────────────────────

    @Test
    fun `ipv4 matches valid addresses`() {
        val p = kexpresso { startOfText(); ipv4(); endOfText() }
        assertTrue(p.matches("0.0.0.0"))
        assertTrue(p.matches("192.168.1.1"))
        assertTrue(p.matches("255.255.255.255"))
        assertTrue(p.matches("10.0.0.1"))
        assertTrue(p.matches("172.16.254.1"))
    }

    @Test
    fun `ipv4 does not match out-of-range octets`() {
        val p = kexpresso { startOfText(); ipv4(); endOfText() }
        assertFalse(p.matches("256.0.0.1"))
        assertFalse(p.matches("0.0.0.256"))
        assertFalse(p.matches("999.999.999.999"))
    }

    @Test
    fun `ipv4 does not match malformed addresses`() {
        val p = kexpresso { startOfText(); ipv4(); endOfText() }
        assertFalse(p.matches("192.168.1"))
        assertFalse(p.matches("192.168.1.1.1"))
        assertFalse(p.matches("192.168.1."))
        assertFalse(p.matches("not.an.ip.address"))
    }

    @Test
    fun `ipv4 extracted from text via findAll`() {
        val p = kexpresso { ipv4() }
        val addresses = p.findAll("Server 192.168.1.1 and gateway 10.0.0.1 are online")
            .map { it.value }.toList()
        assertEquals(listOf("192.168.1.1", "10.0.0.1"), addresses)
    }

    // ── uuid ──────────────────────────────────────────────────────────────────

    @Test
    fun `uuid matches valid RFC 4122 UUIDs`() {
        val p = kexpresso { startOfText(); uuid(); endOfText() }
        assertTrue(p.matches("550e8400-e29b-41d4-a716-446655440000"))
        assertTrue(p.matches("6ba7b810-9dad-11d1-80b4-00c04fd430c8"))
        assertTrue(p.matches("6BA7B810-9DAD-11D1-80B4-00C04FD430C8"))
        assertTrue(p.matches("00000000-0000-1000-8000-000000000000"))
    }

    @Test
    fun `uuid does not match version 0 or version 6 plus`() {
        val p = kexpresso { startOfText(); uuid(); endOfText() }
        // version digit must be 1-5
        assertFalse(p.matches("00000000-0000-0000-8000-000000000000"))
        assertFalse(p.matches("00000000-0000-6000-8000-000000000000"))
    }

    @Test
    fun `uuid does not match invalid variant bits`() {
        val p = kexpresso { startOfText(); uuid(); endOfText() }
        // variant octet must start with [89abAB]
        assertFalse(p.matches("550e8400-e29b-41d4-c716-446655440000"))
        assertFalse(p.matches("550e8400-e29b-41d4-0716-446655440000"))
    }

    @Test
    fun `uuid does not match malformed strings`() {
        val p = kexpresso { startOfText(); uuid(); endOfText() }
        assertFalse(p.matches("550e8400-e29b-41d4-a716-44665544000"))
        assertFalse(p.matches("not-a-uuid"))
        assertFalse(p.matches("550e8400e29b41d4a716446655440000"))
    }

    // ── slug ──────────────────────────────────────────────────────────────────

    @Test
    fun `slug matches valid lowercase slugs`() {
        val p = kexpresso { startOfText(); slug(); endOfText() }
        assertTrue(p.matches("espresso"))
        assertTrue(p.matches("cold-brew"))
        assertTrue(p.matches("top-10-coffee-shops"))
        assertTrue(p.matches("a"))
        assertTrue(p.matches("abc123"))
    }

    @Test
    fun `slug does not match uppercase letters`() {
        val p = kexpresso { startOfText(); slug(); endOfText() }
        assertFalse(p.matches("Espresso"))
        assertFalse(p.matches("Cold-Brew"))
    }

    @Test
    fun `slug does not match leading or trailing hyphens`() {
        val p = kexpresso { startOfText(); slug(); endOfText() }
        assertFalse(p.matches("-coffee"))
        assertFalse(p.matches("coffee-"))
        assertFalse(p.matches("-"))
    }

    @Test
    fun `slug does not match underscores or spaces`() {
        val p = kexpresso { startOfText(); slug(); endOfText() }
        assertFalse(p.matches("cold_brew"))
        assertFalse(p.matches("cold brew"))
    }

    // ── hexColor ──────────────────────────────────────────────────────────────

    @Test
    fun `hexColor matches all valid CSS hex color forms`() {
        val p = kexpresso { startOfText(); hexColor(); endOfText() }
        assertTrue(p.matches("#fff"))
        assertTrue(p.matches("#FFF"))
        assertTrue(p.matches("#f0f0"))
        assertTrue(p.matches("#1a2b3c"))
        assertTrue(p.matches("#1A2B3C"))
        assertTrue(p.matches("#ff000080"))
    }

    @Test
    fun `hexColor does not match invalid lengths`() {
        val p = kexpresso { startOfText(); hexColor(); endOfText() }
        assertFalse(p.matches("#ff"))
        assertFalse(p.matches("#fffff"))
        assertFalse(p.matches("#fffffff"))
        assertFalse(p.matches("#fffffffff"))
    }

    @Test
    fun `hexColor does not match without hash prefix`() {
        val p = kexpresso { startOfText(); hexColor(); endOfText() }
        assertFalse(p.matches("ffffff"))
        assertFalse(p.matches("fff"))
    }

    @Test
    fun `hexColor does not match non-hex digits`() {
        val p = kexpresso { startOfText(); hexColor(); endOfText() }
        assertFalse(p.matches("#xyz"))
        assertFalse(p.matches("#ggg"))
    }

    // ── semanticVersion ───────────────────────────────────────────────────────

    @Test
    fun `semanticVersion matches valid semver strings`() {
        val p = kexpresso { startOfText(); semanticVersion(); endOfText() }
        assertTrue(p.matches("0.0.0"))
        assertTrue(p.matches("1.0.0"))
        assertTrue(p.matches("2.3.4"))
        assertTrue(p.matches("1.0.0-alpha"))
        assertTrue(p.matches("1.0.0-alpha.1"))
        assertTrue(p.matches("1.0.0-rc.1+build.42"))
        assertTrue(p.matches("1.0.0+20240101"))
        assertTrue(p.matches("10.20.30"))
    }

    @Test
    fun `semanticVersion does not match partial or prefixed versions`() {
        val p = kexpresso { startOfText(); semanticVersion(); endOfText() }
        assertFalse(p.matches("1.0"))
        assertFalse(p.matches("1"))
        assertFalse(p.matches("v1.0.0"))
    }

    @Test
    fun `semanticVersion does not match leading zeros in numeric parts`() {
        val p = kexpresso { startOfText(); semanticVersion(); endOfText() }
        assertFalse(p.matches("01.0.0"))
        assertFalse(p.matches("1.00.0"))
        assertFalse(p.matches("1.0.00"))
    }

    @Test
    fun `semanticVersion found within text`() {
        val p = kexpresso { semanticVersion() }
        val versions = p.findAll("Released 1.2.3 and 2.0.0-beta").map { it.value }.toList()
        assertEquals(listOf("1.2.3", "2.0.0-beta"), versions)
    }

    // ── isoDate ───────────────────────────────────────────────────────────────

    @Test
    fun `isoDate matches valid calendar dates`() {
        val p = kexpresso { startOfText(); isoDate(); endOfText() }
        assertTrue(p.matches("2024-01-01"))
        assertTrue(p.matches("1999-12-31"))
        assertTrue(p.matches("2000-06-15"))
        assertTrue(p.matches("2024-02-29"))
    }

    @Test
    fun `isoDate does not match invalid months or days`() {
        val p = kexpresso { startOfText(); isoDate(); endOfText() }
        assertFalse(p.matches("2024-13-01"))
        assertFalse(p.matches("2024-00-01"))
        assertFalse(p.matches("2024-01-00"))
        assertFalse(p.matches("2024-01-32"))
    }

    @Test
    fun `isoDate does not match wrong formats`() {
        val p = kexpresso { startOfText(); isoDate(); endOfText() }
        assertFalse(p.matches("2024/01/01"))
        assertFalse(p.matches("01-01-2024"))
        assertFalse(p.matches("2024-1-1"))
    }

    @Test
    fun `isoDate caveat - does match invalid calendar date Feb 30`() {
        // Documented limitation: day-of-month is not validated against month length.
        val p = kexpresso { startOfText(); isoDate(); endOfText() }
        assertTrue(p.matches("2024-02-30"))
    }

    // ── isoTime ───────────────────────────────────────────────────────────────

    @Test
    fun `isoTime matches valid time strings`() {
        val p = kexpresso { startOfText(); isoTime(); endOfText() }
        assertTrue(p.matches("00:00"))
        assertTrue(p.matches("14:30"))
        assertTrue(p.matches("23:59"))
        assertTrue(p.matches("09:05:30"))
        assertTrue(p.matches("14:30:00Z"))
        assertTrue(p.matches("09:05:30+05:30"))
        assertTrue(p.matches("23:59:59-08:00"))
    }

    @Test
    fun `isoTime does not match invalid hours or minutes`() {
        val p = kexpresso { startOfText(); isoTime(); endOfText() }
        assertFalse(p.matches("24:00"))
        assertFalse(p.matches("12:60"))
        assertFalse(p.matches("12:00:60"))
    }

    @Test
    fun `isoTime does not match wrong formats`() {
        val p = kexpresso { startOfText(); isoTime(); endOfText() }
        assertFalse(p.matches("9:05"))
        assertFalse(p.matches("14-30"))
        assertFalse(p.matches("1430"))
    }

    // ── integerNumber ─────────────────────────────────────────────────────────

    @Test
    fun `integerNumber matches valid integers`() {
        val p = kexpresso { startOfText(); integerNumber(); endOfText() }
        assertTrue(p.matches("0"))
        assertTrue(p.matches("42"))
        assertTrue(p.matches("-7"))
        assertTrue(p.matches("+100"))
        assertTrue(p.matches("999999"))
    }

    @Test
    fun `integerNumber does not match leading zeros`() {
        val p = kexpresso { startOfText(); integerNumber(); endOfText() }
        assertFalse(p.matches("007"))
        assertFalse(p.matches("01"))
    }

    @Test
    fun `integerNumber does not match decimals or letters`() {
        val p = kexpresso { startOfText(); integerNumber(); endOfText() }
        assertFalse(p.matches("3.14"))
        assertFalse(p.matches("abc"))
        assertFalse(p.matches(""))
    }

    // ── decimalNumber ─────────────────────────────────────────────────────────

    @Test
    fun `decimalNumber matches integers and decimals`() {
        val p = kexpresso { startOfText(); decimalNumber(); endOfText() }
        assertTrue(p.matches("0"))
        assertTrue(p.matches("3.14"))
        assertTrue(p.matches("-2.5"))
        assertTrue(p.matches("+0.001"))
        assertTrue(p.matches("42"))
        assertTrue(p.matches("100.0"))
    }

    @Test
    fun `decimalNumber does not match bare decimal point or leading zeros`() {
        val p = kexpresso { startOfText(); decimalNumber(); endOfText() }
        assertFalse(p.matches(".5"))
        assertFalse(p.matches("007.0"))
        assertFalse(p.matches("01.5"))
    }

    @Test
    fun `decimalNumber does not match scientific notation`() {
        val p = kexpresso { startOfText(); decimalNumber(); endOfText() }
        assertFalse(p.matches("1e10"))
        assertFalse(p.matches("1.5E-3"))
    }

    // ── hashtag ───────────────────────────────────────────────────────────────

    @Test
    fun `hashtag matches valid hashtags`() {
        val p = kexpresso { startOfText(); hashtag(); endOfText() }
        assertTrue(p.matches("#coffee"))
        assertTrue(p.matches("#Coffee"))
        assertTrue(p.matches("#ColdBrew2024"))
        assertTrue(p.matches("#Espresso_Shot"))
        assertTrue(p.matches("#a"))
    }

    @Test
    fun `hashtag does not match starting with digit`() {
        val p = kexpresso { startOfText(); hashtag(); endOfText() }
        assertFalse(p.matches("#42"))
        assertFalse(p.matches("#1coffee"))
    }

    @Test
    fun `hashtag does not match bare hash or empty`() {
        val p = kexpresso { startOfText(); hashtag(); endOfText() }
        assertFalse(p.matches("#"))
        assertFalse(p.matches(""))
        assertFalse(p.matches("coffee"))
    }

    @Test
    fun `hashtag extracted from sentence via findAll`() {
        val p = kexpresso { hashtag() }
        val tags = p.findAll("Loving my #Espresso and #ColdBrew today!")
            .map { it.value }.toList()
        assertEquals(listOf("#Espresso", "#ColdBrew"), tags)
    }

    // ── mention ───────────────────────────────────────────────────────────────

    @Test
    fun `mention matches valid at-mentions`() {
        val p = kexpresso { startOfText(); mention(); endOfText() }
        assertTrue(p.matches("@barista"))
        assertTrue(p.matches("@CoffeeLover99"))
        assertTrue(p.matches("@cold_brew"))
        assertTrue(p.matches("@a"))
        // exactly 50 chars
        assertTrue(p.matches("@" + "a".repeat(50)))
    }

    @Test
    fun `mention does not match empty username or too long`() {
        val p = kexpresso { startOfText(); mention(); endOfText() }
        assertFalse(p.matches("@"))
        // 51 chars
        assertFalse(p.matches("@" + "a".repeat(51)))
    }

    @Test
    fun `mention does not match hyphens or spaces`() {
        val p = kexpresso { startOfText(); mention(); endOfText() }
        assertFalse(p.matches("@cold-brew"))
        assertFalse(p.matches("@cold brew"))
    }

    @Test
    fun `mention does not match without at sign`() {
        val p = kexpresso { startOfText(); mention(); endOfText() }
        assertFalse(p.matches("barista"))
    }

    @Test
    fun `mention extracted from text via findAll`() {
        val p = kexpresso { mention() }
        val mentions = p.findAll("Thanks @barista and @CoffeeLover99 for the great brew!")
            .map { it.value }.toList()
        assertEquals(listOf("@barista", "@CoffeeLover99"), mentions)
    }

    // ── e164Phone ─────────────────────────────────────────────────────────────

    @Test
    fun `e164Phone matches valid E164 numbers`() {
        val p = kexpresso { startOfText(); e164Phone(); endOfText() }
        assertTrue(p.matches("+14155552671"))
        assertTrue(p.matches("+442071234567"))
        assertTrue(p.matches("+81312345678"))
        // minimum: + then 7 digits
        assertTrue(p.matches("+1234567"))
        // maximum: + then 15 digits
        assertTrue(p.matches("+123456789012345"))
    }

    @Test
    fun `e164Phone does not match too few or too many digits`() {
        val p = kexpresso { startOfText(); e164Phone(); endOfText() }
        // + then only 6 digits (too short)
        assertFalse(p.matches("+123456"))
        // + then 16 digits (too long)
        assertFalse(p.matches("+1234567890123456"))
    }

    @Test
    fun `e164Phone does not match leading zero in country code`() {
        val p = kexpresso { startOfText(); e164Phone(); endOfText() }
        assertFalse(p.matches("+0123456789"))
    }

    @Test
    fun `e164Phone does not match numbers with separators`() {
        val p = kexpresso { startOfText(); e164Phone(); endOfText() }
        assertFalse(p.matches("+1-415-555-2671"))
        assertFalse(p.matches("+1 415 555 2671"))
        assertFalse(p.matches("14155552671"))
    }
}
