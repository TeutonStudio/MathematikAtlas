package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SmallIssuesBatchRegressionTest {
    @Test
    fun `Variable als Tupel bleibt strukturiert und wird bei Methodenbildung geordnet destrukturiert`() {
        val register = GesamterMathematikAuswerter.erzeugeRegister()
        val quelle = konfiguriereVariablenStruktur(
            MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero),
            VARIABLE_STRUKTUR_TUPEL,
        ).copy(
            id = KnotenId("tupel-variable"),
            parameter = mapOf(
                "name" to "x",
                "werteVorrat" to "R",
                VARIABLE_STRUKTUR to VARIABLE_STRUKTUR_TUPEL,
                VARIABLE_DIMENSION to "3",
            ),
        )
        assertEquals(MathematikAnschlussArten.Tupel.id, quelle.anschlüsse.single().art)
        assertEquals("wert", quelle.anschlüsse.single().name)
        val tupelWert = register.finde(quelle.art)!!.auswerten(
            KnotenAuswertungsKontext(quelle, emptyMap(), RechenKontext()),
        ).ausgaben.getValue("wert")
        val tupel = assertIs<Tupel>(tupelWert.objekt)

        assertEquals(
            listOf("x_1", "x_2", "x_3"),
            tupel.elemente.map { assertIs<Variable>(it).name },
        )
        assertEquals(listOf(0, 1, 2), tupelWert.variablenQuellen.map { it.reihenfolge })
        assertEquals(1, tupelWert.variablenQuellen.map { it.bindungsId }.distinct().size)
        assertEquals(
            listOf("komponente-1", "komponente-2", "komponente-3"),
            tupelWert.variablenQuellen.map { it.bindungsName },
        )
        assertEquals(Tupelraum(List(3) { ReelleZahlen }), tupelWert.werteVorrat)

        val kartesisch = tupel.kartesischerTupelVertrag(
            tupelWert.variablenQuellen.associate { it.name to it.werteVorrat },
        )
        val gültig = assertIs<StrukturPruefung.Gueltig<KartesischerTupelVertrag>>(kartesisch)
        assertEquals(3, gültig.wert.laenge)
        assertEquals(ReelleZahlen, gültig.wert.zahlBereich)

        val methodeKnoten = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero).copy(
            id = KnotenId("methode"),
            parameter = mapOf("name" to "f", "argumentReihenfolge" to ""),
        )
        // Transportreihenfolge der Metadaten darf die Komponentenreihenfolge nicht verändern.
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
    fun `Eigenstaendige Tupelvariable ist nicht mehr verfuegbar`() {
        val register = GesamterMathematikAuswerter.erzeugeRegister()
        assertEquals(null, register.finde("mathematik.tupelvariable"))
        assertFalse(
            de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog
                .alle().any { it.art == "mathematik.tupelvariable" },
        )
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
