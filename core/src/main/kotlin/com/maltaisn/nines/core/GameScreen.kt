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
import com.maltaisn.cardgame.prefs.GamePref
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.prefs.GamePrefsLoader
import com.maltaisn.cardgame.prefs.ListPref
import com.maltaisn.cardgame.stats.Statistics
import com.maltaisn.cardgame.stats.StatsLoader
import com.maltaisn.cardgame.utils.post
import ktx.assets.load
import java.util.*

class GameScreen(private val app: GameApp, locale: Locale) :
        CardGameScreen<GameListener>(locale, app.listener) {

    private lateinit var gameLayout: GameLayout

    private lateinit var languagePref: ListPref


    override fun load() {
        super.load()

        // Load skins
        assetManager.load<TextureAtlas>(PCardRes.ATLAS)
        assetManager.load<TextureAtlas>(Res.ATLAS)

        // Load localized data
        assetManager.load(Res.STRINGS_BUNDLE, I18NBundleLoader.I18NBundleParameter(locale))
        assetManager.load(Res.PREFS_NEW_GAME, GamePrefsLoader.Parameter(
                locale = locale, bundlePath = Res.STRINGS_BUNDLE))
        assetManager.load(Res.PREFS_SETTINGS, GamePrefsLoader.Parameter(
                locale = locale, bundlePath = Res.STRINGS_BUNDLE))
        assetManager.load(Res.MD_RULES, MdLoader.Parameter(locale = locale))
        assetManager.load(Res.STATS, StatsLoader.Parameter(
                locale = locale, bundlePath = Res.STRINGS_BUNDLE))
    }

    override fun start() {
        super.start()

        addSkin(PCardRes.SKIN, PCardRes.ATLAS)
        addSkin(Res.SKIN, Res.ATLAS)

        val settings = assetManager.get<GamePrefs>(Res.PREFS_SETTINGS)
        skin.add("settings", settings)
        skin.add("newGameOptions", assetManager.get<GamePrefs>(Res.PREFS_NEW_GAME))
        skin.add("rules", assetManager.get<Markdown>(Res.MD_RULES))
        skin.add("default", assetManager.get<Statistics>(Res.STATS))
        skin.add("default", assetManager.get<I18NBundle>(Res.STRINGS_BUNDLE))

        gameLayout = GameLayout(skin, app.listener)
        addActor(gameLayout)

        languagePref = settings[PrefKeys.LANGUAGE] as ListPref
        languagePref.valueListeners += ::onLanguageChanged
    }

    private fun onLanguageChanged(pref: GamePref<String?>, value: String?) {
        // Persist language value and restart app.
        app.languagePrefs.putString(PrefKeys.LANGUAGE, value).flush()
        root.post { app.restart() }
    }

    override fun pause() {
        super.pause()

        if (started) {
            gameLayout.save()
        }
    }

    override fun dispose() {
        super.dispose()
        languagePref.valueListeners -= ::onLanguageChanged
    }

}
