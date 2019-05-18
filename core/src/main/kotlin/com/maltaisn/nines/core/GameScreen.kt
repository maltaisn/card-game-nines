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

package com.maltaisn.nines.core

import com.badlogic.gdx.Gdx
import com.maltaisn.cardgame.CardGameScreen
import com.maltaisn.cardgame.markdown.Markdown
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.widget.menu.DefaultGameMenu
import ktx.assets.load


class GameScreen : CardGameScreen() {

    private lateinit var settings: GamePrefs
    private lateinit var newGameOptions: GamePrefs


    override fun load() {
        super.load()

        assetManager.load<GamePrefs>(PREFS_NEW_GAME)
        assetManager.load<GamePrefs>(PREFS_SETTINGS)
        assetManager.load<Markdown>(MD_RULES)
        loadPCardSkin()
    }

    override fun start() {
        super.start()

        val menu = DefaultGameMenu(coreSkin)
        menu.continueItem.enabled = false

        newGameOptions = assetManager.get<GamePrefs>(PREFS_NEW_GAME)
        menu.newGameOptions = newGameOptions
        prefs += newGameOptions

        settings = assetManager.get<GamePrefs>(PREFS_SETTINGS)
        menu.settings = settings
        prefs += settings

        menu.rules = assetManager.get(MD_RULES)

        menu.continueListener = {
            initGame(Game(settings, Gdx.files.local(SAVED_GAME)))
        }

        menu.startGameListener = {
            initGame(Game(settings, newGameOptions))
        }

        gameLayout = GameLayout(assetManager, settings).apply {
            gameMenu = menu
        }
    }

    companion object {
        private const val PREFS_NEW_GAME = "new-game-options.json"
        private const val PREFS_SETTINGS = "settings.json"
        private const val MD_RULES = "rules"
    }

}
