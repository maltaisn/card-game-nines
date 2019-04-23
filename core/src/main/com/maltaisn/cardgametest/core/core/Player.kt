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

import com.maltaisn.cardgame.core.BasePlayer
import com.maltaisn.cardgame.core.PCard


abstract class Player : BasePlayer {

    /**
     * Player's hand.
     */
    lateinit var hand: Hand

    /**
     * List of the tricks taken by a player.
     */
    val tricksTaken = mutableListOf<Trick>()


    constructor() : super()

    protected constructor(player: Player) : super(player) {
        hand = player.hand.clone()
        for (trick in player.tricksTaken) {
            tricksTaken += trick.clone()
        }
    }

    /**
     * Initialize this player to a [position] and with a [hand].
     */
    open fun initialize(position: Int, hand: Hand) {
        tricksTaken.clear()

        this.position = position
        this.hand = hand
    }

    override fun toString() = super.toString() + ", tricks: ${tricksTaken.size}, " +
            "hand: ${hand.toSortedString(PCard.DEFAULT_SORTER)}"

}