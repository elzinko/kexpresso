package kexpresso

import org.junit.Assert.assertTrue
import org.junit.Test

class CommonBuilderTest {

    @Test
    fun `should match a space`() {
        val pattern = CommonBuilder().space().compile()
        assertTrue(pattern.matches(" "))
        assertTrue(pattern.matches("\t"))
    }

    @Test
    fun `should match a digit`() {
        val pattern = CommonBuilder().digit().compile()
        assertTrue(pattern.matches("0"))
        assertTrue(pattern.matches("9"))
    }

    @Test
    fun `should match a word`() {
        val pattern = CommonBuilder().word().compile()
        assertTrue(pattern.matches("Espresso"))
        assertTrue(pattern.matches("Cappuccino"))
    }

    @Test
    fun `should match a capital letter`() {
        val pattern = CommonBuilder().capitalLetter().compile()
        assertTrue(pattern.matches("A"))
        assertTrue(pattern.matches("Z"))
    }

    @Test
    fun `should match end punctuation`() {
        val pattern = CommonBuilder().endPunctuation().compile()
        assertTrue(pattern.matches("."))
        assertTrue(pattern.matches("!"))
        assertTrue(pattern.matches("?"))
    }

    @Test
    fun `should match zero or more words`() {
        val pattern = CommonBuilder().zeroOrMore { word() }.compile()
        assertTrue(pattern.matches(""))
        assertTrue(pattern.matches("Espresso"))
        assertTrue(pattern.matches("Cappuccino Espresso Latte"))
    }

    private fun CommonBuilder.compile() = Regex(pattern.toString())
}
