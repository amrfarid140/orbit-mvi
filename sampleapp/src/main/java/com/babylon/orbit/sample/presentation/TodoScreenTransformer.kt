package com.babylon.orbit.sample.presentation

import com.babylon.orbit.ActionState
import com.babylon.orbit.sample.domain.todo.GetTodoUseCase
import com.babylon.orbit.sample.domain.user.GetUserProfileSwitchesUseCase
import com.babylon.orbit.sample.domain.user.GetUserProfileUseCase
import com.babylon.orbit.sample.domain.user.UserProfileSwitchesStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@FlowPreview
@ExperimentalCoroutinesApi
class TodoScreenTransformer(
    private val todoUseCase: GetTodoUseCase,
    private val getUserProfileSwitchesUseCase: GetUserProfileSwitchesUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase
) {

    internal fun loadTodos(actions: Flow<ActionState<TodoScreenState, Any>>) =
        actions.flatMapLatest { todoUseCase.getTodoList() }

    internal fun loadUserProfileSwitches(actions: Flow<ActionState<TodoScreenState, TodoScreenAction.TodoUserSelected>>) =
        actions.flatMapLatest { actionState ->
            getUserProfileSwitchesUseCase.getUserProfileSwitches()
                .map { UserProfileExtra(it, actionState.action.userId) }
        }

    internal fun loadUserProfile(actions: Flow<ActionState<TodoScreenState, UserProfileExtra>>) =
        actions.filter { it.action.userProfileSwitchesStatus is UserProfileSwitchesStatus.Result }
            .flatMapLatest { getUserProfileUseCase.getUserProfile(it.action.userId) }
}
