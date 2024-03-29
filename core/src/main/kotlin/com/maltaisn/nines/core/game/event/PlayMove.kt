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

package com.maltaisn.nines.core.game.event

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.maltaisn.cardgame.pcard.PCard
import ktx.json.readValue


/**
 * A move in which a player plays a card of his hand to a trick.
 */
class PlayMove() : MoveEvent() {

    /**
     * The card played.
     */
    lateinit var card: PCard
        private set


    constructor(playerPos: Int, card: PCard) : this() {
        this.playerPos = playerPos
        this.card = card
    }

    override fun equals(other: Any?) = other === this || other is PlayMove &&
            super.equals(other) && card == other.card

    override fun hashCode() = arrayOf(super.hashCode(), card).contentHashCode()

    override fun toString() = "Play $card"


    override fun read(json: Json, jsonData: JsonValue) {
        super.read(json, jsonData)
        card = json.readValue(jsonData, "card")
    }

    override fun write(json: Json) {
        super.write(json)
        json.writeValue("card", card)
    }

}
