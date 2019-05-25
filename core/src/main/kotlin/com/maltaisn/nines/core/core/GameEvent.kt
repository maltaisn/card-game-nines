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

import com.maltaisn.cardgame.core.CardGameEvent


sealed class GameEvent : CardGameEvent {

    object Start : GameEvent()

    object End : GameEvent()

    object RoundStart : GameEvent()

    object RoundEnd : GameEvent()

    /**
     * The base class for any move in any game made by a player at [playerPos].
     * All subclasses MUST implement [equals] for [Mcts] to work.
     */
    abstract class Move(val playerPos: Int) : GameEvent(), CardGameEvent.Move {

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is Move) return false
            return playerPos == other.playerPos
        }

        override fun hashCode() = playerPos
    }

}
