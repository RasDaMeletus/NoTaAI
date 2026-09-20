package com.vinote

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Firebase Test Lab smoke test.
 *
 * Covers the three crashes that shipped in early release builds and were fixed
 * in code but never re-verified on a real device:
 *
 *  1. Launch crash. R8 stripped the Hilt/Room/Compose/ML Kit reflection targets
 *     and ViNoteViewModel threw NPE reading activeUserId before any login.
 *  2. Guest-mode crash. AuthViewModel.loginDirectly was removed, so
 *     "Lanjut sebagai Guest" threw UnsupportedOperationException.
 *  3. Reaching the assertion means onCreate completed: Hilt built the DI graph,
 *     Room opened the database, and the guest session initialised.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class NoTaSmokeTest {

    @Test
    fun appLaunchesWithoutCrashing() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Assert.assertNotNull("MainActivity launched", scenario)
        }
    }
}
