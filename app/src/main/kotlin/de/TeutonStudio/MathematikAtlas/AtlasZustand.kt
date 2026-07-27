package de.TeutonStudio.MathematikAtlas

import android.content.Context
import androidx.compose.runtime.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikAtlas.speicher.KartenSpeicher
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*

@Stable
class AtlasZustand(context: Context) {
    val speicher = KartenSpeicher(context)
    val anschlussArten = AnschlussArtRegister(MathematikAnschlussArten.alle)
    private val graphPrüfung = GraphPrüfung(anschlussArten)
    private val auswerter = KartenAuswerter(StandardMathematikAuswerter.erzeugeRegister(), KartenQuelle(speicher::lade))

    var karten by mutableStateOf<List<KartenDaten>>(emptyList())
        private set
    var brotkrumen by mutableStateOf<List<KartenVerweis>>(emptyList())
        private set
    var auswertung by mutableStateOf(KartenAuswertungsErgebnis(emptyMap(), emptyList()))
        private set
    var linkerBereich by mutableStateOf(VerwaltungsBereich.Karten)
    var knotenAuswahlPosition by mutableStateOf<GraphPunkt?>(null)
    var knotenAuswahlStart by mutableStateOf<AnschlussVerweis?>(null)
    var suchText by mutableStateOf("")

    val editor: KartenEditorZustand
    private var letzterGespeicherterStand: KartenDaten

    init {
        if (speicher.liste(archivierteEinschließen = true).isEmpty()) BeispielKarten.alle().forEach(speicher::speichere)
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
        val start = karten.firstOrNull { it.name == "Rechnen" } ?: karten.first()
        editor = KartenEditorZustand(start, graphPrüfung)
        letzterGespeicherterStand = start
        brotkrumen = listOf(KartenVerweis(start.id, start.version))
        werteAus()
    }

    val aktuelleKarte get() = editor.karte
    val ausgewählterKnoten get() = editor.karte.knoten.firstOrNull { it.id == editor.ausgewählterKnoten }

    fun aktualisiereAuswertung() { werteAus() }

    fun öffne(karte: KartenDaten, alsUnterkarte: Boolean = false) {
        val aktualisiert = aktualisiereAssoziativeKnoten(karte)
        editor.ersetzeKarte(aktualisiert)
        letzterGespeicherterStand = aktualisiert
        brotkrumen = if (alsUnterkarte) brotkrumen + KartenVerweis(aktualisiert.id, aktualisiert.version) else listOf(KartenVerweis(aktualisiert.id, aktualisiert.version))
        werteAus()
    }

    fun öffne(verweis: KartenVerweis, alsUnterkarte: Boolean = true) {
        speicher.lade(verweis)?.let { öffne(it, alsUnterkarte) }
    }

    fun geheZuBrotkrume(index: Int) {
        val ziel = brotkrumen.getOrNull(index) ?: return
        speicher.lade(ziel)?.let {
            val aktualisiert = aktualisiereAssoziativeKnoten(it)
            brotkrumen = brotkrumen.take(index + 1)
            editor.ersetzeKarte(aktualisiert)
            letzterGespeicherterStand = aktualisiert
            werteAus()
        }
    }

    fun speichereAktuell() {
        // Vorläufige Erweiterungsanschlüsse gehören zu einer laufenden Kabelgeste und
        // dürfen weder automatisch gespeichert noch als neue Kartenversion gezählt werden.
        if (editor.verbindungsStart != null) return
        if (editor.karte == letzterGespeicherterStand) return
        val gespeichert = speicher.speichere(editor.karte)
        if (gespeichert.version != editor.karte.version) {
            editor.ersetzeKarte(gespeichert, historieLeeren = false)
            brotkrumen = brotkrumen.dropLast(1) + KartenVerweis(gespeichert.id, gespeichert.version)
        }
        letzterGespeicherterStand = gespeichert
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
    }

    fun öffneKnotenAuswahl(position: GraphPunkt, start: AnschlussVerweis? = null) {
        knotenAuswahlPosition = position
        knotenAuswahlStart = start
    }

    fun schließeKnotenAuswahl() {
        knotenAuswahlPosition = null
        knotenAuswahlStart = null
    }

    fun fügeKnotenEin(vorlage: KnotenVorlage, position: GraphPunkt) {
        val knoten = vorlage.erzeuge(position)
        val start = knotenAuswahlStart
        editor.führeAus(KartenAktion.KnotenEinfügen(knoten))
        if (start != null) {
            editor.beginneVerbindung(start)
            val ziel = knoten.anschlüsse.asSequence().map { AnschlussVerweis(knoten.id, it.id) }
                .firstOrNull(editor::kompatibelMitStart)
            if (ziel != null) editor.anschlussAngeklickt(ziel)
        }
        editor.wähleKnoten(knoten.id)
        schließeKnotenAuswahl()
    }

    fun neueKarte() {
        val karte = speicher.speichere(KartenDaten(name = "Neue Karte ${karten.size + 1}"))
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
        öffne(karte)
    }

    fun importiere(text: String) {
        val karte = speicher.importiere(text)
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
        öffne(karte)
    }

    fun archiviereAktuell() {
        speicher.archiviere(editor.karte)
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
        karten.firstOrNull()?.let { öffne(it) }
    }

    fun renderer() = MathematikKnotenRenderer { knoten -> auswertung.knoten[knoten.id] }

    fun sichtbareVorlagen(): List<KnotenVorlage> = (MathematikKnotenVorlagen.alle + gruppenVorlagen()).filter { vorlage ->
        val suchePasst = suchText.isBlank() || vorlage.name.contains(suchText, ignoreCase = true) || vorlage.kategorie.contains(suchText, ignoreCase = true)
        suchePasst && istKompatibelMitOffenerVerbindung(vorlage)
    }

    private fun gruppenVorlagen(): List<KnotenVorlage> = karten.asSequence()
        .filter { it.id != editor.karte.id && !it.archiviert && !referenziertKarte(it, editor.karte.id, mutableSetOf()) }
        .map { karte ->
            val eingänge = karte.knoten.filter { it.art == "mathematik.kartenEingang" }.mapIndexed { index, intern ->
                AnschlussDaten(
                    name = intern.parameter["name"] ?: intern.name,
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = intern.anschlüsse.firstOrNull()?.art ?: MathematikAnschlussArten.Objekt.id,
                    reihenfolge = index,
                )
            }
            val ausgänge = karte.knoten.filter { it.art == "mathematik.kartenAusgang" }.mapIndexed { index, intern ->
                AnschlussDaten(
                    name = intern.parameter["name"] ?: intern.name,
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = intern.anschlüsse.firstOrNull()?.art ?: MathematikAnschlussArten.Objekt.id,
                    reihenfolge = index,
                )
            }
            KnotenVorlage(
                art = "gruppe.${karte.id.wert}",
                name = karte.name,
                kategorie = "Gespeicherte Karten",
                beschreibung = "Wiederverwendbare Karte, fest auf Version ${karte.version} verwiesen.",
                standardGröße = GraphGröße(240f, maxOf(100f, 54f + maxOf(eingänge.size, ausgänge.size) * 28f)),
                anschlüsse = eingänge + ausgänge,
                kartenVerweis = KartenVerweis(karte.id, karte.version),
            )
        }.toList()

    private fun referenziertKarte(karte: KartenDaten, gesuchteId: KartenId, besucht: MutableSet<KartenVerweis>): Boolean {
        val refs = karte.knoten.mapNotNull { it.kartenVerweis }
        if (refs.any { it.kartenId == gesuchteId }) return true
        return refs.any { ref ->
            if (!besucht.add(ref)) false else speicher.lade(ref)?.let { referenziertKarte(it, gesuchteId, besucht) } == true
        }
    }

    private fun istKompatibelMitOffenerVerbindung(vorlage: KnotenVorlage): Boolean {
        val start = knotenAuswahlStart ?: return true
        val probe = vorlage.erzeuge(GraphPunkt.Zero)
        val probeKarte = editor.karte.copy(knoten = editor.karte.knoten + probe)
        return probe.anschlüsse.any { anschluss ->
            graphPrüfung.prüfe(probeKarte, start, AnschlussVerweis(probe.id, anschluss.id)) is VerbindungsPrüfung.Erlaubt
        }
    }

    /** Macht auch vor dieser Version gespeicherte Additions- und Vereinigungs-Knoten erweiterbar. */
    private fun aktualisiereAssoziativeKnoten(karte: KartenDaten): KartenDaten = karte.copy(
        knoten = karte.knoten.map { knoten ->
            if (knoten.art !in setOf("mathematik.addition", "mathematik.vereinigung")) knoten
            else {
                val festeEingänge = knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(2) ?: 2
                val verbundeneEingänge = karte.verbindungen.map { it.zu }.toSet()
                val überzähligeFesteEingänge = knoten.anschlüsse
                    .filter { it.richtung == AnschlussRichtung.Eingang && !it.dynamischErzeugt }
                    .sortedBy { it.reihenfolge }
                    .drop(festeEingänge)
                    .filter { AnschlussVerweis(knoten.id, it.id) !in verbundeneEingänge }
                    .map { it.id }
                    .toSet()
                knoten.copy(
                    anschlüsse = knoten.anschlüsse.filterNot { it.id in überzähligeFesteEingänge }.map { anschluss ->
                        if (anschluss.richtung == AnschlussRichtung.Eingang) anschluss.copy(kannSichErweitern = true) else anschluss
                    },
                    parameter = knoten.parameter + ("festeEingänge" to festeEingänge.toString()),
                )
            }
        },
    )

    private fun werteAus() { auswertung = auswerter.auswerten(editor.karte) }
}

enum class VerwaltungsBereich { Karten, Konzepte, Variablen, Auswertung, Fehler }
