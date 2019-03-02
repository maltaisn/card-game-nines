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

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.assets.loaders.SkinLoader
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.gmail.blueboxware.libgdxplugin.annotations.GDXAssets
import io.github.maltaisn.cardengine.CardGameScreen
import io.github.maltaisn.cardengine.Resources
import io.github.maltaisn.cardengine.core.Card
import io.github.maltaisn.cardengine.core.PCard
import io.github.maltaisn.cardengine.widget.*
import ktx.actors.plusAssign
import ktx.assets.getAsset
import ktx.assets.load


class TestGameScreen(game: TestGame) : CardGameScreen(game) {

    @GDXAssets(skinFiles = ["assets/engine/pcard/pcard.skin"],
            atlasFiles = ["assets/engine/pcard/pcard.atlas"])
    private val cardSkin: Skin

    init {
        assetManager.load<TextureAtlas>(Resources.PCARD_SKIN_ATLAS)
        assetManager.load<Skin>(Resources.PCARD_SKIN, SkinLoader.SkinParameter(Resources.PCARD_SKIN_ATLAS))
        assetManager.finishLoading()

        cardSkin = assetManager.getAsset(Resources.PCARD_SKIN)

        //isDebugAll = true
        Gdx.app.logLevel = Application.LOG_DEBUG

        //setupDeal()
        setupTrick()
        //setupSolitaire()
        //setupNullDeal()
        //setupCardLoop()
    }

    private fun setupDeal() {
        val deck = PCard.fullDeck(true)
        deck.shuffle()

        val hand = CardHand(coreSkin, cardSkin)
        hand.alignment = Align.bottom
        hand.clipPercent = 0.3f

        val stack = CardStack(coreSkin, cardSkin)
        stack.isVisible = false
        stack.cards = deck

        gameLayer.centerTable.add(hand).grow()
        gameLayer.bottomTable.add(stack).grow()

        addListener(object : InputListener() {
            override fun keyUp(event: InputEvent, keycode: Int): Boolean {
                if (keycode == Input.Keys.A) {
                    animationLayer.deal(stack, hand, 12)
                    return true
                }
                return false
            }
        })
    }

    private fun setupTrick() {
        val deck = PCard.fullDeck(true)
        deck.shuffle()

        val trick = CardTrick(coreSkin, cardSkin, 4)
        val hand = CardHand(coreSkin, cardSkin)

        trick.setPlayListener(object : CardContainer.PlayListener {
            override fun canCardsBePlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2): Boolean {
                val index = trick.findInsertPositionForCoordinates(pos.x, pos.y)
                return trick.actors[index] == null
            }

            override fun onCardsPlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2) {
                val index = trick.findInsertPositionForCoordinates(pos.x, pos.y)
                animationLayer.moveCard(src, trick,
                        src.actors.indexOf(actors.first()), index,
                        replaceDst = true)
            }
        })
        trick.setDragListener(object : CardContainer.DragListener {
            override fun onCardDragged(actor: CardActor): AnimationLayer.CardDragger? {
                val dragger = animationLayer.dragCards(actor)
                dragger?.rearrangeable = true
                return dragger
            }
        })
        trick.addClickListener(object : CardContainer.ClickListener {
            override fun onCardClicked(actor: CardActor, index: Int) {
                for (i in 0 until trick.size) {
                    if (trick.actors[i] != null) {
                        animationLayer.moveCard(trick, hand, i, hand.size, replaceSrc = true)
                    }
                }
                animationLayer.update()
            }
        })

        hand.cards = deck.drawTop(12)
        hand.alignment = Align.bottom
        hand.clipPercent = 0.3f
        hand.setDragListener(object : CardContainer.DragListener {
            override fun onCardDragged(actor: CardActor): AnimationLayer.CardDragger? {
                val dragger = animationLayer.dragCards(actor)
                dragger?.rearrangeable = true
                return dragger
            }
        })

        gameLayer.centerTable.add(trick).growY().pad(30f).row()
        gameLayer.centerTable.add(hand).grow().pad(0f, 30f, 0f, 30f)

        val popup = Popup(coreSkin)
        popup.add(CardHand(coreSkin, cardSkin).apply {
            cards = deck.drawTop(5)
            cardSize = CardActor.SIZE_TINY
        }).pad(5f)
        popupLayer += popup

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

    fun setupSolitaire() {
        val deck = PCard.fullDeck(false)
        deck.shuffle()

        repeat(4) {
            val column = CardHand(coreSkin, cardSkin)
            gameLayer.centerTable.add(column).pad(30f, 20f, 30f, 20f).grow()
            column.apply {
                horizontal = false
                cardSize = CardActor.SIZE_NORMAL
                alignment = Align.top
                cards = deck.drawTop(5)
                setDragListener(object : CardContainer.DragListener {
                    override fun onCardDragged(actor: CardActor): AnimationLayer.CardDragger? {
                        val start = column.actors.indexOf(actor)
                        val actors = mutableListOf<CardActor>()
                        for (i in start until column.size) {
                            column.actors[i]?.let { actors += it }
                        }
                        val dragger = animationLayer.dragCards(*actors.toTypedArray())
                        dragger?.rearrangeable = true
                        return dragger
                    }
                })
                setPlayListener(object : CardContainer.PlayListener {
                    override fun canCardsBePlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2) = true

                    override fun onCardsPlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2) {
                        var insertPos = column.findInsertPositionForCoordinates(pos.x, pos.y)
                        for (actor in actors) {
                            animationLayer.moveCard(src, column,
                                    src.actors.indexOf(actor), insertPos)
                            insertPos++
                        }
                    }
                })
            }
        }
    }

    fun setupNullDeal() {
        val deck = PCard.fullDeck(false)
        deck.shuffle()

        val count = 16

        val hand1 = CardHand(coreSkin, cardSkin)
        gameLayer.centerTable.add(hand1).pad(30f).grow().row()
        hand1.cards = deck.drawTop(count)

        val hand2 = CardHand(coreSkin, cardSkin)
        hand2.cards = arrayOfNulls<Card>(count).toList()
        gameLayer.centerTable.add(hand2).pad(30f).grow()

        addListener(object : InputListener() {
            override fun keyUp(event: InputEvent, keycode: Int): Boolean {
                if (keycode == Input.Keys.A) {
                    if (animationLayer.animationRunning) {
                        animationLayer.completeAnimation(true)
                    }
                    if (hand1.actors.first() != null) {
                        animationLayer.deal(hand1, hand2, count, replaceSrc = true, replaceDst = true)
                    } else {
                        animationLayer.deal(hand2, hand1, count, replaceSrc = true, replaceDst = true)
                    }
                    return true
                }
                return false

            }
        })
    }

    fun setupCardLoop() {
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
            setPlayListener(object : CardContainer.PlayListener {
                override fun canCardsBePlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2): Boolean {
                    return src === group2 && (actors.first().card as PCard).color == PCard.BLACK
                }

                override fun onCardsPlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2) {
                    animationLayer.moveCard(src, group1,
                            src.actors.indexOf(actors.first()), 0)
                    group1.sort()
                    (src as? CardHand)?.sort()
                }
            })
        }

        group2.apply {
            sorter = PCard.DEFAULT_SORTER
            cardSize = CardActor.SIZE_BIG
            alignment = Align.bottom
            clipPercent = 0.3f
            cards = deck.drawTop(6)
            sort()
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
        }

        stack1.apply {
            val stackCards = PCard.fullDeck(false)
            PCard.DEFAULT_SORTER.initialize(stackCards)
            stackCards.sortWith(PCard.DEFAULT_SORTER)
            stackCards.reverse()

            visibility = CardContainer.Visibility.NONE
            cards = stackCards
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
            setPlayListener(object : CardContainer.PlayListener {
                override fun canCardsBePlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2): Boolean {
                    return src === group1
                }

                override fun onCardsPlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2) {
                    animationLayer.moveCard(src, stack1,
                            src.actors.indexOf(actors.first()), stack1.size)
                    (src as? CardHand)?.sort()
                }
            })
        }

        stack2.apply {
            visibility = CardContainer.Visibility.ALL
            drawSlot = true
            cardSize = CardActor.SIZE_NORMAL
            cards = deck.drawTop(1)
            addClickListener(object : CardContainer.ClickListener {
                override fun onCardClicked(actor: CardActor, index: Int) {
                    animationLayer.moveCard(stack2, stack1, index, stack1.size)
                    animationLayer.update()
                }
            })
            setPlayListener(object : CardContainer.PlayListener {
                override fun canCardsBePlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2): Boolean {
                    return src === group1 || src === stack1
                }

                override fun onCardsPlayed(actors: List<CardActor>, src: CardContainer, pos: Vector2) {
                    animationLayer.moveCard(src, stack2,
                            src.actors.indexOf(actors.first()), stack2.size)
                    (src as? CardHand)?.sort()
                }
            })
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
    }

}