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

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.maltaisn.nines.core.GameApp
import com.maltaisn.nines.core.GameListener
import java.awt.Desktop
import java.net.URI


object DesktopLauncher : GameListener {

    @JvmStatic
    fun main(args: Array<String>) {
        Lwjgl3Application(GameApp(this), Lwjgl3ApplicationConfiguration().apply {
            setTitle("Cards")
            setWindowedMode(1440, 810)
            setWindowSizeLimits(960, 540, -1, -1)
        })
    }


    override val isRateAppSupported = false

    override fun onReportBugClicked() {
        // Open feedback form
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI("https://forms.gle/AoB7AZdU6QCY1jNC7"))
        }
    }

}
