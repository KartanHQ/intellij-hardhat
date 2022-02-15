package com.nekofar.milad.intellij.hardhat

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

class HardhatRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    LocatableConfigurationBase<HardhatRunConfigurationOptions>(project, factory, name) {

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        TODO("Not yet implemented")
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return HardhatRunConfigurationEditor(project)
    }

    override fun getOptionsClass(): Class<out RunConfigurationOptions> {
        return HardhatRunConfigurationOptions::class.java
    }
}
