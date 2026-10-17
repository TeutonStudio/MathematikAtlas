package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class JoinEingaengeTest {
    private fun quelle(wert: Int) = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero).copy(parameter = mapOf("wert" to wert.toString()))
    private fun ref(k: KnotenDaten, name: String = "wert") = AnschlussVerweis(k.id, k.anschlüsse.first { it.name == name }.id)
    private fun karte(anzahl: Int, vorlage: KnotenVorlage = MathematikKnotenVorlagen.Addition): KartenDaten {
        val ziel = vorlage.erzeuge(GraphPunkt.Zero)
        val quellen = (1..anzahl).map(::quelle)
        return KartenDaten(name = "Join", knoten = quellen + ziel, verbindungen = quellen.mapIndexed { i, q ->
            VerbindungDaten(von = ref(q), zu = ref(ziel, if (i == 0) "a" else "b"))
        }).normalisiereJoinEingänge(überführeEinzelAnschlüsse=true)
    }
    @Test fun `Migration ist idempotent und behaelt alle Verbindungsidentitaeten`() {
        val k = karte(3)
        assertEquals(3, k.verbindungen.size)
        assertEquals(1, k.verbindungen.map { it.zu }.distinct().size)
        assertEquals(k, k.normalisiereJoinEingänge(überführeEinzelAnschlüsse=true))
        assertEquals(k, KartenDatenJson.lese(KartenDatenJson.schreibe(k)))
        assertEquals(10, KartenDatenJson.FORMAT_VERSION)
    }
    @Test fun `Addition sammelt alle Quellen und eine Quelle wird durchgegeben`() {
        for (n in 1..5) {
            val k = karte(n)
            val e = MathematikKartenLaufzeit().auswertenNachMigration(k).knoten.getValue(k.knoten.last().id)
            assertNull(e.fehler)
            assertEquals(RationaleZahl.von(n * (n + 1) / 2), e.ausgaben.getValue("wert").objekt)
        }
    }
    @Test fun `Ohne Operanden entsteht kein impliziter Wert`() {
        val k = karte(0)
        assertNotNull(MathematikKartenLaufzeit().auswertenNachMigration(k).knoten.getValue(k.knoten.last().id).fehler)
    }
    @Test fun `Wiederholte Quelle zaehlt mehrfach`() {
        val k = karte(1)
        val neu = k.wendeAn(KartenAktion.VerbindungEinfügen(k.verbindungen.single().copy(id = neueVerbindungsId())))
        assertEquals(2, neu.verbindungen.size)
        assertEquals(RationaleZahl.von(2), MathematikKartenLaufzeit().auswertenNachMigration(neu).knoten.getValue(k.knoten.last().id).ausgaben.getValue("wert").objekt)
    }
    @Test fun `Lokale Werte werden erhalten leere Plaetze entfallen`() {
        val ziel = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero)
            .copy(parameter = mapOf("standardwert.a" to "4", "standardwert.b" to "7"))
        val k = KartenDaten(name = "Lokal", knoten = listOf(ziel)).normalisiereJoinEingänge(überführeEinzelAnschlüsse=true)
        assertEquals(2, k.knoten.single().parameter.count { it.key.startsWith("join.lokal.") })
        assertEquals(RationaleZahl.von(11), MathematikKartenLaufzeit().auswertenNachMigration(k).knoten.getValue(ziel.id).ausgaben.getValue("wert").objekt)
    }
    @Test fun `Andockpunkte bleiben eindeutig und freier Bereich ergaenzt statt zu ersetzen`() {
        val k = karte(2)
        val runtime = MathematikKartenLaufzeit()
        val editor = KartenEditorZustand(k, runtime.graphPrüfung)
        val ziel = k.knoten.last()
        val dargestellt = editor.darstellungsKarte.knoten.last()
        assertEquals(2, dargestellt.anschlüsse.count { it.beitragsVerbindung != null })
        editor.beginneVerbindung(ref(k.knoten.first()))
        editor.anschlussAngeklickt(AnschlussVerweis(ziel.id, ziel.anschlüsse.first().id))
        assertEquals(3, editor.karte.verbindungen.size)
        editor.rückgängig()
        assertEquals(2, editor.karte.verbindungen.size)
        editor.wiederholen()
        assertEquals(3, editor.karte.verbindungen.size)
    }
    @Test fun `Einzelnes Leitungsende wird gezielt geloest und Undo stellt es wieder her`() {
        val k = karte(3)
        val editor = KartenEditorZustand(k, MathematikKartenLaufzeit().graphPrüfung)
        val port = editor.darstellungsKarte.knoten.last().anschlüsse.first { it.beitragsVerbindung != null }
        val id = port.beitragsVerbindung
        editor.beginneVerbindung(AnschlussVerweis(k.knoten.last().id, port.id))
        editor.beendeVerbindungsVorschau()
        assertEquals(2, editor.karte.verbindungen.size)
        assertFalse(editor.karte.verbindungen.any { it.id == id })
        editor.rückgängig()
        assertEquals(k.verbindungen, editor.karte.verbindungen)
    }
    @Test fun `Matrixprodukt und Tensorprodukt werden kein Join`() {
        assertFalse(istJoinRechnerOperator("matrix.produkt"))
        assertFalse(istJoinRechnerOperator("tensor.tensorprodukt"))
        assertFalse(istJoinRechnerOperator("zahl.subtraktion"))
        assertTrue(istJoinRechnerOperator("menge.schnitt"))
    }
    @Test fun `Multiplikation wechselt Darstellung ohne persistierte Beitraege zu aendern`() {
        val k = karte(2, MathematikKnotenVorlagen.Multiplikation)
        val ziel = k.knoten.last()
        assertTrue(zeigeJoinKapsel(k, ziel))
        val q = k.knoten.first().copy(art = "test.quaternion")
        val h = BedingterWert(Variable("h"), werteVorrat = BenannteMenge("H", "\\mathbb H"))
        val ergebnisse = mapOf(q.id to KnotenAuswertungsErgebnis(mapOf("wert" to h)))
        assertFalse(zeigeJoinKapsel(k.copy(knoten = listOf(q) + k.knoten.drop(1)), ziel, ergebnisse))
        assertTrue(zeigeJoinKapsel(k, ziel))
    }
    @Test fun `Join verhindert Zyklen und behaelt bestehende Quellen bei Ablehnung`() {
        val k = karte(2)
        val ziel = k.knoten.last()
        assertIs<VerbindungsPrüfung.Abgelehnt>(MathematikKartenLaufzeit().graphPrüfung.prüfe(k, ref(ziel), ref(ziel, "a")))
        assertEquals(2, k.verbindungen.size)
    }
    private fun einzeln(ziel: KnotenDaten, wert: BedingterWert): KnotenAuswertungsErgebnis {
        val k = KartenDaten(name = "Einzel", knoten = listOf(ziel)).normalisiereJoinEingänge(überführeEinzelAnschlüsse=true).knoten.single()
        val join = k.anschlüsse.single { it.belegung == AnschlussBelegung.Mehrfach }
        val q = KnotenDaten(art = "test.quelle", name = "Quelle", anschlüsse = listOf(join.copy(id = neueAnschlussId(), name = "wert", richtung = AnschlussRichtung.Ausgang, belegung = AnschlussBelegung.Einzeln)))
        val karte = KartenDaten(name = "Einzel", knoten = listOf(q, k), verbindungen = listOf(VerbindungDaten(von = ref(q), zu = AnschlussVerweis(k.id, join.id))))
        return KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister()).auswertenNachMigration(karte, mapOf(q.id to mapOf("wert" to wert))).knoten.getValue(k.id)
    }
    @Test fun `Alle Zahlenoperatoren erhalten den validierten Einzelwert samt Herkunft`() {
        val wert = BedingterWert(RationaleZahl.von(7), annahmen = setOf(Gleichheit(Variable("x"), RationaleZahl.Eins)))
        val vorlagen = ZahlenRechnerKnotenVorlagen.alle.filter { it.standardParameter[ZAHLENRECHNER_OPERATOR] in setOf("zahl.addition", "zahl.multiplikation", "zahl.minimum", "zahl.maximum") } + listOf(MathematikKnotenVorlagen.Maximum, MathematikKnotenVorlagen.Minimum, MathematikKnotenVorlagen.Multiplikation)
        assertEquals(7, vorlagen.size)
        vorlagen.forEach { v ->
            val e = einzeln(v.erzeuge(GraphPunkt.Zero), wert)
            assertNull(e.fehler, v.name + ": " + e.fehler)
            assertEquals(wert, e.ausgaben.values.single())
        }
    }
    @Test fun `Strukturfamilien validieren Singleton und erhalten den Wert`() {
        val vektor = SpaltenVektor(listOf(RationaleZahl.Eins, RationaleZahl.von(2)))
        val matrix = Matrix(listOf(listOf(RationaleZahl.Eins)))
        val tensor = Tensor(listOf(1, 1, 1), listOf(RationaleZahl.Eins))
        val fälle = listOf(
            Triple(StrukturRechnerKnotenFamilie.VEKTOR, "vektor.addition", vektor),
            Triple(StrukturRechnerKnotenFamilie.VEKTOR, "vektor.hadamard", vektor),
            Triple(StrukturRechnerKnotenFamilie.MATRIX, "matrix.addition", matrix),
            Triple(StrukturRechnerKnotenFamilie.MATRIX, "matrix.hadamard", matrix),
            Triple(StrukturRechnerKnotenFamilie.TENSOR, "tensor.addition", tensor),
            Triple(StrukturRechnerKnotenFamilie.TENSOR, "tensor.hadamard", tensor),
        )
        fälle.forEach { (familie, id, objekt) ->
            val basis = StrukturFormelRechnerVorlagen.alle.first { it.art == familie.knotenArt }.erzeuge(GraphPunkt.Zero)
            val ziel = konfiguriereStrukturRechner(basis, familie, id)
            val wert = BedingterWert(objekt)
            val e = einzeln(ziel, wert)
            assertNull(e.fehler, "$id: ${e.fehler}")
            assertEquals(wert, e.ausgaben["wert"] ?: e.ausgaben["ergebnis"], "$id: ${e.ausgaben.keys}")
        }
    }
    @Test fun `Lokale Aussagen funktionieren am methodenfaehigen Sammelanschluss`() {
        val basis = StrukturFormelRechnerVorlagen.Aussagesatz.erzeuge(GraphPunkt.Zero)
        val ziel = konfiguriereStrukturRechner(basis, StrukturRechnerKnotenFamilie.AUSSAGESATZ, "aussage.konjunktion")
            .copy(parameter = mapOf("operator" to "aussage.konjunktion", "standardwert.a" to "true", "standardwert.b" to "false"))
        val k = KartenDaten(name = "Bool", knoten = listOf(ziel)).normalisiereJoinEingänge(überführeEinzelAnschlüsse=true)
        val e = MathematikKartenLaufzeit().auswertenNachMigration(k).knoten.getValue(ziel.id)
        assertNull(e.fehler)
        assertEquals(Wahrheitswert.Lüge, (e.ausgaben.values.single().objekt as Aussage).entscheide(RechenKontext()).wahrheitswert)
    }
    @Test fun `Cache beruecksichtigt jede Quelle und auch deren Entfernung`() {
        val k = karte(3)
        val runtime = MathematikKartenLaufzeit()
        val ziel = k.knoten.last().id
        assertEquals(RationaleZahl.von(6), runtime.auswertenNachMigration(k).knoten.getValue(ziel).ausgaben.getValue("wert").objekt)
        val geändert = k.copy(knoten = k.knoten.mapIndexed { i, n -> if (i == 2) n.copy(parameter = mapOf("wert" to "10")) else n })
        assertEquals(RationaleZahl.von(13), runtime.auswertenNachMigration(geändert).knoten.getValue(ziel).ausgaben.getValue("wert").objekt)
        assertEquals(RationaleZahl.von(3), runtime.auswertenNachMigration(geändert.copy(verbindungen = geändert.verbindungen.dropLast(1))).knoten.getValue(ziel).ausgaben.getValue("wert").objekt)
    }
    @Test fun `Migration bewahrt Mischreihenfolge lokaler und verbundener Operanden`() {
        val ziel = MathematikKnotenVorlagen.Multiplikation.erzeuge(GraphPunkt.Zero).copy(parameter = mapOf("standardwert.a" to "2"))
        val q = quelle(3)
        val k = KartenDaten(name = "Gemischt", knoten = listOf(q, ziel), verbindungen = listOf(VerbindungDaten(von = ref(q), zu = ref(ziel, "b")))).normalisiereJoinEingänge(überführeEinzelAnschlüsse=true)
        val e = MathematikKartenLaufzeit().auswertenNachMigration(k).knoten.getValue(ziel.id)
        assertEquals(listOf(RationaleZahl.von(2), RationaleZahl.von(3)), e.eingänge.values.map { it.objekt })
        assertEquals(k, k.normalisiereJoinEingänge(überführeEinzelAnschlüsse=true))
    }

    @Test fun `Operatorwechsel zu Einzelanschluessen verlangt bestaetigten Verbindungskonflikt`() {
        val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val a = quelle(2); val b = quelle(3)
        val alt = KartenDaten(name = "Wechsel", knoten = listOf(a, b, basis), verbindungen = listOf(
            VerbindungDaten(von = ref(a), zu = ref(basis, "a")), VerbindungDaten(von = ref(b), zu = ref(basis, "b")))).normalisiereJoinEingänge(überführeEinzelAnschlüsse=true)
        val ziel = alt.knoten.last()
        val neu = konfiguriereZahlenRechner(ziel, UniversellerZahlenOperator.SUBTRAKTION)
        assertTrue(neu.anschlüsse.all { it.belegung == AnschlussBelegung.Einzeln })
        val vorschau = alt.vorschauKnotenErsetzen(neu, MathematikKartenLaufzeit().graphPrüfung)
        assertEquals(1, vorschau.entfallendeVerbindungen.size)
        assertFailsWith<IllegalArgumentException> { alt.wendeAn(KartenAktion.KnotenErsetzen(neu)) }
        val geändert = alt.wendeAn(KartenAktion.KnotenErsetzen(neu, vorschau.entfallendeVerbindungen.map { it.id }.toSet()))
        assertEquals(1, geändert.verbindungen.size)
    }
    @Test fun `Kopien bewahren Reihenfolge und unterdrueckte Kartenfallbacks`() {
        val q = quelle(3)
        val ziel = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("standardwert.b" to "2"),
            eingangsKartenVerweise = mapOf("a" to KartenVerweis(KartenId("fallback"), 1)))
        val k = KartenDaten(name = "Fallback", knoten = listOf(q, ziel), verbindungen = listOf(VerbindungDaten(von = ref(q), zu = ref(ziel, "a")))).normalisiereJoinEingänge(überführeEinzelAnschlüsse=true)
        val clipboard = AtlasZwischenablage()
        assertTrue(clipboard.kopiere(k, k.knoten.map { it.id }.toSet()))
        val ausschnitt = assertNotNull(clipboard.erzeugeEinfügung(GraphPunkt.Zero))
        val kopien = listOf(k, k.alsNeueKarte(), k.copy(knoten = ausschnitt.knoten, verbindungen = ausschnitt.verbindungen))
        kopien.forEach { kopie ->
            var aufrufe = 0
            val auswerter = KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister(), KartenQuelle { aufrufe++; null })
            val e = auswerter.auswertenNachMigration(kopie).knoten.getValue(kopie.knoten.last().id)
            assertNull(e.fehler)
            assertEquals(RationaleZahl.von(5), e.ausgaben.getValue("wert").objekt)
            assertEquals(0, aufrufe)
            assertEquals(k.verbindungen.map { it.eingangsReihenfolge }, kopie.verbindungen.map { it.eingangsReihenfolge })
        }
    }
    @Test fun `Eingebettete binaere Methodenquelle behaelt ihre oeffentliche Signatur`() {
        val quelle = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero)
        val methode = konfiguriereMethodenQuelle(MethodenQuellKnotenVorlagen.Methode.erzeuge(GraphPunkt.Zero), quelle)
        val k = KartenDaten(name = "Methode", knoten = listOf(methode))
        val e = MathematikKartenLaufzeit().auswertenNachMigration(k).knoten.getValue(methode.id)
        assertNull(e.fehler)
        val m = e.ausgaben.getValue("methode").objekt as MathematischeMethode
        assertEquals(RationaleZahl.von(5), vereinfache(m.wendeAn(listOf(RationaleZahl.von(2), RationaleZahl.von(3))) as ZahlAusdruck))
    }

    @Test fun `Geordneter Operatorwechsel uebernimmt lokale Werte und meldet Ueberhang`() {
        val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero).copy(parameter = mapOf(
            "operator" to "zahl.addition", "standardwert.a" to "2", "standardwert.b" to "3"))
        val k = KartenDaten(name = "Lokalwechsel", knoten = listOf(basis)).normalisiereJoinEingänge(überführeEinzelAnschlüsse=true)
        val runtime = MathematikKartenLaufzeit()
        val kandidat = konfiguriereZahlenRechner(k.knoten.single(), UniversellerZahlenOperator.SUBTRAKTION)
        val plan = planeKonfiguration(k, kandidat, runtime.graphPrüfung, runtime::auswerten)
        assertFalse(plan.auswirkung.trenntVerbindungen)
        assertEquals("2", plan.karte.knoten.single().parameter["standardwert.a"])
        assertEquals("3", plan.karte.knoten.single().parameter["standardwert.b"])
        assertEquals(RationaleZahl.von(-1), runtime.auswertenNachMigration(plan.karte).knoten.getValue(basis.id).ausgaben.getValue("wert").objekt)
        val unär = planeKonfiguration(k, konfiguriereZahlenRechner(k.knoten.single(), UniversellerZahlenOperator.BETRAG), runtime.graphPrüfung, runtime::auswerten)
        assertTrue(unär.auswirkung.trenntVerbindungen)
        assertEquals(listOf("3"), unär.auswirkung.entfallendeLokaleWerte)
    }
    @Test fun `Join zu Join behaelt alle Quellen waehrend Join zu binaer geordnet entfaltet`() {
        val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val quellen = (1..3).map(::quelle)
        val ziel = KartenDaten(name = "Vorlage", knoten = listOf(basis)).normalisiereJoinEingänge(überführeEinzelAnschlüsse=true).knoten.single()
        val k = KartenDaten(name = "Wechsel", knoten = quellen + ziel, verbindungen = quellen.mapIndexed { index, q ->
            VerbindungDaten(von = ref(q), zu = ref(ziel, "a"), eingangsReihenfolge = index)
        })
        val runtime = MathematikKartenLaufzeit()
        val join = planeKonfiguration(k, konfiguriereZahlenRechner(ziel, UniversellerZahlenOperator.MULTIPLIKATION), runtime.graphPrüfung, runtime::auswerten)
        assertFalse(join.auswirkung.trenntVerbindungen)
        assertEquals(3, join.karte.verbindungen.size)
        val binär = planeKonfiguration(k, konfiguriereZahlenRechner(ziel, UniversellerZahlenOperator.SUBTRAKTION), runtime.graphPrüfung, runtime::auswerten)
        assertEquals(listOf(k.verbindungen.last()), binär.auswirkung.entfallendeVerbindungen)
        assertEquals(listOf("a", "b"), binär.karte.verbindungen.map { edge -> binär.karte.knoten.last().anschlüsse.single { it.id == edge.zu.anschlussId }.name })
    }

    @Test fun `Joinformel zeigt alle Beitraege und keinen freien Reserveoperanden`() {
        val k = karte(3)
        val ziel = k.knoten.last()
        val ergebnis = MathematikKartenLaufzeit().auswertenNachMigration(k).knoten.getValue(ziel.id)
        assertEquals("1 + 2 + 3", joinOperatorFormel(ziel, ergebnis, " + "))
        val projiziert = k.gruppenProjektion().mitMehrfachAndockpunkten().karte.knoten.last()
        assertEquals("1 + 2 + 3", joinOperatorFormel(projiziert, ergebnis, " + "))
    }

}
