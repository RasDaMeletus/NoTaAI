package com.vinote.data.remote.api

import com.vinote.data.remote.dto.SyncPullRequest
import com.vinote.data.remote.dto.SyncPullResponse
import com.vinote.data.remote.dto.SyncPushRequest
import com.vinote.data.remote.dto.SyncPushResponse
import com.vinote.data.remote.dto.SyncStatusDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Cloud synchronization endpoints. Local-first: Room is the immediate source
 * of truth; sync is eventually consistent. All requests are authenticated and
 * user-scoped via Auth.js/NextAuth (PRD section 8).
 */
interface SyncApi {

    @GET("api/sync/status")
    suspend fun getSyncStatus(): SyncStatusDto

    @POST("api/sync/push")
    suspend fun push(@Body request: SyncPushRequest): SyncPushResponse

    @POST("api/sync/pull")
    suspend fun pull(@Body request: SyncPullRequest): SyncPullResponse
}
