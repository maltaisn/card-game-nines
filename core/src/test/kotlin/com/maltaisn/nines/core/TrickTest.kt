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

package com.maltaisn.nines.core

import com.maltaisn.cardgame.core.PCard
import com.maltaisn.nines.core.game.Trick
import org.junit.Assert.assertEquals
import org.junit.Test

internal class TrickTest {

    @Test
    fun findTrickWinner() {
        val trick1 = Trick(PCard.SPADE, PCard.parseDeck("A♣,5♣,Q♣"))
        assertEquals(0, trick1.findHighest())

        val trick2 = Trick(PCard.SPADE, PCard.parseDeck("A♣,5♣,5♠"))
        assertEquals(2, trick2.findHighest())

        val trick3 = Trick(PCard.SPADE, PCard.parseDeck("A♣,Q♠,5♠"))
        assertEquals(1, trick3.findHighest())

        val trick4 = Trick(PCard.SPADE, PCard.parseDeck("2♣,A♦,A♥"))
        assertEquals(0, trick4.findHighest())
    }

}