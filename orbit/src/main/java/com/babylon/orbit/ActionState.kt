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

package com.babylon.orbit

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.scan

@FlowPreview
@ExperimentalCoroutinesApi
data class ActionState<out STATE : Any, out ACTION : Any>(
    val inputState: STATE,
    val action: ACTION
)

@FlowPreview
@ExperimentalCoroutinesApi
fun <STATE : Any, EVENT : Any> Flow<ActionState<STATE, *>>.buildOrbitFlow(
    middleware: Middleware<STATE, EVENT>,
    inputRelay: SendChannel<Any>
): Flow<STATE> {
    return flowOf(*middleware.orbits.map { transformer -> transformer(this, inputRelay) }.toTypedArray())
        .flattenMerge()
        .scan(middleware.initialState) { currentState, partialReducer -> partialReducer(currentState) }
        .distinctUntilChanged()
}