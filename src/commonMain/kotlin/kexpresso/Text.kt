package kexpresso

/**
 * Appends a pattern that matches an alphanumeric word (`[a-zA-Z0-9]+`).
 *
 * "Word" here means one or more ASCII letters or digits — matching identifiers
 * like `Espresso`, `Cappuccino42`, etc.
 */
fun KexpressoBuilder.word(): KexpressoBuilder = append("[a-zA-Z0-9]+")

/**
 * Appends a pattern that matches a pseudo-identifier (`[a-zA-Z0-9_-]+`).
 *
 * Like [word] but also allows underscores and hyphens, covering usernames
 * and URL slugs such as `cold-brew_2024`.
 */
fun KexpressoBuilder.pseudo(): KexpressoBuilder = append("[a-zA-Z0-9_-]+")

/**
 * Appends a pattern that matches a broadly valid e-mail address.
 *
 * The pattern covers most common address forms, e.g. `barista@coffee.shop`.
 * It is intentionally permissive and not RFC-5321-complete.
 */
fun KexpressoBuilder.email(): KexpressoBuilder =
    append("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")

/**
 * Appends a pattern that matches an HTTP or HTTPS URL.
 *
 * Covers the common `https://example.com/path?query` form.
 * Very long or exotic URLs may not match.
 */
fun KexpressoBuilder.url(): KexpressoBuilder =
    append("https?://[^\\s/\$.?#].[^\\s]*")
