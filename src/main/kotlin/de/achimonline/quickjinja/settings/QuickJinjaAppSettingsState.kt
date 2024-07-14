package de.achimonline.quickjinja.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(
    name = "de.achimonline.quickjinja.settings.QuickJinjaAppSettingsState",
    storages = [Storage("QuickJinja.xml")]
)
class QuickJinjaAppSettingsState : PersistentStateComponent<QuickJinjaAppSettingsState?> {
    var settings = QuickJinjaAppSettings()

    override fun getState(): QuickJinjaAppSettingsState {
        return this
    }

    override fun loadState(state: QuickJinjaAppSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: QuickJinjaAppSettingsState
            get() = ApplicationManager.getApplication().getService(QuickJinjaAppSettingsState::class.java)
    }
}
