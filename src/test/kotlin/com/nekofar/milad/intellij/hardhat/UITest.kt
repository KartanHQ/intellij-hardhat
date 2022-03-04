package com.nekofar.milad.intellij.hardhat

import com.automation.remarks.junit5.Video
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.search.locators.byXpath
import com.nekofar.milad.intellij.hardhat.pages.dialog
import com.nekofar.milad.intellij.hardhat.pages.idea
import com.nekofar.milad.intellij.hardhat.pages.welcomeFrame
import com.nekofar.milad.intellij.hardhat.utils.RemoteRobotExtension
import com.nekofar.milad.intellij.hardhat.utils.StepsLogger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith

@TestMethodOrder(OrderAnnotation::class)
@ExtendWith(RemoteRobotExtension::class)
class UITest {
    init {
        StepsLogger.init()
    }

    @AfterEach
    fun closeProject(remoteRobot: RemoteRobot) = with(remoteRobot) {
        idea {
            menuBar.select("File", "Close Project")
        }
    }

    @Test
    @Video
    @Order(1)
    fun createNewProject(remoteRobot: RemoteRobot) = with(remoteRobot) {
        welcomeFrame {
            createNewProjectLink.click()
            dialog("New Project") {
                findText("JavaScript").click()
                jList(
                    byXpath(
                        "//div[contains(@visible_text_keys, 'create.react.app.name')]"
                    )
                ).clickItem("Hardhat")
                button("Next").click()
                button("Finish").click()
            }
        }
    }
}