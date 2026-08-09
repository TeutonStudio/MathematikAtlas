package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SmallIssuesBatchRegressionTest {
    @Test
    fun `Tupelvariable bleibt Tupel und wird bei Methodenbildung in Indexreihenfolge destrukturiert`() {
        val register = StandardMathematikAuswerter.erzeugeRegister()
        val quelle = TupelVariableKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero).copy(
            id = KnotenId("tupelvariable"),
            parameter = mapOf("name" to "x", "dimension" to "3", "werteVorrat" to "R"),
        )
        val tupelWert = register.finde(quelle.art)!!.auswerten(
            KnotenAuswertungsKontext(quelle, emptyMap(), RechenKontext()),
        ).ausgaben.getValue("tupel")
        val tupel = assertIs<Tupel>(tupelWert.objekt)

        assertEquals(listOf("x_1", "x_2", "x_3"), tupel.elemente.map { it.zuString() })
        assertEquals(listOf(0, 1, 2), tupelWert.variablenQuellen.map { it.reihenfolge })
        assertEquals(1, tupelWert.variablenQuellen.map { it.bindungsId }.distinct().size)

        val methodeKnoten = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero).copy(
            id = KnotenId("methode"),
            parameter = mapOf("name" to "f", "argumentReihenfolge" to ""),
        )
        // Absichtlich umgekehrt angeliefert: die Komponenten-ID/Reihenfolge muss stärker sein
        // als die Listenreihenfolge des transportierten Metadatums.
        val methodeWert = register.finde(methodeKnoten.art)!!.auswerten(
            KnotenAuswertungsKontext(
                methodeKnoten,
                mapOf("term" to tupelWert.copy(variablenQuellen = tupelWert.variablenQuellen.reversed())),
                RechenKontext(),
                mapOf(quelle.id to 0),
            ),
        ).ausgaben.getValue("methode")
        val methode = assertIs<Methode>(methodeWert.objekt)

        assertEquals(listOf("x_1", "x_2", "x_3"), methode.parameter.map { it.name })
        assertIs<Tupel>(methode.vorschrift)
    }

    @Test
    fun `Praedikatmigration benennt nur historischen Standardnamen um`() {
        fun praedikat(name: String) = KnotenDaten(
            id = KnotenId("praedikat-$name"),
            art = "mathematik.termZuMethode",
            name = name,
            anschlüsse = listOf(
                AnschlussDaten(
                    id = AnschlussId("in-$name"),
                    name = "term",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = MathematikAnschlussArten.Aussage.id,
                ),
                AnschlussDaten(
                    id = AnschlussId("out-$name"),
                    name = "methode",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.AussageMethode.id,
                ),
            ),
        )
        val standard = praedikat("Aussage zu Methode")
        val benutzerdefiniert = praedikat("Mein Prädikat")

        val migriert = KartenDaten(name = "Test", knoten = listOf(standard, benutzerdefiniert))
            .migrierePraedikatStandardname()

        assertEquals("Aussage zu Prädikat", migriert.knoten.first { it.id == standard.id }.name)
        assertEquals("Mein Prädikat", migriert.knoten.first { it.id == benutzerdefiniert.id }.name)
    }

    @Test
    fun `Tensorraum Legacydimensionen werden sichtbar und idempotent migriert`() {
        val tensorraum = KnotenDaten(
            id = KnotenId("tensorraum"),
            art = "mathematik.tensorraum",
            name = "Tensorraum",
            position = GraphPunkt(400f, 200f),
            anschlüsse = listOf(
                AnschlussDaten(
                    id = AnschlussId("grundmenge"),
                    name = "grundmenge",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = MathematikAnschlussArten.Menge.id,
                ),
                AnschlussDaten(
                    id = AnschlussId("menge"),
                    name = "menge",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = MathematikAnschlussArten.Menge.id,
                ),
            ),
            parameter = mapOf("dimensionen" to "2, 3, 4"),
        )

        val einmal = KartenDaten(name = "Alt", knoten = listOf(tensorraum)).migriereTensorraumDimensionen()
        val zweimal = einmal.migriereTensorraumDimensionen()
        val migriert = einmal.knoten.first { it.id == tensorraum.id }
        val dimensionen = migriert.anschlüsse.firstOrNull { it.name == "dimensionen" }
        val quelle = einmal.knoten.firstOrNull { it.art == TENSORRAUM_LEGACY_DIMENSIONEN_ART }

        assertNotNull(dimensionen)
        assertEquals(MathematikAnschlussArten.Tupel.id, dimensionen.art)
        assertFalse("dimensionen" in migriert.parameter)
        assertNotNull(quelle)
        assertEquals("2,3,4", quelle.parameter["werte"])
        assertTrue(einmal.verbindungen.any { it.von.knotenId == quelle.id && it.zu.knotenId == tensorraum.id })
        assertEquals(einmal, zweimal)
    }
}
