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
import com.maltaisn.cardgame.Resources
import com.maltaisn.cardgame.core.CardGame
import com.maltaisn.cardgame.core.CardGameEvent
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.prefs.PrefEntry
import com.maltaisn.cardgame.widget.CardGameLayout
import com.maltaisn.cardgame.widget.card.*
import com.maltaisn.nines.core.core.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ktx.async.AsyncExecutorDispatcher
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import ktx.async.onRenderingThread


class GameLayout(assetManager: AssetManager, settings: GamePrefs) :
        CardGameLayout(assetManager, settings) {

    private val hands: List<CardContainer>
    private val playerStack: CardStack
    private val extraHand: CardHand
    private val trick: CardTrick

    private val gameSpeedDelay: Long
        get() = when (settings.getChoice(PrefKeys.GAME_SPEED)) {
            "slow" -> 1500
            "normal" -> 1000
            "fast" -> 500
            "very_fast" -> 100
            else -> 0
        } + (CardAnimationLayer.UPDATE_DURATION * 1000).toLong()

    /** System time when the last move was made. */
    private var lastMoveTime = 0L

    private lateinit var dispatcher: AsyncExecutorDispatcher

    init {
        val cardSkin = assetManager.get<Skin>(Resources.PCARD_SKIN)

        val southHand = CardHand(coreSkin, cardSkin).apply {
            horizontal = true
            clipPercent = 0.3f
            align = Align.bottom
            cardSize = CardActor.SIZE_NORMAL
        }
        val eastHand = CardStack(coreSkin, cardSkin)
        val northHand = CardStack(coreSkin, cardSkin)
        hands = listOf(southHand, eastHand, northHand)

        playerStack = CardStack(coreSkin, cardSkin)

        trick = CardTrick(coreSkin, cardSkin, 3).apply {
            // TODO add custom angles to card trick
        }

        extraHand = CardHand(coreSkin, cardSkin).apply {
            visibility = CardContainer.Visibility.NONE
            horizontal = true
            cardSize = CardActor.SIZE_SMALL
        }

        gameLayer.apply {
            leftTable.add(eastHand).grow()
            rightTable.add(northHand).grow()
            bottomTable.add(playerStack).grow()

            centerTable.add(Stack(trick, extraHand)).grow().row()
            centerTable.add(southHand).growX()
        }
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)
        if (stage != null) {
            dispatcher = newSingleThreadAsyncContext()
        } else {
            dispatcher.dispose()
        }
    }

    override fun initGame(game: CardGame) {
        val state = game.gameState
        if (state != null) {
            game as Game
            // TODO
            //  - Do container transitions
            //  - Must be able to layout every saved state
        }
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
        (game as Game?)?.start()
    }

    private fun endGame() {
        // TODO Show game over dialog
    }

    private fun startRound() {
        // TODO deal cards, etc
        if (settings.getBoolean(PrefKeys.CARD_DEAL_ANIMATION)) {

        }

        // playNext should only be called after game was set up
        playNext()
    }

    private fun endRound() {
        // TODO show scoreboard
    }

    private fun doMove(move: GameEvent.Move) {
        when (move) {
            is TradeHandMove -> {
                // TODO swap hand animation
                //   if player: hide current hand, move trade hand to hidden stack, set hand cards, show hand
                //   if bot: move to bot stack
            }
            is PlayMove -> {
                // TODO move card from hand to trick
                //   if last player to play, collect trick
            }
        }

        playNext()
    }

    override fun onPreferenceValueChanged(pref: PrefEntry) {
        if (pref.key == PrefKeys.PLAYER_NAMES) {
            // TODO change player names in UI
        }
    }

    private fun playNext() {
        lastMoveTime = System.currentTimeMillis()

        val state = game?.gameState as GameState
        if (state.isGameDone) {
            // Round is done
            endRound()
        } else {
            val next = state.playerToMove
            if (next is MctsPlayer) {
                // Find next AI move asynchronously
                KtxAsync.launch(dispatcher) {
                    // Wait between AI players moves to adjust game speed
                    delay(gameSpeedDelay - (System.currentTimeMillis() - lastMoveTime))

                    // Find move
                    val move = next.findMove(state)

                    // Do move
                    onRenderingThread {
                        (game as Game?)?.doMove(move)
                    }
                }
            }
        }
    }

}
