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
import com.maltaisn.cardgame.core.CardGame
import com.maltaisn.cardgame.markdown.Markdown
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.widget.menu.DefaultGameMenu
import com.maltaisn.nines.core.game.*
import ktx.assets.load
import ktx.log.info


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

        newGameOptions = assetManager.get<GamePrefs>(Res.PREFS_NEW_GAME)
        settings = assetManager.get<GamePrefs>(Res.PREFS_SETTINGS)

        val menu = DefaultGameMenu(coreSkin)
        val layout = GameLayout(assetManager, settings)
        layout.gameMenu = menu
        gameLayout = layout

        // Main menu
        menu.continueListener = {
            loadGame()
        }
        menu.continueItem.enabled = SAVED_GAME_FILE.exists()

        // New game submenu
        menu.newGameOptions = newGameOptions
        prefs += newGameOptions
        menu.startGameListener = {
            startGame()
        }

        // Settings submenu
        menu.settings = settings
        prefs += settings

        // Rules submenu
        menu.rules = assetManager.get(Res.MD_RULES)

        // In-game menu
        menu.exitGameListener = {
            saveGame()
            (gameLayout as GameLayout).hide()
        }
        menu.scoreboardListener = {
            info { "Show scoreboard" }
        }
    }

    override fun pause() {
        super.pause()
        saveGame()
    }

    override fun resume() {
        super.resume()
        loadGame()
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

    private fun saveGame() {
        game?.save(gameSaveJson, SAVED_GAME_FILE)
    }

    private fun loadGame() {
        if (SAVED_GAME_FILE.exists()) {
            initGame(Game.load(settings, gameSaveJson, SAVED_GAME_FILE))
        }
    }

    companion object {
        private val SAVED_GAME_FILE = Gdx.files.local("saved-game.json")
    }

}
