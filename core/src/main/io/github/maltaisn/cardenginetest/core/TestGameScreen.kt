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

package io.github.maltaisn.cardenginetest.core

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import io.github.maltaisn.cardengine.CardGameScreen
import io.github.maltaisn.cardengine.PCardSpriteLoader
import io.github.maltaisn.cardengine.core.PCard
import io.github.maltaisn.cardengine.widget.AnimationLayer
import io.github.maltaisn.cardengine.widget.CardActor
import io.github.maltaisn.cardengine.widget.CardContainer
import io.github.maltaisn.cardengine.widget.CardHand


class TestGameScreen(game: TestGame) : CardGameScreen(game) {

    init {
        val cardLoader = PCardSpriteLoader(assetManager)
        assetManager.finishLoading()
        cardLoader.initialize()

        //isDebugAll = true

        val deck = PCard.fullDeck(true)
        deck.shuffle()


        repeat(4) {
            val column = CardHand(cardLoader)
            gameLayer.add(column).pad(30f, 20f, 30f, 20f).grow()
            with(column) {
                horizontal = false
                cardSize = CardActor.CARD_SIZE_NORMAL
                alignment = Align.top
                setCards(deck.drawTop(5))
                setDragListener(object : CardContainer.DragListener {
                    override fun onCardDragged(actor: CardActor): AnimationLayer.CardDragListener? {
                        val start = column.findIndexOfCardActor(actor)
                        val actors = Array(column.size - start) { column.getCardActorAt(it + start) }
                        return animationLayer.dragCards(*actors)
                    }
                })
                setPlayListener(object : CardContainer.PlayListener {
                    override fun canCardsBePlayed(actors: Array<CardActor>, src: CardContainer) = true

                    override fun onCardsPlayed(actors: Array<CardActor>, src: CardContainer, pos: Vector2) {
                        for (actor in actors) {
                            animationLayer.moveCard(src, column,
                                    src.findIndexOfCardActor(actor), column.size)
                        }
                    }
                })
            }
        }

        /*
        val group1 = CardHand(cardLoader)
        val group2 = CardHand(cardLoader)
        val stack1 = CardStack(cardLoader)
        val stack2 = CardStack(cardLoader)

        // Do the layout
        val table = Table()
        table.add(stack1).pad(20f).grow()
        table.add(stack2).pad(20f).grow()
        table.row()
        table.add(group2).colspan(2).pad(20f).padBottom(0f).grow()
        gameLayer.add(group1).pad(20f).fill()
        gameLayer.add(table).grow()

        with(group1) {
            sorter = PCard.DEFAULT_SORTER
            cardSize = CardActor.CARD_SIZE_SMALL
            horizontal = false
            setCards(deck.drawTop(3))
            addClickListener(object : CardContainer.ClickListener {
                override fun onCardClicked(actor: CardActor, index: Int) {
                    animationLayer.moveCard(group1, stack2, index, stack2.size)
                    group1.sort()
                    animationLayer.update()
                }
            })
            addLongClickListener(object : CardContainer.LongClickListener {
                override fun onCardLongClicked(actor: CardActor, index: Int) {
                    animationLayer.moveCard(group1, group2, index, 0)
                    group1.sort()
                    group2.sort()
                    animationLayer.update()
                }
            })
            setDragListener(object : CardContainer.DragListener {
                override fun onCardDragged(actor: CardActor) =
                        if ((actor.card as PCard).color == PCard.RED) {
                            animationLayer.dragCards(actor)
                        } else {
                            null
                        }
            })
            setPlayListener(object: CardContainer.PlayListener {
                override fun canCardsBePlayed(actors: Array<CardActor>, src: CardContainer): Boolean {
                    return src === group2 && (actors.first().card as PCard).color == PCard.BLACK
                }

                override fun onCardsPlayed(actors: Array<CardActor>, src: CardContainer, pos: Vector2) {
                    animationLayer.moveCard(src, group1,
                            src.findIndexOfCardActor(actors.first()), 0)
                    group1.sort()
                    (src as? CardHand)?.sort()
                }
            })
            invalidate()
        }

        with(group2) {
            sorter = PCard.DEFAULT_SORTER
            cardSize = CardActor.CARD_SIZE_BIG
            alignment = Align.bottom
            clipPercent = 0.3f
            setCards(deck.drawTop(6))
            addClickListener(object : CardContainer.ClickListener {
                override fun onCardClicked(actor: CardActor, index: Int) {
                    animationLayer.moveCard(group2, group1, index, 0)
                    group1.sort()
                    group2.sort()
                    animationLayer.update()
                }
            })
            setDragListener(object : CardContainer.DragListener {
                override fun onCardDragged(actor: CardActor) = animationLayer.dragCards(actor)
            })
            invalidate()
        }

        with(stack1) {
            val cards = PCard.fullDeck(false)
            PCard.DEFAULT_SORTER.initialize(cards)
            cards.sortWith(PCard.DEFAULT_SORTER)
            cards.reverse()
            visibility = CardContainer.Visibility.NONE
            setCards(cards)
            addClickListener(object : CardContainer.ClickListener {
                override fun onCardClicked(actor: CardActor, index: Int) {
                    animationLayer.moveCard(stack1, group2, index, 0)
                    group2.sort()
                    animationLayer.update()
                }
            })
            setDragListener(object : CardContainer.DragListener {
                override fun onCardDragged(actor: CardActor) = animationLayer.dragCards(actor)
            })
            setPlayListener(object: CardContainer.PlayListener {
                override fun canCardsBePlayed(actors: Array<CardActor>, src: CardContainer): Boolean {
                    return src === group1
                }

                override fun onCardsPlayed(actors: Array<CardActor>, src: CardContainer, pos: Vector2) {
                    animationLayer.moveCard(src, stack1,
                            src.findIndexOfCardActor(actors.first()), stack1.size)
                    (src as? CardHand)?.sort()
                }
            })
            invalidate()
        }

        with(stack2) {
            visibility = CardContainer.Visibility.ALL
            drawSlot = true
            cardSize = CardActor.CARD_SIZE_NORMAL
            setCards(deck.drawTop(1))
            addClickListener(object : CardContainer.ClickListener {
                override fun onCardClicked(actor: CardActor, index: Int) {
                    animationLayer.moveCard(stack2, stack1, index, stack1.size)
                    animationLayer.update()
                }
            })
            setPlayListener(object: CardContainer.PlayListener {
                override fun canCardsBePlayed(actors: Array<CardActor>, src: CardContainer): Boolean {
                    return src === group1 || src === stack1
                }

                override fun onCardsPlayed(actors: Array<CardActor>, src: CardContainer, pos: Vector2) {
                    animationLayer.moveCard(src, stack2,
                            src.findIndexOfCardActor(actors.first()), stack2.size)
                    (src as? CardHand)?.sort()
                }
            })
            invalidate()
        }

        addListener(object : InputListener() {
            override fun keyUp(event: InputEvent, keycode: Int): Boolean {
                if (keycode == Input.Keys.A) {
                    animationLayer.deal(stack1, group2, 10) {
                        group2.sort()
                    }
                    return true
                }
                return false
            }
        })
        */
    }

}