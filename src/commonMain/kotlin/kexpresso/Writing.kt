package kexpresso

/**
 * Appends a pattern that matches a single sentence.
 *
 * A sentence starts with a capital letter, contains one or more words separated
 * by single spaces, and ends with terminal punctuation (`.`, `!`, or `?`).
 *
 * Example match: `"Espresso is perfect!"`
 */
fun KexpressoBuilder.sentence(): KexpressoBuilder {
    uppercaseLetter()
    word()
    zeroOrMore { whitespace(); word() }
    endPunctuation()
    return this
}

/**
 * Appends a pattern that matches one or more sentences separated by single spaces.
 *
 * Example match: `"Latte is smooth. Espresso is strong!"`
 */
fun KexpressoBuilder.paragraph(): KexpressoBuilder {
    sentence()
    zeroOrMore { whitespace(); sentence() }
    return this
}
