package com.nekofar.milad.intellij.hardhat

import com.intellij.execution.configurations.RunConfigurationOptions

class HardhatRunConfigurationOptions : RunConfigurationOptions() {
    private val _scriptName = string("").provideDelegate(this, "scriptName")

    var scriptName
        get() = _scriptName.getValue(this) ?: ""
        set(value) = _scriptName.setValue(this, value)
}
