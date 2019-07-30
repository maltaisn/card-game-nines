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

package com.maltaisn.nines.core.widget

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.I18NBundle
import com.maltaisn.cardgame.pcard.PCardStyle
import com.maltaisn.cardgame.widget.menu.MenuButton
import com.maltaisn.cardgame.widget.text.FontStyle
import com.maltaisn.nines.core.game.GameState
import ktx.style.get


class TrumpIndicator(skin: Skin) :
        MenuButton(skin, skin.get<TrumpIndicatorStyle>().fontStyle) {

    private val indicatorStyle: TrumpIndicatorStyle = skin.get()
    private val strings: I18NBundle = skin.get()

    /**
     * The trump suit shown by the indicator, or [GameState.NO_TRUMP] for none.
     */
    var trumpSuit = 0
        set(value) {
            field = value
            if (value == GameState.NO_TRUMP) {
                icon = null
                title = strings["trump_indicator_title_none"]
            } else {
                icon = indicatorStyle.pcardStyle.suitIcons[value]
                title = strings["trump_indicator_title"]
            }
        }


    init {
        anchorSide = Side.TOP
        iconSide = Side.RIGHT
        iconSize = indicatorStyle.iconSize
        pad(40f)

        trumpSuit = GameState.NO_TRUMP
    }


    class TrumpIndicatorStyle {
        lateinit var pcardStyle: PCardStyle
        lateinit var fontStyle: FontStyle
        var iconSize = 0f
    }

}