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

import com.maltaisn.cardgame.stats.Statistics
import com.maltaisn.nines.core.game.Game
import com.maltaisn.nines.core.game.event.EndEvent
import com.maltaisn.nines.core.game.event.MoveEvent
import com.maltaisn.nines.core.game.event.PlayMove
import com.maltaisn.nines.core.game.event.RoundEndEvent
import com.maltaisn.nines.core.game.player.HumanPlayer
import com.maltaisn.nines.core.game.player.MctsPlayer
import com.maltaisn.nines.core.game.player.Player


/**
 * Class for updating the game [stats] when an event happens in a [game].
 */
class StatsHandler(private val game: Game,
                   private val stats: Statistics) {

    /**
     * The active statistics variant. This correspond to the difficulty of the MCTS player
     * if they all have the same difficulty and if there's a human player in the game.
     * Otherwise, stats are not recorded.
     */
    private val variant: Int


    init {
        // Find the stats variant.
        var hasHuman = false
        var lastDiff: MctsPlayer.Difficulty? = null
        for (player in game.players) {
            if (player is MctsPlayer) {
                if (lastDiff == null) {
                    lastDiff = player.difficulty
                } else if (lastDiff != player.difficulty) {
                    lastDiff = null
                    break
                }
            } else if (player is HumanPlayer) {
                hasHuman = true
            }
        }
        variant = if (hasHuman && lastDiff != null) {
            lastDiff.ordinal
        } else {
            VARIANT_NONE
        }

        // Attach a listener to the game to receive events.
        // The listener will be detached automatically when the game is disposed.
        game.eventListeners += { event ->
            if (variant != VARIANT_NONE) {
                when (event) {
                    is EndEvent -> onGameEnded()
                    is RoundEndEvent -> onRoundEnded(event)
                    is MoveEvent -> onMove(event)
                }
            }
        }
    }

    private fun onGameEnded() {
        addToStat(GAMES_PLAYED)
        addToStat(ROUNDS_PLAYED_COMPLETE, game.round)

        if (game.winnerPos == 0) {
            addToStat(GAMES_WON)
        }

        val minRoundsStat = stats.getNumber(MIN_ROUNDS_IN_GAME)
        val minRounds = minRoundsStat[variant]
        if (minRounds.isNaN() || minRounds > game.round) {
            minRoundsStat[variant] = game.round
        }

        val maxRoundsStat = stats.getNumber(MAX_ROUNDS_IN_GAME)
        val maxRounds = maxRoundsStat[variant]
        if (maxRounds.isNaN() || maxRounds < game.round) {
            maxRoundsStat[variant] = game.round
        }

        stats.save()
    }

    private fun onRoundEnded(event: RoundEndEvent) {
        addToStat(ROUNDS_PLAYED)

        val diff = event.result.map { 4 - it.toInt() }

        if (Game.findLowestScorePosition(diff) == 0) {
            addToStat(ROUNDS_WON)
        }

        addToStat(ROUND_SCORE_TOTAL, diff[0])

        if (game.players[0].trade == Player.Trade.TRADE) {
            addToStat(TRADES_DONE)
        }

        stats.save()
    }

    private fun onMove(move: MoveEvent) {
        if (move is PlayMove) {
            val state = game.state ?: return
            if (state.currentTrick.cards.size == 0) {
                // Trick was just collected.
                addToStat(TRICKS_PLAYED)

                if (state.posToMove == 0) {
                    // Human player collected the trick.
                    addToStat(TRICKS_WON)
                }
            }
        }

        // Don't save stats here, it would be a bit excessive...
    }

    private fun addToStat(key: String, value: Float = 1f) {
        stats.getNumber(key)[variant] += value
    }

    private fun addToStat(key: String, value: Int) = addToStat(key, value.toFloat())


    companion object {
        private const val VARIANT_NONE = -1

        const val GAMES_PLAYED = "gamesPlayed"
        const val GAMES_WON = "gamesWon"
        const val ROUNDS_PLAYED = "roundsPlayed"
        const val ROUNDS_PLAYED_COMPLETE = "roundsPlayedComplete"
        const val ROUNDS_WON = "roundsWon"
        const val ROUND_SCORE_TOTAL = "roundScoreTotal"
        const val TRICKS_PLAYED = "tricksPlayed"
        const val TRICKS_WON = "tricksWon"
        const val TRADES_DONE = "tradesDone"
        const val MIN_ROUNDS_IN_GAME = "minRoundsInGame"
        const val MAX_ROUNDS_IN_GAME = "maxRoundsInGame"
    }

}
