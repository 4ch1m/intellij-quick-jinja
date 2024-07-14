package de.achimonline.quickjinja.toolwindow

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField

class QuickJinjaToolWindowEditor(project: Project, isViewer: Boolean, placeHolder: String) : EditorTextField(
    EditorFactory.getInstance().createDocument(""), project, FileTypes.PLAIN_TEXT, isViewer, false) {
    private val myPlaceHolder = placeHolder

    constructor(project: Project, isViewer: Boolean) : this(project, isViewer, "")
    constructor(project: Project, placeHolder: String) : this(project, false, placeHolder)

    override fun createEditor(): EditorEx {
        return super.createEditor().apply {
            font = EditorFontType.getGlobalPlainFont()
            setHorizontalScrollbarVisible(true)
            setVerticalScrollbarVisible(true)
            setPlaceholder(myPlaceHolder)
        }
    }
}
