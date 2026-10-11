package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MethodenBereichsOperatorAufrufTest {
    @Test
    fun `strukturierte Restriktion bleibt ueber Methodenaufruf mathematisch auswertbar`() {
        val x = Variable("x")
        val domain = endlicheMenge(0, 1)
        val basis = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = addition(x, RationaleZahl.Eins),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to domain),
        )
        val restriktion = assertNotNull(restriktiereMethode(basis, endlicheMenge(0)).methode)

        val wert = rufeAuf(restriktion, RationaleZahl.Null)

        assertEquals(RationaleZahl.Eins, wert)
    }

    @Test
    fun `strukturierte Bereichsanpassung bleibt ueber Methodenaufruf mathematisch auswertbar`() {
        val ziel = endlicheMenge(0, 1)
        val basis = konstanteMethode("f", endlicheMenge(0), 0, ziel)
        val ergänzung = konstanteMethode("g", endlicheMenge(1), 1, ziel)
        val anpassung = assertNotNull(
            passeMethodenBereichAn(
                basis = basis,
                menge = endlicheMenge(0, 1),
                ergänzungen = listOf(ergänzung),
            ).methode,
        )

        val wert = rufeAuf(anpassung, RationaleZahl.Eins)

        assertEquals(RationaleZahl.Eins, wert)
    }

    private fun rufeAuf(methode: Methode, argument: MathematischesObjekt): AtlasWert {
        val knoten = KnotenDaten(
            art = METHODEN_AUFRUF_ART,
            name = "Methode aufrufen",
            parameter = mapOf(
                METHODEN_AUFRUF_ARGUMENTPROJEKTION to METHODEN_ARGUMENTPROJEKTION_SEPARIERT,
                METHODEN_AUFRUF_ERGEBNISPROJEKTION to METHODEN_ERGEBNISPROJEKTION_DIREKT,
            ),
            anschlüsse = listOf(
                AnschlussDaten(
                    name = "methode",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = AnschlussArtId("mathematik.methode"),
                    reihenfolge = 0,
                ),
                AnschlussDaten(
                    name = "argument",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = AnschlussArtId("mathematik.objekt"),
                    reihenfolge = 1,
                ),
                AnschlussDaten(
                    name = "wert",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = AnschlussArtId("mathematik.objekt"),
                    reihenfolge = 2,
                ),
            ),
        )
        return MethodenAufrufAuswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "methode" to BedingterWert(methode),
                    "argument" to BedingterWert(argument),
                ),
                rechenKontext = RechenKontext(),
            ),
        ).ausgaben.getValue("wert").objekt
    }

    private fun konstanteMethode(
        name: String,
        domain: MengenAusdruck,
        wert: Long,
        ziel: MengenAusdruck,
    ): MathematischeMethode {
        val x = Variable("x")
        return Methode(
            name = name,
            parameter = listOf(x),
            vorschrift = RationaleZahl.von(wert),
            zielMenge = ziel,
            werteVorräte = mapOf(x.name to domain),
        )
    }

    private fun endlicheMenge(vararg werte: Long): MengenAusdruck =
        EndlicheMenge(werte.map { RationaleZahl.von(it) }.toSet())
}
