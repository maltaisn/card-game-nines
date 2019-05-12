package com.maltaisn.nines.core

import com.badlogic.gdx.files.FileHandle
import com.maltaisn.cardgame.core.CardGame
import com.maltaisn.cardgame.core.GameEvent
import com.maltaisn.cardgame.core.PCard
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.prefs.PlayerNamesPref
import com.maltaisn.cardgame.prefs.PrefEntry
import com.maltaisn.cardgame.widget.card.CardAnimationLayer
import com.maltaisn.nines.core.core.GameState
import com.maltaisn.nines.core.core.HumanPlayer
import com.maltaisn.nines.core.core.MctsPlayer
import com.maltaisn.nines.core.core.MctsPlayer.Difficulty
import com.maltaisn.nines.core.core.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random


class Game : CardGame {

    var players: List<Player> = emptyList()
        private set

    /** The difficulty set for AI players. */
    val difficulty: Difficulty

    /** The position of the current dealer. */
    var dealerPos = 0
        private set

    /** The trump suit for the current round. */
    val trumpSuit: Int
        get() = TRUMP_SUITS[round % TRUMP_SUITS.size]


    private val gameSpeedDelay: Long
        get() = when (settings.getChoice(PrefKeys.GAME_SPEED)) {
            "slow" -> 1500
            "normal" -> 1000
            "fast" -> 500
            "very_fast" -> 100
            else -> error("Unknown game speed.")
        } + (CardAnimationLayer.UPDATE_DURATION * 1000).toLong()

    /** System time when the last move was made. */
    private var lastMoveTime = 0


    constructor(settings: GamePrefs, newGameOptions: GamePrefs) : super(settings) {
        difficulty = when (newGameOptions.getInt(PrefKeys.DIFFICULTY)) {
            0 -> Difficulty.BEGINNER
            1 -> Difficulty.INTERMEDIATE
            2 -> Difficulty.ADVANCED
            3 -> Difficulty.EXPERT
            else -> error("Wrong difficulty level.")
        }
    }

    constructor(settings: GamePrefs, file: FileHandle) : super(settings) {
        difficulty = Difficulty.BEGINNER
        dealerPos = 0
        // TODO
    }


    override fun start() {
        // Create players
        val south = HumanPlayer()
        val west = MctsPlayer(difficulty)
        val north = MctsPlayer(difficulty)
        players = listOf(south, west, north)

        val startScore = settings.getInt(PrefKeys.START_SCORE)
        south.score = startScore
        west.score = startScore
        north.score = startScore
        updatePlayerNames()

        dealerPos = Random.nextInt(3)

        super.start()
        startRound()
    }

    override fun startRound() {
        dealerPos = (dealerPos + 1) % 3
        gameState = GameState(settings, players, Random.nextInt(3), trumpSuit)

        super.startRound()
    }

    override fun doMove(move: GameEvent.Move) {
        coroutineScope.launch {
            // Wait between AI players moves to adjust game speed
            if (gameState?.playerToMove !is HumanPlayer) {
                delay(gameSpeedDelay - (System.currentTimeMillis() - lastMoveTime))
            }

            launch(Dispatchers.Main) {
                // Do move and callback on main thread
                super.doMove(move)
                val next = gameState?.playerToMove
                if (next is MctsPlayer) {
                    // Find next AI move asynchronously
                    GlobalScope.launch {
                        doMove(next.findMove(gameState!!))
                    }
                }
            }
        }
    }

    override fun onPreferenceValueChanged(pref: PrefEntry) {
        if (pref.key == PrefKeys.PLAYER_NAMES) {
            updatePlayerNames()
        }
    }

    /** Update [players] names from the preference values. */
    private fun updatePlayerNames() {
        val pref = settings[PrefKeys.PLAYER_NAMES] as PlayerNamesPref
        repeat(3) {
            players[it].name = pref.names[it]
        }
    }

    override fun save(file: FileHandle) {
        TODO()
    }


    override fun toString() = super.toString().dropLast(1) + ", difficulty: $difficulty," +
            "dealer: ${players[dealerPos].name}, trumpSuit: " +
            (if (trumpSuit == GameState.NO_TRUMP) "none" else PCard.SUIT_STR[trumpSuit]) + "]"


    companion object {
        private val TRUMP_SUITS = intArrayOf(PCard.HEART, PCard.SPADE,
                PCard.DIAMOND, PCard.SPADE, GameState.NO_TRUMP)
    }

}
