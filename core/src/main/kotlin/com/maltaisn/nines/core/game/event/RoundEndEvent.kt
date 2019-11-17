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

package com.maltaisn.nines.core.game.event

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.maltaisn.nines.core.game.Trick
import ktx.json.readArrayValue

/**
 * An event for the end of a round.
 * The event saves the round results and tricks played.
 */
class RoundEndEvent() : GameEvent() {

    lateinit var result: List<Float>
        private set

    lateinit var tricks: List<Trick>
        private set

    constructor(result: List<Float>, tricks: List<Trick>) : this() {
        this.result = result
        this.tricks = tricks
    }

    override fun equals(other: Any?) = other === this || other is RoundEndEvent &&
            result == other.result && tricks == other.tricks

    override fun hashCode() = arrayOf(result, tricks).contentHashCode()

    override fun toString() = "Round end [result: $result, tricks: $tricks]"


    override fun read(json: Json, jsonData: JsonValue) {
        result = json.readArrayValue(jsonData, "result")
        tricks = json.readArrayValue(jsonData, "tricks")
    }

    override fun write(json: Json) {
        json.writeValue("result", result)
        json.writeValue("tricks", tricks)
    }
}
