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

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
import com.gmail.blueboxware.libgdxplugin.annotations.GDXAssets
import com.maltaisn.cardgame.CoreRes
import com.maltaisn.cardgame.core.CardGame
import com.maltaisn.cardgame.core.CardGameEvent
import com.maltaisn.cardgame.core.PCard
import com.maltaisn.cardgame.core.sortWith
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.prefs.PrefEntry
import com.maltaisn.cardgame.widget.CardGameLayout
import com.maltaisn.cardgame.widget.Popup
import com.maltaisn.cardgame.widget.PopupButton
import com.maltaisn.cardgame.widget.TimeAction
import com.maltaisn.cardgame.widget.card.*
import com.maltaisn.nines.core.core.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import ktx.actors.onClick
import ktx.async.AsyncExecutorDispatcher
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import ktx.async.onRenderingThread
import kotlin.math.PI


class GameLayout(assetManager: AssetManager, settings: GamePrefs) :
        CardGameLayout(assetManager, settings) {

    @GDXAssets(propertiesFiles = ["assets/strings.properties"])
    private val bundle = assetManager.get<I18NBundle>(Res.STRINGS_BUNDLE)

    private val hiddenStacks: List<CardStack>
    private val playerHand: CardHand
    private val extraHand: CardHand
    private val trick: CardTrick

    private val tradePopup: Popup
    private val collectPopup: Popup

    private val gameSpeedDelay: Float
        get() = when (settings.getChoice(PrefKeys.GAME_SPEED)) {
            "slow" -> 1.5f
            "normal" -> 1f
            "fast" -> 0.5f
            else -> 0f
        }

    /** System time when the last move was made. */
    private var lastMoveTime = 0L

    private lateinit var dispatcher: AsyncExecutorDispatcher
    private var aiPlayerJob: Job? = null


    init {
        val cardSkin = assetManager.get<Skin>(CoreRes.PCARD_SKIN)

        playerHand = CardHand(coreSkin, cardSkin).apply {
            sorter = PCard.DEFAULT_SORTER
            clipPercent = 0.3f
            align = Align.bottom
            cardSize = CardActor.SIZE_NORMAL
            shown = false
        }

        hiddenStacks = List(3) {
            CardStack(coreSkin, cardSkin).apply {
                visibility = CardContainer.Visibility.NONE
            }
        }

        trick = CardTrick(coreSkin, cardSkin, 3).apply {
            shown = false
        }

        extraHand = CardHand(coreSkin, cardSkin).apply {
            sorter = PCard.DEFAULT_SORTER
            visibility = CardContainer.Visibility.NONE
            cardSize = CardActor.SIZE_SMALL
            maxCardSpacing = 30f
            enabled = false
            shown = false
        }

        gameLayer.apply {
            bottomTable.add(hiddenStacks[0]).grow()
            leftTable.add(hiddenStacks[1]).grow()
            topTable.add(hiddenStacks[2]).grow()

            centerTable.add(Stack(trick, extraHand)).grow().row()
            centerTable.add(playerHand).growX()
        }

        cardAnimationLayer.register(playerHand, *hiddenStacks.toTypedArray(), trick, extraHand)

        // Trade hand popup
        tradePopup = Popup(coreSkin)
        popupGroup.addActor(tradePopup)

        val tradeBtn = PopupButton(coreSkin, bundle["action_trade"])
        tradeBtn.onClick {
            val game = game as Game
            val state = game.gameState as GameState
            game.doMove(state.getMoves().find { it is TradeHandMove && it.trade }!!)
            tradePopup.hide()
        }

        val noTradeBtn = PopupButton(coreSkin, bundle["action_no_trade"])
        noTradeBtn.onClick {
            val game = game as Game
            val state = game.gameState as GameState
            game.doMove(state.getMoves().find { it is TradeHandMove && !it.trade }!!)
            tradePopup.hide()
        }

        tradePopup.apply {
            add(tradeBtn).minWidth(150f)
            add(noTradeBtn).minWidth(150f)
        }

        // Collect trick popup
        collectPopup = Popup(coreSkin)
        popupGroup.addActor(collectPopup)

        val collectBtn = PopupButton(coreSkin, bundle["action_ok"])
        collectBtn.onClick {
            val moveDuration = collectTrick(hiddenStacks.first(), 0f)
            collectPopup.hide()

            doDelayed(moveDuration) {
                playNext()
            }
        }
        collectPopup.add(collectBtn).minWidth(150f)
    }


    override fun setStage(stage: Stage?) {
        super.setStage(stage)
        if (stage != null) {
            dispatcher = newSingleThreadAsyncContext()
        } else {
            dispatcher.dispose()
            aiPlayerJob?.cancel()
            aiPlayerJob = null
        }
    }

    override fun initGame(game: CardGame) {
        super.initGame(game)

        cardAnimationLayer.completeAnimation(true)

        game as Game
        when (game.phase) {
            Game.Phase.ENDED, Game.Phase.GAME_STARTED -> {
                // Round wasn't started yet or has just ended. Hide all containers.
                hide()
            }
            Game.Phase.ROUND_STARTED -> {
                // Round has started
                val state = game.gameState as GameState

                if (state.phase == GameState.Phase.TRADE) {
                    // Trade phase: show extra hand
                    extraHand.apply {
                        cards = state.extraHand.cards
                        fade(true)
                    }
                } else {
                    // Play phase: show trick
                    trick.apply {
                        cards = state.currentTrick.cards
                        fade(true)
                    }
                }

                playerHand.apply {
                    cards = state.players.first().hand.cards
                    slide(true, CardContainer.Direction.DOWN)
                }
            }
        }
    }

    fun hide() {
        // Clear all animations
        cardAnimationLayer.completeAnimation(true)
        clearActions()

        // Hide all containers and all popups
        extraHand.fade(false)
        trick.fade(false)
        playerHand.slide(false, CardContainer.Direction.DOWN)
        tradePopup.hide()
        collectPopup.hide()

        // Cancel AI job
        aiPlayerJob?.cancel()
        aiPlayerJob = null
    }

    override fun doEvent(event: CardGameEvent) {
        event as GameEvent
        when (event) {
            GameEvent.Start -> startGame()
            GameEvent.End -> endGame()
            GameEvent.RoundStart -> startRound()
            GameEvent.RoundEnd -> endRound()
            is GameEvent.Move -> doMove(event)
        }
    }

    private fun startGame() {
        (game as Game?)?.startRound()
    }

    private fun endGame() {
        // TODO Show game over dialog
    }

    private fun startRound() {
        val game = game as Game
        val state = game.gameState as GameState

        var moveDuration = 0.5f

        // Set player hand
        val playerCards = game.players.first().hand.cards.toMutableList()
        playerCards.sortWith(PCard.DEFAULT_SORTER)

        val hiddenStack = hiddenStacks.first()
        if (settings.getBoolean(PrefKeys.CARD_DEAL_ANIMATION)) {
            hiddenStack.cards = playerCards
            playerHand.cards = emptyList()
            playerHand.shown = true

            doDelayed(moveDuration) {
                cardAnimationLayer.deal(hiddenStack, playerHand, playerCards.size, fromLast = false)
            }
            moveDuration += CardAnimationLayer.DEAL_DELAY * playerCards.size

        } else {
            hiddenStack.cards = emptyList()
            playerHand.cards = playerCards
            playerHand.slide(true, CardContainer.Direction.DOWN)
            moveDuration += CardContainer.TRANSITION_DURATION
        }

        // Set cards in containers
        extraHand.apply {
            cards = state.extraHand.cards
            fade(true)
        }
        for (i in 1..2) {
            hiddenStacks[i].cards = state.players[i].hand.cards
        }
        trick.cards = List(3) { null }

        // Start playing
        moveDuration += 0.5f
        doDelayed(moveDuration) {
            playNext()
        }
    }

    private fun endRound() {
        // TODO show scoreboard
    }

    private fun doMove(move: GameEvent.Move) {
        val game = game as Game
        val state = game.gameState as GameState

        val isSouth = move.playerPos == 0

        var moveDuration = 0f
        when (move) {
            is TradeHandMove -> {
                if (move.trade) {
                    // Do swap hand animation
                    val hiddenStack = hiddenStacks[move.playerPos]

                    if (isSouth) {
                        // Hide player hand
                        playerHand.slide(false, CardContainer.Direction.DOWN)
                        moveDuration += CardContainer.TRANSITION_DURATION

                        moveDuration += 0.1f
                        doDelayed(moveDuration) {
                            // Move player hand cards to hidden stack
                            hiddenStack.cards = playerHand.cards
                            playerHand.cards = emptyList()
                            playerHand.shown = true

                            // Move cards from extra hand to player hand
                            for (i in 0 until GameState.CARDS_COUNT) {
                                cardAnimationLayer.moveCard(extraHand, playerHand, 0, 0)
                            }
                            playerHand.sort()
                            cardAnimationLayer.update()
                        }
                        moveDuration += CardAnimationLayer.UPDATE_DURATION

                        // Move cards from hidden stack to extra hand
                        moveDuration += 0.1f
                        doDelayed(moveDuration) {
                            for (i in 0 until GameState.CARDS_COUNT) {
                                cardAnimationLayer.moveCard(hiddenStack, extraHand, 0, 0)
                            }
                            cardAnimationLayer.update()
                        }
                        moveDuration += CardAnimationLayer.UPDATE_DURATION

                    } else {
                        // Move cards from extra hand to hidden stack
                        for (i in 0 until GameState.CARDS_COUNT) {
                            cardAnimationLayer.moveCard(extraHand, hiddenStack, 0, hiddenStack.size)
                        }
                        cardAnimationLayer.update()
                        moveDuration += CardAnimationLayer.UPDATE_DURATION

                        // Move cards from hidden stack to extra hand
                        moveDuration += 0.1f
                        doDelayed(moveDuration) {
                            for (i in 0 until GameState.CARDS_COUNT) {
                                cardAnimationLayer.moveCard(hiddenStack, extraHand, 0, 0)
                            }
                            cardAnimationLayer.update()
                        }
                        moveDuration += CardAnimationLayer.UPDATE_DURATION
                    }
                } else {
                    moveDuration = 0f
                }

                // Hide extra hand if it's last player choice
                if ((move.playerPos + 1) % 3 == game.dealerPos) {
                    moveDuration += gameSpeedDelay + 0.5f
                    doDelayed(moveDuration) {
                        extraHand.fade(false)
                        trick.shown = true
                    }
                    moveDuration += CardContainer.TRANSITION_DURATION
                }
            }
            is PlayMove -> {
                // Move card from player hand to the trick
                val src = if (isSouth) playerHand else hiddenStacks[move.playerPos]
                cardAnimationLayer.moveCard(src, trick, src.cards.indexOf(move.card),
                        trick.actors.count { it != null }, replaceSrc = false, replaceDst = true)
                cardAnimationLayer.update()
                moveDuration = CardAnimationLayer.UPDATE_DURATION

                if (isSouth) {
                    // Unhighlight cards in case they were highlighted before move
                    moveDuration += 0.1f
                    doDelayed(moveDuration) {
                        playerHand.highlightAllCards(false)
                    }
                    moveDuration += CardHand.HIGHLIGHT_DURATION
                }

                when (state.currentTrick.cards.size) {
                    0 -> {
                        // Player plays last
                        moveDuration = if (settings.getBoolean(PrefKeys.AUTO_COLLECT)) {
                            // Collect the trick automatically.
                            collectTrick(hiddenStacks[state.posToMove], moveDuration + 1f)

                        } else {
                            doDelayed(moveDuration + 0.2f) {
                                collectPopup.show(playerHand, Popup.Side.ABOVE)
                            }
                            Float.POSITIVE_INFINITY
                        }
                    }
                    1 -> {
                        // Player plays first: adjust trick start angle
                        trick.startAngle = -((move.playerPos + 3.0 / 8) * PI * 2 / 3).toFloat()
                    }
                }
            }
        }

        if (moveDuration != Float.POSITIVE_INFINITY) {
            // After move animation is done, start next player turn.
            doDelayed(moveDuration) {
                playNext()
            }
        }
    }

    private fun collectTrick(dst: CardContainer, delay: Float): Float {
        // Collect the trick to the destination container.
        // Temporarily show the hidden stack so the trick is shown while collected.
        var moveDuration = delay
        dst.visibility = CardContainer.Visibility.ALL

        doDelayed(moveDuration) {
            for (i in 0 until trick.capacity) {
                cardAnimationLayer.moveCard(trick, dst, i, 0, replaceSrc = true)
            }
            cardAnimationLayer.update()
        }
        moveDuration += CardAnimationLayer.UPDATE_DURATION

        doDelayed(moveDuration) {
            dst.visibility = CardContainer.Visibility.NONE
        }

        return moveDuration
    }

    override fun onPreferenceValueChanged(pref: PrefEntry) {
        if (pref.key == PrefKeys.PLAYER_NAMES) {
            // TODO change player names in UI
        }
    }

    private fun playNext() {
        lastMoveTime = System.currentTimeMillis()

        val game = game as Game
        val state = game.gameState as GameState
        if (state.isGameDone) {
            // Round is done
            endRound()
        } else {
            val next = state.playerToMove
            if (next is MctsPlayer) {
                // Find next AI move asynchronously
                aiPlayerJob = KtxAsync.launch(dispatcher) {
                    // Wait between AI players moves to adjust game speed
                    delay((gameSpeedDelay * 1000).toLong() -
                            (System.currentTimeMillis() - lastMoveTime))

                    // Find move
                    yield()
                    val move = next.findMove(state)

                    // Do move
                    yield()
                    onRenderingThread {
                        aiPlayerJob = null
                        (game as Game?)?.doMove(move)
                    }
                }
            } else {
                // Human player turn
                val moves = state.getMoves()
                if (moves.size == 1 && settings.getBoolean(PrefKeys.AUTO_PLAY)) {
                    // Only one card is playable, auto-play it.
                    game.doMove(moves.first())

                } else {
                    if (state.phase == GameState.Phase.TRADE) {
                        tradePopup.show(playerHand, Popup.Side.ABOVE)

                    } else {
                        if (settings.getBoolean(PrefKeys.SELECT_PLAYABLE)) {
                            val playableCards = moves.map { (it as PlayMove).card }
                            if (playableCards.size < playerHand.size) {
                                // Highlight playable cards
                                playerHand.highlightCards(playableCards)
                            }
                        }

                        playerHand.clickListener = { actor, _ ->
                            val move = moves.find { it is PlayMove && it.card == actor.card }
                            if (move != null) {
                                // This card can be played, play it.
                                game.doMove(move)

                                // Remove click listener
                                playerHand.clickListener = null
                            }
                        }
                    }
                }
            }
        }
    }

    private inline fun doDelayed(delay: Float, crossinline action: () -> Unit) {
        addAction(object : TimeAction(delay / SPEED_MULTIPLIER) {
            override fun end() {
                action()
            }
        })
    }

}
