package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import kotlin.test.*

class InspektorAnschlussDatenTest {
    private val port = AnschlussDaten(name = "operanden", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = MathematikAnschlussArten.Zahl.id, belegung = AnschlussBelegung.Mehrfach)
    private val ziel = KnotenDaten(art = "test", name = "Ziel", anschlüsse = listOf(port))
    private val aus = AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Zahl.id)
    private val quelle = KnotenDaten(art = "test", name = "Quelle", anschlüsse = listOf(aus))
    private val wert = BedingterWert(RationaleZahl.von(7))

    @Test fun mehrfachBeitraegeBleibenVollstaendigGeordnetUndNachVerbindungAdressiert() {
        val andere = quelle.copy(id = neueKnotenId(), name = "Andere Quelle")
        val v1 = VerbindungDaten(von = AnschlussVerweis(quelle.id, aus.id), zu = AnschlussVerweis(ziel.id, port.id), eingangsReihenfolge = 3)
        val v2 = VerbindungDaten(von = AnschlussVerweis(andere.id, aus.id), zu = v1.zu, eingangsReihenfolge = 0)
        val knoten = ziel.copy(parameter = mapOf("join.lokal.a" to "2", "join.position.a" to "1"))
        val karte = KartenDaten(name = "Mehrfach", knoten = listOf(quelle, andere, knoten), verbindungen = listOf(v1, v2))
        val ergebnisse = mapOf(quelle.id to KnotenAuswertungsErgebnis(mapOf("wert" to wert)), andere.id to KnotenAuswertungsErgebnis(mapOf("wert" to wert)),
            knoten.id to KnotenAuswertungsErgebnis(emptyMap(), eingänge = mapOf("operanden@lokal:a" to wert)))
        val beiträge = inspektorAnschlussBeiträge(karte, knoten, port, ergebnisse)
        assertEquals(listOf(v2.id.wert, "lokal:a", v1.id.wert), beiträge.map { it.schlüssel })
        assertTrue(beiträge.all { it.wert == wert })
        assertEquals(3, beiträge.size)
    }

    @Test fun fallbackBleibtAlsInaktivSichtbarWennEineQuelleIhnErsetzt() {
        val einzel = port.copy(belegung = AnschlussBelegung.Einzeln)
        val knoten = ziel.copy(anschlüsse = listOf(einzel), eingangsKartenVerweise = mapOf(einzel.name to KartenVerweis(KartenId("fallback"), 2)))
        val verbindung = VerbindungDaten(von = AnschlussVerweis(quelle.id, aus.id), zu = AnschlussVerweis(ziel.id, port.id))
        val beiträge = inspektorAnschlussBeiträge(KartenDaten(name = "Karte", knoten = listOf(quelle, knoten), verbindungen = listOf(verbindung)), knoten, einzel,
            mapOf(quelle.id to KnotenAuswertungsErgebnis(mapOf("wert" to wert))))
        assertEquals(2, beiträge.size)
        assertEquals(wert, beiträge.first().wert)
        assertNull(beiträge.last().wert)
        assertTrue(beiträge.last().hinweis!!.startsWith("Inaktiv"))
    }

    @Test fun fehlerhafteQuelleZeigtKeinenVeraltetenWert() {
        val v = VerbindungDaten(von = AnschlussVerweis(quelle.id, aus.id), zu = AnschlussVerweis(ziel.id, port.id))
        val karte = KartenDaten(name = "Karte", knoten = listOf(quelle, ziel), verbindungen = listOf(v))
        val beiträge = inspektorAnschlussBeiträge(karte, ziel, port, mapOf(quelle.id to KnotenAuswertungsErgebnis(mapOf("wert" to wert), fehler = "Fehler")))
        assertNull(beiträge.single().wert)
        assertNotNull(beiträge.single().hinweis)
    }

    @Test fun standardwerteWerdenFachlichValidiertUndLeereZahlEntferntNurDenStandardwert() {
        val einzel = port.copy(belegung = AnschlussBelegung.Einzeln)
        val knoten = ziel.copy(anschlüsse = listOf(einzel), parameter = mapOf("anderes" to "erhalten"))
        assertEquals("1/2", setzeInspektorStandardwert(knoten, einzel, " 1/2 ").parameter["standardwert.operanden"])
        for (text in listOf("1/0", "-", "kein Wert")) assertFailsWith<IllegalArgumentException> { setzeInspektorStandardwert(knoten, einzel, text) }
        assertEquals(knoten, setzeInspektorStandardwert(setzeInspektorStandardwert(knoten, einzel, "2"), einzel, ""))
        assertFalse(hatInspektorStandardwert(ziel, port))
        val aussage = einzel.copy(art = MathematikAnschlussArten.Aussage.id)
        val aussageKnoten = knoten.copy(anschlüsse = listOf(aussage))
        assertFailsWith<IllegalArgumentException> { setzeInspektorStandardwert(aussageKnoten, aussage, "yes") }
        assertEquals("false", setzeInspektorStandardwert(aussageKnoten, aussage, "false").parameter["standardwert.operanden"])
    }

    @Test fun gemeinsameAusgangsoptionenFolgenDemModusUndDiagnosenSindEindeutig() {
        val diagonale = MatrixdiagonaleKnotenVorlagen.Matrixdiagonale.erzeuge(GraphPunkt.Zero)
        assertTrue(hatGemeinsameAusgangsEinstellungen(konfiguriereMatrixdiagonale(diagonale, MATRIXDIAGONALE_AUSLESEN)))
        assertFalse(hatGemeinsameAusgangsEinstellungen(konfiguriereMatrixdiagonale(diagonale, MATRIXDIAGONALE_ERZEUGEN)))
        val meldungen = inspektorMeldungen(ziel, KnotenAuswertungsErgebnis(emptyMap(), fehler = "Fehler", warnungen = listOf("Fehler", "Hinweis")),
            listOf(KnotenMeldung("Fehler", KnotenMeldungsArt.Warnung)))
        assertEquals(listOf("Fehler", "Hinweis"), meldungen.map { it.text })
        assertEquals(KnotenMeldungsArt.Fehler, meldungen.first().art)
    }

    @Test fun relationUndStandardnameWechselnAtomarUndEigeneNamenBleibenErhalten() {
        val basis = MathematikKnotenVorlagen.Kleiner.erzeuge(GraphPunkt.Zero)
        val editor = de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand(
            KartenDaten(name = "Relation", knoten = listOf(basis)),
            de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung(de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister(MathematikAnschlussArten.alle)))
        val neu = konfiguriereOrdnungsrelation(basis, "größerGleich")
        editor.führeAus(de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion.KnotenErsetzen(neu))
        assertEquals("größerGleich", editor.karte.knoten.single().parameter["relation"])
        assertEquals("Größer oder gleich", editor.karte.knoten.single().name)
        editor.rückgängig(); assertEquals(basis, editor.karte.knoten.single())
        assertEquals("Mein Vergleich", konfiguriereOrdnungsrelation(basis.copy(name = "Mein Vergleich"), "größer").name)
        assertFailsWith<IllegalArgumentException> { konfiguriereOrdnungsrelation(basis, "falsch") }
    }

    @Test fun richtungswinkelVerwendetSeinenVorhandenenGanzzahlStandardwert() {
        val basis = RichtungWinkelVorlage.erzeuge(GraphPunkt.Zero)
        val port = basis.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }
        val neu = setzeInspektorStandardwert(basis, port, "3")
        assertEquals("3", neu.parameter[RICHTUNG_WINKEL_ANZAHL])
        assertFalse("standardwert.n" in neu.parameter)
        val beiträge = inspektorAnschlussBeiträge(KartenDaten(name = "Richtung", knoten = listOf(neu)), neu, port, emptyMap())
        assertEquals("Standardwert: 3", beiträge.single().titel)
        for (text in listOf("", "-1", "32", "1/2")) assertFailsWith<IllegalArgumentException> { setzeInspektorStandardwert(basis, port, text) }
    }
}
