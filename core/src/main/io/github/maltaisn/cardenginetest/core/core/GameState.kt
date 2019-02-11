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

package io.github.maltaisn.cardenginetest.core.core

import io.github.maltaisn.cardengine.core.BaseGameState
import io.github.maltaisn.cardengine.core.BaseMove
import io.github.maltaisn.cardengine.core.Deck
import io.github.maltaisn.cardengine.core.PCard
import java.util.*
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
class GameState : BaseGameState<Player> {

    /** Game result, `null` if game is not done. */
    private var result: Result? = null

    /** Position of the dealer. */
    private val dealer: Int

    /** The extra hand dealt. */
    private var extraHand: Hand

    /** The trump suit, a PCard constant or [NO_TRUMP]. */
    val trumpSuit: Int

    /** The game phase, either trade or play. */
    var phase: Phase

    /** How many trades took place during the trade phase. */
    var tradesCount: Int
        private set

    /** How many tricks have been played. */
    var tricksPlayed: Int
        private set

    /** The current trick being played. */
    val currentTrick: Trick


    constructor(players: List<Player>, dealer: Int, trumpSuit: Int) : super(players, dealer) {
        this.dealer = dealer
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
        playerToMove = getPlayerNextTo(dealer)
        phase = Phase.TRADE
        tricksPlayed = 0
        currentTrick = Trick(trumpSuit)
    }

    private constructor(state: GameState) : super(state) {
        result = state.result
        dealer = state.dealer
        extraHand = state.extraHand.clone()
        trumpSuit = state.trumpSuit
        phase = state.phase
        tradesCount = state.tradesCount
        tricksPlayed = state.tricksPlayed
        currentTrick = state.currentTrick.clone()
    }

    override fun doMove(move: BaseMove) {
        val player = players[playerToMove]
        when (move) {
            is TradeHandMove -> {
                // Swap player's hand with the extra hand
                if (move.trade) {
                    val temp = player.hand
                    player.hand = extraHand
                    extraHand = temp

                    tradesCount++
                }

                if (playerToMove == dealer) {
                    // Everyone had the chance to trade hands
                    phase = Phase.PLAY
                }

                playerToMove = getPlayerNextTo(playerToMove)
            }
            is PlayMove -> {
                // Play card in trick
                currentTrick += move.card
                player.hand.remove(move.card)

                if (currentTrick.size == 3) {
                    // All players have played, find who takes the trick.
                    val trickWinner = (currentTrick.findHighest() + playerToMove + 1) % 3

                    players[trickWinner].tricksTaken += currentTrick.clone()
                    currentTrick.clear()
                    playerToMove = trickWinner
                    tricksPlayed++

                    if (tricksPlayed == TRICKS_IN_ROUND) {
                        // Round is done, create result.
                        result = Result(List(3) { players[it].tricksTaken.size.toDouble() })
                    }
                } else {
                    playerToMove = getPlayerNextTo(playerToMove)
                }
            }
        }

        for (p in players) {
            p.onMove(this, move)
        }
    }

    override fun getMoves(): MutableList<BaseMove> {
        val moves = ArrayList<BaseMove>()
        if (isGameDone()) return moves

        val player = players[playerToMove]
        when (phase) {
            Phase.PLAY -> {
                if (currentTrick.isNotEmpty()) {
                    // Player must follow the suit of first card in trick.
                    val trickSuit = currentTrick.getSuit()
                    for (card in player.hand) {
                        if (card.suit == trickSuit) {
                            moves += PlayMove(playerToMove, card)
                        }
                    }
                }
                if (moves.isEmpty()) {
                    // Player plays first or has no cards in required suit, can play any.
                    for (card in player.hand) {
                        moves += PlayMove(playerToMove, card)
                    }
                }
            }
            Phase.TRADE -> {
                // Player has the choice to trade his hand with the extra hand or not.
                moves += TradeHandMove(playerToMove, false)
                moves += TradeHandMove(playerToMove, true)
            }
        }

        return moves
    }

    override fun getRandomMove(): BaseMove? {
        if (isGameDone()) return null

        val player = players[playerToMove]
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
                        return PlayMove(playerToMove, cards.random())
                    }
                }
                // Player plays first or has no cards in required suit, can play any.
                return PlayMove(playerToMove, player.hand.random())
            }
            Phase.TRADE -> {
                // Player has the choice to trade his hand with the extra hand or not.
                return TradeHandMove(playerToMove, Random.nextBoolean())
            }
        }
    }

    override fun clone() = GameState(this)

    override fun randomizedClone(observer: Int): GameState {
        val state = clone()
        val player = state.players[observer]
        player as MctsPlayer

        // Create a list of all hands unknown to the observer.
        val hands = ArrayList<Hand>()
        for (p in state.players) {
            if (p.hand.id !in player.knownHands) {
                hands += p.hand
            }
        }
        if (state.extraHand.id !in player.knownHands) {
            hands += state.extraHand
        }

        // Take all cards from these hands and shuffle them.
        val unseen = Deck<PCard>()
        for (hand in hands) {
            unseen += hand
        }
        unseen.shuffle()

        // Sort the unseen cards by suit
        val suitCards = List(4) { Deck<PCard>() }
        for (card in unseen) {
            suitCards[card.suit] += card
        }

        // Redistribute the cards to the unknown hands.
        // If the observer knows the hand doesn't have cards of a suit, don't give any.
        // FIXME
        for (hand in hands) {
            val handSuits = player.handSuits[hand.id]
            val size = hand.size
            hand.clear()
            while (hand.size < size) {
                val cards = suitCards[handSuits.random()]
                if (cards.isNotEmpty()) {
                    hand += cards.drawTop()
                }
            }
        }

        return state
    }

    override fun getResult() = result

    override fun isGameDone() = result != null


    override fun toString() = "playerToMove: $playerToMove, " +
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