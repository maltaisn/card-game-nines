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

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.maltaisn.cardgame.markdown.Markdown
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.widget.menu.DefaultGameMenu
import com.maltaisn.cardgametest.core.TestGame
import ktx.assets.load


class MenuTest(game: TestGame) : CardGameTest(game) {

    private val prefs = mutableListOf<GamePrefs>()

    override fun load() {
        super.load()

        assetManager.load<GamePrefs>(PREFS_NEW_GAME)
        assetManager.load<GamePrefs>(PREFS_SETTINGS)
        assetManager.load<Markdown>(MD_RULES)
    }

    override fun start() {
        super.start()

        //isDebugAll = true

        val menu = DefaultGameMenu(coreSkin)
        menu.continueItem.enabled = false
        menu.shown = true
        gameMenu = menu

        menu.newGameOptions = assetManager.get(PREFS_NEW_GAME)
        menu.settings = assetManager.get(PREFS_SETTINGS)
        menu.rules = assetManager.get(MD_RULES)

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

    override fun pause() {
        super.pause()

        // Save all preferences when game is paused
        for (pref in prefs) {
            pref.save()
        }
    }

    companion object {
        private const val PREFS_NEW_GAME = "new-game-options.json"
        private const val PREFS_SETTINGS = "settings.json"
        private const val MD_RULES = "rules"
    }

}