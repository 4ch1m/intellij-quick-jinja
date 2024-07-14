package de.achimonline.quickjinja.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.PROJECT)
@State(
    name = "de.achimonline.quickjinja.settings.QuickJinjaProjectSettingsState",
    storages = [Storage("QuickJinja_project.xml")]
)
class QuickJinjaProjectSettingsState : PersistentStateComponent<QuickJinjaProjectSettingsState?> {
    var settings = QuickJinjaProjectSettings()

    override fun getState(): QuickJinjaProjectSettingsState {
        return this
    }

    override fun loadState(state: QuickJinjaProjectSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(project: Project): QuickJinjaProjectSettingsState =
            project.getService(QuickJinjaProjectSettingsState::class.java)
    }
}
