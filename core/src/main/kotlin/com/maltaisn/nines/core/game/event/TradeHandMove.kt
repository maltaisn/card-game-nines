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


/**
 * A move event in which a player chooses whether to trade with the extra hand or not.
 */
class TradeHandMove() : MoveEvent() {

    /**
     * Whether the move is a hand trade or a pass.
     */
    var trade = false
        private set


    constructor(playerPos: Int, trade: Boolean) : this() {
        this.playerPos = playerPos
        this.trade = trade
    }


    override fun equals(other: Any?): Boolean {
        if (other !is TradeHandMove) return false
        return playerPos == other.playerPos && trade == other.trade
    }

    override fun hashCode() = playerPos

    override fun toString() = if (trade) "Trade hand" else "Don't trade hand"


    override fun read(json: Json, jsonData: JsonValue) {
        super.read(json, jsonData)
        trade = jsonData.getBoolean("trade")
    }

    override fun write(json: Json) {
        super.write(json)
        json.writeValue("trade", trade)
    }

}
