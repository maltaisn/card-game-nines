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

package io.github.maltaisn.cardgametest.core.tests

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Table
import io.github.maltaisn.cardgame.widget.Slider
import io.github.maltaisn.cardgametest.core.TestGame
import ktx.log.debug


class SliderTest(game: TestGame) : CardGameTest(game) {

    init {
        isDebugAll = true

        val content = Table()
        content.background = coreSkin.getDrawable("submenu-content-background")

        val slider = Slider(coreSkin)
        slider.progress = 50f
        slider.changeListener = { value ->
            debug { "Slider value change to $value" }
        }
        content.add(slider).width(300f).expand()

        gameLayer.centerTable.add(content).grow().pad(20f, 20f, 0f, 20f)

        addListener(object : InputListener() {
            override fun keyUp(event: InputEvent, keycode: Int): Boolean {
                if (keycode == Input.Keys.E) {
                    slider.enabled = !slider.enabled
                }
                return false
            }
        })
    }

}