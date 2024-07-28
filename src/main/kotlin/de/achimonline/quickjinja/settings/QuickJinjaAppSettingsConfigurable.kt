package de.achimonline.quickjinja.settings

import com.intellij.icons.AllIcons
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.dsl.builder.*
import de.achimonline.quickjinja.bundle.QuickJinjaBundle.message
import de.achimonline.quickjinja.helper.QuickJinjaHelper
import de.achimonline.quickjinja.process.QuickJinjaProcess
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JEditorPane
import javax.swing.JLabel

@Suppress("DialogTitleCapitalization")
class QuickJinjaAppSettingsConfigurable : BoundConfigurable(message("settings.display.name")){
    private val settings
        get() = QuickJinjaAppSettingsState.instance.settings

    private lateinit var executable: TextFieldWithBrowseButton
    private lateinit var testButton: JButton

    private val heartIcon = IconLoader.getIcon("/icons/heart-solid.svg", QuickJinjaAppSettingsConfigurable::class.java)

    private enum class TestStatusIcon(val value: Icon) {
        UNKNOWN(AllIcons.RunConfigurations.TestUnknown),
        SUCCESS(AllIcons.General.InspectionsOK),
        FAILURE(AllIcons.General.NotificationError)
    }

    private var showTestResults = AtomicBooleanProperty(false)

    private lateinit var testResultCommentPython: JEditorPane
    private lateinit var testResultStatusPython: JLabel

    private lateinit var testResultCommentSimpleTemplate: JEditorPane
    private lateinit var testResultStatusSimpleTemplate: JLabel

    private lateinit var testResultCommentAnsibleFilters: JEditorPane
    private lateinit var testResultStatusAnsibleFilters: JLabel

    override fun createPanel(): DialogPanel {
        return panel {
            group(message("settings.group.executable")) {
                row {
                    executable = textFieldWithBrowseButton()
                        .resizableColumn()
                        .align(AlignX.FILL)
                        .label(message("settings.group.executable.command"))
                        .comment(message("settings.group.executable.command.comment", "https://github.com/4ch1m/intellij-quick-jinja"))
                        .bindText(settings::executable)
                        .component

                    testButton = button(message("settings.group.executable.test")) {
                        executeTest()
                    }.component
                }

                indent {
                    row {
                        label(message("settings.group.executable.test.python"))

                        testResultCommentPython = comment("").component
                        testResultStatusPython = icon(TestStatusIcon.UNKNOWN.value).component
                    }
                        .topGap(TopGap.SMALL)
                        .layout(RowLayout.PARENT_GRID)
                        .visibleIf(showTestResults)

                    row {
                        label(message("settings.group.executable.test.simple-template"))

                        testResultCommentSimpleTemplate = comment("").component
                        testResultStatusSimpleTemplate = icon(TestStatusIcon.UNKNOWN.value).component
                    }
                        .layout(RowLayout.PARENT_GRID)
                        .visibleIf(showTestResults)

                    row {
                        label(message("settings.group.executable.test.ansible-filters"))

                        testResultCommentAnsibleFilters = comment("").component
                        testResultStatusAnsibleFilters = icon(TestStatusIcon.UNKNOWN.value).component
                    }
                        .layout(RowLayout.PARENT_GRID)
                        .visibleIf(showTestResults)
                }

            }

            group(message("settings.group.jinja.options")) {
                row {
                    browserLink(message("settings.group.jinja.options.online.documentation"), "https://jinja.palletsprojects.com/en/latest/api/#jinja2.Environment")
                }.bottomGap(BottomGap.SMALL)

                twoColumnsRow(
                    {
                        label(message("settings.group.jinja.options.block_start_string"))
                        textField().align(AlignX.RIGHT).bindText(settings::blockStartString)
                    },
                    {
                        label(message("settings.group.jinja.options.block_end_string"))
                        textField().align(AlignX.RIGHT).bindText(settings::blockEndString)
                    }
                )

                twoColumnsRow(
                    {
                        label(message("settings.group.jinja.options.variable_start_string"))
                        textField().align(AlignX.RIGHT).bindText(settings::variableStartString)
                    },
                    {
                        label(message("settings.group.jinja.options.variable_end_string"))
                        textField().align(AlignX.RIGHT).bindText(settings::variableEndString)
                    }
                ).layout(RowLayout.PARENT_GRID)

                twoColumnsRow(
                    {
                        label(message("settings.group.jinja.options.comment_start_string"))
                        textField().align(AlignX.RIGHT).bindText(settings::commentStartString)
                    },
                    {
                        label(message("settings.group.jinja.options.comment_end_string"))
                        textField().align(AlignX.RIGHT).bindText(settings::commentEndString)
                    }
                ).layout(RowLayout.PARENT_GRID)

                twoColumnsRow(
                    {
                        label(message("settings.group.jinja.options.line_statement_prefix"))
                        textField().align(AlignX.RIGHT).bindText(settings::lineStatementPrefix)
                    },
                    {
                        label(message("settings.group.jinja.options.line_comment_prefix"))
                        textField().align(AlignX.RIGHT).bindText(settings::lineCommentPrefix)
                    }
                ).layout(RowLayout.PARENT_GRID)

                twoColumnsRow(
                    {
                        label(message("settings.group.jinja.options.newline_sequence"))
                        textField().align(AlignX.RIGHT).bindText(settings::newlineSequence)
                    }
                ).layout(RowLayout.PARENT_GRID).bottomGap(BottomGap.SMALL)

                row {
                    checkBox(message("settings.group.jinja.options.trim_blocks"))
                        .comment(message("settings.group.jinja.options.trim_blocks.comment"))
                        .bindSelected(settings::trimBlocks)
                }

                row {
                    checkBox(message("settings.group.jinja.options.lstrip_blocks")).bindSelected(settings::lstripBlocks)
                }

                row {
                    checkBox(message("settings.group.jinja.options.keep_trailing_newline")).bindSelected(settings::keepTrailingNewline)
                }

                row {
                    checkBox(message("settings.group.jinja.options.autoescape")).bindSelected(settings::autoEscape)
                }
            }

            group(message("settings.group.miscellaneous")) {
                row {
                    val hexColorRegex = Regex("^#([a-f]|[A-F]|[0-9]){3}(([a-f]|[A-F]|[0-9]){3})?$")

                    textField()
                        .label(message("settings.group.miscellaneous.html-resultviewer.backgroundcolor"))
                        .comment(message("settings.group.miscellaneous.html-resultviewer.backgroundcolor.comment"))
                        .validationOnInput {
                            when {
                                hexColorRegex.matches(it.text.trim()) -> { null }
                                else -> { error(message("settings.group.miscellaneous.html-resultviewer.backgroundcolor.error")) }
                            }
                        }
                        .bindText(settings::htmlResultViewerBackgroundColor)
                }
            }

            group {
                row {
                    icon(heartIcon)
                    text(message("settings.donation", "https://paypal.me/AchimSeufert"))
                }
            }
        }
    }

    private fun executeTest() {
        testButton.isEnabled = false

        showTestResults.set(true)

        testResultCommentPython.text = ""
        testResultStatusPython.icon = TestStatusIcon.UNKNOWN.value

        testResultCommentAnsibleFilters.text = ""
        testResultStatusSimpleTemplate.icon = TestStatusIcon.UNKNOWN.value

        testResultCommentSimpleTemplate.text = ""
        testResultStatusAnsibleFilters.icon = TestStatusIcon.UNKNOWN.value

        if (testPython()) {
            if (testSimpleTemplate()) {
                testAnsibleFilter()
            }
        }

        testButton.isEnabled = true
    }

    private fun executableTestProcessor(
        result: QuickJinjaProcess.Result,
        resultCondition: (String) -> Boolean,
        comment: JEditorPane,
        icon: JLabel,
        commentFormatter: (String) -> String = { input: String -> input }
    ): Boolean {
        val success = result.returnCode == 0 && resultCondition(result.stdout)

        if (success) {
            icon.icon = TestStatusIcon.SUCCESS.value
            comment.text = commentFormatter(result.stdout.trim())
        } else {
            icon.icon = TestStatusIcon.FAILURE.value
            comment.text = ""
        }

        return success
    }

    private fun testPython(): Boolean {
        return executableTestProcessor(
            result = QuickJinjaHelper.getPythonVersion(executable.text),
            resultCondition = { input: String -> input.lowercase().contains("python") },
            comment = testResultCommentPython,
            icon = testResultStatusPython
        )
    }

    private fun testSimpleTemplate(): Boolean {
        return executableTestProcessor(
            result = QuickJinjaHelper.renderTemplate(
                executablePath = executable.text,
                template = "Hello {{ name }}!",
                variables = "{ \"name\": \"Jinja\" }"
            ),
            resultCondition = { input: String -> input.trim() == "Hello Jinja!" },
            comment = testResultCommentSimpleTemplate,
            icon = testResultStatusSimpleTemplate
        )
    }

    private fun testAnsibleFilter(): Boolean {
        return executableTestProcessor(
            result = QuickJinjaHelper.renderTemplate(
                executablePath = executable.text,
                template = "{{ \"/foo/bar/test.txt\" | basename }}"
            ),
            resultCondition = { input: String -> input.trim() == "test.txt" },
            comment = testResultCommentAnsibleFilters,
            icon = testResultStatusAnsibleFilters,
            commentFormatter = { "| basename" }
        )
    }
}
