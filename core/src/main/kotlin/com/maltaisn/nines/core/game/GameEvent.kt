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
import com.maltaisn.cardgame.game.CardGameEvent
import com.maltaisn.cardgame.game.CardPlayer
import com.maltaisn.cardgame.game.GameResult
import com.maltaisn.cardgame.readArrayValue
import com.maltaisn.cardgame.readValue


/**
 * Any event in a nines game.
 * Some events could be `object` but that's harder to deserialize.
 */
sealed class GameEvent : CardGameEvent, Json.Serializable {

    class Start : GameEvent() {
        override fun read(json: Json, jsonData: JsonValue) = Unit
        override fun write(json: Json) = Unit
    }

    class End : GameEvent() {
        override fun read(json: Json, jsonData: JsonValue) = Unit
        override fun write(json: Json) = Unit
    }

    class RoundStart() : GameEvent() {

        var hands = emptyList<Hand>()

        constructor(hands: List<Hand>) : this() {
            this.hands = hands
        }

        override fun read(json: Json, jsonData: JsonValue) {
            hands = json.readArrayValue("hands", jsonData)
        }

        override fun write(json: Json) {
            json.writeValue("hands", hands)
        }
    }

    class RoundEnd() : GameEvent() {

        lateinit var result: GameResult

        constructor(result: GameResult) : this() {
            this.result = result
        }

        override fun read(json: Json, jsonData: JsonValue) {
            result = json.readValue("result", jsonData)
        }

        override fun write(json: Json) {
            json.writeValue("result", result)
        }
    }

    /**
     * The base class for any move in any game made by a player at a [position][playerPos].
     */
    abstract class Move : GameEvent(), CardGameEvent.Move {

        var playerPos = CardPlayer.NO_POSITION
            protected set

        override fun read(json: Json, jsonData: JsonValue) {
            playerPos = jsonData.getInt("playerPos")
        }

        override fun write(json: Json) {
            json.writeValue("playerPos", playerPos)
        }
    }

}
