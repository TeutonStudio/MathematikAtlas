package de.TeutonStudio.MathematikAtlas

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import de.TeutonStudio.MathematikAtlas.speicher.ProfilFarbe
import de.TeutonStudio.MathematikAtlas.speicher.ProfilFarbPalettenGenerator
import de.TeutonStudio.MathematikAtlas.speicher.RgbFarbe

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

/** Einzige Compose-Abbildung der UI-unabhängig berechneten Profilpalette. */
internal object ProfilFarbschemaGenerator {
    fun erzeuge(quelle: ProfilFarbe, dunkel: Boolean): ColorScheme {
        val r = ProfilFarbPalettenGenerator.erzeuge(quelle, dunkel)
        val fehler = if (dunkel) {
            listOf(Color(0xFFFCA5A5), Color(0xFF450A0A), Color(0xFF7F1D1D), Color(0xFFFEE2E2))
        } else {
            listOf(Color(0xFFB91C1C), Color.White, Color(0xFFFEE2E2), Color(0xFF7F1D1D))
        }
        val gemeinsam = if (dunkel) {
            darkColorScheme(
                primary = r.primary.compose(),
                onPrimary = r.onPrimary.compose(),
                primaryContainer = r.primaryContainer.compose(),
                onPrimaryContainer = r.onPrimaryContainer.compose(),
                inversePrimary = r.inversePrimary.compose(),
                secondary = r.secondary.compose(),
                onSecondary = r.onSecondary.compose(),
                secondaryContainer = r.secondaryContainer.compose(),
                onSecondaryContainer = r.onSecondaryContainer.compose(),
                tertiary = r.tertiary.compose(),
                onTertiary = r.onTertiary.compose(),
                tertiaryContainer = r.tertiaryContainer.compose(),
                onTertiaryContainer = r.onTertiaryContainer.compose(),
                background = r.background.compose(),
                onBackground = r.onBackground.compose(),
                surface = r.surface.compose(),
                onSurface = r.onSurface.compose(),
                surfaceVariant = r.surfaceVariant.compose(),
                onSurfaceVariant = r.onSurfaceVariant.compose(),
                surfaceContainerLowest = r.surfaceContainerLowest.compose(),
                surfaceContainerLow = r.surfaceContainerLow.compose(),
                surfaceContainer = r.surfaceContainer.compose(),
                surfaceContainerHigh = r.surfaceContainerHigh.compose(),
                surfaceContainerHighest = r.surfaceContainerHighest.compose(),
                outline = r.outline.compose(),
                outlineVariant = r.outlineVariant.compose(),
                inverseSurface = r.inverseSurface.compose(),
                inverseOnSurface = r.inverseOnSurface.compose(),
                error = fehler[0],
                onError = fehler[1],
                errorContainer = fehler[2],
                onErrorContainer = fehler[3],
            )
        } else {
            lightColorScheme(
                primary = r.primary.compose(),
                onPrimary = r.onPrimary.compose(),
                primaryContainer = r.primaryContainer.compose(),
                onPrimaryContainer = r.onPrimaryContainer.compose(),
                inversePrimary = r.inversePrimary.compose(),
                secondary = r.secondary.compose(),
                onSecondary = r.onSecondary.compose(),
                secondaryContainer = r.secondaryContainer.compose(),
                onSecondaryContainer = r.onSecondaryContainer.compose(),
                tertiary = r.tertiary.compose(),
                onTertiary = r.onTertiary.compose(),
                tertiaryContainer = r.tertiaryContainer.compose(),
                onTertiaryContainer = r.onTertiaryContainer.compose(),
                background = r.background.compose(),
                onBackground = r.onBackground.compose(),
                surface = r.surface.compose(),
                onSurface = r.onSurface.compose(),
                surfaceVariant = r.surfaceVariant.compose(),
                onSurfaceVariant = r.onSurfaceVariant.compose(),
                surfaceContainerLowest = r.surfaceContainerLowest.compose(),
                surfaceContainerLow = r.surfaceContainerLow.compose(),
                surfaceContainer = r.surfaceContainer.compose(),
                surfaceContainerHigh = r.surfaceContainerHigh.compose(),
                surfaceContainerHighest = r.surfaceContainerHighest.compose(),
                outline = r.outline.compose(),
                outlineVariant = r.outlineVariant.compose(),
                inverseSurface = r.inverseSurface.compose(),
                inverseOnSurface = r.inverseOnSurface.compose(),
                error = fehler[0],
                onError = fehler[1],
                errorContainer = fehler[2],
                onErrorContainer = fehler[3],
            )
        }
        return gemeinsam
    }
}

@Composable
internal fun MathematikAtlasTheme(
    modus: DarstellungsModus,
    profilFarbe: ProfilFarbe,
    onModusÄndern: (DarstellungsModus) -> Unit,
    inhalt: @Composable () -> Unit,
) {
    val dunkel = modus.istDunkel(isSystemInDarkTheme())
    val farbschema = remember(profilFarbe, dunkel) {
        ProfilFarbschemaGenerator.erzeuge(profilFarbe, dunkel)
    }
    MaterialTheme(colorScheme = farbschema) {
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

private fun RgbFarbe.compose(): Color = Color(argbLong)

private fun Context.findeActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findeActivity()
    else -> null
}
