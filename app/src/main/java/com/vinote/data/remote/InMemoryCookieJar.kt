package com.vinote.data.remote

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal in-memory cookie jar that OkHttp uses during login to capture the
 * session cookie issued by the backend. Provided as a Hilt singleton.
 */
@Singleton
class InMemoryCookieJar @Inject constructor() : CookieJar {
    private val store = mutableMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { c ->
            store.getOrPut(url.host) { mutableListOf() }.removeAll { it.name == c.name }
            store.getOrPut(url.host) { mutableListOf() }.add(c)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return store[url.host].orEmpty()
    }

    fun getSessionToken(baseUrl: String): String? {
        val host = baseUrl.toHttpUrl().host
        return store[host].orEmpty()
            .firstOrNull { it.name == "next-auth.session-token" }
            ?.value
    }

    fun clear() {
        store.clear()
    }
}
