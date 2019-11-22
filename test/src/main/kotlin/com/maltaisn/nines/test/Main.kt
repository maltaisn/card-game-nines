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

package com.maltaisn.nines.test

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.utils.I18NBundle
import com.maltaisn.cardgame.pcard.PCard
import com.maltaisn.cardgame.pcard.toSortedString
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.prefs.PlayerNamesPref
import com.maltaisn.nines.core.PrefKeys
import com.maltaisn.nines.core.Res
import com.maltaisn.nines.core.builders.buildSettings
import com.maltaisn.nines.core.game.Game
import com.maltaisn.nines.core.game.GameState
import com.maltaisn.nines.core.game.event.*
import com.maltaisn.nines.core.game.player.AiPlayer
import com.maltaisn.nines.core.game.player.MctsPlayer
import com.maltaisn.nines.core.game.player.MctsPlayer.Difficulty
import com.maltaisn.nines.core.game.player.Player
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray


object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        Gdx.gl = mock()
        HeadlessApplication(object : ApplicationAdapter() {})
        val settings = buildSettings(I18NBundle.createBundle(Gdx.files.internal(Res.STRINGS)))

        // Create players
        val players = listOf<Player>(
                MctsPlayer(Difficulty.EXPERT),
                MctsPlayer(Difficulty.EXPERT),
                MctsPlayer(Difficulty.EXPERT))

        //playGame(settings, players, VERBOSE_ALL)
        playGames(settings, players, VERBOSE_NONE, 1000)
        //playGamesParallel(settings, players, 1000)
    }

    /**
     * Play a single game.
     */
    private fun playGame(settings: GamePrefs,
                         players: List<Player>,
                         verbosity: Int): Game {
        require(players.size == 3) { "There must be exactly 3 players." }

        val game = Game(settings, players[0], players[1], players[2])
        val names = (settings[PrefKeys.PLAYER_NAMES] as PlayerNamesPref).defaultValue

        game.eventListeners += { event ->
            when (event) {
                is StartEvent -> {
                    if (verbosity >= VERBOSE_ROUNDS) {
                        println("=== GAME STARTED ===")
                    }
                }
                is RoundStartEvent -> {
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
                    if (verbosity >= VERBOSE_ROUNDS) {
                        println(">>> Round ${game.round} ended, " +
                                "diff: ${event.tricksTaken.map { 4 - it }}, " +
                                "scores: ${players.map { it.score }}\n")
                    }
                }
                is EndEvent -> {
                    if (verbosity >= VERBOSE_ROUNDS) {
                        println("=== GAME ENDED after ${game.round} rounds, " +
                                "scores: ${players.map { it.score }}, " +
                                "winner: ${names[game.winnerPos]} ===\n")
                    }
                }
                is MoveEvent -> {
                    if (verbosity >= VERBOSE_MOVES) {
                        val state = game.state!!
                        val player = players[event.playerPos]
                        print("${names[event.playerPos]} did: $event")
                        if (event is PlayMove) {
                            val trickCards = if (state.currentTrick.cards.isEmpty()) {
                                state.tricksPlayed.last()
                            } else {
                                state.currentTrick
                            }.cards
                            print(", trick: $trickCards")
                        }
                        if (verbosity == VERBOSE_ALL) {
                            print(", hand: ${player.hand.cards.toSortedString()}")
                        }
                        println()
                        if (state.tricksPlayed.size > 0 && state.currentTrick.cards.isEmpty()) {
                            println("> Trick #${state.tricksPlayed.size} taken by ${names[state.posToMove]} " +
                                    "(${player.tricksTaken})\n")
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
     * Play a number of games.
     */
    private fun playGames(settings: GamePrefs,
                          players: List<Player>,
                          verbosity: Int,
                          count: Int) {
        var gamesPlayed = 0
        val gamesWon = IntArray(players.size)

        repeat(count) {
            // Shuffle players to avoid bias. (eg: playing after a beginner is clearly an advantage)
            val shuffledPlayers = players.shuffled()
            val game = playGame(settings, shuffledPlayers.map { it.clone() }, verbosity)

            val winner = shuffledPlayers[game.winnerPos]
            gamesWon[players.indexOfFirst { it === winner }]++
            gamesPlayed++

            println("$gamesPlayed / $count, scores: ${gamesWon.contentToString()}")
        }
    }

    /**
     * Play a number of games in parallel.
     */
    private fun playGamesParallel(settings: GamePrefs,
                                  players: List<Player>,
                                  count: Int) {
        val gamesPlayed = AtomicInteger()
        val gamesWon = AtomicIntegerArray(players.size)
        val scoreDistribution = AtomicIntegerArray(14)

        runBlocking {
            List(count) {
                GlobalScope.async {
                    val shuffledPlayers = players.shuffled()
                    val game = playGame(settings, shuffledPlayers.map { it.clone() }, VERBOSE_NONE)

                    val winner = shuffledPlayers[game.winnerPos]
                    gamesWon.incrementAndGet(players.indexOfFirst { it === winner })
                    val played = gamesPlayed.incrementAndGet()
                    println("$played / $count, scores: $gamesWon")

                    for (event in game.events) {
                        if (event is RoundEndEvent) {
                            for (score in event.tricksTaken) {
                                scoreDistribution.incrementAndGet(score)
                            }
                        }
                    }
                }
            }.awaitAll()
        }

        println("\nROUND SCORE DISTRIBUTION")
        var total = 0
        for (i in 0..13) {
            total += scoreDistribution[i]
        }
        val numberFmt = DecimalFormat.getPercentInstance().apply {
            maximumFractionDigits = 3
            minimumFractionDigits = 3
        }
        for (i in 0..13) {
            val amount = scoreDistribution[i]
            println("${4 - i} pts; $amount; ${numberFmt.format(amount.toFloat() / total)}")
        }
    }

    private const val VERBOSE_NONE = 0
    private const val VERBOSE_ROUNDS = 1
    private const val VERBOSE_MOVES = 2
    private const val VERBOSE_ALL = 3

}

