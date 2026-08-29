package com.vinote.data.remote

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction for retrieving the current authenticated session token used to
 * call the backend. Concrete implementations obtain the token from
 * Auth.js/NextAuth (Phase 1). The token is the minimum credential required for
 * backend authorization.
 */
@Singleton
class SessionTokenProvider @Inject constructor() {

    @Volatile
    private var token: String? = null

    fun getToken(): String? = token

    fun setToken(newToken: String?) {
        token = newToken
    }

    fun clear() {
        token = null
    }
}
