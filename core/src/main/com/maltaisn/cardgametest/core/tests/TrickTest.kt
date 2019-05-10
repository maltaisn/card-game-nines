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
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.utils.Align
import com.maltaisn.cardgame.CardGameLayout
import com.maltaisn.cardgame.core.PCard
import com.maltaisn.cardgame.widget.Popup
import com.maltaisn.cardgame.widget.PopupButton
import com.maltaisn.cardgame.widget.card.CardActor
import com.maltaisn.cardgame.widget.card.CardContainer
import com.maltaisn.cardgame.widget.card.CardHand
import com.maltaisn.cardgame.widget.card.CardTrick
import com.maltaisn.cardgametest.core.TestGameApp
import ktx.actors.onClick


class TrickTest(game: TestGameApp) : CardGameTest(game) {

    override fun layout(layout: CardGameLayout) {
        super.layout(layout)

        val deck = PCard.fullDeck(true)
        deck.shuffle()

        val trick = CardTrick(coreSkin, cardSkin, 4)
        val hand = CardHand(coreSkin, cardSkin)
        val popup = Popup(coreSkin)

        trick.playListener = object : CardContainer.PlayListener {
            override fun canCardsBePlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2): Boolean {
                val index = trick.findInsertPositionForCoordinates(pos.x, pos.y)
                return trick.actors[index] == null
            }

            override fun onCardsPlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2) {
                actors.first().highlighted = false
                val index = trick.findInsertPositionForCoordinates(pos.x, pos.y)
                layout.cardAnimationLayer.moveCard(src, trick,
                        src.actors.indexOf(actors.first()), index,
                        replaceDst = true)
            }
        }
        trick.dragListener = { actor ->
            val dragger = layout.cardAnimationLayer.dragCards(actor)
            dragger?.rearrangeable = true
            dragger
        }
        trick.clickListener = { _, _ ->
            for (i in 0 until trick.size) {
                if (trick.actors[i] != null) {
                    layout.cardAnimationLayer.moveCard(trick, hand, i, hand.size, replaceSrc = true)
                }
            }
            layout.cardAnimationLayer.update()
        }

        hand.cards = deck.drawTop(12)
        hand.align = Align.bottom
        hand.clipPercent = 0.3f
        hand.dragListener = { actor ->
            val dragger = layout.cardAnimationLayer.dragCards(actor)
            dragger?.rearrangeable = true
            dragger
        }
        hand.highlightListener = { actor, _ ->
            if ((actor.card as PCard).color == PCard.RED) {
                if (popup.shown) popup.hide() else popup.show(hand, Popup.Side.ABOVE)
                true
            } else {
                false
            }
        }

        layout.gameLayer.centerTable.add(trick).growY().pad(30f).row()
        layout.gameLayer.centerTable.add(hand).grow().pad(0f, 30f, 0f, 30f)

        // Popup
        val popupBtn = PopupButton(coreSkin, "Draw a card")
        popupBtn.onClick { popup.hide() }
        popup.add(popupBtn)

        /*
        popup.add(PopupButton(coreSkin, "Last two")).fillX()
        popup.add(PopupButton(coreSkin, "Hearts")).fillX().row()
        popup.add(PopupButton(coreSkin, "Barbu")).fillX()
        popup.add(PopupButton(coreSkin, "Queens")).fillX().row()
        popup.add(PopupButton(coreSkin, "Domino")).fillX()
        popup.add(PopupButton(coreSkin, "Tricks")).fillX().row()
        popup.add(PopupButton(coreSkin, "Trump")).colspan(2)
        */

        layout.popupGroup.addActor(popup)

        // Transition tests
        addListener(object : InputListener() {
            override fun keyUp(event: InputEvent, keycode: Int): Boolean {
                when (keycode) {
                    Input.Keys.E -> popupBtn.enabled = !popupBtn.enabled
                    Input.Keys.P -> if (popup.shown) popup.hide() else popup.show(hand, Popup.Side.ABOVE)
                    Input.Keys.F -> hand.fade(!hand.shown)
                    Input.Keys.UP -> hand.slide(!hand.shown, CardContainer.Direction.UP)
                    Input.Keys.DOWN -> hand.slide(!hand.shown, CardContainer.Direction.DOWN)
                    Input.Keys.LEFT -> hand.slide(!hand.shown, CardContainer.Direction.LEFT)
                    Input.Keys.RIGHT -> hand.slide(!hand.shown, CardContainer.Direction.RIGHT)
                    else -> return false
                }
                return true
            }
        })
    }

}
