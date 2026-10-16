package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class MethodenQuellKnotenTest {
    private val auswerter = KartenAuswerter(StandardMathematikAuswerter.erzeugeRegister())
    @Test fun `Standardquelle ist Identitaet und Ausgang bleibt bei Quellenwechsel stabil`() {
        val knoten = MethodenQuellKnotenVorlagen.Methode.erzeuge(GraphPunkt(0f, 0f))
        val ergebnis = auswerter.auswerten(KartenDaten(name = "Test", knoten = listOf(knoten)))
        assertSame(IdentitaetsMethode, ergebnis.knoten.getValue(knoten.id).ausgaben.getValue("methode").objekt)
        val neu = konfiguriereMethodenQuelle(knoten, MathematikKnotenVorlagen.Potenz.erzeuge(GraphPunkt(0f, 0f)))
        assertEquals(knoten.anschlüsse.last().id, neu.anschlüsse.single().id)
        assertEquals(knoten.anschlüsse.last().id, konfiguriereMethodenQuelle(neu).anschlüsse.last().id)
        val geladen = KartenDatenJson.lese(KartenDatenJson.schreibe(KartenDaten(name = "Test", knoten = listOf(neu)))).knoten.single()
        assertEquals(neu, geladen)
        assertEquals("mathematik.potenz", methodenQuellKnoten(geladen)?.art)
    }
    @Test fun `einzelner Knoten exportiert Originalportidentitaeten`() {
        val quelle = MathematikKnotenVorlagen.Potenz.erzeuge(GraphPunkt(0f, 0f))
        val besitzer = MethodenQuellKnotenVorlagen.Methode.erzeuge(GraphPunkt(0f, 0f))
        val export = auswerter.exportiereKarteAlsMethode(methodenQuellKarte(quelle, besitzer.id))
        assertNull(export.fehler)
        val methode = assertIs<MathematischeMethode>(export.ausgaben.getValue("methode").objekt)
        assertEquals(quelle.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.map { it.id.wert }, methode.signatur.argumente.map { it.id })
        assertEquals(quelle.anschlüsse.filter { it.richtung == AnschlussRichtung.Ausgang }.map { it.id.wert }, methode.signatur.ergebnisse.map { it.id })
        assertEquals(Tupel(listOf(RationaleZahl.von(8))), methode.wendeMitBedingungenAn(Tupel(listOf(RationaleZahl.von(2), RationaleZahl.von(3)))).ergebnis)
        val source = konfiguriereMethodenQuelle(besitzer, quelle)
        val ergebnis = auswerter.auswerten(KartenDaten(name = "Test", knoten = listOf(source)))
        assertNull(ergebnis.knoten.getValue(source.id).fehler)
    }
    @Test fun `Kopieren und Undo erhalten die Quellkonfiguration`() {
        val quelle = konfiguriereMethodenQuelle(MethodenQuellKnotenVorlagen.Methode.erzeuge(GraphPunkt(0f, 0f)), MathematikKnotenVorlagen.Potenz.erzeuge(GraphPunkt(0f, 0f)), quelleId = "knoten:mathematik.potenz:standard")
        val karte = KartenDaten(name = "Karte", knoten = listOf(quelle))
        val kopie = karte.alsNeueKarte("Kopie")
        assertNotEquals(quelle.id, kopie.knoten.single().id)
        assertEquals(quelle.eigenschaften, kopie.knoten.single().eigenschaften)
        val arten = de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister(MathematikAnschlussArten.alle)
        val editor = de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand(karte, de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung(arten))
        editor.führeAus(de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion.KnotenErsetzen(konfiguriereMethodenQuelle(quelle)))
        assertNull(methodenQuellKnoten(editor.karte.knoten.single()))
        editor.rückgängig()
        assertEquals(quelle, editor.karte.knoten.single())
        editor.wiederholen()
        assertNull(methodenQuellKnoten(editor.karte.knoten.single()))
    }
    @Test fun `dynamische Eingangsanzahl wird im Export erhalten`() {
        val quelle = konfiguriereMethodenQuellParameter(MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt(0f, 0f)), "festeEingänge", "4")
        val export = auswerter.exportiereKarteAlsMethode(methodenQuellKarte(quelle, KnotenId("vier")))
        assertNull(export.fehler)
        val methode = assertIs<MathematischeMethode>(export.ausgaben.getValue("methode").objekt)
        assertEquals(4, methode.signatur.argumente.size)
        assertEquals(Tupel(listOf(RationaleZahl.von(4))), methode.wendeMitBedingungenAn(Tupel(List(4) { RationaleZahl.Eins })).ergebnis)
    }
    @Test fun `mehrere Ausgaenge und ein Tupelausgang bleiben verschieden`() {
        val register = StandardMathematikAuswerter.erzeugeRegister()
        val ports = listOf("links", "rechts").map { AnschlussDaten(name = it, richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = MathematikAnschlussArten.Zahl.id) }
        register.registriere("test.paar") { KnotenAuswertungsErgebnis(mapOf("links" to BedingterWert(RationaleZahl.Eins), "rechts" to BedingterWert(RationaleZahl.von(2)))) }
        val source = KnotenDaten(art = "test.paar", name = "Paar", anschlüsse = ports)
        val export = KartenAuswerter(register).exportiereKarteAlsMethode(methodenQuellKarte(source, KnotenId("paar")))
        assertNull(export.fehler)
        assertEquals(2, (export.ausgaben.getValue("methode").objekt as MathematischeMethode).signatur.ergebnisse.size)
    }
    @Test fun `unbekannte und widerspruechliche Quellenwahl liefern Diagnose`() {
        val standard = MethodenQuellKnotenVorlagen.Methode.erzeuge(GraphPunkt(0f, 0f))
        val ungueltig = standard.copy(eigenschaften = mapOf(METHODEN_QUELLE_ID to KnotenEigenschaft.Text("unbekannt")))
        val ergebnis = auswerter.auswerten(KartenDaten(name = "Test", knoten = listOf(ungueltig))).knoten.getValue(ungueltig.id)
        assertNotNull(ergebnis.fehler)
        assertTrue(ergebnis.ausgaben.isEmpty())
        val falscherTyp = standard.copy(eigenschaften = mapOf(METHODEN_QUELLE_ID to KnotenEigenschaft.Ganzzahl(3)))
        assertNotNull(auswerter.auswerten(KartenDaten(name = "Test", knoten = listOf(falscherTyp))).knoten.getValue(falscherTyp.id).fehler)
    }
    @Test fun `kanonische Potenz ist als Methode verfuegbar`() {
        val potenzen = methodenKnotenQuellen().filter { it.titel == "Potenz" }
        assertTrue(potenzen.any { it.fehler == null }, potenzen.joinToString { "${it.id}: ${it.fehler} / ${it.knoten.anschlüsse}" })
    }
}
