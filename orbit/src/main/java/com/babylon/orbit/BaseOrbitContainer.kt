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
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@FlowPreview
@ExperimentalCoroutinesApi
class BaseOrbitContainer<STATE : Any, SIDE_EFFECT : Any>(
    middleware: Middleware<STATE, SIDE_EFFECT>
) : OrbitContainer<STATE, SIDE_EFFECT> {

    private val state = ConflatedBroadcastChannel(middleware.initialState).asFlow()
    // TODO(Fix loopback issues when channel is conflated)
    private val input = BroadcastChannel<Any>(Channel.CONFLATED) // Creates a Buffered Broadcast channel with default buffer size
    override val sideEffect: Flow<SIDE_EFFECT> = middleware.sideEffect
    override val orbit: Flow<STATE>

    init {
        input.sendBlocking(LifecycleAction.Created)
        orbit = input.asFlow().flatMapLatest { input ->
            state.map { state ->
                ActionState(state, input)
            }
        }.buildOrbitFlow(middleware) {
            input.send(it)
        }
    }

    override suspend fun sendAction(action: Any) {
        input.send(action)
    }

    override fun disposeOrbit() {
        input.cancel()
    }
}
