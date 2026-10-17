package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.runtime.*
import kotlinx.coroutines.*
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
    fun anschlussInfoFür(karte: KartenDaten, ref: AnschlussVerweis) = laufzeit.anschlussInfoFür(karte, ref)

    private val laufzeit = MathematikKartenLaufzeit(
        kartenQuelle = KartenQuelle(speicher::lade),
        nichtAuswertbareKnotenArten = setOf("karte.notiz"),
    )
    val vorlagen: List<KnotenVorlage> = desktopKnotenVorlagen()
    val editor = KartenEditorZustand(
        speicher.ladeAktuell() ?: KartenDaten(name = "Neue Desktop-Karte"),
        laufzeit.graphPrüfung,
    )
    init {
        val verbindungsPlaner = ÜbersetzerVerbindungsPlaner(laufzeit.graphPrüfung)
        editor.verbindungsPlaner = verbindungsPlaner::vorschläge
        editor.verbindungÜberAdapterMöglich = verbindungsPlaner::möglich
        editor.öffneGruppe(speicher.ladeAktiveGruppe())
    }
    private val rechenScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var rechenAuftrag: Job? = null
    private var rechenGeneration = 0L
    private var rechenMomentaufnahme: KartenDaten? = null
    var auswertung by mutableStateOf(if (editor.karte.knoten.any { besitztPrädikatsRechenweg(it.art) || it.kartenVerweis != null })
        KartenAuswertungsErgebnis(emptyMap(), emptyList()) else laufzeit.auswerten(editor.karte))
        private set
    var konfigurationsPlan by mutableStateOf<KonfigurationsPlan?>(null)
        private set
    var meldung by mutableStateOf<String?>(null)
        private set

    fun beendeBerechnungen() { rechenScope.cancel() }

    fun aktualisiereAuswertung() {
        val moment = editor.karte
        if (moment == rechenMomentaufnahme && rechenAuftrag?.isActive == true) return
        val generation = ++rechenGeneration
        rechenAuftrag?.cancel()
        rechenMomentaufnahme = moment
        if (moment.knoten.none { besitztPrädikatsRechenweg(it.art) || it.kartenVerweis != null }) {
            übernehmeUrbildAuswertung(moment, laufzeit.auswerten(moment))
            return
        }
        auswertung = KartenAuswertungsErgebnis(emptyMap(), emptyList())
        rechenAuftrag = rechenScope.launch {
            val berechnet = withContext(Dispatchers.Default) {
                MathematikKartenLaufzeit(kartenQuelle = KartenQuelle(speicher::lade), nichtAuswertbareKnotenArten = setOf("karte.notiz")).auswerten(moment)
            }
            if (generation == rechenGeneration && editor.karte == moment) übernehmeUrbildAuswertung(moment, berechnet)
        }
    }

    private fun übernehmeUrbildAuswertung(moment: KartenDaten, ergebnis: KartenAuswertungsErgebnis) {
        val synchron = synchronisiereUrbildAnschluesse(moment, ergebnis, laufzeit.graphPrüfung)
        if (synchron != moment) {
            val auswahl = editor.ausgewählteKnoten
            val aktiv = editor.ausgewählterKnoten
            editor.ersetzeKarte(synchron, historieLeeren = false)
            editor.stelleAuswahlWiederHer(auswahl, aktiv)
            auswertung = laufzeit.auswerten(synchron)
        } else auswertung = ergebnis
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
        val gespeichert = runCatching {
            val gespeichert = speicher.speichere(editor.karte)
            speicher.speichereAktiveGruppe(editor.geöffneteGruppe)
            gespeichert
        }.getOrElse { fehler ->
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
        if(knoten.art == "karte.notiz") DesktopNotizRenderer
        else if(knoten.art == "mathematik.visualisierung") de.TeutonStudio.MathematikKnoten.visualisierung.ui.VisualisierungsKnotenRenderer { auswertung.knoten[it.id] }.mitAuswertungszeit { auswertung.knoten[it.id] }
        else if(knoten.art == GEOMETRY_NODES_PREFIX+"preview") GeometryNodesVorschauRenderer { auswertung.knoten[it.id] }.mitAuswertungszeit { auswertung.knoten[it.id] }
        else MathematikKnotenRenderer(
            ergebnisFür = { auswertung.knoten[it.id] },
            beiKnotenKlick = { editor.wähleKnoten(it.id) },
        ).mitAuswertungszeit { auswertung.knoten[it.id] }

    fun beantrageKonfiguration(kandidat: KnotenDaten) {
        runCatching { planeKonfiguration(editor.karte, kandidat, laufzeit.graphPrüfung, laufzeit::auswerten) }
            .onFailure { meldung = it.message }
            .onSuccess { plan ->
                if (!plan.auswirkung.trenntVerbindungen) übernehmeKonfigurationsPlan(plan)
                else konfigurationsPlan = plan
            }
    }
    fun bestätigeKonfiguration() { konfigurationsPlan?.let(::übernehmeKonfigurationsPlan) }
    fun verwerfeKonfiguration() { konfigurationsPlan = null }
    private fun übernehmeKonfigurationsPlan(plan: KonfigurationsPlan) {
        editor.führeAus(KartenAktion.KartenInhaltErsetzen(plan.karte))
        konfigurationsPlan = null; aktualisiereAuswertung()
    }

    fun schließeMeldung() { meldung = null }
}
