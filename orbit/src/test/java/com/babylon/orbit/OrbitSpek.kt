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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature

@ExperimentalCoroutinesApi
@FlowPreview
internal class OrbitSpek : Spek({

    val testCoroutineDispatcher = TestCoroutineDispatcher()
    val testScope = TestCoroutineScope()

    beforeEachTest {
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    fun <T : Any> Flow<T>.startCollecting(result: MutableList<T>) = testScope.launch {
        collect { result.add(it) }
    }


    Feature("Orbit DSL") {

        Scenario("no flows") {
            lateinit var middleware: Middleware<State, String>
            lateinit var orbitContainer: BaseOrbitContainer<State, String>
            val result = mutableListOf<State>()

            Given("A middleware with no flows") {
                middleware = createTestMiddleware {}
                orbitContainer = BaseOrbitContainer(middleware)
            }

            When("connecting to the middleware") {
                runBlocking { orbitContainer.orbit.collect { result.add(it) } }
            }

            Then("emits the initial state") {
                assertThat(result).isEqualTo(listOf(middleware.initialState))
            }
        }

        Scenario("a flow that reduces an action") {
            lateinit var middleware: Middleware<State, String>
            lateinit var orbitContainer: BaseOrbitContainer<State, String>
            val result = mutableListOf<State>()

            Given("A middleware with one reducer flow") {
                middleware = createTestMiddleware {
                    perform("something")
                        .on<Int>()
                        .withReducer { State(currentState.id + event) }
                }
                orbitContainer = BaseOrbitContainer(middleware)
            }

            When("sending an action") {
                orbitContainer.orbit.startCollecting(result)
                runBlocking { orbitContainer.sendAction(5) }
            }

            Then("produces a correct end state") {
                assertThat(result).isEqualTo(listOf(State(42), State(47)))
            }
        }

        Scenario("a flow with a transformer and reducer") {
            lateinit var middleware: Middleware<State, String>
            lateinit var orbitContainer: BaseOrbitContainer<State, String>
            val result = mutableListOf<State>()

            Given("A middleware with a transformer and reducer") {
                middleware = createTestMiddleware {
                    perform("something")
                        .on<Int>()
                        .transform { map { it.action * 2 } }
                        .withReducer { State(currentState.id + event) }
                }
                orbitContainer = BaseOrbitContainer(middleware)
            }

            When("sending an action") {
                orbitContainer.orbit.startCollecting(result)
                runBlocking { orbitContainer.sendAction(5) }
            }

            Then("produces a correct end state") {
                assertThat(result).isEqualTo(listOf(State(42), State(52)))
            }
        }

        Scenario("a flow with two transformers and a reducer") {
            lateinit var middleware: Middleware<State, String>
            lateinit var orbitContainer: BaseOrbitContainer<State, String>
            val result = mutableListOf<State>()

            Given("A middleware with two transformers and a reducer") {
                middleware = createTestMiddleware {
                    perform("something")
                        .on<Int>()
                        .transform { map { it.action * 2 } }
                        .transform { map { it * 2 } }
                        .withReducer { State(currentState.id + event) }
                }
                orbitContainer = BaseOrbitContainer(middleware)
            }

            When("sending an action") {
                orbitContainer.orbit.startCollecting(result)
                runBlocking { orbitContainer.sendAction(5) }
            }

            Then("produces a correct end state") {
                assertThat(result).isEqualTo(listOf(State(42), State(62)))
            }
        }

        Scenario("a flow with two transformers that is ignored") {
            lateinit var middleware: Middleware<State, String>
            lateinit var orbitContainer: BaseOrbitContainer<State, String>
            val result = mutableListOf<State>()

            Given("A middleware with two transformer flows") {
                middleware = createTestMiddleware {
                    perform("something")
                        .on<Int>()
                        .transform { map { it.action * 2 } }
                        .transform { map { it * 2 } }
                        .ignoringEvents()

                    perform("unlatch")
                        .on<Int>()
                        .transform {
                            this
                        }
                        .ignoringEvents()
                }
                orbitContainer = BaseOrbitContainer(middleware)
            }

            When("sending an action") {
                orbitContainer.orbit.startCollecting(result)
                runBlocking { orbitContainer.sendAction(5) }
            }

            Then("emits just the initial state after connecting") {
                assertThat(result).isEqualTo(listOf(State(42)))
            }
        }

        Scenario("a flow with a transformer loopback and a flow with a transformer and reducer") {
            data class IntModified(val value: Int)

            lateinit var middleware: Middleware<State, String>
            lateinit var orbitContainer: BaseOrbitContainer<State, String>
            val result = mutableListOf<State>()

            Given("A middleware with a transformer loopback flow and transform/reduce flow") {
                middleware = createTestMiddleware {
                    perform("something")
                        .on<Int>()
                        .transform { map { it.action * 2 } }
                        .loopBack {
                            IntModified(event)
                        }

                    perform("something")
                        .on<IntModified>()
                        .transform { map { it.action.value * 2 } }
                        .withReducer { State(currentState.id + event) }
                }
                orbitContainer = BaseOrbitContainer(middleware)
            }

            When("sending an action") {
                orbitContainer.orbit.startCollecting(result)
                runBlocking { orbitContainer.sendAction(5) }
            }

            Then("produces a correct end state") {
                assertThat(result).isEqualTo(listOf(State(42), State(62)))
            }
        }

        Scenario("a flow with two transformers with reducers") {
            lateinit var middleware: Middleware<State, String>
            lateinit var orbitContainer: BaseOrbitContainer<State, String>
            val result = mutableListOf<State>()

            fun myReducer(event: Int): State {
                return State(event)
            }

            Given("A middleware with two transform/reduce flows") {
                middleware = createTestMiddleware {
                    perform("something")
                        .on<Int>()
                        .transform { map { it.action * 2 } }
                        .withReducer { myReducer(event) }

                    perform("something")
                        .on<Int>()
                        .transform { map { it.action + 2 } }
                        .withReducer { State(event) }
                }
                orbitContainer = BaseOrbitContainer(middleware)
            }

            When("sending an action") {
                orbitContainer.orbit.startCollecting(result)
                runBlocking { orbitContainer.sendAction(5) }
            }

            Then("produces a correct series of states") {
                assertThat(result).isEqualTo(listOf(State(42), State(10), State(7)))
            }
        }

        Scenario("a flow with three transformers with reducers") {

            class One
            class Two
            class Three

            lateinit var middleware: Middleware<State, String>
            lateinit var orbitContainer: BaseOrbitContainer<State, String>
            val result = mutableListOf<State>()
            val expectedOutput = mutableListOf(State(0))

            Given("A middleware with three transform/reduce flows") {
                middleware = createTestMiddleware(State(0)) {
                    perform("one")
                        .on<One>()
                        .withReducer { State(1) }

                    perform("two")
                        .on<Two>()
                        .withReducer { State(2) }

                    perform("three")
                        .on<Three>()
                        .withReducer { State(3) }
                }
                orbitContainer = BaseOrbitContainer(middleware)
            }

            When("sending actions") {
                orbitContainer.orbit.startCollecting(result)
                for (i in 0 until 99) {
                    val value = (i % 3)
                    expectedOutput.add(State(value + 1))
                    runBlocking {
                        orbitContainer.sendAction(
                            when (value) {
                                0 -> One()
                                1 -> Two()
                                2 -> Three()
                                else -> throw IllegalStateException("misconfigured test")
                            }
                        )
                    }
                }
            }

            Then("produces a correct series of states") {
                assertThat(result).isEqualTo(expectedOutput)
            }
        }

        Scenario("posting side effects as first transformer") {
            lateinit var middleware: Middleware<State, String>
            lateinit var orbitContainer: BaseOrbitContainer<State, String>
            val result = mutableListOf<State>()
            val sideEffects = mutableListOf<String>()

            Given("A middleware with a single post side effect as the first transformer") {
                middleware = createTestMiddleware(State(1)) {
                    perform("something")
                        .on<Int>()
                        .postSideEffect { "${inputState.id + action}" }
                        .withReducer { currentState.copy(id = currentState.id + 1) }
                }
                orbitContainer = BaseOrbitContainer(middleware)
            }

            When("sending actions") {
                orbitContainer.orbit.startCollecting(result)
                orbitContainer.sideEffect.startCollecting(sideEffects)

                runBlocking { orbitContainer.sendAction(5) }

            }

            Then("produces a correct series of states") {
                assertThat(result).isEqualTo(listOf(State(1), State(2)))
            }

            And("posts a correct series of side effects") {
                assertThat(sideEffects).isEqualTo(listOf("6"))
            }
        }

        Scenario("posting side effects as non-first transformer") {
            lateinit var middleware: Middleware<State, String>
            lateinit var orbitContainer: BaseOrbitContainer<State, String>
            val result = mutableListOf<State>()
            val sideEffects = mutableListOf<String>()

            Given("A middleware with a single post side effect as the second transformer") {
                middleware = createTestMiddleware(State(1)) {
                    perform("something")
                        .on<Int>()
                        .transform { map { it.action * 2 } }
                        .postSideEffect { event.toString() }
                        .withReducer { currentState.copy(id = currentState.id + 1) }
                }
                orbitContainer = BaseOrbitContainer(middleware)
            }

            When("sending actions") {
                orbitContainer.orbit.startCollecting(result)
                orbitContainer.sideEffect.startCollecting(sideEffects)

                runBlocking { orbitContainer.sendAction(5) }
            }

            Then("produces a correct series of states") {
                assertThat(result).isEqualTo(listOf(State(1), State(2)))
            }

            And("posts a correct series of side effects") {
                assertThat(sideEffects).isEqualTo(listOf("10"))
            }
        }

        Scenario("non-posting side effects as first transformer") {
            lateinit var middleware: Middleware<State, String>
            lateinit var orbitContainer: BaseOrbitContainer<State, String>
            val result = mutableListOf<State>()
            val sideEffects = mutableListOf<String>()
            val sideEffectList = mutableListOf<String>()

            Given("A middleware with a single side effect as the first transformer") {
                middleware = createTestMiddleware(State(1)) {
                    perform("something")
                        .on<Int>()
                        .sideEffect { sideEffectList.add("${inputState.id + action}") }
                        .withReducer { currentState.copy(id = currentState.id + 1) }
                }
                orbitContainer = BaseOrbitContainer(middleware)
            }

            When("sending actions") {
                orbitContainer.orbit.startCollecting(result)
                orbitContainer.sideEffect.startCollecting(sideEffects)
                runBlocking { orbitContainer.sendAction(5) }
            }

            Then("produces a correct series of states") {
                assertThat(result).isEqualTo(listOf(State(1), State(2)))
            }

            And("posts no side effects") {
                assertThat(sideEffects.isEmpty()).isTrue()
            }

            And("the side effect is executed") {
                assertThat(sideEffectList).containsExactly("6")
            }
        }

        Scenario("non-posting side effects as non-first transformer") {
            lateinit var middleware: Middleware<State, String>
            lateinit var orbitContainer: BaseOrbitContainer<State, String>
            val result = mutableListOf<State>()
            val sideEffects = mutableListOf<String>()
            val sideEffectList = mutableListOf<String>()

            Given("A middleware with a single side effect as the second transformer") {
                middleware = createTestMiddleware(State(1)) {
                    perform("something")
                        .on<Int>()
                        .transform { map { it.action * 2 } }
                        .sideEffect { sideEffectList.add(event.toString()) }
                        .withReducer { currentState.copy(id = currentState.id + 1) }
                }
                orbitContainer = BaseOrbitContainer(middleware)
            }

            When("sending actions") {
                orbitContainer.orbit.startCollecting(result)
                orbitContainer.sideEffect.startCollecting(sideEffects)
                runBlocking { orbitContainer.sendAction(5) }
            }

            Then("produces a correct series of states") {
                assertThat(result).isEqualTo(listOf(State(1), State(2)))
            }

            And("posts no side effects") {
                assertThat(sideEffects.isEmpty()).isTrue()
            }

            And("the side effect is executed") {
                assertThat(sideEffectList).containsExactly("10")
            }
        }

    }

    afterEachTest {
        Dispatchers.resetMain()
        testScope.cleanupTestCoroutines()
    }
})

@ExperimentalCoroutinesApi
@FlowPreview
private fun createTestMiddleware(
    initialState: State = State(42),
    block: OrbitsBuilder<State, String>.() -> Unit
) = middleware<State, String>(initialState) {
    this.apply(block)
}

private data class State(val id: Int)
