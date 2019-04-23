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

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import com.maltaisn.cardgame.core.PCard
import com.maltaisn.cardgame.widget.card.CardActor
import com.maltaisn.cardgame.widget.card.CardContainer
import com.maltaisn.cardgame.widget.card.CardHand
import com.maltaisn.cardgametest.core.TestGame


class SolitaireTest(game: TestGame) : CardGameTest(game) {

    override fun start() {
        super.start()

        val deck = PCard.fullDeck(false)
        deck.shuffle()

        repeat(4) {
            val column = CardHand(coreSkin, cardSkin)
            gameLayer.centerTable.add(column).pad(30f, 20f, 30f, 20f).grow()
            column.apply {
                horizontal = false
                cardSize = CardActor.SIZE_NORMAL
                align = Align.top
                cards = deck.drawTop(5)
                dragListener = { actor ->
                    val start = column.actors.indexOf(actor)
                    val actors = mutableListOf<CardActor>()
                    for (i in start until column.size) {
                        column.actors[i]?.let { actors += it }
                    }
                    val dragger = cardAnimationLayer.dragCards(*actors.toTypedArray())
                    dragger?.rearrangeable = true
                    dragger
                }
                playListener = object : CardContainer.PlayListener {
                    override fun canCardsBePlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2) = true

                    override fun onCardsPlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2) {
                        var insertPos = column.findInsertPositionForCoordinates(pos.x, pos.y)
                        for (actor in actors) {
                            cardAnimationLayer.moveCard(src, column,
                                    src.actors.indexOf(actor), insertPos)
                            insertPos++
                        }
                    }
                }
            }
        }
    }

}