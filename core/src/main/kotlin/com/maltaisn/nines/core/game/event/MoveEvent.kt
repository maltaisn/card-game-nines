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
import com.maltaisn.cardgame.game.CardGameEvent
import com.maltaisn.cardgame.game.CardPlayer

/**
 * The base class for any move made by a player.
 */
abstract class MoveEvent : GameEvent(), CardGameEvent.Move {

    /**
     * The position of the player who made the move.
     */
    var playerPos = CardPlayer.NO_POSITION
        protected set


    override fun equals(other: Any?) = other === this || other is MoveEvent &&
            playerPos == other.playerPos

    override fun hashCode() = playerPos


    override fun read(json: Json, jsonData: JsonValue) {
        playerPos = jsonData.getInt("playerPos")
    }

    override fun write(json: Json) {
        json.writeValue("playerPos", playerPos)
    }

}
