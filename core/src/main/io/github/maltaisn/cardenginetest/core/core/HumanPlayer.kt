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

package io.github.maltaisn.cardenginetest.core.core

import io.github.maltaisn.cardengine.core.BaseGameState
import io.github.maltaisn.cardengine.core.BaseMove
import io.github.maltaisn.cardengine.core.BasePlayer
import io.github.maltaisn.cardengine.core.PCard


/**
 * Defines a human player
 */
class HumanPlayer : Player {

    constructor()

    private constructor(player: Player) : super(player)


    override fun play(state: BaseGameState<out BasePlayer>): BaseMove {
        state as GameState

        // Print info
        print("\nIt's your turn, ")
        if (state.phase == GameState.Phase.TRADE) {
            println("do you want to trade your hand?")
        } else {
            val trick = state.currentTrick
            if (trick.isEmpty()) {
                println("you lead.")
            } else {
                println("the trick is ${state.currentTrick}.")
            }
            print("Tricks taken by ")
            for (player in state.players) {
                print("[${player.name}]: ${player.tricksTaken.size}, ")
            }
            println()
        }
        println("Your hand: ${hand.toSortedString(PCard.DEFAULT_SORTER)}\n")

        // Print possible moves
        val moves = state.getMoves()
        for (i in 0 until moves.size) {
            println("${i + 1}. ${moves[i]}")
        }

        // Ask which move to do
        while (true) {
            println("Which move? ")
            val answer = readLine()
            try {
                val index = answer!!.toInt() - 1
                if (index !in 0 until moves.size) continue
                return moves[index]
            } catch (e: Exception) {
                continue
            }
        }
    }

    override fun clone() = HumanPlayer(this)

}