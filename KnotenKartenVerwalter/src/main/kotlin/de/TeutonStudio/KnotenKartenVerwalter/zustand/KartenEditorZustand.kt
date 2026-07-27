package de.TeutonStudio.KnotenKartenVerwalter.zustand

import androidx.compose.runtime.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*

@Stable
class KartenEditorZustand(
    startKarte: KartenDaten,
    private val prüfung: GraphPrüfung,
) {
    var karte by mutableStateOf(startKarte)
        private set
    var ausgewählterKnoten by mutableStateOf<KnotenId?>(null)
        private set
    var ausgewählteVerbindung by mutableStateOf<VerbindungsId?>(null)
        private set
    var verbindungsStart by mutableStateOf<AnschlussVerweis?>(null)
        private set
    var letzteMeldung by mutableStateOf<String?>(null)
        private set
    var verbindungsVorschau by mutableStateOf<GraphPunkt?>(null)
        private set

    private val rückgängig = ArrayDeque<KartenDaten>()
    private val wiederholen = ArrayDeque<KartenDaten>()
    private var interaktionsStart: KartenDaten? = null

    fun ersetzeKarte(neu: KartenDaten, historieLeeren: Boolean = true) {
        karte = neu
        ausgewählterKnoten = null
        ausgewählteVerbindung = null
        verbindungsStart = null
        verbindungsVorschau = null
        interaktionsStart = null
        if (historieLeeren) { rückgängig.clear(); wiederholen.clear() }
    }

    fun beginneInteraktion() {
        if (interaktionsStart == null) interaktionsStart = karte
    }

    fun beendeInteraktion() {
        val start = interaktionsStart ?: return
        interaktionsStart = null
        if (start == karte) return
        rückgängig.addLast(start)
        if (rückgängig.size > 100) rückgängig.removeFirst()
        wiederholen.clear()
    }

    fun führeAus(aktion: KartenAktion, mitHistorie: Boolean = true) {
        val standVorAktion = karte.ohneUnverbundeneDynamischeEingänge()
        val neu = karte.wendeAn(aktion).ohneUnverbundeneDynamischeEingänge()
        if (neu == karte) return
        if (mitHistorie) {
            rückgängig.addLast(standVorAktion)
            if (rückgängig.size > 100) rückgängig.removeFirst()
            wiederholen.clear()
        }
        karte = neu
    }

    fun wähleKnoten(id: KnotenId?) {
        ausgewählterKnoten = id
        ausgewählteVerbindung = null
    }

    fun wähleVerbindung(id: VerbindungsId?) {
        ausgewählteVerbindung = id
        ausgewählterKnoten = null
    }


    fun beginneVerbindung(ref: AnschlussVerweis, position: GraphPunkt? = null) {
        verbindungsStart = ref
        verbindungsVorschau = position
        fügeDynamischeEingängeHinzu(ref)
        letzteMeldung = "Verbindung ziehen oder einen Gegenanschluss wählen."
    }

    fun aktualisiereVerbindungsVorschau(position: GraphPunkt) { verbindungsVorschau = position }

    fun beendeVerbindungsVorschau(startBeibehalten: Boolean = false) {
        verbindungsVorschau = null
        if (!startBeibehalten) verbindungsStart = null
        if (!startBeibehalten) entferneUnverbundeneDynamischeEingänge()
    }

    fun anschlussAngeklickt(ref: AnschlussVerweis) {
        val start = verbindungsStart
        if (start == null) {
            verbindungsStart = ref
            fügeDynamischeEingängeHinzu(ref)
            letzteMeldung = "Anschluss gewählt. Wähle einen kompatiblen Gegenanschluss."
            return
        }
        if (start == ref) {
            verbindungsStart = null
            letzteMeldung = null
            entferneUnverbundeneDynamischeEingänge()
            return
        }
        when (val ergebnis = prüfung.prüfe(karte, start, ref)) {
            VerbindungsPrüfung.Erlaubt -> {
                val normal = prüfung.normalisiere(karte, start, ref)
                if (normal != null) führeAus(KartenAktion.VerbindungEinfügen(VerbindungDaten(von = normal.first, zu = normal.second)))
                verbindungsStart = null
                verbindungsVorschau = null
                letzteMeldung = null
            }
            is VerbindungsPrüfung.Abgelehnt -> {
                letzteMeldung = ergebnis.grund
                verbindungsStart = null
                verbindungsVorschau = null
                entferneUnverbundeneDynamischeEingänge()
            }
        }
    }

    fun kompatibelMitStart(ref: AnschlussVerweis): Boolean {
        val start = verbindungsStart ?: return true
        return prüfung.prüfe(karte, start, ref) is VerbindungsPrüfung.Erlaubt
    }

    fun löscheAuswahl() {
        ausgewählterKnoten?.let { führeAus(KartenAktion.KnotenLöschen(it)) }
        ausgewählteVerbindung?.let { führeAus(KartenAktion.VerbindungLöschen(it)) }
        ausgewählterKnoten = null
        ausgewählteVerbindung = null
    }

    fun dupliziereAuswahl() {
        val original = karte.knoten.firstOrNull { it.id == ausgewählterKnoten } ?: return
        val neueAnschlüsse = original.anschlüsse.map { it.copy(id = neueAnschlussId()) }
        val kopie = original.copy(
            id = neueKnotenId(),
            name = "${original.name} Kopie",
            position = original.position + GraphPunkt(28f, 28f),
            anschlüsse = neueAnschlüsse,
        )
        führeAus(KartenAktion.KnotenEinfügen(kopie))
        wähleKnoten(kopie.id)
    }

    /** Entfernt alle eingehenden und ausgehenden Verbindungen des ausgewählten Knotens. */
    fun isoliereAusgewähltenKnoten() {
        val id = ausgewählterKnoten ?: return
        führeAus(KartenAktion.KnotenIsolieren(id))
    }

    /** Legt die dauerhaft sichtbare Mindestzahl der Eingänge eines erweiterbaren Knotens fest. */
    fun setzeFesteEingangAnzahl(knotenId: KnotenId, anzahl: Int) {
        val gewünschteAnzahl = anzahl.coerceAtLeast(2)
        val vorher = karte
        val verbindungen = karte.verbindungen.map { it.zu }.toSet()
        val neu = karte.copy(knoten = karte.knoten.map { knoten ->
            if (knoten.id != knotenId) return@map knoten
            val vorlage = knoten.anschlüsse.firstOrNull {
                it.richtung == AnschlussRichtung.Eingang && it.kannSichErweitern
            } ?: return@map knoten
            val feste = knoten.anschlüsse.filter {
                it.richtung == AnschlussRichtung.Eingang && !it.dynamischErzeugt
            }
            val zusätzliche = if (feste.size < gewünschteAnzahl) {
                (feste.size until gewünschteAnzahl).map { index ->
                    vorlage.copy(
                        id = neueAnschlussId(),
                        name = "input${knoten.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang } + index - feste.size + 1}",
                        reihenfolge = knoten.anschlüsse.maxOfOrNull { it.reihenfolge }?.plus(index - feste.size + 1) ?: index,
                    )
                }
            } else emptyList()
            val zuEntfernen = if (feste.size > gewünschteAnzahl) feste
                .filter { AnschlussVerweis(knoten.id, it.id) !in verbindungen }
                .sortedByDescending { it.reihenfolge }
                .take(feste.size - gewünschteAnzahl)
                .map { it.id }
                .toSet()
            else emptySet()
            knoten.copy(
                anschlüsse = (knoten.anschlüsse.filterNot { it.id in zuEntfernen } + zusätzliche),
                parameter = knoten.parameter + ("festeEingänge" to gewünschteAnzahl.toString()),
            )
        }).ohneUnverbundeneDynamischeEingänge()
        if (neu == vorher) return
        rückgängig.addLast(vorher)
        if (rückgängig.size > 100) rückgängig.removeFirst()
        wiederholen.clear()
        karte = neu
    }

    /** Vertauscht einen Eingang mit seinem direkten Nachbarn und erhält alle Verbindungen über die Anschluss-IDs. */
    fun verschiebeEingang(knotenId: KnotenId, anschlussId: AnschlussId, richtung: Int) {
        val knoten = karte.knoten.firstOrNull { it.id == knotenId } ?: return
        val eingänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
        val index = eingänge.indexOfFirst { it.id == anschlussId }
        val ziel = index + richtung
        if (index !in eingänge.indices || ziel !in eingänge.indices) return
        val links = eingänge[index]
        val rechts = eingänge[ziel]
        val neu = knoten.anschlüsse.map {
            when (it.id) {
                links.id -> it.copy(reihenfolge = rechts.reihenfolge)
                rechts.id -> it.copy(reihenfolge = links.reihenfolge)
                else -> it
            }
        }
        führeAus(KartenAktion.KnotenAnschlüsseÄndern(knotenId, neu))
    }

    fun kannRückgängig() = rückgängig.isNotEmpty()
    fun kannWiederholen() = wiederholen.isNotEmpty()

    fun rückgängig() {
        if (rückgängig.isEmpty()) return
        wiederholen.addLast(karte)
        karte = rückgängig.removeLast()
    }

    fun wiederholen() {
        if (wiederholen.isEmpty()) return
        rückgängig.addLast(karte)
        karte = wiederholen.removeLast()
    }

    /** Assoziative Knoten bieten beim Ziehen eines passenden Kabels einen vorläufigen Eingang an. */
    private fun fügeDynamischeEingängeHinzu(start: AnschlussVerweis) {
        var erweitert = karte
        karte.knoten.forEach { knoten ->
            val vorlage = knoten.anschlüsse.firstOrNull {
                it.richtung == AnschlussRichtung.Eingang && it.kannSichErweitern
            } ?: return@forEach
            val neuerAnschluss = vorlage.copy(
                id = neueAnschlussId(),
                name = if (vorlage.name.matches(Regex("[A-Za-zÄÖÜäöü]+\\d+"))) {
                    val präfix = vorlage.name.dropLastWhile(Char::isDigit)
                    "$präfix${knoten.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang && it.name.startsWith(präfix) } + 1}"
                } else "input${knoten.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang } + 1}",
                reihenfolge = knoten.anschlüsse.filter { it.kante == vorlage.kante }.maxOfOrNull { it.reihenfolge }?.plus(1) ?: 0,
                dynamischErzeugt = true,
            )
            val probe = erweitert.copy(knoten = erweitert.knoten.map {
                if (it.id == knoten.id) it.copy(anschlüsse = it.anschlüsse + neuerAnschluss) else it
            })
            if (prüfung.prüfe(probe, start, AnschlussVerweis(knoten.id, neuerAnschluss.id)) is VerbindungsPrüfung.Erlaubt) {
                erweitert = probe
            }
        }
        karte = erweitert
    }

    private fun entferneUnverbundeneDynamischeEingänge() {
        karte = karte.ohneUnverbundeneDynamischeEingänge()
    }

    private fun KartenDaten.ohneUnverbundeneDynamischeEingänge(): KartenDaten {
        val verbundeneEingänge = verbindungen.map { it.zu }.toSet()
        return copy(knoten = knoten.map { knoten ->
            knoten.copy(anschlüsse = knoten.anschlüsse.filterNot {
                it.dynamischErzeugt && AnschlussVerweis(knoten.id, it.id) !in verbundeneEingänge
            })
        })
    }
}

@Composable
fun merkeKartenEditorZustand(startKarte: KartenDaten, prüfung: GraphPrüfung): KartenEditorZustand =
    remember(startKarte.id, prüfung) { KartenEditorZustand(startKarte, prüfung) }
