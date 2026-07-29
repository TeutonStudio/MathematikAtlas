package de.TeutonStudio.MathematikAtlas

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

enum class DarstellungsModus(val anzeigeName: String) {
    System("System"),
    Hell("Hell"),
    Dunkel("Dunkel");

    fun istDunkel(systemIstDunkel: Boolean): Boolean = when (this) {
        System -> systemIstDunkel
        Hell -> false
        Dunkel -> true
    }

    companion object {
        fun ausGespeichert(wert: String?): DarstellungsModus =
            entries.firstOrNull { it.name == wert } ?: System
    }
}

internal class DarstellungsEinstellungenSpeicher(context: Context) {
    private val einstellungen = context.applicationContext.getSharedPreferences(DATEI, Context.MODE_PRIVATE)

    fun lade(): DarstellungsModus = DarstellungsModus.ausGespeichert(einstellungen.getString(SCHLÜSSEL_MODUS, null))

    fun speichere(modus: DarstellungsModus) {
        einstellungen.edit().putString(SCHLÜSSEL_MODUS, modus.name).apply()
    }

    private companion object {
        const val DATEI = "darstellungs-einstellungen"
        const val SCHLÜSSEL_MODUS = "modus"
    }
}

@Immutable
internal data class DarstellungsSteuerung(
    val modus: DarstellungsModus,
    val ändereModus: (DarstellungsModus) -> Unit,
)

internal val LocalDarstellungsSteuerung = staticCompositionLocalOf {
    DarstellungsSteuerung(DarstellungsModus.System) {}
}

private val hellesFarbschema = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF172554),
    secondary = Color(0xFF6D28D9),
    tertiary = Color(0xFF047857),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFFCBD5E1),
)

private val dunklesFarbschema = darkColorScheme(
    primary = Color(0xFF93C5FD),
    onPrimary = Color(0xFF172554),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFFC4B5FD),
    tertiary = Color(0xFF6EE7B7),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

@Composable
internal fun MathematikAtlasTheme(
    modus: DarstellungsModus,
    onModusÄndern: (DarstellungsModus) -> Unit,
    inhalt: @Composable () -> Unit,
) {
    val dunkel = modus.istDunkel(isSystemInDarkTheme())
    MaterialTheme(colorScheme = if (dunkel) dunklesFarbschema else hellesFarbschema) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalDarstellungsSteuerung provides DarstellungsSteuerung(modus, onModusÄndern),
        ) {
            Systemleisten(dunkel)
            inhalt()
        }
    }
}

@Composable
private fun Systemleisten(dunkel: Boolean) {
    val view = LocalView.current
    val farbe = MaterialTheme.colorScheme.background.toArgb()
    SideEffect {
        val window = view.context.findeActivity()?.window ?: return@SideEffect
        @Suppress("DEPRECATION")
        run {
            window.statusBarColor = farbe
            window.navigationBarColor = farbe
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val maske = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (dunkel) 0 else maske, maske)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (dunkel) {
                0
            } else {
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
    }
}

private fun Context.findeActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findeActivity()
    else -> null
}
