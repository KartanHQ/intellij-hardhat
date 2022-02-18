package com.nekofar.milad.intellij.hardhat.config

import com.intellij.ide.IconProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.nekofar.milad.intellij.hardhat.HardhatIcons
import javax.swing.Icon

class HardhatConfigIconProvider : IconProvider(), DumbAware {
    override fun getIcon(element: PsiElement, @Iconable.IconFlags flags: Int): Icon? {
        val fileElement = element as? PsiFile
        return if (fileElement != null && fileElement.name.startsWith(
                "hardhat.config",
                true
            )
        ) HardhatIcons.FileType else null
    }
}