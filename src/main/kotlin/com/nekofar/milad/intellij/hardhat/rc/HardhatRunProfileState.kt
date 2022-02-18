package com.nekofar.milad.intellij.hardhat.rc

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.ParametersList
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import com.intellij.javascript.debugger.CommandLineDebugConfigurator
import com.intellij.javascript.nodejs.NodeCommandLineUtil
import com.intellij.javascript.nodejs.NodeConsoleAdditionalFilter
import com.intellij.javascript.nodejs.NodeStackTraceFilter
import com.intellij.javascript.nodejs.debug.NodeLocalDebuggableRunProfileStateSync
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.library.yarn.YarnPnpNodePackage
import com.intellij.javascript.nodejs.util.NodePackage
import com.intellij.lang.javascript.buildTools.TypeScriptErrorConsoleFilter
import com.intellij.webcore.util.CommandLineUtil
import java.io.File
import java.nio.charset.StandardCharsets

class HardhatRunProfileState(
    private val environment: ExecutionEnvironment,
    private val options: HardhatRunConfigurationOptions,
    private val hardhatPackage: NodePackage
) : NodeLocalDebuggableRunProfileStateSync() {

    override fun executeSync(configurator: CommandLineDebugConfigurator?): ExecutionResult {
        val interpreter = NodeJsInterpreterRef.create(options.interpreterRef).resolveNotNull(environment.project)
        val commandLine = NodeCommandLineUtil.createCommandLine(true)
        NodeCommandLineUtil.configureCommandLine(commandLine, configurator, interpreter) {
            configureCommandLine(commandLine, interpreter)
        }
        val processHandler = NodeCommandLineUtil.createProcessHandler(commandLine, true)
        ProcessTerminatedListener.attach(processHandler)
        val console: ConsoleView = createConsole(processHandler, commandLine.workDirectory)
        console.attachToProcess(processHandler)
        return DefaultExecutionResult(console, processHandler)
    }

    private fun createConsole(processHandler: OSProcessHandler, workDirectory: File?): ConsoleView {
        val project = environment.project
        val consoleView = NodeCommandLineUtil.createConsole(processHandler, project, false)
        consoleView.addMessageFilter(NodeStackTraceFilter(project, workDirectory))
        consoleView.addMessageFilter(NodeConsoleAdditionalFilter(project, workDirectory))
        consoleView.addMessageFilter(TypeScriptErrorConsoleFilter(project, workDirectory))
        return consoleView
    }

    private fun configureCommandLine(commandLine: GeneralCommandLine, interpreter: NodeJsInterpreter?) {
        commandLine.withCharset(StandardCharsets.UTF_8)

        CommandLineUtil.setWorkingDirectory(commandLine, File(options.configFile.orEmpty()).parentFile, false)

        if (hardhatPackage is YarnPnpNodePackage) {
            hardhatPackage.addYarnRunToCommandLine(
                commandLine,
                environment.project,
                interpreter,
                null as String?
            )
        } else {
            commandLine.addParameter(getHardhatBinFile().absolutePath)
        }

        commandLine.addParameter("--config")
        commandLine.addParameter(options.configFile.orEmpty())

        val arguments = options.arguments.orEmpty().trim { it <= ' ' }
        if (arguments.isNotEmpty()) {
            commandLine.addParameters(*ParametersList.parse(arguments))
        }
    }

    private fun getHardhatBinFile(): File {
        return File(hardhatPackage.systemDependentPath, "internal" + File.separator + "cli" + File.separator + "cli.js")
    }
}
