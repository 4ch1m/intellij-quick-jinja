package de.achimonline.quickjinja.parser

import java.io.File

interface QuickJinjaParser {
    fun parse(data: String): Any?
    fun save(data: Any): File
    fun parseAndSave(data: String): File? {
        val parsedData = parse(data)

        return if (parsedData != null) {
            save(parsedData)
        } else {
            null
        }
    }
}
