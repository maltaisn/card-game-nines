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

import com.maltaisn.cardgame.core.*
import com.maltaisn.cardgame.prefs.GamePrefs
import kotlin.random.Random

/**
 * Nines game state for a single round.
 *
 * ## Rules
 * - 4 hands are dealt from a 52-card deck, one for each player and one extra.
 * - The player to the left of the dealer decides if he wants to trade his hand with the extra hand.
 *   The next players can also trade their hand on their turn, even if others already did.
 * - The trump suit goes Hearts, Spades, Diamond, Club, No trump.
 * - Each trick is won by the highest card or the highest trump card if trick was ruffed.
 * - A player must follow the trick's suit whenever possible.
 * - For each trick in excess of 4, score decreases by 1, and for each trick shy of 4,
 *   score increases by 1. First player down to 0 wins.
 *
 * [Wikipedia page](https://en.wikipedia.org/wiki/Nines_(card_game)).
 */
class GameState : CardGameState {

    @Suppress("UNCHECKED_CAST")
    override val players: List<Player>
        get() = super.players as List<Player>

    /** Position of the dealer. */
    val dealerPos: Int

    /** The extra hand dealt. */
    var extraHand: Hand
        private set

    /** The trump suit, a PCard suit constant or [NO_TRUMP]. */
    val trumpSuit: Int

    /** The current game phase. */
    var phase: Phase
        private set

    /** How many trades took place during the trade phase. */
    var tradesCount: Int
        private set

    /** How many tricks have been played. */
    var tricksPlayed: Int
        private set

    /** The current trick being played. */
    val currentTrick: Trick

    override var result: GameResult? = null
        private set


    constructor(settings: GamePrefs, players: List<Player>,
                dealer: Int, trumpSuit: Int) : super(settings, players, dealer) {
        this.dealerPos = dealer
        this.trumpSuit = trumpSuit

        // Create and shuffle a 52-card deck.
        val deck = PCard.fullDeck(false)
        deck.shuffle()

        // Deal 13 cards to each player + extra hand, assigning an ID to each hand.
        var id = 0
        for (player in players) {
            player.initialize(id, Hand(id, deck.drawTop(TRICKS_IN_ROUND)))
            id++
        }
        extraHand = Hand(id, deck.drawTop(TRICKS_IN_ROUND))

        tradesCount = 0
        posToMove = getPositionNextTo(dealer)
        phase = Phase.TRADE
        tricksPlayed = 0
        currentTrick = Trick(trumpSuit)
    }

    private constructor(state: GameState) : super(state) {
        result = state.result
        dealerPos = state.dealerPos
        extraHand = state.extraHand.clone()
        trumpSuit = state.trumpSuit
        phase = state.phase
        tradesCount = state.tradesCount
        tricksPlayed = state.tricksPlayed
        currentTrick = state.currentTrick.clone()
    }


    override fun doMove(move: GameEvent.Move) {
        val player = players[posToMove]
        when (move) {
            is TradeHandMove -> {
                // Swap player's hand with the extra hand
                if (move.trade) {
                    val temp = player.hand
                    player.hand = extraHand
                    extraHand = temp

                    tradesCount++
                }

                if (posToMove == dealerPos) {
                    // Everyone had the chance to trade hands
                    phase = Phase.PLAY
                }

                posToMove = getPositionNextTo(posToMove)
            }
            is PlayMove -> {
                // Play card in trick
                currentTrick += move.card
                player.hand.remove(move.card)

                if (currentTrick.size == 3) {
                    // All players have played, find who takes the trick.
                    val trickWinner = (currentTrick.findHighest() + posToMove + 1) % 3

                    players[trickWinner].tricksTaken += currentTrick.clone()
                    currentTrick.clear()
                    posToMove = trickWinner
                    tricksPlayed++

                    if (tricksPlayed == TRICKS_IN_ROUND) {
                        // Round is done, create result.
                        result = GameResult(List(3) { players[it].tricksTaken.size.toFloat() })
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

    override fun getMoves(): MutableList<GameEvent.Move> {
        val moves = mutableListOf<GameEvent.Move>()
        if (isGameDone) {
            return moves
        }

        val player = players[posToMove]
        when (phase) {
            Phase.PLAY -> {
                if (currentTrick.isNotEmpty()) {
                    // Player must follow the suit of first card in trick.
                    val trickSuit = currentTrick.getSuit()
                    for (card in player.hand) {
                        if (card.suit == trickSuit) {
                            moves += PlayMove(posToMove, card)
                        }
                    }
                }
                if (moves.isEmpty()) {
                    // Player plays first or has no cards in required suit, can play any.
                    for (card in player.hand) {
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

    override fun getRandomMove(): GameEvent.Move? {
        if (isGameDone) {
            return null
        }

        val player = players[posToMove]
        when (phase) {
            Phase.PLAY -> {
                if (currentTrick.isNotEmpty()) {
                    // Player must follow the suit of first card in trick.
                    val cards = Deck<PCard>()
                    val trickSuit = currentTrick.getSuit()
                    for (card in player.hand) {
                        if (card.suit == trickSuit) {
                            cards += card
                        }
                    }
                    if (cards.isNotEmpty()) {
                        return PlayMove(posToMove, cards.random())
                    }
                }
                // Player plays first or has no cards in required suit, can play any.
                return PlayMove(posToMove, player.hand.random())
            }
            Phase.TRADE -> {
                // Player has the choice to trade his hand with the extra hand or not.
                return TradeHandMove(posToMove, Random.nextBoolean())
            }
        }
    }

    override fun clone() = GameState(this)

    override fun randomizedClone(observer: Int): GameState {
        val state = clone()
        val player = state.players[observer]
        (player as MctsPlayer).randomizeState(state)
        return state
    }

    override fun toString() = "posToMove: $posToMove, " +
            "tricksPlayed: $tricksPlayed, phase: $phase, trump: ${if (trumpSuit == NO_TRUMP)
                "none" else PCard.SUIT_STR[trumpSuit].toString()}, currentTrick: $currentTrick]"

    enum class Phase {
        TRADE, PLAY
    }

    companion object {
        const val TRICKS_IN_ROUND = 13
        const val NO_TRUMP = -1
    }

}