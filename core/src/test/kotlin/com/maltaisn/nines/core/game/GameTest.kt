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

package com.maltaisn.nines.core.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.maltaisn.cardgame.pcard.PCard
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.prefs.PlayerNamesPref
import com.maltaisn.nines.core.PrefKeys
import com.maltaisn.nines.core.game.event.*
import com.maltaisn.nines.core.game.player.AiPlayer
import com.maltaisn.nines.core.game.player.MctsPlayer
import com.maltaisn.nines.core.game.player.Player
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray


@RunWith(MockitoJUnitRunner::class)
internal class GameTest {

    @Test
    fun runGameTest() {
        // Mock settings
        Gdx.app = mock()
        val prefs: Preferences = mock()
        whenever(Gdx.app.getPreferences(any())).thenReturn(prefs)
        val settings = GamePrefs("com.maltaisn.nines.settings") {
            slider(PrefKeys.START_SCORE) {
                defaultValue = 9f
            }
            playerNames(PrefKeys.PLAYER_NAMES) {
                defaultValue = arrayOf("South", "East", "North")
            }
        }

        // Create players
        val south = MctsPlayer(MctsPlayer.Difficulty.EXPERT)
        val west = MctsPlayer(MctsPlayer.Difficulty.ADVANCED)
        val north = MctsPlayer(MctsPlayer.Difficulty.ADVANCED)

        //playGame(settings, south, west, north, VERBOSE_MOVES)
        playGames(settings, south, west, north, 1000)
    }

    /**
     * Play a single game.
     */
    private fun playGame(settings: GamePrefs,
                         south: Player, west: Player, north: Player,
                         verbosity: Int): Game {
        val game = Game(settings, south, west, north)
        val players = game.players
        val names = (settings[PrefKeys.PLAYER_NAMES] as PlayerNamesPref).value

        var lastScores = IntArray(3) { settings.getInt(PrefKeys.START_SCORE) }

        game.eventListeners += { event ->
            when (event) {
                is StartEvent -> {
                    if (verbosity >= VERBOSE_ROUNDS) {
                        println("=== GAME STARTED ===")
                    }
                }
                is RoundStartEvent -> {
                    // >>> Round 1 started, trump: ♥
                    if (verbosity >= VERBOSE_ROUNDS) {
                        val trumpStr = if (game.trumpSuit == GameState.NO_TRUMP) {
                            "none"
                        } else {
                            PCard.SUIT_STR[game.trumpSuit].toString()
                        }
                        println(">>> Round ${game.round} started, trump: $trumpStr")
                    }
                }
                is RoundEndEvent -> {
                    // >>> Round 1 ended, diff: [-2, 1, 0], scores: [7, 10, 9]
                    if (verbosity >= VERBOSE_ROUNDS) {
                        val scores = IntArray(3) { players[it].score }
                        val diff = IntArray(3) { scores[it] - lastScores[it] }
                        lastScores = scores
                        println(">>> Round ${game.round} ended, " +
                                "diff: ${diff.contentToString()}, " +
                                "scores: ${scores.contentToString()}\n")
                    }
                }
                is EndEvent -> {
                    // === GAME ENDED after 13 rounds, scores: [-1, 6, 9], winner: South ===
                    if (verbosity >= VERBOSE_ROUNDS) {
                        val scores = IntArray(3) { players[it].score }
                        println("=== GAME ENDED after ${game.round} rounds, " +
                                "scores: ${scores.contentToString()}, " +
                                "winner: ${names[game.winnerPos]} ===\n")
                    }
                }
                is MoveEvent -> {
                    if (verbosity >= VERBOSE_MOVES) {
                        val state = game.state!!
                        val player = players[event.playerPos]
                        // South did: Trade hand, trick: []
                        print("${names[event.playerPos]} did: $event, trick: ${state.currentTrick}")
                        if (verbosity == VERBOSE_ALL) {
                            // West did: Play 5♥, trick: [A♥, 5♥], hand: [...]
                            print(", hand: ${player.hand}")
                        }
                        println()
                        if (state.tricksPlayed.size > 0 && state.currentTrick.cards.isEmpty()) {
                            // > Trick #5 taken by North
                            println("> Trick #${state.tricksPlayed} taken by ${names[state.posToMove]}\n")
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
                if (next is AiPlayer) {
                    val move = next.findMove(state)
                    game.doMove(move)
                } else {
                    println("\n${names[next.position]}'s turn ${next.hand}, choose a move:")
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
     * Play a number of games in parallel.
     */
    private fun playGames(settings: GamePrefs,
                          south: Player, west: Player, north: Player,
                          count: Int) {
        val gamesPlayed = AtomicInteger()
        val gamesWon = AtomicIntegerArray(3)

        runBlocking {
            (0 until count).map {
                GlobalScope.async {
                    val game = playGame(settings, south.clone(), west.clone(), north.clone(), VERBOSE_NONE)
                    gamesWon.addAndGet(game.winnerPos, 1)
                    val played = gamesPlayed.addAndGet(1)
                    println("$played / $count, scores: $gamesWon")
                }
            }.awaitAll()
        }
    }

    companion object {
        const val VERBOSE_NONE = 0
        const val VERBOSE_ROUNDS = 1
        const val VERBOSE_MOVES = 2
        const val VERBOSE_ALL = 3
    }
}
