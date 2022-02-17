package com.nekofar.milad.intellij.hardhat.rc

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.impl.BeforeRunTaskAwareConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.javascript.JSRunProfileWithCompileBeforeLaunchOption
import com.intellij.javascript.nodejs.debug.NodeDebugRunConfiguration
import com.intellij.javascript.nodejs.interpreter.NodeInterpreterUtil
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.nekofar.milad.intellij.hardhat.HardhatBundle
import java.io.File

class HardhatRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    LocatableConfigurationBase<HardhatRunConfigurationOptions>(project, factory, name),
    NodeDebugRunConfiguration, JSRunProfileWithCompileBeforeLaunchOption, BeforeRunTaskAwareConfiguration {

    override fun getOptions(): HardhatRunConfigurationOptions {
        return super.getOptions() as HardhatRunConfigurationOptions
    }

    override fun getDefaultOptionsClass(): Class<out LocatableRunConfigurationOptions> {
        return HardhatRunConfigurationOptions::class.java
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return HardhatRunConfigurationEditor(project)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        val hardhatPackage = getHardhatPackage()

        return hardhatPackage?.let { HardhatRunProfileState(environment, options, hardhatPackage) }
    }

    private fun getHardhatPackage(): NodePackage? {
        return NodePackage.findDefaultPackage(project, "hardhat", interpreter)
    }

    override fun getInterpreter(): NodeJsInterpreter? {
        return NodeJsInterpreterRef.create(options.interpreterRef).resolve(project)
    }

    @Throws(RuntimeConfigurationException::class)
    override fun checkConfiguration() {
        if (getHardhatPackage() == null) {
            throw RuntimeConfigurationError(HardhatBundle.message("hardhat.run.configuration.hardhatPackage.not.found"))
        }

        val configFile = options.configFile?.trim { it <= ' ' }.orEmpty()
        if (configFile.isEmpty()) {
            throw RuntimeConfigurationError(HardhatBundle.message("hardhat.run.configuration.configFile.unspecified"))
        } else {
            val file = File(configFile)
            if (!file.isAbsolute || !file.isFile) {
                throw RuntimeConfigurationError(HardhatBundle.message("hardhat.run.configuration.configFile.not.found"))
            }
        }

        NodeInterpreterUtil.checkForRunConfiguration(interpreter, project)
    }

    override fun useRunExecutor(): Boolean {
        return true
    }
}
