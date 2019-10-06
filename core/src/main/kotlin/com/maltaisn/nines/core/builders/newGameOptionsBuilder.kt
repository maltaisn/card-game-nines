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

package com.maltaisn.nines.core.builders

import com.badlogic.gdx.utils.I18NBundle
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.nines.core.PrefKeys


fun buildNewGameOptions(strings: I18NBundle) =
        GamePrefs("com.maltaisn.nines.newGameSettings") {
            slider(PrefKeys.DIFFICULTY) {
                title = strings["pref_difficulty"]
                help = strings["pref_difficulty_help"]
                enumValues = listOf(
                        strings["difficulty_0"],
                        strings["difficulty_1"],
                        strings["difficulty_2"],
                        strings["difficulty_3"])
                minValue = 0f
                maxValue = 3f
                defaultValue = 1f
            }
        }
