/*
 * Copyright 2019 Nicolas Maltais
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.maltaisn.nines.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.maltaisn.nines.core.GameApp
import com.maltaisn.nines.core.GameListener
import java.awt.Desktop
import java.net.URI


object DesktopLauncher : GameListener {

    override val isTextInputDelegated = false
    override val isRateAppSupported = false

    override var isFullscreen = false
        set(value) {
            field = value
            if (value) {
                // Set fullscreen and remember previous window dimensions
                val mode = Gdx.graphics.displayMode
                windowWidth = Gdx.graphics.width
                windowHeight = Gdx.graphics.height
                Gdx.graphics.setFullscreenMode(mode)
            } else {
                // Restore window to its previous dimensions
                Gdx.graphics.setWindowedMode(windowWidth, windowHeight)
            }
        }

    private var windowWidth = 1440
    private var windowHeight = 810


    @JvmStatic
    fun main(args: Array<String>) {
        Lwjgl3Application(GameApp(this), Lwjgl3ApplicationConfiguration().apply {
            setTitle("Cards")
            setWindowedMode(windowWidth, windowHeight)
            setWindowSizeLimits(960, 540, -1, -1)
            setWindowIcon("icon-16.png", "icon-32.png", "icon-48.png")
        })
    }


    override fun onReportBugClicked() {
        // Open feedback form
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI("https://forms.gle/AoB7AZdU6QCY1jNC7"))
        }
    }

    override fun onViewSourceClicked() {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI("https://github.com/maltaisn/card-game-nines"))
        }
    }

}
