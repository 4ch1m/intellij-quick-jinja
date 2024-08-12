package de.achimonline.quickjinja.toolwindow

import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.JBUI.CurrentTheme
import java.awt.Color
import javax.swing.JLabel

class QuickJinjaToolWindowPreviewLabel(text: String, type: Type = Type.INFO): JLabel(text) {
    enum class Type(val background: Color) {
        INFO(CurrentTheme.NotificationInfo.backgroundColor()),
        WARNING(CurrentTheme.NotificationWarning.backgroundColor()),
        ERROR(CurrentTheme.NotificationError.backgroundColor())
    }

    init {
        isOpaque = true
        background = type.background
    }

    fun setText(text: String?, type: Type) {
        background = type.background
        super.setText(text?.let { StringUtil.shortenTextWithEllipsis(it, 50, 3) })
    }
}
