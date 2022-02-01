plugins {
    id("org.jetbrains.intellij") version "1.3.1"
    kotlin("jvm") version "1.6.10"
}

group = "com.nekofar.milad"
version = "1.0.0-alpha.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("2021.3.1")
}
tasks {
    patchPluginXml {
        changeNotes.set("""
            Add change notes here.<br>
            <em>most HTML tags may be used</em>        """.trimIndent())
    }
}