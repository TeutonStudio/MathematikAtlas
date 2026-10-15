package de.TeutonStudio.MathematikAtlas

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class VollbildGeraeteTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun systemleistenSindNachStartUndFokusrueckkehrAusgeblendet() {
        compose.waitForIdle()
        compose.runOnIdle {
            compose.activity.onWindowFocusChanged(true)
        }
        compose.waitUntil(5000) {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.systemBars()) == false
        }
        compose.runOnIdle {
            val insets = ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
            assertNotNull(insets)
            assertFalse(insets!!.isVisible(WindowInsetsCompat.Type.statusBars()))
            assertFalse(insets.isVisible(WindowInsetsCompat.Type.navigationBars()))
        }
    }
}
