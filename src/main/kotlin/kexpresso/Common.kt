package kexpresso

class CommonBuilder {
    val pattern = StringBuilder()

    fun space(): CommonBuilder {
        pattern.append("\\s")
        return this
    }

    fun digit(): CommonBuilder {
        pattern.append("\\d")
        return this
    }

    fun word(): CommonBuilder {
        pattern.append("[a-zA-Z0-9]+")
        return this
    }

    fun capitalLetter(): CommonBuilder {
        pattern.append("[A-Z]")
        return this
    }

    fun endPunctuation(): CommonBuilder {
        pattern.append("[.!?]")
        return this
    }

    fun zeroOrMore(block: CommonBuilder.() -> Unit): CommonBuilder {
        pattern.append("(?:")
        this.block()
        pattern.append(")*")
        return this
    }
}
