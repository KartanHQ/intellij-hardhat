package com.nekofar.milad.intellij.hardhat

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

class HardhatRunConfiguration(project: Project, factory: HardhatRunConfigurationFactory, name: String) :
    RunConfigurationBase<HardhatRunConfigurationOptions>(project, factory, name) {

    override fun getOptions() = super.getOptions() as HardhatRunConfigurationOptions

    var scriptName
        get() = options.scriptName
        set(value) { options.scriptName  = value}

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = HardhatSettingsEditor()

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return object : CommandLineState(environment) {
            @Throws(ExecutionException::class)
            override fun startProcess(): ProcessHandler {
                val commandLine = GeneralCommandLine(options.scriptName)
                val processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
                ProcessTerminatedListener.attach(processHandler)
                return processHandler
            }
        }
    }
}