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

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.maltaisn.cardgame.utils.FontStyle
import com.maltaisn.nines.core.GameLayout
import com.maltaisn.nines.core.Res
import com.maltaisn.nines.core.widget.HandsTable
import com.maltaisn.nines.core.widget.TrumpIndicator
import ktx.style.add
import ktx.style.get


/**
 * Load Nines styles to core skin.
 */
fun loadSkin(assetManager: AssetManager, coreSkin: Skin) {
    coreSkin.apply {
        // Load atlas regions
        addRegions(assetManager[Res.ATLAS])

        // Add styles
        add(HandsTable.HandsTableStyle(
                headerFontStyle = get("normalBold"),
                playerNameFontStyle = FontStyle {
                    size = 44f
                    color = get("textColorGray")
                }
        ))
        add(TrumpIndicator.TrumpIndicatorStyle(
                pcardStyle = get(),
                fontStyle = FontStyle {
                    weight = 0.1f
                    size = 44f
                    color = get("textColorWhite")
                    shadowColor = get("black")
                },
                iconSize = 64f
        ))
        add(GameLayout.GameLayoutStyle(
                appIcon = get("icon")
        ))
    }
}
