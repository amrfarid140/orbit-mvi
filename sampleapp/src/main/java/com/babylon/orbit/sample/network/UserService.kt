package com.babylon.orbit.sample.network

import com.babylon.orbit.sample.domain.user.UserProfile
import com.babylon.orbit.sample.domain.user.UserProfileSwitches
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow

interface UserService {

    fun getUserProfileSwitches(): Flow<UserProfileSwitches>

    fun getUserProfile(userId: Int): Flow<UserProfile>
}
