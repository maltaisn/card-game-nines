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

package io.github.maltaisn.cardenginetest.core

import io.github.maltaisn.cardengine.core.PCard
import io.github.maltaisn.cardenginetest.core.core.Hand
import io.github.maltaisn.cardenginetest.core.core.HeuristicPlayer
import io.github.maltaisn.cardenginetest.core.core.Trick
import org.junit.Assert.*
import org.junit.Test

internal class HeuristicPlayerTest {

    @Test
    fun clone() {
        val p1 = HeuristicPlayer()
        p1.name = "Heuristic"
        p1.hand = Hand(0, PCard.parseDeck(',', "4♠,5♠,6♠"))

        val p2 = p1.clone()
        assertNotSame(p1, p2)
        assertEquals(p1.name, p2.name)
        assertNotSame(p1.hand, p2.hand)
        assertEquals(p1.hand, p2.hand)
    }

    @Test
    fun getHandScore() {
        val p = HeuristicPlayer()
        p.hand = Hand(0, PCard.parseDeck(',', "A♠,K♠,Q♠,J♠,10♠,9♠"))
        assertEquals(2.80, p.getHandScore(PCard.HEART), 0.01)

        p.hand = Hand(0, PCard.parseDeck(',', "A♠,9♠,8♥,10♥,A♥,2♥,8♦,10♦"))
        assertEquals(3.67, p.getHandScore(PCard.HEART), 0.01)
    }

    @Test
    fun findLowestInShortestSuit() {
        val deck1 = PCard.parseDeck(',', "2♦,3♦,Q♦,K♦,A♦,5♣,8♣")
        assertEquals(PCard.parse("5♣"), HeuristicPlayer.findLowestInShortestSuit(deck1, PCard.SPADE))

        val deck2 = PCard.parseDeck(',', "J♠,10♥,K♣,A♦")
        assertEquals(PCard.parse("J♠"), HeuristicPlayer.findLowestInShortestSuit(deck2, PCard.HEART))

        val deck3 = PCard.parseDeck(',', "A♠,2♠,3♠,4♠,5♠,6♠")
        assertEquals(PCard.parse("2♠"), HeuristicPlayer.findLowestInShortestSuit(deck3, PCard.SPADE))
    }

    @Test
    fun findLowestTrump() {
        val deck1 = PCard.parseDeck(',', "2♦,3♦,Q♦,K♦,A♦")
        assertEquals(PCard.parse("2♦"), HeuristicPlayer.findLowestTrump(deck1, PCard.DIAMOND))

        val deck2 = PCard.parseDeck(',', "J♠,10♥,K♣,A♦")
        assertEquals(PCard.parse("J♠"), HeuristicPlayer.findLowestTrump(deck2, PCard.SPADE))

        val deck3 = PCard.parseDeck(',', "A♠,2♠,3♠,4♠,5♠,6♠")
        assertNull(HeuristicPlayer.findLowestTrump(deck3, PCard.DIAMOND))
    }

    @Test
    fun findLowestToWin() {
        // Player has only required suit.
        val deck1 = PCard.parseDeck(',', "3♥,6♥,9♥,Q♥,A♥")
        val trick1 = Trick(PCard.SPADE, PCard.parseDeck(',', "2♥,A♣"))
        assertEquals(PCard.parse("3♥"), HeuristicPlayer.findLowestToWin(deck1, trick1))

        // Player can only play trump.
        val deck2 = PCard.parseDeck(',', "3♠,4♠,5♠,A♠,2♣")
        val trick2 = Trick(PCard.SPADE, PCard.parseDeck(',', "2♥,A♣"))
        assertEquals(PCard.parse("3♠"), HeuristicPlayer.findLowestToWin(deck2, trick2))

        // Player can't win.
        val deck3 = PCard.parseDeck(',', "3♦,4♦,5♦,A♦,2♣")
        val trick3 = Trick(PCard.SPADE, PCard.parseDeck(',', "2♥,A♣"))
        assertEquals(null, HeuristicPlayer.findLowestToWin(deck3, trick3))
    }
}