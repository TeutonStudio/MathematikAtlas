package de.TeutonStudio.MathematikAtlas

import android.content.Context
import androidx.compose.runtime.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikAtlas.speicher.KartenSpeicher
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKnoten.visualisierung.ui.VisualisierungsKnotenRenderer

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
        private set
    private var ausgewählteKnotenKategorie by mutableStateOf<String?>(null)

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
        sichereKnotenKategorieAuswahl()
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

    /** Fügt ein Tupel und dessen Umwandlung zu einem orientierten Vektor als eine zusammenhängende Knotenfolge ein. */
    fun fügeTupelVektorEin(spalte: Boolean, position: GraphPunkt) {
        val tupel = MathematikKnotenVorlagen.Tupel.erzeuge(position)
        val umwandlungVorlage = if (spalte) MathematikKnotenVorlagen.TupelZuSpalte else MathematikKnotenVorlagen.TupelZuZeile
        val umwandlung = umwandlungVorlage.erzeuge(position + GraphPunkt(270f, 0f))
        val tupelAusgang = tupel.anschlüsse.firstOrNull { it.name == "tupel" } ?: return
        val umwandlungsEingang = umwandlung.anschlüsse.firstOrNull { it.name == "tupel" } ?: return
        val vorläufigeKarte = editor.karte.copy(knoten = editor.karte.knoten + listOf(tupel, umwandlung))
        val interneVerbindung = verbindungWennErlaubt(
            vorläufigeKarte,
            AnschlussVerweis(tupel.id, tupelAusgang.id),
            AnschlussVerweis(umwandlung.id, umwandlungsEingang.id),
        ) ?: return
        val karteMitInternerVerbindung = vorläufigeKarte.copy(verbindungen = vorläufigeKarte.verbindungen + interneVerbindung)
        val externeVerbindung = knotenAuswahlStart?.let { start ->
            tupel.anschlüsse.asSequence()
                .map { AnschlussVerweis(tupel.id, it.id) }
                .mapNotNull { ziel -> verbindungWennErlaubt(karteMitInternerVerbindung, start, ziel) }
                .firstOrNull()
        }
        if (knotenAuswahlStart != null && externeVerbindung == null) return

        editor.beginneInteraktion()
        editor.führeAus(KartenAktion.KnotenEinfügen(tupel), mitHistorie = false)
        editor.führeAus(KartenAktion.KnotenEinfügen(umwandlung), mitHistorie = false)
        editor.führeAus(KartenAktion.VerbindungEinfügen(interneVerbindung), mitHistorie = false)
        externeVerbindung?.let { editor.führeAus(KartenAktion.VerbindungEinfügen(it), mitHistorie = false) }
        editor.beendeInteraktion()
        editor.wähleKnoten(umwandlung.id)
        schließeKnotenAuswahl()
    }

    fun kannTupelVektorEinfügen(): Boolean = istKompatibelMitOffenerVerbindung(MathematikKnotenVorlagen.Tupel)

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
    fun rendererFür(knoten: KnotenDaten) = if (knoten.art == "mathematik.visualisierung") {
        VisualisierungsKnotenRenderer { daten -> auswertung.knoten[daten.id] }
    } else renderer()

    fun setzeSuchText(text: String) {
        suchText = text
        sichereKnotenKategorieAuswahl()
    }

    fun knotenKategorien(): List<String> = alleKnotenVorlagen().map { it.kategorie }.distinct()

    fun sichtbareVorlagen(): List<KnotenVorlage> = alleKnotenVorlagen().filter { vorlage ->
        val suchePasst = suchText.isBlank() || vorlage.name.contains(suchText, ignoreCase = true) || vorlage.kategorie.contains(suchText, ignoreCase = true) || vorlage.beschreibung.contains(suchText, ignoreCase = true)
        suchePasst && istKompatibelMitOffenerVerbindung(vorlage)
    }

    fun aktiveKnotenKategorie(): String? = ausgewählteKnotenKategorie?.takeIf { kategorie ->
        sichtbareVorlagen().any { it.kategorie == kategorie }
    }

    fun wähleKnotenKategorie(kategorie: String?) {
        ausgewählteKnotenKategorie = kategorie?.takeIf { gewählt ->
            sichtbareVorlagen().any { it.kategorie == gewählt }
        }
    }

    private fun sichereKnotenKategorieAuswahl() {
        if (aktiveKnotenKategorie() == null) ausgewählteKnotenKategorie = null
    }

    private fun alleKnotenVorlagen(): List<KnotenVorlage> = MathematikKnotenVorlagen.alle + gruppenVorlagen()

    private fun gruppenVorlagen(): List<KnotenVorlage> = karten.asSequence()
        .filter { it.id != editor.karte.id && !it.archiviert && !referenziertKarte(it, editor.karte.id, mutableSetOf()) }
        .flatMap { karte ->
            val eingänge = öffentlicheKartenAnschlüsse(karte, "mathematik.kartenEingang", AnschlussRichtung.Eingang, AnschlussKante.Links)
            val ausgänge = öffentlicheKartenAnschlüsse(karte, "mathematik.kartenAusgang", AnschlussRichtung.Ausgang, AnschlussKante.Rechts)
            val gruppe = KnotenVorlage(
                art = "gruppe.${karte.id.wert}",
                name = karte.name,
                kategorie = "Gespeicherte Karten",
                beschreibung = "Wiederverwendbare Karte, fest auf Version ${karte.version} verwiesen.",
                standardGröße = GraphGröße(240f, maxOf(100f, 54f + maxOf(eingänge.size, ausgänge.size) * 28f)),
                anschlüsse = eingänge + ausgänge,
                kartenVerweis = KartenVerweis(karte.id, karte.version),
            )
            listOf(gruppe)
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

    private fun verbindungWennErlaubt(
        karte: KartenDaten,
        erster: AnschlussVerweis,
        zweiter: AnschlussVerweis,
    ): VerbindungDaten? {
        if (graphPrüfung.prüfe(karte, erster, zweiter) !is VerbindungsPrüfung.Erlaubt) return null
        val (von, zu) = graphPrüfung.normalisiere(karte, erster, zweiter) ?: return null
        return VerbindungDaten(von = von, zu = zu)
    }

    /** Migriert bekannte Knotendaten, darunter Karten-Schnittstellen, assoziative Eingänge und die Bezeichnung „Differenz“. */
    private fun aktualisiereAssoziativeKnoten(karte: KartenDaten): KartenDaten = migriereAssoziativeKnoten(karte)

    private fun werteAus() { auswertung = auswerter.auswerten(editor.karte) }
}

enum class VerwaltungsBereich { Karten, Konzepte, Variablen, Auswertung, Fehler }

/** Reine Lade-Migration für bekannte assoziative Knoten; auch von JVM-Tests prüfbar. */
internal fun migriereAssoziativeKnoten(karte: KartenDaten): KartenDaten {
    val migriert = migriereKartenAusgangZuEinzelanschluss(karte)
    return migriert.copy(knoten = migriert.knoten.map { ursprünglicherKnoten ->
        val knoten = if (ursprünglicherKnoten.art == "mathematik.differenz" && ursprünglicherKnoten.name == "Mengendifferenz") ursprünglicherKnoten.copy(name = "Differenz") else ursprünglicherKnoten
        if (knoten.art !in assoziativeKnotenArten) knoten else {
            val festeEingänge = knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(2) ?: 2
            val verbundeneEingänge = migriert.verbindungen.map { it.zu }.toSet()
            val überzähligeFesteEingänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang && !it.dynamischErzeugt }
                .sortedBy { it.reihenfolge }.drop(festeEingänge).filter { AnschlussVerweis(knoten.id, it.id) !in verbundeneEingänge }.map { it.id }.toSet()
            knoten.copy(
                anschlüsse = knoten.anschlüsse.filterNot { it.id in überzähligeFesteEingänge }.map { anschluss -> if (anschluss.richtung == AnschlussRichtung.Eingang) anschluss.copy(kannSichErweitern = true) else anschluss },
                parameter = knoten.parameter + mapOf("festeEingänge" to festeEingänge.toString(), "operatorAnzeige" to if (knoten.parameter["operatorAnzeige"] == "name") "name" else "wert"),
            )
        }
    })
}

private val assoziativeKnotenArten = setOf(
    "mathematik.addition", "mathematik.extremwert", "mathematik.vereinigung", "mathematik.schnitt", "mathematik.kartesischesProdukt", "mathematik.tupel", "mathematik.vektor", "mathematik.zeilenVektor", "mathematik.matrix",
)

internal fun öffentlicheKartenAnschlüsse(
    karte: KartenDaten,
    interneArt: String,
    richtung: AnschlussRichtung,
    kante: AnschlussKante,
): List<AnschlussDaten> = karte.knoten.asSequence()
    .filter { it.art == interneArt }
    .mapNotNull { intern ->
        intern.anschlüsse.firstOrNull { it.name == "wert" }?.let { wert -> öffentlicherKartenName(intern) to wert.art }
    }
    .distinctBy { it.first }
    .mapIndexed { index, (name, art) ->
        AnschlussDaten(name = name, richtung = richtung, kante = kante, art = art, reihenfolge = index)
    }
    .toList()

internal fun öffentlicherKartenName(knoten: KnotenDaten): String =
    knoten.parameter["name"]?.trim()?.takeIf(String::isNotEmpty) ?: knoten.name

internal fun migriereKartenAusgangZuEinzelanschluss(karte: KartenDaten): KartenDaten {
    val entfernteAnschlüsse = karte.knoten.asSequence()
        .filter { it.art == "mathematik.kartenAusgang" }
        .flatMap { knoten -> knoten.anschlüsse.asSequence().filter { it.name == "zielmenge" }.map { AnschlussVerweis(knoten.id, it.id) } }
        .toSet()
    if (entfernteAnschlüsse.isEmpty()) return karte
    return karte.copy(
        knoten = karte.knoten.map { knoten ->
            if (knoten.art == "mathematik.kartenAusgang") knoten.copy(anschlüsse = knoten.anschlüsse.filterNot { it.name == "zielmenge" }) else knoten
        },
        verbindungen = karte.verbindungen.filter { it.von !in entfernteAnschlüsse && it.zu !in entfernteAnschlüsse },
    )
}
