package kexpresso

/**
 * Typed, ergonomic accessor over a [MatchResult]'s captured groups (by name or index).
 *
 * Obtain an instance via the [MatchResult.captures] extension property:
 * ```kotlin
 * val pattern = kexpresso {
 *     capture("year") { exactly(4) { digit() } }
 *     literal("-")
 *     capture("month") { exactly(2) { digit() } }
 * }
 * val caps = pattern.find("2026-06")?.captures
 * caps?.int("year")   // 2026
 * caps?.string("month") // "06"
 * ```
 */
class Captures internal constructor(private val match: MatchResult) {

    // ── private helpers ──────────────────────────────────────────────────────

    /**
     * Returns the raw string value of the named group [name], or `null` if the group
     * is absent, unmatched, not available on this platform, or if [name] is not a
     * recognised group name (the JVM collection throws [IllegalArgumentException] in that case).
     */
    private fun rawByName(name: String): String? = try {
        (match.groups as? MatchNamedGroupCollection)?.get(name)?.value
    } catch (_: IllegalArgumentException) {
        null
    }

    /**
     * Returns the raw string value of the group at [index], or `null` if the group is
     * absent, unmatched, or [index] is out of bounds.
     */
    private fun rawByIndex(index: Int): String? = try {
        match.groups[index]?.value
    } catch (_: IndexOutOfBoundsException) {
        null
    }

    // ── by name (nullable) ───────────────────────────────────────────────────

    /**
     * Returns the string value of the named group [name], or `null` if the group is absent,
     * unmatched, or the platform does not support named group access.
     *
     * Example:
     * ```kotlin
     * caps.string("drink") // "Espresso"
     * caps.string("missing") // null
     * ```
     */
    fun string(name: String): String? = rawByName(name)

    /**
     * Returns the value of the named group [name] parsed as [Int], or `null` if the group
     * is absent, unmatched, or its value cannot be parsed as an integer.
     *
     * Example:
     * ```kotlin
     * caps.int("year") // 2026
     * caps.int("label") // null  (not a number)
     * ```
     */
    fun int(name: String): Int? = rawByName(name)?.toIntOrNull()

    /**
     * Returns the value of the named group [name] parsed as [Long], or `null` if the group
     * is absent, unmatched, or its value cannot be parsed as a long.
     *
     * Example:
     * ```kotlin
     * caps.long("timestamp") // 1748908800000L
     * ```
     */
    fun long(name: String): Long? = rawByName(name)?.toLongOrNull()

    /**
     * Returns the value of the named group [name] parsed as [Double], or `null` if the group
     * is absent, unmatched, or its value cannot be parsed as a double.
     *
     * Example:
     * ```kotlin
     * caps.double("price") // 3.5
     * ```
     */
    fun double(name: String): Double? = rawByName(name)?.toDoubleOrNull()

    /**
     * Returns the value of the named group [name] parsed as [Boolean] using strict parsing
     * (`"true"` → `true`, `"false"` → `false`, anything else → `null`), or `null` if the
     * group is absent or unmatched.
     *
     * Example:
     * ```kotlin
     * caps.boolean("decaf") // true  (when group value is "true")
     * caps.boolean("decaf") // null  (when group value is "yes")
     * ```
     */
    fun boolean(name: String): Boolean? = rawByName(name)?.toBooleanStrictOrNull()

    // ── by index (nullable) ──────────────────────────────────────────────────

    /**
     * Returns the string value of the group at [index], or `null` if the group is absent or
     * unmatched. Index `0` is the whole match; `1` is the first capturing group, etc.
     *
     * Example:
     * ```kotlin
     * caps.string(0) // whole match
     * caps.string(1) // first capturing group
     * ```
     */
    fun string(index: Int): String? = rawByIndex(index)

    /**
     * Returns the value of the group at [index] parsed as [Int], or `null` if the group is
     * absent, unmatched, or its value cannot be parsed as an integer.
     *
     * Example:
     * ```kotlin
     * caps.int(1) // 42  (when first group captured "42")
     * ```
     */
    fun int(index: Int): Int? = rawByIndex(index)?.toIntOrNull()

    /**
     * Returns the value of the group at [index] parsed as [Long], or `null` if the group is
     * absent, unmatched, or its value cannot be parsed as a long.
     *
     * Example:
     * ```kotlin
     * caps.long(2) // 1748908800000L
     * ```
     */
    fun long(index: Int): Long? = rawByIndex(index)?.toLongOrNull()

    /**
     * Returns the value of the group at [index] parsed as [Double], or `null` if the group is
     * absent, unmatched, or its value cannot be parsed as a double.
     *
     * Example:
     * ```kotlin
     * caps.double(1) // 3.5
     * ```
     */
    fun double(index: Int): Double? = rawByIndex(index)?.toDoubleOrNull()

    /**
     * Returns the value of the group at [index] parsed as [Boolean] using strict parsing
     * (`"true"` → `true`, `"false"` → `false`, anything else → `null`), or `null` if the
     * group is absent or unmatched.
     *
     * Example:
     * ```kotlin
     * caps.boolean(1) // false  (when group value is "false")
     * ```
     */
    fun boolean(index: Int): Boolean? = rawByIndex(index)?.toBooleanStrictOrNull()

    // ── OrThrow variants (name only) ─────────────────────────────────────────

    /**
     * Returns the string value of the named group [name], or throws [NoSuchElementException]
     * if the group is absent or unmatched.
     *
     * Example:
     * ```kotlin
     * caps.stringOrThrow("drink") // "Espresso"
     * caps.stringOrThrow("missing") // throws NoSuchElementException
     * ```
     *
     * @throws NoSuchElementException if the group [name] is absent or unmatched.
     */
    fun stringOrThrow(name: String): String =
        rawByName(name) ?: throw NoSuchElementException("Named group '$name' is absent or unmatched.")

    /**
     * Returns the value of the named group [name] parsed as [Int], or throws if the group is
     * absent or its value cannot be parsed.
     *
     * Example:
     * ```kotlin
     * caps.intOrThrow("year") // 2026
     * caps.intOrThrow("label") // throws NumberFormatException
     * ```
     *
     * @throws NoSuchElementException if the group [name] is absent or unmatched.
     * @throws NumberFormatException if the group value cannot be parsed as an integer.
     */
    fun intOrThrow(name: String): Int {
        val raw = stringOrThrow(name)
        return raw.toIntOrNull()
            ?: throw NumberFormatException("Named group '$name' value \"$raw\" cannot be parsed as Int.")
    }

    /**
     * Returns the value of the named group [name] parsed as [Long], or throws if the group is
     * absent or its value cannot be parsed.
     *
     * Example:
     * ```kotlin
     * caps.longOrThrow("timestamp") // 1748908800000L
     * ```
     *
     * @throws NoSuchElementException if the group [name] is absent or unmatched.
     * @throws NumberFormatException if the group value cannot be parsed as a long.
     */
    fun longOrThrow(name: String): Long {
        val raw = stringOrThrow(name)
        return raw.toLongOrNull()
            ?: throw NumberFormatException("Named group '$name' value \"$raw\" cannot be parsed as Long.")
    }

    /**
     * Returns the value of the named group [name] parsed as [Double], or throws if the group
     * is absent or its value cannot be parsed.
     *
     * Example:
     * ```kotlin
     * caps.doubleOrThrow("price") // 3.5
     * ```
     *
     * @throws NoSuchElementException if the group [name] is absent or unmatched.
     * @throws NumberFormatException if the group value cannot be parsed as a double.
     */
    fun doubleOrThrow(name: String): Double {
        val raw = stringOrThrow(name)
        return raw.toDoubleOrNull()
            ?: throw NumberFormatException("Named group '$name' value \"$raw\" cannot be parsed as Double.")
    }

    /**
     * Returns the value of the named group [name] parsed as [Boolean] (strict: `"true"` or
     * `"false"` only), or throws if the group is absent or its value is not a valid boolean.
     *
     * Example:
     * ```kotlin
     * caps.booleanOrThrow("decaf") // true
     * caps.booleanOrThrow("decaf") // throws IllegalArgumentException when value is "yes"
     * ```
     *
     * @throws NoSuchElementException if the group [name] is absent or unmatched.
     * @throws IllegalArgumentException if the group value is not `"true"` or `"false"`.
     */
    fun booleanOrThrow(name: String): Boolean {
        val raw = stringOrThrow(name)
        return raw.toBooleanStrictOrNull()
            ?: throw IllegalArgumentException(
                "Named group '$name' value \"$raw\" cannot be parsed as Boolean (expected \"true\" or \"false\").",
            )
    }
}

/**
 * Returns a [Captures] accessor for this [MatchResult]'s captured groups.
 *
 * This is the primary entry point for the typed captures API:
 * ```kotlin
 * val pattern = kexpresso {
 *     capture("year")  { exactly(4) { digit() } }
 *     literal("-")
 *     capture("month") { exactly(2) { digit() } }
 *     literal("-")
 *     capture("day")   { exactly(2) { digit() } }
 * }
 * val caps = pattern.find("2026-06-03")?.captures
 * caps?.int("year")   // 2026
 * caps?.int("month")  // 6
 * caps?.int("day")    // 3
 * ```
 */
val MatchResult.captures: Captures get() = Captures(this)
