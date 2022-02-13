package com.nekofar.milad.intellij.hardhat

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.nekofar.milad.intellij.hardhat.HardhatRunConfiguration
import javax.swing.JComponent
import javax.swing.JPanel

class HardhatSettingsEditor : SettingsEditor<HardhatRunConfiguration>() {
    private lateinit var myPanel: JPanel
    private lateinit var myScriptName: LabeledComponent<TextFieldWithBrowseButton>

    override fun resetEditorFrom(hardhatRunConfiguration: HardhatRunConfiguration) {
        myScriptName.component.text = hardhatRunConfiguration.scriptName
    }

    override fun applyEditorTo(hardhatRunConfiguration: HardhatRunConfiguration) {
        hardhatRunConfiguration.scriptName = myScriptName.component.text
    }

    override fun createEditor(): JComponent {
        return myPanel
    }

    private fun createUIComponents() {
        myScriptName = LabeledComponent()
        myScriptName.component = TextFieldWithBrowseButton()
    }
}