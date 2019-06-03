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

package com.maltaisn.nines.core.game

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.maltaisn.cardgame.core.PCard
import com.maltaisn.cardgame.readArrayValue


/**
 * A game trick, made of 0 to 3 cards.
 */
class Trick() : Cloneable, Json.Serializable {

    /**
     * Trump suit when this trick was played.
     */
    var trumpSuit = 0
        private set

    /**
     * The cards in the trick. There should always be between 0 and 3 cards.
     */
    val cards = mutableListOf<PCard>()


    constructor(trumpSuit: Int, cards: List<PCard> = ArrayList(3)): this() {
        this.trumpSuit = trumpSuit
        this.cards += cards
    }

    /**
     * Get the required suit in this trick if there's at least one card in it.
     * Returns `-1` if there's no card in trick.
     */
    val suit: Int
        get() = if (cards.isEmpty()) -1 else cards.first().suit

    /**
     * Find the index of the highest card in the trick.
     */
    fun findHighest(): Int {
        var highestIndex = 0
        var highest = cards.first()
        for (i in 1 until cards.size) {
            val card = cards.get(i)

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

    public override fun clone() = Trick(trumpSuit, cards)

    override fun toString() = cards.toString()


    override fun read(json: Json, jsonData: JsonValue) {
        trumpSuit = jsonData.getInt("trump")
        cards += json.readArrayValue<ArrayList<PCard>, PCard>("cards", jsonData)
    }

    override fun write(json: Json) {
        json.writeValue("trump", trumpSuit)
        json.writeValue("cards", cards)
    }

}