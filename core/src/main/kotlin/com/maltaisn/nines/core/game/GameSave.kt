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

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import com.maltaisn.cardgame.addClassTag
import com.maltaisn.cardgame.core.GameResult
import com.maltaisn.cardgame.core.PCard
import com.maltaisn.cardgame.setSerializer


val gameSaveJson = Json().apply {
    setOutputType(JsonWriter.OutputType.javascript)
    setUsePrototypes(false)
    setEnumNames(true)
    setTypeName("type")

    // Add class tags
    addClassTag<GameState>("state")
    addClassTag<GameResult>("result")
    addClassTag<Trick>("trick")
    addClassTag<Hand>("hand")
    addClassTag<GameEvent.RoundStart>("roundStart")
    addClassTag<TradeHandMove>("tradeHandMove")
    addClassTag<PlayMove>("playMove")
    addClassTag<HumanPlayer>("humanPlayer")
    addClassTag<MctsPlayer>("mctsPlayer")

    // Register custom serializers
    setSerializer(PCard.JsonSerializer)
    setSerializer(GameEventSerializer(GameEvent.Start, "start"))
    setSerializer(GameEventSerializer(GameEvent.End, "end"))
    setSerializer(GameEventSerializer(GameEvent.RoundEnd, "roundEnd"))
}

private class GameEventSerializer<T : GameEvent>(val event: T, val value: String) : Json.Serializer<T> {
    override fun write(json: Json, obj: T, knownType: Class<*>?) = json.writeValue(value)
    override fun read(json: Json, jsonData: JsonValue, type: Class<*>?) = event
}