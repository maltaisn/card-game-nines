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
import com.maltaisn.cardgame.CardGameLayout
import com.maltaisn.cardgame.core.Card
import com.maltaisn.cardgame.core.PCard
import com.maltaisn.cardgame.widget.card.CardHand
import com.maltaisn.cardgametest.core.TestGameApp


class NullDealTest(game: TestGameApp) : CardGameTest(game) {

    override fun layout(layout: CardGameLayout) {
        super.layout(layout)

        val deck = PCard.fullDeck(false)
        deck.shuffle()

        val count = 16

        val hand1 = CardHand(coreSkin, cardSkin)
        layout.gameLayer.centerTable.add(hand1).pad(30f).grow().row()
        hand1.cards = deck.drawTop(count)

        val hand2 = CardHand(coreSkin, cardSkin)
        hand2.cards = arrayOfNulls<Card>(count).toList()
        layout.gameLayer.centerTable.add(hand2).pad(30f).grow()

        addListener(object : InputListener() {
            override fun keyUp(event: InputEvent, keycode: Int): Boolean {
                if (keycode == Input.Keys.A) {
                    if (layout.cardAnimationLayer.animationRunning) {
                        layout.cardAnimationLayer.completeAnimation(true)
                    }
                    if (hand1.actors.first() != null) {
                        layout.cardAnimationLayer.deal(hand1, hand2, count, replaceSrc = true, replaceDst = true)
                    } else {
                        layout.cardAnimationLayer.deal(hand2, hand1, count, replaceSrc = true, replaceDst = true)
                    }
                    return true
                }
                return false

            }
        })
    }

}