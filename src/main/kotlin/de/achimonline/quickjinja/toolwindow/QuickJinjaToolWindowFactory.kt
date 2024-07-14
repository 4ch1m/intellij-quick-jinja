package de.achimonline.quickjinja.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.EditorTextField
import com.intellij.ui.GotItTooltip
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.not
import com.intellij.ui.layout.selected
import com.intellij.util.ui.EmptyIcon
import de.achimonline.quickjinja.bundle.QuickJinjaBundle.message
import de.achimonline.quickjinja.helper.QuickJinjaHelper.Companion.createTempFileFromText
import de.achimonline.quickjinja.parser.QuickJinjaParserJson
import de.achimonline.quickjinja.parser.QuickJinjaParserYaml
import de.achimonline.quickjinja.process.QuickJinjaProcess
import de.achimonline.quickjinja.python.QuickJinjaPython
import de.achimonline.quickjinja.settings.*
import java.awt.Component
import java.io.File
import javax.swing.Icon
import javax.swing.JCheckBox
import javax.swing.JLabel

@Suppress("DialogTitleCapitalization")
class QuickJinjaToolWindowFactory: ToolWindowFactory, DumbAware {
    private var editorFile: VirtualFile? = null

    private lateinit var tabPane: JBTabbedPane
    private lateinit var tabs: List<Tab>

    private lateinit var runButton: ActionButton

    private lateinit var templateFilePath: TextFieldWithBrowseButton

    private lateinit var variablesEditor: EditorTextField
    private lateinit var variablesEditorStatus: JLabel
    private lateinit var variablesLoadFromFile: JCheckBox
    private lateinit var variablesSelectedFile: TextFieldWithBrowseButton
    private lateinit var variablesFileStatus: JLabel

    private lateinit var resultViewer: EditorTextField
    private lateinit var resultStatus: JLabel

    private enum class TabId {
        VARIABLES,
        RESULT
    }

    private data class Tab (
        val id: TabId,
        val title: String,
        val icon: Icon,
        val component: Component,
        val tip: String
    )

    private enum class StatusIcon(val value: Icon) {
        OK(AllIcons.RunConfigurations.ToolbarPassed),
        ERROR(AllIcons.RunConfigurations.TestError),
        UNKNOWN(EmptyIcon.ICON_0)
    }

    private val fileDocumentManager = FileDocumentManager.getInstance()

    private lateinit var appSettings: QuickJinjaAppSettings
    private lateinit var projectSettings: QuickJinjaProjectSettings

    private val parsers = listOf(QuickJinjaParserJson(), QuickJinjaParserYaml())

    init {
        // subscribe to the messagebus and store info about the current editor file
        ApplicationManager
            .getApplication()
            .messageBus
            .connect()
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    editorFile = event.newFile

                    if (::projectSettings.isInitialized) {
                        if (!projectSettings.templatePinned) {
                            if (editorFile != null) {
                                templateFilePath.text = editorFile!!.path
                            }
                        }
                    }
                }
            })
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        appSettings = QuickJinjaAppSettingsState.instance.settings
        projectSettings = QuickJinjaProjectSettingsState.getInstance(project).settings

        tabPane = JBTabbedPane()

        tabs = listOf(
            Tab(
                TabId.VARIABLES,
                message("toolwindow.tab.variables"),
                AllIcons.Debugger.VariablesTab,
                panel {
                    val valuesEditorRow = row {
                        variablesEditor = cell(QuickJinjaToolWindowEditor(project, message("toolwindow.tab.variables.editor.placeholder")))
                            .resizableColumn()
                            .align(Align.FILL)
                            .applyToComponent {
                                text = projectSettings.variables

                                addDocumentListener(object : DocumentListener {
                                    override fun documentChanged(event: DocumentEvent) {
                                        validateVariables()
                                        projectSettings.variables = variablesEditor.text
                                    }
                                })
                            }.component

                        variablesEditorStatus = icon(StatusIcon.UNKNOWN.value)
                            .align(AlignY.TOP)
                            .component
                    }.resizableRow()

                    row {
                        variablesLoadFromFile = checkBox(message("toolwindow.tab.variables.file.checkbox"))
                            .applyToComponent {
                                isSelected = projectSettings::loadVariablesFromFile.get()
                            }
                            .onChanged {
                                projectSettings.loadVariablesFromFile = variablesLoadFromFile.isSelected

                                if (variablesLoadFromFile.isSelected) {
                                    validateVariables()
                                }
                            }.component

                        variablesSelectedFile = textFieldWithBrowseButton(message("toolwindow.tab.variables.file.browse.dialog.title"), project)
                            .align(AlignX.FILL)
                            .resizableColumn()
                            .enabledIf(variablesLoadFromFile.selected)
                            .applyToComponent {
                                isEditable = false
                                text = projectSettings.variablesFilePath
                            }
                            .onChanged {
                                if (!File(variablesSelectedFile.text).exists()) {
                                    variablesFileStatus.icon = StatusIcon.ERROR.value
                                } else {
                                    validateVariables()
                                }

                                projectSettings.variablesFilePath = variablesSelectedFile.text
                            }.component

                        valuesEditorRow.enabledIf(variablesLoadFromFile.selected.not())

                        variablesFileStatus = icon(StatusIcon.UNKNOWN.value)
                            .enabledIf(variablesLoadFromFile.selected)
                            .align(AlignX.LEFT)
                            .component
                    }
                },
                message("toolwindow.tab.variables.tip")
            ),
            Tab(
                TabId.RESULT,
                message("toolwindow.tab.result"),
                AllIcons.Actions.Preview,
                panel {
                    row {
                        resultViewer = cell(QuickJinjaToolWindowEditor(project, true))
                            .resizableColumn()
                            .align(Align.FILL)
                            .component

                        resultStatus = icon(StatusIcon.UNKNOWN.value)
                            .align(AlignY.TOP)
                            .component
                    }.resizableRow()
                },
                message("toolwindow.tab.result.tip")
            )
        )

        val pane = panel {
            row {
                runButton = actionButton(object : DumbAwareAction(
                    message("toolwindow.action.text"),
                    message("toolwindow.action.description"),
                    AllIcons.Debugger.ThreadRunning
                ) {
                    override fun actionPerformed(e: AnActionEvent) {
                        run()
                    }
                }).component

                label(message("toolwindow.template"))

                templateFilePath = textFieldWithBrowseButton(
                    message("toolwindow.template.file.browse.dialog.title"),
                    project
                ).applyToComponent {
                    isEditable = false
                    text = projectSettings.templateFilePath
                }.onChanged {
                    projectSettings.templateFilePath = templateFilePath.text
                }
                    .resizableColumn()
                    .align(AlignX.FILL)
                    .component

                actionButton(object : ToggleAction(
                    message("toolwindow.template.pin.text"),
                    message("toolwindow.template.pin.description"),
                    AllIcons.Actions.PinTab
                ) {
                    override fun isDumbAware(): Boolean {
                        return true
                    }

                    override fun isSelected(e: AnActionEvent): Boolean {
                        return projectSettings.templatePinned
                    }

                    override fun setSelected(e: AnActionEvent, state: Boolean) {
                        projectSettings.templatePinned = state

                        if (!state) {
                            templateFilePath.text = editorFile?.path ?: ""
                        }
                    }

                    override fun getActionUpdateThread(): ActionUpdateThread {
                        return ActionUpdateThread.EDT
                    }
                })
            }

            row {
                cell(tabPane)
                    .resizableColumn()
                    .align(Align.FILL)
                    .component
            }.resizableRow()
        }

        tabs.forEachIndexed { index, tab ->
            tabPane.insertTab(
                tab.title,
                tab.icon,
                tab.component,
                tab.tip,
                index
            )
        }

        toolWindow.contentManager.addContent(
            ContentFactory
                .getInstance()
                .createContent(pane, null, false)
        )

        val gotItTooltip = createGotItTooltip(pane)

        if (gotItTooltip.canShow()) {
            invokeLater {
                gotItTooltip.show(runButton, GotItTooltip.BOTTOM_MIDDLE)
            }
        }

        validateVariables() // manually trigger validation after toolwindow creation
    }

    private fun getVariablesData(): String {
        return if (variablesLoadFromFile.isSelected) {
            val variablesFile = File(variablesSelectedFile.text)

            if (variablesFile.exists()) {
                // check if the selected variables file is among the unsaved documents; if so get the cached content
                getContentOfUnsavedFile(variablesSelectedFile.text) ?: variablesFile.readText()
            } else {
                ""
            }
        } else {
            variablesEditor.text
        }
    }

    private fun validateVariables(): Boolean {
        val variablesStatusIcon = if (variablesLoadFromFile.isSelected) variablesFileStatus else variablesEditorStatus

        if (variablesLoadFromFile.isSelected) {
            if (!File(variablesSelectedFile.text).exists()) {
                variablesStatusIcon.icon = StatusIcon.ERROR.value
                return false
            }
        }

        val variablesData = getVariablesData()

        if (variablesData.trim().isEmpty()) {
            variablesStatusIcon.icon = StatusIcon.UNKNOWN.value
        } else {
            variablesStatusIcon.icon = StatusIcon.OK.value

            try {
                parsers.firstNotNullOf { parser ->
                    parser.parse(variablesData)
                }
            } catch(nse: NoSuchElementException) {
                variablesStatusIcon.icon = StatusIcon.ERROR.value
                return false
            }
        }

        return true
    }

    private fun generateFileFromVariables(): File? {
        val variablesData = getVariablesData()

        if (variablesData.trim().isEmpty()) {
            return null
        }

        return try {
            parsers.firstNotNullOf { parser ->
                parser.parseAndSave(variablesData)
            }
        } catch(nse: NoSuchElementException) {
            null
        }
    }

    private fun getContentOfUnsavedFile(filePath: String): String? {
        val unsavedDoc = fileDocumentManager.unsavedDocuments.firstOrNull { unsavedDocument ->
            fileDocumentManager.getFile(unsavedDocument)!!.path == filePath
        }

        if (unsavedDoc != null) {
            return unsavedDoc.text
        }

        return null
    }

    private fun createTempFileFromCachedDocument(virtualFile: VirtualFile): File {
        val cachedDocument = fileDocumentManager.getCachedDocument(virtualFile)
        return createTempFileFromText(cachedDocument!!.text)
    }

    private fun run() {
        if (editorFile == null || templateFilePath.text.trim().isEmpty()) {
            return
        }

        val templateFile: File = if (templateFilePath.text == editorFile!!.path) {
            // the chosen file actually is the currently opened editor file; get the up-to-date content from cache
            createTempFileFromCachedDocument(editorFile!!)
        } else {
            // check if chosen file is amongst the unsaved documents; if so, then also get the cached content
            val contentOfUnsavedFile = getContentOfUnsavedFile(templateFilePath.text)
            if (contentOfUnsavedFile != null) {
                createTempFileFromText(contentOfUnsavedFile)
            } else {
                File(templateFilePath.text)
            }
        }

        val pythonScript = QuickJinjaPython.createScriptFile(
            templateFile = templateFile,
            variablesFile = generateFileFromVariables(),
            options = appSettings.getJinjaOptions()
        )

        val processResult = QuickJinjaProcess.run(appSettings.executable, listOf(pythonScript.absolutePath))

        val result: String

        if (processResult.returnCode == 0) {
            resultStatus.icon = StatusIcon.OK.value
            result = processResult.stdout
        } else {
            resultStatus.icon = StatusIcon.ERROR.value
            result = listOf(processResult.stdout, processResult.stderr).joinToString(System.lineSeparator()).trim()
        }

        resultViewer.text = result

        tabPane.selectedIndex = tabs.indexOf(
            tabs.find {
                it.id == TabId.RESULT
            }
        )
    }

    private fun createGotItTooltip(parentComponent: Component): GotItTooltip {
        return GotItTooltip(
            "de.achimonline.quickjinja.gotit.setup",
            message("gotitpopup.setup")
        ).withIcon(AllIcons.General.Information)
            .withLink(message("gotitpopup.setup.link")) {
                ShowSettingsUtil.getInstance().editConfigurable(parentComponent, QuickJinjaAppSettingsConfigurable())
            }
    }
}
