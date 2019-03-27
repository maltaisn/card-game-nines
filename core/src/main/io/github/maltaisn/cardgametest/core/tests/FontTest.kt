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

package io.github.maltaisn.cardgametest.core.tests

import com.badlogic.gdx.graphics.Color
import io.github.maltaisn.cardgame.widget.SdfLabel
import io.github.maltaisn.cardgametest.core.TestGame


class FontTest(game: TestGame) : CardGameTest(game) {

    init {
        val text = "The quick brown fox jumps over a lazy dog."
        //val text = "!\"#\$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz" +
        //        "{|}~\u007F¡¢£¤¥¦§¨©ª«¬\u00AD®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ"

        repeat(10) {
            val label = SdfLabel(text, coreSkin, SdfLabel.FontStyle().apply {
                bold = false
                drawShadow = true
                shadowColor = Color.BLACK
                fontSize = 12f + it * 4f
            })
            gameLayer.centerTable.add(label).expand().center().row()
        }
    }

}