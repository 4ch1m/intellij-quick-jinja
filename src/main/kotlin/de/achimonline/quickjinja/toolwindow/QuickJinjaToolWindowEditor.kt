package de.achimonline.quickjinja.toolwindow

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import java.io.File

class QuickJinjaToolWindowEditor(project: Project, isViewer: Boolean, placeHolder: String) : EditorTextField(
    EditorFactory.getInstance().createDocument(""), project, FileTypes.PLAIN_TEXT, isViewer, false) {
    private val myPlaceHolder = placeHolder

    constructor(project: Project, isViewer: Boolean) : this(project, isViewer, "")
    constructor(project: Project, placeHolder: String) : this(project, false, placeHolder)

    private fun getIdeDefaultMonospacedFontName(): String? {
        val fontPath = listOf(
                System.getProperty("java.home"),
                "lib",
                "fonts",
                "JetBrainsMono-Regular.ttf"
            ).joinToString(File.separator)

        return if (File(fontPath).exists()) {
            "JetBrains Mono Regular"
        } else {
            null
        }
    }

    override fun createEditor(): EditorEx {
        return super.createEditor().apply {
            colorsScheme.editorFontName = getIdeDefaultMonospacedFontName()
            setHorizontalScrollbarVisible(true)
            setVerticalScrollbarVisible(true)
            setPlaceholder(myPlaceHolder)
        }
    }
}
