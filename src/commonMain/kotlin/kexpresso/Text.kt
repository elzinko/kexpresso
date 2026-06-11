package kexpresso

/**
 * Appends a pattern that matches an alphanumeric word (`[a-zA-Z0-9]+`).
 *
 * "Word" here means one or more ASCII letters or digits — matching identifiers
 * like `Espresso`, `Cappuccino42`, etc.
 *
 * **Not the same as [KexpressoBuilder.wordChar]**: `word()` matches *one or more*
 * `[a-zA-Z0-9]`, while `wordChar()` matches a single `\w` character (which also
 * includes the underscore `_`). Use [handle] for one-or-more `[a-zA-Z0-9_-]+` (the
 * username/slug shape).
 */
fun KexpressoBuilder.word(): KexpressoBuilder = append("[a-zA-Z0-9]+")

/**
 * Appends a pattern that matches a handle (`[a-zA-Z0-9_-]+`).
 *
 * Like [word] but also allows underscores and hyphens. Covers GitHub/Twitter
 * usernames and URL slugs such as `cold-brew_2024`, `octo-cat`, `barista_42`.
 */
fun KexpressoBuilder.handle(): KexpressoBuilder = append("[a-zA-Z0-9_-]+")

/**
 * Alias for [handle]; matches a handle / pseudo-identifier (`[a-zA-Z0-9_-]+`).
 *
 * @suppress
 */
@Deprecated(
    message = "Use handle() instead — the standard term for `[a-zA-Z0-9_-]+` " +
        "(usernames, slugs). pseudo() will be removed in 1.0.",
    replaceWith = ReplaceWith("handle()"),
    level = DeprecationLevel.WARNING,
)
fun KexpressoBuilder.pseudo(): KexpressoBuilder = handle()

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
