package com.babylon.orbit.sample.domain.todo

import com.babylon.orbit.sample.network.TodoService
import io.reactivex.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class GetTodoUseCase(
    private val todoService: TodoService
) {

    suspend fun getTodoList(): Flow<TodoStatus> =
        todoService.getTodo()
            .map { TodoStatus.Result(it) as TodoStatus }
            .onStart { emit(TodoStatus.Loading) }
            .catch { emit(TodoStatus.Failure(it)) }
}
