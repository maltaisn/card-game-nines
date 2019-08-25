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

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
import com.maltaisn.cardgame.pcard.PCard
import com.maltaisn.cardgame.pcard.PCardStyle
import com.maltaisn.cardgame.prefs.GamePref
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.stats.Statistics
import com.maltaisn.cardgame.utils.postDelayed
import com.maltaisn.cardgame.widget.*
import com.maltaisn.cardgame.widget.card.*
import com.maltaisn.cardgame.widget.menu.DefaultGameMenu
import com.maltaisn.cardgame.widget.menu.MenuItem
import com.maltaisn.cardgame.widget.menu.PagedSubMenu
import com.maltaisn.cardgame.widget.menu.SubMenu
import com.maltaisn.cardgame.widget.prefs.ResetGameDialog
import com.maltaisn.cardgame.widget.table.ScoresTable
import com.maltaisn.cardgame.widget.table.TableViewContent
import com.maltaisn.cardgame.widget.table.TricksTable
import com.maltaisn.nines.core.game.player.Player
import com.maltaisn.nines.core.widget.HandsTable
import com.maltaisn.nines.core.widget.TrumpIndicator
import ktx.actors.onClick
import ktx.actors.onKeyDown
import ktx.style.get
import java.text.NumberFormat
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


class GameLayout(skin: Skin) : CardGameLayout(skin), GameContract.View {

    private val presenter: GameContract.Presenter = GamePresenter()

    //@GDXAssets(propertiesFiles = ["assets/strings.properties"])
    private val strings: I18NBundle = skin.get()

    override val settings: GamePrefs = skin["settings"]
    override val newGameOptions: GamePrefs = skin["newGameOptions"]

    override val stats: Statistics = skin.get()

    private val menu: DefaultGameMenu
    private val resetGameDialog: ResetGameDialog

    private val hiddenStacks: List<CardStack>
    private val playerHand: CardHand
    private val extraHand: CardHand
    private val trick: CardTrick

    private val playerLabelTable: FadeTable
    private val playerLabels: List<PlayerLabel>

    private val tradePopup: Popup
    private val collectPopup: Popup
    private val idlePopup: Popup

    private val trumpIndicator: TrumpIndicator

    private val dealerChip: DealerChip

    private val scoresPage: PagedSubMenu.Page
    private val scoresTable: ScoresTable

    private val handsPage: PagedSubMenu.Page
    private val handsTable: HandsTable

    private val tricksPage: PagedSubMenu.Page
    private val tricksTable: TricksTable

    private val lastTrickPage: PagedSubMenu.Page
    private val lastTrick: CardTrick

    private val continueItem: MenuItem

    private val gameOverDialog: AlertDialog


    private var idleAction: Action? = null
        set(value) {
            if (value == null) removeAction(field)
            field = value
        }

    override val numberFormat: NumberFormat = NumberFormat.getInstance()

    override val extraHandString: String = strings["scoreboard_hands_extra"]


    init {
        val cardStyle: PCardStyle = skin.get()

        // SCOREBOARD
        // Scores page
        scoresTable = ScoresTable(skin, 3)
        scoresPage = PagedSubMenu.Page(0, strings["scoreboard_scores"],
                skin.getDrawable(CoreIcons.LIST), SubMenu.ITEM_POS_TOP)
        scoresPage.content = Container(scoresTable).pad(60f, 30f, 60f, 30f).fill()

        // Hands page
        handsTable = HandsTable(skin, cardStyle)
        handsPage = PagedSubMenu.Page(1, strings["scoreboard_hands"],
                skin.getDrawable(CoreIcons.CARDS), SubMenu.ITEM_POS_TOP)
        handsPage.content = Container(handsTable).pad(60f, 30f, 60f, 30f).fill()

        // Tricks page
        tricksTable = TricksTable(skin, cardStyle, 3)
        tricksTable.cardSize = CardActor.SIZE_NORMAL
        tricksPage = PagedSubMenu.Page(2, strings["scoreboard_tricks"],
                skin.getDrawable(CoreIcons.CARDS), SubMenu.ITEM_POS_TOP)
        tricksPage.content = Container(tricksTable).pad(60f, 30f, 60f, 30f).fill()

        // Last trick page
        lastTrick = CardTrick(cardStyle, 3).apply {
            cardSize = CardActor.SIZE_BIG
            enabled = false
        }
        lastTrickPage = PagedSubMenu.Page(3, strings["scoreboard_last_trick"],
                skin.getDrawable(CoreIcons.CARDS), SubMenu.ITEM_POS_TOP)
        lastTrickPage.content = Container(TableViewContent(skin).apply {
            add(lastTrick).grow()
        }).pad(60f, 30f, 60f, 30f).fill()

        // Continue item
        continueItem = MenuItem(4, strings["scoreboard_continue"],
                skin.getDrawable(CoreIcons.ARROW_RIGHT), SubMenu.ITEM_POS_BOTTOM)
        continueItem.checkable = false

        // ABOUT MENU
        // About page
        val aboutPage = PagedSubMenu.Page(0, strings["about_menu"],
                skin.getDrawable(CoreIcons.INFO), SubMenu.ITEM_POS_TOP)
        val aboutView = AboutView(skin, strings["app_name"],
                strings["about_version"], strings["about_author"]).apply {
            addButton(strings["about_rate"], skin.getDrawable(CoreIcons.STAR))
                    .onClick(presenter::onAboutRateClicked)
            addButton(strings["about_report_bug"], skin.getDrawable(CoreIcons.ALERT))
                    .onClick(presenter::onAboutReportBugClicked)
        }
        aboutPage.content = aboutView

        // MENU
        menu = DefaultGameMenu(skin)
        menu.callback = presenter
        menu.newGameOptions = newGameOptions
        menu.settings = settings
        menu.rules = skin[Res.MD_RULES]
        menu.stats = stats

        menu.scoreboardMenu.addItems(scoresPage, handsPage,
                tricksPage, lastTrickPage, continueItem)
        menu.scoreboardMenu.itemClickListener = { item ->
            if (item === continueItem) {
                presenter.onScoreboardContinueItemClicked()
            }
        }

        menu.aboutMenu.addItems(aboutPage)
        menu.aboutMenu.checkItem(aboutPage)

        // Confirm dialog, for settings that resets the saved game
        resetGameDialog = ResetGameDialog(skin)
        menu.confirmCallback = presenter::onPrefNeedsConfirm

        addActor(menu)

        // Card containers
        playerHand = CardHand(cardStyle).apply {
            sorter = PCard.DEFAULT_SORTER
            clipPercent = 0.3f
            align = Align.bottom
            cardSize = CardActor.SIZE_NORMAL
            shown = false
            clickListener = { actor, _ -> presenter.onPlayerCardClicked(actor.card as PCard) }
        }

        hiddenStacks = List(3) {
            CardStack(cardStyle).apply {
                cardSize = CardActor.SIZE_NORMAL
                visibility = CardContainer.Visibility.NONE
            }
        }

        extraHand = CardHand(cardStyle).apply {
            sorter = PCard.DEFAULT_SORTER
            visibility = CardContainer.Visibility.NONE
            cardSize = CardActor.SIZE_SMALL
            maxCardSpacing = 60f
            enabled = false
            shown = false
        }

        trick = CardTrick(cardStyle, 3).apply {
            cardSize = CardActor.SIZE_NORMAL
            shown = false
        }

        cardAnimationGroup.register(trick, extraHand, playerHand, *hiddenStacks.toTypedArray())

        // Player labels
        playerLabels = List(3) { PlayerLabel(skin) }

        // Trump indicator
        trumpIndicator = TrumpIndicator(skin)

        // Dealer chip
        dealerChip = DealerChip(skin)
        dealerChip.distance = 0f

        // Do the layout
        bottomTable.add(hiddenStacks[0]).grow()
        leftTable.add(hiddenStacks[1]).grow()
        topTable.add(hiddenStacks[2]).grow()

        playerLabelTable = FadeTable().apply {
            pad(0f, 60f, 330f, 60f)
            add(trumpIndicator).align(Align.topLeft).minWidth(300f).padLeft(200f).expand()
            add(playerLabels[2]).align(Align.topRight).width(240f).expand().pad(60f, 0f, 0f, 300f).row()
            add(playerLabels[1]).align(Align.topLeft).width(240f).expand().row()
            add(playerLabels[0]).align(Align.bottomLeft).width(240f).expand().padLeft(200f)
        }
        val containerTable = Table().apply {
            pad(40f, 120f, 0f, 120f)
            stack(trick, extraHand).grow().row()
            add(playerHand).growX()
        }
        centerTable.stack(playerLabelTable, WidgetGroup(dealerChip), containerTable).grow()

        // Trade hand popup
        tradePopup = Popup(skin)
        popupGroup.addActor(tradePopup)

        val tradeBtn = Button(skin, strings["popup_trade"])
        tradeBtn.onClick { presenter.onTradeBtnClicked(true) }

        val noTradeBtn = Button(skin, strings["popup_no_trade"])
        noTradeBtn.onClick { presenter.onTradeBtnClicked(false) }

        tradePopup.apply {
            add(tradeBtn).minWidth(300f).padRight(10f)
            add(noTradeBtn).minWidth(300f)
        }

        // Collect trick popup
        collectPopup = Popup(skin)
        popupGroup.addActor(collectPopup)

        val collectBtn = Button(skin, strings["popup_ok"])
        collectBtn.onClick { presenter.onCollectTrickBtnClicked() }
        collectPopup.add(collectBtn).minWidth(300f)

        // Idle popup
        idlePopup = Popup(skin)
        popupGroup.addActor(idlePopup)

        val idleBtn = Button(skin, strings["popup_your_turn"])
        idlePopup.add(idleBtn).minWidth(300f)
        idlePopup.touchable = Touchable.disabled

        // Game over dialog
        gameOverDialog = AlertDialog(skin).apply {
            dialogWidth = 800f
            shadowType = Dialog.ShadowType.NONE
            title = strings["game_over"]
            addButton(strings["game_over_scores"]).onClick {
                presenter.onGameOverDialogScoresBtnClicked()
            }
            addButton(strings["game_over_new_game"]).onClick {
                presenter.onGameOverDialogNewGameBtnClicked()
            }
        }

        // Back key listener
        onKeyDown(true) {
            if (it == Input.Keys.BACK || it == Input.Keys.ESCAPE) {
                presenter.onBackPress()
            }
        }
    }


    override fun setStage(stage: Stage?) {
        super.setStage(stage)
        if (stage != null) {
            presenter.attach(this)
        } else {
            presenter.detach()
        }
    }

    override fun save() {
        presenter.onSave()
    }

    override fun doDelayed(delay: Float, action: () -> Unit) {
        postDelayed(delay, action)
    }

    override fun completeAnimations() {
        cardAnimationGroup.clearDelayedMoves()
        cardAnimationGroup.completeAnimation()
        clearActions()
    }

    // Menu
    override fun goToPreviousMenu() = menu.goBack()

    override fun showInGameMenu(saveLast: Boolean) = menu.showMenu(menu.inGameMenu, saveLast)

    override fun showScoreboard() = menu.showMenu(menu.scoreboardMenu)

    override fun setContinueItemEnabled(enabled: Boolean) {
        menu.continueItem.enabled = enabled
    }

    override fun showResetGameDialog(pref: GamePref<*>, callback: (Boolean) -> Unit) {
        resetGameDialog.let {
            it.pref = pref
            it.callback = { keep ->
                if (keep) {
                    // User decided to change the setting, so reset the saved game
                    presenter.onPrefConfirmed()
                }
                callback(keep)
            }
            it.show(stage)
        }
    }

    // Player labels
    override fun setPlayerLabelsShown(shown: Boolean) = playerLabelTable.fade(shown)

    override fun setPlayerNames(names: List<String>) {
        repeat(3) {
            playerLabels[it].name = names[it]
        }
    }

    override fun setPlayerScores(scores: List<Int>, tricksTaken: List<Int>?) {
        repeat(3) {
            var scoreStr = scores[it].toString()
            if (tricksTaken != null) {
                scoreStr += " (${tricksTaken[it]})"
            }
            playerLabels[it].score = scoreStr
        }
    }

    override fun setPlayerTradeStatus(trade: List<Player.Trade>) {
        repeat(3) {
            playerLabels[it].score = when (trade[it]) {
                Player.Trade.TRADE -> strings["player_trade"]
                Player.Trade.NO_TRADE -> strings["player_no_trade"]
                Player.Trade.UNKNOWN -> null
            }
        }
    }

    // Player hand
    override fun setPlayerHandShown(shown: Boolean, animate: Boolean) {
        if (animate) {
            playerHand.slide(shown, CardContainer.Direction.DOWN)
        } else {
            playerHand.shown = shown
        }
    }

    override fun setPlayerHandEnabled(enabled: Boolean) {
        playerHand.enabled = enabled
    }

    override fun setPlayerHandCards(cards: List<PCard>) {
        playerHand.cards = cards
    }

    override fun dealPlayerCards() {
        cardAnimationGroup.deal(hiddenStacks[0], playerHand,
                hiddenStacks[0].size, fromLast = false)
    }

    override fun moveCardsFromExtraHandToPlayerHand(count: Int) {
        moveAllContainerCards(extraHand, playerHand, count)
    }

    override fun moveCardsFromExtraHandToHiddenStack(pos: Int, count: Int) {
        moveAllContainerCards(extraHand, hiddenStacks[pos], count)
    }

    override fun moveCardsFromHiddenStackToExtraHand(pos: Int, count: Int) {
        moveAllContainerCards(hiddenStacks[pos], extraHand, count)
    }

    /**
     * Move all card from a [src] container to a [dst] container.
     * Cards are moved at the end of the destination container, keeping the order.
     */
    private fun moveAllContainerCards(src: CardContainer, dst: CardContainer, count: Int) {
        for (i in 0 until count) {
            cardAnimationGroup.moveCard(src, dst, 0, dst.size)
        }
        if (dst is CardHand) {
            dst.sort()
        }
        cardAnimationGroup.update()
    }

    override fun highlightPlayerCards(cards: List<PCard>) = playerHand.highlightCards(cards)

    override fun unhighlightAllPlayerCards() = playerHand.highlightAllCards(false)

    // Extra hand
    override fun setExtraHandShown(shown: Boolean, animate: Boolean) {
        if (animate) {
            extraHand.fade(shown)
        } else {
            extraHand.shown = shown
        }
    }

    override fun setExtraHandCards(cards: List<PCard>) {
        extraHand.cards = cards
    }

    // Trick
    override fun setTrickShown(shown: Boolean, animate: Boolean) {
        if (animate) {
            trick.fade(shown)
        } else {
            trick.shown = shown
        }
    }

    override fun setTrickCards(cards: List<PCard?>) {
        trick.cards = cards
    }

    override fun setTrickStartAngle(angle: Float) {
        trick.startAngle = angle
    }

    // Hidden stacks
    override fun setHiddenStackCards(pos: Int, cards: List<PCard>) {
        hiddenStacks[pos].cards = cards
    }

    override fun setHiddenStackCardsShown(pos: Int, shown: Boolean) {
        hiddenStacks[pos].visibility = if (shown) {
            CardContainer.Visibility.ALL
        } else {
            CardContainer.Visibility.NONE
        }
    }

    override fun movePlayerCardToTrick(pos: Int, card: PCard) {
        val src = if (pos == 0) playerHand else hiddenStacks[pos]
        cardAnimationGroup.moveCard(src, trick, src.cards.indexOf(card),
                trick.actors.count { it != null }, replaceSrc = false, replaceDst = true)
        cardAnimationGroup.update()
    }

    override fun sortPlayerHand() {
        playerHand.sort()
    }

    override fun collectTrick(playerPos: Int) {
        setHiddenStackCardsShown(playerPos, true)

        for (i in 0 until trick.capacity) {
            cardAnimationGroup.moveCard(trick, hiddenStacks[playerPos], i, 0, replaceSrc = true)
        }
        cardAnimationGroup.update()

        postDelayed(CardAnimationGroup.UPDATE_DURATION) {
            setHiddenStackCardsShown(playerPos, false)
        }
    }

    // Popups
    override var tradePopupShown by PopupShownDelegate(tradePopup)
    override var collectPopupShown by PopupShownDelegate(collectPopup)
    override var idlePopupShown by PopupShownDelegate(idlePopup)

    class PopupShownDelegate(private val popup: Popup) : ReadWriteProperty<GameLayout, Boolean> {
        override fun getValue(thisRef: GameLayout, property: KProperty<*>) = popup.shown

        override fun setValue(thisRef: GameLayout, property: KProperty<*>, value: Boolean) {
            if (value) {
                popup.show(thisRef.playerHand, Popup.Side.ABOVE)
            } else {
                popup.hide()
            }
        }
    }

    override fun showIdlePopupAfterDelay(delay: Float) {
        idleAction = postDelayed(delay) {
            idlePopup.show(playerHand, Popup.Side.ABOVE)
            idleAction = null
        }
    }

    override fun cancelDelayedIdlePopup() {
        idleAction = null
    }

    // Dealer chip
    override fun showDealerChip(pos: Int) {
        dealerChip.show(getDealerChipActor(pos), getDealerChipSide(pos))
    }

    override fun moveDealerChip(pos: Int) {
        dealerChip.moveTo(getDealerChipActor(pos), getDealerChipSide(pos))
    }

    override fun hideDealerChip() = dealerChip.hide()

    private fun getDealerChipActor(pos: Int) = playerLabels.getOrNull(pos) ?: centerTable

    private fun getDealerChipSide(pos: Int) = when (pos) {
        0 -> Align.right
        1 -> Align.right
        2 -> Align.left
        else -> Align.center
    }

    // Trump indicator
    override fun setTrumpIndicatorShown(shown: Boolean) {
        trumpIndicator.isVisible = shown
    }

    override fun setTrumpIndicatorSuit(suit: Int) {
        trumpIndicator.trumpSuit = suit
    }

    // Scores page
    override fun checkScoreboardScoresPage() {
        menu.scoreboardMenu.checkItem(scoresPage)
    }

    override fun scrollScoresPageToBottom() {
        scoresTable.itemScrollView.scrollToBottom()
    }

    override fun setScoresTableHeaders(headers: List<ScoresTable.Header>) {
        scoresTable.headers = headers
    }

    override fun setScoresTableFooters(footers: List<ScoresTable.Score>) {
        scoresTable.footerScores = footers
    }

    override fun addScoresTableRow(row: List<ScoresTable.Score>) {
        scoresTable.scores += row
        scoresTable.cellAdapter?.notifyChanged()
    }

    override fun clearScoresTable() {
        scoresTable.scores.clear()
        scoresTable.cellAdapter?.notifyChanged()
    }

    // Tricks page
    override fun setTricksPageShown(shown: Boolean) {
        tricksPage.shown = shown
    }

    override fun setTricksTableHeaders(headers: List<String>) {
        tricksTable.headers = headers
    }

    override fun setTricksTableTricks(tricks: List<List<TricksTable.TrickCard>>) {
        tricksTable.cards = tricks
    }

    // Hands page
    override fun setHandsPageShown(shown: Boolean) {
        handsPage.shown = shown
    }

    override fun setHandsPageHands(hands: List<HandsTable.PlayerRow>) {
        handsTable.players = hands
    }

    // Last trick page
    override fun setLastTrickPageShown(shown: Boolean) {
        lastTrickPage.shown = shown
    }

    override fun setLastTrickCards(cards: List<PCard>) {
        lastTrick.cards = cards
    }

    override fun setLastTrickStartAngle(angle: Float) {
        lastTrick.startAngle = angle
    }

    override fun setScoreboardContinueItemShown(shown: Boolean) {
        continueItem.shown = shown
    }

    // Game over dialog
    override fun setGameOverDialogShown(shown: Boolean) {
        if (shown) {
            gameOverDialog.show(stage)
        } else {
            gameOverDialog.hide()
        }
    }

    override fun setGameOverDialogMessage(name: String, isHuman: Boolean) {
        gameOverDialog.message = if (isHuman) {
            strings["game_over_message_human"]
        } else {
            strings.format("game_over_message_ai", name)
        }
    }

}
