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
 * URL and anon key are compiled in from .env via the secrets Gradle plugin
 * (BuildConfig.SUPABASE_URL / BuildConfig.SUPABASE_ANON_KEY), which is the
 * correct place for the public, RLS-protected publishable key. Private keys
 * (service_role, OpenRouter, Midtrans) never enter the APK.
 */
class SupabaseClientProvider(private val appContext: Context) {

    val isConfigured: Boolean by lazy {
        val url = supabaseUrl
        val key = supabaseAnonKey
        !url.isNullOrBlank() && !key.isNullOrBlank() &&
                !url.contains("placeholder") && !key.contains("placeholder")
    }

    val clientOrNull: SupabaseClient? by lazy {
        try {
            val url = supabaseUrl
            val key = supabaseAnonKey
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
        get() = com.example.BuildConfig.SUPABASE_URL ?: ""

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
        get() = com.example.BuildConfig.SUPABASE_ANON_KEY ?: ""
}
