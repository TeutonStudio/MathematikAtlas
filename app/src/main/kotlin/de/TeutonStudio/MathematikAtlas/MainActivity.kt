package de.TeutonStudio.MathematikAtlas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.TeutonStudio.MathematikAtlas.speicher.LokalesProfilSpeicher

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darstellungsSpeicher = remember { DarstellungsEinstellungenSpeicher(applicationContext) }
            var darstellungsModus by remember { mutableStateOf(darstellungsSpeicher.lade()) }
            val profilSpeicher = remember { LokalesProfilSpeicher(applicationContext) }
            val initialesProfil = remember(profilSpeicher) { profilSpeicher.lade() }
            val profilAenderung by LokalesProfilSpeicher.profilAenderungen.collectAsState()
            val profil = profilAenderung ?: initialesProfil
            val zustand = remember { AtlasZustand(applicationContext) }

            MathematikAtlasTheme(
                modus = darstellungsModus,
                profilFarbe = profil.lieblingsFarbe,
                onModusÄndern = { modus ->
                    darstellungsModus = modus
                    darstellungsSpeicher.speichere(modus)
                },
            ) {
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    MathematikAtlasApp(zustand)
                }
            }
        }
    }
}
