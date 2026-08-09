package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class MethodenArgumenteAuswerterTest {
    private val x = Variable("x")
    private val y = Variable("y")
    private val methode = Methode(
        name = "f",
        parameter = listOf(x, y),
        ausgaben = mapOf("wert" to addition(x, y)),
        zielMengen = mapOf("wert" to ReelleZahlen),
        werteVorräte = mapOf(x.name to ReelleZahlen, y.name to GanzeZahlen),
    )

    @Test
    fun `Tupelprojektion erhält Name und Wertevorrat strukturell`() {
        val knoten = KnotenDaten(
            art = METHODEN_ARGUMENTE_ART,
            name = "Methodenargumente",
            parameter = mapOf(METHODEN_ARGUMENTE_PROJEKTION to METHODEN_ARGUMENTPROJEKTION_TUPEL),
        )

        val ergebnis = MethodenArgumenteAuswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("methode" to BedingterWert(methode)),
                rechenKontext = RechenKontext(),
            ),
        )
        val tupel = assertIs<Tupel>(ergebnis.ausgaben.getValue("argumente").objekt)
        val argumente = tupel.elemente.map { assertIs<MethodenArgumentWert>(it) }

        assertEquals(listOf("x", "y"), argumente.map { it.name })
        assertEquals(listOf(ReelleZahlen, GanzeZahlen), argumente.map { it.werteVorrat })
        assertEquals("x \\in \\mathbb{R}", argumente.first().zuLatex())
    }

    @Test
    fun `separierte Projektion liefert geordnete Argumente plus Dimension`() {
        val knoten = KnotenDaten(
            art = METHODEN_ARGUMENTE_ART,
            name = "Methodenargumente",
            parameter = mapOf(METHODEN_ARGUMENTE_PROJEKTION to METHODEN_ARGUMENTPROJEKTION_SEPARIERT),
        )

        val ergebnis = MethodenArgumenteAuswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("methode" to BedingterWert(methode)),
                rechenKontext = RechenKontext(),
            ),
        )

        assertEquals(setOf("x", "y", "dimension"), ergebnis.ausgaben.keys)
        assertEquals("x", assertIs<MethodenArgumentWert>(ergebnis.ausgaben.getValue("x").objekt).name)
        assertEquals("y", assertIs<MethodenArgumentWert>(ergebnis.ausgaben.getValue("y").objekt).name)
        assertEquals(RationaleZahl.von(2), ergebnis.ausgaben.getValue("dimension").objekt)
    }

    @Test
    fun `unbekannter Methodenwert darf keine erfundene Signatur erzeugen`() {
        val knoten = KnotenDaten(
            art = METHODEN_ARGUMENTE_ART,
            name = "Methodenargumente",
        )

        assertFailsWith<IllegalStateException> {
            MethodenArgumenteAuswerter.auswerten(
                KnotenAuswertungsKontext(
                    knoten = knoten,
                    eingänge = mapOf("methode" to BedingterWert(AllgemeinerParameter("f"))),
                    rechenKontext = RechenKontext(),
                ),
            )
        }
    }
}
