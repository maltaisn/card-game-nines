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

package com.maltaisn.nines.core.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.SerializationException
import com.maltaisn.cardgame.fromJson
import com.maltaisn.cardgame.game.CardGame
import com.maltaisn.cardgame.game.CardGameEvent
import com.maltaisn.cardgame.game.CardPlayer
import com.maltaisn.cardgame.pcard.PCard
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.cardgame.readArrayValue
import com.maltaisn.cardgame.readValue
import com.maltaisn.nines.core.PrefKeys
import com.maltaisn.nines.core.game.event.*
import kotlinx.coroutines.*
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import ktx.async.onRenderingThread
import ktx.log.error
import kotlin.math.max
import kotlin.random.Random

class Game() : CardGame() {

    lateinit var settings: GamePrefs
        private set

    lateinit var players: List<Player>
        private set

    var state: GameState? = null
        private set

    val events: List<GameEvent>
        get() = _events

    private val _events = mutableListOf<GameEvent>()

    /** The current phase of the game. */
    var phase = Phase.ENDED
        private set

    /** The current round. */
    var round = 0
        private set

    /** The position of the current dealer. */
    var dealerPos = CardPlayer.NO_POSITION
        private set

    /** The trump suit for the current round. */
    val trumpSuit: Int
        get() = TRUMP_SUITS[max(0, round - 1) % TRUMP_SUITS.size]

    /**
     * The position of the player who won if the game has ended.
     * If no player has won yet, value is [CardPlayer.NO_POSITION].
     */
    var winnerPos = CardPlayer.NO_POSITION

    /** Whether the game is done or not. */
    val isDone: Boolean
        get() = winnerPos != CardPlayer.NO_POSITION

    /**
     * The position of the leading player in the game.
     * If there's a tie in scores, this returns [CardPlayer.NO_POSITION].
     */
    val leaderPos: Int
        get() {
            var minIndex = 0
            var tie = false
            for (i in 1..2) {
                val score = players[i].score
                val min = players[minIndex].score
                if (score < min) {
                    minIndex = i
                    tie = false
                } else if (score == min) {
                    tie = true
                }
            }
            return if (tie) CardPlayer.NO_POSITION else minIndex
        }

    val gameSpeedDelay: Float
        get() = when (settings.getChoice(PrefKeys.GAME_SPEED)) {
            "slow" -> 1.5f
            "normal" -> 1f
            "fast" -> 0.5f
            else -> 0f
        }

    /**
     * Listener called when a game event happens, or `null` for none.
     * Note that the listener is called at a point where the event has already happened
     * so the game and the state are already changed. Everything must take this into account.
     * When a move is made by player at pos 0, `state.posToMove` will be 1 not 0 when the
     * event listener is called, since the state was changed already.
     */
    var eventListener: ((GameEvent) -> Unit)? = null

    /** System time when the last move was made. */
    private var lastMoveTime = 0L

    private val dispatcher = newSingleThreadAsyncContext()
    private var aiPlayerJob: Job? = null


    constructor(settings: GamePrefs, south: Player, east: Player, north: Player) : this() {
        this.settings = settings
        settings.addListener(this)
        players = listOf(south, east, north)
    }

    /**
     * Start the next player's turn, with a delay to follow game speed.
     */
    fun playNext() {
        lastMoveTime = System.currentTimeMillis()

        val state = state!!
        if (state.isGameDone) {
            // Round is done
            endRound()
        } else {
            val next = state.playerToMove
            if (next is MctsPlayer) {
                // Find next AI move asynchronously
                aiPlayerJob = KtxAsync.launch(dispatcher) {
                    // Wait between AI players moves to adjust game speed
                    delay((gameSpeedDelay * 1000).toLong() -
                            (System.currentTimeMillis() - lastMoveTime))

                    // Find move
                    yield()
                    val move = next.findMove(state)

                    // Do move
                    yield()
                    onRenderingThread {
                        aiPlayerJob = null
                        doMove(move)
                    }
                }
            }
        }
    }

    /**
     * Start or restart the game.
     */
    fun start() {
        check(phase == Phase.ENDED) { "Game has already started." }
        phase = Phase.GAME_STARTED

        round = 0
        state = null

        val startScore = settings.getInt(PrefKeys.START_SCORE)
        for (player in players) {
            player.score = startScore
        }

        dealerPos = Random.nextInt(3)
        winnerPos = CardPlayer.NO_POSITION

        _events.clear()

        doEvent(StartEvent())
    }

    /** End the game. */
    fun end() {
        if (phase == Phase.ROUND_STARTED) {
            // End round if necessary
            endRound()
        }
        check(phase == Phase.GAME_STARTED) { "Game has already ended." }
        phase = Phase.ENDED

        doEvent(EndEvent())
    }

    /** Start a new round. */
    fun startRound() {
        check(phase == Phase.GAME_STARTED) { "Round has already started or game has not started." }
        phase = Phase.ROUND_STARTED

        round++

        val state = GameState(settings, players, dealerPos, trumpSuit)
        this.state = state

        doEvent(RoundStartEvent(trumpSuit,
                players.map { it.hand.clone() } + state.extraHand.clone()))
    }

    /** End the current round. */
    fun endRound() {
        check(phase == Phase.ROUND_STARTED) { "Round has already ended or game has not started." }
        phase = Phase.GAME_STARTED

        val state = state!!

        // Update the scores
        val result = state.result!!
        for ((i, player) in players.withIndex()) {
            player.score += 4 - result.playerResults[i].toInt()
        }

        doEvent(RoundEndEvent(result, state.tricksPlayed))

        this.state = null

        // Check if any player has won
        val leader = players.getOrNull(leaderPos)
        if (leader != null && leader.score <= 0) {
            winnerPos = leader.position
            end()
        }

        dealerPos = (dealerPos + 1) % 3
    }

    /** Do a [move] on the game state. */
    fun doMove(move: CardGameEvent.Move) {
        move as MoveEvent
        check(phase != Phase.ENDED) { "Game has not started." }

        state?.doMove(move)
        doEvent(move)
    }

    /**
     * Cancel the next AI player turn.
     */
    fun cancelAiTurn() {
        aiPlayerJob?.cancel()
        aiPlayerJob = null
    }

    private fun doEvent(event: GameEvent) {
        _events += event
        eventListener?.invoke(event)
    }

    override fun dispose() {
        if (::settings.isInitialized) {
            settings.removeListener(this)
        }

        eventListener = null

        cancelAiTurn()
        dispatcher.dispose()
    }

    override fun toString() = "${events.size} events, phase: $phase, round: $round, " +
            "dealerPos: $dealerPos, trumpSuit: ${(PCard.SUIT_STR.getOrNull(trumpSuit) ?: "none")}]"


    override fun read(json: Json, jsonData: JsonValue) {
        json as GameSaveJson
        super.read(json, jsonData)

        val version = jsonData.getInt("_ninesVersion")
        json.ninesVersion = version
        if (version != VERSION || json.version != CardGame.VERSION) {
            // Version of saved game doesn't match game version.
            // Current behavior is to throw an exception to abort.
            throw SerializationException("Game version mismatch: " +
                    "file is $version/${json.version} and current is $VERSION/${CardGame.VERSION}")
        }

        players = json.readArrayValue("players", jsonData)
        state = json.readValue("state", jsonData)
        _events += json.readArrayValue<ArrayList<GameEvent>, GameEvent>("events", jsonData)
        phase = json.readValue("phase", jsonData)
        round = jsonData.getInt("round")
        dealerPos = jsonData.getInt("dealerPos")
        winnerPos = jsonData.getInt("winnerPos")

        state?.players = players
    }

    override fun write(json: Json) {
        super.write(json)
        json.writeValue("_ninesVersion", VERSION)
        json.writeValue("players", players)
        if (state != null) {
            json.writeValue("state", state)
        }
        json.writeValue("events", events)
        json.writeValue("phase", phase)
        json.writeValue("round", round)
        json.writeValue("dealerPos", dealerPos)
        json.writeValue("winnerPos", winnerPos)
    }

    fun save(json: Json) {
        KtxAsync.launch(Dispatchers.IO) {
            json.toJson(this@Game, GAME_SAVE_FILE)
        }
    }

    enum class Phase {
        /** Game has not started or is done. */
        ENDED,
        /** Game is started but round is done. */
        GAME_STARTED,
        /** Round has started and is being played. */
        ROUND_STARTED
    }

    companion object {
        val VERSION = 1

        /** The game save file location. */
        val GAME_SAVE_FILE = Gdx.files.local("saved-game.json")

        /** Returns whether or not there's a game saved on the disk. */
        val hasSavedGame: Boolean
            get() = GAME_SAVE_FILE.exists()

        /**
         * Load a game instance with [settings] using [json].
         * Calls [onDone] when done loading, and with `null` if loading failed.
         */
        fun load(settings: GamePrefs, json: Json, onDone: (Game?) -> Unit) {
            if (hasSavedGame) {
                KtxAsync.launch(Dispatchers.IO) {
                    onDone(try {
                        val game: Game = json.fromJson(GAME_SAVE_FILE)
                        game.settings = settings
                        game.state?.settings = settings
                        settings.addListener(game)
                        game
                    } catch (e: SerializationException) {
                        // Corrupt game file or version mismatch
                        error(e) { "Could not deserialize saved game." }
                        null
                    })
                }
            }
        }

        private val TRUMP_SUITS = intArrayOf(PCard.HEART, PCard.SPADE,
                PCard.DIAMOND, PCard.SPADE, GameState.NO_TRUMP)
    }
}
