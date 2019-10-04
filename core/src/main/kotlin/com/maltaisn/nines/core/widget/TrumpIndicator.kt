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

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.I18NBundle
import com.maltaisn.cardgame.pcard.PCardStyle
import com.maltaisn.cardgame.widget.FboWidgetGroup
import com.maltaisn.cardgame.widget.ShadowImage
import com.maltaisn.cardgame.widget.action.ActionDelegate
import com.maltaisn.cardgame.widget.action.TimeAction
import com.maltaisn.cardgame.widget.menu.MenuButton
import com.maltaisn.msdfgdx.FontStyle
import com.maltaisn.nines.core.game.GameState
import ktx.actors.alpha
import ktx.style.get


class TrumpIndicator(skin: Skin) : FboWidgetGroup() {

    private val style: TrumpIndicatorStyle = skin.get()
    private val strings: I18NBundle = skin.get()

    var shown = false
        set(value) {
            if (field == value) return
            field = value

            if (transitionAction == null) {
                transitionAction = TransitionAction()
            }
        }

    var icon: Drawable?
        get() = btn.icon
        set(value) {
            btn.icon = value
            if (value == null) {
                iconCell.size(0f).padRight(0f)
            } else {
                iconCell.size(style.iconSize).padRight(30f)
            }
        }

    /**
     * The trump suit shown by the indicator, or [GameState.NO_TRUMP] for none.
     */
    var trumpSuit = 0
        set(value) {
            field = value
            if (value == GameState.NO_TRUMP) {
                icon = null
                btn.title = strings["trump_indicator_title_none"]
            } else {
                icon = style.pcardStyle.suitIcons[value]
                btn.title = strings["trump_indicator_title"]
            }
        }

    private val btn = MenuButton(skin, style.fontStyle, anchorSide = MenuButton.AnchorSide.TOP)
    private val iconCell: Cell<ShadowImage>

    private var transitionAction by ActionDelegate<TransitionAction>()


    init {
        // Do the layout
        addActor(btn)
        btn.apply {
            iconCell = add(iconImage)
            add(titleLabel)
            pad(40f)
            setFillParent(true)
        }

        icon = null
        trumpSuit = GameState.NO_TRUMP
        isVisible = false
    }


    private inner class TransitionAction :
            TimeAction(TRANSITION_DURATION, Interpolation.smooth, reversed = !shown) {

        init {
            isVisible = true
            alpha = if (shown) 0f else 1f
            renderToFrameBuffer = true
        }

        override fun update(progress: Float) {
            reversed = !shown
            btn.y = (1 - progress) * height
            alpha = progress
        }

        override fun end() {
            isVisible = shown
            renderToFrameBuffer = false
            transitionAction = null
            btn.y = 0f
        }
    }


    class TrumpIndicatorStyle {
        lateinit var pcardStyle: PCardStyle
        lateinit var fontStyle: FontStyle
        var iconSize = 0f
    }

    companion object {
        const val TRANSITION_DURATION = 0.3f
    }

}
