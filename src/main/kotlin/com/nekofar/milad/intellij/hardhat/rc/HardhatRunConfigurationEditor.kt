package com.nekofar.milad.intellij.hardhat.rc

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterField
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.RawCommandLineEditor
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import com.nekofar.milad.intellij.hardhat.HardhatBundle
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator

class HardhatRunConfigurationEditor(project: Project) : SettingsEditor<HardhatRunConfiguration>() {

    private val configFileField = TextFieldWithBrowseButton()
    private val argumentsEditor = RawCommandLineEditor()
    private val interpreterField = NodeJsInterpreterField(project)

    private val panel: JPanel by lazy {
        FormBuilder.createFormBuilder()
            .setAlignLabelOnRight(false)
            .setHorizontalGap(UIUtil.DEFAULT_HGAP)
            .setVerticalGap(UIUtil.DEFAULT_VGAP)
            .addLabeledComponent(
                HardhatBundle.message("hardhat.run.configuration.editor.configFile.label"),
                configFileField
            )
            .addLabeledComponent(
                HardhatBundle.message("hardhat.run.configuration.editor.arguments.label"),
                argumentsEditor
            )
            .addComponent(JSeparator())
            .addLabeledComponent(
                HardhatBundle.message("hardhat.run.configuration.editor.nodeInterpreter.label"),
                interpreterField
            )
            .panel
    }

    init {
        configFileField.addBrowseFolderListener(
            HardhatBundle.message("hardhat.file.chooser.title"),
            HardhatBundle.message("hardhat.file.chooser.description"),
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor()
        )
    }

    override fun resetEditorFrom(runConfiguration: HardhatRunConfiguration) {
        configFileField.text = runConfiguration.state?.configFile.orEmpty()
        argumentsEditor.text = runConfiguration.state?.arguments.orEmpty()
        interpreterField.interpreterRef = NodeJsInterpreterRef.create(runConfiguration.state?.interpreterRef)
    }

    override fun applyEditorTo(runConfiguration: HardhatRunConfiguration) {
        runConfiguration.state?.configFile = configFileField.text
        runConfiguration.state?.arguments = argumentsEditor.text
        runConfiguration.state?.interpreterRef = interpreterField.interpreterRef.referenceName
    }

    override fun createEditor(): JComponent {
        return panel
    }
}
