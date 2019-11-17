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
import com.maltaisn.nines.core.game.GameState
import com.maltaisn.nines.core.game.Hand
import ktx.json.readArrayValue

/**
 * An event for the start of a round.
 * The event saves the trump suit of the round and the initial hands.
 */
class RoundStartEvent() : GameEvent() {

    var trumpSuit = GameState.NO_TRUMP
        private set

    lateinit var hands: List<Hand>
        private set

    constructor(trumpSuit: Int, hands: List<Hand>) : this() {
        this.trumpSuit = trumpSuit
        this.hands = hands
    }

    override fun equals(other: Any?) = other == this || other is RoundStartEvent &&
            trumpSuit == other.trumpSuit && hands == other.hands

    override fun hashCode() = arrayOf(trumpSuit, hands).contentHashCode()

    override fun toString() = "Round start [trumpSuit: $trumpSuit, hands: $hands]"


    override fun read(json: Json, jsonData: JsonValue) {
        trumpSuit = jsonData.getInt("trumpSuit")
        hands = json.readArrayValue(jsonData, "hands")
    }

    override fun write(json: Json) {
        json.writeValue("trumpSuit", trumpSuit)
        json.writeValue("hands", hands)
    }
}
