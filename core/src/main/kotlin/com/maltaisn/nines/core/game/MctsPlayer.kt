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
import com.maltaisn.cardgame.game.CardGameState
import com.maltaisn.cardgame.game.PCard
import com.maltaisn.cardgame.game.ai.Mcts
import com.maltaisn.cardgame.game.drawTop
import com.maltaisn.cardgame.readValue
import com.maltaisn.nines.core.game.event.TradeHandMove

/**
 * Defines a player played by the computer using MCTS algorithm.
 */
class MctsPlayer() : Player() {

    /**
     * Bitfield of known hand IDs. These won't be randomized by [randomizeState].
     */
    var knownHands = 0

    /**
     * The playing difficulty for this player.
     */
    lateinit var difficulty: Difficulty


    constructor(difficulty: Difficulty) : this() {
        this.difficulty = difficulty
    }


    override fun initialize(position: Int, hand: Hand) {
        super.initialize(position, hand)

        knownHands = 1 shl hand.id
    }

    /**
     * Find the best move to play given the current game state.
     * This always gets moves from [CardGameState.getMoves], never creates them.
     */
    fun findMove(state: CardGameState<*>): CardGameEvent.Move {
        state as GameState
        return if (state.phase == GameState.Phase.TRADE) {
            // Do random simulations of trading and not trading.
            // Choose the option that maximizes the average result.
            // This is better than MCTS itself because we don't want exploitation, both
            // options must be tested the same. With full MCTS, an option could be only tested
            // once in 10000 simulations if the initial result is bad enough.
            state.getMoves().maxBy { Mcts.simulate(state, it, 1000) }!!
        } else {
            Mcts.run(state, 1000)
        }
    }

    override fun onMove(state: CardGameState<*>, move: CardGameEvent.Move) {
        state as GameState

        if (move is TradeHandMove) {
            if (move.playerPos == position && move.trade) {
                // Now two hands are known.
                knownHands = knownHands or (1 shl hand.id)
            }
        }
    }


    /**
     * Randomizes a game [state] from the point of view of this player.
     */
    fun randomizeState(state: GameState) {
        // TODO adjust for difficulty

        // Create a list of all hands unknown to the observer.
        val hands = mutableListOf<Hand>()
        for (p in state.players) {
            if (knownHands and (1 shl p.hand.id) == 0) {
                hands += p.hand
            }
        }
        if (knownHands and (1 shl state.extraHand.id) == 0) {
            hands += state.extraHand
        }

        // Take all cards from these hands and shuffle them.
        val unseen = mutableListOf<PCard>()
        for (hand in hands) {
            unseen += hand.cards
        }
        unseen.shuffle()

        // Redistribute the cards to other players
        for (player in state.players) {
            if (player !== this) {
                val cards = player.hand.cards
                val size = cards.size
                cards.clear()
                cards += unseen.drawTop(size)
            }
        }
    }


    override fun clone() = cloneTo(MctsPlayer(difficulty)).also {
        it.knownHands = knownHands
    }

    override fun toString() = super.toString().dropLast(1) + ", difficulty: $difficulty]"


    override fun read(json: Json, jsonData: JsonValue) {
        super.read(json, jsonData)
        knownHands = jsonData.getInt("knownHands")
        difficulty = json.readValue("difficulty", jsonData)
    }

    override fun write(json: Json) {
        super.write(json)
        json.writeValue("knownHands", knownHands)
        json.writeValue("difficulty", difficulty)
    }


    enum class Difficulty {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED,
        EXPERT
    }

}
