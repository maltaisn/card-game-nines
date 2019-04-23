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

package com.maltaisn.cardgametest.core.tests

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.maltaisn.cardgame.widget.FontStyle
import com.maltaisn.cardgame.widget.SdfTextField
import com.maltaisn.cardgame.widget.Slider
import com.maltaisn.cardgame.widget.Switch
import com.maltaisn.cardgametest.core.TestGame
import ktx.log.debug


class PrefWidgetsTest(game: TestGame) : CardGameTest(game) {

    override fun start() {
        super.start()

        isDebugAll = true

        val content = Table()
        content.background = coreSkin.getDrawable("submenu-content-background")

        // Switch
        val switch = Switch(coreSkin)
        switch.checkListener = { checked ->
            debug { "Switch checked change to $checked" }
        }
        content.add(switch).size(300f, 100f).expand().row()

        // Slider
        val slider = Slider(coreSkin)
        slider.progress = 50f
        slider.changeListener = { value ->
            debug { "Slider value changed to $value" }
        }
        content.add(slider).width(300f).expand().row()

        // Text field
        val textField = SdfTextField(coreSkin, FontStyle().apply {
            fontSize = 24f
            fontColor = Color.BLACK
        }, "Text input")
        textField.maxLength = 20
        content.add(textField).width(300f).expand().row()

        gameLayer.centerTable.add(content).grow().pad(20f, 20f, 0f, 20f)

        addListener(object : InputListener() {
            override fun keyUp(event: InputEvent, keycode: Int): Boolean {
                if (keycode == Input.Keys.C) {
                    switch.check(!switch.checked, !Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT))
                } else if (keycode == Input.Keys.E) {
                    switch.enabled = !switch.enabled
                    slider.enabled = !slider.enabled
                }
                return false
            }
        })
    }

}