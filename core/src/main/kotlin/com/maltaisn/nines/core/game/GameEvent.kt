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
import com.maltaisn.cardgame.core.CardGameEvent
import com.maltaisn.cardgame.core.CardPlayer
import com.maltaisn.cardgame.core.Mcts
import com.maltaisn.cardgame.readArrayValue


sealed class GameEvent : CardGameEvent {

    object Start : GameEvent()

    object End : GameEvent()

    class RoundStart() : GameEvent(), Json.Serializable {

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

    object RoundEnd : GameEvent()

    /**
     * The base class for any move in any game made by a player at a [position][playerPos].
     * All subclasses MUST implement [equals] for [Mcts] to work.
     */
    abstract class Move : GameEvent(), CardGameEvent.Move, Json.Serializable {

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
