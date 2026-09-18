package com.vinote.data.supabase

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Provides a singleton Supabase client for the app.
 * URL and anon key are read from local.properties; if missing, the provider
 * falls back to a stub that throws on use so the app still compiles.
 */
class SupabaseClientProvider(private val appContext: Context) {

    val isConfigured: Boolean by lazy {
        val url = readLocalProperty("supabase.url")
        val key = readLocalProperty("supabase.anon.key")
        !url.isNullOrBlank() && !key.isNullOrBlank() &&
                !url.contains("placeholder") && !key.contains("placeholder")
    }

    val clientOrNull: SupabaseClient? by lazy {
        try {
            val url = readLocalProperty("supabase.url")
            val key = readLocalProperty("supabase.anon.key")
            if (url.isNullOrBlank() || key.isNullOrBlank() || url.contains("placeholder")) {
                null
            } else {
                createSupabaseClient(
                    supabaseUrl = url,
                    supabaseKey = key
                ) {
                    install(Postgrest)
                    install(Auth)
                    install(Realtime)
                    install(Storage)
                }
            }
        } catch (t: Throwable) {
            android.util.Log.w("SupabaseClientProvider", "Failed to initialize Supabase client: ${t.message}")
            null
        }
    }

    val client: SupabaseClient
        get() = clientOrNull ?: throw IllegalStateException("Supabase is not configured or failed to initialize.")

    val supabaseUrl: String
        get() = readLocalProperty("supabase.url") ?: ""

    val supabaseFunctionsUrl: String
        get() = "${supabaseUrl.trimEnd('/')}/functions/v1"

    /**
     * True only when a real https Supabase project URL is configured. Gateways
     * check this before making a call so the user gets a clear setup message
     * instead of a cryptic "no scheme" network error.
     */
    val isFunctionsConfigured: Boolean
        get() = supabaseUrl.startsWith("http://") || supabaseUrl.startsWith("https://")

    val supabaseAnonKey: String
        get() = readLocalProperty("supabase.anon.key") ?: ""

    private fun readLocalProperty(name: String): String? = try {
        val props = java.util.Properties()
        val file = java.io.File(appContext.filesDir.parentFile, "local.properties")
        if (file.exists()) {
            file.inputStream().use { props.load(it) }
        }
        props.getProperty(name)
    } catch (e: Exception) {
        null
    }
}
