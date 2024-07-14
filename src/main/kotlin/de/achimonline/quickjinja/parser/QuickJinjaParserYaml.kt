package de.achimonline.quickjinja.parser

import com.google.gson.GsonBuilder
import de.achimonline.quickjinja.helper.QuickJinjaHelper
import org.yaml.snakeyaml.Yaml
import java.io.File

class QuickJinjaParserYaml : QuickJinjaParser {
    companion object {
        private val yaml = Yaml()
    }

    override fun parse(data: String): Map<String, Any>? {
        return try {
            val yml: Map<String, Any> = yaml.load(data)
            return yml
        } catch (_: Exception) {
            null
        }
    }

    override fun save(data: Any): File {
        return QuickJinjaHelper.createTempFileFromText(
            GsonBuilder().setPrettyPrinting().create().toJson(data)
        )
    }
}
