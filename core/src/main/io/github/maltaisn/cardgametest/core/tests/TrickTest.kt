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

package io.github.maltaisn.cardgametest.core.tests

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.utils.Align
import io.github.maltaisn.cardgame.core.PCard
import io.github.maltaisn.cardgame.widget.Popup
import io.github.maltaisn.cardgame.widget.PopupButton
import io.github.maltaisn.cardgame.widget.card.CardActor
import io.github.maltaisn.cardgame.widget.card.CardContainer
import io.github.maltaisn.cardgame.widget.card.CardHand
import io.github.maltaisn.cardgame.widget.card.CardTrick
import io.github.maltaisn.cardgametest.core.TestGame
import ktx.actors.plusAssign


class TrickTest(game: TestGame) : CardGameTest(game) {

    init {
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
                cardAnimationLayer.moveCard(src, trick,
                        src.actors.indexOf(actors.first()), index,
                        replaceDst = true)
            }
        }
        trick.dragListener = { actor ->
            val dragger = cardAnimationLayer.dragCards(actor)
            dragger?.rearrangeable = true
            dragger
        }
        trick.clickListener = { _, _ ->
            for (i in 0 until trick.size) {
                if (trick.actors[i] != null) {
                    cardAnimationLayer.moveCard(trick, hand, i, hand.size, replaceSrc = true)
                }
            }
            cardAnimationLayer.update()
        }

        hand.cards = deck.drawTop(12)
        hand.align = Align.bottom
        hand.clipPercent = 0.3f
        hand.dragListener = { actor ->
            val dragger = cardAnimationLayer.dragCards(actor)
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

        gameLayer.centerTable.add(trick).growY().pad(30f).row()
        gameLayer.centerTable.add(hand).grow().pad(0f, 30f, 0f, 30f)

        popup.add(PopupButton(coreSkin, "Draw a card").apply { clickListener = { popup.hide() } })
        /*
        popup.add(PopupButton(coreSkin, "Last two")).fillX()
        popup.add(PopupButton(coreSkin, "Hearts")).fillX().row()
        popup.add(PopupButton(coreSkin, "Barbu")).fillX()
        popup.add(PopupButton(coreSkin, "Queens")).fillX().row()
        popup.add(PopupButton(coreSkin, "Domino")).fillX()
        popup.add(PopupButton(coreSkin, "Tricks")).fillX().row()
        popup.add(PopupButton(coreSkin, "Trump")).colspan(2)
        */
        popupGroup += popup

        // Transition tests
        addListener(object : InputListener() {
            override fun keyUp(event: InputEvent, keycode: Int): Boolean {
                when (keycode) {
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