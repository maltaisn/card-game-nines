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
import io.github.maltaisn.cardgame.widget.menu.DefaultGameMenu
import io.github.maltaisn.cardgametest.core.TestGame


class MenuTest(game: TestGame) : CardGameTest(game) {

    init {
        val menu = DefaultGameMenu(coreSkin)
        gameMenu = menu
        menu.shown = true

        menu.continueItem.enabled = false

        addListener(object : InputListener() {
            override fun keyUp(event: InputEvent, keycode: Int): Boolean {
                if (keycode == Input.Keys.M) {
                    menu.shown = !menu.shown
                    return true
                }
                return false
            }
        })
    }

}