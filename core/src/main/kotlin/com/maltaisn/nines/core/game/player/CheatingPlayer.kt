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

import com.maltaisn.cardgame.game.ai.Mcts
import com.maltaisn.nines.core.game.GameState

/**
 * A player played by the computer using MCTS algorithm, but that doesn't randomize
 * the state to include only information that the player can know. Since the game state
 * is fully known, the imperfect information game becomes a perfect information game
 * and the player moves will always converge towards the perfect move.
 */
class CheatingPlayer : AiPlayer() {

    override fun findMove(state: GameState) =
            if (state.phase == GameState.Phase.TRADE) {
                // See MctsPlayer#findMove.
                state.getMoves().maxBy { Mcts.simulate(state, it, 500) }!!
            } else {
                Mcts.run(state, 500)
            }

    override fun clone() = cloneTo(CheatingPlayer())

}
