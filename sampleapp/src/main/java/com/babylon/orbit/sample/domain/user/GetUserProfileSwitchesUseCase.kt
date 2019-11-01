package com.babylon.orbit.sample.domain.user

import com.babylon.orbit.sample.network.UserService
import io.reactivex.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class GetUserProfileSwitchesUseCase(
    private val userService: UserService
) {

    suspend fun getUserProfileSwitches(): Flow<UserProfileSwitchesStatus> =
        userService.getUserProfileSwitches()
            .map { UserProfileSwitchesStatus.Result(it) as UserProfileSwitchesStatus}
            .onStart { emit(UserProfileSwitchesStatus.Loading) }
            .catch { emit(UserProfileSwitchesStatus.Failure(it)) }
}
