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

import com.maltaisn.cardgame.CardGameListener


/**
 * Listener to be implemented for each backend to provide backend dependent behavior
 */
interface GameListener : CardGameListener {

    /**
     * Whether there's a way to rate app or not.
     * The rate button in the About menu will only be shown if yes.
     */
    val isRateAppSupported: Boolean

    fun onReportBugClicked()

    fun onRateAppClicked() = Unit

}
