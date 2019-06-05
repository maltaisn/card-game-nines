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
import com.maltaisn.cardgame.readValue


/**
 * A move representing a player playing a card in a trick
 */
class PlayMove() : GameEvent.Move() {

    /**
     * The card played.
     */
    lateinit var card: PCard
        private set


    constructor(playerPos: Int, card: PCard) : this() {
        this.playerPos = playerPos
        this.card = card
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is PlayMove) return false
        return playerPos == other.playerPos && card == other.card
    }

    override fun hashCode() = card.value

    override fun toString() = "Play $card"


    override fun read(json: Json, jsonData: JsonValue) {
        super.read(json, jsonData)
        card = json.readValue("card", jsonData)
    }

    override fun write(json: Json) {
        super.write(json)
        json.writeValue("card", card)
    }

}