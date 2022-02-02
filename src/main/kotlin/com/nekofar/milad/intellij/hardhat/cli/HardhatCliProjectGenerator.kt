package com.nekofar.milad.intellij.hardhat.cli

import com.intellij.execution.filters.Filter
import com.intellij.lang.javascript.boilerplate.NpmPackageProjectGenerator
import com.intellij.lang.javascript.boilerplate.NpxPackageDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.vfs.VirtualFile
import com.nekofar.milad.intellij.hardhat.HardhatBundle
import icons.HardhatIcons
import javax.swing.Icon

class HardhatCliProjectGenerator: NpmPackageProjectGenerator() {
    private val packageName = "hardhat"
    private val executable = "hardhat"

    override fun getName(): String {
        return HardhatBundle.message("hardhat.project.generator.name")
    }

    override fun getDescription(): String? {
        return HardhatBundle.message("hardhat.project.generator.description")
    }

    override fun filters(project: Project, baseDir: VirtualFile): Array<Filter> {
        return emptyArray()
    }

    override fun customizeModule(p0: VirtualFile, p1: ContentEntry?) {}

    override fun packageName(): String {
        return packageName
    }

    override fun presentablePackageName(): String {
        return HardhatBundle.message("hardhat.project.generator.presentable.package.name")
    }

    override fun getNpxCommands(): List<NpxPackageDescriptor.NpxCommand> {
        return listOf(NpxPackageDescriptor.NpxCommand(packageName, executable))
    }

    override fun getIcon(): Icon {
        return HardhatIcons.ProjectGenerator
    }
}