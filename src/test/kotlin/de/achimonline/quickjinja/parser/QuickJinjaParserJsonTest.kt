package de.achimonline.quickjinja.parser

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

import kotlin.test.assertNull

class QuickJinjaParserJsonTest {
    private val parser = QuickJinjaParserJson()

    @Test
    fun parse() {
        listOf(
            "",
            " ",
            System.lineSeparator(),
            "\"foo\": \"bar\" }",
            "{ \"foo\": \"bar\"",
            "{ foo: \"bar\" }",
            "{ \"foo: \"bar\" }",
            "{ \"foo\": bar }",
            "{ \"foo\": }"
        ).forEach {
            assertNull(parser.parse(it))
        }

        assertNotNull("{ \"foo\": \"bar\" }")
    }

    @Test
    fun save() {
        val jsonString = "{\"foo\":\"bar\"}"
        val jsonObject = parser.parse(jsonString)
        val jsonFile = parser.save(jsonObject!!)

        assertEquals(jsonString, jsonFile.readText())
    }
}
