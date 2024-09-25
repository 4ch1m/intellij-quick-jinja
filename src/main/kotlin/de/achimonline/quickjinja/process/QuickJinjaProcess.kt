package de.achimonline.quickjinja.process

import com.intellij.execution.configurations.GeneralCommandLine

class QuickJinjaProcess {
    data class Result(
        val rc: Int,
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

            val inputStream = process.inputStream.bufferedReader().use { it.readText() }
            val errorStream = process.errorStream.bufferedReader().use { it.readText() }

            process.waitFor()

            return Result(
                process.exitValue(),
                inputStream,
                errorStream
            )
        }
    }
}
