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
import com.maltaisn.cardgame.game.player.CardMctsPlayer
import com.maltaisn.cardgame.pcard.PCard
import com.maltaisn.cardgame.utils.Hungarian
import com.maltaisn.nines.core.game.GameState
import com.maltaisn.nines.core.game.Hand
import com.maltaisn.nines.core.game.event.PlayMove
import com.maltaisn.nines.core.game.event.TradeHandMove
import ktx.json.readArrayValue
import ktx.json.readValue
import kotlin.math.max
import kotlin.random.Random

/**
 * A player played by the computer using MCTS algorithm. This player gathers information
 * from the game like played cards, known hands, known suits in other player hands so
 * that the randomized game state is a closer match to reality to enhance play level.
 *
 * @property difficulty The playing difficulty for this player.
 *
 * Higher difficulties do more simulations to find better moves.
 * Games won when playing against 2 players of the difficulty below:
 * - Beginner: 866, Random: 77, Random: 57 (out of 1000)
 * - Intermediate: 691, Beginner: 151, Beginner: 158 (out of 1000)
 * - Advanced: 548, Intermediate: 214, Intermediate: 238 (out of 1000)
 * - Expert: 222, Advanced: 151, Advanced: 127 (out of 500)
 * - Perfect: 39, Expert: 33, Expert: 28 (out of 200)
 * - Cheating: 78, Perfect: 11, Perfect: 11 (out of 100)
 *
 * Beginner should be much better than random but still very easily beatable.
 * Expert should be worse than perfect, which is an approximation of the best possible player,
 * so it's possible for good players to beat it. Expert is currently about 15% worse than perfect.
 *
 * Difficulties between beginner and expert should be relatively evenly distributed.
 * The fact that the cheating player doesn't always win against the perfect player
 * and that the beginner doesn't always win against the random player
 * shows that luck plays a big part in a game of Nines.
 */
class MctsPlayer() : AiPlayer(), CardMctsPlayer {

    private val mcts = Mcts()


    lateinit var difficulty: Difficulty
        private set

    /**
     * A map of cards known to belong to a hand (mapped by hand ID).
     * An ID of [Hand.NO_ID] means the card was played in a trick.
     */
    private val knownCards = linkedMapOf<PCard, Int>()

    /**
     * A list of possible card suits in a hand, indexed by hand ID.
     * This is only used if [Difficulty.rememberOpponentSuits] is `true`.
     */
    private var knownSuits: List<MutableList<Int>> = emptyList()


    override var isMctsClone = false


    constructor(difficulty: Difficulty) : this() {
        this.difficulty = difficulty
    }


    override fun initialize(position: Int, hand: Hand) {
        super.initialize(position, hand)

        // Add hand cards to initially known cards.
        knownCards.clear()
        rememberCurrentHand()

        // Initialize known suits to all suits.
        knownSuits = List(4) { PCard.SUITS.toMutableList() }
    }

    override fun findMove(state: GameState): CardGameEvent.Move {
        assert(!isMctsClone)

        val moves = state.getMoves()
        return if (state.phase == GameState.Phase.TRADE) {
            // Do random simulations of trading and not trading.
            // Choose the option that maximizes the average result.
            // This is better than MCTS itself because we don't want exploitation, both
            // options must be tested the same. With full MCTS, an option could be only tested
            // once in 1000 simulations if the initial result is bad enough.

            // The trade result is also weighted as more players trade to deter players.
            // This is the only heuristic used in the AI.
            val tradeMove = moves.find { it is TradeHandMove && it.trade }!!
            val noTradeMove = moves.find { it is TradeHandMove && !it.trade }!!
            val tradeScore = mcts.simulate(state, tradeMove, difficulty.tradeIter) * difficulty.tradeWeights[state.tradesCount]
            val noTradeScore = mcts.simulate(state, noTradeMove, difficulty.tradeIter)
            if (tradeScore > noTradeScore) tradeMove else noTradeMove

        } else {
            mcts.run(state, max(moves.size, difficulty.playIter))
        }
    }

    override fun onMove(state: GameState, move: CardGameEvent.Move) {
        if (isMctsClone) {
            // MCTS Clones have no interest to learn from moves since they never make informed decisions.
            // They only make random moves.
            return
        }

        // Remember game cards.
        when (move) {
            is TradeHandMove -> {
                if (move.playerPos == position && move.trade) {
                    // The player traded. Remember only some cards of the previous hand.
                    for (card in state.extraHand.cards) {
                        if (!shouldRemember()) {
                            knownCards -= card
                        }
                    }

                    // Remember new hand.
                    rememberCurrentHand()
                }
            }
            is PlayMove -> {
                // Only remember some of the cards played.
                if (shouldRemember() || move.card in knownCards) {
                    knownCards[move.card] = Hand.NO_ID
                }

                // Remember when a player is out of a suit.
                if (difficulty.rememberOpponentSuits && move.playerPos != position) {
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
                        knownSuits[id] -= trickSuit
                    }
                }
            }
        }
    }

    private fun shouldRemember() = Random.nextFloat() >= difficulty.forgetRatio

    private fun rememberCurrentHand() {
        knownCards += hand.cards.associateWith { hand.id }
    }


    override fun randomizeGameState(state: CardGameState<*>) {
        super.randomizeGameState(state)
        state as GameState

        // The next steps aim to separate all known cards and all unknown cards. A known card is a card that this
        // player knows belong to a specific hand. Known cards are given back to their known owners.
        // Unknown cards are randomly distributed to players and extra hand to fill the gaps. Resulting state has
        // the same number of cards everywhere but reflects this player's current knowledge of the game state.

        // List all hands in the game.
        val hands = Array(4) { EMPTY_HAND }
        hands[state.extraHand.id] = state.extraHand
        for (player in state.players) {
            hands[player.hand.id] = player.hand
        }

        // List all cards in the game (except current trick cards which are always considered known)
        val unknownCards = mutableListOf<PCard>()
        hands.flatMapTo(unknownCards) { it.cards }
        state.tricksPlayed.flatMapTo(unknownCards) { it.cards }

        // Clear all hands and keep track of how many cards they had.
        val missingCount = IntArray(4)
        for (hand in hands) {
            missingCount[hand.id] = hand.cards.size
            hand.cards.clear()
        }

        // Give known cards to their owner hand and remove them from the unknown list.
        for ((card, id) in knownCards) {
            unknownCards -= card
            if (id != Hand.NO_ID) {
                hands[id].cards += card
                missingCount[id]--
            }
        }

        // Redistribute the unknown cards to players.
        if (unknownCards.isNotEmpty()) {
            if (difficulty.rememberOpponentSuits && knownSuits.any { it.size < 4 }) {
                assert(difficulty.forgetRatio == 0f)

                // Redistribute the cards to the players, giving only cards of known suits in each hand.
                // This is solved using the Hungarian algorithm, where unknown cards are "workers" that are
                // assigned to a position in a player's hand, the "task".

                // Build a cost matrix, with random costs between 0 and 1 for cards that can be assigned
                // and arbitrarly high costs for cards that cannot be assigned.
                var i = 0
                val costMatrix = arrayOfNulls<FloatArray>(unknownCards.size)
                for (hand in hands) {
                    repeat(missingCount[hand.id]) {
                        costMatrix[i] = FloatArray(unknownCards.size) { card ->
                            if (unknownCards[card].suit in knownSuits[hand.id]) {
                                Random.nextFloat()
                            } else {
                                Hungarian.DISALLOWED
                            }
                        }
                        i++
                    }
                }

                // Find assignment and redistribute cards to players in the same order.
                val assignment = Hungarian(costMatrix.requireNoNulls()).execute()
                i = 0
                for (hand in hands) {
                    repeat(missingCount[hand.id]) {
                        val card = unknownCards[assignment[i]]
                        hand.cards += card
                        assert(card.suit in knownSuits[hand.id])
                        i++
                    }
                }

            } else {
                // Naively redistribute the player cards, without taking known suits into account.
                unknownCards.shuffle()
                for (hand in hands) {
                    hand.cards += unknownCards.drawTop(missingCount[hand.id])
                }
            }
        }
    }

    override fun clone() = cloneTo(MctsPlayer(difficulty)).also { p ->
        assert(!isMctsClone)
        p.knownCards += knownCards
        p.knownSuits = knownSuits.map { it.toMutableList() }
    }

    override fun equals(other: Any?) = other === this || other is MctsPlayer &&
            super.equals(other) && difficulty == other.difficulty &&
            knownSuits == other.knownSuits && isMctsClone == other.isMctsClone

    override fun hashCode() = arrayOf(super.hashCode(), difficulty,
            knownCards, knownSuits, isMctsClone).contentHashCode()

    override fun toString() = super.toString().dropLast(1) + ", difficulty: $difficulty]" +
            if (isMctsClone) "[CLONE]" else ""


    override fun read(json: Json, jsonData: JsonValue) {
        super.read(json, jsonData)

        difficulty = json.readValue(jsonData, "difficulty")
        knownSuits = json.readArrayValue<ArrayList<IntArray>, IntArray>(
                jsonData, "knownSuits").map { it.toMutableList() }

        val knownCardsKeys: ArrayList<PCard> = json.readArrayValue(jsonData, "knownCards")
        val knownCardsValues = jsonData["knownCardsPos"].asIntArray().toList()
        knownCards += knownCardsKeys.zip(knownCardsValues).toMap()
    }

    override fun write(json: Json) {
        super.write(json)
        assert(!isMctsClone)

        json.writeValue("difficulty", difficulty)
        json.writeValue("knownCards", knownCards.keys)
        json.writeValue("knownCardsPos", knownCards.values.toIntArray())
        json.writeValue("knownSuits", knownSuits.map { it.toIntArray() })
    }

    /**
     * A player difficulty.
     * @property tradeIter Number of trades simulated during trade phase.
     * @property playIter Number of moves simulated each turn of the play phase.
     * @property forgetRatio The percentage of played cards that the player forgets.
     * @property rememberOpponentSuits Whether the player remembers when a player is out of a suit.
     * @property tradeWeights Weight applied on trade move results to deter player from trading
     * when some players have already traded. Indexed by trades count, should be of length 3.
     */
    enum class Difficulty(val tradeIter: Int,
                          val playIter: Int,
                          val forgetRatio: Float,
                          val rememberOpponentSuits: Boolean,
                          val tradeWeights: FloatArray) {

        BEGINNER(2, 10, 0.75f, false, floatArrayOf(1f, 1f, 1f)),
        INTERMEDIATE(12, 50, 0.5f, false, floatArrayOf(1f, 1f, 1f)),
        ADVANCED(70, 200, 0.25f, false, floatArrayOf(1f, 0.85f, 0.7f)),
        EXPERT(140, 350, 0f, true, floatArrayOf(1f, 0.85f, 0.7f)),
        PERFECT(1000, 2000, 0f, true, floatArrayOf(1f, 0.85f, 0.7f))
    }

}
