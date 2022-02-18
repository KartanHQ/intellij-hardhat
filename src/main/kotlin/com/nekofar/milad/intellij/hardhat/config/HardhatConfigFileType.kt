package com.nekofar.milad.intellij.hardhat.config

import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import com.nekofar.milad.intellij.hardhat.HardhatBundle
import com.nekofar.milad.intellij.hardhat.HardhatIcons
import javax.swing.Icon

object HardhatConfigFileType : LanguageFileType(JavascriptLanguage.INSTANCE) {
    override fun getIcon(): Icon = HardhatIcons.FileType
    override fun getName(): String = HardhatBundle.message("hardhat.file.type.name")
    override fun getDescription(): String = HardhatBundle.message("hardhat.file.type.description")
    override fun getDefaultExtension(): String = "js"
}
