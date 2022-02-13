package com.nekofar.milad.intellij.hardhat

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

class HardhatRunConfigurationFactory(private val runConfigurationType: HardhatRunConfigurationType) :
    ConfigurationFactory(runConfigurationType) {
    override fun getId() = "Hardhat"
    override fun getName() = runConfigurationType.displayName
    override fun createTemplateConfiguration(project: Project) = HardhatRunConfiguration(project, this, "Hardhat")
    override fun getOptionsClass(): Class<out BaseState> = HardhatRunConfigurationOptions::class.java
}
