package com.babylon.orbit

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/*
What do we want to log:
- which flow was triggered
- by what action & state combination
- any outcome (new state emitted, something looped back etc.)
 */

@DslMarker
annotation class OrbitDsl

@ExperimentalCoroutinesApi
@FlowPreview
fun <STATE : Any, SIDE_EFFECT : Any> middleware(
    initialState: STATE,
    init: OrbitsBuilder<STATE, SIDE_EFFECT>.() -> Unit
): Middleware<STATE, SIDE_EFFECT> {

    return OrbitsBuilder<STATE, SIDE_EFFECT>(initialState).apply {
        init(this)
    }.build()
}

@ExperimentalCoroutinesApi
@FlowPreview
@OrbitDsl
open class OrbitsBuilder<STATE : Any, SIDE_EFFECT : Any>(private val initialState: STATE) {
    // Since this caches unconsumed events we restrict it to one subscriber at a time
    protected val sideEffectSubject= ConflatedBroadcastChannel<SIDE_EFFECT>()

    private val orbits = mutableListOf<TransformerFunction<STATE>>()

    @Suppress("unused") // Used for the nice extension function highlight
    fun OrbitsBuilder<STATE, SIDE_EFFECT>.perform(description: String) = ActionFilter(description)

    inline fun <reified ACTION : Any> ActionFilter.on() =
        this@OrbitsBuilder.FirstTransformer<ACTION> { it.ofActionType() }

    @Suppress("unused") // Used for the nice extension function highlight
    fun ActionFilter.on(vararg classes: Class<*>) = this@OrbitsBuilder.FirstTransformer { actions ->
        flowOf(*classes.map {
                elementClass -> actions.filter { elementClass.isInstance(it.action) }
        }.toTypedArray()).flattenMerge()
    }

    @Suppress("unused")
    @OrbitDsl
    inner class ActionFilter(private val description: String) {
        inline fun <reified ACTION : Any> Flow<ActionState<STATE, *>>.ofActionType(): Flow<ActionState<STATE, ACTION>> =
            filter { it.action is ACTION }.map { ActionState(it.inputState, it.action as ACTION) }
    }

    @OrbitDsl
    inner class FirstTransformer<ACTION : Any>(
        private val upstreamTransformer: (Flow<ActionState<STATE, *>>) -> Flow<ActionState<STATE, ACTION>>
    ) {

        fun <T : Any> transform(transformer: Flow<ActionState<STATE, ACTION>>.() -> Flow<T>) =
                this@OrbitsBuilder.Transformer { rawActions ->
                    transformer(upstreamTransformer(rawActions))
                }

        fun postSideEffect(sideEffect: ActionState<STATE, ACTION>.() -> SIDE_EFFECT) =
            sideEffectInternal {
                this@OrbitsBuilder.sideEffectSubject.send(
                    it.sideEffect()
                )
            }

        fun sideEffect(sideEffect: ActionState<STATE, ACTION>.() -> Unit) =
            sideEffectInternal {
                it.sideEffect()
            }

        private fun sideEffectInternal(sideEffect: suspend (ActionState<STATE, ACTION>) -> Unit) =
            this@OrbitsBuilder.FirstTransformer { rawActions ->
                upstreamTransformer(rawActions)
                    .onEach {
                        sideEffect(it)
                    }
            }

        fun withReducer(reducer: ReducerReceiver<STATE, ACTION>.() -> STATE) {
            this@OrbitsBuilder.orbits += { rawActions, _ ->
                upstreamTransformer(rawActions)
                    .map {
                        { state: STATE ->
                            ReducerReceiver(state, it.action).reducer()
                        }
                    }
            }
        }

        fun ignoringEvents() {
            this@OrbitsBuilder.orbits += { upstream, _ ->
                upstreamTransformer(upstream)
                    .map {
                        { state: STATE -> state }
                    }
            }
        }
    }

    @OrbitDsl
    inner class Transformer<EVENT : Any>(private val upstreamTransformer: (Flow<ActionState<STATE, *>>) -> Flow<EVENT>) {

        fun <T : Any> transform(transformer: Flow<EVENT>.() -> Flow<T>) =
            this@OrbitsBuilder.Transformer { rawActions ->
                transformer(upstreamTransformer(rawActions))
            }

        fun postSideEffect(sideEffect: EventReceiver<EVENT>.() -> SIDE_EFFECT) =
            sideEffectInternal {
                this@OrbitsBuilder.sideEffectSubject.send(EventReceiver(it).sideEffect())
            }

        fun sideEffect(sideEffect: EventReceiver<EVENT>.() -> Unit) =
            sideEffectInternal {
                EventReceiver(it).sideEffect()
            }

        private fun sideEffectInternal(sideEffect: suspend (EVENT) -> Unit) =
            this@OrbitsBuilder.Transformer { rawActions ->
                upstreamTransformer(rawActions)
                    .onEach {
                        sideEffect(it)
                    }
            }

        fun <T : Any> loopBack(mapper: EventReceiver<EVENT>.() -> T) {
            this@OrbitsBuilder.orbits += { upstream, inputRelay ->
                upstreamTransformer(upstream)
                    .onEach { action -> inputRelay(EventReceiver(action).mapper()) }
                    .map {
                        { state: STATE -> state }
                    }
            }
        }

        fun ignoringEvents() {
            this@OrbitsBuilder.orbits += { upstream, _ ->
                upstreamTransformer(upstream)
                    .map {
                        { state: STATE -> state }
                    }
            }
        }

        fun withReducer(reducer: ReducerReceiver<STATE, EVENT>.() -> STATE) {
            this@OrbitsBuilder.orbits += { rawActions, _ ->
                upstreamTransformer(rawActions)
                    .map {
                        { state: STATE ->
                            ReducerReceiver(state, it).reducer()
                        }
                    }
            }
        }
    }

    fun build() = object : Middleware<STATE, SIDE_EFFECT> {
        override val initialState: STATE = this@OrbitsBuilder.initialState
        override val orbits: List<TransformerFunction<STATE>> = this@OrbitsBuilder.orbits
        override val sideEffect: Flow<SIDE_EFFECT> = sideEffectSubject.asFlow()
    }
}

class ReducerReceiver<STATE : Any, EVENT : Any>(
    val currentState: STATE,
    val event: EVENT
)

class EventReceiver<EVENT : Any>(
    val event: EVENT
)
