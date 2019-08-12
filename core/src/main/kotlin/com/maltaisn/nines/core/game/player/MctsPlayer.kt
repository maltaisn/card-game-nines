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
import com.maltaisn.cardgame.game.drawTop
import com.maltaisn.cardgame.pcard.PCard
import com.maltaisn.cardgame.readValue
import com.maltaisn.cardgame.utils.BitField
import com.maltaisn.cardgame.utils.Hungarian
import com.maltaisn.nines.core.game.GameState
import com.maltaisn.nines.core.game.Hand
import com.maltaisn.nines.core.game.event.PlayMove
import com.maltaisn.nines.core.game.event.TradeHandMove
import kotlin.random.Random

/**
 * A player played by the computer using MCTS algorithm. This player gathers information
 * from the game like played cards, known hands, known suits in other player hands to
 * restrict the possible game states when randomizing.
 *
 * @property difficulty The playing difficulty for this player.
 *
 * Higher difficulties do more simulations to find better moves.
 * Games won when playing 1000 games against 2 players of the difficulty below:
 * - Beginner: 962, Random: 14, Random: 24
 * - Intermediate: 582, Beginner: 211, Beginner: 207
 * - Advanced: 607, Intermediate: 185, Intermediate: 208
 * - Expert: 500, Advanced: 238, Advanced: 262
 *
 * So each difficulty is about 2-3x harder than the previous one.
 *
 * The intermediate difficulty can remember its previous hand after trading,
 * but it will forget it if any other player trades afterwards.
 *
 * The advanced difficulty will remember its previous hand no matter what.
 *
 * The expert difficulty will notice when a player cannot follow the trick suit
 * to know what suits players are out of.
 */
class MctsPlayer(var difficulty: Int = -1) : AiPlayer() {

    /**
     * Bit field of known hand IDs. These won't be randomized by [randomizeGameState].
     */
    private var knownHandIds = BitField()

    /**
     * Bit field of possible suits in each hand, by hand ID.
     * When a player can't follow the suit, the suit is removed from the bit field.
     */
    private var knownSuits: Array<BitField> = emptyArray()


    override fun initialize(position: Int, hand: Hand) {
        super.initialize(position, hand)

        knownHandIds += hand.id
        knownSuits = Array(4) { BitField(15) }
    }

    override fun findMove(state: GameState): CardGameEvent.Move {
        val iter = when (difficulty) {
            DIFF_BEGINNER -> 10
            DIFF_INTERMEDIATE -> 25
            DIFF_ADVANCED -> 100
            DIFF_EXPERT -> 500
            else -> 0
        }

        return if (state.phase == GameState.Phase.TRADE) {
            // Do random simulations of trading and not trading.
            // Choose the option that maximizes the average result.
            // This is better than MCTS itself because we don't want exploitation, both
            // options must be tested the same. With full MCTS, an option could be only tested
            // once in 10000 simulations if the initial result is bad enough.
            val moves = state.getMoves()
            moves.maxBy { Mcts.simulate(state, it, iter / moves.size) }!!
        } else {
            Mcts.run(state, iter)
        }
    }

    override fun onMove(state: GameState, move: CardGameEvent.Move) {
        if (move is TradeHandMove) {
            if (move.playerPos == position && move.trade) {
                // This player traded.
                if (difficulty == DIFF_BEGINNER) {
                    // Forget previous hand.
                    knownHandIds = BitField()
                }

                // Add new hand to known hands.
                knownHandIds += hand.id

            } else if (difficulty == DIFF_INTERMEDIATE) {
                // Another player traded. Forget previous hand.
                knownHandIds = BitField()
                knownHandIds += hand.id
            }

        } else if (move is PlayMove && difficulty >= DIFF_EXPERT) {
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
                knownSuits[id].minus(trickSuit)
            }
        }
    }


    override fun randomizeGameState(state: CardGameState<*>) {
        state as GameState

        // Create a list of all hands unknown to the observer.
        val unknownHands = mutableListOf<Hand>()
        for (player in state.players) {
            if (player.hand.id !in knownHandIds) {
                // The player hasn't seen this hand
                unknownHands += player.hand
            }
        }
        if (state.extraHand.id !in knownHandIds) {
            // The player hasn't seen the extra hand
            unknownHands += state.extraHand
        }

        // Take all cards from the unknown hands
        val unknownCards = mutableListOf<PCard>()
        for (hand in unknownHands) {
            unknownCards += hand.cards
        }

        if (unknownCards.isNotEmpty()) {
            if (difficulty < DIFF_EXPERT) {
                // Naively redistribute the cards in the unseen hands, without taking known suits into account.
                unknownCards.shuffle()
                for (hand in unknownHands) {
                    val size = hand.cards.size
                    hand.cards.clear()
                    hand.cards += unknownCards.drawTop(size)
                }

            } else {
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
                            if (unknownCards[it].suit in knownSuits[hand.id]) {
                                Random.nextInt(1024)
                            } else {
                                Hungarian.DISALLOWED
                            }
                        }
                        i++
                    }
                }

                val assignment = Hungarian(costMatrix.requireNoNulls()).execute()
                i = 0
                for (hand in unknownHands) {
                    val size = hand.cards.size
                    hand.cards.clear()
                    repeat(size) {
                        hand.cards += unknownCards[assignment[i]]
                        //assert(hand.cards.last().suit in knownSuits[hand.id])
                        i++
                    }
                }
            }
        }
    }


    override fun clone() = cloneTo(MctsPlayer(difficulty)).also {
        it.knownHandIds = knownHandIds
        it.knownSuits = knownSuits.clone()
    }

    override fun toString() = super.toString().dropLast(1) + ", difficulty: $difficulty]"


    override fun read(json: Json, jsonData: JsonValue) {
        super.read(json, jsonData)
        difficulty = json.readValue("difficulty", jsonData)
        knownHandIds = BitField(jsonData.getInt("knownHands"))

        val suits = jsonData["knownSuits"].asIntArray()
        knownSuits = Array(suits.size) { BitField(suits[it]) }
    }

    override fun write(json: Json) {
        super.write(json)
        json.writeValue("difficulty", difficulty)
        json.writeValue("knownHands", knownHandIds.value)
        json.writeValue("knownSuits", IntArray(4) { knownSuits[it].value })
    }


    companion object {
        const val DIFF_BEGINNER = 0
        const val DIFF_INTERMEDIATE = 1
        const val DIFF_ADVANCED = 2
        const val DIFF_EXPERT = 3
    }

}
