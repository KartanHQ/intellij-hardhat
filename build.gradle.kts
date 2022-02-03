plugins {
    id("org.jetbrains.intellij") version "1.3.1"
    kotlin("jvm") version "1.6.10"
}

group = "com.nekofar.milad"
version = "1.0.0-alpha.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("2021.3.2")
    type.set("IU")
    plugins.set(listOf("JavaScript"))
}

tasks {
    patchPluginXml {
        changeNotes.set("""
            Initial release of the plugin.        """.trimIndent())
    }
}