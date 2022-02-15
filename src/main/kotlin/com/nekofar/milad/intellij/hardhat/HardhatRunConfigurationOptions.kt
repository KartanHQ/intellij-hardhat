package com.nekofar.milad.intellij.hardhat

import com.intellij.execution.configurations.LocatableRunConfigurationOptions

class HardhatRunConfigurationOptions : LocatableRunConfigurationOptions() {
    private val _configFile = string("").provideDelegate(this, "configFile")
    private val _arguments = string("").provideDelegate(this, "arguments")

    var configFile: String
        get() = _configFile.getValue(this) ?: ""
        set(configFile) = _configFile.setValue(this, configFile)

    var arguments: String
        get() = _arguments.getValue(this) ?: ""
        set(arguments) = _arguments.setValue(this, arguments)
}
