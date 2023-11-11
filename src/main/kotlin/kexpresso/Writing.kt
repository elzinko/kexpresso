package kexpresso

class WritingBuilder(private val textBuilder: TextBuilder) {
    fun sentence(): WritingBuilder {
        textBuilder.capitalLetter().word().zeroOrMore { space().word() }.endPunctuation()
        return this
    }

    fun paragraph(): WritingBuilder {
        textBuilder.zeroOrMore { sentence() }
        return this
    }

    fun space() = textBuilder.space()
    fun word() = textBuilder.word()
}
