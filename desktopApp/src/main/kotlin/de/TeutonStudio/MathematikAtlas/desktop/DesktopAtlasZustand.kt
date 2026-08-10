package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.runtime.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRenderer
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.KartenQuelle
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.*

@Stable
class DesktopAtlasZustand(
    val speicher: DesktopKartenSpeicher = DesktopKartenSpeicher(),
) {
    private val laufzeit = MathematikKartenLaufzeit(
        kartenQuelle = KartenQuelle(speicher::lade),
    )
    val vorlagen: List<KnotenVorlage> = laufzeit.vorlagen
    val editor = KartenEditorZustand(
        speicher.ladeAktuell() ?: KartenDaten(name = "Neue Desktop-Karte"),
        laufzeit.graphPrüfung,
    )
    var auswertung by mutableStateOf(laufzeit.auswerten(editor.karte))
        private set
    var meldung by mutableStateOf<String?>(null)
        private set

    fun aktualisiereAuswertung() {
        auswertung = laufzeit.auswerten(editor.karte)
    }

    fun berechneKnotenCacheNeu(knotenId: KnotenId) {
        laufzeit.verwerfeCache(knotenId)
        aktualisiereAuswertung()
    }

    fun fügeEin(vorlage: KnotenVorlage, position: GraphPunkt) {
        val knoten = vorlage.erzeuge(position)
        editor.führeAus(KartenAktion.KnotenEinfügen(knoten))
        editor.wähleKnoten(knoten.id)
        aktualisiereAuswertung()
    }

    fun speichere() {
        val gespeichert = runCatching { speicher.speichere(editor.karte) }.getOrElse { fehler ->
            meldung = "Speichern fehlgeschlagen: ${fehler.message}"
            return
        }
        editor.ersetzeKarte(gespeichert, historieLeeren = false)
        meldung = "Gespeichert als Version ${gespeichert.version}."
    }

    fun importiere(text: String) {
        val gespeichert = runCatching { speicher.importiere(text) }.getOrElse { fehler ->
            meldung = "Import fehlgeschlagen: ${fehler.message}"
            return
        }
        editor.ersetzeKarte(gespeichert)
        aktualisiereAuswertung()
        meldung = "${gespeichert.name} importiert."
    }

    fun neueKarte() {
        editor.ersetzeKarte(KartenDaten(name = "Neue Desktop-Karte"))
        aktualisiereAuswertung()
    }

    fun benenneAuswahlUm(name: String) {
        val id = editor.ausgewählterKnoten ?: return
        val knoten = editor.karte.knoten.firstOrNull { it.id == id } ?: return
        val bereinigt = name.trim()
        if (bereinigt.isNotEmpty() && bereinigt != knoten.name) {
            editor.führeAus(KartenAktion.KnotenErsetzen(knoten.copy(name = bereinigt)))
            aktualisiereAuswertung()
        }
    }

    fun rendererFür(knoten: KnotenDaten): KnotenRenderer =
        MathematikKnotenRenderer(
            ergebnisFür = { auswertung.knoten[it.id] },
            beiKnotenKlick = { editor.wähleKnoten(it.id) },
        ).mitAuswertungszeit { auswertung.knoten[it.id] }

    fun schließeMeldung() { meldung = null }
}
