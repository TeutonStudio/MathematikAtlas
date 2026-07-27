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
            val eingänge = karte.knoten.filter { it.art == "mathematik.kartenEingang" }.mapIndexed { index, intern ->
                AnschlussDaten(
                    name = intern.parameter["name"] ?: intern.name,
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = intern.anschlüsse.firstOrNull { it.name == "wert" }?.art ?: MathematikAnschlussArten.Objekt.id,
                    reihenfolge = index,
                )
            }
            val ausgänge = karte.knoten.filter { it.art == "mathematik.kartenAusgang" }.mapIndexed { index, intern ->
                AnschlussDaten(
                    name = intern.parameter["name"] ?: intern.name,
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = intern.anschlüsse.firstOrNull { it.name == "wert" }?.art ?: MathematikAnschlussArten.Objekt.id,
                    reihenfolge = index,
                )
            }
            val gruppe = KnotenVorlage(
                art = "gruppe.${karte.id.wert}",
                name = karte.name,
                kategorie = "Gespeicherte Karten",
                beschreibung = "Wiederverwendbare Karte, fest auf Version ${karte.version} verwiesen.",
                standardGröße = GraphGröße(240f, maxOf(100f, 54f + maxOf(eingänge.size, ausgänge.size) * 28f)),
                anschlüsse = eingänge + ausgänge,
                kartenVerweis = KartenVerweis(karte.id, karte.version),
            )
            listOfNotNull(gruppe, methodenVorlage(karte))
        }.toList()

    private fun methodenVorlage(karte: KartenDaten): KnotenVorlage? {
        val eingänge = karte.knoten.filter { it.art == "mathematik.kartenEingang" }
        val ausgänge = karte.knoten.filter { it.art == "mathematik.kartenAusgang" }
        if (eingänge.size != 1 || ausgänge.size != 1) return null
        val ausgang = ausgänge.single()
        val wert = ausgang.anschlüsse.firstOrNull { it.name == "wert" } ?: return null
        val zielmenge = ausgang.anschlüsse.firstOrNull { it.name == "zielmenge" } ?: return null
        val wertArt = quelleArt(karte, ausgang.id, wert.id) ?: return null
        val zielMengeArt = quelleArt(karte, ausgang.id, zielmenge.id) ?: return null
        if (!anschlussArten.istUnterart(zielMengeArt, MathematikAnschlussArten.Menge.id)) return null
        val funktionsArt = when {
            anschlussArten.istUnterart(wertArt, MathematikAnschlussArten.Zahl.id) -> MathematikAnschlussArten.ZahlFunktion.id
            anschlussArten.istUnterart(wertArt, MathematikAnschlussArten.Menge.id) -> MathematikAnschlussArten.MengenFunktion.id
            anschlussArten.istUnterart(wertArt, MathematikAnschlussArten.SpaltenVektor.id) -> MathematikAnschlussArten.SpaltenVektorFunktion.id
            anschlussArten.istUnterart(wertArt, MathematikAnschlussArten.ZeilenVektor.id) -> MathematikAnschlussArten.ZeilenVektorFunktion.id
            else -> return null
        }
        return KnotenVorlage(
            art = "methode.${karte.id.wert}",
            name = "${karte.name} (Methode)",
            kategorie = "Methoden",
            beschreibung = "Einwertige Methode; ihre Grundmenge wird aus der Zielmenge des Karten-Ausgangs abgeleitet.",
            standardGröße = GraphGröße(240f, 90f),
            anschlüsse = listOf(AnschlussDaten(
                name = "methode", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = funktionsArt,
            )),
            kartenVerweis = KartenVerweis(karte.id, karte.version),
        )
    }

    private fun quelleArt(karte: KartenDaten, zielKnoten: KnotenId, zielAnschluss: AnschlussId): AnschlussArtId? {
        val quelle = karte.verbindungen.firstOrNull { it.zu == AnschlussVerweis(zielKnoten, zielAnschluss) }?.von ?: return null
        return karte.knoten.firstOrNull { it.id == quelle.knotenId }?.anschlüsse?.firstOrNull { it.id == quelle.anschlussId }?.art
    }

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

    /** Migriert bekannte Knotendaten, darunter Karten-Ausgänge, assoziative Eingänge und die Bezeichnung „Differenz“. */
    private fun aktualisiereAssoziativeKnoten(karte: KartenDaten): KartenDaten = karte.copy(
        knoten = karte.knoten.map { ursprünglicherKnoten ->
            val knoten = if (ursprünglicherKnoten.art == "mathematik.differenz" && ursprünglicherKnoten.name == "Mengendifferenz") {
                ursprünglicherKnoten.copy(name = "Differenz")
            } else ursprünglicherKnoten
            if (knoten.art == "mathematik.kartenAusgang" && knoten.anschlüsse.none { it.name == "zielmenge" }) {
                knoten.copy(anschlüsse = knoten.anschlüsse + AnschlussDaten(
                    name = "zielmenge", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links,
                    art = MathematikAnschlussArten.Menge.id, reihenfolge = 1,
                ))
            } else if (knoten.art !in assoziativeKnotenArten) knoten
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
                    parameter = knoten.parameter + mapOf(
                        "festeEingänge" to festeEingänge.toString(),
                        "operatorAnzeige" to if (knoten.parameter["operatorAnzeige"] == "name") "name" else "wert",
                    ),
                )
            }
        },
    )

    private companion object {
        val assoziativeKnotenArten = setOf(
            "mathematik.addition", "mathematik.vereinigung", "mathematik.schnitt", "mathematik.kartesischesProdukt", "mathematik.tupel", "mathematik.vektor", "mathematik.zeilenVektor", "mathematik.matrix",
        )
    }

    private fun werteAus() { auswertung = auswerter.auswerten(editor.karte) }
}

enum class VerwaltungsBereich { Karten, Konzepte, Variablen, Auswertung, Fehler }
