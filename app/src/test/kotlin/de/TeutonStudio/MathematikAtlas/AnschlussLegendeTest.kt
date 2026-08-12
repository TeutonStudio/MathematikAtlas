package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten
import de.TeutonStudio.TypSystem.AnschlussVertrag
import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    fun `semantischer Methodenanschluss erscheint unbedingt als Methode in der Legende`() {
        val argument = TypAusdruck.Parameterisiert(
            TypId("typ.tupel"),
            listOf(TypAusdruck.Atom(TypId("mathematik.zahl"))),
        )
        val methodenTyp = TypAusdruck.Parameterisiert(
            TypId("mathematik.methode"),
            listOf(argument, TypAusdruck.Atom(TypId("mathematik.menge"))),
        )
        val methode = AnschlussDaten(
            name = "f",
            kante = AnschlussKante.Rechts,
            art = MathematikAnschlussArten.Objekt.id,
            vertrag = AnschlussVertrag(typ = methodenTyp),
        )
        val karte = KartenDaten(
            name = "Methodenlegende",
            knoten = listOf(
                KnotenDaten(
                    art = "test.methode",
                    name = "Methode",
                    anschlüsse = listOf(methode),
                ),
            ),
        )

        val eintrag = anschlussLegendenEinträge(karte, register)
            .firstOrNull { it.arten.singleOrNull()?.id == MathematikAnschlussArten.Methode.id }

        assertNotNull(eintrag)
        assertEquals("Methode", eintrag.titel)
        assertEquals(methodenTyp, eintrag.symbolAnschluss.vertrag.typ)
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
