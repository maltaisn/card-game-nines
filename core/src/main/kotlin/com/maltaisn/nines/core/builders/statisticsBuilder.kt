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
import com.maltaisn.cardgame.stats.Statistics
import com.maltaisn.nines.core.StatsHandler


fun buildStatistics(strings: I18NBundle) =
        Statistics("com.maltaisn.nines.stats") {
            variants = listOf(
                    strings["difficulty_0"],
                    strings["difficulty_1"],
                    strings["difficulty_2"],
                    strings["difficulty_3"])
            number(StatsHandler.GAMES_PLAYED) {
                title = strings["stat_games_played"]
            }
            number(StatsHandler.GAMES_WON) {
                title = strings["stat_games_won"]
            }
            percent("gamesWonPercent") {
                title = strings["stat_games_won_percent"]
                fracStatKey = "gamesWon"
                totalStatKey = "gamesPlayed"
            }
            number(StatsHandler.ROUNDS_PLAYED) {
                title = strings["stat_rounds_played"]
            }
            number(StatsHandler.ROUNDS_PLAYED_COMPLETE) {
                internal = true
            }
            number(StatsHandler.ROUNDS_WON) {
                title = strings["stat_rounds_won"]
            }
            percent("roundsWonPercent") {
                title = strings["stat_rounds_won_percent"]
                fracStatKey = "roundsWon"
                totalStatKey = "roundsPlayed"
            }
            average("averageRoundsPerGame") {
                title = strings["stat_average_rounds_per_game"]
                totalStatKey = "roundsPlayedComplete"
                countStatKey = "gamesPlayed"
            }
            number(StatsHandler.MIN_ROUNDS_IN_GAME) {
                defaultValue = Float.NaN
                title = strings["stat_min_rounds_in_game"]
            }
            number(StatsHandler.MAX_ROUNDS_IN_GAME) {
                defaultValue = Float.NaN
                title = strings["stat_max_rounds_in_game"]
            }
            number(StatsHandler.ROUND_SCORE_TOTAL) {
                internal = true
            }
            average("averageScorePerRound") {
                title = strings["stat_average_score_per_round"]
                totalStatKey = "roundScoreTotal"
                countStatKey = "roundsPlayed"
                precision = 1
            }
            number(StatsHandler.TRICKS_PLAYED) {
                title = strings["stat_tricks_played"]
            }
            number(StatsHandler.TRICKS_WON) {
                title = strings["stat_tricks_won"]
            }
            percent("tricksWonPercent") {
                title = strings["stat_tricks_won_percent"]
                fracStatKey = "tricksWon"
                totalStatKey = "tricksPlayed"
            }
            number(StatsHandler.TRADES_DONE) {
                internal = true
            }
            percent("tradesDonePercent") {
                title = strings["stat_trades_done_percent"]
                fracStatKey = "tradesDone"
                totalStatKey = "roundsPlayed"
            }
        }
