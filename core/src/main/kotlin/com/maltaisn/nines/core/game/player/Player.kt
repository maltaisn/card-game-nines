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

package com.maltaisn.nines.core.game.player

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.maltaisn.cardgame.game.CardGameEvent
import com.maltaisn.cardgame.game.CardPlayer
import com.maltaisn.cardgame.readValue
import com.maltaisn.nines.core.game.GameState
import com.maltaisn.nines.core.game.Hand


abstract class Player : CardPlayer(), Json.Serializable {

    /** The player score at the start of the round. */
    var score = 0

    /** The player's hand. */
    var hand = EMPTY_HAND

    /** Whether the player has traded his hand or not. */
    var trade = Trade.UNKNOWN

    /** The number of tricks taken by a player. */
    var tricksTaken = 0

    /**
     * Initialize this player to a [position] and with a [hand].
     */
    open fun initialize(position: Int, hand: Hand) {
        this.position = position
        this.hand = hand

        trade = Trade.UNKNOWN
        tricksTaken = 0
    }

    /**
     * Called when [state] performs a [move] for any player.
     */
    open fun onMove(state: GameState, move: CardGameEvent.Move) = Unit


    protected fun <T : Player> cloneTo(player: T) = super.cloneTo(player).also {
        it.score = score
        it.hand = hand.clone()
        it.trade = trade
        it.tricksTaken = tricksTaken
    }


    override fun toString() = super.toString().dropLast(1) +
            ", score: $score, tricksTaken: $tricksTaken, hand: $hand]"


    override fun read(json: Json, jsonData: JsonValue) {
        super.read(json, jsonData)
        score = jsonData.getInt("score")
        hand = json.readValue("hand", jsonData)
        trade = json.readValue("trade", jsonData)
        tricksTaken = jsonData.getInt("tricksTaken")
    }

    override fun write(json: Json) {
        super.write(json)
        json.writeValue("score", score)
        json.writeValue("hand", hand)
        json.writeValue("trade", trade)
        json.writeValue("tricksTaken", tricksTaken)
    }

    enum class Trade {
        TRADE,  // Has traded
        NO_TRADE,  // Hasn't traded
        UNKNOWN  // Hasn't had the choice yet
    }

    companion object {
        private val EMPTY_HAND = Hand(Hand.NO_ID, mutableListOf())
    }

}
