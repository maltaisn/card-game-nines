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

package io.github.maltaisn.cardgametest.core.core

import io.github.maltaisn.cardgame.core.PCard


class Game(player1: Player, player2: Player, player3: Player,
           private var dealer: Int, points: Int = 9,
           private val verbose: Boolean = true) {

    val players: List<Player> = listOf(player1, player2, player3)

    var showHands = false

    var scores = IntArray(3) { points }
        private set

    private var gameDone = false
    private var round: Int = 0
    private var trumpSuit = TRUMP_SUITS[round]
    private var state = GameState(players, dealer, trumpSuit)

    init {
        printRound()
    }

    /**
     * Play one player's turn in the game
     * Returns true if game is done, when a player has a score of 0 or less
     */
    fun play(): Boolean {
        if (gameDone) return true

        if (state.isGameDone()) {
            // Round is done
            // Update scores from result of round for each player
            val result = state.getResult() as Result
            for (i in 0 until 3) {
                scores[i] += 4 - result.playerResults[i].toInt()
                if (scores[i] <= 0) {
                    gameDone = true
                }
            }

            if (gameDone) return true

            if (verbose) {
                println("\nSCORES:")
                for (i in 0 until 3) {
                    println("[${players[i].name}] score: ${scores[i]} pts")
                }
            }

            // Start next round
            dealer = (dealer + 1) % players.size
            round++
            trumpSuit = TRUMP_SUITS[round % TRUMP_SUITS.size]
            state = GameState(players, dealer, trumpSuit)

            printRound()
        }

        // Do a player's move
        val player = players[state.playerToMove]
        val move = player.play(state)
        state.doMove(move)

        if (verbose) {
            if (move is PlayMove) {
                val playedLast = state.currentTrick.isEmpty()
                val playerToMove = state.playerToMove
                val trick = if (playedLast) players[playerToMove].tricksTaken.last() else state.currentTrick

                print("[${player.name}] $move, trick: $trick")
                println(if (showHands) player.hand.toSortedString(PCard.DEFAULT_SORTER) else "")

                if (playedLast) {
                    println("-> Trick #${state.tricksPlayed} taken by [${players[playerToMove].name}].")
                    printSeparator()
                }
            } else {
                println("[${player.name}] $move\n")
                if (player.position == dealer) {
                    printSeparator()
                }
            }
        }

        return false
    }

    private fun printRound() {
        if (verbose) {
            val suitStr = if (trumpSuit == GameState.NO_TRUMP) "none" else PCard.SUIT_STR[trumpSuit].toString()
            println("\nROUND ${round + 1}, trump: $suitStr")
            printSeparator()
        }
    }

    private fun printSeparator() {
        println("=".repeat(60))
    }

    companion object {

        val TRUMP_SUITS = intArrayOf(PCard.HEART, PCard.SPADE, PCard.DIAMOND, PCard.CLUB, GameState.NO_TRUMP)

    }

}