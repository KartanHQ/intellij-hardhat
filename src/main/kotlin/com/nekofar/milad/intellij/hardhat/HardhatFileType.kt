package com.nekofar.milad.intellij.hardhat

import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object HardhatFileType: LanguageFileType(JavascriptLanguage.INSTANCE) {
    override fun getIcon(): Icon = HardhatIcons.FileType
    override fun getName(): String = HardhatBundle.message("hardhat.file.type.name")
    override fun getDescription(): String = HardhatBundle.message("hardhat.file.type.description")
    override fun getDefaultExtension(): String = "js"
}