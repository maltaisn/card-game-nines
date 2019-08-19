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
import com.maltaisn.cardgame.game.CardPlayer
import com.maltaisn.cardgame.game.drawTop
import com.maltaisn.cardgame.pcard.PCard
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.nines.core.game.event.MoveEvent
import com.maltaisn.nines.core.game.event.PlayMove
import com.maltaisn.nines.core.game.event.TradeHandMove
import com.maltaisn.nines.core.game.player.Player
import ktx.json.readArrayValue
import ktx.json.readValue
import kotlin.random.Random

/**
 * Nines game state for a single round.
 */
class GameState() : CardGameState<Player>() {

    /** Position of the dealer. */
    var dealerPos = CardPlayer.NO_POSITION
        private set

    /** The extra hand dealt. */
    lateinit var extraHand: Hand
        private set

    /** The trump suit, a PCard suit constant or [NO_TRUMP]. */
    var trumpSuit = -1
        private set

    /** The current game phase. */
    var phase = Phase.TRADE
        private set

    /** The list of tricks that were played during the round. */
    val tricksPlayed = mutableListOf<Trick>()

    /** The current trick being played. */
    var currentTrick = Trick(CardPlayer.NO_POSITION)
        private set


    constructor(settings: GamePrefs, players: List<Player>,
                dealerPos: Int, trumpSuit: Int) : this() {
        initialize(settings, players, dealerPos)
        this.dealerPos = dealerPos
        this.trumpSuit = trumpSuit

        // Create and shuffle a 52-card deck.
        val deck = PCard.fullDecks(shuffled = true)

        // Deal 13 cards to each player + extra hand, assigning an ID to each hand.
        var id = 0
        for (player in players) {
            player.initialize(id, Hand(id, deck.drawTop(CARDS_COUNT)))
            id++
        }
        extraHand = Hand(id, deck.drawTop(CARDS_COUNT))
    }

    override fun doMove(move: CardGameEvent.Move) {
        val player = players[posToMove]
        when (move) {
            is TradeHandMove -> {
                // Swap player's hand with the extra hand
                player.trade = if (move.trade) {
                    val temp = player.hand
                    player.hand = extraHand
                    extraHand = temp
                    Player.Trade.TRADE
                } else {
                    Player.Trade.NO_TRADE
                }

                posToMove = getPositionNextTo(posToMove)

                if (posToMove == dealerPos) {
                    // Everyone had the chance to trade hands.
                    // Player to the left of the dealer starts.
                    phase = Phase.PLAY
                    posToMove = (dealerPos + 1) % 3
                    currentTrick = Trick(posToMove)
                }
            }
            is PlayMove -> {
                // Play card in trick
                currentTrick.cards += move.card
                player.hand.cards -= move.card

                if (currentTrick.cards.size == 3) {
                    // All players have played, find who takes the trick and leads next.
                    val trickWinner = currentTrick.findWinner(trumpSuit)

                    players[trickWinner].tricksTaken++
                    tricksPlayed += currentTrick

                    posToMove = trickWinner
                    currentTrick = Trick(posToMove)

                    if (tricksPlayed.size == CARDS_COUNT) {
                        // Round is done, create result.
                        result = List(3) { players[it].tricksTaken.toFloat() }
                    }
                } else {
                    posToMove = getPositionNextTo(posToMove)
                }
            }
        }

        for (p in players) {
            p.onMove(this, move)
        }
    }

    override fun getMoves(): MutableList<CardGameEvent.Move> {
        val moves = mutableListOf<CardGameEvent.Move>()
        if (isGameDone) {
            return moves
        }

        val player = players[posToMove]
        when (phase) {
            Phase.PLAY -> {
                if (currentTrick.cards.isNotEmpty()) {
                    // Player must follow the suit of first card in trick.
                    val trickSuit = currentTrick.suit
                    for (card in player.hand.cards) {
                        if (card.suit == trickSuit) {
                            moves += PlayMove(posToMove, card)
                        }
                    }
                }
                if (moves.isEmpty()) {
                    // Player plays first or has no cards in required suit, can play any.
                    for (card in player.hand.cards) {
                        moves += PlayMove(posToMove, card)
                    }
                }
            }
            Phase.TRADE -> {
                // Player has the choice to trade his hand with the extra hand or not.
                moves += TradeHandMove(posToMove, false)
                moves += TradeHandMove(posToMove, true)
            }
        }

        return moves
    }

    override fun getRandomMove(): MoveEvent? {
        if (isGameDone) {
            return null
        }

        val player = players[posToMove]
        when (phase) {
            Phase.PLAY -> {
                if (currentTrick.cards.isNotEmpty()) {
                    // Player must follow the suit of first card in trick.
                    val cards = mutableListOf<PCard>()
                    val trickSuit = currentTrick.suit
                    for (card in player.hand.cards) {
                        if (card.suit == trickSuit) {
                            cards += card
                        }
                    }
                    if (cards.isNotEmpty()) {
                        return PlayMove(posToMove, cards.random())
                    }
                }
                // Player plays first or has no cards in required suit, can play any.
                return PlayMove(posToMove, player.hand.cards.random())
            }
            Phase.TRADE -> {
                // Player has the choice to trade his hand with the extra hand or not.
                return TradeHandMove(posToMove, Random.nextBoolean())
            }
        }
    }

    override fun clone() = cloneTo(GameState()).also {
        it.dealerPos = dealerPos
        it.trumpSuit = trumpSuit
        it.extraHand = extraHand.clone()
        it.phase = phase
        it.tricksPlayed += tricksPlayed
        it.currentTrick = currentTrick.clone()
    }

    override fun toString() = "[posToMove: $posToMove, " +
            "tricksPlayed: ${tricksPlayed.size}, phase: $phase, trump: ${if (trumpSuit == NO_TRUMP)
                "none" else PCard.SUIT_STR[trumpSuit].toString()}, currentTrick: $currentTrick]"


    override fun read(json: Json, jsonData: JsonValue) {
        super.read(json, jsonData)
        dealerPos = jsonData.getInt("dealerPos")
        extraHand = json.readValue(jsonData, "extraHand")
        trumpSuit = jsonData.getInt("trumpSuit")
        phase = json.readValue(jsonData, "phase")
        tricksPlayed += json.readArrayValue<ArrayList<Trick>, Trick>(jsonData, "tricksPlayed")
        currentTrick = json.readValue(jsonData, "currentTrick")
    }

    override fun write(json: Json) {
        super.write(json)
        json.writeValue("dealerPos", dealerPos)
        json.writeValue("extraHand", extraHand)
        json.writeValue("trumpSuit", trumpSuit)
        json.writeValue("phase", phase)
        json.writeValue("tricksPlayed", tricksPlayed)
        json.writeValue("currentTrick", currentTrick)
    }


    enum class Phase {
        TRADE, PLAY
    }

    companion object {
        const val CARDS_COUNT = 13
        const val NO_TRUMP = -1
    }

}
