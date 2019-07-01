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

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
import com.maltaisn.cardgame.CoreRes
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
import com.maltaisn.nines.core.game.*
import com.maltaisn.nines.core.game.MctsPlayer.Difficulty
import ktx.actors.onClick
import java.text.NumberFormat
import kotlin.math.PI


class GameLayout(assetManager: AssetManager,
                 val newGameOptions: GamePrefs,
                 val settings: GamePrefs) :
        CardGameLayout(assetManager) {

    var game: Game? = null
        private set

    //@GDXAssets(propertiesFiles = ["assets/strings.properties"])
    private val bundle: I18NBundle = assetManager.get(Res.STRINGS_BUNDLE)

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

    private val scoresTable: ScoresTable

    private var idleAction: Action? = null
        set(value) {
            if (value == null) removeAction(field)
            field = value
        }

    private val numberFormat = NumberFormat.getInstance()

    init {
        // Menu
        menu = object : DefaultGameMenu(coreSkin) {
            override fun onContinueClicked() {
                Game.load(this@GameLayout.settings, GameSaveJson) {
                    initGame(it)
                }
            }

            override fun onStartGameClicked() {
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
                game?.save(GameSaveJson)
                hide()
                continueItem.enabled = true
            }

            override fun onScoreboardOpened() {
                hide()
            }

            override fun onScoreboardClosed() {
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
        menu.rules = assetManager.get(Res.MD_RULES)
        addActor(menu)

        // Card containers
        val cardSkin: Skin = assetManager.get(CoreRes.PCARD_SKIN)

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

        val tradeBtn = PopupButton(coreSkin, bundle["popup_trade"])
        tradeBtn.onClick {
            val game = game!!
            game.doMove(game.state?.getMoves()?.find { (it as TradeHandMove).trade }!!)
            tradePopup.hide()
        }

        val noTradeBtn = PopupButton(coreSkin, bundle["popup_no_trade"])
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

        val collectBtn = PopupButton(coreSkin, bundle["popup_ok"])
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

        val idleBtn = PopupButton(coreSkin, bundle["popup_your_turn"])
        idlePopup.add(idleBtn).minWidth(150f)
        idlePopup.touchable = Touchable.disabled

        // Score table
        scoresTable = ScoresTable(coreSkin, 3)
        val scoresView = Container(scoresTable).pad(30f, 15f, 30f, 15f).fill()
        val scoresPage = PagedSubMenu.Page(0, bundle["scoreboard_scores"],
                coreSkin.getDrawable(MenuIcons.LIST), SubMenu.ITEM_POS_TOP)
        scoresPage.content = scoresView
        menu.scoreboardMenu.addItem(scoresPage)
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
                is GameEvent.Start -> startGame()
                is GameEvent.End -> endGame()
                is GameEvent.RoundStart -> startRound()
                is GameEvent.RoundEnd -> endRound(it)
                is GameEvent.Move -> doMove(it)
            }
        }

        cardAnimationLayer.clearDelayedMoves()
        cardAnimationLayer.completeAnimation()

        playerHand.enabled = (game.players[0] is HumanPlayer)

        updatePlayerNames()

        // Set scores in scores table
        for (event in game.events) {
            if (event is GameEvent.RoundEnd) {
                addScoresTableRow(event)
            }
        }
        updateTotalScoreFooters()

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
                    setTrickStartAngle((state.posToMove - state.currentTrick.cards.size + 3) % 3)

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

    private fun endRound(event: GameEvent.RoundEnd) {
        // Update scores table
        addScoresTableRow(event)
        updateTotalScoreFooters()

        // Show scoreboard after a small delay
        postDelayed(1f) {
            menu.showScoreboard()
        }
    }

    private fun doMove(move: GameEvent.Move) {
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
                        setTrickStartAngle(move.playerPos)
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
                    val delay = if (state.currentTrick.cards.size == 0 && state.tricksPlayed == 0) 0f else 3f
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
     * Adjust the trick start angle so that the card of
     * the player who led the trick (at [startPos]) is below.
     */
    private fun setTrickStartAngle(startPos: Int) {
        trick.startAngle = -((startPos + 3.0 / 8) * PI * 2 / 3).toFloat()
    }

    /** Update the total scores in the scores table footers. */
    private fun updateTotalScoreFooters() {
        val game = game!!
        scoresTable.footerScores = List(3) {
            ScoresTable.Score(numberFormat.format(game.players[it].score))
        }
    }

    /** Add the scores row corresponding to the end [event] of a round. */
    private fun addScoresTableRow(event: GameEvent.RoundEnd) {
        scoresTable.scores += List(3) {
            ScoresTable.Score(numberFormat.format(event.result.playerResults[it]))
        }
        scoresTable.cellAdapter?.notifyChanged()
    }

    override fun onPreferenceValueChanged(pref: PrefEntry) {
        if (pref.key == PrefKeys.PLAYER_NAMES) {
            updatePlayerNames()
        }
    }

    /**
     * Update the name of players and the names displayed in player labels and the
     * scores table headers. Also sets the difficulty shown in the headers.
     */
    private fun updatePlayerNames() {
        val game = game!!

        val namesPref = settings[PrefKeys.PLAYER_NAMES] as PlayerNamesPref
        val diffPref = newGameOptions[PrefKeys.DIFFICULTY] as SliderPref

        val scoresHeaders = mutableListOf<ScoresTable.Header>()
        repeat(3) {
            val player = game.players[it]
            val name = namesPref.names[it]
            player.name = name
            playerLabels[it].name = name
            scoresHeaders += ScoresTable.Header(name, if (player is MctsPlayer) {
                diffPref.enumValues?.get(player.difficulty.ordinal)
            } else {
                null
            })
        }
        scoresTable.headers = scoresHeaders
    }

    private fun updatePlayerScores() {
        val game = game as Game
        repeat(3) {
            val player = game.players[it]
            playerLabels[it].score = if (game.tradePhaseEnded) {
                bundle.format("player_score", player.score, player.tricksTaken.size)
            } else {
                when (player.trade) {
                    Player.Trade.TRADE -> bundle["player_trade"]
                    Player.Trade.NO_TRADE -> bundle["player_no_trade"]
                    Player.Trade.UNKNOWN -> null
                }
            }
        }
    }

}
