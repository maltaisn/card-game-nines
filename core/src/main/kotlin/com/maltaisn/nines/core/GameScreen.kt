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

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.I18NBundle
import com.maltaisn.cardgame.CardGameScreen
import com.maltaisn.cardgame.markdown.Markdown
import com.maltaisn.cardgame.pcard.PCardRes
import com.maltaisn.cardgame.prefs.GamePrefs
import ktx.assets.load

class GameScreen : CardGameScreen() {

    private lateinit var gameLayout: GameLayout

    override fun load() {
        super.load()

        assetManager.load<TextureAtlas>(PCardRes.SKIN_ATLAS)

        assetManager.load<GamePrefs>(Res.PREFS_NEW_GAME)
        assetManager.load<GamePrefs>(Res.PREFS_SETTINGS)
        assetManager.load<Markdown>(Res.MD_RULES)
        assetManager.load<I18NBundle>(Res.STRINGS_BUNDLE)
    }

    override fun start() {
        super.start()

        addSkin(PCardRes.SKIN, PCardRes.SKIN_ATLAS)
        addSkin(Res.SKIN)

        skin.add("newGameOptions", assetManager.get<GamePrefs>(Res.PREFS_NEW_GAME))
        skin.add("settings", assetManager.get<GamePrefs>(Res.PREFS_SETTINGS))
        skin.add("rules", assetManager.get<Markdown>(Res.MD_RULES))
        skin.add("default", assetManager.get<I18NBundle>(Res.STRINGS_BUNDLE))

        gameLayout = GameLayout(skin)
        addActor(gameLayout)
    }

    override fun pause() {
        super.pause()
        gameLayout.save()
    }

    override fun resume() {
        // TODO resume game correctly
        //loadGame()
    }

}
