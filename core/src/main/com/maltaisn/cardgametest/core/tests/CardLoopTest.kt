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
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.maltaisn.cardgame.core.PCard
import com.maltaisn.cardgame.widget.card.CardActor
import com.maltaisn.cardgame.widget.card.CardContainer
import com.maltaisn.cardgame.widget.card.CardHand
import com.maltaisn.cardgame.widget.card.CardStack
import com.maltaisn.cardgametest.core.TestGame


class CardLoopTest(game: TestGame) : CardGameTest(game) {

    override fun start() {
        super.start()

        val deck = PCard.fullDeck(false)
        deck.shuffle()

        val group1 = CardHand(coreSkin, cardSkin)
        val group2 = CardHand(coreSkin, cardSkin)
        val stack1 = CardStack(coreSkin, cardSkin)
        val stack2 = CardStack(coreSkin, cardSkin)

        // Do the layout
        val table = Table()
        table.add(stack1).pad(20f).grow()
        table.add(stack2).pad(20f).grow()
        table.row()
        table.add(group2).colspan(2).pad(20f).padBottom(0f).grow()
        gameLayer.centerTable.add(group1).pad(20f).fill()
        gameLayer.centerTable.add(table).grow()

        group1.apply {
            sorter = PCard.DEFAULT_SORTER
            cardSize = CardActor.SIZE_SMALL
            horizontal = false
            cards = deck.drawTop(3)
            sort()
            clickListener = { _, index ->
                cardAnimationLayer.moveCard(group1, stack2, index, stack2.size)
                group1.sort()
                cardAnimationLayer.update()
            }
            longClickListener = { _, index ->
                cardAnimationLayer.moveCard(group1, group2, index, 0)
                group1.sort()
                group2.sort()
                cardAnimationLayer.update()
            }
            dragListener = { actor ->
                if ((actor.card as PCard).color == PCard.RED) {
                    cardAnimationLayer.dragCards(actor)
                } else {
                    null
                }
            }
            playListener = object : CardContainer.PlayListener {
                override fun canCardsBePlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2): Boolean {
                    return src === group2 && (actors.first().card as PCard).color == PCard.BLACK
                }

                override fun onCardsPlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2) {
                    cardAnimationLayer.moveCard(src, group1,
                            src.actors.indexOf(actors.first()), 0)
                    group1.sort()
                    (src as? CardHand)?.sort()
                }
            }
        }

        group2.apply {
            sorter = PCard.DEFAULT_SORTER
            cardSize = CardActor.SIZE_BIG
            align = Align.bottom
            clipPercent = 0.3f
            cards = deck.drawTop(6)
            sort()
            clickListener = { _, index ->
                cardAnimationLayer.moveCard(group2, group1, index, 0)
                group1.sort()
                group2.sort()
                cardAnimationLayer.update()
            }
            dragListener = { actor ->
                cardAnimationLayer.dragCards(actor)
            }
        }

        stack1.apply {
            val stackCards = PCard.fullDeck(false)
            PCard.DEFAULT_SORTER.initialize(stackCards)
            stackCards.sortWith(PCard.DEFAULT_SORTER)
            stackCards.reverse()

            visibility = CardContainer.Visibility.NONE
            cards = stackCards
            clickListener = { _, index ->
                cardAnimationLayer.moveCard(stack1, group2, index, 0)
                group2.sort()
                cardAnimationLayer.update()
            }
            dragListener = { actor -> cardAnimationLayer.dragCards(actor) }
            playListener = object : CardContainer.PlayListener {
                override fun canCardsBePlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2): Boolean {
                    return src === group1
                }

                override fun onCardsPlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2) {
                    cardAnimationLayer.moveCard(src, stack1,
                            src.actors.indexOf(actors.first()), stack1.size)
                    (src as? CardHand)?.sort()
                }
            }
        }

        stack2.apply {
            visibility = CardContainer.Visibility.ALL
            drawSlot = true
            cardSize = CardActor.SIZE_NORMAL
            cards = deck.drawTop(1)
            clickListener = { _, index ->
                cardAnimationLayer.moveCard(stack2, stack1, index, stack1.size)
                cardAnimationLayer.update()
            }
            playListener = object : CardContainer.PlayListener {
                override fun canCardsBePlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2): Boolean {
                    return src === group1 || src === stack1
                }

                override fun onCardsPlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2) {
                    cardAnimationLayer.moveCard(src, stack2,
                            src.actors.indexOf(actors.first()), stack2.size)
                    (src as? CardHand)?.sort()
                }
            }
        }

        addListener(object : InputListener() {
            override fun keyUp(event: InputEvent, keycode: Int): Boolean {
                if (keycode == Input.Keys.A) {
                    cardAnimationLayer.deal(stack1, group2, 10) {
                        group2.sort()
                    }
                    return true
                }
                return false
            }
        })
    }

}