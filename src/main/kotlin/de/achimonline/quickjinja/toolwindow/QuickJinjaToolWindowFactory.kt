package de.achimonline.quickjinja.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ide.CopyPasteManager.ContentChangedListener
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.openapi.wm.ex.ToolWindowManagerListener.ToolWindowManagerEventType.HideToolWindow
import com.intellij.ui.EditorTextField
import com.intellij.ui.GotItTooltip
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.intellij.ui.layout.not
import com.intellij.ui.layout.selected
import de.achimonline.quickjinja.bundle.QuickJinjaBundle.message
import de.achimonline.quickjinja.helper.QuickJinjaHelper.Companion.createTempFileFromText
import de.achimonline.quickjinja.parser.QuickJinjaParserJson
import de.achimonline.quickjinja.parser.QuickJinjaParserYaml
import de.achimonline.quickjinja.process.QuickJinjaProcess
import de.achimonline.quickjinja.python.QuickJinjaPython
import de.achimonline.quickjinja.settings.*
import de.achimonline.quickjinja.settings.ResultViewMode.HTML
import de.achimonline.quickjinja.settings.ResultViewMode.PLAIN_TEXT
import de.achimonline.quickjinja.settings.TemplateSource.*
import de.achimonline.quickjinja.toolwindow.QuickJinjaToolWindowFactory.TabId.RESULT
import de.achimonline.quickjinja.toolwindow.QuickJinjaToolWindowFactory.TabId.VARIABLES
import de.achimonline.quickjinja.toolwindow.QuickJinjaToolWindowPreviewLabel.Type
import java.awt.Component
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.nio.file.Paths
import javax.swing.Icon
import javax.swing.JCheckBox
import javax.swing.JLabel

@Suppress("DialogTitleCapitalization")
class QuickJinjaToolWindowFactory: ToolWindowFactory, ToolWindowManagerListener, FileEditorManagerListener, DumbAware {
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
        UNKNOWN(AllIcons.RunConfigurations.TestUnknown)
    }

    private var editorFile: VirtualFile? = null
    private var selectionModel: SelectionModel? = null
    private var selectedText: String? = null
    private var clipboard: String? = null

    private lateinit var tabPane: JBTabbedPane
    private lateinit var tabs: List<Tab>

    private lateinit var runButton: ActionButton

    private lateinit var templateFilePath: TextFieldWithBrowseButton
    private lateinit var templateTextSelectionPreview: QuickJinjaToolWindowPreviewLabel
    private lateinit var templateClipboardPreview: QuickJinjaToolWindowPreviewLabel

    private lateinit var variablesEditor: EditorTextField
    private lateinit var variablesEditorStatus: JLabel
    private lateinit var variablesLoadFromFile: JCheckBox
    private lateinit var variablesSelectedFile: TextFieldWithBrowseButton
    private lateinit var variablesFileStatus: JLabel

    private lateinit var resultPlainTextView: EditorTextField
    private val resultHtmlView = JCEFHtmlPanel("about:blank")
    private lateinit var resultStatus: JLabel

    private val fileDocumentManager = FileDocumentManager.getInstance()
    private val copyPasteManager = CopyPasteManager.getInstance()

    private lateinit var appSettings: QuickJinjaAppSettings
    private lateinit var projectSettings: QuickJinjaProjectSettings

    private val parsers = listOf(
        QuickJinjaParserJson(),
        QuickJinjaParserYaml()
    )

    private val editorSelectionListener = object : SelectionListener {
        override fun selectionChanged(selectionEvent: SelectionEvent) {
            updateTextSelection(selectionEvent.editor.selectionModel.selectedText)
        }
    }

    private val clipBoardContentChangedListener =
        ContentChangedListener { _, newTransferable -> updateClipboardContent(newTransferable?.getTransferData(DataFlavor.stringFlavor) as String?) }

    private fun updateEditorFile(virtualFile: VirtualFile?) {
        editorFile = virtualFile

        if (!projectSettings.templatePinned) {
            templateFilePath.text = virtualFile?.path ?: ""
        }
    }

    private fun updateTextSelection(newSelectedText: String?) {
        if (newSelectedText.isNullOrBlank()) {
            templateTextSelectionPreview.setText(message("toolwindow.template.source.selection.empty"), Type.WARNING)
            selectedText = null
        } else {
            templateTextSelectionPreview.setText(newSelectedText, Type.INFO)
            selectedText = newSelectedText
        }
    }

    private fun updateSelectionModel(newSelectionModel: SelectionModel?) {
        selectionModel?.removeSelectionListener(editorSelectionListener)
        selectionModel = newSelectionModel
        selectionModel?.addSelectionListener(editorSelectionListener)
    }

    private fun updateClipboardContent(newClipboardContent: String?) {
        if (newClipboardContent.isNullOrBlank()) {
            templateClipboardPreview.setText(message("toolwindow.template.source.clipboard.empty"), Type.WARNING)
            clipboard = null
        } else {
            templateClipboardPreview.setText(newClipboardContent, Type.INFO)
            clipboard = newClipboardContent
        }
    }

    override fun toolWindowShown(toolWindow: ToolWindow) {
        if (editorFile == null) {
            updateEditorFile(FileEditorManager.getInstance(toolWindow.project).selectedTextEditor?.virtualFile)
        }

        if (selectionModel == null) {
            val initialSelectionModel = FileEditorManager.getInstance(toolWindow.project).selectedTextEditor?.selectionModel
            updateTextSelection(initialSelectionModel?.selectedText)
            initialSelectionModel?.addSelectionListener(editorSelectionListener)
            selectionModel = initialSelectionModel
        }

        updateClipboardContent(copyPasteManager.contents?.getTransferData(DataFlavor.stringFlavor) as String?)
        copyPasteManager.addContentChangedListener(clipBoardContentChangedListener, toolWindow.disposable)
    }

    override fun stateChanged(
        toolWindowManager: ToolWindowManager,
        changeType: ToolWindowManagerListener.ToolWindowManagerEventType
    ) {
        if (changeType == HideToolWindow) {
            updateSelectionModel(null)
            copyPasteManager.removeContentChangedListener(clipBoardContentChangedListener)
        }
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        updateEditorFile(event.newFile)
        updateTextSelection(event.manager.selectedTextEditor?.selectionModel?.selectedText)
        updateSelectionModel(event.manager.selectedTextEditor?.selectionModel)
    }

    private fun createVariablesTab(project: Project): Tab {
        return Tab(
            VARIABLES,
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
                                    projectSettings.variables = variablesEditor.text
                                    validateVariables()
                                }
                            })
                        }
                        .component

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
                            validateVariables()
                        }
                        .component

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
                        }
                        .component

                    valuesEditorRow.enabledIf(variablesLoadFromFile.selected.not())

                    variablesFileStatus = icon(StatusIcon.UNKNOWN.value)
                        .enabledIf(variablesLoadFromFile.selected)
                        .align(AlignX.LEFT)
                        .component
                }
            },
            message("toolwindow.tab.variables.tip")
        )
    }

    private fun createResultTab(project: Project): Tab {
        val resultPlainTextViewActive = AtomicBooleanProperty(projectSettings.resultViewMode == PLAIN_TEXT)
        val resultHtmlViewActive = AtomicBooleanProperty(projectSettings.resultViewMode == HTML)

        return Tab(
            RESULT,
            message("toolwindow.tab.result"),
            AllIcons.Actions.Preview,
            panel {
                buttonsGroup {
                    row(message("toolwindow.tab.result.view.mode")) {
                        radioButton(
                            message("toolwindow.tab.result.view.mode.plaintext"),
                            PLAIN_TEXT
                        )
                            .bindSelected(resultPlainTextViewActive)
                            .onChanged {
                                if (it.isSelected) {
                                    projectSettings.resultViewMode = PLAIN_TEXT
                                }
                            }

                        radioButton(
                            message("toolwindow.tab.result.view.mode.html"),
                            HTML
                        )
                            .bindSelected(resultHtmlViewActive)
                            .onChanged {
                                if (it.isSelected) {
                                    projectSettings.resultViewMode = HTML
                                }
                            }
                    }
                }.bind(projectSettings::resultViewMode)

                row {
                    resultPlainTextView = cell(QuickJinjaToolWindowEditor(project, true))
                        .resizableColumn()
                        .align(Align.FILL)
                        .visibleIf(resultPlainTextViewActive)
                        .component

                    cell(resultHtmlView.component)
                        .resizableColumn()
                        .align(Align.FILL)
                        .visibleIf(resultHtmlViewActive)

                    resultStatus = icon(StatusIcon.UNKNOWN.value)
                        .align(AlignY.TOP)
                        .component
                }.resizableRow()
            },
            message("toolwindow.tab.result.tip")
        )
    }

    private fun createGotItTooltip(parentComponent: Component): GotItTooltip {
        return GotItTooltip("de.achimonline.quickjinja.gotit.setup", message("gotitpopup.setup"))
            .withIcon(AllIcons.General.Information)
            .withLink(message("gotitpopup.setup.link")) {
                ShowSettingsUtil.getInstance().editConfigurable(parentComponent, QuickJinjaAppSettingsConfigurable())
            }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        appSettings = QuickJinjaAppSettingsState.instance.settings
        projectSettings = QuickJinjaProjectSettingsState.getInstance(project).settings

        project.messageBus.connect(toolWindow.contentManager).let {
            it.subscribe(ToolWindowManagerListener.TOPIC, this)
            it.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
        }

        resultHtmlView.apply {
            setOpenLinksInExternalBrowser(true)
            setPageBackgroundColor(appSettings.htmlResultViewerBackgroundColor)
        }

        tabPane = JBTabbedPane()

        tabs = listOf(
            createVariablesTab(project),
            createResultTab(project)
        )

        val pane = panel {
            indent {
                row {
                    runButton = actionButton(object : DumbAwareAction(
                        message("toolwindow.action.text"),
                        message("toolwindow.action.description"),
                        AllIcons.Debugger.ThreadRunning
                    ) {
                        override fun actionPerformed(e: AnActionEvent) {
                            run(project)
                        }
                    }).component

                    label(message("toolwindow.template.source"))
                }
            }

            buttonsGroup {
                indent {
                    row(message("toolwindow.template.source.file")) {
                        radioButton("", FILE)
                            .onChanged {
                                if (it.isSelected) {
                                    projectSettings.templateSource = FILE
                                }
                            }

                        templateFilePath = textFieldWithBrowseButton(
                            message("toolwindow.template.source.file.browse.dialog.title"),
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
                            message("toolwindow.template.source.file.pin.text"),
                            message("toolwindow.template.source.file.pin.description"),
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

                        cell()
                    }
                }

                indent {
                    row(message("toolwindow.template.source.selection")) {
                        radioButton("", TEXT_SELECTION)
                            .onChanged {
                                if (it.isSelected) {
                                    projectSettings.templateSource = TEXT_SELECTION
                                }
                            }

                        templateTextSelectionPreview = cell(QuickJinjaToolWindowPreviewLabel(message("toolwindow.template.source.clipboard.empty"), Type.WARNING))
                            .align(AlignX.FILL)
                            .resizableColumn()
                            .component

                        cell()
                    }
                }

                indent {
                    row(message("toolwindow.template.source.clipboard")) {
                        radioButton("", CLIPBOARD)
                            .onChanged {
                                if (it.isSelected) {
                                    projectSettings.templateSource = CLIPBOARD
                                }
                            }

                        templateClipboardPreview = cell(QuickJinjaToolWindowPreviewLabel(message("toolwindow.template.source.clipboard.empty"), Type.WARNING))
                            .align(AlignX.FILL)
                            .resizableColumn()
                            .component

                        cell()
                    }
                }
            }.bind(projectSettings::templateSource)

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

        val currentClipboard: String? = CopyPasteManager.getInstance().contents?.getTransferData(DataFlavor.stringFlavor) as String?

        if (currentClipboard.isNullOrBlank()) {
            templateClipboardPreview.setText(message("toolwindow.template.source.clipboard.empty"), Type.WARNING)
        } else {
            templateClipboardPreview.setText(currentClipboard, Type.INFO)
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

    private fun switchToTab(tabId: TabId) {
        tabPane.selectedIndex = tabs.indexOf(
            tabs.find {
                it.id == tabId
            }
        )
    }

    private fun run(project: Project) {
        // validate variables again (in case a vars-file is used and the contents changed)
        if (variablesLoadFromFile.isSelected && !validateVariables()) {
            switchToTab(VARIABLES)
            return
        }

        val templateFile: File?

        when (projectSettings.templateSource) {
            FILE -> {
                templateFile = if (editorFile == null || templateFilePath.text.trim().isEmpty()) {
                    null
                } else if (templateFilePath.text == editorFile!!.path) {
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
            }
            TEXT_SELECTION -> {
                templateFile = if (selectedText == null || selectedText!!.trim().isEmpty()) null else createTempFileFromText(selectedText!!)
            }
            CLIPBOARD -> {
                templateFile = if (clipboard == null || clipboard!!.trim().isEmpty()) null else createTempFileFromText(clipboard!!)
            }
        }

        if (templateFile == null) {
            return
        }

        val pythonScript = QuickJinjaPython.createScriptFile(
            templateFile = templateFile,
            variablesFile = generateFileFromVariables(),
            options = appSettings.getJinjaOptions(),
            fileSystemLoaderPaths = listOfNotNull(project.basePath, Paths.get(templateFilePath.text).parent.toString())
        )

        val processResult = QuickJinjaProcess.run(appSettings.executable, listOf(pythonScript.absolutePath))

        if (processResult.rc == 0) {
            resultStatus.icon = StatusIcon.OK.value
            resultPlainTextView.text = processResult.stdout
            resultHtmlView.setHtml(processResult.stdout)
        } else {
            resultStatus.icon = StatusIcon.ERROR.value
            val result = listOf(processResult.stdout, processResult.stderr).joinToString(System.lineSeparator()).trim()
            resultPlainTextView.text = result
            resultHtmlView.setHtml(
                """
                    <pre>
                    $result
                    </pre>
                """.trimIndent()
            )
        }

        switchToTab(RESULT)
    }
}
