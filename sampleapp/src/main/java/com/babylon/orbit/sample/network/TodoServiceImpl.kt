package com.babylon.orbit.sample.network

import com.babylon.orbit.sample.domain.todo.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class TodoServiceImpl : TodoService {

    @SuppressWarnings("MagicNumber")
    override fun getTodo(): Flow<List<Todo>> {

        return flowOf(
            listOf(
                Todo(1, 1, "first todo"),
                Todo(2, 2, "second todo"),
                Todo(3, 3, "third todo"),
                Todo(4, 4, "fourth todo"),
                Todo(5, 5, "fifth todo"),
                Todo(6, 6, "sixth todo")
            )
        )
    }
}
