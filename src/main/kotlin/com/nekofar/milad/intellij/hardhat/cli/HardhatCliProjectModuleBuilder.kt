package com.nekofar.milad.intellij.hardhat.cli

import com.intellij.ide.util.projectWizard.WebTemplateNewProjectWizard
import com.intellij.ide.wizard.GeneratorNewProjectWizardBuilderAdapter

class HardhatCliProjectModuleBuilder :
    GeneratorNewProjectWizardBuilderAdapter(WebTemplateNewProjectWizard(HardhatCliProjectGenerator()))
