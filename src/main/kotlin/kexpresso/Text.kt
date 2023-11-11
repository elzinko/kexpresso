package kexpresso

class TextBuilder(private val commonBuilder: CommonBuilder) {
    fun pseudo(): TextBuilder {
        commonBuilder.pattern.append("[a-zA-Z0-9_-]+")
        return this
    }

    fun email(): TextBuilder {
        commonBuilder.pattern.append("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        return this
    }

    fun zeroOrMore(block: CommonBuilder.() -> Unit): TextBuilder {
        commonBuilder.zeroOrMore { this.block() }
        return this
    }

    fun capitalLetter() = commonBuilder.capitalLetter()
    fun word() = commonBuilder.word()
    fun endPunctuation() = commonBuilder.endPunctuation()
    fun space() = commonBuilder.space()
}
