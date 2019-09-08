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

import com.maltaisn.cardgame.pcard.PCard
import com.maltaisn.cardgame.prefs.GamePref
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.stats.Statistics
import com.maltaisn.cardgame.widget.menu.DefaultGameMenu
import com.maltaisn.cardgame.widget.table.ScoresTable
import com.maltaisn.cardgame.widget.table.TricksTable
import com.maltaisn.nines.core.game.player.Player
import com.maltaisn.nines.core.widget.HandsTable
import java.text.NumberFormat


interface GameContract {

    interface View {

        val settings: GamePrefs
        val newGameOptions: GamePrefs

        val stats: Statistics

        val numberFormat: NumberFormat
        val extraHandString: String

        fun save()

        fun doDelayed(delay: Float, action: () -> Unit)

        fun completeAnimations()

        // Menu
        fun goToPreviousMenu()

        fun showInGameMenu(saveLast: Boolean)
        fun showScoreboard()
        fun setContinueItemEnabled(enabled: Boolean)

        fun showResetGameDialog(pref: GamePref<*>, callback: (Boolean) -> Unit)

        // Player labels
        fun setPlayerLabelsShown(shown: Boolean)

        fun setPlayerNames(names: List<String>)
        fun setPlayerScores(scores: List<Int>, tricksTaken: List<Int>?)
        fun setPlayerTradeStatus(trade: List<Player.Trade>)

        // Player hand
        fun setPlayerHandShown(shown: Boolean, animate: Boolean = true)

        fun setPlayerHandEnabled(enabled: Boolean)
        fun setPlayerHandCards(cards: List<PCard>)
        fun dealPlayerCards(delay: Float)
        fun moveCardsFromExtraHandToPlayerHand(count: Int)
        fun moveCardsFromExtraHandToHiddenStack(pos: Int, count: Int)
        fun moveCardsFromHiddenStackToExtraHand(pos: Int, count: Int)
        fun highlightPlayerCards(cards: List<PCard>)
        fun unhighlightAllPlayerCards()

        // Extra hand
        fun setExtraHandShown(shown: Boolean, animate: Boolean = true)

        fun setExtraHandCards(cards: List<PCard>)

        // Trick
        fun setTrickShown(shown: Boolean, animate: Boolean = true)

        fun setTrickCards(cards: List<PCard?>)
        fun setTrickStartAngle(angle: Float)

        // Hidden stacks
        fun setHiddenStackCards(pos: Int, cards: List<PCard>)

        fun setHiddenStackCardsShown(pos: Int, shown: Boolean)
        fun movePlayerCardToTrick(pos: Int, card: PCard, duration: Float)
        fun sortPlayerHand()
        fun collectTrick(playerPos: Int, duration: Float)

        // Popups
        var tradePopupShown: Boolean
        var collectPopupShown: Boolean
        var idlePopupShown: Boolean

        fun showIdlePopupAfterDelay(delay: Float)
        fun cancelDelayedIdlePopup()

        // Dealer chip
        fun showDealerChip(pos: Int)
        fun moveDealerChip(pos: Int)
        fun hideDealerChip()

        // Trump indicator
        fun setTrumpIndicatorShown(shown: Boolean)
        fun setTrumpIndicatorSuit(suit: Int)

        // Scoreboard
        fun checkScoreboardScoresPage()

        fun scrollScoresPageToBottom()
        fun setScoresTableHeaders(headers: List<ScoresTable.Header>)
        fun setScoresTableFooters(footers: List<ScoresTable.Score>)
        fun addScoresTableRow(row: List<ScoresTable.Score>)
        fun clearScoresTable()

        fun setTricksPageShown(shown: Boolean)
        fun setTricksTableHeaders(headers: List<String>)
        fun setTricksTableTricks(tricks: List<List<TricksTable.TrickCard>>)

        fun setHandsPageShown(shown: Boolean)
        fun setHandsPageHands(hands: List<HandsTable.PlayerRow>)

        fun setLastTrickPageShown(shown: Boolean)
        fun setLastTrickCards(cards: List<PCard>)
        fun setLastTrickStartAngle(angle: Float)

        fun setScoreboardContinueItemShown(shown: Boolean)

        // Game over dialog
        fun setGameOverDialogShown(shown: Boolean)
        fun setGameOverDialogMessage(name: String, isHuman: Boolean)

        // Sound
        fun playSound(sound: String, volume: Float)
    }

    interface Presenter : DefaultGameMenu.Callback {

        fun attach()
        fun detach()

        fun onSave()

        fun onPrefNeedsConfirm(pref: GamePref<*>, callback: (Boolean) -> Unit)
        fun onPrefConfirmed()

        fun onBackPress()

        fun onTradeBtnClicked(trade: Boolean)
        fun onCollectTrickBtnClicked()

        fun onPlayerCardClicked(card: PCard)

        fun onScoreboardContinueItemClicked()

        fun onAboutRateClicked()
        fun onAboutReportBugClicked()

        fun onGameOverDialogScoresBtnClicked()
        fun onGameOverDialogNewGameBtnClicked()
    }

}
