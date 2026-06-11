package kexpresso

/**
 * Severity levels for a [ReDoSFinding].
 *
 * Currently only [WARNING] is defined; additional levels may be added in future releases
 * without breaking binary compatibility.
 */
@ExperimentalKexpressoApi
enum class ReDoSSeverity {
    /** A potential catastrophic-backtracking shape was detected (heuristic, not a proof). */
    WARNING,
}

/**
 * A single potential ReDoS (catastrophic-backtracking) finding produced by
 * [KexpressoPattern.analyze].
 *
 * @property message  Human-readable description of the risky construct.
 * @property index    Position in [KexpressoPattern.source] where the risky group starts.
 * @property severity Always [ReDoSSeverity.WARNING] today; reserved for finer-grained levels.
 */
@ExperimentalKexpressoApi
data class ReDoSFinding(
    val message: String,
    val index: Int,
    val severity: ReDoSSeverity = ReDoSSeverity.WARNING,
)

/**
 * The result of a static ReDoS analysis on a [KexpressoPattern].
 *
 * **This is a best-effort heuristic**, not a proof of (non-)vulnerability. It detects the
 * canonical "evil regex" shape — nested unbounded quantifiers — but cannot reason about
 * every pathological input or engine behaviour.
 *
 * @property findings All detected findings; empty when no risky constructs were found.
 */
@ExperimentalKexpressoApi
data class ReDoSReport(
    val findings: List<ReDoSFinding>,
) {
    /**
     * Returns `true` if at least one finding was detected.
     *
     * A `true` result means the pattern *may* be vulnerable; a `false` result does not
     * guarantee safety — it only means the heuristic found no flagged shapes.
     */
    val isPotentiallyVulnerable: Boolean get() = findings.isNotEmpty()
}

/**
 * Performs best-effort static analysis for catastrophic-backtracking (ReDoS) risk.
 *
 * The heuristic detects **nested unbounded quantifiers**: an unbounded quantifier (`*`, `+`,
 * or `{n,}`) applied to a group whose body itself contains at least one unbounded quantifier
 * applied to something that can match a variable number of characters.
 *
 * Examples flagged: `(?:a+)+`, `(a*)*`, `(?:\w+)+`, `(?:(?:x)+)*`, `(.+)+`.
 *
 * **Limitations (be honest about them):**
 * - This is a syntactic heuristic on the source string; it does not prove vulnerability.
 * - It does not detect alternation-based or polynomial-backtracking ReDoS patterns.
 * - Detection centres on a group that itself carries an unbounded quantifier; a risky
 *   construct nested only inside non-quantified groups (e.g. `(?:(?:a+)+)`, reachable via
 *   [KexpressoBuilder.raw] or `group { … }`) may not be flagged. The DSL's quantifier
 *   blocks normally emit the outer quantifier, so realistic footguns are caught.
 * - False positives are guarded for: quantifier chars inside `[...]`, escaped quantifiers
 *   (`\+`, `\*`), bounded outer or inner quantifiers, possessive quantifiers (`*+`, `++`),
 *   and atomic groups `(?>...)`.
 *
 * @return a [ReDoSReport] containing zero or more [ReDoSFinding]s.
 */
@ExperimentalKexpressoApi
fun KexpressoPattern.analyze(): ReDoSReport {
    val findings = mutableListOf<ReDoSFinding>()
    ReDoSScanner(source).scanForNestedUnbounded(findings)
    return ReDoSReport(findings)
}

/**
 * Convenience property: `true` if [analyze] returns at least one finding.
 *
 * See [analyze] for important caveats about what this property does — and does not — guarantee.
 */
@ExperimentalKexpressoApi
val KexpressoPattern.isPotentiallyVulnerable: Boolean get() = analyze().isPotentiallyVulnerable

// ── Internal scanner ──────────────────────────────────────────────────────────

/**
 * Lightweight descriptor for a parsed regex quantifier token.
 *
 * @property length      Number of source characters occupied by this quantifier.
 * @property isUnbounded `true` for `*`, `+`, `{n,}` (with optional lazy `?`).
 *                       `false` for `?`, `{n}`, `{n,m}`, and any possessive form (`*+`, `++`).
 */
private data class QuantifierInfo(val length: Int, val isUnbounded: Boolean)

/**
 * Classification of a group's opening syntax.
 *
 * @property bodyStart Index of the first character inside the group body (after all prefix chars).
 * @property isSafe    `true` when the group never backtracks (atomic group `(?>...)`).
 */
private data class GroupOpen(val bodyStart: Int, val isSafe: Boolean)

/**
 * Character-by-character scanner that walks a regex source string and identifies groups
 * that carry an outer unbounded quantifier AND whose body contains an inner unbounded
 * quantifier — the canonical "evil regex" (catastrophic-backtracking) shape.
 *
 * Design notes:
 * - No regex is used to parse the regex string; every rule is handled explicitly.
 * - Quantifiers are always consumed (advanced past) after the token they apply to,
 *   preventing the scanner from misreading the second char of `++` or `*+` as a new token.
 * - All helper functions operate on an arbitrary string [s] at a given position, so the
 *   same logic works for both the top-level scan and recursive body scans.
 */
@ExperimentalKexpressoApi
@Suppress("TooManyFunctions")
private class ReDoSScanner(private val source: String) {

    // ── Top-level scan ────────────────────────────────────────────────────────

    /**
     * Walk [source] and append a [ReDoSFinding] for every group that has both an outer
     * unbounded quantifier and at least one inner unbounded quantifier in its body.
     */
    fun scanForNestedUnbounded(findings: MutableList<ReDoSFinding>) {
        var i = 0
        while (i < source.length) {
            i = processTopLevelChar(i, findings)
        }
    }

    private fun processTopLevelChar(i: Int, findings: MutableList<ReDoSFinding>): Int {
        return when {
            source[i] == '\\' -> i + 2                   // escaped char → skip both
            source[i] == '[' -> skipCharClass(source, i) // char class → skip to after ']'
            source[i] == '(' -> processGroup(i, findings)
            else -> i + 1
        }
    }

    /**
     * Analyse the group starting at [groupStart] (which must be `(`).
     * Adds a finding if the group has an outer unbounded quantifier and an inner one.
     * Returns the position immediately after `)` (not past the quantifier — the
     * top-level loop continues from there and will skip non-`(` chars naturally).
     */
    private fun processGroup(groupStart: Int, findings: MutableList<ReDoSFinding>): Int {
        val open = parseGroupOpen(source, groupStart)
        val closeIndex = findGroupClose(source, open.bodyStart)
        if (closeIndex < 0) return groupStart + 1
        val afterClose = closeIndex + 1
        val quant = readQuantifier(source, afterClose)
        if (!open.isSafe && quant != null && quant.isUnbounded) {
            val body = source.substring(open.bodyStart, closeIndex)
            if (bodyContainsUnbounded(body)) {
                val snippetEnd = minOf(afterClose + quant.length, source.length)
                val snippet = source.substring(groupStart, snippetEnd)
                findings += ReDoSFinding(
                    message = "Nested unbounded quantifier at index $groupStart: $snippet …",
                    index = groupStart,
                )
            }
        }
        // Advance to after ')'; the outer loop will skip quantifier chars naturally.
        return afterClose
    }

    // ── Group-open classification ─────────────────────────────────────────────

    /**
     * Starting at [i] (which must point at `(` in [s]), returns a [GroupOpen] describing
     * where the group body begins and whether the group is safe (atomic).
     *
     * Supported prefix forms and their body offsets from [i]:
     * - `(?>...`  → offset 3 (atomic group, isSafe=true)
     * - `(?<=...` / `(?<!...` → offset 4 (lookbehind)
     * - `(?<name>...` → after `>` (named capture)
     * - `(?:...` / `(?=...` / `(?!...` → offset 3
     * - `(...` → offset 1 (plain capturing)
     */
    private fun parseGroupOpen(s: String, i: Int): GroupOpen {
        return when {
            s.startsWith("(?>", i) ->
                GroupOpen(bodyStart = i + 3, isSafe = true)
            s.startsWith("(?<", i) ->
                parseLookbehindOrNamed(s, i)
            s.startsWith("(?", i) && i + 2 < s.length ->
                GroupOpen(bodyStart = i + 3, isSafe = false) // (?:, (?=, (?!, (?<= already handled
            else ->
                GroupOpen(bodyStart = i + 1, isSafe = false)
        }
    }

    private fun parseLookbehindOrNamed(s: String, i: Int): GroupOpen {
        val third = if (i + 3 < s.length) s[i + 3] else ' '
        return if (third == '=' || third == '!') {
            // (?<= or (?<! — lookbehind; body starts at i+4
            GroupOpen(bodyStart = i + 4, isSafe = false)
        } else {
            // (?<name> — scan for closing '>'
            val gt = s.indexOf('>', i + 3)
            GroupOpen(bodyStart = if (gt >= 0) gt + 1 else i + 3, isSafe = false)
        }
    }

    // ── Closing-paren finder ──────────────────────────────────────────────────

    /**
     * Starting from [bodyStart] inside [s], walks forward with balanced-paren tracking
     * and returns the index of the matching `)`, or -1 if not found.
     * Respects `\` escapes and character classes.
     */
    private fun findGroupClose(s: String, bodyStart: Int): Int {
        var depth = 0
        var i = bodyStart
        while (i < s.length) {
            when {
                s[i] == '\\' -> i += 2
                s[i] == '[' -> i = skipCharClass(s, i)
                s[i] == '(' -> { depth++; i++ }
                s[i] == ')' -> {
                    if (depth == 0) return i
                    depth--
                    i++
                }
                else -> i++
            }
        }
        return -1
    }

    // ── Quantifier reading ────────────────────────────────────────────────────

    /**
     * Reads the quantifier in [s] at position [pos].
     * Returns `null` if there is no quantifier at [pos].
     *
     * Possessive suffixes (`*+`, `++`) are recognised as bounded (non-backtracking) so they
     * do NOT contribute to a ReDoS finding.  Lazy suffixes (`*?`, `+?`) remain unbounded.
     */
    private fun readQuantifier(s: String, pos: Int): QuantifierInfo? {
        if (pos >= s.length) return null
        return when (s[pos]) {
            '?' -> QuantifierInfo(if (pos + 1 < s.length && s[pos + 1] == '?') 2 else 1, false)
            '*' -> readStarQuant(s, pos)
            '+' -> readPlusQuant(s, pos)
            '{' -> parseCurly(s, pos)
            else -> null
        }
    }

    private fun readStarQuant(s: String, pos: Int): QuantifierInfo {
        if (pos + 1 < s.length && s[pos + 1] == '+') return QuantifierInfo(2, false) // possessive *+
        return QuantifierInfo(if (pos + 1 < s.length && s[pos + 1] == '?') 2 else 1, true)
    }

    private fun readPlusQuant(s: String, pos: Int): QuantifierInfo {
        if (pos + 1 < s.length && s[pos + 1] == '+') return QuantifierInfo(2, false) // possessive ++
        return QuantifierInfo(if (pos + 1 < s.length && s[pos + 1] == '?') 2 else 1, true)
    }

    /**
     * Parses a `{...}` quantifier in [s] at [pos].
     * Returns `null` when the braces don't form a valid quantifier (treated as literals).
     */
    private fun parseCurly(s: String, pos: Int): QuantifierInfo? {
        val end = s.indexOf('}', pos + 1)
        if (end < 0) return null
        val inside = s.substring(pos + 1, end)
        val lazyLen = if (end + 1 < s.length && s[end + 1] == '?') 1 else 0
        val totalLen = (end - pos + 1) + lazyLen
        return classifyCurlyContent(inside, totalLen)
    }

    private fun classifyCurlyContent(inside: String, totalLen: Int): QuantifierInfo? {
        val commaIdx = inside.indexOf(',')
        return when {
            commaIdx < 0 ->
                // {n} — exact, bounded
                if (inside.isNotEmpty() && inside.all { it.isDigit() })
                    QuantifierInfo(totalLen, false) else null
            commaIdx == inside.length - 1 -> {
                // {n,} — at-least, unbounded
                val n = inside.dropLast(1)
                if (n.all { it.isDigit() }) QuantifierInfo(totalLen, true) else null
            }
            else -> classifyBoundedCurly(inside, totalLen)
        }
    }

    private fun classifyBoundedCurly(inside: String, totalLen: Int): QuantifierInfo? {
        val parts = inside.split(",")
        val valid = parts.size == 2 &&
            parts[0].all { it.isDigit() } &&
            parts[1].all { it.isDigit() }
        return if (valid) QuantifierInfo(totalLen, false) else null
    }

    // ── Body scan (inner unbounded detection) ─────────────────────────────────

    /**
     * Returns `true` if [body] (the group body string, without wrapping parens) contains
     * at least one unbounded quantifier applied to a token that can consume characters.
     *
     * Each token (escape sequence, char class, group, or single char) is examined; when a
     * quantifier follows it, the quantifier's characters are consumed so the loop does not
     * re-examine them as new tokens.
     *
     * Zero-width assertions (`^`, `$`, `\b`, `\B`, `\A`, `\z`, `\Z`) are excluded — they
     * consume no characters and cannot drive catastrophic backtracking.
     */
    @Suppress("ReturnCount")
    fun bodyContainsUnbounded(body: String): Boolean {
        var i = 0
        while (i < body.length) {
            val result = checkBodyToken(body, i)
            if (result.foundUnbounded) return true
            i = result.nextPos
        }
        return false
    }

    /**
     * Examines the token at position [i] in [body] and returns the next position to
     * continue scanning from, along with whether an unbounded quantifier was found.
     */
    private fun checkBodyToken(body: String, i: Int): ScanStep {
        return when {
            body[i] == '\\' -> checkEscapedToken(body, i)
            body[i] == '[' -> checkCharClassToken(body, i)
            body[i] == '(' -> checkNestedGroup(body, i)
            body[i] in "^$" -> ScanStep(i + 1, false) // zero-width anchor
            else -> checkSingleChar(body, i)
        }
    }

    private fun checkEscapedToken(body: String, i: Int): ScanStep {
        val next = if (i + 1 < body.length) body[i + 1] else ' '
        // Zero-width assertions consume no characters — skip them (and their quantifier, if any)
        val isZeroWidth = next in "bBAzZ"
        val tokenEnd = i + 2
        if (isZeroWidth) return ScanStep(tokenEnd, false)
        val q = readQuantifier(body, tokenEnd)
        return ScanStep(tokenEnd + (q?.length ?: 0), q != null && q.isUnbounded)
    }

    private fun checkCharClassToken(body: String, i: Int): ScanStep {
        val classEnd = skipCharClass(body, i)
        val q = readQuantifier(body, classEnd)
        return ScanStep(classEnd + (q?.length ?: 0), q != null && q.isUnbounded)
    }

    private fun checkNestedGroup(body: String, i: Int): ScanStep {
        val open = parseGroupOpen(body, i)
        val closeIdx = findGroupClose(body, open.bodyStart)
        if (closeIdx < 0) return ScanStep(i + 1, false)
        val afterClose = closeIdx + 1
        val q = readQuantifier(body, afterClose)
        // If the nested group has an outer unbounded quantifier, flag immediately.
        if (!open.isSafe && q != null && q.isUnbounded) {
            return ScanStep(afterClose + (q.length), true)
        }
        // Recurse into the nested body regardless.
        val nestedBody = body.substring(open.bodyStart, closeIdx)
        return ScanStep(afterClose + (q?.length ?: 0), bodyContainsUnbounded(nestedBody))
    }

    private fun checkSingleChar(body: String, i: Int): ScanStep {
        val q = readQuantifier(body, i + 1)
        return ScanStep(i + 1 + (q?.length ?: 0), q != null && q.isUnbounded)
    }

    // ── Character-class skipping ──────────────────────────────────────────────

    /**
     * Given that [i] points at `[` in [s], returns the index immediately after the matching
     * `]`, respecting `\]` escapes and the edge cases where `]` or `^]` at the very start
     * of a class are treated as literal `]` by most engines.
     * Returns `s.length` if no matching `]` is found.
     */
    private fun skipCharClass(s: String, i: Int): Int {
        var j = i + 1
        if (j < s.length && s[j] == '^') j++       // negated class [^...]
        if (j < s.length && s[j] == ']') j++       // literal ] as first member
        while (j < s.length) {
            when {
                s[j] == '\\' -> j += 2
                s[j] == ']' -> return j + 1
                else -> j++
            }
        }
        return s.length
    }
}

// ── Tiny result carrier ───────────────────────────────────────────────────────

/**
 * Result of scanning one token in the body: where to resume, and whether an unbounded
 * quantifier was found on that token.
 */
private data class ScanStep(val nextPos: Int, val foundUnbounded: Boolean)
