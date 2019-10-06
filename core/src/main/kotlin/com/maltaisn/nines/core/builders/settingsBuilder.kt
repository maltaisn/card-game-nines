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
import com.maltaisn.cardgame.widget.CoreIcons
import com.maltaisn.nines.core.PrefKeys


fun buildSettings(strings: I18NBundle) =
        GamePrefs("com.maltaisn.nines.settings") {
            category("interface_category") {
                title = strings["pref_catg_interface"]
                icon = CoreIcons.LIST
                list("language") {
                    title = strings["pref_language"]
                    entries += mapOf(
                            "auto" to strings["pref_language_auto"],
                            "en" to "English",
                            "fr" to "Français")
                    defaultValue = "auto"
                }
                switch(PrefKeys.ENABLE_SOUND) {
                    title = strings["pref_enable_sound"]
                    defaultValue = true
                }
                switch(PrefKeys.FULLSCREEN) {
                    title = strings["pref_fullscreen"]
                    defaultValue = false
                }
                playerNames(PrefKeys.PLAYER_NAMES) {
                    title = strings["pref_player_names"]
                    inputTitle = strings["pref_player_names_input"]
                    maxLength = 15
                    defaultValue = arrayOf(
                            strings["pref_player_names_south"],
                            strings["pref_player_names_west"],
                            strings["pref_player_names_north"])
                }
                list(PrefKeys.GAME_SPEED) {
                    title = strings["pref_game_speed"]
                    entries += mapOf(
                            "slow" to strings["pref_game_speed_slow"],
                            "normal" to strings["pref_game_speed_normal"],
                            "fast" to strings["pref_game_speed_fast"],
                            "very_fast" to strings["pref_game_speed_very_fast"])
                    defaultValue = "normal"
                }
                switch(PrefKeys.CARD_DEAL_ANIMATION) {
                    title = strings["pref_card_deal_animation"]
                    defaultValue = false
                }
                switch(PrefKeys.AUTO_PLAY) {
                    title = strings["pref_auto_play"]
                    help = strings["pref_auto_play_help"]
                    defaultValue = false
                }
                switch(PrefKeys.AUTO_COLLECT) {
                    title = strings["pref_auto_collect"]
                    shortTitle = strings["pref_auto_collect_short"]
                    help = strings["pref_auto_collect_help"]
                    defaultValue = true
                }
                switch(PrefKeys.SELECT_PLAYABLE) {
                    title = strings["pref_select_playable"]
                    shortTitle = strings["pref_select_playable_short"]
                    help = strings["pref_select_playable_help"]
                    defaultValue = true
                }
                switch(PrefKeys.DISPLAY_SCORE) {
                    title = strings["pref_display_score"]
                    defaultValue = true
                }
                switch(PrefKeys.REORDER_HAND) {
                    title = strings["pref_reorder_hand"]
                    shortTitle = strings["pref_reorder_hand_short"]
                    help = strings["pref_reorder_hand_help"]
                    defaultValue = true
                }
            }
            category("game_category") {
                title = strings["pref_catg_game"]
                icon = CoreIcons.CARDS
                slider(PrefKeys.START_SCORE) {
                    title = strings["pref_start_score"]
                    minValue = 4f
                    maxValue = 30f
                    defaultValue = 9f
                    confirmChanges = true
                }
            }
        }
