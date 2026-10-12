package de.TeutonStudio.KnotenKartenVerwalter.zustand

import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*

enum class AuswahlModus { Einzeln, Gruppe }
enum class AuswahlÄnderung { Ersetzen, Hinzufügen, Umschalten }
enum class VerbindungsAblageErgebnis { NeueVerbindungAufHintergrund, BestehendeVerbindungGelöst, StartBeibehalten, KeineAktiveVerbindung }

@Stable
class KartenEditorZustand(
    startKarte: KartenDaten,
    private val prüfung: GraphPrüfung,
) {
    var karte by mutableStateOf(
        startKarte.bereinigteVisuelleGruppen(),
    )
        private set
    var auswertungsRevision by mutableStateOf(0L)
        private set
    var auswahlModus by mutableStateOf(AuswahlModus.Einzeln)
        private set
    var ausgewählteKnoten by mutableStateOf<Set<KnotenId>>(emptySet())
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

    var geöffneteGruppe by mutableStateOf<VisuelleGruppenId?>(null)
        private set
    var ausgewählteGruppe by mutableStateOf<VisuelleGruppenId?>(null)
        private set
    private var gruppenÜbergangsToken: Any? = null
    fun beginneGruppenÜbergang(token: Any) { gruppenÜbergangsToken = token; gruppenÜbergangAktiv = true }
    fun beendeGruppenÜbergang(token: Any) { if (gruppenÜbergangsToken === token) { gruppenÜbergangsToken = null; gruppenÜbergangAktiv = false } }
    var gruppenÜbergangAktiv by mutableStateOf(false)
    var gruppenInspektorOffen by mutableStateOf(false)
    private var projektionsQuelle: KartenDaten? = null
    private var projektionsGruppe: VisuelleGruppenId? = null
    private var projektionsCache by mutableStateOf<GruppenProjektion?>(null)
    var projektionImHintergrund = false
    private fun hatAktuelleProjektion(): Boolean {
        val alt = projektionsQuelle ?: return false
        return alt.id == karte.id && alt.knoten == karte.knoten && alt.verbindungen == karte.verbindungen &&
            alt.visuelleGruppen.hatGleicheGruppenProjektionWie(karte.visuelleGruppen) && projektionsGruppe == geöffneteGruppe
    }
    val projektionWirdBerechnet: Boolean get() = projektionImHintergrund && karte.knoten.size >= 500 && !hatAktuelleProjektion()
    suspend fun berechneProjektionImHintergrund() {
        val quelle = karte
        val gruppe = geöffneteGruppe
        val ergebnis = withContext(Dispatchers.Default) { quelle.gruppenProjektion(gruppe, kotlin.coroutines.coroutineContext) }
        if (quelle.id == karte.id && quelle.knoten == karte.knoten && quelle.verbindungen == karte.verbindungen &&
            quelle.visuelleGruppen.hatGleicheGruppenProjektionWie(karte.visuelleGruppen) && gruppe == geöffneteGruppe) {
            projektionsQuelle = quelle
            projektionsGruppe = gruppe
            projektionsCache = ergebnis
            bereinigeAuswahl()
        }
    }
    val projektion: GruppenProjektion
        get() {
            if (!hatAktuelleProjektion()) {
                if (projektionImHintergrund && karte.knoten.size >= 500) {
                    return projektionsCache?.takeIf { projektionsQuelle?.id == karte.id && projektionsGruppe == geöffneteGruppe }
                        ?: GruppenProjektion(karte.copy(knoten = emptyList(), verbindungen = emptyList(), visuelleGruppen = emptyList()), emptyMap(), emptyMap(), emptyMap(), emptySet())
                }
                projektionsCache = karte.gruppenProjektion(geöffneteGruppe)
                projektionsQuelle = karte
                projektionsGruppe = geöffneteGruppe
            }
            return projektionsCache!!
        }
    private var inhaltsMinima by mutableStateOf<Map<KnotenId, GraphGröße>>(emptyMap())

    fun meldeInhaltsminimum(id: KnotenId, minimum: GraphGröße) {
        if (!minimum.breite.isFinite() || !minimum.höhe.isFinite() || minimum.breite <= 0f || minimum.höhe <= 0f) return
        if (inhaltsMinima[id] != minimum) inhaltsMinima = inhaltsMinima + (id to minimum)
    }

    val darstellungsKarte: KartenDaten
        get() = projektion.karte.mitInhaltsminima(inhaltsMinima).copy(ansicht = geöffneteGruppe?.let { id -> karte.visuelleGruppen.firstOrNull { it.id == id }?.ansicht } ?: karte.ansicht)

    fun öffneGruppe(id: VisuelleGruppenId?) {
        if (id != null && karte.visuelleGruppen.none { it.id == id }) return
        brecheVerbindungsVorschauAb()
        geöffneteGruppe = id
        wähleKnoten(null)
        ausgewählteGruppe = null
        gruppenInspektorOffen = false
    }

    fun wähleGruppe(id: VisuelleGruppenId?, inspektor: Boolean = false) {
        wähleKnoten(null)
        ausgewählteGruppe = id
        ausgewählteKnoten = id?.let { GruppenIndex(karte).knotenIn(it) } ?: emptySet()
        gruppenInspektorOffen = inspektor && id != null
    }

    private val rückgängig = ArrayDeque<KartenDaten>()
    private val wiederholen = ArrayDeque<KartenDaten>()
    private var rückgängigVerfügbar by mutableStateOf(false)
    private var wiederholenVerfügbar by mutableStateOf(false)
    private var interaktionsStart: KartenDaten? = null
    /** Verbindung, deren Eingangsende während einer Neuverdrahtung gerade frei gezogen wird. */
    private var neuZuVerdrahtendeVerbindung: VerbindungDaten? = null

    fun ersetzeKarte(neu: KartenDaten, historieLeeren: Boolean = true) {
        val vorher = karte
        if (neu.id != karte.id) { öffneGruppe(null); inhaltsMinima = emptyMap() }
        karte = neu.bereinigteVisuelleGruppen()
        if (!vorher.hatGleichenAuswertungsinhaltWie(karte)) auswertungsRevision += 1
        ausgewählteKnoten = emptySet()
        ausgewählterKnoten = null
        ausgewählteVerbindung = null
        verbindungsStart = null
        verbindungsVorschau = null
        neuZuVerdrahtendeVerbindung = null
        letzteMeldung = null
        interaktionsStart = null
        if (historieLeeren) leereHistorie()
    }

    fun beginneInteraktion() {
        if (interaktionsStart == null) interaktionsStart = karte
    }

    fun beendeInteraktion() {
        val start = interaktionsStart ?: return
        interaktionsStart = null
        karte = karte.bereinigteVisuelleGruppen().mitAutomatischenVisuellenGruppenMitgliedschaften()
        if (start == karte) return
        merkeFürRückgängig(start)
    }

    /** Verwirft die noch nicht bestätigte Gestenänderung, ohne einen Historieneintrag anzulegen. */
    fun verwerfeLaufendeInteraktion(): Boolean {
        val start = interaktionsStart ?: return false
        interaktionsStart = null
        karte = start
        bereinigeAuswahl()
        return true
    }

    fun führeAus(aktion: KartenAktion, mitHistorie: Boolean = true, dynamischeEingängeErhalten: Boolean = false) {
        if (aktion is KartenAktion.AnsichtÄndern) {
            val gruppe = geöffneteGruppe
            if (gruppe != null) karte = karte.wendeAn(KartenAktion.VisuelleGruppeAnsichtÄndern(gruppe, aktion.ansicht))
            else if (karte.ansicht != aktion.ansicht) karte = karte.copy(ansicht = aktion.ansicht)
            return
        }
        val begrenzteAktion = if (aktion is KartenAktion.KnotenGrößeÄndern) {
            val minimum = inhaltsMinima[aktion.id]
            if (minimum == null) aktion else aktion.copy(größe = GraphGröße(
                maxOf(aktion.größe.breite, minimum.breite), maxOf(aktion.größe.höhe, minimum.höhe)))
        } else aktion
        val wirksameAktion = if (
            aktion is KartenAktion.KnotenVerschieben &&
            ausgewählteKnoten.size > 1 &&
            aktion.id in ausgewählteKnoten
        ) {
            val aktuell = karte.knoten.firstOrNull { it.id == aktion.id }
            val delta = aktuell?.let { aktion.position - it.position } ?: GraphPunkt.Zero
            KartenAktion.KnotenMehrfachVerschieben(ausgewählteKnoten, delta)
        } else begrenzteAktion
        var angewendet = karte.wendeAn(wirksameAktion)
        val neuEingefügt = when (wirksameAktion) {
            is KartenAktion.KnotenEinfügen -> listOf(wirksameAktion.knoten)
            is KartenAktion.KnotenMehrfachEinfügen -> wirksameAktion.knoten
            else -> emptyList()
        }
        geöffneteGruppe?.let { gid ->
            if (neuEingefügt.isNotEmpty()) {
                val index = GruppenIndex(angewendet)
                val ids = index.knotenIn(gid) + neuEingefügt.map { it.id }
                val geometrie = angewendet.visuelleGruppenGeometrieFür(ids)
                angewendet = angewendet.copy(visuelleGruppen = angewendet.visuelleGruppen.map { g ->
                    if (g.id == gid) g.copy(knotenIds = g.knotenIds + neuEingefügt.map { it.id },
                        position = geometrie?.let { GraphPunkt(minOf(g.position.x,it.first.x),minOf(g.position.y,it.first.y)) } ?: g.position,
                        größe = geometrie?.let {
                            GraphGröße(maxOf(g.position.x+g.größe.breite,it.first.x+it.second.breite)-minOf(g.position.x,it.first.x),
                                maxOf(g.position.y+g.größe.höhe,it.first.y+it.second.höhe)-minOf(g.position.y,it.first.y))
                        } ?: g.größe) else g
                })
                var kind = angewendet.visuelleGruppen.first { it.id == gid }
                index.vorfahren(gid).forEach { elternId ->
                    angewendet = angewendet.copy(visuelleGruppen = angewendet.visuelleGruppen.map { eltern ->
                        if (eltern.id != elternId) eltern else {
                            val x = minOf(eltern.position.x,kind.position.x)
                            val y = minOf(eltern.position.y,kind.position.y - VISUELLE_GRUPPE_KOPFZEILE_HÖHE)
                            eltern.copy(position = GraphPunkt(x,y), größe = GraphGröße(
                                maxOf(eltern.position.x + eltern.größe.breite,kind.position.x + kind.größe.breite)-x,
                                maxOf(eltern.position.y + eltern.größe.höhe,kind.position.y + kind.größe.höhe)-y))
                        }
                    })
                    kind = angewendet.visuelleGruppen.first { it.id == elternId }
                }
            }
        }
        val reineUmordnung = !wirksameAktion.istAuswertungsRelevant() || dynamischeEingängeErhalten || karte.istReineAnschlussUmordnung(angewendet)
        val standVorAktion = (if (reineUmordnung) karte else karte.ohneUnverbundeneDynamischeEingänge()).bereinigteVisuelleGruppen()
        var neu = (if (reineUmordnung) angewendet else angewendet.ohneUnverbundeneDynamischeEingänge())
            .bereinigteVisuelleGruppen()
        if (interaktionsStart == null && wirksameAktion.ändertGruppenGeometrie()) neu = neu.mitAutomatischenVisuellenGruppenMitgliedschaften()
        if (neu == karte) return
        val historienRelevant = mitHistorie && wirksameAktion !is KartenAktion.AnsichtÄndern
        if (historienRelevant) merkeFürRückgängig(standVorAktion)
        karte = neu
        if (geöffneteGruppe != null && karte.visuelleGruppen.none { it.id == geöffneteGruppe }) öffneGruppe(null)
        if (wirksameAktion.istAuswertungsRelevant()) auswertungsRevision += 1
        bereinigeAuswahl()
    }

    fun wähleKnoten(id: KnotenId?, änderung: AuswahlÄnderung = AuswahlÄnderung.Ersetzen) {
        projektion.blöcke[id]?.let { wähleGruppe(it); return }
        if (id in projektion.portale) {
            val ziel = projektion.originalAnschlüsse.entries.firstOrNull { it.key.knotenId == id }?.value?.knotenId
            if (ziel != null) wähleKnoten(ziel, änderung)
            return
        }
        ausgewählteGruppe = null
        if (id == null) {
            ausgewählteKnoten = emptySet()
            ausgewählterKnoten = null
        } else {
            val effektiv = if (änderung == AuswahlÄnderung.Ersetzen && auswahlModus == AuswahlModus.Gruppe) {
                AuswahlÄnderung.Hinzufügen
            } else änderung
            ausgewählteKnoten = when (effektiv) {
                AuswahlÄnderung.Ersetzen -> setOf(id)
                AuswahlÄnderung.Hinzufügen -> ausgewählteKnoten + id
                AuswahlÄnderung.Umschalten -> if (id in ausgewählteKnoten) ausgewählteKnoten - id else ausgewählteKnoten + id
            }
            ausgewählterKnoten = when {
                id in ausgewählteKnoten -> id
                else -> ausgewählteKnoten.lastOrNull()
            }
        }
        ausgewählteVerbindung = null
    }

    fun wähleAlleKnoten() {
        ausgewählteKnoten = darstellungsKarte.knoten.filter { it.id !in projektion.blöcke && it.id !in projektion.portale }.mapTo(linkedSetOf()) { it.id }
        ausgewählterKnoten = ausgewählteKnoten.lastOrNull()
        ausgewählteVerbindung = null
    }

    fun wähleKnotenImBereich(bereich: androidx.compose.ui.geometry.Rect, änderung: AuswahlÄnderung) {
        val treffer = darstellungsKarte.knoten.asSequence().filter { it.id !in projektion.blöcke && it.id !in projektion.portale }.filter { knoten ->
            val links = knoten.position.x
            val oben = knoten.position.y
            val rechts = links + knoten.größe.breite
            val unten = oben + knoten.größe.höhe
            rechts >= bereich.left && links <= bereich.right && unten >= bereich.top && oben <= bereich.bottom
        }.map { it.id }.toSet()
        ausgewählteKnoten = when (änderung) {
            AuswahlÄnderung.Ersetzen -> treffer
            AuswahlÄnderung.Hinzufügen -> ausgewählteKnoten + treffer
            AuswahlÄnderung.Umschalten -> (ausgewählteKnoten - treffer) + (treffer - ausgewählteKnoten)
        }
        ausgewählterKnoten = ausgewählteKnoten.lastOrNull()
        ausgewählteVerbindung = null
    }

    fun verschiebeAuswahl(delta: GraphPunkt, mitHistorie: Boolean = true) {
        ausgewählteGruppe?.let { führeAus(KartenAktion.VisuelleGruppeVerschieben(it,delta),mitHistorie); return }
        if (ausgewählteKnoten.isEmpty() || delta == GraphPunkt.Zero) return
        führeAus(KartenAktion.KnotenMehrfachVerschieben(ausgewählteKnoten, delta), mitHistorie)
    }

    fun wähleRäumlichNächsten(richtung: GraphPunkt): Boolean {
        val sichtbareKnoten = darstellungsKarte.knoten.filter { it.id !in projektion.portale }
        val aktuell = sichtbareKnoten.firstOrNull { it.id == ausgewählterKnoten || projektion.blöcke[it.id] == ausgewählteGruppe && ausgewählteGruppe != null } ?: sichtbareKnoten.firstOrNull() ?: return false
        val mitte = GraphPunkt(aktuell.position.x + aktuell.größe.breite / 2f, aktuell.position.y + aktuell.größe.höhe / 2f)
        val kandidat = sichtbareKnoten.asSequence().filter { it.id != aktuell.id }.mapNotNull { knoten ->
            val delta = GraphPunkt(
                knoten.position.x + knoten.größe.breite / 2f - mitte.x,
                knoten.position.y + knoten.größe.höhe / 2f - mitte.y,
            )
            val vorwärts = delta.x * richtung.x + delta.y * richtung.y
            if (vorwärts <= 0f) null else knoten to (delta.x * delta.x + delta.y * delta.y + 3f * vorwärts)
        }.minByOrNull { it.second }?.first ?: return false
        wähleKnoten(kandidat.id)
        return true
    }

    fun brecheInteraktionAb(): Boolean {
        if (verbindungsStart != null || verbindungsVorschau != null) {
            verwerfeVerbindungsInteraktion()
            return true
        }
        if (ausgewählteKnoten.isNotEmpty() || ausgewählteVerbindung != null) {
            wähleKnoten(null)
            return true
        }
        return false
    }

    fun setzeAuswahlModus(modus: AuswahlModus) {
        if (auswahlModus == modus) return
        auswahlModus = modus
        if (modus == AuswahlModus.Einzeln) {
            ausgewählteKnoten = ausgewählterKnoten?.let(::setOf) ?: emptySet()
        }
    }

    fun stelleAuswahlWiederHer(ids: Set<KnotenId>, aktiv: KnotenId?) {
        val gültigeIds = ids.intersect(karte.knoten.mapTo(mutableSetOf()) { it.id })
        ausgewählteKnoten = if (auswahlModus == AuswahlModus.Einzeln) {
            aktiv?.takeIf { it in gültigeIds }?.let(::setOf) ?: emptySet()
        } else gültigeIds
        ausgewählterKnoten = aktiv?.takeIf { it in ausgewählteKnoten } ?: ausgewählteKnoten.lastOrNull()
        ausgewählteVerbindung = null
    }

    fun wähleVerbindung(id: VerbindungsId?) {
        ausgewählteVerbindung = id
        ausgewählteKnoten = emptySet()
        ausgewählterKnoten = null
    }

    /**
     * Beginnt eine neue Verbindung. Wird ein bereits belegter Eingang gegriffen,
     * bleibt der bisherige Ausgang verankert und das Eingangsende wird neu gezogen.
     */
    fun beginneVerbindung(darstellungsRef: AnschlussVerweis, position: GraphPunkt? = null) {
        val ref = projektion.original(darstellungsRef)
        if (gruppenÜbergangAktiv || darstellungsRef.knotenId in projektion.portale) return
        val anschluss = karte.findeAnschluss(ref)
        val bestehend = if (anschluss?.richtung == AnschlussRichtung.Eingang) {
            karte.verbindungen.firstOrNull { it.zu == ref }
        } else null
        neuZuVerdrahtendeVerbindung = bestehend
        val effektiverStart = bestehend?.von ?: ref
        verbindungsStart = effektiverStart
        verbindungsVorschau = position
        fügeDynamischeEingängeHinzu(effektiverStart)
        letzteMeldung = if (bestehend != null) {
            "Bestehende Verbindung neu verbinden oder auf dem Hintergrund lösen."
        } else {
            "Verbindung ziehen oder einen Gegenanschluss wählen."
        }
    }

    fun aktualisiereVerbindungsVorschau(position: GraphPunkt) { verbindungsVorschau = position }

    /**
     * Beendet einen Drag ohne Ziel. Bei einer Neuverdrahtung entspricht das dem
     * bewussten Abziehen des Eingangs und löscht die alte Verbindung als einen Undo-Schritt.
     */
    fun beendeVerbindungsVorschau(startBeibehalten: Boolean = false): VerbindungsAblageErgebnis {
        val hatteAktivenStart = verbindungsStart != null
        verbindungsVorschau = null
        if (startBeibehalten) return if (hatteAktivenStart) {
            VerbindungsAblageErgebnis.StartBeibehalten
        } else VerbindungsAblageErgebnis.KeineAktiveVerbindung

        val abgezogeneVerbindung = neuZuVerdrahtendeVerbindung
        verbindungsStart = null
        neuZuVerdrahtendeVerbindung = null
        letzteMeldung = null
        if (abgezogeneVerbindung != null && karte.verbindungen.any { it.id == abgezogeneVerbindung.id }) {
            führeAus(KartenAktion.VerbindungLöschen(abgezogeneVerbindung.id))
            return VerbindungsAblageErgebnis.BestehendeVerbindungGelöst
        } else {
            entferneUnverbundeneDynamischeEingänge()
            return if (hatteAktivenStart) {
                VerbindungsAblageErgebnis.NeueVerbindungAufHintergrund
            } else VerbindungsAblageErgebnis.KeineAktiveVerbindung
        }
    }

    /** Bricht einen Pointer-Drag ab, ohne eine bestehende Verbindung zu verändern. */
    fun brecheVerbindungsVorschauAb() {
        verwerfeVerbindungsInteraktion()
    }

    fun anschlussAngeklickt(darstellungsRef: AnschlussVerweis) {
        if (gruppenÜbergangAktiv) return
        val ref = projektion.original(darstellungsRef)
        val start = verbindungsStart
        if (start == null) {
            beginneVerbindung(ref)
            letzteMeldung = if (neuZuVerdrahtendeVerbindung != null) {
                "Bestehende Verbindung gewählt. Wähle einen neuen kompatiblen Eingang."
            } else {
                "Anschluss gewählt. Wähle einen kompatiblen Gegenanschluss."
            }
            return
        }
        if (start == ref) {
            verwerfeVerbindungsInteraktion()
            return
        }

        // Die bisher verschobene Kante gehört nicht zum Graphen, gegen den die neue
        // Position geprüft wird. Andernfalls könnte sie fälschlich einen Zyklus melden.
        val verschoben = neuZuVerdrahtendeVerbindung
        val prüfKarte = if (verschoben != null) {
            karte.copy(verbindungen = karte.verbindungen.filterNot { it.id == verschoben.id })
        } else karte
        when (val ergebnis = prüfung.prüfe(prüfKarte, start, ref)) {
            VerbindungsPrüfung.Erlaubt -> {
                val normal = prüfung.normalisiere(prüfKarte, start, ref)
                if (normal == null) {
                    letzteMeldung = "Die Verbindung konnte nicht normalisiert werden."
                    verwerfeVerbindungsInteraktion(meldungBeibehalten = true)
                    return
                }
                val unverändert = verschoben?.let { it.von == normal.first && it.zu == normal.second } == true ||
                    verschoben == null && karte.verbindungen.any { it.von == normal.first && it.zu == normal.second }
                if (!unverändert) {
                    val neueVerbindung = VerbindungDaten(von = normal.first, zu = normal.second)
                    if (verschoben != null) {
                        führeAus(KartenAktion.VerbindungNeuVerbinden(verschoben.id, neueVerbindung))
                    } else {
                        führeAus(KartenAktion.VerbindungEinfügen(neueVerbindung))
                    }
                } else {
                    entferneUnverbundeneDynamischeEingänge()
                }
                verbindungsStart = null
                verbindungsVorschau = null
                neuZuVerdrahtendeVerbindung = null
                letzteMeldung = null
            }
            is VerbindungsPrüfung.Abgelehnt -> {
                letzteMeldung = ergebnis.grund
                verbindungsStart = null
                verbindungsVorschau = null
                neuZuVerdrahtendeVerbindung = null
                entferneUnverbundeneDynamischeEingänge()
            }
        }
    }

    fun kompatibelMitStart(darstellungsRef: AnschlussVerweis): Boolean {
        if (gruppenÜbergangAktiv || darstellungsRef.knotenId in projektion.portale) return false
        val ref = projektion.original(darstellungsRef)
        val start = verbindungsStart ?: return true
        // Der aktive Start bleibt anklickbar, damit der Zwei-Klick-Modus durch einen
        // zweiten Klick auf denselben Anschluss zuverlässig abgebrochen werden kann.
        if (start == ref) return true
        val verschoben = neuZuVerdrahtendeVerbindung
        val prüfKarte = if (verschoben != null) {
            karte.copy(verbindungen = karte.verbindungen.filterNot { it.id == verschoben.id })
        } else karte
        return prüfung.prüfe(prüfKarte, start, ref) is VerbindungsPrüfung.Erlaubt
    }

    fun löscheAuswahl() {
        ausgewählteGruppe?.let {
            führeAus(KartenAktion.VisuelleGruppeLöschen(it))
            wähleGruppe(null)
            return
        }
        val knotenIds = ausgewählteKnoten.ifEmpty { setOfNotNull(ausgewählterKnoten) }
        when {
            knotenIds.isNotEmpty() -> führeAus(KartenAktion.KnotenMehrfachLöschen(knotenIds))
            ausgewählteVerbindung != null -> führeAus(KartenAktion.VerbindungLöschen(ausgewählteVerbindung!!))
        }
        ausgewählteKnoten = emptySet()
        ausgewählterKnoten = null
        ausgewählteVerbindung = null
    }

    fun schneideAuswahlAus() {
        val gruppe = ausgewählteGruppe
        if (gruppe == null) { löscheAuswahl(); return }
        val index = GruppenIndex(karte)
        val gruppenIds = index.nachfahren(gruppe)
        val knotenIds = index.knotenIn(gruppe)
        führeAus(KartenAktion.KartenInhaltErsetzen(karte.copy(
            knoten = karte.knoten.filterNot { it.id in knotenIds },
            verbindungen = karte.verbindungen.filterNot { it.von.knotenId in knotenIds || it.zu.knotenId in knotenIds },
            visuelleGruppen = karte.visuelleGruppen.filterNot { it.id in gruppenIds },
        )))
        wähleGruppe(null)
    }

    fun dupliziereAuswahl() {
        ausgewählteGruppe?.let { gid ->
            val zwischenablage = AtlasZwischenablage()
            if (zwischenablage.kopiere(karte, GruppenIndex(karte).knotenIn(gid))) {
                val g = karte.visuelleGruppen.first { it.id == gid }
                zwischenablage.erzeugeEinfügung(g.position + GraphPunkt(28f,70f))?.let { a ->
                    führeAus(KartenAktion.KnotenMehrfachEinfügen(a.knoten,a.verbindungen,a.gruppen))
                }
            }
            return
        }
        val ids = ausgewählteKnoten.ifEmpty { setOfNotNull(ausgewählterKnoten) }
        val originale = karte.knoten.filter { it.id in ids }
        if (originale.isEmpty()) return
        val knotenIds = originale.associate { it.id to neueKnotenId() }
        val anschlussIds = originale.flatMap { it.anschlüsse }.associate { it.id to neueAnschlussId() }
        val kopien = originale.map { original ->
            original.copy(
                id = knotenIds.getValue(original.id),
                name = "${original.name} Kopie",
                position = original.position + GraphPunkt(28f, 28f),
                anschlüsse = original.anschlüsse.map { it.copy(id = anschlussIds.getValue(it.id)) },
            )
        }
        val interneVerbindungen = karte.verbindungen.filter {
            it.von.knotenId in ids && it.zu.knotenId in ids
        }.map { verbindung ->
            VerbindungDaten(
                von = AnschlussVerweis(
                    knotenIds.getValue(verbindung.von.knotenId),
                    anschlussIds.getValue(verbindung.von.anschlussId),
                ),
                zu = AnschlussVerweis(
                    knotenIds.getValue(verbindung.zu.knotenId),
                    anschlussIds.getValue(verbindung.zu.anschlussId),
                ),
            )
        }
        führeAus(KartenAktion.KnotenMehrfachEinfügen(kopien, interneVerbindungen))
        ausgewählteKnoten = kopien.mapTo(mutableSetOf()) { it.id }
        ausgewählterKnoten = kopien.lastOrNull()?.id
        ausgewählteVerbindung = null
    }

    fun gruppiereAuswahlVisuell() {
        if (ausgewählteKnoten.size < 2) return
        führeAus(KartenAktion.VisuelleGruppeErstellen(ausgewählteKnoten))
    }

    fun hebeVisuelleGruppierungDerAuswahlAuf() {
        if (ausgewählteKnoten.isEmpty()) return
        führeAus(KartenAktion.VisuelleGruppierungAufheben(ausgewählteKnoten))
    }

    fun auswahlIstVisuellGruppiert(): Boolean = karte.visuelleGruppen.any { gruppe ->
        gruppe.knotenIds.any { it in ausgewählteKnoten }
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
        }).ohneUnverbundeneDynamischeEingänge().bereinigteVisuelleGruppen()
        if (neu == vorher) return
        merkeFürRückgängig(vorher)
        karte = neu
        auswertungsRevision += 1
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

    /** Ändert die Art eines Anschlusses als einen Undo/Redo-Schritt und bereinigt inkompatible Kanten. */
    fun ändereAnschlussArt(ref: AnschlussVerweis, art: AnschlussArtId) {
        val vorher = karte
        val neu = prüfung.ändereAnschlussArt(vorher, ref, art)
            .ohneUnverbundeneDynamischeEingänge()
            .bereinigteVisuelleGruppen()
        if (neu == vorher) return
        merkeFürRückgängig(vorher)
        karte = neu
        auswertungsRevision += 1
    }

    fun kannRückgängig() = rückgängigVerfügbar
    fun kannWiederholen() = wiederholenVerfügbar

    fun rückgängig() {
        if (rückgängig.isEmpty()) return
        val aktuelleAnsicht = karte.ansicht
        verwerfeVerbindungsInteraktion()
        wiederholen.fügeBegrenztHinzu(karte)
        val vorher = karte
        karte = rückgängig.removeLast().mitGruppenAnsichtenAus(vorher)
            .copy(ansicht = aktuelleAnsicht)
            .bereinigteVisuelleGruppen()
        if (!vorher.hatGleichenAuswertungsinhaltWie(karte)) auswertungsRevision += 1
        aktualisiereHistorienStatus()
        bereinigeAuswahl()
    }

    fun wiederholen() {
        if (wiederholen.isEmpty()) return
        val aktuelleAnsicht = karte.ansicht
        verwerfeVerbindungsInteraktion()
        rückgängig.fügeBegrenztHinzu(karte)
        val vorher = karte
        karte = wiederholen.removeLast().mitGruppenAnsichtenAus(vorher)
            .copy(ansicht = aktuelleAnsicht)
            .bereinigteVisuelleGruppen()
        if (!vorher.hatGleichenAuswertungsinhaltWie(karte)) auswertungsRevision += 1
        aktualisiereHistorienStatus()
        bereinigeAuswahl()
    }

    private fun merkeFürRückgängig(stand: KartenDaten) {
        rückgängig.fügeBegrenztHinzu(stand)
        wiederholen.clear()
        aktualisiereHistorienStatus()
    }

    private fun leereHistorie() {
        rückgängig.clear()
        wiederholen.clear()
        aktualisiereHistorienStatus()
    }

    private fun aktualisiereHistorienStatus() {
        rückgängigVerfügbar = rückgängig.isNotEmpty()
        wiederholenVerfügbar = wiederholen.isNotEmpty()
    }

    private fun ArrayDeque<KartenDaten>.fügeBegrenztHinzu(stand: KartenDaten) {
        addLast(stand)
        if (size > 100) removeFirst()
    }

    /**
     * Assoziative Knoten bieten beim Ziehen eines passenden Kabels einen vorläufigen Eingang an,
     * sobald mindestens zwei feste erweiterbare Eingänge vorhanden und vollständig verbunden sind.
     */
    private fun fügeDynamischeEingängeHinzu(start: AnschlussVerweis) {
        var erweitert = karte
        val zuIgnorierendeVerbindung = neuZuVerdrahtendeVerbindung?.id
        karte.knoten.forEach { knoten ->
            val vorlage = knoten.anschlüsse.firstOrNull {
                it.richtung == AnschlussRichtung.Eingang && it.kannSichErweitern
            } ?: return@forEach
            val festeErweiterbareEingänge = knoten.anschlüsse.asSequence()
                .filter {
                    it.richtung == AnschlussRichtung.Eingang &&
                        it.kannSichErweitern &&
                        !it.dynamischErzeugt
                }
                .sortedBy { it.reihenfolge }
                .toList()
            val verbundeneEingänge = karte.verbindungen.asSequence()
                .filterNot { it.id == zuIgnorierendeVerbindung }
                .map { it.zu }
                .toSet()
            if (festeErweiterbareEingänge.size < 2 || festeErweiterbareEingänge.any {
                    AnschlussVerweis(knoten.id, it.id) !in verbundeneEingänge
                }) return@forEach
            val neuerAnschluss = vorlage.copy(
                id = neueAnschlussId(),
                name = if (vorlage.name.matches(Regex("[A-Za-zÄÖÜäöü]+[.]?\\d+"))) {
                    val präfix = vorlage.name.dropLastWhile(Char::isDigit)
                    "$präfix${knoten.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang && it.name.startsWith(präfix) } + 1}"
                } else "input${knoten.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang } + 1}",
                reihenfolge = knoten.anschlüsse.filter { it.kante == vorlage.kante }.maxOfOrNull { it.reihenfolge }?.plus(1) ?: 0,
                dynamischErzeugt = true,
            )
            val probe = erweitert.copy(knoten = erweitert.knoten.map {
                if (it.id == knoten.id) it.copy(anschlüsse = it.anschlüsse + neuerAnschluss) else it
            })
            val prüfProbe = if (zuIgnorierendeVerbindung != null) {
                probe.copy(verbindungen = probe.verbindungen.filterNot { it.id == zuIgnorierendeVerbindung })
            } else probe
            if (prüfung.prüfe(prüfProbe, start, AnschlussVerweis(knoten.id, neuerAnschluss.id)) is VerbindungsPrüfung.Erlaubt) {
                erweitert = probe
            }
        }
        karte = erweitert
    }

    /** Bricht Auswahl oder Prüfung ab, ohne eine nur vorgemerkte alte Verbindung zu löschen. */
    private fun verwerfeVerbindungsInteraktion(meldungBeibehalten: Boolean = false) {
        val warAktiv = verbindungsStart != null || verbindungsVorschau != null || neuZuVerdrahtendeVerbindung != null
        verbindungsStart = null
        verbindungsVorschau = null
        neuZuVerdrahtendeVerbindung = null
        if (!meldungBeibehalten) letzteMeldung = null
        if (warAktiv) entferneUnverbundeneDynamischeEingänge()
    }

    private fun entferneUnverbundeneDynamischeEingänge() {
        karte = karte.ohneUnverbundeneDynamischeEingänge()
    }

    private fun bereinigeAuswahl() {
        if (geöffneteGruppe != null && karte.visuelleGruppen.none { it.id == geöffneteGruppe }) öffneGruppe(null)
        if (ausgewählteGruppe != null && karte.visuelleGruppen.none { it.id == ausgewählteGruppe }) ausgewählteGruppe = null
        if (ausgewählteGruppe != null) {
            ausgewählteKnoten = GruppenIndex(karte).knotenIn(ausgewählteGruppe!!)
            ausgewählterKnoten = null
            return
        }
        val gültigeKnoten = darstellungsKarte.knoten.filter { it.id !in projektion.blöcke && it.id !in projektion.portale }.mapTo(mutableSetOf()) { it.id }
        ausgewählteKnoten = ausgewählteKnoten.intersect(gültigeKnoten)
        ausgewählterKnoten = ausgewählterKnoten?.takeIf { it in gültigeKnoten }
            ?: ausgewählteKnoten.lastOrNull()
        ausgewählteVerbindung = ausgewählteVerbindung?.takeIf { id -> karte.verbindungen.any { it.id == id } }
    }

    private fun KartenDaten.ohneUnverbundeneDynamischeEingänge(): KartenDaten {
        val verbundeneEingänge = verbindungen.map { it.zu }.toSet()
        return copy(knoten = knoten.map { knoten ->
            knoten.copy(anschlüsse = knoten.anschlüsse.filterNot {
                it.richtung == AnschlussRichtung.Eingang && it.dynamischErzeugt &&
                    AnschlussVerweis(knoten.id, it.id) !in verbundeneEingänge
            })
        })
    }
}

private fun KartenAktion.istAuswertungsRelevant(): Boolean = when (this) {
    is KartenAktion.AnsichtÄndern,
    is KartenAktion.KnotenVerschieben,
    is KartenAktion.KnotenMehrfachVerschieben,
    is KartenAktion.KnotenGrößeÄndern,
    is KartenAktion.VisuelleGruppeFaltungÄndern,
    is KartenAktion.VisuelleGruppeAnsichtÄndern,
    is KartenAktion.VisuelleGruppeErstellen,
    is KartenAktion.VisuelleGruppeVerschieben,
    is KartenAktion.VisuelleGruppeGrößeÄndern,
    is KartenAktion.VisuelleGruppeTitelÄndern,
    is KartenAktion.VisuelleGruppenKinderZuordnen,
    is KartenAktion.VisuelleGruppeLöschen,
    is KartenAktion.VisuelleGruppierungAufheben -> false
    else -> true
}

private fun KartenDaten.hatGleichenAuswertungsinhaltWie(anderer: KartenDaten): Boolean =
    knoten.map { it.copy(position = GraphPunkt.Zero, größe = GraphGröße()) } ==
        anderer.knoten.map { it.copy(position = GraphPunkt.Zero, größe = GraphGröße()) } &&
        verbindungen == anderer.verbindungen

@Composable
fun merkeKartenEditorZustand(startKarte: KartenDaten, prüfung: GraphPrüfung): KartenEditorZustand =
    remember(startKarte.id, prüfung) { KartenEditorZustand(startKarte, prüfung) }

private fun KartenAktion.ändertGruppenGeometrie(): Boolean = when (this) {
    is KartenAktion.KnotenEinfügen, is KartenAktion.KnotenMehrfachEinfügen,
    is KartenAktion.KnotenVerschieben, is KartenAktion.KnotenMehrfachVerschieben,
    is KartenAktion.KnotenGrößeÄndern, is KartenAktion.VisuelleGruppeErstellen,
    is KartenAktion.VisuelleGruppeVerschieben, is KartenAktion.VisuelleGruppeGrößeÄndern,
    is KartenAktion.VisuelleGruppenKinderZuordnen -> true
    else -> false
}

private fun List<VisuelleKnotenGruppeDaten>.hatGleicheGruppenProjektionWie(anderer: List<VisuelleKnotenGruppeDaten>): Boolean =
    this === anderer || size == anderer.size && indices.all { i -> this[i] == anderer[i] || this[i].copy(ansicht = anderer[i].ansicht, ansichtInitialisiert = anderer[i].ansichtInitialisiert) == anderer[i] }

private fun KartenDaten.mitGruppenAnsichtenAus(aktuell: KartenDaten): KartenDaten {
    val ansichten = aktuell.visuelleGruppen.associateBy { it.id }
    return copy(visuelleGruppen = visuelleGruppen.map { g ->
        ansichten[g.id]?.let { g.copy(ansicht = it.ansicht, ansichtInitialisiert = it.ansichtInitialisiert) } ?: g
    })
}
