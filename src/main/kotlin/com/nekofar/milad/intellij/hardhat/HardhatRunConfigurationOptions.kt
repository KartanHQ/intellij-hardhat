package com.nekofar.milad.intellij.hardhat

import com.intellij.execution.configurations.LocatableRunConfigurationOptions

class HardhatRunConfigurationOptions : LocatableRunConfigurationOptions() {
    private val _configFile = string("").provideDelegate(this, "configFile")
    private val _arguments = string("").provideDelegate(this, "arguments")
    private var _interpreterRef = string("").provideDelegate(this,"interpreterRef")

    var configFile: String
        get() = _configFile.getValue(this).orEmpty()
        set(configFile) = _configFile.setValue(this, configFile)

    var arguments: String
        get() = _arguments.getValue(this).orEmpty()
        set(arguments) = _arguments.setValue(this, arguments)

    var interpreterRef: String
        get() = _interpreterRef.getValue(this).orEmpty()
        set(interpreterRef) = _interpreterRef.setValue(this, interpreterRef)
}
