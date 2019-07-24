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

import com.maltaisn.cardgame.addClassTag
import com.maltaisn.cardgame.game.CardGameJson
import com.maltaisn.cardgame.game.GameResult
import com.maltaisn.cardgame.game.PCard
import com.maltaisn.cardgame.setSerializer
import com.maltaisn.nines.core.game.event.*


object GameSaveJson : CardGameJson() {

    /**
     * The version of the card game library that the [Game] being currently
     * deserialized was written by.
     */
    var ninesVersion = -1

    init {
        // Add class tags
        addClassTag<GameState>("state")
        addClassTag<GameResult>("result")
        addClassTag<Trick>("trick")
        addClassTag<Hand>("hand")
        addClassTag<StartEvent>("startEvent")
        addClassTag<EndEvent>("endEvent")
        addClassTag<RoundStartEvent>("roundStartEvent")
        addClassTag<RoundEndEvent>("roundEndEvent")
        addClassTag<TradeHandMove>("tradeHandMove")
        addClassTag<PlayMove>("playMove")
        addClassTag<HumanPlayer>("humanPlayer")
        addClassTag<MctsPlayer>("mctsPlayer")

        // Register custom serializers
        setSerializer(PCard.JsonSerializer)
    }

}
