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

import io.github.maltaisn.cardengine.core.*


/**
 * Defines a player played by the computer using MCTS algorithm.
 */
class MctsPlayer : Player {

    /** IDs of known hands. They won't be randomized by [GameState.randomizedClone]. */
    val knownHands = ArrayList<Int>()

    /**
     * Array indexed by hand ID. Each index is a list of
     * possible suits a hand can have. If the player with the hand cannot
     * follow the suit on a trick, a suit is removed from the list.
     */
    lateinit var handSuits: Array<ArrayList<Int>>
        private set


    private val mcts: Mcts
    private val mctsIter: Int


    constructor(mcts: Mcts, mctsIter: Int) : super() {
        this.mcts = mcts
        this.mctsIter = mctsIter
        mcts.explorationParam = 1.5
    }

    private constructor(player: MctsPlayer) : super(player) {
        knownHands += player.knownHands
        handSuits = Array(4) { ArrayList(player.handSuits[it]) }
        mcts = player.mcts
        mctsIter = player.mctsIter
    }


    override fun initialize(position: Int, hand: Hand) {
        super.initialize(position, hand)

        knownHands.clear()
        knownHands += hand.id
        handSuits = Array(4) {
            arrayListOf(PCard.HEART, PCard.SPADE, PCard.DIAMOND, PCard.CLUB)
        }
    }

    override fun play(state: BaseGameState<out BasePlayer>): BaseMove {
        state as GameState
        return if (state.phase == GameState.Phase.TRADE) {
            // Do random simulations of trading and not trading.
            // Choose the option that maximizes the average result.
            // This is better than MCTS itself because we don't want exploitation, both
            // options must be tested the same. With MCTS, an option can be only tested
            // once in 10000 simulations if the initial result is bad enough.
            state.getMoves().maxBy { simulateMove(state, it, mctsIter) }!!
        } else {
            mcts.run(state, mctsIter)
        }
    }

    override fun onMove(state: BaseGameState<out BasePlayer>, move: BaseMove) {
        state as GameState

        if (move is TradeHandMove) {
            if (move.player == position && move.trade) {
                // Now two hands are known.
                knownHands += hand.id
            }
        } else if (move is PlayMove) {
            val trick = if (state.currentTrick.isEmpty()) {
                state.players[state.playerToMove].tricksTaken.last()
            } else {
                state.currentTrick
            }
            val trickSuit = trick.getSuit()
            if (trick.last().suit != trickSuit) {
                // The player that moved couldn't follow suit, remember that.
                handSuits[state.players[move.player].hand.id].remove(trickSuit)
            }
        }
    }

    /**
     * Compute the average result of [iter] simulations of [rootState] doing a [move].
     * This is the same as the "Simulate" step of MCTS.
     */
    private fun simulateMove(rootState: GameState, move: BaseMove, iter: Int): Double {
        var score = 0.0
        val player = rootState.playerToMove
        repeat(iter) {
            val state = rootState.randomizedClone(player)
            state.doMove(move)

            // Simulate
            var randomMove = state.getRandomMove()
            while (randomMove != null) {
                state.doMove(randomMove)
                randomMove = state.getRandomMove()
            }

            score += state.getResult()!!.playerResults[player]
        }
        return score / iter
    }

    override fun clone() = MctsPlayer(this)

}