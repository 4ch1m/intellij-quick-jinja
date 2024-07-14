package de.achimonline.quickjinja.helper

import de.achimonline.quickjinja.process.QuickJinjaProcess
import de.achimonline.quickjinja.python.QuickJinjaPython
import java.io.File
import kotlin.io.path.createTempFile

class QuickJinjaHelper {
    companion object {
        fun createTempFileFromText(text: String): File {
            val file = createTempFile().toFile()
            file.writeText(text)
            file.deleteOnExit()

            return file
        }

        fun getPythonVersion(executablePath: String): QuickJinjaProcess.Result {
            return QuickJinjaProcess.run(
                executablePath, listOf("--version")
            )
        }

        fun renderTemplate(
            executablePath: String,
            template: String,
            variables: String? = null
        ): QuickJinjaProcess.Result {
            var variablesFile: File? = null

            if (variables != null) {
                variablesFile = createTempFileFromText(variables)
            }

            val pythonScript = QuickJinjaPython.createScriptFile(
                templateFile = createTempFileFromText(template),
                variablesFile = variablesFile
            )

            return QuickJinjaProcess.run(
                executable = executablePath,
                parameters = listOf(pythonScript.absolutePath)
            )
        }
    }
}
