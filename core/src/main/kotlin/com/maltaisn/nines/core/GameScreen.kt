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

import com.badlogic.gdx.utils.I18NBundle
import com.maltaisn.cardgame.CardGameScreen
import com.maltaisn.cardgame.markdown.Markdown
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.nines.core.game.GameSaveJson
import ktx.assets.load

class GameScreen : CardGameScreen() {

    private lateinit var gameLayout: GameLayout

    private lateinit var newGameOptions: GamePrefs
    private lateinit var settings: GamePrefs

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

        newGameOptions = assetManager.get(Res.PREFS_NEW_GAME)
        settings = assetManager.get(Res.PREFS_SETTINGS)

        gameLayout = GameLayout(assetManager, newGameOptions, settings)
        addActor(gameLayout)

        prefs += newGameOptions
        prefs += settings
    }

    override fun pause() {
        super.pause()
        gameLayout.game?.save(GameSaveJson)
    }

    override fun resume() {
        // TODO resume game correctly
        //loadGame()
    }

}
