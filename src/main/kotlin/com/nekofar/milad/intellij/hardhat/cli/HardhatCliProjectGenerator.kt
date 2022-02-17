package com.nekofar.milad.intellij.hardhat.cli

import com.intellij.execution.filters.Filter
import com.intellij.lang.javascript.boilerplate.NpmPackageProjectGenerator
import com.intellij.lang.javascript.boilerplate.NpxPackageDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.vfs.VirtualFile
import com.nekofar.milad.intellij.hardhat.HardhatBundle
import com.nekofar.milad.intellij.hardhat.HardhatIcons

class HardhatCliProjectGenerator : NpmPackageProjectGenerator() {
    private val packageName = "hardhat"
    private val executable = "hardhat"

    override fun getIcon() = HardhatIcons.ProjectGenerator
    override fun getName() = HardhatBundle.message("hardhat.project.generator.name")
    override fun getDescription() = HardhatBundle.message("hardhat.project.generator.description")
    override fun packageName() = packageName
    override fun presentablePackageName() = HardhatBundle.message("hardhat.project.generator.presentable.package.name")
    override fun getNpxCommands() = listOf(NpxPackageDescriptor.NpxCommand(packageName, executable))
    override fun filters(project: Project, baseDir: VirtualFile): Array<Filter> = emptyArray()
    override fun customizeModule(baseDir: VirtualFile, entry: ContentEntry?) { /* Do nothing */ }
}
