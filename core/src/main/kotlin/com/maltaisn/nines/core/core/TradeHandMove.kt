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

package com.maltaisn.nines.core.core

import com.maltaisn.cardgame.core.GameEvent


/**
 * A move representing a player trading his hand with the extra hand
 */
class TradeHandMove(player: Int, val trade: Boolean) : GameEvent.Move(player) {

    override fun toString() = if (trade) "Trade hand" else "Don't trade hand"

    override fun equals(other: Any?): Boolean {
        if (other !is TradeHandMove) return false
        return super.equals(other) && trade == other.trade
    }

    override fun hashCode() = playerPos.hashCode()

}