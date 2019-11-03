package com.babylon.orbit.sample.domain.todo

import com.babylon.orbit.sample.network.TodoService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@FlowPreview
@ExperimentalCoroutinesApi
class GetTodoUseCase(
    private val todoService: TodoService
) {

    fun getTodoList(): Flow<TodoStatus> =
        todoService.getTodo()
            .map { TodoStatus.Result(it) as TodoStatus }
            .onStart { emit(TodoStatus.Loading) }
            .catch { emit(TodoStatus.Failure(it)) }
}
