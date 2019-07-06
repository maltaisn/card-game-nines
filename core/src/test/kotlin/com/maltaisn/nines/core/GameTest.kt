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

import com.maltaisn.cardgame.game.PCard
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.prefs.buildGamePrefsFromMap
import com.maltaisn.nines.core.game.*
import com.maltaisn.nines.core.game.MctsPlayer.Difficulty
import com.maltaisn.nines.core.game.event.*


fun main() {
    // Create players
    val south = HumanPlayer()
    val east = MctsPlayer(Difficulty.INTERMEDIATE)
    val north = MctsPlayer(Difficulty.ADVANCED)

    playGame(settings, south, east, north, VERBOSE_MOVES)
    //playGames(settings, south, east, north, 30, VERBOSE_ROUNDS)
}

/**
 * Play a single game.
 */
private fun playGame(settings: GamePrefs,
                     south: Player, east: Player, north: Player,
                     verbosity: Int): Game {
    val game = Game(settings, south, east, north)
    val players = game.players

    var lastScores = IntArray(3) { settings.getInt(PrefKeys.START_SCORE) }

    game.eventListener = { event ->
        when (event) {
            is StartEvent -> {
                println("=== GAME STARTED ===")
            }
            is RoundStartEvent -> {
                // >>> Round 1 started, trump: ♥
                val trumpStr = if (game.trumpSuit == GameState.NO_TRUMP) {
                    "none"
                } else {
                    PCard.SUIT_STR[game.trumpSuit].toString()
                }
                println(">>> Round ${game.round} started, trump: $trumpStr")
            }
            is RoundEndEvent -> {
                // >>> Round 1 ended, diff: [-2, 1, 0], scores: [7, 10, 9]
                val scores = IntArray(3) { players[it].score }
                val diff = IntArray(3) { scores[it] - lastScores[it] }
                lastScores = scores
                println(">>> Round ${game.round} ended, " +
                        "diff: ${diff.contentToString()}, " +
                        "scores: ${scores.contentToString()}\n")
            }
            is EndEvent -> {
                // === GAME ENDED after 13 rounds, scores: [-1, 6, 9], winner: South ===
                val scores = IntArray(3) { players[it].score }
                println("=== GAME ENDED after ${game.round} rounds, " +
                        "scores: ${scores.contentToString()}, " +
                        "winner: ${NAMES[game.winnerPos]} ===\n")
            }
            is MoveEvent -> {
                if (verbosity > VERBOSE_ROUNDS) {
                    val state = game.state!!
                    val player = players[event.playerPos]
                    // South did: Trade hand, trick: []
                    print("${NAMES[event.playerPos]} did: $event, trick: ${state.currentTrick}")
                    if (verbosity == VERBOSE_ALL) {
                        // East did: Play 5♥, trick: [A♥, 5♥], hand: [...]
                        print(", hand: ${player.hand}")
                    }
                    println()
                    if (state.tricksPlayed.size > 0 && state.currentTrick.cards.isEmpty()) {
                        // > Trick #5 taken by North
                        println("> Trick #${state.tricksPlayed} taken by ${NAMES[state.posToMove]}\n")
                    }
                }
            }
        }
    }

    // Play the game
    game.start()
    while (!game.isDone) {
        game.startRound()
        val state = game.state!!
        var moves = state.getMoves()
        while (moves.isNotEmpty()) {
            val next = state.playerToMove
            if (next is MctsPlayer) {
                val move = next.findMove(state)
                game.doMove(move)
            } else {
                println("\n${NAMES[next.position]}'s turn ${next.hand}, choose a move:")
                for ((i, move) in moves.withIndex()) {
                    println("${i + 1}. $move")
                }
                var answer = 0
                while (answer <= 0 || answer > moves.size) {
                    print(">> ")
                    answer = readLine()?.toIntOrNull() ?: 0
                }
                println()
                game.doMove(moves[answer - 1])
            }

            moves = state.getMoves()
        }
        game.endRound()
    }

    return game
}

/**
 * Play a number of games.
 */
private fun playGames(settings: GamePrefs,
                      south: Player, east: Player, north: Player,
                      count: Int, verbosity: Int) {
    val gamesWon = intArrayOf(0, 0, 0)
    repeat(count) {
        println("=== GAME ${it + 1} / $count ===")
        val game = playGame(settings, south, east, north, verbosity)
        gamesWon[game.winnerPos]++
    }
    println("Games won: ${gamesWon.contentToString()}")
}

private val settings = buildGamePrefsFromMap(mapOf(
        PrefKeys.START_SCORE to 9,
        PrefKeys.GAME_SPEED to "none",
        PrefKeys.PLAYER_NAMES to arrayOf("South", "East", "North")
))

private const val VERBOSE_ROUNDS = 0
private const val VERBOSE_MOVES = 1
private const val VERBOSE_ALL = 2

private val NAMES = listOf("South", "East", "North")
