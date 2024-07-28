package de.achimonline.quickjinja.settings

data class QuickJinjaAppSettings(
    var executable: String = "python3",
    var htmlResultViewerBackgroundColor: String = "#ffffff",

    // Jinja options ...
    var blockStartString: String = "{%",
    var blockEndString: String = "%}",
    var variableStartString: String = "{{",
    var variableEndString: String = "}}",
    var commentStartString: String = "{#",
    var commentEndString: String = "#}",
    var lineStatementPrefix: String = "",
    var lineCommentPrefix: String = "",
    var newlineSequence: String = "\\n",
    var trimBlocks: Boolean = false,
    var lstripBlocks: Boolean = false,
    var keepTrailingNewline: Boolean = false,
    var autoEscape: Boolean = false
) {
    fun getJinjaOptions(): Map<String, Any> {
        return mapOf(
            "block_start_string" to blockStartString,
            "block_end_string" to blockEndString,
            "variable_start_string" to variableStartString,
            "variable_end_string" to variableEndString,
            "comment_start_string" to commentStartString,
            "comment_end_string" to commentEndString,
            "line_statement_prefix" to lineStatementPrefix,
            "line_comment_prefix" to lineCommentPrefix,
            "newline_sequence" to newlineSequence,
            "trim_blocks" to trimBlocks,
            "lstrip_blocks" to lstripBlocks,
            "keep_trailing_newline" to keepTrailingNewline,
            "autoescape" to autoEscape
        )
    }
}
