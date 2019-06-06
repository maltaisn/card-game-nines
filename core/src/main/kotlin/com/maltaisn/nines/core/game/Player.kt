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
import com.maltaisn.cardgame.core.CardPlayer
import com.maltaisn.cardgame.readArrayValue
import com.maltaisn.cardgame.readValue


abstract class Player : CardPlayer(), Json.Serializable {

    /** The player score at the start of the round. */
    var score = 0

    /** The player's hand. */
    var hand = EMPTY_HAND

    /** List of the tricks taken by a player. */
    val tricksTaken = mutableListOf<Trick>()

    /**
     * Initialize this player to a [position] and with a [hand].
     */
    open fun initialize(position: Int, hand: Hand) {
        tricksTaken.clear()

        this.position = position
        this.hand = hand
    }

    abstract override fun clone(): Player

    protected fun <T : Player> cloneTo(player: T) = super.cloneTo(player).also {
        it.score = score
        it.hand = hand.clone()
        for (trick in tricksTaken) {
            it.tricksTaken += trick.clone()
        }
    }


    override fun toString() = super.toString().dropLast(1) +
            ", score: $score, ${tricksTaken.size} tricks taken, hand: $hand]"


    override fun read(json: Json, jsonData: JsonValue) {
        super.read(json, jsonData)
        score = jsonData.getInt("score")
        hand = json.readValue("hand", jsonData)
        tricksTaken += json.readArrayValue<ArrayList<Trick>, Trick>("tricksTaken", jsonData)
    }

    override fun write(json: Json) {
        super.write(json)
        json.writeValue("score", score)
        json.writeValue("hand", hand)
        json.writeValue("tricksTaken", tricksTaken)
    }


    companion object {
        private val EMPTY_HAND = Hand(Hand.NO_ID, mutableListOf())
    }

}