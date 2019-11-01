package com.babylon.orbit

import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.Observable
import io.reactivex.rxkotlin.cast
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.Subject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.observeOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/*
What do we want to log:
- which flow was triggered
- by what action & state combination
- any outcome (new state emitted, something looped back etc.)
 */

@OrbitDsl
open class OrbitsBuilderFlow<STATE : Any, SIDE_EFFECT : Any>(private val initialState: STATE) {
    // Since this caches unconsumed events we restrict it to one subscriber at a time
    protected val sideEffectSubject= ConflatedBroadcastChannel<SIDE_EFFECT>()

    private val orbits = mutableListOf<TransformerFunctionFlow<STATE>>()

    @Suppress("unused") // Used for the nice extension function highlight
    fun OrbitsBuilder<STATE, SIDE_EFFECT>.perform(description: String) = ActionFilter(description)

    inline fun <reified ACTION : Any> ActionFilter.on() =
        this@OrbitsBuilderFlow.FirstTransformer<ACTION> { it.ofActionType() }

    @Suppress("unused") // Used for the nice extension function highlight
    fun ActionFilter.on(vararg classes: Class<*>) = this@OrbitsBuilderFlow.FirstTransformer { actions ->
        flowOf(*classes.map {
                elementClass -> actions.filter { elementClass.isInstance(it.action) }
        }.toTypedArray()).flattenMerge()
    }

    @OrbitDsl
    inner class ActionFilter(private val description: String) {
        inline fun <reified ACTION : Any> Flow<ActionState<STATE, *>>.ofActionType(): Flow<ActionState<STATE, ACTION>> =
            filter { it.action is ACTION }.map { ActionState(it.inputState, it.action as ACTION) }
    }

    @OrbitDsl
    inner class FirstTransformer<ACTION : Any>(
        private val upstreamTransformer: (Flow<ActionState<STATE, *>>) -> Flow<ActionState<STATE, ACTION>>
    ) {

        suspend fun <T : Any> transform(transformer: Flow<ActionState<STATE, ACTION>>.() -> Flow<T>) =
            withContext(Dispatchers.IO) {
                this@OrbitsBuilderFlow.Transformer { rawActions ->
                    transformer(upstreamTransformer(rawActions))
                }
            }


        suspend fun postSideEffect(sideEffect: ActionState<STATE, ACTION>.() -> SIDE_EFFECT) =
            sideEffectInternal {
                this@OrbitsBuilderFlow.sideEffectSubject.send(
                    it.sideEffect()
                )
            }

        suspend fun sideEffect(sideEffect: ActionState<STATE, ACTION>.() -> Unit) =
            sideEffectInternal {
                it.sideEffect()
            }

        private suspend fun sideEffectInternal(sideEffect: suspend (ActionState<STATE, ACTION>) -> Unit) =
            this@OrbitsBuilderFlow.FirstTransformer { rawActions ->
                upstreamTransformer(rawActions)
                    .onEach {
                        sideEffect(it)
                    }
            }

        fun withReducer(reducer: ReducerReceiver<STATE, ACTION>.() -> STATE) {
            this@OrbitsBuilderFlow.orbits += { rawActions, _ ->
                upstreamTransformer(rawActions)
                    .map {
                        { state: STATE ->
                            ReducerReceiver(state, it.action).reducer()
                        }
                    }
            }
        }

        fun ignoringEvents() {
            this@OrbitsBuilderFlow.orbits += { upstream, _ ->
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
            this@OrbitsBuilderFlow.Transformer { rawActions ->
                transformer(upstreamTransformer(rawActions))
            }

        fun postSideEffect(sideEffect: EventReceiver<EVENT>.() -> SIDE_EFFECT) =
            sideEffectInternal {
                this@OrbitsBuilderFlow.sideEffectSubject.send(EventReceiver(it).sideEffect())
            }

        fun sideEffect(sideEffect: EventReceiver<EVENT>.() -> Unit) =
            sideEffectInternal {
                EventReceiver(it).sideEffect()
            }

        private fun sideEffectInternal(sideEffect: suspend (EVENT) -> Unit) =
            this@OrbitsBuilderFlow.Transformer { rawActions ->
                upstreamTransformer(rawActions)
                    .onEach {
                        sideEffect(it)
                    }
            }

        fun <T : Any> loopBack(mapper: EventReceiver<EVENT>.() -> T) {
            this@OrbitsBuilderFlow.orbits += { upstream, inputRelay ->
                upstreamTransformer(upstream)
                    .onEach { action -> inputRelay.onNext(EventReceiver(action).mapper()) }
                    .map {
                        { state: STATE -> state }
                    }
            }
        }

        fun ignoringEvents() {
            this@OrbitsBuilderFlow.orbits += { upstream, _ ->
                upstreamTransformer(upstream)
                    .map {
                        { state: STATE -> state }
                    }
            }
        }

        fun withReducer(reducer: ReducerReceiver<STATE, EVENT>.() -> STATE) {
            this@OrbitsBuilderFlow.orbits += { rawActions, _ ->
                upstreamTransformer(rawActions)
                    .map {
                        { state: STATE ->
                            ReducerReceiver(state, it).reducer()
                        }
                    }
            }
        }
    }

    fun build() = object : MiddlewareFlow<STATE, SIDE_EFFECT> {
        override val initialState: STATE = this@OrbitsBuilderFlow.initialState
        override val orbits: List<TransformerFunctionFlow<STATE>> = this@OrbitsBuilderFlow.orbits
        override val sideEffect: Flow<SIDE_EFFECT> = sideEffectSubject.asFlow()
    }
}
