package de.achimonline.quickjinja.parser

import com.google.gson.FormattingStyle
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class QuickJinjaParserYamlTest {
    private val parser = QuickJinjaParserYaml()

    @Test
    fun parse() {
        listOf(
            "",
            " ",
            System.lineSeparator(),
            ":",
            " : bar",
            "foo: 'bar' 'baz'".trimIndent()
        ).forEach {
            assertNull(parser.parse(it))
        }

        assertNotNull(parser.parse("foo: bar"))

        assertNotNull(parser.parse("""
            foo:
                bar: >
                    baz
        """.trimIndent()))
    }

    @Test
    fun save() {
        val yamlString = "foo: bar"
        val yamlObject = parser.parse(yamlString)
        val yamlFile = parser.save(yamlObject!!)

        assertEquals(
            listOf(
                "{",
                "${FormattingStyle.PRETTY.indent}\"foo\": \"bar\"",
                "}"
            ).joinToString(FormattingStyle.PRETTY.newline),
            yamlFile.readText()
        )
    }
}
