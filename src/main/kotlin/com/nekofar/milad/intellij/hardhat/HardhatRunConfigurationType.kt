package com.nekofar.milad.intellij.hardhat

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue

class HardhatRunConfigurationType :
    SimpleConfigurationType(
        "HardhatRunConfigurationType",
        HardhatBundle.message("hardhat.run.configuration.type.name"),
        HardhatBundle.message("hardhat.run.configuration.type.description"),
        NotNullLazyValue.createValue { HardhatIcons.RunConfigurationType }
    ) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        TODO("Not yet implemented")
    }
}