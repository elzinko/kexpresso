package kexpresso

/**
 * Opt-in marker for kexpresso APIs that may still evolve before 1.0.
 *
 * Symbols annotated with `@ExperimentalKexpressoApi` are useful and shipped, but their
 * signature, semantics, or output format may change before the SemVer 1.0 freeze. To use
 * them, either:
 *
 * - Annotate the call site with `@OptIn(ExperimentalKexpressoApi::class)`, or
 * - Propagate the marker by annotating the enclosing declaration with
 *   `@ExperimentalKexpressoApi`.
 *
 * The opt-in level is `WARNING`: code still compiles without the opt-in, but the compiler
 * surfaces a warning so you know you are taking on a stability risk.
 *
 * Typical experimental areas in kexpresso today: best-effort domain helpers
 * (`Domains.kt`), AST-based example generation (`examples`), the reverse engineering
 * surface (`Kexpresso.from`, `toKexpressoCode`), the ReDoS analyzer, and the
 * natural-language helpers in `Text.kt`/`Writing.kt`.
 *
 * Stable APIs — the builder DSL, `KexpressoPattern` core operations (`matches`,
 * `find`, `replaceAll`, etc.), and `describe()` over structured AST — are not marked.
 */
@RequiresOptIn(
    message =
        "This kexpresso API is experimental and may change before 1.0. " +
            "Opt in by adding @OptIn(ExperimentalKexpressoApi::class) or by propagating " +
            "@ExperimentalKexpressoApi on the enclosing declaration.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.TYPEALIAS,
)
public annotation class ExperimentalKexpressoApi
