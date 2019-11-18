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

import com.maltaisn.cardgame.pcard.PCard
import com.maltaisn.nines.core.game.GameSaveJson
import com.maltaisn.nines.core.game.Hand
import ktx.json.fromJson
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame


internal class MctsPlayerTest {

    private val player = MctsPlayer(MctsPlayer.Difficulty.ADVANCED).apply {
        score = 13
        initialize(1, Hand(1, listOf(PCard("4♥"), PCard("K♥"), PCard("10♥"))))
        tricksTaken = 9
    }

    @Test
    fun serialization() {
        val json = GameSaveJson.toJson(player)
        val player2: MctsPlayer = GameSaveJson.fromJson(json)
        assertEquals(player, player2)
    }

    @Test
    fun clone() {
        val player2 = player.clone()
        assertNotSame(player, player2)
        assertEquals(player, player2)
    }

}
