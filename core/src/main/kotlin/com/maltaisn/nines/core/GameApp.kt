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

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.maltaisn.cardgame.CardGameApp
import ktx.async.KtxAsync
import java.util.*

class GameApp(val listener: GameListener) : CardGameApp() {

    /**
     * The game locale, stored in a preferences file.
     */
    private val locale: Locale
        get() {
            val localeStr = languagePrefs.getString(PrefKeys.LANGUAGE, "auto")
            return if (localeStr == "auto") {
                Locale.getDefault()
            } else {
                val localeParts = localeStr.split('_', limit = 3)
                Locale(localeParts[0], localeParts.getOrNull(1) ?: "",
                        localeParts.getOrNull(2) ?: "")
            }
        }

    val languagePrefs: Preferences by lazy {
        Gdx.app.getPreferences("com.maltaisn.nines")
    }

    override fun create() {
        super.create()

        KtxAsync.initiate()

        restart()
    }

    /**
     * Start or restart the game.
     */
    fun restart() {
        // Kill previous screen
        screen?.apply {
            hide()
            pause()
            dispose()
        }

        // Start game
        setScreen(GameScreen(this, locale))
    }

}
