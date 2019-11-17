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
import com.maltaisn.cardgame.pcard.PCard
import com.maltaisn.cardgame.pcard.toSortedString
import ktx.json.readArrayValue


/**
 * A player's hand. Each hand have an ID.
 */
class Hand() : Cloneable, Json.Serializable {

    var id = NO_ID
        private set

    val cards = mutableListOf<PCard>()


    constructor(id: Int, cards: List<PCard>) : this() {
        this.id = id
        this.cards += cards
    }

    public override fun clone() = Hand(id, cards)

    override fun equals(other: Any?) = other === this || other is Hand &&
            id == other.id && cards == other.cards

    override fun hashCode() = arrayOf(id, cards).contentHashCode()

    override fun toString() = "[id: $id, cards: ${cards.toSortedString()}]"


    override fun read(json: Json, jsonData: JsonValue) {
        id = jsonData.getInt("id")
        cards += json.readArrayValue<ArrayList<PCard>, PCard>(jsonData, "cards")
    }

    override fun write(json: Json) {
        json.writeValue("id", id)
        json.writeValue("cards", cards)
    }

    companion object {
        const val NO_ID = -1
    }

}
