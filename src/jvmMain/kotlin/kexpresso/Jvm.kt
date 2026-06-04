package kexpresso

/**
 * Returns a [java.util.regex.Pattern] equivalent to this pattern.
 *
 * This is a **JVM-only** extension: [java.util.regex.Pattern] does not exist on Kotlin/JS,
 * so this conversion is unavailable on non-JVM targets.
 */
fun KexpressoPattern.toPattern(): java.util.regex.Pattern = regex.toPattern()
