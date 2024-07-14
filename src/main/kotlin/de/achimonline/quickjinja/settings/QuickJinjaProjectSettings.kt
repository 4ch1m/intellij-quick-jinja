package de.achimonline.quickjinja.settings

data class QuickJinjaProjectSettings(
    var templateFilePath: String = "",
    var templatePinned: Boolean = false,
    var variables: String = "",
    var loadVariablesFromFile: Boolean = false,
    var variablesFilePath: String = ""
)
