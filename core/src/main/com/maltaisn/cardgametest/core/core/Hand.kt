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

package com.maltaisn.cardgametest.core.core

import com.maltaisn.cardgame.core.Deck
import com.maltaisn.cardgame.core.PCard


/**
 * A player's hand. Each hand have an ID.
 */
class Hand : Deck<PCard> {

    val id: Int

    constructor(id: Int) : super() {
        this.id = id
    }

    constructor(id: Int, cards: Collection<PCard>) : super(cards) {
        this.id = id
    }

    override fun clone() = Hand(id, this)

}