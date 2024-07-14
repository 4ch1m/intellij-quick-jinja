package de.achimonline.quickjinja.process

import com.intellij.execution.configurations.GeneralCommandLine

class QuickJinjaProcess {
    data class Result(
        val returnCode: Int,
        val stdout: String,
        val stderr: String
    )

    companion object {
        fun run(executable: String, parameters: List<String>): Result {
            val process = GeneralCommandLine()
                .withEnvironment(System.getenv())
                .withExePath(executable)
                .withParameters(parameters)
                .createProcess()

            process.waitFor()

            return Result(
                process.exitValue(),
                process.inputStream.bufferedReader().use { it.readText() },
                process.errorStream.bufferedReader().use { it.readText() }
            )
        }
    }
}
