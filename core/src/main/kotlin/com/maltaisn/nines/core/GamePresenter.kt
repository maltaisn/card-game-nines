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

import com.badlogic.gdx.Files
import com.maltaisn.cardgame.CoreRes
import com.maltaisn.cardgame.game.CardPlayer
import com.maltaisn.cardgame.game.sortWith
import com.maltaisn.cardgame.pcard.PCard
import com.maltaisn.cardgame.prefs.GamePref
import com.maltaisn.cardgame.prefs.PlayerNamesPref
import com.maltaisn.cardgame.prefs.SliderPref
import com.maltaisn.cardgame.widget.DealerChip
import com.maltaisn.cardgame.widget.FadeTable
import com.maltaisn.cardgame.widget.card.CardAnimationGroup
import com.maltaisn.cardgame.widget.card.CardContainer
import com.maltaisn.cardgame.widget.card.CardHand
import com.maltaisn.cardgame.widget.menu.SubMenu
import com.maltaisn.cardgame.widget.table.ScoresTable
import com.maltaisn.cardgame.widget.table.TricksTable
import com.maltaisn.nines.core.game.Game
import com.maltaisn.nines.core.game.Game.Phase
import com.maltaisn.nines.core.game.GameSaveJson
import com.maltaisn.nines.core.game.GameState
import com.maltaisn.nines.core.game.event.*
import com.maltaisn.nines.core.game.player.CheatingPlayer
import com.maltaisn.nines.core.game.player.HumanPlayer
import com.maltaisn.nines.core.game.player.MctsPlayer
import com.maltaisn.nines.core.game.player.RandomPlayer
import com.maltaisn.nines.core.widget.HandsTable
import com.maltaisn.nines.core.widget.TrumpIndicator
import ktx.assets.file
import java.util.*
import kotlin.math.PI
import kotlin.math.max


class GamePresenter(private val layout: GameContract.View) : GameContract.Presenter {

    private var game: Game? = null

    private val namesPref: PlayerNamesPref
        get() = layout.settings[PrefKeys.PLAYER_NAMES] as PlayerNamesPref

    /** Whether a game is currently shown in the layout or not. */
    private var gameShown = false

    /** Whether the game_summary menu is currently opened or not. */
    private var gameSummaryShown = false

    /**
     * Whether the trade phase has ended in the layout. This is used by [updatePlayerScores]
     * to make sure that the last player choice to trade is displayed for some time, because
     * [GameState.phase] is already changed at the time [onMove] is called.
     */
    private var tradePhaseEnded = false

    /**
     * Whether game is currently in the state of waiting for human input. Can be either:
     * - Waiting to play a card
     * - Waiting to decide to trade or not
     * - Waiting to confirm collect trick popup
     */
    private var waitingForHumanInput = false

    /** Whether continue was clicked and the game is currently loading. */
    private var gameLoading = false

    private var lastBackPressTime = 0L

    private var animationDuration = 0f


    override fun attach() {
        layout.settings.addValueListener(::onPreferenceValueChanged)
        updateAnimationDuration()

        // Check the scores page, thus adding the scores table to the layout.
        // It must be pre-added so that when it's time to show it, it will have been laid out
        // and scroll to bottom will work...
        layout.checkGameSummaryScoresPage()

        layout.setContinueItemEnabled(GAME_SAVE_FILE.exists())
    }

    override fun detach() {
        layout.settings.removeValueListener(::onPreferenceValueChanged)

        disposeGame()
    }


    private fun requireGame() = checkNotNull(game) { "Game must be initialized." }

    private fun requireState() = checkNotNull(game?.state) { "Game must have a round started." }


    override fun onSave() {
        game?.save(GAME_SAVE_FILE, GameSaveJson)

        layout.apply {
            settings.save()
            newGameOptions.save()
            stats.save()
        }
    }

    override fun onPrefNeedsConfirm(pref: GamePref<*>, callback: (Boolean) -> Unit) {
        if (GAME_SAVE_FILE.exists()) {
            layout.showResetGameDialog(pref, callback)
        }
    }

    override fun onPrefConfirmed() {
        // User has changed a preference that requires erasing the game save.
        eraseGameSave()
    }

    override fun onBackPress() {
        if ((System.currentTimeMillis() - lastBackPressTime) / 1000f < BACK_PRESS_COOLDOWN) {
            // Back was pressed too quickly, do nothing.
            return
        }
        lastBackPressTime = System.currentTimeMillis()

        // Do the same action as when the back arrow is pressed.
        when {
            gameShown -> onExitGameClicked()
            gameSummaryShown -> onGameSummaryCloseClicked()
            else -> layout.goToPreviousMenu()
        }
    }


    override fun onContinueClicked() {
        if (gameLoading) return
        gameLoading = true

        Game.load(GAME_SAVE_FILE, layout.settings, GameSaveJson) { game ->
            gameLoading = false
            if (game != null) {
                // Game loaded successfully, show the game.
                layout.showInGameMenu(true)
                initGame(game)
                show()
                if (game.phase == Phase.ROUND_ENDED) {
                    // Game was saved after round end, start a new round.
                    game.startRound()
                }
            } else {
                // Could not load save file.
                eraseGameSave()
            }
        }
    }

    override fun onStartGameClicked() {
        layout.showInGameMenu(false)

        val difficulty = "mcts_${layout.newGameOptions.getInt(PrefKeys.DIFFICULTY)}"

        // Create players
        val south = HumanPlayer()
        val west = createPlayer(difficulty)
        val north = createPlayer(difficulty)

        // Create and start the game
        val game = Game(layout.settings, south, west, north)
        initGame(game)

        show()
        game.start()
    }

    private fun createPlayer(type: String) = when (type) {
        "human" -> HumanPlayer()
        "mcts_0" -> MctsPlayer(MctsPlayer.Difficulty.BEGINNER)
        "mcts_1" -> MctsPlayer(MctsPlayer.Difficulty.INTERMEDIATE)
        "mcts_2" -> MctsPlayer(MctsPlayer.Difficulty.ADVANCED)
        "mcts_3" -> MctsPlayer(MctsPlayer.Difficulty.EXPERT)
        "mcts_4" -> MctsPlayer(MctsPlayer.Difficulty.PERFECT)
        "cheat" -> CheatingPlayer()
        "random" -> RandomPlayer()
        else -> error("Unknown player type")
    }

    override fun onExitGameClicked() {
        hide()
        layout.setTrumpIndicatorShown(false)
        layout.goToPreviousMenu()

        game?.save(GAME_SAVE_FILE, GameSaveJson)
        layout.setContinueItemEnabled(true)
        disposeGame()
    }

    override fun onGameSummaryOpenClicked() {
        showGameSummary()
    }

    override fun onGameSummaryCloseClicked() {
        hideGameSummary()
    }

    override fun onGameSummaryContinueItemClicked() {
        hideGameSummary()
    }

    override fun onTradeBtnClicked(trade: Boolean) {
        if (!waitingForHumanInput) return
        waitingForHumanInput = false

        val game = requireGame()
        val state = requireState()

        // Do the trade move and hide the popup
        game.doMove(state.getMoves().find { (it as TradeHandMove).trade == trade }!!)
        layout.tradePopupShown = false
    }

    override fun onCollectTrickBtnClicked() {
        if (!waitingForHumanInput) return
        waitingForHumanInput = false

        collectTrick()

        layout.collectPopupShown = false
        layout.doDelayed(animationDuration) {
            playNext()
        }
    }

    override fun onPlayerCardClicked(card: PCard) {
        val game = requireGame()
        val state = requireState()

        if (!waitingForHumanInput || state.phase != GameState.Phase.PLAY) return
        waitingForHumanInput = false

        // Play the clicked card if it can be played.
        val move = state.getMoves().find { (it as PlayMove).card == card }
        if (move != null) {
            // This card can be played, play it.
            game.doMove(move)

            // Hide and cancel idle popup
            layout.idlePopupShown = false
            layout.cancelDelayedIdlePopup()
        }
    }

    override fun onPlayerCardDealt() {
        playSound(CoreRes.SOUND_CARD_DROP, 0.3f)
    }

    override fun onAboutRateAppClicked() {
        layout.listener.onRateAppClicked()
    }

    override fun onAboutReportBugClicked() {
        layout.listener.onReportBugClicked()
    }

    override fun onGameOverDialogScoresBtnClicked() {
        showGameSummary()
    }

    override fun onGameOverDialogNewGameBtnClicked() {
        layout.setGameOverDialogShown(false)

        layout.hideDealerChip()
        layout.setPlayerHandShown(false, animate = false)

        layout.doDelayed(DealerChip.FADE_DURATION) {
            disposeGame()

            gameShown = false
            onStartGameClicked()
        }
    }

    /**
     * Initialize a new [game], if no game is currently set.
     */
    private fun initGame(game: Game) {
        check(this.game == null) { "A game is already shown in the layout." }
        this.game = game

        StatsHandler(game, layout.stats)

        game.eventListeners += { event ->
            when (event) {
                is StartEvent -> onGameStarted()
                is EndEvent -> onGameEnded()
                is RoundStartEvent -> onRoundStarted()
                is RoundEndEvent -> onRoundEnded()
                is MoveEvent -> onMove(event)
            }
        }
    }

    /**
     * Dispose the current game if one is set.
     */
    private fun disposeGame() {
        game?.dispose()
        game = null
    }

    /**
     * Show and initialize all layout components for a [game].
     * If the game has started, player turns will begin or continue.
     * If the game hasn't started, layout will stay hidden.
     */
    private fun show() {
        if (gameShown) return
        gameShown = true

        val game = requireGame()

        layout.completeAnimations()

        layout.setPlayerHandEnabled(game.players[0] is HumanPlayer)

        // Show trump indicator after some time
        if (game.phase != Phase.ENDED) {
            layout.doDelayed(TrumpIndicator.TRANSITION_DURATION + 0.1f) {
                layout.setTrumpIndicatorSuit(game.trumpSuit)
                layout.setTrumpIndicatorShown(true)
            }
        }

        // Update player names
        updatePlayerNames()

        if (game.phase == Phase.ROUND_STARTED) {
            // Round has started
            val state = requireState()

            layout.setPlayerLabelsShown(true)
            layout.showDealerChip(game.dealerPos)

            tradePhaseEnded = (state.phase == GameState.Phase.PLAY)

            val humanPlayerTurn = (state.posToMove == 0 && game.players[0] is HumanPlayer)

            if (tradePhaseEnded) {
                // Play phase: show and adjust trick
                layout.setTrickShown(true)
                layout.setTrickCards(List(3) { state.currentTrick.cards.getOrNull(it) })
                layout.setTrickStartAngle(getTrickStartAngle(state.currentTrick.startPos))

                if (humanPlayerTurn) {
                    // Immediately show idle popup if it's a human player's turn
                    layout.idlePopupShown = true
                }

            } else {
                // Trade phase: show extra hand
                layout.setExtraHandShown(true)
                layout.setExtraHandCards(state.extraHand.cards)
            }

            layout.setPlayerHandShown(true)
            layout.setPlayerHandCards(game.players[0].hand.cards)

            for (i in 1..2) {
                layout.setHiddenStackCards(i, game.players[i].hand.cards)
            }

            // NOTE: taken tricks cards are not put back in the hidden stack since they're not used

            updatePlayerScores()

            // Wait a bit then start playing
            layout.doDelayed(0.5f) {
                playNext()
            }

        } else if (game.phase == Phase.ENDED) {
            tradePhaseEnded = true

            // Game has ended.
            showGameOverDialog()

            // Only show player labels with scores
            layout.showDealerChip(game.dealerPos)
            layout.setPlayerLabelsShown(true)
            layout.setTrumpIndicatorShown(false)
            updatePlayerScores()
        }
    }

    /**
     * Hide the layout cancelling all animations and disposes of the game.
     */
    private fun hide() {
        if (!gameShown) return
        gameShown = false

        val game = requireGame()

        layout.apply {
            completeAnimations()

            unhighlightAllPlayerCards()

            setPlayerHandShown(false)
            setExtraHandShown(false)
            setTrickShown(false)

            setPlayerLabelsShown(false)
            hideDealerChip()

            tradePopupShown = false
            collectPopupShown = false
            idlePopupShown = false
            cancelDelayedIdlePopup()

            setGameOverDialogShown(false)

            // Hide all hidden stacks cards. If hide() is called during trick collection, hidden stack is
            // visible and failing to hide it back will result in hands being shown during animations.
            repeat(3) { setHiddenStackCardsShown(it, false) }
        }

        game.cancelAiTurn()
    }

    private fun onGameStarted() {
        game?.startRound()
    }

    private fun onGameEnded() {
        val game = requireGame()

        // Show the game over dialog
        layout.doDelayed(1f) {
            layout.setTrumpIndicatorShown(false)
            showGameOverDialog()

            // Play game over sound
            playSound(if (game.players[0] !is HumanPlayer || game.winnerPos == 0) {
                // Play this sound if winner is human or there's no human in the game.
                CoreRes.SOUND_GAME_WIN
            } else {
                CoreRes.SOUND_GAME_LOSE
            })
        }
    }

    /**
     * Change the layout for the start of a round, showing the extra hand and setting initial hands
     * for all players. The player turns are then initiated.
     */
    private fun onRoundStarted() {
        val game = requireGame()
        val state = requireState()

        tradePhaseEnded = false

        updatePlayerScores()

        // Show the player labels and the dealer chip
        var moveDuration = 0.2f
        layout.doDelayed(moveDuration) {
            layout.setPlayerLabelsShown(true)
            layout.showDealerChip(if (game.round == 1) -1 else (game.dealerPos + 2) % 3)
        }
        moveDuration += max(FadeTable.DEFAULT_FADE_DURATION, DealerChip.FADE_DURATION) + 0.1f

        // Move the dealer chip to the dealer player
        layout.doDelayed(moveDuration) {
            layout.moveDealerChip(game.dealerPos)
        }
        moveDuration += DealerChip.MOVE_DURATION + 0.1f

        // Set player hand. They must be sorted first to be dealt in correct order.
        val playerCards = game.players[0].hand.cards.toMutableList()
        playerCards.sortWith(PCard.DEFAULT_SORTER)

        if (layout.settings.getBoolean(PrefKeys.CARD_DEAL_ANIMATION)) {
            layout.setHiddenStackCards(0, playerCards)
            layout.setPlayerHandCards(emptyList())
            layout.setPlayerHandShown(true, animate = false)

            layout.doDelayed(moveDuration) {
                layout.dealPlayerCards(animationDuration)
            }
            moveDuration += animationDuration * playerCards.size

        } else {
            layout.setHiddenStackCards(0, emptyList())
            layout.setPlayerHandCards(playerCards)

            layout.doDelayed(moveDuration) {
                layout.setPlayerHandShown(true)
            }
            moveDuration += CardContainer.TRANSITION_DURATION
        }

        // Set cards in containers
        for (i in 1..2) {
            layout.setHiddenStackCards(i, state.players[i].hand.cards)
        }
        layout.setTrickCards(List(3) { null })

        // Show the extra hand
        layout.setExtraHandCards(state.extraHand.cards)
        moveDuration += 0.1f
        layout.doDelayed(moveDuration) {
            layout.setExtraHandShown(true)
        }
        moveDuration += CardContainer.TRANSITION_DURATION

        // Start playing
        moveDuration += 0.5f
        layout.doDelayed(moveDuration) {
            playNext()
        }
    }

    /**
     * End the round by updating and then showing the game summary.
     */
    private fun onRoundEnded() {
        val game = requireGame()

        layout.cancelDelayedIdlePopup()

        // Show game summary after a small delay if nobody has won yet.
        if (game.winnerPos == CardPlayer.NO_POSITION) {
            layout.doDelayed(1f) {
                showGameSummary()
                playSound(CoreRes.SOUND_GAME_DONE)
            }
        }
    }

    /**
     * Do a [move] in the layout.
     * If the move is a trade move, the extra hand will be exchanged with the player hand.
     * If the move is a play move, the played card will be moved to the trick. When the
     * trick is done, it is collected by the player. The next player turn is then initiated.
     */
    private fun onMove(move: MoveEvent) {
        val game = requireGame()
        val state = requireState()

        val pos = move.playerPos
        val isSouth = (pos == 0)

        var moveDuration = 0f
        when (move) {
            is TradeHandMove -> {
                // Update the "Traded" or "Didn't trade" status on the player label.
                updatePlayerScores()

                if (move.trade) {
                    val volume = if (game.players[pos] is HumanPlayer) 1f else 0.5f

                    // Do swap hand animation
                    if (isSouth) {
                        // Hide player hand
                        layout.setPlayerHandShown(false)
                        moveDuration += CardContainer.TRANSITION_DURATION

                        moveDuration += 0.1f
                        layout.doDelayed(moveDuration) {
                            // Move player hand cards to hidden stack
                            // Extra hand is used since the move has already been done in the state
                            layout.setHiddenStackCards(0, state.extraHand.cards)
                            layout.setPlayerHandCards(emptyList())
                            layout.setPlayerHandShown(true, animate = false)

                            // Move cards from extra hand to player hand
                            layout.moveCardsFromExtraHandToPlayerHand(GameState.CARDS_COUNT)
                            playSound(CoreRes.SOUND_CARD_TAKE)
                        }

                    } else {
                        // Move cards from extra hand to hidden stack
                        layout.moveCardsFromExtraHandToHiddenStack(pos, GameState.CARDS_COUNT)
                        playSound(CoreRes.SOUND_CARD_TAKE, volume)
                    }

                    // Move cards from hidden stack to extra hand
                    moveDuration += CardAnimationGroup.DEFAULT_UPDATE_DURATION + 0.1f
                    layout.doDelayed(moveDuration) {
                        layout.moveCardsFromHiddenStackToExtraHand(pos, GameState.CARDS_COUNT)
                        playSound(CoreRes.SOUND_CARD_TAKE, volume)
                    }
                    moveDuration += CardAnimationGroup.DEFAULT_UPDATE_DURATION

                } else {
                    moveDuration = animationDuration * 2
                }

                // If this player had the last choice
                if ((move.playerPos + 1) % 3 == game.dealerPos) {
                    tradePhaseEnded = true

                    // Hide extra hand and show trick
                    moveDuration += game.gameSpeedDelay + 0.5f
                    layout.doDelayed(moveDuration) {
                        layout.setExtraHandShown(false)
                        layout.setTrickShown(true, animate = false)
                    }
                    moveDuration += CardContainer.TRANSITION_DURATION

                    moveDuration += 0.1f
                    layout.doDelayed(moveDuration) {
                        updatePlayerScores()
                    }
                }
            }
            is PlayMove -> {
                // Move card from player hand to the trick
                layout.movePlayerCardToTrick(pos, move.card, animationDuration)
                if (layout.settings.getBoolean(PrefKeys.REORDER_HAND)) {
                    layout.sortPlayerHand()
                }
                playSound(CoreRes.SOUND_CARD_SHOVE, 0.5f)

                moveDuration = animationDuration + 0.1f

                if (isSouth && layout.settings.getBoolean(PrefKeys.SELECT_PLAYABLE)) {
                    // Unhighlight cards in case they were highlighted before move
                    layout.doDelayed(moveDuration) {
                        layout.unhighlightAllPlayerCards()
                    }
                    moveDuration += CardHand.HIGHLIGHT_DURATION
                }

                when (state.currentTrick.cards.size) {
                    0 -> {
                        // Player plays last
                        if (layout.settings.getBoolean(PrefKeys.AUTO_COLLECT)) {
                            // Collect the trick automatically after some time.
                            moveDuration += animationDuration * 2
                            layout.doDelayed(moveDuration) {
                                collectTrick()
                            }
                            moveDuration += animationDuration

                        } else {
                            // Show the collect popup for confirmation
                            layout.doDelayed(moveDuration + 0.2f) {
                                layout.collectPopupShown = true
                            }
                            moveDuration = Float.POSITIVE_INFINITY
                            waitingForHumanInput = true
                        }
                    }
                    1 -> {
                        // Player plays first: adjust trick start angle
                        layout.setTrickStartAngle(getTrickStartAngle(move.playerPos))
                    }
                }
            }
        }

        if (moveDuration.isFinite()) {
            // After move animation is done, start next player turn.
            layout.doDelayed(moveDuration) {
                playNext()
            }
        }
    }

    /**
     * Collect the trick to the hidden stack of the player who won the trick.
     * Also updates the scores in the labels and the last trick in the game summary.
     */
    private fun collectTrick() {
        val state = requireState()

        layout.collectTrick(state.posToMove, animationDuration)

        updatePlayerScores()

        playSound(CoreRes.SOUND_CARD_TAKE, 0.5f)
    }

    /**
     * Start the next player's turn. If player is human, prepare the layout for the turn.
     */
    private fun playNext() {
        val game = requireGame()
        val state = requireState()

        val player = state.playerToMove
        var moveDone = false

        if (player is HumanPlayer) {
            // Prepare the layout for a human player's turn, at the south position.
            val moves = state.getMoves()
            if (moves.size == 1 && (layout.settings.getBoolean(PrefKeys.AUTO_PLAY) ||
                            player.hand.cards.size == 1)) {
                // Only one card is playable, auto-play it.
                game.doMove(moves.first())
                moveDone = true

            } else {
                // More than one possible move, wait for input.
                waitingForHumanInput = true

                if (state.phase == GameState.Phase.TRADE) {
                    layout.tradePopupShown = true

                } else {
                    // Show idle popup after some time if not already shown
                    if (!layout.idlePopupShown) {
                        layout.showIdlePopupAfterDelay(if (state.currentTrick.cards.size == 0
                                && state.tricksPlayed.size == 0) 0f else IDLE_POPUP_DELAY)
                    }

                    // Highlight playable cards if necessary
                    if (layout.settings.getBoolean(PrefKeys.SELECT_PLAYABLE)) {
                        val playableCards = moves.map { (it as PlayMove).card }
                        if (playableCards.size < state.players[0].hand.cards.size) {
                            // Highlight playable cards. This is delayed so that the player hand
                            // has time to layout if cards were set on same frame
                            layout.doDelayed(0.1f) {
                                layout.highlightPlayerCards(playableCards)
                            }
                        }
                    }
                }
            }
        }

        if (!moveDone) {
            game.playNext()
        }
    }

    /**
     * Returns the start angle for a card trick if the
     * first player to play is at the [startPos] position.
     */
    private fun getTrickStartAngle(startPos: Int) =
            -((startPos + 3.0 / 8) * PI * 2 / 3).toFloat()

    /**
     * Update the player labels to display either the scores and tricks taken for the player
     * if in play phase, or the trade status if in trade phase.
     */
    private fun updatePlayerScores() {
        val game = requireGame()

        if (tradePhaseEnded) {
            val scores = List(3) { game.players[it].score }
            val tricksTaken = if (layout.settings.getBoolean(PrefKeys.DISPLAY_SCORE)) {
                List(3) { game.players[it].tricksTaken }
            } else {
                null
            }
            layout.setPlayerScores(scores, tricksTaken)

        } else {
            layout.setPlayerTradeStatus(List(3) { game.players[it].trade })
        }
    }

    /**
     * Update the name of players everywhere it's displayed:
     * - The player labels.
     * - The headers of the scores table. The difficulty is set at the same time.
     * - The name column of the hands table.
     */
    private fun updatePlayerNames() {
        val game = requireGame()

        // Player labels
        val names = List(3) { namesPref.getPlayerName(it) }
        layout.setPlayerNames(names)

        // Scores table headers
        val diffPref = layout.newGameOptions[PrefKeys.DIFFICULTY] as SliderPref
        layout.setScoresTableHeaders(List(3) { pos ->
            val player = game.players[pos]
            ScoresTable.Header(names[pos], when (player) {
                is MctsPlayer -> if (player.difficulty == MctsPlayer.Difficulty.PERFECT) {
                    "Perfect"
                } else {
                    diffPref.enumValues?.get(player.difficulty.ordinal)
                }
                is CheatingPlayer -> "Cheater"
                is RandomPlayer -> "Random"
                else -> null
            })
        })

        // Tricks page
        layout.setTricksTableHeaders(names)
    }

    private fun showGameSummary() {
        val game = requireGame()

        // Hide game layout
        hide()

        // Update all pages and items
        updateScoresPage()
        updateLastTrickPage()
        updateTricksPage()
        updateHandsPage()

        layout.apply {
            showGameSummary()
            checkGameSummaryScoresPage()
            scrollScoresPageToBottom()
            setGameSummaryContinueItemShown(game.phase == Phase.ROUND_ENDED)

            // Show trump indicator
            setTrumpIndicatorShown(true)
            setTrumpIndicatorSuit(game.trumpSuit)
        }

        gameSummaryShown = true
    }

    private fun hideGameSummary() {
        val game = requireGame()

        layout.goToPreviousMenu()
        show()

        if (game.phase != Phase.ROUND_STARTED) {
            // Hide trump indicator to change suit later (in show)
            layout.setTrumpIndicatorShown(false)
        }

        if (game.phase == Phase.ROUND_ENDED) {
            // Last round has ended, start a new one.
            game.startRound()

            // Wait until game summary is hidden to hide the pages
            layout.doDelayed(SubMenu.TRANSITION_DURATION) {
                layout.setHandsPageShown(false)
                layout.setTricksPageShown(false)
                layout.setGameSummaryContinueItemShown(false)
            }
        }

        gameSummaryShown = false
    }

    /**
     * Update the scores page in game summary.
     */
    private fun updateScoresPage() {
        val game = requireGame()

        // Clear table and re-add all rows
        layout.clearScoresTable()
        for (event in game.events) {
            if (event is RoundEndEvent) {
                layout.addScoresTableRow(List(3) {
                    val diff = Game.MINIMUM_TRICKS - event.result[it]
                    ScoresTable.Score(layout.numberFormat.format(diff))
                })
            }
        }

        // Update total score footers
        val leaderPos = game.leaderPos
        layout.setScoresTableFooters(List(3) { pos ->
            val highlight = if (pos == leaderPos) {
                ScoresTable.Score.Highlight.POSITIVE
            } else {
                ScoresTable.Score.Highlight.NONE
            }
            ScoresTable.Score(layout.numberFormat.format(game.players[pos].score), highlight)
        })
    }

    /**
     * Update the hands page in game summary.
     */
    private fun updateHandsPage() {
        val game = requireGame()

        val shown = game.round > 0 && (game.phase == Phase.ENDED ||
                game.phase == Phase.ROUND_ENDED)
        layout.setHandsPageShown(shown)

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
                HandsTable.PlayerRow(namesPref.getPlayerName(it), hands[it].cards)
            }

            playerRows += HandsTable.PlayerRow(layout.extraHandString, hands[3].cards)
            layout.setHandsPageHands(playerRows)
        }
    }

    /**
     * Update the tricks page in game summary.
     */
    private fun updateTricksPage() {
        val game = requireGame()

        val shown = game.round > 0 && (game.phase == Phase.ENDED ||
                game.phase == Phase.ROUND_ENDED)
        layout.setTricksPageShown(shown)

        if (shown) {
            val startEvent = game.events.last { it is RoundStartEvent } as RoundStartEvent
            val endEvent = game.events.last { it is RoundEndEvent } as RoundEndEvent
            layout.setTricksTableTricks(endEvent.tricks.map { trick ->
                val rotated = trick.cards.toMutableList()
                Collections.rotate(rotated, trick.startPos)
                val winner = trick.findWinner(startEvent.trumpSuit)
                List(3) { TricksTable.TrickCard(rotated[it], it == winner) }
            })
        }
    }

    /**
     * Update the last trick page in game summary.
     */
    private fun updateLastTrickPage() {
        val game = requireGame()

        var shown = (game.phase == Phase.ROUND_STARTED)
        if (shown) {
            val state = requireState()
            shown = (state.tricksPlayed.size > 0)
            if (shown) {
                val trick = state.tricksPlayed.last()
                layout.setLastTrickCards(trick.cards)
                layout.setLastTrickStartAngle(getTrickStartAngle(trick.startPos))
            }
        }

        layout.setLastTrickPageShown(shown)
    }

    /**
     * Show the game over dialog and set the message accordingly.
     */
    private fun showGameOverDialog() {
        val game = requireGame()

        val winner = game.players[game.winnerPos]
        layout.setGameOverDialogMessage(namesPref.getPlayerName(winner.position), winner is HumanPlayer)
        layout.setGameOverDialogShown(true)
    }

    /**
     * Erase the game save file and disable the continue item.
     */
    private fun eraseGameSave() {
        GAME_SAVE_FILE.delete()
        layout.setContinueItemEnabled(false)
    }

    /**
     * Play a [sound] by name if sounds are enabled.
     */
    private fun playSound(sound: String, volume: Float = 1f) {
        if (layout.settings.getBoolean(PrefKeys.ENABLE_SOUND)) {
            layout.playSound(sound, volume)
        }
    }

    private fun updateAnimationDuration() {
        animationDuration = when (layout.settings.getChoice(PrefKeys.GAME_SPEED)) {
            "slow", "normal" -> 0.4f
            "fast" -> 0.3f
            else -> 0.2f
        }
    }

    private fun <T : Any> onPreferenceValueChanged(pref: GamePref<T>, value: T) {
        if (pref.key == PrefKeys.GAME_SPEED) {
            updateAnimationDuration()
        }
    }

    companion object {
        /** The delay after the start of a human player turn before the idle popup is shown. */
        private const val IDLE_POPUP_DELAY = 5f

        /** The minimum time between each back press needed to actually trigger it. */
        private const val BACK_PRESS_COOLDOWN = 1f

        /** The game save file location. */
        val GAME_SAVE_FILE = file("saved-game.json", Files.FileType.Local)
    }

}
