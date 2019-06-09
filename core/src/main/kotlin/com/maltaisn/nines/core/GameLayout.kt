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
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
import com.maltaisn.cardgame.CoreRes
import com.maltaisn.cardgame.game.CardGame
import com.maltaisn.cardgame.game.CardGameEvent
import com.maltaisn.cardgame.game.PCard
import com.maltaisn.cardgame.game.sortWith
import com.maltaisn.cardgame.postDelayed
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.prefs.PlayerNamesPref
import com.maltaisn.cardgame.prefs.PrefEntry
import com.maltaisn.cardgame.widget.*
import com.maltaisn.cardgame.widget.card.*
import com.maltaisn.nines.core.game.*
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

    //@GDXAssets(propertiesFiles = ["assets/strings.properties"])
    private val bundle = assetManager.get<I18NBundle>(Res.STRINGS_BUNDLE)

    private val hiddenStacks: List<CardStack>
    private val playerHand: CardHand
    private val extraHand: CardHand
    private val trick: CardTrick

    private val playerLabelTable: FadeTable
    private val playerLabels: List<PlayerLabel>

    private val tradePopup: Popup
    private val collectPopup: Popup
    private val idlePopup: Popup

    private val gameSpeedDelay: Float
        get() = when (settings.getChoice(PrefKeys.GAME_SPEED)) {
            "slow" -> 1.5f
            "normal" -> 1f
            "fast" -> 0.5f
            else -> 0f
        }

    /** System time when the last move was made. */
    private var lastMoveTime = 0L

    private var tradePhaseEnded = false

    private var idleAction: Action? = null
        set(value) {
            if (value == null) removeAction(field)
            field = value
        }

    private lateinit var dispatcher: AsyncExecutorDispatcher
    private var aiPlayerJob: Job? = null

    init {
        val cardSkin = assetManager.get<Skin>(CoreRes.PCARD_SKIN)

        // Card containers
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

        extraHand = CardHand(coreSkin, cardSkin).apply {
            sorter = PCard.DEFAULT_SORTER
            visibility = CardContainer.Visibility.NONE
            cardSize = CardActor.SIZE_SMALL
            maxCardSpacing = 30f
            enabled = false
            shown = false
        }

        trick = CardTrick(coreSkin, cardSkin, 3).apply {
            shown = false
        }

        cardAnimationLayer.register(trick, extraHand, playerHand, *hiddenStacks.toTypedArray())

        // Player labels
        playerLabels = List(3) { PlayerLabel(coreSkin) }
        updatePlayerNames()

        // Do the layout
        gameLayer.apply {
            bottomTable.add(hiddenStacks[0]).grow()
            leftTable.add(hiddenStacks[1]).grow()
            topTable.add(hiddenStacks[2]).grow()

            playerLabelTable = FadeTable().apply {
                pad(40f, 30f, 140f, 30f)
                add(playerLabels[2]).align(Align.topRight).width(120f).expand().padRight(150f).row()
                add(playerLabels[1]).align(Align.topLeft).width(120f).expand().row()
                add(playerLabels[0]).align(Align.bottomLeft).width(120f).expand().padLeft(100f)
            }
            val containerTable = Table().apply {
                add(Stack(trick, extraHand)).grow().row()
                add(playerHand).growX()
            }
            centerTable.add(Stack(playerLabelTable, containerTable)).grow()
        }

        // Trade hand popup
        tradePopup = Popup(coreSkin)
        popupGroup.addActor(tradePopup)

        val tradeBtn = PopupButton(coreSkin, bundle["popup_trade"])
        tradeBtn.onClick {
            val game = game as Game
            val state = game.gameState!!
            game.doMove(state.getMoves().find { it is TradeHandMove && it.trade }!!)
            tradePopup.hide()
        }

        val noTradeBtn = PopupButton(coreSkin, bundle["popup_no_trade"])
        noTradeBtn.onClick {
            val game = game as Game
            val state = game.gameState!!
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

        val collectBtn = PopupButton(coreSkin, bundle["popup_ok"])
        collectBtn.onClick {
            val moveDuration = collectTrick(hiddenStacks[0], 0f)
            collectPopup.hide()

            postDelayed(moveDuration) {
                playNext()
            }
        }
        collectPopup.add(collectBtn).minWidth(150f)

        // Idle popup
        idlePopup = Popup(coreSkin)
        popupGroup.addActor(idlePopup)

        val idleBtn = PopupButton(coreSkin, bundle["popup_your_turn"])
        idlePopup.add(idleBtn).minWidth(150f)
        idlePopup.touchable = Touchable.disabled
    }


    override fun setStage(stage: Stage?) {
        super.setStage(stage)
        if (stage != null) {
            dispatcher = newSingleThreadAsyncContext()
        } else {
            aiPlayerJob?.cancel()
            aiPlayerJob = null
            dispatcher.dispose()
        }
    }

    override fun initGame(game: CardGame<*>) {
        game as Game
        super.initGame(game)

        cardAnimationLayer.clearDelayedMoves()
        cardAnimationLayer.completeAnimation()

        if (game.players[0] !is HumanPlayer) {
            playerHand.enabled = false
        }

        when (game.phase) {
            Game.Phase.ENDED, Game.Phase.GAME_STARTED -> {
                // Round wasn't started yet or has just ended. Hide all containers.
                hide()
            }
            Game.Phase.ROUND_STARTED -> {
                // Round has started
                val state = game.gameState!!
                playerLabelTable.fade(true)

                if (state.phase == GameState.Phase.TRADE) {
                    // Trade phase: show extra hand
                    extraHand.apply {
                        cards = state.extraHand.cards
                        fade(true)
                    }
                    tradePhaseEnded = false

                } else {
                    // Play phase: show and adjust trick
                    trick.apply {
                        cards = List(3) { state.currentTrick.cards.getOrNull(it) }
                        fade(true)
                    }
                    setTrickStartAngle((state.posToMove - state.currentTrick.cards.size + 3) % 3)

                    // Immediately show idle popup if it's a human player's turn
                    if (state.posToMove == 0 && game.players[0] is HumanPlayer) {
                        idlePopup.show(playerHand, Popup.Side.ABOVE)
                    }

                    tradePhaseEnded = true
                }

                playerHand.apply {
                    cards = state.players[0].hand.cards
                    slide(true, CardContainer.Direction.DOWN)
                }
                for (i in 1..2) {
                    hiddenStacks[i].cards = state.players[i].hand.cards
                }

                // NOTE: taken tricks cards are not put back in the hidden stack since they're not used

                updatePlayerScores()

                playNext()
            }
        }
    }

    fun hide() {
        // Clear all animations
        cardAnimationLayer.clearDelayedMoves()
        cardAnimationLayer.completeAnimation()
        clearActions()

        // Hide all containers and all popups
        playerHand.highlightAllCards(false)
        playerHand.slide(false, CardContainer.Direction.DOWN)
        playerHand.clickListener = null

        extraHand.fade(false)
        trick.fade(false)

        playerLabelTable.fade(false)

        tradePopup.hide()
        collectPopup.hide()

        idlePopup.hide()
        idleAction = null

        // Cancel AI job
        aiPlayerJob?.cancel()
        aiPlayerJob = null
    }

    override fun doEvent(event: CardGameEvent) {
        event as GameEvent
        when (event) {
            is GameEvent.Start -> startGame()
            is GameEvent.End -> endGame()
            is GameEvent.RoundStart -> startRound()
            is GameEvent.RoundEnd -> endRound()
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
        val state = game.gameState!!

        var moveDuration = 0.5f

        tradePhaseEnded = false

        // Set player hand
        val playerCards = game.players[0].hand.cards.toMutableList()
        playerCards.sortWith(PCard.DEFAULT_SORTER)

        val hiddenStack = hiddenStacks[0]
        if (settings.getBoolean(PrefKeys.CARD_DEAL_ANIMATION)) {
            hiddenStack.cards = playerCards
            playerHand.cards = emptyList()
            playerHand.shown = true

            postDelayed(moveDuration) {
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

        playerLabelTable.fade(true)
        updatePlayerScores()

        // Start playing
        moveDuration += 0.5f
        postDelayed(moveDuration) {
            playNext()
        }
    }

    private fun endRound() {
        hide()

        // TODO show scoreboard
    }

    private fun doMove(move: GameEvent.Move) {
        val game = game as Game
        val state = game.gameState!!

        val isSouth = move.playerPos == 0

        var moveDuration = 0f
        when (move) {
            is TradeHandMove -> {
                updatePlayerScores()

                if (move.trade) {
                    // Do swap hand animation
                    val hiddenStack = hiddenStacks[move.playerPos]

                    if (isSouth) {
                        // Hide player hand
                        playerHand.slide(false, CardContainer.Direction.DOWN)
                        moveDuration += CardContainer.TRANSITION_DURATION

                        moveDuration += 0.1f
                        postDelayed(moveDuration) {
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
                        postDelayed(moveDuration) {
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
                        postDelayed(moveDuration) {
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

                // If this player had the last choice
                if ((move.playerPos + 1) % 3 == game.dealerPos) {
                    tradePhaseEnded = true

                    // Hide extra hand
                    moveDuration += gameSpeedDelay + 0.5f
                    postDelayed(moveDuration) {
                        extraHand.fade(false)
                        trick.shown = true
                    }
                    moveDuration += CardContainer.TRANSITION_DURATION

                    moveDuration += 0.1f
                    postDelayed(moveDuration) {
                        updatePlayerScores()
                    }
                }
            }
            is PlayMove -> {
                // Move card from player hand to the trick
                val src = if (isSouth) playerHand else hiddenStacks[move.playerPos]
                cardAnimationLayer.moveCard(src, trick, src.cards.indexOf(move.card),
                        trick.actors.count { it != null }, replaceSrc = false, replaceDst = true)
                cardAnimationLayer.update()
                moveDuration = CardAnimationLayer.UPDATE_DURATION

                if (isSouth && settings.getBoolean(PrefKeys.SELECT_PLAYABLE)) {
                    // Unhighlight cards in case they were highlighted before move
                    moveDuration += 0.1f
                    postDelayed(moveDuration) {
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
                            postDelayed(moveDuration + 0.2f) {
                                collectPopup.show(playerHand, Popup.Side.ABOVE)
                            }
                            Float.POSITIVE_INFINITY
                        }
                    }
                    1 -> {
                        // Player plays first: adjust trick start angle
                        setTrickStartAngle(move.playerPos)
                    }
                }
            }
        }

        if (moveDuration.isFinite()) {
            // After move animation is done, start next player turn.
            postDelayed(moveDuration) {
                playNext()
            }
        }
    }

    private fun collectTrick(dst: CardContainer, delay: Float): Float {
        // Collect the trick to the destination container.
        // Temporarily show the hidden stack so the trick is shown while collected.
        var moveDuration = delay
        dst.visibility = CardContainer.Visibility.ALL

        postDelayed(moveDuration) {
            for (i in 0 until trick.capacity) {
                cardAnimationLayer.moveCard(trick, dst, i, 0, replaceSrc = true)
            }
            cardAnimationLayer.update()

            updatePlayerScores()
        }
        moveDuration += CardAnimationLayer.UPDATE_DURATION

        postDelayed(moveDuration) {
            dst.visibility = CardContainer.Visibility.NONE
        }

        return moveDuration
    }

    /** Adjust the trick start angle so that the card of the player who led the trick (at [startPos]) is below. */
    private fun setTrickStartAngle(startPos: Int) {
        trick.startAngle = -((startPos + 3.0 / 8) * PI * 2 / 3).toFloat()
    }

    override fun onPreferenceValueChanged(pref: PrefEntry) {
        if (pref.key == PrefKeys.PLAYER_NAMES) {
            updatePlayerNames()
        }
    }

    private fun updatePlayerNames() {
        val pref = settings[PrefKeys.PLAYER_NAMES] as PlayerNamesPref
        for (i in 0..2) {
            playerLabels[i].name = pref.names[i]
        }
    }

    private fun updatePlayerScores() {
        for (i in 0..2) {
            val player = (game as Game).players[i]
            playerLabels[i].score = if (tradePhaseEnded) {
                bundle.format("player_score", player.score, player.tricksTaken.size)
            } else {
                when (player.trade) {
                    Player.Trade.TRADE -> bundle["player_trade"]
                    Player.Trade.NO_TRADE -> bundle["player_no_trade"]
                    Player.Trade.UNKNOWN -> null
                }
            }
        }
    }

    /**
     * Start the next player's turn, with a delay to follow game speed.
     */
    private fun playNext() {
        lastMoveTime = System.currentTimeMillis()

        val game = game as Game
        val state = game.gameState!!

        if (state.isGameDone) {
            // Round is done
            game.endRound()
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
                        // Show idle popup after some time if not already shown (by initGame)
                        if (!idlePopup.shown) {
                            val delay = if (state.currentTrick.cards.size == 0 && state.tricksPlayed == 0) 0f else 3f
                            idleAction = postDelayed(delay) {
                                idlePopup.show(playerHand, Popup.Side.ABOVE)
                                idleAction = null
                            }
                        }

                        // Highlight playable cards if necessary
                        if (settings.getBoolean(PrefKeys.SELECT_PLAYABLE)) {
                            val playableCards = moves.map { (it as PlayMove).card }
                            if (playableCards.size < playerHand.size) {
                                // Highlight playable cards. This is delayed so that the player hand
                                // has time to layout if cards were set on same frame
                                postDelayed(0.1f) {
                                    playerHand.highlightCards(playableCards)
                                }
                            }
                        }

                        playerHand.clickListener = { actor, _ ->
                            val move = moves.find { it is PlayMove && it.card == actor.card }
                            if (move != null) {
                                // This card can be played, play it.
                                game.doMove(move)

                                // Remove click listener
                                playerHand.clickListener = null

                                // Hide and cancel idle popup
                                idlePopup.hide()
                                idleAction = null
                            }
                        }
                    }
                }
            }
        }
    }

}
