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

import com.maltaisn.cardgame.game.CardGameState
import com.maltaisn.cardgame.game.event.CardGameMove
import com.maltaisn.nines.core.game.GameState


/**
 * A player played by the computer
 */
abstract class AiPlayer : Player() {

    /**
     * Find the best move to play given the current game state.
     * The moves must come from [CardGameState.getMoves] and not be created.
     */
    abstract fun findMove(state: GameState): CardGameMove

}
