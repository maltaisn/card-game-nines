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

package com.maltaisn.nines.core.game.player

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.maltaisn.cardgame.game.CardGameEvent
import com.maltaisn.cardgame.game.CardGameState
import com.maltaisn.cardgame.game.ai.Mcts
import com.maltaisn.cardgame.game.ai.Munkres
import com.maltaisn.cardgame.pcard.PCard
import com.maltaisn.cardgame.readValue
import com.maltaisn.nines.core.game.GameState
import com.maltaisn.nines.core.game.Hand
import com.maltaisn.nines.core.game.event.PlayMove
import com.maltaisn.nines.core.game.event.TradeHandMove
import kotlin.random.Random

/**
 * A player played by the computer using MCTS algorithm. This player gathers information
 * from the game like played cards, known hands, known suits in other player hands to
 * restrict the possible game states when randomizing.
 * @property difficulty The playing difficulty for this player.
 */
class MctsPlayer(var difficulty: Int = -1) : AiPlayer() {

    /**
     * Bitfield of known hand IDs. These won't be randomized by [randomizeState].
     */
    private var knownHands = 0

    /**
     * Bit field of possible suits in each hand, by hand ID.
     * When a player can't follow the suit, the suit is removed from the bit field.
     */
    private var knownSuits = IntArray(0)


    override fun initialize(position: Int, hand: Hand) {
        super.initialize(position, hand)

        knownHands = 1 shl hand.id
        knownSuits = IntArray(4) { 15 }
    }

    override fun findMove(state: GameState) =
            if (state.phase == GameState.Phase.TRADE) {
                // Do random simulations of trading and not trading.
                // Choose the option that maximizes the average result.
                // This is better than MCTS itself because we don't want exploitation, both
                // options must be tested the same. With full MCTS, an option could be only tested
                // once in 10000 simulations if the initial result is bad enough.
                state.getMoves().maxBy { Mcts.simulate(state, it, 1000) }!!
            } else {
                Mcts.run(state, 1000)
            }

    override fun onMove(state: GameState, move: CardGameEvent.Move) {
        if (move is TradeHandMove) {
            if (move.playerPos == position && move.trade) {
                // Now two hands are known.
                knownHands = knownHands or (1 shl hand.id)
            }

        } else if (move is PlayMove) {
            val trick = if (state.currentTrick.cards.isEmpty()) {
                state.tricksPlayed.last()
            } else {
                state.currentTrick
            }
            val trickSuit = trick.suit
            if (trick.cards.last().suit != trickSuit) {
                // The player that moved couldn't follow suit, remove the
                // trick suit from the known suits of the hand.
                val id = state.players[move.playerPos].hand.id
                knownSuits[id] = knownSuits[id] and (1 shl trickSuit).inv()
            }
        }
    }


    override fun randomizeGameState(state: CardGameState<*>) {
        state as GameState

        // TODO adjust for difficulty

        // Create a list of all hands unknown to the observer.
        val unknownHands = mutableListOf<Hand>()
        for (p in state.players) {
            if (knownHands and (1 shl p.hand.id) == 0) {
                // The player hasn't seen this hand
                unknownHands += p.hand
            }
        }
        if (knownHands and (1 shl state.extraHand.id) == 0) {
            // The player hasn't seen the extra hand
            unknownHands += state.extraHand
        }

        // Take all cards from the unknown hands
        val unknownCards = mutableListOf<PCard>()
        for (hand in unknownHands) {
            unknownCards += hand.cards
        }

/*
        // Naively redistribute the cards in the unseen hands, without taking known suits into account.
        unseen.shuffle()
        for (hand in hands) {
            val size = hand.cards.size
            hand.cards.clear()
            hand.cards += unseen.drawTop(size)
        }
*/

        // Redistribute the cards to the unseen hands, giving only cards of known suits in each hand.
        // This is solved using the Hungarian algorithm, where unknown cards are "workers" that are
        // assigned to a position in an unknown hand, the "task".

        // Build a cost matrix, with random costs between 0 and 1 for cards that can be assigned
        // and infinite costs for cards that cannot be assigned.
        var i = 0
        val costMatrix = arrayOfNulls<IntArray>(unknownCards.size)
        for (hand in unknownHands) {
            repeat(hand.cards.size) {
                costMatrix[i] = IntArray(unknownCards.size) {
                    if (knownSuits[hand.id] and (1 shl unknownCards[it].suit) != 0) {
                        Random.nextInt(0, 1024)
                    } else {
                        Int.MAX_VALUE
                    }
                }
                i++
            }
        }

        val assignment = Munkres(costMatrix.requireNoNulls()).findOptimalAssignment()
        i = 0
        for (hand in unknownHands) {
            val size = hand.cards.size
            hand.cards.clear()
            repeat(size) {
                hand.cards += unknownCards[assignment[i]]
                //assert(knownSuits[hand.id] and (1 shl hand.cards.last().suit) != 0)
                i++
            }
        }
    }


    override fun clone() = cloneTo(MctsPlayer(difficulty)).also {
        it.knownHands = knownHands
        it.knownSuits = knownSuits.clone()
    }

    override fun toString() = super.toString().dropLast(1) + ", difficulty: $difficulty]"


    override fun read(json: Json, jsonData: JsonValue) {
        super.read(json, jsonData)
        difficulty = json.readValue("difficulty", jsonData)
        knownHands = jsonData.getInt("knownHands")
        knownSuits = jsonData["knownSuits"].asIntArray()
    }

    override fun write(json: Json) {
        super.write(json)
        json.writeValue("difficulty", difficulty)
        json.writeValue("knownHands", knownHands)
        json.writeValue("knownSuits", knownSuits)
    }


    companion object {
        const val DIFF_BEGINNER = 0
        const val DIFF_INTERMEDIATE = 1
        const val DIFF_ADVANCED = 2
        const val DIFF_EXPERT = 3
    }

}
