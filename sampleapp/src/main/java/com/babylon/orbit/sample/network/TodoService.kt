package com.babylon.orbit.sample.network

import com.babylon.orbit.sample.domain.todo.Todo
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow

interface TodoService {

    fun getTodo(): Flow<List<Todo>>
}
