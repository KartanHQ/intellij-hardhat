package com.nekofar.milad.intellij.hardhat

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

class HardhatRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    LocatableConfigurationBase<HardhatRunConfigurationOptions>(project, factory, name) {

    override fun getOptionsClass(): Class<out RunConfigurationOptions> {
        return HardhatRunConfigurationOptions::class.java
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return HardhatRunConfigurationEditor(project)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        val hardhatPackage = getHardhatPackage()

        return hardhatPackage?.let { HardhatRunProfileState(environment, state!!, hardhatPackage) }
    }

    private fun getHardhatPackage(): NodePackage? {
        return NodePackage.findDefaultPackage(
            project,
            "hardhat",
            NodeJsInterpreterRef.createProjectRef().resolve(project)
        )
    }
}
