package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals

class MethodenZielmengeProjektionTest {
    private val x = Variable("x")
    private val methode = Methode(
        name = "f",
        parameter = listOf(x),
        ausgaben = mapOf("wert" to x),
        zielMengen = mapOf("wert" to ReelleZahlen),
        werteVorräte = mapOf(x.name to ReelleZahlen),
    )

    @Test
    fun `1D Tupelprojektion projiziert auch die Methoden Zielmenge`() {
        val knoten = KnotenDaten(
            art = METHODEN_ZIELMENGE_ART,
            name = "Methoden-Zielmenge",
            parameter = mapOf(
                METHODEN_ZIELMENGE_ERGEBNISPROJEKTION to METHODEN_ERGEBNISPROJEKTION_TUPEL,
            ),
        )

        val ergebnis = MethodenZielmengeSignaturAuswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("methode" to BedingterWert(methode)),
                rechenKontext = RechenKontext(),
            ),
        )

        assertEquals(Tupelraum(listOf(ReelleZahlen)), ergebnis.ausgaben.getValue("menge").objekt)
    }

    @Test
    fun `tupelige Zielmenge wird nicht doppelt verpackt`() {
        val ziel = Tupelraum(listOf(ReelleZahlen, GanzeZahlen))
        val tupelMethode = Methode(
            name = "g",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to Tupel(listOf(x, RationaleZahl.Eins))),
            zielMengen = mapOf("wert" to ziel),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )
        val knoten = KnotenDaten(
            art = METHODEN_ZIELMENGE_ART,
            name = "Methoden-Zielmenge",
            parameter = mapOf(
                METHODEN_ZIELMENGE_ERGEBNISPROJEKTION to METHODEN_ERGEBNISPROJEKTION_TUPEL,
            ),
        )

        val ergebnis = MethodenZielmengeSignaturAuswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("methode" to BedingterWert(tupelMethode)),
                rechenKontext = RechenKontext(),
            ),
        )

        assertEquals(ziel, ergebnis.ausgaben.getValue("menge").objekt)
    }
}
