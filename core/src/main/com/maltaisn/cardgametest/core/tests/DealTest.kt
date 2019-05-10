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
import com.badlogic.gdx.utils.Align
import com.maltaisn.cardgame.CardGameLayout
import com.maltaisn.cardgame.core.PCard
import com.maltaisn.cardgame.widget.card.CardHand
import com.maltaisn.cardgame.widget.card.CardStack
import com.maltaisn.cardgametest.core.TestGameApp


class DealTest(game: TestGameApp) : CardGameTest(game) {

    override fun layout(layout: CardGameLayout) {
        super.layout(layout)

        val deck = PCard.fullDeck(true)
        deck.shuffle()

        val hand = CardHand(coreSkin, cardSkin)
        hand.align = Align.bottom
        hand.clipPercent = 0.3f

        val stack = CardStack(coreSkin, cardSkin)
        stack.isVisible = false
        stack.cards = deck

        layout.gameLayer.centerTable.add(hand).grow()
        layout.gameLayer.bottomTable.add(stack).grow()

        addListener(object : InputListener() {
            override fun keyUp(event: InputEvent, keycode: Int): Boolean {
                if (keycode == Input.Keys.A) {
                    layout.cardAnimationLayer.deal(stack, hand, 12)
                    return true
                }
                return false
            }
        })
    }

}
