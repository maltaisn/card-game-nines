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
import com.badlogic.gdx.utils.I18NBundle
import com.maltaisn.cardgame.CardGameScreen
import com.maltaisn.cardgame.markdown.Markdown
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.widget.menu.DefaultGameMenu
import com.maltaisn.nines.core.core.HumanPlayer
import com.maltaisn.nines.core.core.MctsPlayer
import ktx.assets.load


class GameScreen : CardGameScreen() {

    private lateinit var settings: GamePrefs
    private lateinit var newGameOptions: GamePrefs


    override fun load() {
        super.load()

        assetManager.load<GamePrefs>(Res.PREFS_NEW_GAME)
        assetManager.load<GamePrefs>(Res.PREFS_SETTINGS)
        assetManager.load<Markdown>(Res.MD_RULES)
        assetManager.load<I18NBundle>(Res.STRINGS_BUNDLE)
        loadPCardSkin()
    }

    override fun start() {
        super.start()

        val menu = DefaultGameMenu(coreSkin)
        menu.continueItem.enabled = false

        newGameOptions = assetManager.get<GamePrefs>(Res.PREFS_NEW_GAME)
        menu.newGameOptions = newGameOptions
        prefs += newGameOptions

        settings = assetManager.get<GamePrefs>(Res.PREFS_SETTINGS)
        menu.settings = settings
        prefs += settings

        menu.rules = assetManager.get(Res.MD_RULES)

        menu.continueListener = { initGame(Game(settings, Gdx.files.local(SAVED_GAME))) }
        menu.startGameListener = { startGame() }

        gameLayout = GameLayout(assetManager, settings).apply {
            gameMenu = menu
        }
    }

    private fun startGame() {
        val difficulty = when (newGameOptions.getInt(PrefKeys.DIFFICULTY)) {
            0 -> MctsPlayer.Difficulty.BEGINNER
            1 -> MctsPlayer.Difficulty.INTERMEDIATE
            2 -> MctsPlayer.Difficulty.ADVANCED
            3 -> MctsPlayer.Difficulty.EXPERT
            else -> error("Wrong difficulty level.")
        }

        // Create players
        val south = HumanPlayer()
        val east = MctsPlayer(difficulty)
        val north = MctsPlayer(difficulty)

        val game = Game(settings, south, east, north)
        initGame(game)
        game.start()
    }

}
