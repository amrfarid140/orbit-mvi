package com.babylon.orbit.sample

import com.babylon.orbit.ActionState
import com.babylon.orbit.LifecycleAction
import com.babylon.orbit.sample.domain.todo.GetTodoUseCase
import com.babylon.orbit.sample.domain.todo.TodoStatus
import com.babylon.orbit.sample.presentation.TodoScreenAction
import com.babylon.orbit.sample.presentation.TodoScreenState
import com.babylon.orbit.sample.presentation.TodoScreenTransformer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature

class TodoScreenTransformerSpek : Spek({

    val testCoroutineDispatcher = TestCoroutineDispatcher()
    val testScope = TestCoroutineScope()

    beforeEachTest {
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    Feature("Todo transformers") {
        val mockGetTodoUseCase by memoized { mock<GetTodoUseCase> {
            on { getTodoList() } doReturn  flowOf<TodoStatus>(TodoStatus.Loading)
        }  }
        val transformer by memoized {
            TodoScreenTransformer(mockGetTodoUseCase, mock(), mock())
        }

        listOf(
            LifecycleAction.Created,
            TodoScreenAction.RetryAction
        ).forEach { event ->
            Scenario("load todo transformer is sent a $event") {

                Given("an $event") {}

                When("passing the event named ${event.javaClass} into the transformer") {
                    testScope.launch { transformer.loadTodos(createActionState(TodoScreenState(), event)).collect{ } }
                }

                Then("should trigger the correct action") {
                    verify(mockGetTodoUseCase).getTodoList()
                }
            }
        }
    }

    afterEachTest {
        Dispatchers.resetMain()
        testScope.cleanupTestCoroutines()
    }
})

private fun <ACTION : Any, STATE : Any> createActionState(action: ACTION, state: STATE) =
    flowOf(ActionState(action, state))
