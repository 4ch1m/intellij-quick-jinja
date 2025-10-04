package de.achimonline.quickjinja.settings

enum class TemplateSource {
    FILE,
    TEXT_SELECTION,
    CLIPBOARD
}

enum class ResultViewMode {
    PLAIN_TEXT,
    HTML
}

data class QuickJinjaProjectSettings(
    var templateFilePath: String = "",
    var templatePinned: Boolean = false,
    var variables: String = "",
    var loadVariablesFromFile: Boolean = false,
    var variablesFilePath: String = "",
    var resultViewMode: ResultViewMode = ResultViewMode.PLAIN_TEXT,
    var templateSource: TemplateSource = TemplateSource.FILE,
    var useCustomFilters: Boolean = false,
    var customFiltersFilePath: String = "",
    var useCustomTests: Boolean = false,
    var customTestsFilePath: String = ""
)
