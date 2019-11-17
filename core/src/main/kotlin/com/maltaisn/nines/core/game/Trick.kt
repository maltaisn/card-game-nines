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
import com.maltaisn.cardgame.game.CardPlayer
import com.maltaisn.cardgame.pcard.PCard
import ktx.json.readArrayValue


/**
 * A card trick containing 0 to 3 cards.
 */
class Trick() : Cloneable, Json.Serializable {

    /**
     * The position of the player who played the first card of the trick.
     */
    var startPos = CardPlayer.NO_POSITION
        private set

    /**
     * The cards in the trick. There should always be between 0 and 3 cards.
     */
    val cards = mutableListOf<PCard>()

    /**
     * The required suit in this trick if there's at least one card in it.
     * Returns `-1` if there's no card in trick.
     */
    val suit: Int
        get() = if (cards.isEmpty()) -1 else cards.first().suit


    constructor(startPos: Int) : this() {
        this.startPos = startPos
    }


    /**
     * Find the position of the player who played the highest card in the trick.
     */
    fun findWinner(trumpSuit: Int): Int {
        var highestIndex = 0
        var highest = cards.first()
        for (i in 1 until cards.size) {
            val card = cards[i]
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
        return (highestIndex + startPos) % 3
    }

    public override fun clone() = Trick().also { trick ->
        trick.startPos = startPos
        trick.cards += cards
    }

    override fun equals(other: Any?) = other === this || other is Trick &&
            startPos == other.startPos && cards == other.cards

    override fun hashCode() = arrayOf(startPos, cards).contentHashCode()

    override fun toString() = "[startPos: $startPos, cards: $cards]"


    override fun read(json: Json, jsonData: JsonValue) {
        startPos = jsonData.getInt("startPos")
        cards += json.readArrayValue<ArrayList<PCard>, PCard>(jsonData, "cards")
    }

    override fun write(json: Json) {
        json.writeValue("startPos", startPos)
        json.writeValue("cards", cards)
    }

}
