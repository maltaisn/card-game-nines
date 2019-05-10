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

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.maltaisn.cardgame.CardGameLayout
import com.maltaisn.cardgame.CardGameScreen
import com.maltaisn.cardgame.Resources
import com.maltaisn.cardgame.core.CardGame
import com.maltaisn.cardgame.core.GameEvent
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgametest.core.TestGameApp
import ktx.assets.getAsset


abstract class CardGameTest(game: TestGameApp) : CardGameScreen(game) {

    protected lateinit var cardSkin: Skin


    override fun load() {
        super.load()
        loadPCardSkin()
    }

    override fun start() {
        super.start()
        cardSkin = assetManager.getAsset(Resources.PCARD_SKIN)

        val settings = GamePrefs("unused")
        gameLayout = object : CardGameLayout(assetManager, settings) {
            override var shown = true

            init {
                layout(this)
            }

            override fun initGame(game: CardGame) {
                throw UnsupportedOperationException()
            }

            override fun doEvent(event: GameEvent) {
                throw UnsupportedOperationException()
            }
        }
    }

    open fun layout(layout: CardGameLayout) {

    }

}
