package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnschlussLegendeTest {
    private val register = AnschlussArtRegister(MathematikAnschlussArten.alle)

    @Test
    fun `Legende dedupliziert Einzelarten und bewahrt ODER Signaturen`() {
        val zahl = AnschlussDaten(
            name = "zahl",
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Zahl.id,
        )
        val gemischt = AnschlussDaten(
            name = "wert",
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Objekt.id,
            zulässigeArten = setOf(
                MathematikAnschlussArten.Zahl.id,
                MathematikAnschlussArten.Tupel.id,
            ),
        )
        val karte = KartenDaten(
            name = "Legendentest",
            knoten = listOf(
                KnotenDaten(
                    art = "test.eins",
                    name = "Eins",
                    anschlüsse = listOf(zahl, gemischt),
                ),
                KnotenDaten(
                    art = "test.zwei",
                    name = "Zwei",
                    anschlüsse = listOf(zahl.copy()),
                ),
            ),
        )

        val einträge = anschlussLegendenEinträge(karte, register)

        assertEquals(2, einträge.size)
        assertEquals(
            listOf(MathematikAnschlussArten.Zahl.id),
            einträge[0].arten.map { it.id },
        )
        assertEquals(
            listOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.Zahl.id),
            einträge[1].arten.map { it.id },
        )
        assertTrue(einträge[1].gestreift)
        assertTrue(einträge.flatMap { it.arten }.all { it.beschreibung.isNotBlank() })
    }

    @Test
    fun `Legende liefert fuer unbekannte Anschlussart sicheren Fallback`() {
        val unbekannt = AnschlussArtId("plugin.unbekannt")
        val karte = KartenDaten(
            name = "Plugin",
            knoten = listOf(
                KnotenDaten(
                    art = "test.plugin",
                    name = "Plugin",
                    anschlüsse = listOf(
                        AnschlussDaten(
                            name = "wert",
                            kante = AnschlussKante.Rechts,
                            art = unbekannt,
                        ),
                    ),
                ),
            ),
        )

        val eintrag = anschlussLegendenEinträge(karte, register).single()

        assertEquals(unbekannt, eintrag.arten.single().id)
        assertEquals("plugin.unbekannt", eintrag.titel)
        assertTrue(eintrag.arten.single().beschreibung.contains("Nicht registrierte Anschlussart"))
    }

    @Test
    fun `Leere Karte besitzt leere Legende`() {
        assertTrue(anschlussLegendenEinträge(KartenDaten(name = "Leer"), register).isEmpty())
    }
}
