package de.TeutonStudio.MathematikAtlas

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import androidx.lifecycle.lifecycleScope
import de.TeutonStudio.MathematikAtlas.speicher.LokalesProfilSpeicher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var atlasZustand: AtlasZustand

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        atlasZustand = AtlasZustand(applicationContext)
        setContent {
            val darstellungsSpeicher = remember { DarstellungsEinstellungenSpeicher(applicationContext) }
            var darstellungsModus by remember { mutableStateOf(darstellungsSpeicher.lade()) }
            val profilSpeicher = remember { LokalesProfilSpeicher(applicationContext) }
            val initialesProfil = remember(profilSpeicher) { profilSpeicher.lade() }
            val profilAenderung by LokalesProfilSpeicher.profilAenderungen.collectAsState()
            val profil = profilAenderung ?: initialesProfil
            val zustand = remember { atlasZustand }

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
        verarbeiteMatlasIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        verarbeiteMatlasIntent(intent)
    }

    private fun verarbeiteMatlasIntent(eingang: Intent?) {
        val uri = eingang?.matlasUri() ?: return
        setIntent(Intent(Intent.ACTION_MAIN).setClass(this, MainActivity::class.java))
        lifecycleScope.launch {
            val ergebnis = runCatching {
                val inhalt = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Die ausgewählte Datei konnte nicht gelesen werden.")
                }
                atlasZustand.importiere(inhalt)
            }
            ergebnis.fold(
                onSuccess = {
                    Toast.makeText(
                        this@MainActivity,
                        "Mathematik-Atlas-Freigabe wurde importiert.",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
                onFailure = { fehler ->
                    Toast.makeText(
                        this@MainActivity,
                        "Import fehlgeschlagen: ${fehler.message ?: "ungültige .matlas-Datei"}",
                        Toast.LENGTH_LONG,
                    ).show()
                },
            )
        }
    }
}

@Suppress("DEPRECATION")
private fun Intent.matlasUri(): Uri? = when (action) {
    Intent.ACTION_VIEW -> data
    Intent.ACTION_SEND -> getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        ?: clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
    else -> null
}
