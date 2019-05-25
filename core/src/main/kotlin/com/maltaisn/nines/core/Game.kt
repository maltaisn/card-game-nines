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

import com.badlogic.gdx.files.FileHandle
import com.maltaisn.cardgame.core.CardGame
import com.maltaisn.cardgame.core.CardGameEvent
import com.maltaisn.cardgame.core.PCard
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.prefs.PlayerNamesPref
import com.maltaisn.cardgame.prefs.PrefEntry
import com.maltaisn.nines.core.core.GameEvent
import com.maltaisn.nines.core.core.GameState
import com.maltaisn.nines.core.core.Player
import kotlin.math.max
import kotlin.random.Random


class Game : CardGame {

    override val events: List<GameEvent>
        get() = _events

    private val _events = mutableListOf<GameEvent>()

    val players: List<Player>

    /** The current phase of the game. */
    var phase = Phase.ENDED
        private set

    /** The current round. */
    var round = 0
        private set

    /** The position of the current dealer. */
    var dealerPos = 0
        private set

    /** The trump suit for the current round. */
    val trumpSuit: Int
        get() = TRUMP_SUITS[max(0, round - 1) % TRUMP_SUITS.size]

    /** The player who won if the game has ended. */
    var winner: Player? = null

    /** Whether the game is done or not. */
    val isDone: Boolean
        get() = winner != null


    constructor(settings: GamePrefs, south: Player, east: Player, north: Player) : super(settings) {
        players = listOf(south, east, north)
    }

    constructor(settings: GamePrefs, file: FileHandle) : super(settings, file) {
        dealerPos = 0
        players = emptyList()
        // TODO
    }

    /** Start the game. */
    fun start() {
        check(phase == Phase.ENDED) { "Game has already started." }
        phase = Phase.GAME_STARTED

        round = 0
        gameState = null

        val startScore = settings.getInt(PrefKeys.START_SCORE)
        for (player in players) {
            player.score = startScore
        }
        updatePlayerNames()

        dealerPos = Random.nextInt(3)
        winner = null

        _events.clear()
        _events += GameEvent.Start
        eventListener?.invoke(GameEvent.Start)
    }

    /** End the game. */
    fun end() {
        if (phase == Phase.ROUND_STARTED) {
            // End round if necessary
            endRound()
        }
        check(phase == Phase.GAME_STARTED) { "Game has already ended." }
        phase = Phase.ENDED

        _events += GameEvent.End
        eventListener?.invoke(GameEvent.End)
    }

    /** Start a new round. */
    fun startRound() {
        check(phase == Phase.GAME_STARTED) { "Round has already started or game has not started." }
        phase = Phase.ROUND_STARTED

        round++

        dealerPos = (dealerPos + 1) % 3
        gameState = GameState(settings, players, Random.nextInt(3), trumpSuit)

        _events += GameEvent.RoundStart
        eventListener?.invoke(GameEvent.RoundStart)
    }

    /** End the current round. */
    fun endRound() {
        check(phase == Phase.ROUND_STARTED) { "Round has already ended or game has not started." }
        phase = Phase.GAME_STARTED

        // Update the scores
        val result = gameState?.result!!.playerResults
        for ((i, player) in players.withIndex()) {
            player.score += 4 - result[i].toInt()
        }

        _events += GameEvent.RoundEnd
        eventListener?.invoke(GameEvent.RoundEnd)

        // Check if any player has won
        // If there's any tie, continue playing
        var minIndex = 0
        var tie = false
        for (i in 1..2) {
            val score = players[i].score
            val min = players[minIndex].score
            if (score < min) {
                minIndex = i
                tie = false
            } else if (score == min) {
                tie = true
            }
        }

        val minPlayer = players[minIndex]
        if (minPlayer.score <= 0 && !tie) {
            winner = minPlayer
            end()
        }
    }

    /** Do a [move] on the game state. */
    fun doMove(move: CardGameEvent.Move) {
        move as GameEvent.Move
        check(phase != Phase.ENDED) { "Game has not started." }

        _events += move
        gameState?.doMove(move)
        eventListener?.invoke(move)
    }

    override fun onPreferenceValueChanged(pref: PrefEntry) {
        if (pref.key == PrefKeys.PLAYER_NAMES) {
            updatePlayerNames()
        }
    }

    /** Update [players] names from the preference values. */
    private fun updatePlayerNames() {
        val pref = settings[PrefKeys.PLAYER_NAMES] as PlayerNamesPref
        repeat(3) {
            players[it].name = pref.names[it]
        }
    }

    override fun save(file: FileHandle) {
        // TODO
    }


    override fun toString() = super.toString().dropLast(1) +
            ", phase: $phase, round: $round, dealer: ${players[dealerPos].name}, trumpSuit: " +
            (if (trumpSuit == GameState.NO_TRUMP) "none" else PCard.SUIT_STR[trumpSuit]) + "]"


    enum class Phase {
        /** Game has not started or is done. */
        ENDED,
        /** Game is started but round is done. */
        GAME_STARTED,
        /** Round has started and is being played. */
        ROUND_STARTED
    }

    companion object {
        private val TRUMP_SUITS = intArrayOf(PCard.HEART, PCard.SPADE,
                PCard.DIAMOND, PCard.SPADE, GameState.NO_TRUMP)
    }

}
