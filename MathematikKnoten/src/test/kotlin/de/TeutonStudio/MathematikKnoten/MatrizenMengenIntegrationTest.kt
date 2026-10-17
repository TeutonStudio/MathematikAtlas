package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.katalog.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class MatrizenMengenIntegrationTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private fun z(n: Long) = RationaleZahl.von(n)
    private fun quelle(a: MengenKnotenAuswahl = MengenKnotenAuswahl.REELLE_MATRIZEN) = MengenKnotenVorlagen.vorlage(a).erzeuge(GraphPunkt.Zero)
    private fun operator() = konfiguriereMengenRechner(MengenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero), MengenRechnerOperator.MATRIZENMENGE)
    private fun eval(k: KnotenDaten, eingänge: Map<String, BedingterWert>) = register.findeNachMigration(k.art)
        .auswerten(KnotenAuswertungsKontext(k, eingänge, RechenKontext()))
    private fun dimensionen(n: BedingterWert = BedingterWert(z(2)), m: BedingterWert = BedingterWert(z(3))) = mapOf("n" to n, "m" to m)

    @Test fun `beide Quellen und Operator besitzen dieselbe Matrixsemantik`() {
        for (a in listOf(MengenKnotenAuswahl.REELLE_MATRIZEN, MengenKnotenAuswahl.KOMPLEXE_MATRIZEN)) {
            val k = quelle(a)
            assertEquals(listOf("n", "m", "menge"), k.anschlüsse.map { it.name })
            val wert = assertIs<Tensorraum>(eval(k, dimensionen()).ausgaben.getValue("menge").objekt)
            assertTrue(wert.matrixForm)
            assertEquals(listOf(z(2), z(3)), wert.dimensionen)
            assertEquals(a.menge(), wert.elementMenge)
            assertEquals(wert, eval(operator(), dimensionen() + ("grundmenge" to BedingterWert(a.menge()))).ausgaben.getValue("menge").objekt)
            assertEquals(MathematischeTypen.Matrix, assertIs<de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert>(wert.elementTypAusdruck()).konstruktor)
            assertEquals(wert, MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(wert)))
            val geladen = MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(KartenDaten(name = "Quelle", knoten = listOf(k))))
            val migriert = geladen.knoten.single { it.id == k.id }
            assertEquals(MENGEN_DIMENSION_TENSOR, mengenDimensionsart(migriert))
            assertEquals(k.anschlüsse.single { it.name == "menge" }.id, migriert.anschlüsse.single { it.name == "menge" }.id)
            assertEquals(k.anschlüsse.filter { it.name in setOf("n","m") }.map { it.id }.toSet(), geladen.knoten.single { it.art == "mathematik.tupel" }.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.map { it.id }.toSet())
        }
    }

    @Test fun `symbolische natuerliche Dimension behaelt Quelle Bindung und offene Bedingung`() {
        val n = Variable("n", bindungsId = "quelle-n")
        val q = VariablenQuelle(KnotenId("quellknoten"), "n", NatürlicheZahlen, bindungsId = "quelle-n")
        val positiv = Vergleich(n, VergleichsArt.Größer, z(0))
        val wert = eval(quelle(), dimensionen(BedingterWert(n, variablenQuellen = listOf(q)))).ausgaben.getValue("menge")
        val raum = assertIs<Tensorraum>(wert.objekt)
        assertEquals(n, raum.dimensionen[0])
        assertEquals(listOf(q), wert.variablenQuellen)
        assertContains(wert.annahmen, positiv)
        val ersetzt = assertIs<Tensorraum>(ersetze(raum, mapOf("quelle-n" to z(4))))
        assertEquals(listOf(z(4), z(3)), ersetzt.dimensionen)
        assertEquals(raum, MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(raum)))
        assertIs<FehlendeObermenge>(bereichFürTyp(raum.elementTypAusdruck()))
        val mitNull = q.copy(werteVorrat = NichtnegativeGanzeZahlenSemantik.menge)
        val bedingt = eval(quelle(), dimensionen(BedingterWert(n, variablenQuellen = listOf(mitNull)))).ausgaben.getValue("menge")
        assertContains(bedingt.annahmen, positiv)
        assertEquals(listOf(mitNull), bedingt.variablenQuellen)
    }

    @Test fun `Dimensionen verwerfen fehlende ungueltige und unbewiesene Quellen`() {
        val n = Variable("n", bindungsId = "n")
        val falsch = VariablenQuelle(KnotenId("falsch"), "n", ReelleZahlen, bindungsId = "n")
        val falscheBindung = falsch.copy(werteVorrat = NatürlicheZahlen, bindungsId = "andere")
        val inputs = listOf(
            emptyMap(), dimensionen(BedingterWert(z(0))), dimensionen(BedingterWert(z(-1))),
            dimensionen(BedingterWert(RationaleZahl.von(1, 2))), dimensionen(BedingterWert(n)),
            dimensionen(BedingterWert(n, variablenQuellen = listOf(falsch))),
            dimensionen(BedingterWert(n, variablenQuellen = listOf(falscheBindung))),
            dimensionen(BedingterWert(z(2), annahmen = setOf(WahrheitsKonstante(false)))),
        )
        for (eingänge in inputs) {
            val versuch = runCatching { eval(quelle(), eingänge) }
            assertTrue(versuch.isFailure || versuch.getOrThrow().fehler != null, eingänge.toString())
        }
    }

    @Test fun `historischer Matrizenraum migriert idempotent und behaelt alle Verbindungen`() {
        val alt = MengenraumKnotenVorlagen.Matrizenraum.erzeuge(GraphPunkt.Zero)
        val quelle = MengenKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val kante = VerbindungDaten(von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single { it.name == "menge" }.id),
            zu = AnschlussVerweis(alt.id, alt.anschlüsse.single { it.name == "grundmenge" }.id))
        val original = KartenDaten(name = "Alt", knoten = listOf(quelle, alt), verbindungen = listOf(kante))
        val neu = MathematikKartenCodec.lade(KartenDatenJson.schreibe(original))
        val k = neu.knoten.single { it.id == alt.id }
        assertEquals(MengenRechner.KNOTEN_ART, k.art, k.parameter.toString())
        assertFalse(RECHNER_MIGRATION_DIAGNOSE in k.parameter)
        assertEquals(alt.anschlüsse.map { it.id }.toSet(), k.anschlüsse.filter { it.name in setOf("grundmenge", "menge") }.map { it.id }.toSet())
        assertEquals(original.verbindungen, neu.verbindungen.filter { it.id == kante.id })
        val dimensionen = neu.knoten.single { it.art == "mathematik.tupel" }
        assertEquals("2", dimensionen.parameter["standardwert.n"])
        assertEquals("2", dimensionen.parameter["standardwert.m"])
        assertEquals(neu, MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(neu)))
        val wert = KartenAuswerter(register).auswerten(neu).knoten.getValue(k.id)
        assertNull(wert.fehler)
        assertEquals(listOf(z(2), z(2)), assertIs<Tensorraum>(wert.ausgaben.getValue("menge").objekt).dimensionen)
    }

    @Test fun `verbundener ungueltiger Eingang aktiviert keinen Standardwert`() {
        val k = quelle().let { it.copy(parameter = it.parameter + mapOf("standardwert.n" to "2", "standardwert.m" to "3")) }
        val zahl = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero).let { it.copy(parameter = mapOf("wert" to "0")) }
        val kante = VerbindungDaten(von = AnschlussVerweis(zahl.id, zahl.anschlüsse.single().id), zu = AnschlussVerweis(k.id, k.anschlüsse.single { it.name == "n" }.id))
        val ergebnis = KartenAuswerter(register).auswerten(KartenDaten(name = "Kein Ersatz", knoten = listOf(zahl, k), verbindungen = listOf(kante))).knoten.getValue(k.id)
        assertNotNull(ergebnis.fehler)
        assertTrue(ergebnis.ausgaben.isEmpty())
    }

    @Test fun `Variantenwechsel erhaelt Ports und bildet einen Undo Schritt`() {
        val start = quelle()
        val neu = konfiguriereMengenAuswahl(start, MengenKnotenAuswahl.KOMPLEXE_MATRIZEN)
        assertEquals(start.anschlüsse, neu.anschlüsse)
        val editor = KartenEditorZustand(KartenDaten(name = "Undo", knoten = listOf(start)), GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle)))
        editor.führeAus(KartenAktion.KnotenErsetzen(neu))
        editor.rückgängig()
        assertEquals(start, editor.karte.knoten.single())
        editor.wiederholen()
        assertEquals(neu, editor.karte.knoten.single())
        val kopie = editor.karte.alsNeueKarte()
        assertEquals(neu.parameter, kopie.knoten.single().parameter)
        assertNotEquals(neu.id, kopie.knoten.single().id)
    }

    @Test fun `Katalog und Suche bieten nur die integrierten Matrizenvarianten`() {
        val katalog = KanonischerMathematikKnotenKatalog.alle()
        assertFalse(katalog.any { it.art == "mathematik.matrizenraum" })
        assertEquals(0, katalog.count { MengenKnotenAuswahl.vonIdOderNull(it.standardParameter[MENGEN_KNOTEN_AUSWAHL])?.istMatrizenmenge == true })
        val treffer = OperatorKnotenSuchindex.suche("Matrizenraum", katalog)
        assertTrue(treffer.any { it.art == MengenRechner.KNOTEN_ART })
        assertTrue(treffer.any { it.standardParameter[MENGENRECHNER_OPERATOR_PARAMETER] == MengenRechnerOperator.TENSORRAUM.stabileId })
        assertFalse(treffer.any { it.art == "mathematik.matrizenraum" })
    }
}
