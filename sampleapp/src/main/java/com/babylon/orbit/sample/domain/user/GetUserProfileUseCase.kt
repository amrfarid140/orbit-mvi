package com.babylon.orbit.sample.domain.user

import com.babylon.orbit.sample.network.UserService
import io.reactivex.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onErrorResume
import kotlinx.coroutines.flow.onStart
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GetUserProfileUseCase(
    private val userService: UserService
) {

    suspend fun getUserProfile(userId: Int): Flow<UserProfileStatus> =
        userService.getUserProfile(userId)
            .map { UserProfileStatus.Result(it) as UserProfileStatus }
            .onStart { emit(UserProfileStatus.Loading) }
            .catch { emit(UserProfileStatus.Failure(it)) }
}
