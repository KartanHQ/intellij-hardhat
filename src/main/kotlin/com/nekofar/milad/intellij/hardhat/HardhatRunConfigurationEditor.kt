package com.nekofar.milad.intellij.hardhat

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.RawCommandLineEditor
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent
import javax.swing.JPanel

class HardhatRunConfigurationEditor(project: Project) : SettingsEditor<HardhatRunConfiguration>() {

    private val configFileField = TextFieldWithBrowseButton()
    private val argumentsEditor = RawCommandLineEditor()

    private val panel: JPanel by lazy {
        FormBuilder.createFormBuilder()
            .setAlignLabelOnRight(false)
            .setHorizontalGap(UIUtil.DEFAULT_HGAP)
            .setVerticalGap(UIUtil.DEFAULT_VGAP)
            .addLabeledComponent(
                HardhatBundle.message("hardhat.run.configuration.editor.config.file.label"),
                configFileField
            )
            .addLabeledComponent(
                HardhatBundle.message("hardhat.run.configuration.editor.arguments.label"),
                argumentsEditor
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
        configFileField.text = runConfiguration.state?.configFile.toString()
        argumentsEditor.text = runConfiguration.state?.arguments.toString()
    }

    override fun applyEditorTo(runConfiguration: HardhatRunConfiguration) {
        runConfiguration.state?.configFile = configFileField.text
        runConfiguration.state?.arguments = argumentsEditor.text
    }

    override fun createEditor(): JComponent {
        return panel
    }
}
