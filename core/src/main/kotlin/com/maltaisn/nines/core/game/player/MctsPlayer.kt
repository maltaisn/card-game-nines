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
import com.maltaisn.cardgame.utils.BitField
import com.maltaisn.cardgame.utils.Hungarian
import com.maltaisn.nines.core.game.GameState
import com.maltaisn.nines.core.game.Hand
import com.maltaisn.nines.core.game.event.PlayMove
import com.maltaisn.nines.core.game.event.TradeHandMove
import ktx.json.readValue
import kotlin.math.max
import kotlin.math.roundToInt
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
 * - Beginner: 680, Random: 114, Random: 206
 * - Intermediate: 635, Beginner: 178, Beginner: 187
 * - Advanced: 560, Intermediate: 202, Intermediate: 238
 * - Expert: 602, Advanced: 185, Advanced: 213
 * - Perfect: 585, Expert: 204, Expert: 211
 * - Cheating: 509, Perfect: 239, Perfect: 252
 *
 * Beginner should be much better than random but still very easily beatable.
 * Expert should be worse than perfect, which is an approximation of the best possible player,
 * so it's possible for good players to beat it. Difficulties between beginner and expert
 * should be evenly distributed, in this case, each difficulty is ~3x better than the last.
 * The fact that the cheating player doesn't always win against the perfect player
 * shows that luck plays a big part in a game of Nines.
 */
class MctsPlayer() : AiPlayer() {

    lateinit var difficulty: Difficulty
        private set

    /**
     * Bit field of known hand IDs. These won't be randomized by [randomizeGameState].
     */
    private var knownHandIds = BitField()

    /**
     * Bit field of possible suits in each hand, by hand ID.
     * When a player can't follow the suit, the suit is removed from the bit field.
     */
    private var knownSuits: Array<BitField> = emptyArray()


    constructor(difficulty: Difficulty) : this() {
        this.difficulty = difficulty
    }


    override fun initialize(position: Int, hand: Hand) {
        super.initialize(position, hand)

        knownHandIds += hand.id
        knownSuits = Array(4) { BitField(15) }
    }

    override fun findMove(state: GameState): CardGameEvent.Move {
        val moves = state.getMoves()
        return if (state.phase == GameState.Phase.TRADE) {
            // Do random simulations of trading and not trading.
            // Choose the option that maximizes the average result.
            // This is better than MCTS itself because we don't want exploitation, both
            // options must be tested the same. With full MCTS, an option could be only tested
            // once in 10000 simulations if the initial result is bad enough.
            moves.maxBy { Mcts.simulate(state, it, difficulty.tradeIter) }!!
        } else {
            Mcts.run(state, max(moves.size, difficulty.playIter))
        }
    }

    override fun onMove(state: GameState, move: CardGameEvent.Move) {
        if (move is TradeHandMove) {
            if (move.playerPos == position && move.trade) {
                // This player traded.
                if (!difficulty.remembersHandAfterTrade) {
                    // Forget previous hand.
                    knownHandIds = BitField()
                }

                // Add new hand to known hands.
                knownHandIds += hand.id
            }

        } else if (move is PlayMove && difficulty.rememberOpponentSuits) {
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

        // Separate cards that the player knows and those it doesn't.
        val knownCards = state.tricksPlayed.flatMapTo(mutableListOf()) { it.cards }
        val unknownCards = mutableListOf<PCard>()
        val unknownHands = mutableListOf<Hand>()
        for (player in state.players) {
            if (player.hand.id !in knownHandIds) {
                unknownHands += player.hand
                unknownCards += player.hand.cards
            } else if (player.position != position) {
                // Don't include the player's own cards in known cards.
                knownCards += player.hand.cards
            }
        }
        if (state.extraHand.id in knownHandIds) {
            knownCards += state.extraHand.cards
        } else {
            unknownHands += state.extraHand
            unknownCards += state.extraHand.cards
        }

        // Move a few random cards from known cards to unknown cards so the player forgets about them.
        unknownCards += knownCards.subList(0, (difficulty.forgetRatio * knownCards.size).roundToInt())

        // Redistribute the unknown cards in the unknown hands.
        if (unknownCards.isNotEmpty()) {
            if (difficulty.rememberOpponentSuits) {
                assert(difficulty.forgetRatio == 0f)

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

            } else {
                // Naively redistribute the cards in the unseen hands, without taking known suits into account.
                unknownCards.shuffle()
                for (hand in unknownHands) {
                    val size = hand.cards.size
                    hand.cards.clear()
                    hand.cards += unknownCards.drawTop(size)
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
        difficulty = json.readValue(jsonData, "difficulty")
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

    /**
     * A player difficulty.
     * @property tradeIter Number of trades simulated during trade phase.
     * @property playIter Number of moves simulated each turn of the play phase.
     * @property forgetRatio The percentage of played cards that the player forgets.
     * @property remembersHandAfterTrade Whether the player remembers its previous hand after trading.
     * @property rememberOpponentSuits Whether the player remembers when a player is out of a suit.
     */
    enum class Difficulty(val tradeIter: Int,
                          val playIter: Int,
                          val forgetRatio: Float,
                          val remembersHandAfterTrade: Boolean,
                          val rememberOpponentSuits: Boolean) {

        BEGINNER(2, 0, 1f, false, false),
        INTERMEDIATE(3, 5, 0.8f, true, false),
        ADVANCED(6, 10, 0.4f, true, false),
        EXPERT(50, 50, 0f, true, true),
        PERFECT(250, 500, 0f, true, true)
    }

}
