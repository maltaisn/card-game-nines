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

import com.badlogic.gdx.assets.loaders.I18NBundleLoader
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.I18NBundle
import com.maltaisn.cardgame.CardGameScreen
import com.maltaisn.cardgame.markdown.Markdown
import com.maltaisn.cardgame.markdown.MdLoader
import com.maltaisn.cardgame.pcard.PCardRes
import com.maltaisn.cardgame.pcard.loadPCardSkin
import com.maltaisn.cardgame.prefs.ListPref
import com.maltaisn.cardgame.prefs.SwitchPref
import com.maltaisn.cardgame.utils.post
import com.maltaisn.nines.core.builders.buildNewGameOptions
import com.maltaisn.nines.core.builders.buildSettings
import com.maltaisn.nines.core.builders.buildStatistics
import com.maltaisn.nines.core.builders.loadSkin
import ktx.assets.load
import ktx.style.add
import java.util.*

class GameScreen(private val app: GameApp, locale: Locale) :
        CardGameScreen<GameListener>(locale, app.listener) {

    private lateinit var gameLayout: GameLayout


    override fun load() {
        super.load()

        // Load skin atlases
        assetManager.load<TextureAtlas>(PCardRes.ATLAS)
        assetManager.load<TextureAtlas>(Res.ATLAS)

        // Load localized data
        assetManager.load(Res.STRINGS, I18NBundleLoader.I18NBundleParameter(locale))
        assetManager.load(Res.RULES, MdLoader.Parameter(locale = locale))
    }

    override fun start() {
        super.start()

        // Load skins
        loadPCardSkin(assetManager, skin)
        loadSkin(assetManager, skin)

        // Add skin resources
        val strings = assetManager.get<I18NBundle>(Res.STRINGS)
        val settings = buildSettings(strings)
        skin.add(strings)
        skin.add("settings", settings)
        skin.add("newGameOptions", buildNewGameOptions(strings))
        skin.add(buildStatistics(strings))
        skin.add("rules", assetManager.get<Markdown>(Res.RULES))

        // Language preference
        val languagePref = settings[PrefKeys.LANGUAGE] as ListPref
        languagePref.valueListeners += { _, language ->
            // Persist language value and restart app.
            app.languagePrefs.putString(PrefKeys.LANGUAGE, language).flush()
            root.post { app.restart() }
        }

        // Fullscreen preference
        val fullscreenPref = settings[PrefKeys.FULLSCREEN] as SwitchPref
        fullscreenPref.valueListeners += { _, fullscreen ->
            listener.isFullscreen = fullscreen
        }
        listener.isFullscreen = fullscreenPref.value

        // Game layout
        gameLayout = GameLayout(skin, app.listener)
        addActor(gameLayout)
    }

    override fun pause() {
        super.pause()

        if (started) {
            gameLayout.save()
        }
    }

    override fun resume() {
        if (listener.isFullscreen) {
            listener.isFullscreen = true
        }
    }

}
