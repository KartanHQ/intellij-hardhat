package com.nekofar.milad.intellij.hardhat.rc

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.nekofar.milad.intellij.hardhat.HardhatBundle
import com.nekofar.milad.intellij.hardhat.HardhatIcons

class HardhatConfigurationType :
    SimpleConfigurationType(
        "HardhatRunConfiguration",
        HardhatBundle.message("hardhat.run.configuration.type.name"),
        HardhatBundle.message("hardhat.run.configuration.type.description"),
        NotNullLazyValue.createValue { HardhatIcons.RunConfigurationType }
    ),
    DumbAware {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return HardhatRunConfiguration(project, this, "Hardhat")
    }

    override fun isEditableInDumbMode(): Boolean {
        return true
    }
}
