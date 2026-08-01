package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.Negation
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.WahrheitsKonstante
import de.TeutonStudio.MathematikRechenSystem.kern.Wahrheitswert
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AussagenLogikKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Negation besitzt genau einen Aussageeingang und Aussageausgang`() {
        val knoten = AussagenLogikKnotenVorlagen.Negation.erzeuge(GraphPunkt.Zero)
        val eingang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }
        val ausgang = knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }

        assertEquals("mathematik.negation", knoten.art)
        assertEquals("aussage", eingang.name)
        assertEquals(MathematikAnschlussArten.Aussage.id, eingang.art)
        assertEquals(MathematikAnschlussArten.Aussage.id, ausgang.art)
        assertNotNull(register.finde(knoten.art))
        assertEquals(1, alleMathematikKnotenVorlagen().count { it.art == knoten.art })
    }

    @Test
    fun `Negationsknoten kehrt Wahr zu Lüge um`() {
        val knoten = AussagenLogikKnotenVorlagen.Negation.erzeuge(GraphPunkt.Zero)
        val ausgabe = requireNotNull(register.finde(knoten.art)).auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("aussage" to BedingterWert(WahrheitsKonstante(true))),
                rechenKontext = RechenKontext(),
            ),
        ).ausgaben.getValue("aussage").objekt

        assertIs<Negation>(ausgabe)
        assertEquals(Wahrheitswert.Lüge, ausgabe.entscheide().wahrheitswert)
        assertEquals("\\neg\\left(\\mathcal{Wahr}\\right)", ausgabe.zuLatex())
    }

    @Test
    fun `Adjunktionsvorlage ersetzt die historische feste UND Variante`() {
        val vorlage = alleMathematikKnotenVorlagen().single {
            it.art == "mathematik.adjunktion" && it.name == "Adjunktion"
        }
        val knoten = vorlage.erzeuge(GraphPunkt.Zero)
        val eingänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }

        assertEquals(2, eingänge.size)
        assertTrue(eingänge.all { it.kannSichErweitern })
        assertEquals(AUSSAGEN_LOGIK_XOR, knoten.parameter[AUSSAGEN_LOGIK_SEMANTIK])
    }

    @Test
    fun `dreistellige Adjunktion verwendet ungerade Parität`() {
        val vorlage = alleMathematikKnotenVorlagen().single {
            it.art == "mathematik.adjunktion" && it.name == "Adjunktion"
        }
        val basis = vorlage.erzeuge(GraphPunkt.Zero)
        val dritter = basis.anschlüsse.first { it.richtung == AnschlussRichtung.Eingang }.copy(
            id = AnschlussId("test-c"),
            name = "c",
            reihenfolge = 2,
            dynamischErzeugt = true,
        )
        val knoten = basis.copy(anschlüsse = basis.anschlüsse + dritter)
        val ausgabe = requireNotNull(register.finde(knoten.art)).auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "a" to BedingterWert(WahrheitsKonstante(true)),
                    "b" to BedingterWert(WahrheitsKonstante(true)),
                    "c" to BedingterWert(WahrheitsKonstante(true)),
                ),
                rechenKontext = RechenKontext(),
            ),
        ).ausgaben.getValue("aussage").objekt

        assertEquals(Wahrheitswert.Wahr, assertIs<de.TeutonStudio.MathematikRechenSystem.kern.Aussage>(ausgabe).entscheide().wahrheitswert)
        assertEquals(
            "\\mathcal{Wahr} \\stackrel{\\bullet}{\\lor} \\mathcal{Wahr} \\stackrel{\\bullet}{\\lor} \\mathcal{Wahr}",
            ausgabe.zuLatex(),
        )
    }

    @Test
    fun `Wahrheitstabelle beginnt mit Wahr und wechselt den letzten Eingang zuerst`() {
        val tabelle = Wahrheitstabelle(AussagenOperatorArt.Adjunktion, 3)

        assertEquals(BigInteger.valueOf(8), tabelle.zeilenAnzahl)
        assertEquals(listOf(true, true, true), tabelle.zeile(BigInteger.ZERO).eingänge)
        assertTrue(tabelle.zeile(BigInteger.ZERO).ergebnis)
        assertEquals(listOf(true, true, false), tabelle.zeile(BigInteger.ONE).eingänge)
        assertFalse(tabelle.zeile(BigInteger.ONE).ergebnis)
    }

    @Test
    fun `große Wahrheitstabelle materialisiert nur angeforderte Zeilen`() {
        val tabelle = Wahrheitstabelle(AussagenOperatorArt.Konjunktion, 70)

        assertEquals(BigInteger.ONE.shiftLeft(70), tabelle.zeilenAnzahl)
        assertTrue(tabelle.zeile(BigInteger.ZERO).ergebnis)
        assertFalse(tabelle.zeile(BigInteger.ONE).ergebnis)
    }
}
