package de.achimonline.quickjinja.parser

import com.google.gson.Gson
import com.google.gson.Strictness
import com.google.gson.internal.Streams
import com.google.gson.stream.JsonReader
import de.achimonline.quickjinja.helper.QuickJinjaHelper.Companion.createTempFileFromText
import java.io.File
import java.io.StringReader

class QuickJinjaParserJson : QuickJinjaParser {
    companion object {
        private val gson = Gson()
    }

    override fun parse(data: String): Any? {
        try {
            if (!data.trim().startsWith("{")) {
                throw Exception("JSON must start with curly braces.")
            }

            val stringReader = StringReader(data)
            val jsonReader = JsonReader(stringReader)
            jsonReader.strictness = Strictness.STRICT // explicitly set "strict mode"

            return Streams.parse(jsonReader)
        } catch (_: Exception) {
            return null
        }
    }

    override fun save(data: Any): File {
        return createTempFileFromText(gson.toJson(data))
    }
}
