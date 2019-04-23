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
import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.maltaisn.cardgame.markdown.Markdown
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.widget.menu.DefaultGameMenu
import com.maltaisn.cardgametest.core.TestGame


class MenuTest(game: TestGame) : CardGameTest(game) {

    private val prefs = mutableListOf<GamePrefs>()

    init {
        //isDebugAll = true

        // Menu
        val menu = DefaultGameMenu(coreSkin)
        menu.continueItem.enabled = false
        menu.shown = true
        gameMenu = menu

        menu.newGameOptions = loadPrefs("new-game-options.json")
        menu.settings = loadPrefs("settings.json")
        menu.rules = loadMarkdown("rules")

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

    private fun loadPrefs(name: String): GamePrefs {
        val file = AssetDescriptor(name, GamePrefs::class.java)
        assetManager.load(file)
        assetManager.finishLoading()
        val prefs = assetManager.get(file)
        this.prefs += prefs
        return prefs
    }

    private fun loadMarkdown(name: String): Markdown {
        val file = AssetDescriptor(name, Markdown::class.java)
        assetManager.load(file)
        assetManager.finishLoading()
        return assetManager.get(file)
    }

    override fun pause() {
        super.pause()

        // Save all preferences when game is paused
        for (pref in prefs) {
            pref.save()
        }
    }

}