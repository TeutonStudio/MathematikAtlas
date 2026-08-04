package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.BegriffsAussage
import de.TeutonStudio.MathematikRechenSystem.kern.FundamentalerZahlbereich
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.NachweisStatus
import de.TeutonStudio.MathematikRechenSystem.kern.NatürlicheZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.SKALARPRODUKT_ZERTIFIKAT_VERSION
import de.TeutonStudio.MathematikRechenSystem.kern.SkalarproduktLinearitaet
import de.TeutonStudio.MathematikRechenSystem.kern.SpaltenVektor
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.ZeilenVektor
import de.TeutonStudio.MathematikRechenSystem.kern.multiplikation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SkalarproduktKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Begriffsknoten besitzt genau Methoden-Eingang und Aussagen-Ausgang`() {
        val vorlage = SkalarproduktKnotenVorlagen.Begriff

        assertTrue(vorlage.art in alleMathematikKnotenVorlagen().map { it.art })
        assertEquals(2, vorlage.anschlüsse.size)
        val methode = vorlage.anschlüsse.single { it.name == "methode" }
        val aussage = vorlage.anschlüsse.single { it.name == "aussage" }
        assertEquals(AnschlussRichtung.Eingang, methode.richtung)
        assertEquals(MathematikAnschlussArten.Methode.id, methode.art)
        assertEquals(AnschlussRichtung.Ausgang, aussage.richtung)
        assertEquals(MathematikAnschlussArten.Aussage.id, aussage.art)
        assertNotNull(register.finde(vorlage.art))
    }

    @Test
    fun `Begriffsknoten erzeugt mit vollständigen Referenzen eine nachgewiesene Aussage`() {
        val knoten = SkalarproduktKnotenVorlagen.Begriff.erzeuge(GraphPunkt.Zero).copy(
            parameter = SkalarproduktKnotenVorlagen.Begriff.standardParameter + mapOf(
                SKALARPRODUKT_ZAHLBEREICH_PARAMETER to FundamentalerZahlbereich.REELL.id,
                SKALARPRODUKT_LINEARITAET_PARAMETER to SkalarproduktLinearitaet.RECHTSLINEAR.name,
                SKALARPRODUKT_ZERTIFIKAT_VERSION_PARAMETER to SKALARPRODUKT_ZERTIFIKAT_VERSION.toString(),
                SKALARPRODUKT_NACHWEIS_LINEARITAET to "lemma.linear",
                SKALARPRODUKT_NACHWEIS_SYMMETRIE to "lemma.symmetrie",
                SKALARPRODUKT_NACHWEIS_POSITIV to "lemma.positiv",
            ),
        )

        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("methode" to BedingterWert(reelleMultiplikation())),
                rechenKontext = RechenKontext(),
            ),
        )

        val aussage = assertIs<BegriffsAussage>(ergebnis.ausgaben.getValue("aussage").objekt)
        assertEquals(NachweisStatus.Nachgewiesen, aussage.pruefung.status)
    }

    @Test
    fun `Standardskalarprodukt akzeptiert Zeile und Spalte ohne Orientierungszwang`() {
        val knoten = VektorRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "links" to BedingterWert(
                        ZeilenVektor(listOf(RationaleZahl.von(1), RationaleZahl.von(2))),
                    ),
                    "rechts" to BedingterWert(
                        SpaltenVektor(listOf(RationaleZahl.von(3), RationaleZahl.von(4))),
                    ),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        val wert = ergebnis.ausgaben.getValue(VEKTOR_RECHNER_AUSGANG)
        assertEquals(RationaleZahl.von(11), wert.objekt)
        assertEquals(NatürlicheZahlen, wert.zielMenge)
        assertTrue(wert.latexDarstellung.orEmpty().contains("\\sum_{i=0}^{1}"))
    }

    private fun reelleMultiplikation(): Methode {
        val links = Variable("u")
        val rechts = Variable("v")
        return Methode(
            name = "g",
            parameter = listOf(links, rechts),
            vorschrift = multiplikation(links, rechts),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(
                links.name to ReelleZahlen,
                rechts.name to ReelleZahlen,
            ),
        )
    }
}
