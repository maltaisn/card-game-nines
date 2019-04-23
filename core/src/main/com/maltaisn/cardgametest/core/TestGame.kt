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

package com.maltaisn.cardgametest.core

import com.maltaisn.cardgame.CardGame
import com.maltaisn.cardgametest.core.tests.MenuTest

class TestGame : CardGame() {

    override fun create() {
        super.create()

        setScreen(MenuTest(this))
        //setScreen(CardLoopTest(this))
        //setScreen(DealTest(this))
        //setScreen(FontTest(this))
        //setScreen(NullDealTest(this))
        //setScreen(SolitaireTest(this))
        //setScreen(TrickTest(this))
        //setScreen(PrefWidgetsTest(this))
    }

}
