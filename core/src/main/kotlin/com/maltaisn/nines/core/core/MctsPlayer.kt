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

package com.maltaisn.nines.core.core

import com.maltaisn.cardgame.core.*


/**
 * Defines a player played by the computer using MCTS algorithm.
 */
class MctsPlayer : Player {

    /**
     * The playing difficulty for this player.
     */
    val difficulty: Difficulty

    /** IDs of known hands. They won't be randomized by [GameState.randomizedClone]. */
    val knownHands = mutableListOf<Int>()

    /**
     * Array indexed by hand ID. Each index is a list of
     * possible suits a hand can have. If the player with the hand cannot
     * follow the suit on a trick, a suit is removed from the list.
     * TODO ArrayList<Int> can be replaced with bit field
     */
    lateinit var handSuits: Array<ArrayList<Int>>
        private set


    constructor(difficulty: Difficulty) : super() {
        this.difficulty = difficulty
    }

    private constructor(player: MctsPlayer) : super(player) {
        difficulty = player.difficulty
        knownHands += player.knownHands
        handSuits = Array(4) { ArrayList(player.handSuits[it]) }
    }


    override fun initialize(position: Int, hand: Hand) {
        super.initialize(position, hand)

        knownHands.clear()
        knownHands += hand.id
        handSuits = Array(4) {
            arrayListOf(PCard.HEART, PCard.SPADE, PCard.DIAMOND, PCard.CLUB)
        }
    }

    /**
     * Find the best move to play given the current game state.
     * This always gets moves from [CardGameState.getMoves], never creates them.
     */
    fun findMove(state: CardGameState): GameEvent.Move {
        state as GameState
        return if (state.phase == GameState.Phase.TRADE) {
            // Do random simulations of trading and not trading.
            // Choose the option that maximizes the average result.
            // This is better than MCTS itself because we don't want exploitation, both
            // options must be tested the same. With MCTS, an option can be only tested
            // once in 10000 simulations if the initial result is bad enough.
            state.getMoves().maxBy { Mcts.simulate(state, it, 1000) }!!
        } else {
            Mcts.run(state, 1000)
        }
    }

    override fun onMove(state: CardGameState, move: GameEvent.Move) {
        state as GameState

        if (move is TradeHandMove) {
            if (move.playerPos == position && move.trade) {
                // Now two hands are known.
                knownHands += hand.id
            }
        } else if (move is PlayMove) {
            val trick = if (state.currentTrick.isEmpty()) {
                state.players[state.posToMove].tricksTaken.last()
            } else {
                state.currentTrick
            }
            val trickSuit = trick.getSuit()
            if (trick.last().suit != trickSuit) {
                // The player that moved couldn't follow suit, remember that.
                handSuits[state.players[move.playerPos].hand.id].remove(trickSuit)
            }
        }
    }


    /**
     * Randomizes a game [state] from the point of view of this player.
     */
    fun randomizeState(state: GameState) {
        // TODO adjust for difficulty

        // Create a list of all hands unknown to the observer.
        val hands = mutableListOf<Hand>()
        for (p in state.players) {
            if (p.hand.id !in knownHands) {
                hands += p.hand
            }
        }
        if (state.extraHand.id !in knownHands) {
            hands += state.extraHand
        }

        // Take all cards from these hands and shuffle them.
        val unseen = Deck<PCard>()
        for (hand in hands) {
            unseen += hand
        }
        unseen.shuffle()

        // Sort the unseen cards by suit
        val suitCards = List(4) { Deck<PCard>() }
        for (card in unseen) {
            suitCards[card.suit] += card
        }

        // Redistribute the cards to the unknown hands.
        // If the observer knows the hand doesn't have cards of a suit, don't give any.
        // FIXME hungarian algorithm
        //   https://cs.stackexchange.com/questions/102999/split-a-list-of-elements-into-sub-lists-each-with-different-criteria
        for (hand in hands) {
            val handSuits = handSuits[hand.id]
            val size = hand.size
            hand.clear()
            while (hand.size < size) {
                val cards = suitCards[handSuits.random()]
                if (cards.isNotEmpty()) {
                    hand += cards.drawTop()
                }
            }
        }
    }


    override fun clone() = MctsPlayer(this)

    override fun toString() = super.toString().dropLast(1) + ", difficulty: $difficulty]"

    enum class Difficulty {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED,
        EXPERT
    }

}