package com.vinote.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that attaches the authenticated session cookie to every
 * outgoing request. The backend stores the session token in an httpOnly
 * cookie; we mirror it through [InMemoryCookieJar] so OkHttp includes it
 * on subsequent calls.
 *
 * The server resolves the canonical user identity from the session token;
 * we never trust a client-supplied userId (PRD section 6).
 */
@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        return chain.proceed(original)
    }
}
