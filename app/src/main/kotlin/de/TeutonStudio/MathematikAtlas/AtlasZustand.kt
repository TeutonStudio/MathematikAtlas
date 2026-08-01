package de.TeutonStudio.MathematikAtlas

import android.content.Context
import androidx.compose.runtime.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikAtlas.speicher.KartenJson
import de.TeutonStudio.MathematikAtlas.speicher.KartenSpeicher
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKnoten.visualisierung.ui.VisualisierungsKnotenRenderer

@Stable
class AtlasZustand(context: Context) {
    val speicher = KartenSpeicher(context)
    val anschlussArten = AnschlussArtRegister(MathematikAnschlussArten.alle)
    private val graphPrüfung = GraphPrüfung(anschlussArten)
    private val auswerter = KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister(), KartenQuelle(speicher::lade))

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
        if (editor.verbindungsStart != null || editor.karte == letzterGespeicherterStand) return
        val gespeichert = speicher.speichere(editor.karte)
        if (gespeichert.version != editor.karte.version) {
            editor.ersetzeKarte(gespeichert, historieLeeren = false)
            brotkrumen = brotkrumen.dropLast(1) + KartenVerweis(gespeichert.id, gespeichert.version)
        }
        letzterGespeicherterStand = gespeichert
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
    }

    fun öffneBearbeitbareKopie(vorlage: KartenDaten) {
        val basisName = "${vorlage.name} – Kopie"
        val vorhandeneNamen = karten.mapTo(mutableSetOf()) { it.name }
        var name = basisName
        var nummer = 2
        while (name in vorhandeneNamen) {
            name = "$basisName $nummer"
            nummer += 1
        }
        val gespeichert = speicher.speichere(vorlage.alsNeueKarte(name))
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
        linkerBereich = VerwaltungsBereich.Karten
        öffne(gespeichert)
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
            val ziel = knoten.anschlüsse.asSequence().map { AnschlussVerweis(knoten.id, it.id) }.firstOrNull(editor::kompatibelMitStart)
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

    /** Übernimmt bearbeitetes Karten-JSON. Referenzierte Versionen werden vom Speicher automatisch fortgeschrieben. */
    fun übernehmeJson(text: String): String? {
        val gelesen = runCatching { KartenJson.lese(text) }.getOrElse { fehler ->
            return "Ungültiges JSON: ${fehler.message ?: fehler::class.simpleName}"
        }
        gelesen.validierungsFehler()?.let { return it }
        val gespeichert = runCatching { speicher.speichere(gelesen) }.getOrElse { fehler ->
            return "Die Karte konnte nicht gespeichert werden: ${fehler.message ?: fehler::class.simpleName}"
        }
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
        öffne(gespeichert)
        return null
    }

    fun archiviereAktuell() {
        speicher.archiviere(editor.karte)
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
        karten.firstOrNull()?.let { öffne(it) }
    }

    fun renderer() = MathematikKnotenRenderer { knoten -> auswertung.knoten[knoten.id] }

    fun rendererFür(knoten: KnotenDaten) = when {
        knoten.art.startsWith("konzept.") -> KonzeptDokumentationsRenderer
        knoten.art == "mathematik.visualisierung" -> VisualisierungsKnotenRenderer { daten -> auswertung.knoten[daten.id] }
        knoten.art == "mathematik.geometrie.visualisierung" -> GeometrieVisualisierungsKnotenRenderer { daten -> auswertung.knoten[daten.id] }
        else -> renderer()
    }

    fun setzeSuchText(text: String) {
        suchText = text
        sichereKnotenKategorieAuswahl()
    }

    fun knotenKategorien(): List<String> = alleKnotenVorlagen().map { it.kategorie }.distinct()

    fun sichtbareVorlagen(): List<KnotenVorlage> = alleKnotenVorlagen().filter { vorlage ->
        val suchePasst = suchText.isBlank() || vorlage.name.contains(suchText, ignoreCase = true) || vorlage.kategorie.contains(suchText, ignoreCase = true) || vorlage.beschreibung.contains(suchText, ignoreCase = true)
        suchePasst && istKompatibelMitOffenerVerbindung(vorlage)
    }

    fun aktiveKnotenKategorie(): String? = ausgewählteKnotenKategorie?.takeIf { kategorie -> sichtbareVorlagen().any { it.kategorie == kategorie } }

    fun wähleKnotenKategorie(kategorie: String?) {
        ausgewählteKnotenKategorie = kategorie?.takeIf { gewählt -> sichtbareVorlagen().any { it.kategorie == gewählt } }
    }

    private fun sichereKnotenKategorieAuswahl() {
        if (aktiveKnotenKategorie() == null) ausgewählteKnotenKategorie = null
    }

    private fun alleKnotenVorlagen(): List<KnotenVorlage> = alleMathematikKnotenVorlagen() + MengenraumKnotenVorlagen.alle + GeometrieKnotenVorlagen.alle + gruppenVorlagen()

    private fun gruppenVorlagen(): List<KnotenVorlage> = karten.asSequence()
        .filter { it.id != editor.karte.id && !it.archiviert && !referenziertKarte(it, editor.karte.id, mutableSetOf()) }
        .map { kartenVorlage(it) }
        .toList()

    private fun referenziertKarte(karte: KartenDaten, gesuchteId: KartenId, besucht: MutableSet<KartenVerweis>): Boolean {
        val refs = karte.knoten.mapNotNull { it.kartenVerweis }
        if (refs.any { it.kartenId == gesuchteId }) return true
        return refs.any { ref -> if (!besucht.add(ref)) false else speicher.lade(ref)?.let { referenziertKarte(it, gesuchteId, besucht) } == true }
    }

    private fun istKompatibelMitOffenerVerbindung(vorlage: KnotenVorlage): Boolean {
        val start = knotenAuswahlStart ?: return true
        val probe = vorlage.erzeuge(GraphPunkt.Zero)
        val probeKarte = editor.karte.copy(knoten = editor.karte.knoten + probe)
        return probe.anschlüsse.any { anschluss -> graphPrüfung.prüfe(probeKarte, start, AnschlussVerweis(probe.id, anschluss.id)) is VerbindungsPrüfung.Erlaubt }
    }

    private fun verbindungWennErlaubt(karte: KartenDaten, erster: AnschlussVerweis, zweiter: AnschlussVerweis): VerbindungDaten? {
        if (graphPrüfung.prüfe(karte, erster, zweiter) !is VerbindungsPrüfung.Erlaubt) return null
        val (von, zu) = graphPrüfung.normalisiere(karte, erster, zweiter) ?: return null
        return VerbindungDaten(von = von, zu = zu)
    }

    private fun KartenDaten.validierungsFehler(): String? {
        if (id.wert.isBlank()) return "Die Karten-ID darf nicht leer sein."
        if (name.isBlank()) return "Der Kartenname darf nicht leer sein."
        if (version < 1) return "Die Kartenversion muss mindestens 1 sein."
        if (!ansicht.zoom.isFinite() || ansicht.zoom <= 0f) return "Der Ansichtszoom muss eine positive Zahl sein."
        if (!ansicht.verschiebung.x.isFinite() || !ansicht.verschiebung.y.isFinite()) return "Die Ansichtsposition enthält keine gültigen Zahlen."

        val knotenIds = knoten.map { it.id }
        if (knotenIds.size != knotenIds.toSet().size) return "Knoten-IDs müssen innerhalb einer Karte eindeutig sein."
        if (knoten.any { it.id.wert.isBlank() || it.art.isBlank() || it.name.isBlank() }) {
            return "Jeder Knoten benötigt eine ID, eine Art und einen Namen."
        }
        if (knoten.any { !it.position.x.isFinite() || !it.position.y.isFinite() || !it.größe.breite.isFinite() || !it.größe.höhe.isFinite() || it.größe.breite <= 0f || it.größe.höhe <= 0f }) {
            return "Knotenpositionen und -größen müssen gültige positive Maße enthalten."
        }
        if (knoten.any { k -> k.anschlüsse.map { it.id }.let { ids -> ids.size != ids.toSet().size } }) {
            return "Anschluss-IDs müssen innerhalb eines Knotens eindeutig sein."
        }

        val verbindungsIds = verbindungen.map { it.id }
        if (verbindungsIds.size != verbindungsIds.toSet().size) return "Verbindungs-IDs müssen eindeutig sein."
        val anschlussVerweise = knoten.flatMap { k -> k.anschlüsse.map { AnschlussVerweis(k.id, it.id) } }.toSet()
        if (verbindungen.any { it.von !in anschlussVerweise || it.zu !in anschlussVerweise }) {
            return "Mindestens eine Verbindung verweist auf einen nicht vorhandenen Anschluss."
        }
        return null
    }

    private fun aktualisiereAssoziativeKnoten(karte: KartenDaten): KartenDaten =
        migriereAssoziativeKnoten(migriereAussagenOperatoren(karte))

    private fun werteAus() {
        auswertung = if (editor.karte.knoten.any { it.art.startsWith("konzept.") }) {
            KartenAuswertungsErgebnis(emptyMap(), emptyList())
        } else {
            auswerter.auswerten(editor.karte)
        }
    }
}
