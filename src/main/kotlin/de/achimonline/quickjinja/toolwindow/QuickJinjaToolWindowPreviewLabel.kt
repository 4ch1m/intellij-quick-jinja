package de.achimonline.quickjinja.toolwindow

import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.JBUI
import java.awt.Color
import javax.swing.JLabel

class QuickJinjaToolWindowPreviewLabel(text: String): JLabel(text) {
    init {
        isOpaque = true
    }

    private fun setText(text: String?, backgroundColor: Color) {
        background = backgroundColor
        super.setText(text?.let { StringUtil.shortenTextWithEllipsis(it, 50, 3) })
    }

    fun setTextInfo(text: String?) {
        setText(text, JBUI.CurrentTheme.NotificationInfo.backgroundColor())
    }

    fun setTextError(text: String?) {
        setText(text, JBUI.CurrentTheme.NotificationError.backgroundColor())
    }

    fun setTextWarning(text: String?) {
        setText(text, JBUI.CurrentTheme.NotificationWarning.backgroundColor())
    }
}
