/*
 * Copyright 2019 Babylon Partners Limited
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.babylon.orbit

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow

@FlowPreview
typealias TransformerFunction<STATE> = (Flow<ActionState<STATE, Any>>, suspend (Any) -> Unit) -> (Flow<(STATE) -> STATE>)

@FlowPreview
@ExperimentalCoroutinesApi
interface Middleware<STATE : Any, SIDE_EFFECT : Any> {
    val initialState: STATE
    val orbits: List<TransformerFunction<STATE>>
    val sideEffect: Flow<SIDE_EFFECT>
}
