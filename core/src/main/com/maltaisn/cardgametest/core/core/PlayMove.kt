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

package com.maltaisn.cardgametest.core.core

import com.maltaisn.cardgame.core.BaseMove
import com.maltaisn.cardgame.core.PCard


/**
 * A move representing a player playing a card in a trick
 */
@Suppress("EqualsOrHashCode")
class PlayMove(player: Int, val card: PCard) : BaseMove(player) {

    override fun toString() = "Play $card"

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is PlayMove) return false
        return super.equals(other) && card == other.card
    }

}