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

import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
import com.maltaisn.cardgame.game.PCard
import com.maltaisn.cardgame.game.sortWith
import com.maltaisn.cardgame.postDelayed
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.prefs.PlayerNamesPref
import com.maltaisn.cardgame.prefs.PrefEntry
import com.maltaisn.cardgame.prefs.SliderPref
import com.maltaisn.cardgame.widget.*
import com.maltaisn.cardgame.widget.card.*
import com.maltaisn.cardgame.widget.menu.DefaultGameMenu
import com.maltaisn.cardgame.widget.menu.MenuIcons
import com.maltaisn.cardgame.widget.menu.PagedSubMenu
import com.maltaisn.cardgame.widget.menu.SubMenu
import com.maltaisn.cardgame.widget.menu.table.ScoresTable
import com.maltaisn.cardgame.widget.menu.table.TableViewContent
import com.maltaisn.cardgame.widget.menu.table.TricksTable
import com.maltaisn.nines.core.game.*
import com.maltaisn.nines.core.game.MctsPlayer.Difficulty
import com.maltaisn.nines.core.game.event.*
import com.maltaisn.nines.core.widget.HandsTable
import ktx.actors.onClick
import ktx.style.get
import java.text.NumberFormat
import java.util.*
import kotlin.math.PI


class GameLayout(coreSkin: Skin, cardSkin: Skin) : CardGameLayout(coreSkin) {

    var game: Game? = null
        private set

    //@GDXAssets(propertiesFiles = ["assets/strings.properties"])
    private val strings: I18NBundle = coreSkin.get()

    private val newGameOptions: GamePrefs = coreSkin["newGameOptions"]
    private val settings: GamePrefs = coreSkin["settings"]

    private val namesPref = settings[PrefKeys.PLAYER_NAMES] as PlayerNamesPref

    private val menu: DefaultGameMenu

    private val hiddenStacks: List<CardStack>
    private val playerHand: CardHand
    private val extraHand: CardHand
    private val trick: CardTrick

    private val playerLabelTable: FadeTable
    private val playerLabels: List<PlayerLabel>

    private val tradePopup: Popup
    private val collectPopup: Popup
    private val idlePopup: Popup

    private val scoresPage: PagedSubMenu.Page
    private val scoresTable: ScoresTable

    private val handsPage: PagedSubMenu.Page
    private val handsTable: HandsTable

    private val tricksPage: PagedSubMenu.Page
    private val tricksTable: TricksTable

    private val lastTrickPage: PagedSubMenu.Page
    private val lastTrick: CardTrick

    private var idleAction: Action? = null
        set(value) {
            if (value == null) removeAction(field)
            field = value
        }

    private val numberFormat = NumberFormat.getInstance()

    init {
        // SCOREBOARD
        // Scores page
        scoresTable = ScoresTable(coreSkin, 3)
        scoresPage = PagedSubMenu.Page(0, strings["scoreboard_scores"],
                coreSkin.getDrawable(MenuIcons.LIST), SubMenu.ITEM_POS_TOP)
        scoresPage.content = Container(scoresTable).pad(30f, 15f, 30f, 15f).fill()

        // Hands page
        handsTable = HandsTable(coreSkin, cardSkin)
        handsPage = PagedSubMenu.Page(1, strings["scoreboard_hands"],
                coreSkin.getDrawable(MenuIcons.CARDS), SubMenu.ITEM_POS_TOP)
        handsPage.content = Container(handsTable).pad(30f, 15f, 30f, 15f).fill()

        // Tricks page
        tricksTable = TricksTable(coreSkin, cardSkin, 3)
        tricksPage = PagedSubMenu.Page(2, strings["scoreboard_tricks"],
                coreSkin.getDrawable(MenuIcons.CARDS), SubMenu.ITEM_POS_TOP)
        tricksPage.content = Container(tricksTable).pad(30f, 15f, 30f, 15f).fill()

        // Last trick page
        lastTrick = CardTrick(coreSkin, cardSkin, 3).apply {
            cardSize = CardActor.SIZE_BIG
            enabled = false
        }
        lastTrickPage = PagedSubMenu.Page(3, strings["scoreboard_last_trick"],
                coreSkin.getDrawable(MenuIcons.CARDS), SubMenu.ITEM_POS_TOP)
        lastTrickPage.content = Container(TableViewContent(coreSkin).apply {
            add(lastTrick).grow()
        }).pad(30f, 15f, 30f, 15f).fill()

        // MENU
        menu = object : DefaultGameMenu(coreSkin) {
            override fun onContinueClicked() {
                Game.load(this@GameLayout.settings, GameSaveJson) {
                    if (it != null) {
                        super.onContinueClicked()
                        initGame(it)
                    } else {
                        // Could not load save file, delete it and disable continue
                        Game.GAME_SAVE_FILE.delete()
                        continueItem.enabled = false
                    }
                }
            }

            override fun onStartGameClicked() {
                super.onStartGameClicked()

                val difficulty = when (this@GameLayout.newGameOptions.getInt(PrefKeys.DIFFICULTY)) {
                    0 -> Difficulty.BEGINNER
                    1 -> Difficulty.INTERMEDIATE
                    2 -> Difficulty.ADVANCED
                    3 -> Difficulty.EXPERT
                    else -> error("Unknown difficulty level.")
                }

                // Create players
                //val south = HumanPlayer()
                val south = MctsPlayer(difficulty)
                val east = MctsPlayer(difficulty)
                val north = MctsPlayer(difficulty)

                val game = Game(this@GameLayout.settings, south, east, north)
                initGame(game)
                game.start()
            }

            override fun onExitGameClicked() {
                super.onExitGameClicked()

                game?.save(GameSaveJson)
                hide()
                continueItem.enabled = true
            }

            override fun onScoreboardOpened() {
                super.onScoreboardOpened()

                hide()
                scoresPage.checked = true
            }

            override fun onScoreboardClosed() {
                super.onScoreboardClosed()

                val game = game!!
                initGame(game)
                if (game.phase == Game.Phase.GAME_STARTED) {
                    // Last round has ended, start a new one.
                    game.startRound()
                }
            }
        }
        menu.continueItem.enabled = Game.hasSavedGame
        menu.newGameOptions = newGameOptions
        menu.settings = settings
        menu.rules = coreSkin[Res.MD_RULES]

        menu.scoreboardMenu.apply {
            addItem(scoresPage)
            addItem(handsPage)
            addItem(tricksPage)
            addItem(lastTrickPage)
        }

        addActor(menu)

        // Card containers
        playerHand = CardHand(coreSkin, cardSkin).apply {
            sorter = PCard.DEFAULT_SORTER
            clipPercent = 0.3f
            align = Align.bottom
            cardSize = CardActor.SIZE_NORMAL
            shown = false
        }

        hiddenStacks = List(3) {
            CardStack(coreSkin, cardSkin).apply {
                visibility = CardContainer.Visibility.NONE
            }
        }

        extraHand = CardHand(coreSkin, cardSkin).apply {
            sorter = PCard.DEFAULT_SORTER
            visibility = CardContainer.Visibility.NONE
            cardSize = CardActor.SIZE_SMALL
            maxCardSpacing = 30f
            enabled = false
            shown = false
        }

        trick = CardTrick(coreSkin, cardSkin, 3).apply {
            cardSize = CardActor.SIZE_NORMAL
            shown = false
        }

        cardAnimationLayer.register(trick, extraHand, playerHand, *hiddenStacks.toTypedArray())

        // Player labels
        playerLabels = List(3) { PlayerLabel(coreSkin) }

        // Do the layout
        gameLayer.apply {
            bottomTable.add(hiddenStacks[0]).grow()
            leftTable.add(hiddenStacks[1]).grow()
            topTable.add(hiddenStacks[2]).grow()

            playerLabelTable = FadeTable().apply {
                pad(40f, 30f, 140f, 30f)
                add(playerLabels[2]).align(Align.topRight).width(120f).expand().padRight(150f).row()
                add(playerLabels[1]).align(Align.topLeft).width(120f).expand().row()
                add(playerLabels[0]).align(Align.bottomLeft).width(120f).expand().padLeft(100f)
            }
            val containerTable = Table().apply {
                add(Stack(trick, extraHand)).grow().row()
                add(playerHand).growX()
            }
            centerTable.add(Stack(playerLabelTable, containerTable)).grow()
        }

        // Trade hand popup
        tradePopup = Popup(coreSkin)
        popupGroup.addActor(tradePopup)

        val tradeBtn = PopupButton(coreSkin, strings["popup_trade"])
        tradeBtn.onClick {
            val game = game!!
            game.doMove(game.state?.getMoves()?.find { (it as TradeHandMove).trade }!!)
            tradePopup.hide()
        }

        val noTradeBtn = PopupButton(coreSkin, strings["popup_no_trade"])
        noTradeBtn.onClick {
            val game = game!!
            game.doMove(game.state?.getMoves()?.find { !(it as TradeHandMove).trade }!!)
            tradePopup.hide()
        }

        tradePopup.apply {
            add(tradeBtn).minWidth(150f)
            add(noTradeBtn).minWidth(150f)
        }

        // Collect trick popup
        collectPopup = Popup(coreSkin)
        popupGroup.addActor(collectPopup)

        val collectBtn = PopupButton(coreSkin, strings["popup_ok"])
        collectBtn.onClick {
            val moveDuration = collectTrick(hiddenStacks[0], 0f)
            collectPopup.hide()

            postDelayed(moveDuration) {
                (game as Game).playNext()
            }
        }
        collectPopup.add(collectBtn).minWidth(150f)

        // Idle popup
        idlePopup = Popup(coreSkin)
        popupGroup.addActor(idlePopup)

        val idleBtn = PopupButton(coreSkin, strings["popup_your_turn"])
        idlePopup.add(idleBtn).minWidth(150f)
        idlePopup.touchable = Touchable.disabled
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)
        if (stage == null) {
            settings.removeListener(this)
            game?.dispose()
            game = null
        } else {
            settings.addListener(this)
        }
    }

    fun initGame(game: Game) {
        if (game !== this.game) {
            this.game?.dispose()
            this.game = game
        }

        game.eventListener = {
            when (it) {
                is StartEvent -> startGame()
                is EndEvent -> endGame()
                is RoundStartEvent -> startRound()
                is RoundEndEvent -> endRound(it)
                is MoveEvent -> doMove(it)
            }
        }

        cardAnimationLayer.clearDelayedMoves()
        cardAnimationLayer.completeAnimation()

        playerHand.enabled = (game.players[0] is HumanPlayer)

        // Update player names displayed everywhere
        // This also calls updateHandsPage()
        updatePlayerNames()

        // Update scores page
        scoresTable.scores.clear()
        for (event in game.events) {
            if (event is RoundEndEvent) {
                addScoresTableRow(event)
            }
        }
        updateTotalScoreFooters()

        updateTricksPage()

        when (game.phase) {
            Game.Phase.ENDED, Game.Phase.GAME_STARTED -> {
                // Round wasn't started yet or has just ended. Hide all containers.
                hide()
            }
            Game.Phase.ROUND_STARTED -> {
                // Round has started
                val state = game.state!!
                playerLabelTable.fade(true)

                if (state.phase == GameState.Phase.TRADE) {
                    // Trade phase: show extra hand
                    extraHand.apply {
                        cards = state.extraHand.cards
                        fade(true)
                    }
                    game.tradePhaseEnded = false

                } else {
                    // Play phase: show and adjust trick
                    trick.apply {
                        cards = List(3) { state.currentTrick.cards.getOrNull(it) }
                        fade(true)
                    }
                    trick.startAngle = getTrickStartAngle(state.currentTrick.startPos)

                    updateLastTrickPage()

                    // Immediately show idle popup if it's a human player's turn
                    if (state.posToMove == 0 && game.players[0] is HumanPlayer) {
                        idlePopup.show(playerHand, Popup.Side.ABOVE)
                    }

                    game.tradePhaseEnded = true
                }

                playerHand.apply {
                    cards = state.players[0].hand.cards
                    slide(true, CardContainer.Direction.DOWN)
                }
                for (i in 1..2) {
                    hiddenStacks[i].cards = state.players[i].hand.cards
                }

                // NOTE: taken tricks cards are not put back in the hidden stack since they're not used

                updatePlayerScores()

                game.playNext()
            }
        }
    }

    /**
     * Hide the game layout with an animation, but not the menu.
     * [initGame] must be called to show the game back.
     */
    fun hide() {
        // Clear all animations
        cardAnimationLayer.clearDelayedMoves()
        cardAnimationLayer.completeAnimation()
        clearActions()

        // Hide all containers and all popups
        playerHand.highlightAllCards(false)
        playerHand.slide(false, CardContainer.Direction.DOWN)
        playerHand.clickListener = null

        extraHand.fade(false)
        trick.fade(false)

        playerLabelTable.fade(false)

        tradePopup.hide()
        collectPopup.hide()

        idlePopup.hide()
        idleAction = null

        game?.cancelAiTurn()
    }

    private fun startGame() {
        updateTotalScoreFooters()
        game?.startRound()
    }

    private fun endGame() {
        // TODO Show game over dialog
    }

    private fun startRound() {
        val game = game!!
        val state = game.state!!

        var moveDuration = 0.5f

        game.tradePhaseEnded = false

        // Hide previous round scoreboard pages
        handsPage.shown = false
        tricksPage.shown = false
        lastTrickPage.shown = false

        // Set player hand
        val playerCards = game.players[0].hand.cards.toMutableList()
        playerCards.sortWith(PCard.DEFAULT_SORTER)

        val hiddenStack = hiddenStacks[0]
        if (settings.getBoolean(PrefKeys.CARD_DEAL_ANIMATION)) {
            hiddenStack.cards = playerCards
            playerHand.cards = emptyList()
            playerHand.shown = true

            postDelayed(moveDuration) {
                cardAnimationLayer.deal(hiddenStack, playerHand, playerCards.size, fromLast = false)
            }
            moveDuration += CardAnimationLayer.DEAL_DELAY * playerCards.size

        } else {
            hiddenStack.cards = emptyList()
            playerHand.cards = playerCards
            playerHand.slide(true, CardContainer.Direction.DOWN)
            moveDuration += CardContainer.TRANSITION_DURATION
        }

        // Set cards in containers
        extraHand.apply {
            cards = state.extraHand.cards
            fade(true)
        }
        for (i in 1..2) {
            hiddenStacks[i].cards = state.players[i].hand.cards
        }
        trick.cards = List(3) { null }

        playerLabelTable.fade(true)
        updatePlayerScores()

        // Start playing
        moveDuration += 0.5f
        postDelayed(moveDuration) {
            game.playNext()
        }
    }

    private fun endRound(event: RoundEndEvent) {
        // Update scoreboard
        addScoresTableRow(event)
        updateTotalScoreFooters()
        updateHandsPage()
        updateTricksPage()
        updateLastTrickPage()

        // Show scoreboard after a small delay
        postDelayed(1f) {
            menu.showScoreboard()
        }
    }

    private fun doMove(move: MoveEvent) {
        val game = game!!
        val state = game.state!!

        val isSouth = move.playerPos == 0

        var moveDuration = 0f
        when (move) {
            is TradeHandMove -> {
                updatePlayerScores()

                if (move.trade) {
                    // Do swap hand animation
                    val hiddenStack = hiddenStacks[move.playerPos]

                    if (isSouth) {
                        // Hide player hand
                        playerHand.slide(false, CardContainer.Direction.DOWN)
                        moveDuration += CardContainer.TRANSITION_DURATION

                        moveDuration += 0.1f
                        postDelayed(moveDuration) {
                            // Move player hand cards to hidden stack
                            hiddenStack.cards = playerHand.cards
                            playerHand.cards = emptyList()
                            playerHand.shown = true

                            // Move cards from extra hand to player hand
                            for (i in 0 until GameState.CARDS_COUNT) {
                                cardAnimationLayer.moveCard(extraHand, playerHand, 0, 0)
                            }
                            playerHand.sort()
                            cardAnimationLayer.update()
                        }
                        moveDuration += CardAnimationLayer.UPDATE_DURATION

                        // Move cards from hidden stack to extra hand
                        moveDuration += 0.1f
                        postDelayed(moveDuration) {
                            for (i in 0 until GameState.CARDS_COUNT) {
                                cardAnimationLayer.moveCard(hiddenStack, extraHand, 0, 0)
                            }
                            cardAnimationLayer.update()
                        }
                        moveDuration += CardAnimationLayer.UPDATE_DURATION

                    } else {
                        // Move cards from extra hand to hidden stack
                        for (i in 0 until GameState.CARDS_COUNT) {
                            cardAnimationLayer.moveCard(extraHand, hiddenStack, 0, hiddenStack.size)
                        }
                        cardAnimationLayer.update()
                        moveDuration += CardAnimationLayer.UPDATE_DURATION

                        // Move cards from hidden stack to extra hand
                        moveDuration += 0.1f
                        postDelayed(moveDuration) {
                            for (i in 0 until GameState.CARDS_COUNT) {
                                cardAnimationLayer.moveCard(hiddenStack, extraHand, 0, 0)
                            }
                            cardAnimationLayer.update()
                        }
                        moveDuration += CardAnimationLayer.UPDATE_DURATION
                    }
                } else {
                    moveDuration = 0f
                }

                // If this player had the last choice
                if ((move.playerPos + 1) % 3 == game.dealerPos) {
                    game.tradePhaseEnded = true

                    // Hide extra hand
                    moveDuration += game.gameSpeedDelay + 0.5f
                    postDelayed(moveDuration) {
                        extraHand.fade(false)
                        trick.shown = true
                    }
                    moveDuration += CardContainer.TRANSITION_DURATION

                    moveDuration += 0.1f
                    postDelayed(moveDuration) {
                        updatePlayerScores()
                    }
                }
            }
            is PlayMove -> {
                // Move card from player hand to the trick
                val src = if (isSouth) playerHand else hiddenStacks[move.playerPos]
                cardAnimationLayer.moveCard(src, trick, src.cards.indexOf(move.card),
                        trick.actors.count { it != null }, replaceSrc = false, replaceDst = true)
                cardAnimationLayer.update()
                moveDuration = CardAnimationLayer.UPDATE_DURATION

                if (isSouth && settings.getBoolean(PrefKeys.SELECT_PLAYABLE)) {
                    // Unhighlight cards in case they were highlighted before move
                    moveDuration += 0.1f
                    postDelayed(moveDuration) {
                        playerHand.highlightAllCards(false)
                    }
                    moveDuration += CardHand.HIGHLIGHT_DURATION
                }

                when (state.currentTrick.cards.size) {
                    0 -> {
                        // Player plays last
                        moveDuration = if (settings.getBoolean(PrefKeys.AUTO_COLLECT)) {
                            // Collect the trick automatically.
                            collectTrick(hiddenStacks[state.posToMove], moveDuration + 1f)

                        } else {
                            postDelayed(moveDuration + 0.2f) {
                                collectPopup.show(playerHand, Popup.Side.ABOVE)
                            }
                            Float.POSITIVE_INFINITY
                        }
                    }
                    1 -> {
                        // Player plays first: adjust trick start angle
                        trick.startAngle = getTrickStartAngle(move.playerPos)
                    }
                }
            }
        }

        if (moveDuration.isFinite()) {
            // After move animation is done, start next player turn.
            postDelayed(moveDuration) {
                if (state.playerToMove is HumanPlayer) {
                    prepareHumanTurn()
                }
                game.playNext()
            }
        }
    }

    /**
     * Collect the trick to the [dst] card container after a [delay].
     * The destination container is temporarily shown so the cards are not hidden during the transition.
     * Returns the duration of the collect animation.
     */
    private fun collectTrick(dst: CardContainer, delay: Float): Float {
        var moveDuration = delay
        dst.visibility = CardContainer.Visibility.ALL

        updateLastTrickPage()

        // Move trick to the player's hidden stack
        postDelayed(moveDuration) {
            for (i in 0 until trick.capacity) {
                cardAnimationLayer.moveCard(trick, dst, i, 0, replaceSrc = true)
            }
            cardAnimationLayer.update()

            updatePlayerScores()
        }
        moveDuration += CardAnimationLayer.UPDATE_DURATION

        postDelayed(moveDuration) {
            dst.visibility = CardContainer.Visibility.NONE
        }

        return moveDuration
    }

    /**
     * Prepare the layout for a human player's turn, at the south position.
     */
    private fun prepareHumanTurn() {
        val game = game!!
        val state = game.state!!

        val moves = state.getMoves()
        if (moves.size == 1 && settings.getBoolean(PrefKeys.AUTO_PLAY)) {
            // Only one card is playable, auto-play it.
            game.doMove(moves.first())

        } else {
            if (state.phase == GameState.Phase.TRADE) {
                tradePopup.show(playerHand, Popup.Side.ABOVE)

            } else {
                // Show idle popup after some time if not already shown (by initGame)
                if (!idlePopup.shown) {
                    val delay = if (state.currentTrick.cards.size == 0
                            && state.tricksPlayed.size == 0) 0f else 3f
                    idleAction = postDelayed(delay) {
                        idlePopup.show(playerHand, Popup.Side.ABOVE)
                        idleAction = null
                    }
                }

                // Highlight playable cards if necessary
                if (settings.getBoolean(PrefKeys.SELECT_PLAYABLE)) {
                    val playableCards = moves.map { (it as PlayMove).card }
                    if (playableCards.size < playerHand.size) {
                        // Highlight playable cards. This is delayed so that the player hand
                        // has time to layout if cards were set on same frame
                        postDelayed(0.1f) {
                            playerHand.highlightCards(playableCards)
                        }
                    }
                }

                playerHand.clickListener = { actor, _ ->
                    val move = moves.find { it is PlayMove && it.card == actor.card }
                    if (move != null) {
                        // This card can be played, play it.
                        game.doMove(move)

                        // Remove click listener
                        playerHand.clickListener = null

                        // Hide and cancel idle popup
                        idlePopup.hide()
                        idleAction = null
                    }
                }
            }
        }
    }

    /**
     * Adjust a trick start angle so that the card of
     * the player who led the trick (at [startPos]) is below.
     */
    private fun getTrickStartAngle(startPos: Int) = -((startPos + 3.0 / 8) * PI * 2 / 3).toFloat()

    /** Update the total scores in the scores table footers. */
    private fun updateTotalScoreFooters() {
        val game = game!!

        // Find the leader player position
        var minIndex = 0
        var tie = false
        for (i in 1..2) {
            val score = game.players[i].score
            val min = game.players[minIndex].score
            if (score < min) {
                minIndex = i
                tie = false
            } else if (score == min) {
                tie = true
            }
        }

        scoresTable.footerScores = List(3) {
            ScoresTable.Score(numberFormat.format(game.players[it].score),
                    if (it == minIndex && !tie) ScoresTable.Score.Highlight.POSITIVE else ScoresTable.Score.Highlight.NONE)
        }
    }

    /** Add the scores row corresponding to the end [event] of a round. */
    private fun addScoresTableRow(event: RoundEndEvent) {
        scoresTable.scores += List(3) {
            val diff = 4 - event.result.playerResults[it]
            ScoresTable.Score(numberFormat.format(diff))
        }
        scoresTable.cellAdapter?.notifyChanged()

        /*
        when {
            diff > 0 -> ScoresTable.Score.Highlight.NEGATIVE
            diff < 0 -> ScoresTable.Score.Highlight.POSITIVE
            else -> ScoresTable.Score.Highlight.NONE
        }
         */
    }

    /** If round is done, update the hands page in scoreboard. */
    private fun updateHandsPage() {
        val game = game!!

        val shown = game.round > 0 && game.phase == Game.Phase.GAME_STARTED
        handsPage.shown = shown

        if (shown) {
            // Get the starting hands and redo all the trade moves to get the correct initial hands.
            val startIndex = game.events.indexOfLast { it is RoundStartEvent }
            val hands = (game.events[startIndex] as RoundStartEvent).hands.toMutableList()
            for (event in game.events.subList(startIndex + 1, startIndex + 4)) {
                if ((event as TradeHandMove).trade) {
                    val temp = hands[3]
                    hands[3] = hands[event.playerPos]
                    hands[event.playerPos] = temp
                }
            }

            val playerRows = MutableList(3) {
                HandsTable.PlayerRow(namesPref.names[it], hands[it].cards)
            }
            playerRows += HandsTable.PlayerRow(strings["scoreboard_hands_extra"], hands[3].cards)
            handsTable.players = playerRows
        }
    }

    /** If round is done, update the tricks page in scoreboard. */
    private fun updateTricksPage() {
        val game = game!!

        val shown = game.round > 0 && game.phase == Game.Phase.GAME_STARTED
        tricksPage.shown = shown

        if (shown) {
            val startEvent = game.events.last { it is RoundStartEvent } as RoundStartEvent
            val endEvent = game.events.last { it is RoundEndEvent } as RoundEndEvent
            tricksTable.cards = endEvent.tricks.map { trick ->
                val rotated = trick.cards.toMutableList()
                Collections.rotate(rotated, trick.startPos)
                val winner = trick.findWinner(startEvent.trumpSuit)
                List(3) { TricksTable.TrickCard(rotated[it], it == winner) }
            }
        }
    }

    /** Copy the current trick to the last trick container and update the page visibility in scoreboard. */
    private fun updateLastTrickPage() {
        val game = game!!
        val state = game.state!!

        lastTrickPage.shown = (state.tricksPlayed.size > 0 && game.phase == Game.Phase.ROUND_STARTED)
        if (lastTrickPage.shown) {
            val trick = state.tricksPlayed.last()
            lastTrick.cards = trick.cards
            lastTrick.startAngle = getTrickStartAngle(trick.startPos)
        }
    }

    override fun onPreferenceValueChanged(pref: PrefEntry) {
        if (pref.key == PrefKeys.PLAYER_NAMES) {
            updatePlayerNames()
        }
    }

    /**
     * Update the name of players everywhere it's displayed:
     * - The player labels.
     * - The headers of the scores table. The difficulty is set at the same time.
     * - The name column of the hands table.
     */
    private fun updatePlayerNames() {
        val game = game!!

        val diffPref = newGameOptions[PrefKeys.DIFFICULTY] as SliderPref

        val scoresHeaders = mutableListOf<ScoresTable.Header>()
        repeat(3) {
            val player = game.players[it]
            val name = namesPref.names[it]

            // Label
            playerLabels[it].name = name

            // Scores page
            scoresHeaders += ScoresTable.Header(name, if (player is MctsPlayer) {
                diffPref.enumValues?.get(player.difficulty.ordinal)
            } else {
                null
            })

            // Hands page
            updateHandsPage()

            // Tricks page
            tricksTable.headers = namesPref.names.toList()
        }
        scoresTable.headers = scoresHeaders
    }

    private fun updatePlayerScores() {
        val game = game as Game
        repeat(3) {
            val player = game.players[it]
            playerLabels[it].score = if (game.tradePhaseEnded) {
                strings.format("player_score", player.score, player.tricksTaken)
            } else {
                when (player.trade) {
                    Player.Trade.TRADE -> strings["player_trade"]
                    Player.Trade.NO_TRADE -> strings["player_no_trade"]
                    Player.Trade.UNKNOWN -> null
                }
            }
        }
    }

}
