package com.nekofar.milad.intellij.hardhat

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.icons.AllIcons

class HardhatRunConfigurationType : ConfigurationType {
    override fun getId() = "HardhatRunConfigurationType"
    override fun getIcon() = AllIcons.General.Information
    override fun getDisplayName() = HardhatBundle.message("hardhat.run.configuration.name")
    override fun getConfigurationTypeDescription() = HardhatBundle.message("hardhat.run.configuration.description")
    override fun getConfigurationFactories(): Array<ConfigurationFactory> = arrayOf(HardhatRunConfigurationFactory(this))
}