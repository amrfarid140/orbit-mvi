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

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

@FlowPreview
@ExperimentalCoroutinesApi
abstract class OrbitViewModel<STATE : Any, SIDE_EFFECT : Any>(
    middleware: Middleware<STATE, SIDE_EFFECT>
) : ViewModel(), CoroutineScope {

    constructor(
        initialState: STATE,
        init: OrbitsBuilder<STATE, SIDE_EFFECT>.() -> Unit
    ) : this(middleware(initialState, init))

    override val coroutineContext: CoroutineContext = Dispatchers.IO
    private val container: AndroidOrbitContainer<STATE, SIDE_EFFECT> = AndroidOrbitContainer(middleware)

    /**
     * Designed to be called in onStart or onResume, depending on your use case.
     * DO NOT call in other lifecycle methods unless you know what you're doing!
     * The subscriptions will be disposed in methods symmetric to the ones they were called in.
     * For example onStart -> onStop, onResume -> onPause, onCreate -> onDestroy.
     */
    fun connect(
        stateConsumer: (STATE) -> Unit,
        sideEffectConsumer: (SIDE_EFFECT) -> Unit = {}
    ) = launch {
        container.orbit.collect {
            withContext(Dispatchers.Main) { stateConsumer(it) }
        }
        container.sideEffect.collect {
            withContext(Dispatchers.Main) { sideEffectConsumer(it) }
        }
    }

    fun sendAction(action: Any) = launch {
        container.sendAction(action)
    }

    override fun onCleared() {
        cancel()
        container.disposeOrbit()
    }
}
