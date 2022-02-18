package com.nekofar.milad.intellij.hardhat.rc

import com.intellij.execution.configurations.LocatableRunConfigurationOptions
import com.intellij.util.xmlb.annotations.OptionTag

class HardhatRunConfigurationOptions : LocatableRunConfigurationOptions() {
    @get:OptionTag
    var configFile by string()

    @get:OptionTag
    var arguments by string()

    @get:OptionTag
    var interpreterRef by string()
}
