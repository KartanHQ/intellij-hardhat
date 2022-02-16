package com.nekofar.milad.intellij.hardhat.cli

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.lang.javascript.boilerplate.JavaScriptNewTemplatesFactoryBase

class HardhatProjectTemplateFactory : JavaScriptNewTemplatesFactoryBase() {
    override fun createTemplates(context: WizardContext?) = arrayOf(HardhatCliProjectGenerator())
}
