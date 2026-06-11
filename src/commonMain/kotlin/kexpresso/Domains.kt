package kexpresso

/**
 * Appends a pattern that matches an IPv4 address in dotted-decimal notation.
 *
 * Each octet must be in the range 0–255, decimal only. Leading zeros are not
 * permitted (e.g. `01` in an octet does not match).
 *
 * Example match: `"192.168.1.1"`, `"0.0.0.0"`, `"255.255.255.255"`
 *
 * **Caveats:**
 * - Does NOT validate CIDR notation (e.g. `192.168.1.0/24` will not match fully).
 * - Does NOT cover IPv6.
 */
fun KexpressoBuilder.ipv4(): KexpressoBuilder =
    append(
        "(?:(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]\\d|\\d)\\.){3}" +
            "(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]\\d|\\d)"
    )

/**
 * Appends a pattern that matches a UUID in RFC 4122 format (versions 1–5).
 *
 * The pattern accepts both upper- and lowercase hex digits.
 *
 * Example match: `"550e8400-e29b-41d4-a716-446655440000"`
 *
 * **Caveats:**
 * - Version bits (position 15: `[1-5]`) and variant bits (position 20: `[89abAB]`) are
 *   validated, but the rest of the structure is not semantically validated.
 * - Version 0 (nil UUID `00000000-…`) and version 6+ do not match.
 */
fun KexpressoBuilder.uuid(): KexpressoBuilder =
    append(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}" +
            "-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"
    )

/**
 * Appends a pattern that matches a URL/CMS slug.
 *
 * A slug consists of one or more groups of lowercase alphanumeric characters
 * separated by single hyphens. No leading or trailing hyphen is allowed.
 *
 * Example match: `"cold-brew"`, `"espresso"`, `"top-10-coffee-shops"`
 *
 * **Caveats:**
 * - Uppercase letters do NOT match; slugs must be fully lowercase.
 * - Does not allow underscores (use `handle()` for that).
 */
fun KexpressoBuilder.slug(): KexpressoBuilder =
    append("[a-z0-9]+(?:-[a-z0-9]+)*")

/**
 * Appends a pattern that matches a CSS hex color code.
 *
 * Accepts the following forms (the `#` prefix is required):
 * - `#RGB` (3 hex digits)
 * - `#RGBA` (4 hex digits)
 * - `#RRGGBB` (6 hex digits)
 * - `#RRGGBBAA` (8 hex digits)
 *
 * Example match: `"#fff"`, `"#1a2b3c"`, `"#ff000080"`
 *
 * **Caveats:**
 * - Case-insensitive hex digits — both `#FFF` and `#fff` match.
 * - 5- or 7-digit forms do not match (they are invalid CSS).
 */
fun KexpressoBuilder.hexColor(): KexpressoBuilder =
    append("#(?:[0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})")

/**
 * Appends a pattern that matches a Semantic Version string (SemVer 2.0.0).
 *
 * Covers the full semver.org spec: `MAJOR.MINOR.PATCH[-pre-release][+build]`.
 * No leading zeros are allowed in numeric identifiers.
 *
 * Example match: `"1.0.0"`, `"2.3.4-alpha.1"`, `"1.0.0-rc.1+build.42"`
 *
 * **Caveats:**
 * - A leading `v` (e.g. `v1.0.0`) does NOT match — strip it before matching.
 * - Partial forms like `"1.0"` do NOT match.
 * - Leading zeros in numeric parts (e.g. `01.0.0`) do NOT match.
 */
fun KexpressoBuilder.semanticVersion(): KexpressoBuilder =
    append(
        "(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)" +
            "(?:-[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?" +
            "(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?"
    )

/**
 * Appends a pattern that matches an ISO-8601 calendar date in the form `YYYY-MM-DD`.
 *
 * Month must be `01`–`12`; day must be `01`–`31`.
 *
 * Example match: `"2024-01-15"`, `"1999-12-31"`
 *
 * **Caveats:**
 * - Does NOT validate day-of-month against the actual calendar. For example,
 *   `"2024-02-30"` matches even though February never has 30 days.
 * - Does NOT accept other ISO-8601 date representations (e.g. ordinal `2024-032`
 *   or week-based `2024-W05-1`).
 */
fun KexpressoBuilder.isoDate(): KexpressoBuilder =
    append("\\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01])")

/**
 * Appends a pattern that matches an ISO-8601 time in the form `HH:MM[:SS][Z|±HH:MM]`.
 *
 * Hours: `00`–`23`; minutes and seconds: `00`–`59`. Seconds and timezone are optional.
 *
 * Example match: `"14:30"`, `"09:05:30Z"`, `"23:59:59+05:30"`
 *
 * **Caveats:**
 * - Leap seconds (`:60`) are NOT accepted.
 * - Does not cover fractional seconds (e.g. `14:30:00.500`).
 * - Timezone offset is validated for hour range (`00`–`23`) and minute range (`00`–`59`),
 *   but exotic offsets like `+14:00` (which are valid for Pacific island nations) also match.
 */
fun KexpressoBuilder.isoTime(): KexpressoBuilder =
    append(
        "(?:[01]\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?" +
            "(?:Z|[+-](?:[01]\\d|2[0-3]):[0-5]\\d)?"
    )

/**
 * Appends a pattern that matches a signed or unsigned integer without leading zeros.
 *
 * `0` itself is valid. An optional leading `+` or `-` is accepted.
 *
 * Example match: `"0"`, `"42"`, `"-7"`, `"+100"`
 *
 * **Caveats:**
 * - Leading zeros are not allowed (e.g. `007` does not match as a single token).
 * - There is no upper bound on the number of digits.
 */
fun KexpressoBuilder.integerNumber(): KexpressoBuilder =
    append("[+-]?(?:0|[1-9]\\d*)")

/**
 * Appends a pattern that matches a decimal number with an optional fractional part.
 *
 * The integer part must not have leading zeros (except `0` itself).
 * An optional leading `+` or `-` is accepted.
 *
 * Example match: `"0"`, `"3.14"`, `"-2.5"`, `"+0.001"`
 *
 * **Caveats:**
 * - A bare decimal point (e.g. `".5"`) does NOT match; there must be at least one
 *   digit before the decimal point.
 * - Scientific notation (e.g. `1e10`) is not supported.
 */
fun KexpressoBuilder.decimalNumber(): KexpressoBuilder =
    append("[+-]?(?:0|[1-9]\\d*)(?:\\.\\d+)?")

/**
 * Appends a pattern that matches a social-media hashtag.
 *
 * A hashtag begins with `#`, followed by an ASCII letter, then zero or more word
 * characters (`\w` — letters, digits, and underscores).
 *
 * Example match: `"#coffee"`, `"#ColdBrew2024"`, `"#Espresso_Shot"`
 *
 * **Caveats:**
 * - The first character after `#` must be a letter (not a digit), so `#42` does not match.
 * - Unicode letters/emoji are not supported.
 */
fun KexpressoBuilder.hashtag(): KexpressoBuilder =
    append("#[a-zA-Z]\\w*")

/**
 * Appends a pattern that matches an @mention (Twitter/X convention).
 *
 * A mention begins with `@`, followed by 1 to 50 characters from `[a-zA-Z0-9_]`.
 *
 * Example match: `"@barista"`, `"@CoffeeLover99"`, `"@cold_brew"`
 *
 * **Caveats:**
 * - Length limit is 50 characters (Twitter/X maximum). Other platforms (Instagram,
 *   GitHub, etc.) have different limits.
 * - Unicode characters and hyphens are not accepted.
 */
fun KexpressoBuilder.mention(): KexpressoBuilder =
    append("@[a-zA-Z0-9_]{1,50}")

/**
 * Appends a pattern that matches an E.164 international phone number.
 *
 * Format: `+` followed by a non-zero country code digit, then 6–14 more digits
 * (total 7–15 digits after the `+`).
 *
 * Example match: `"+14155552671"`, `"+442071234567"`, `"+81312345678"`
 *
 * **Caveats:**
 * - No separators (spaces, dashes, parentheses) are accepted — the number must be
 *   in compact E.164 form.
 * - The pattern does not validate country codes or subscriber number length rules,
 *   only the overall length constraint of 7–15 digits.
 */
fun KexpressoBuilder.e164Phone(): KexpressoBuilder =
    append("\\+[1-9]\\d{6,14}")

/**
 * Appends a pattern that matches an IPv6 address — full and `::` -compressed forms.
 *
 * The pattern covers the most common representations:
 * - Full form: eight groups of 1–4 hex digits separated by colons,
 *   e.g. `"2001:0db8:85a3:0000:0000:8a2e:0370:7334"`.
 * - `::` -compressed forms where one contiguous run of all-zero groups is replaced by
 *   `::`, e.g. `"2001:db8::1"`, `"::1"` (loopback), `"::"` (all zeros).
 *
 * Example match: `"2001:0db8:85a3:0000:0000:8a2e:0370:7334"`, `"::1"`, `"fe80::1"`
 *
 * **Caveats:**
 * - Embedded IPv4 notation (e.g. `"::ffff:192.168.1.1"`) does **not** match.
 * - Zone IDs (e.g. `"fe80::1%eth0"`) do **not** match.
 * - The pattern does **not** validate that a compressed form expands to exactly 128 bits;
 *   over-specified forms like `"1:2:3:4:5:6:7::8"` may not match as intended.
 * - For a plain IPv4 address, use [ipv4] instead.
 */
fun KexpressoBuilder.ipv6(): KexpressoBuilder =
    // The branches are wrapped in a non-capturing group so the helper composes correctly with
    // surrounding tokens (otherwise the bare `|` would bind only the first/last branch).
    append(
        "(?:" +
            "(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}" +
            "|(?:[0-9a-fA-F]{1,4}:){1,7}:" +
            "|:(?::[0-9a-fA-F]{1,4}){1,7}" +
            "|(?:[0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}" +
            "|(?:[0-9a-fA-F]{1,4}:){1,5}(?::[0-9a-fA-F]{1,4}){1,2}" +
            "|(?:[0-9a-fA-F]{1,4}:){1,4}(?::[0-9a-fA-F]{1,4}){1,3}" +
            "|(?:[0-9a-fA-F]{1,4}:){1,3}(?::[0-9a-fA-F]{1,4}){1,4}" +
            "|(?:[0-9a-fA-F]{1,4}:){1,2}(?::[0-9a-fA-F]{1,4}){1,5}" +
            "|[0-9a-fA-F]{1,4}:(?::[0-9a-fA-F]{1,4}){1,6}" +
            "|::" +
            ")"
    )

/**
 * Appends a pattern that matches an IEEE 802 MAC address.
 *
 * Both colon-separated (`01:23:45:67:89:AB`) and hyphen-separated
 * (`01-23-45-67-89-AB`) forms are accepted. Mixed separators are not.
 * Case-insensitive hex digits (`a`–`f` and `A`–`F`) are both valid.
 *
 * Example match: `"01:23:45:67:89:AB"`, `"01-23-45-67-89-ab"`
 *
 * **Caveats:**
 * - Exactly 6 two-hex-digit octets are required; no short or long forms.
 * - Dot-separated form (used by Cisco, e.g. `0123.4567.89ab`) does **not** match.
 * - Mixed separators (e.g. `01:23-45:67-89:AB`) do **not** match.
 */
fun KexpressoBuilder.macAddress(): KexpressoBuilder =
    // Wrapped in a non-capturing group so the two separator branches compose correctly with
    // surrounding tokens (a bare `|` would let adjacent tokens bind to only one branch).
    append(
        "(?:" +
            "(?:[0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}" +
            "|(?:[0-9a-fA-F]{2}-){5}[0-9a-fA-F]{2}" +
            ")"
    )

/**
 * Appends a pattern that matches a standard Base64-encoded string.
 *
 * Accepts strings made up of groups of four Base64 characters (`A–Z`, `a–z`, `0–9`,
 * `+`, `/`), with optional `=` or `==` padding at the end.
 *
 * Example match: `"S2V4cHJlc3Nv"`, `"dGVzdA=="`, `"YQ=="`, `"YQ"` (unpadded), `"TWE"`
 *
 * **Caveats:**
 * - Also matches the **empty string** (zero groups, zero padding — valid per the pattern).
 * - Does **not** enforce that the total character count is a multiple of 4; the padding
 *   check (`==` / `=` suffix) relies on the trailing group matching correctly.
 * - URL-safe Base64 characters (`-` and `_` instead of `+` and `/`) do **not** match —
 *   for that, use [jwt] which uses the base64url alphabet.
 */
fun KexpressoBuilder.base64(): KexpressoBuilder =
    // Trailing group accepts a final 2- or 3-char chunk with OR without padding, so both padded
    // (`YQ==`, `TWE=`) and unpadded (`YQ`, `TWE`) Base64 are matched, as the docs advertise.
    append(
        "(?:[A-Za-z0-9+/]{4})*" +
            "(?:[A-Za-z0-9+/]{2}(?:==)?|[A-Za-z0-9+/]{3}=?)?"
    )

/**
 * Appends a pattern that matches a JSON Web Token (JWT) in compact serialisation.
 *
 * A JWT consists of three base64url-encoded segments (header, payload, signature)
 * separated by dots, e.g. `"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.abc123"`.
 * Base64url characters are `A–Z`, `a–z`, `0–9`, `-`, and `_` (no padding `=`).
 *
 * Example match: `"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"`
 *
 * **Caveats:**
 * - Structural validation only — the pattern does **not** verify the signature, decode
 *   the payload, or check expiry/claims.
 * - Each segment must contain at least one character; empty segments do **not** match.
 * - Standard Base64 characters `+` and `/` are not part of the base64url alphabet and
 *   do **not** match here.
 */
fun KexpressoBuilder.jwt(): KexpressoBuilder =
    append("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")
