package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.GanzeZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.SpaltenVektor
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechner
import de.TeutonStudio.MathematikRechenSystem.kern.VektorRechnerOperator
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KonsolidierteKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Erstellen Dialog enthält genau einen Zahlenrechner und einen Mengenknoten`() {
        val vorlagen = alleMathematikKnotenVorlagen() + MengenraumKnotenVorlagen.alle
        assertEquals(1, vorlagen.count { it.art == ZAHLENRECHNER_ART })
        assertEquals(1, vorlagen.count { it.art == MENGEN_KNOTEN_ART })
        assertEquals(1, vorlagen.count { it.art == VektorRechner.KNOTEN_ART })
        assertTrue(vorlagen.none { it.art in historischeMengenKnotenArten })
        assertTrue(vorlagen.none { it.art in historischeSkalarproduktArten })
    }

    @Test
    fun `Mengenknoten wertet die Inspector Auswahl aus`() {
        val knoten = MengenKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf(MENGEN_KNOTEN_AUSWAHL to MengenKnotenAuswahl.GANZE_ZAHLEN.stabileId),
        )
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(knoten, emptyMap(), RechenKontext()),
        )
        assertEquals(GanzeZahlen, ergebnis.ausgaben.getValue("menge").objekt)
    }

    @Test
    fun `Einheitsvektor verwendet nullbasierte Position und dynamische Dimension`() {
        val knoten = EinheitsvektorKnotenVorlagen.Spalte.erzeuge(GraphPunkt.Zero)
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    EINHEITSVEKTOR_POSITION to BedingterWert(RationaleZahl.von(1)),
                    EINHEITSVEKTOR_DIMENSION to BedingterWert(RationaleZahl.von(3)),
                ),
                RechenKontext(),
            ),
        )
        val vektor = assertIs<SpaltenVektor>(ergebnis.ausgaben.getValue("vektor").objekt)
        assertEquals(
            listOf(RationaleZahl.Null, RationaleZahl.Eins, RationaleZahl.Null),
            vektor.werte,
        )
    }

    @Test
    fun `Einheitsvektor lehnt Position außerhalb der Dimension ab`() {
        val knoten = EinheitsvektorKnotenVorlagen.Zeile.erzeuge(GraphPunkt.Zero)
        assertFailsWith<IllegalArgumentException> {
            register.finde(knoten.art)!!.auswerten(
                KnotenAuswertungsKontext(
                    knoten,
                    mapOf(
                        EINHEITSVEKTOR_POSITION to BedingterWert(RationaleZahl.von(3)),
                        EINHEITSVEKTOR_DIMENSION to BedingterWert(RationaleZahl.von(3)),
                    ),
                    RechenKontext(),
                ),
            )
        }
    }

    @Test
    fun `Vektorrechner bildet Skalarprodukt auch aus kartesischen Tupeln`() {
        val knoten = VektorRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        assertEquals(VEKTOR_RECHNER_AUSGANG, knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.name)
        val ergebnis = register.finde(knoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "links" to BedingterWert(Tupel(listOf(RationaleZahl.von(1), RationaleZahl.von(2)))),
                    "rechts" to BedingterWert(Tupel(listOf(RationaleZahl.von(3), RationaleZahl.von(4)))),
                ),
                RechenKontext(),
            ),
        )
        val wert = assertIs<RationaleZahl>(ergebnis.ausgaben.getValue(VEKTOR_RECHNER_AUSGANG).objekt)
        assertEquals(BigInteger.valueOf(11), wert.zähler)
        assertEquals(BigInteger.ONE, wert.nenner)
    }

    @Test
    fun `Migration erhält Anschluss IDs und übernimmt historische Konfiguration`() {
        val alteMenge = MengenraumKnotenVorlagen.Primzahlen.erzeuge(GraphPunkt.Zero)
        val alteEinheit = MathematikKnotenVorlagen.EinheitsSpalte.erzeuge(GraphPunkt(10f, 10f)).copy(
            parameter = mapOf("index" to "2", "dimension" to "5"),
        )
        val altesSkalarprodukt = MathematikKnotenVorlagen.SkalarproduktZeile.erzeuge(GraphPunkt(20f, 20f))
        val alt = KartenDaten(
            name = "Migrationstest",
            knoten = listOf(alteMenge, alteEinheit, altesSkalarprodukt),
        )
        val migriert = alt.migriereKonsolidierteKnoten()

        val menge = migriert.knoten.first { it.id == alteMenge.id }
        assertEquals(MENGEN_KNOTEN_ART, menge.art)
        assertEquals(alteMenge.anschlüsse.single().id, menge.anschlüsse.single().id)

        val einheit = migriert.knoten.first { it.id == alteEinheit.id }
        assertEquals("2", einheit.parameter["standardwert.position"])
        assertEquals("5", einheit.parameter["standardwert.dimension"])
        assertNotNull(einheit.anschlüsse.firstOrNull { it.name == EINHEITSVEKTOR_POSITION && it.richtung == AnschlussRichtung.Eingang })
        assertEquals(
            alteEinheit.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.id,
            einheit.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.id,
        )

        val skalar = migriert.knoten.first { it.id == altesSkalarprodukt.id }
        assertEquals(VektorRechner.KNOTEN_ART, skalar.art)
        assertEquals(VektorRechnerOperator.SKALARPRODUKT.stabileId, skalar.parameter[VEKTOR_RECHNER_OPERATOR])
        assertEquals(VEKTOR_RECHNER_AUSGANG, skalar.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.name)
        assertEquals(
            altesSkalarprodukt.anschlüsse.map { it.id }.toSet(),
            skalar.anschlüsse.map { it.id }.toSet(),
        )

        assertEquals(migriert, migriert.migriereKonsolidierteKnoten())
    }
}
