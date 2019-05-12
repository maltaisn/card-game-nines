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

import com.maltaisn.cardgame.core.Deck
import com.maltaisn.cardgame.core.PCard


/**
 * A game trick, made of 0 to 3 cards.
 */
class Trick : Deck<PCard> {

    /** The trump suit when this trick was played. */
    val trumpSuit: Int

    constructor(trumpSuit: Int) : super(3) {
        this.trumpSuit = trumpSuit
    }

    constructor(trumpSuit: Int, cards: Collection<PCard>) : super(cards) {
        this.trumpSuit = trumpSuit
        assert(cards.size <= 3)
    }

    /**
     * Get the required suit in this trick if there's at least one card in it.
     * Returns `-1` if there's no card in trick.
     */
    fun getSuit() = if (isEmpty()) -1 else get(0).suit

    /**
     * Find the index of the highest card in the trick.
     */
    fun findHighest(): Int {
        var highestIndex = 0
        var highest = first()
        for (i in 1 until size) {
            val card = get(i)

            if ((card.suit == trumpSuit && (highest.suit != trumpSuit
                            || card.greaterThan(highest, true)))
                    || card.suit == highest.suit
                    && (trumpSuit == GameState.NO_TRUMP || highest.suit != trumpSuit)
                    && card.greaterThan(highest, true)) {
                // Card is higher if:
                // - This card is trump and:
                //     - Highest isn't or;
                //     - Highest is but this card's rank is higher
                // - This card has the same suit as highest and:
                //     - There is no trump suit or highest isn't trump and;
                //     - This card's rank is higher
                highestIndex = i
                highest = card
            }
        }
        return highestIndex
    }

    override fun clone() = Trick(trumpSuit, super.clone())

}