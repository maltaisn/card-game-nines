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

package com.maltaisn.cardgametest.core

class GameTest {
/*
    @Test
    fun play() {

        // Create players
        val player1 = MctsPlayer(MctsPlayer.Difficulty.ADVANCED)
        player1.name = "AI1"
        val player2 = MctsPlayer(MctsPlayer.Difficulty.EXPERT)
        player2.name = "AI2"
        val player3 = MctsPlayer(MctsPlayer.Difficulty.INTERMEDIATE)
        player3.name = "AI3"

        playGames(player1, player2, player3, 30)
    }

    private fun playGames(player1: Player, player2: Player, player3: Player, count: Int) {
        val gamesWon = intArrayOf(0, 0, 0)
        lateinit var game: Game
        repeat(count) {
            // Play game
            print("Game ${it + 1}: ")
            game = Game(player1, player2, player3,
                    dealer = Random.nextInt(3),
                    points = 9, verbose = false)

            // Play until game is done
            while (!game.play()) {
                // Do nothing...
            }

            // Print scores
            var winner = 0
            for (i in 0 until 3) {
                val score = game.players[i].score
                if (score < game.players[winner].score) {
                    winner = i
                }
                print("[${game.players[i].name}]: $score")
                if (i != 2) print(", ")
            }
            println()
            gamesWon[winner]++
        }

        print("Result: ")
        for (i in 0 until 3) {
            print("[${game.players[i].name}]: ${gamesWon[i]}")
            if (i != 2) print(", ")
        }
    }

    private fun playGame(player1: Player, player2: Player, player3: Player) {
        // Play game
        val game = OldGame(player1, player2, player3,
                dealer = Random.nextInt(3),
                points = 9, verbose = true)

        // Play until game is done
        while (!game.play()) {
            // Do nothing...
        }

        // Print scores
        println("GAME IS DONE")
        for (i in 0 until 3) {
            println("[P${i + 1}] Score: ${game.scores[i]} pts")
        }
    }
*/
}