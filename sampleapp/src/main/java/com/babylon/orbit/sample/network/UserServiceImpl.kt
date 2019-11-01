package com.babylon.orbit.sample.network

import com.babylon.orbit.sample.domain.user.UserProfile
import com.babylon.orbit.sample.domain.user.UserProfileSwitches
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.lang.RuntimeException

class UserServiceImpl : UserService {

    override fun getUserProfileSwitches(): Flow<UserProfileSwitches> {
        return flowOf(UserProfileSwitches(true))
    }

    override fun getUserProfile(userId: Int): Flow<UserProfile> {
        return flow {
            if (userId == 1) {
                emit(UserProfile(1, "Babylon User", "user@babylon.com"))
            } else {
                throw RuntimeException("user with id $userId not found")
            }
        }
    }
}
