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
import kotlin.math.max


/**
 * A player played by the computer using heuristics i.e hardcoded strategies.
 *
 * ## Strategy
 * - Trade hand depending on hand score and how many players have already traded.
 *     Hand score is approximated by counting the number of tricks that can be made.
 * - If playing first or second in a trick play the aces, then the lowest card in the suit
 *     with the least cards.
 * - If playing last in a trick, play the lowest card that allows the player to take the trick.
 * - If player can't play higher in a trick, play the lowest card in the suit.
 *     - If suit is not available, play lowest trump.
 *     - If trump is not available, play lowest card of any suit.
 *
 * ## Weak points
 * - Algorithm for choosing whether to trade hands is very approximate.
 * - Remembering hand if player trades hand after this player.
 * - Remembering if any card was played, eg: player plays A♥, now K♥ is the highest in ♥.
 * - Playing middle ranked cards when second to prevent third player from too easily winning trick.
 */
class HeuristicPlayer : Player {

    constructor() : super()

    private constructor(player: HeuristicPlayer) : super(player)


    override fun play(state: BaseGameState<out BasePlayer>): BaseMove {
        state as GameState
        val moves = state.getMoves()

        if (moves.size == 1) return moves[0]

        when (state.phase) {
            GameState.Phase.PLAY -> {
                val trumpSuit = state.currentTrick.trumpSuit
                val cards = moves.map { (it as PlayMove).card }
                var cardPlayed: PCard? = null

                when (state.currentTrick.size) {
                    // Plays first, play any ace.
                    0 -> cardPlayed = cards.find { it.rank == PCard.ACE }

                    // Plays second, play any ace.
                    // If there's no ace, try to ruff.
                    1 -> cardPlayed = cards.find { it.rank == PCard.ACE }
                            ?: findLowestTrump(cards, trumpSuit)

                    // Plays last, find the lowest card that allows to take the trick.
                    2 -> cardPlayed = findLowestToWin(cards, state.currentTrick)
                }
                if (cardPlayed == null) {
                    // Find the lowest card in the suit with the least cards.
                    // This aims to discard short suits so player can ruff.
                    cardPlayed = findLowestInShortestSuit(cards, trumpSuit)
                }

                return moves.find { (it as PlayMove).card == cardPlayed }!!
            }
            GameState.Phase.TRADE -> {
                // Choose whether to trade hand.
                // This depends on how many players have already traded
                // since the quality of the extra hand decreases.
                val score = getHandScore(state.trumpSuit)
                val trade = score < when (state.tradesCount) {
                    0 -> 4.0
                    1 -> 3.0
                    2 -> 2.5
                    else -> 0.0
                }

                return moves.find { (it as TradeHandMove).trade == trade }!!
            }
        }
    }

    override fun clone() = HeuristicPlayer(this)

    /**
     * Get an approximate number of tricks that can be won with the player's hand
     */
    fun getHandScore(trumpSuit: Int): Double {
        var score = 0.0
        for (card in hand) {
            score += if (card.suit == trumpSuit) {
                if (card.rank == PCard.ACE) {
                    1.0
                } else {
                    max(0.5, (card.rank - 2) / 12.0)
                }
            } else {
                when (card.rank) {
                    PCard.ACE -> 1.0
                    PCard.KING -> 0.8
                    PCard.QUEEN -> 0.6
                    PCard.JACK -> 0.4
                    else -> 0.0
                }
            }
        }
        return score
    }


    companion object {
        /**
         * Find the lowest card to win a [trick] from [cards].
         * If player cannot win the trick, `null` is returned.
         * @param cards The list of cards that can be played.
         */
        internal fun findLowestToWin(cards: List<PCard>, trick: Trick): PCard? {
            val trickSuit = trick.getSuit()

            var lowest: PCard? = null
            for (i in 0 until cards.size) {
                val card = cards[i]
                if (card.suit == trick.trumpSuit || card.suit == trickSuit) {
                    // Only trump and same suit as first card allows to win
                    val dupTrick = trick.clone()
                    dupTrick += card
                    if ((lowest == null || card.lessThan(lowest, true)) &&
                            dupTrick.findHighest() == 2) {
                        // Playing this card made the player win.
                        // We want the lowest rank. All cards checked are necessarily of the same suit
                        // because player can either win by having the needed suit or by having trump.
                        lowest = card
                    }
                }
            }
            return lowest
        }

        /**
         * Find the lowest card in [trumpSuit], or `null` if there isn't one.
         * @param cards The list of cards that can be played.
         */
        internal fun findLowestTrump(cards: List<PCard>, trumpSuit: Int): PCard? {
            var lowest: PCard? = null
            for (i in 0 until cards.size) {
                val card = cards[i]
                if (card.suit == trumpSuit && (lowest == null || card.lessThan(lowest, true))) {
                    lowest = card
                }
            }
            return lowest
        }

        /**
         * Find the lowest card in the suit with the shortest cards, except [trumpSuit].
         */
        internal fun findLowestInShortestSuit(cards: List<PCard>, trumpSuit: Int): PCard {
            // Find the number of cards and the lowest card for each suit.
            val suitCards = IntArray(4)
            val lowestCards = arrayOfNulls<PCard>(4)
            for (card in cards) {
                val suit = card.suit
                suitCards[suit]++
                val lowestInSuit = lowestCards[suit]
                if (lowestInSuit == null || card.lessThan(lowestInSuit, true)) {
                    lowestCards[suit] = card
                }
            }

            // Return the lowest card in the shortest suit, except trump suit.
            var minSuit = -1
            for (i in PCard.SUIT_RANGE) {
                val count = suitCards[i]
                if (count != 0 && i != trumpSuit) {
                    if (minSuit == -1 || count < suitCards[minSuit]) {
                        minSuit = i
                    } else if (count == suitCards[minSuit]) {
                        // Two suits have same number of card
                        // Choose suits with the lowest lowest card
                        if (lowestCards[i]!!.lessThan(lowestCards[minSuit]!!, true)) {
                            minSuit = i
                        }
                    }
                }
            }
            return if (minSuit != -1) {
                lowestCards[minSuit]
            } else {
                // Player has only trump, no choice.
                lowestCards[trumpSuit]
            }!!
        }
    }
}