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

import com.maltaisn.cardgame.pcard.PCard
import org.junit.Assert.assertEquals
import org.junit.Test

internal class TrickTest {

    @Test
    fun findTrickWinner() {
        val trick1 = Trick(0)
        trick1.cards += PCard.parseDeck("A♣,5♣,Q♣")
        assertEquals(0, trick1.findWinner(PCard.SPADE))

        val trick2 = Trick(0)
        trick2.cards += PCard.parseDeck("A♣,5♣,5♠")
        assertEquals(2, trick2.findWinner(PCard.SPADE))

        val trick3 = Trick(0)
        trick3.cards += PCard.parseDeck("A♣,Q♠,5♠")
        assertEquals(1, trick3.findWinner(PCard.SPADE))

        val trick4 = Trick(0)
        trick4.cards += PCard.parseDeck("2♣,A♦,A♥")
        assertEquals(0, trick4.findWinner(PCard.SPADE))
    }

}
