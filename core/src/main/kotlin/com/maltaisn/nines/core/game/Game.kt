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

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.SerializationException
import com.maltaisn.cardgame.game.CardGame
import com.maltaisn.cardgame.game.event.CardGameMove
import com.maltaisn.cardgame.game.player.CardPlayer
import com.maltaisn.cardgame.pcard.PCard
import com.maltaisn.cardgame.prefs.GamePrefs
import com.maltaisn.nines.core.PrefKeys
import com.maltaisn.nines.core.game.event.*
import com.maltaisn.nines.core.game.player.AiPlayer
import com.maltaisn.nines.core.game.player.Player
import kotlinx.coroutines.*
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import ktx.async.onRenderingThread
import ktx.json.fromJson
import ktx.json.readArrayValue
import ktx.json.readValue
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
    var phase = Phase.NOT_STARTED
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
        private set

    /** Whether the game is done or not. */
    val isDone: Boolean
        get() = winnerPos != CardPlayer.NO_POSITION

    /**
     * The position of the leading player in the game.
     * If there's a tie in scores, this returns [CardPlayer.NO_POSITION].
     */
    val leaderPos: Int
        get() = findLowestScorePosition(players.map { it.score })

    val gameSpeedDelay: Float
        get() = when (settings.getChoice(PrefKeys.GAME_SPEED)) {
            "slow" -> 1f
            "normal" -> 0.5f
            "fast" -> 0.25f
            else -> 0f
        }

    /**
     * Listeners called when a game event happens.
     * Note that the listeners are called at a point where the events have already happened
     * so the game and the state are already changed. Everything must take this into account.
     * When a move is made by player at pos 0, `state.posToMove` will be 1 not 0 when the
     * event listeners are called, since the state was changed already.
     */
    val eventListeners = mutableListOf<(GameEvent) -> Unit>()

    /** System time when the last move was made. */
    private var lastMoveTime = 0L

    private val dispatcher = newSingleThreadAsyncContext()
    private var aiPlayerJob: Job? = null


    constructor(settings: GamePrefs, south: Player, west: Player, north: Player) : this() {
        this.settings = settings
        players = listOf(south, west, north)
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
            if (next is AiPlayer) {
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
        check(phase == Phase.NOT_STARTED)
        phase = Phase.ROUND_ENDED

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

    /** Start a new round. */
    fun startRound() {
        check(phase == Phase.ROUND_ENDED)
        phase = Phase.ROUND_STARTED

        round++
        dealerPos = (dealerPos + 1) % 3

        val state = GameState(settings, players, dealerPos, trumpSuit)
        this.state = state

        doEvent(RoundStartEvent(trumpSuit,
                players.map { it.hand.clone() } + state.extraHand.clone()))
    }

    /** End the current round. */
    fun endRound() {
        check(phase == Phase.ROUND_STARTED)
        phase = Phase.ROUND_ENDED

        val state = state!!

        // Update the scores
        val tricksTaken = players.map { it.tricksTaken }
        for (player in players) {
            player.score += MINIMUM_TRICKS - player.tricksTaken
        }

        // Check if any player has won
        val leader = players.getOrNull(leaderPos)
        if (leader != null && leader.score <= 0) {
            winnerPos = leader.position
        }

        doEvent(RoundEndEvent(tricksTaken, state.tricksPlayed))

        this.state = null

        if (winnerPos != CardPlayer.NO_POSITION) {
            end()
        }
    }

    /** End the game. */
    fun end() {
        check(phase == Phase.ROUND_ENDED)
        phase = Phase.ENDED

        doEvent(EndEvent())
    }

    /** Do a [move] on the game state. */
    fun doMove(move: CardGameMove) {
        check(phase == Phase.ROUND_STARTED)

        state?.doMove(move)
        doEvent(move as MoveEvent)
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
        for (listener in eventListeners) {
            listener.invoke(event)
        }
    }

    override fun dispose() {
        eventListeners.clear()

        cancelAiTurn()
        dispatcher.dispose()
    }

    override fun toString() = "[${events.size} events, phase: $phase, round: $round, " +
            "dealerPos: $dealerPos, trumpSuit: ${(PCard.SUIT_STR.getOrNull(trumpSuit) ?: "none")}]"


    override fun read(json: Json, jsonData: JsonValue) {
        json as GameSaveJson
        super.read(json, jsonData)

        val version = jsonData.getInt("_gameVersion")
        json.gameVersion = version
        if (version != VERSION || json.version != CardGame.VERSION) {
            // Version of saved game doesn't match game version.
            // Current behavior is to throw an exception to abort.
            throw SerializationException("Game version mismatch: " +
                    "file is $version/${json.version} and current is $VERSION/${CardGame.VERSION}")
        }

        players = json.readArrayValue(jsonData, "players")
        state = json.readValue(jsonData, "state")
        _events += json.readArrayValue<ArrayList<GameEvent>, GameEvent>(jsonData, "events")
        phase = json.readValue(jsonData, "phase")
        round = jsonData.getInt("round")
        dealerPos = jsonData.getInt("dealerPos")
        winnerPos = jsonData.getInt("winnerPos")

        state?.players = players
    }

    override fun write(json: Json) {
        super.write(json)
        json.writeValue("_gameVersion", VERSION)
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

    fun save(file: FileHandle, json: Json) {
        KtxAsync.launch(Dispatchers.IO) {
            json.toJson(this@Game, file)
        }
    }

    enum class Phase {
        /** Gmae has not yet started. */
        NOT_STARTED,
        /** Game has started but no round is being played. */
        ROUND_ENDED,
        /** Round has started and is being played. */
        ROUND_STARTED,
        /** Game is done, a player has won. */
        ENDED
    }

    companion object {
        const val VERSION = 1

        /**
         * Load a game instance with [settings] using [json].
         * Calls [onDone] when done loading, and with `null` if loading failed.
         */
        fun load(file: FileHandle, settings: GamePrefs, json: Json, onDone: (Game?) -> Unit) {
            KtxAsync.launch(Dispatchers.IO) {
                onDone(try {
                    val game: Game = json.fromJson(file)!!
                    game.settings = settings
                    game.state?.settings = settings
                    game
                } catch (e: Exception) {
                    // Corrupt game file or version mismatch
                    error(e) { "Could not deserialize saved game." }
                    null
                })
            }
        }

        /**
         * Find the position with the lowest score in a [scores] list.
         * Returns [CardPlayer.NO_POSITION] if there's a tie.
         */
        fun findLowestScorePosition(scores: List<Int>): Int {
            var minIndex = 0
            var tie = false
            for (i in 1 until scores.size) {
                val score = scores[i]
                val min = scores[minIndex]
                if (score < min) {
                    minIndex = i
                    tie = false
                } else if (score == min) {
                    tie = true
                }
            }
            return if (tie) CardPlayer.NO_POSITION else minIndex
        }

        private val TRUMP_SUITS = intArrayOf(PCard.HEART, PCard.SPADE,
                PCard.DIAMOND, PCard.CLUB, GameState.NO_TRUMP)

        /** The minimum number of tricks to decrease the score. */
        const val MINIMUM_TRICKS = (GameState.CARDS_COUNT - 1) / 3
    }
}
