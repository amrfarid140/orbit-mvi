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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

@FlowPreview
@ExperimentalCoroutinesApi
class AndroidOrbitContainer<STATE : Any, SIDE_EFFECT : Any> private constructor(
    private val delegate: BaseOrbitContainer<STATE, SIDE_EFFECT>
) : OrbitContainer<STATE, SIDE_EFFECT> by delegate, CoroutineScope {

    private val job = Job()

    constructor(middleware: Middleware<STATE, SIDE_EFFECT>) : this(BaseOrbitContainer(middleware))

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
}
