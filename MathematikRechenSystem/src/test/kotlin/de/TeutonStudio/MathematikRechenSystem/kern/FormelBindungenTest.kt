package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FormelBindungenTest {
    @Test
    fun `gleicher sichtbarer Name darf verschiedene Quellen besitzen`() {
        val links = FormelAusdruck.Variable("v-links", "x", typ = FormelTyp.ZAHL)
        val rechts = FormelAusdruck.Variable("v-rechts", "x", typ = FormelTyp.ZAHL)
        val wurzel = FormelAusdruck.Operation(
            id = "summe",
            operatorId = "zahl.addition",
            argumente = listOf(
                FormelArgument("a", 0, links),
                FormelArgument("b", 1, rechts),
            ),
            typ = FormelTyp.ZAHL,
        )
        val formel = GebundeneFormel(
            wurzel = wurzel,
            quellen = listOf(
                FormelVariablenQuelle("quelle-links", "x", FormelVariablenArt.FREI, FormelTyp.ZAHL),
                FormelVariablenQuelle("quelle-rechts", "x", FormelVariablenArt.EXTERN, FormelTyp.ZAHL),
            ),
            variablenVorkommen = mapOf(
                links.id to "quelle-links",
                rechts.id to "quelle-rechts",
            ),
        )

        assertEquals(FormelBindungsPruefung.Gueltig, FormelBindungsPruefer.pruefe(formel))
    }

    @Test
    fun `gebundenes Vorkommen ausserhalb des Quantorbereichs ist ungueltig`() {
        val innen = FormelAusdruck.Variable("innen", "x", typ = FormelTyp.ZAHL)
        val ausserhalb = FormelAusdruck.Variable("aussen", "x", typ = FormelTyp.ZAHL)
        val teilbaum = FormelAusdruck.Operation(
            id = "bereich",
            operatorId = "zahl.betrag",
            argumente = listOf(FormelArgument("argument", 0, innen)),
            typ = FormelTyp.ZAHL,
        )
        val wurzel = FormelAusdruck.Operation(
            id = "wurzel",
            operatorId = "zahl.addition",
            argumente = listOf(
                FormelArgument("a", 0, teilbaum),
                FormelArgument("b", 1, ausserhalb),
            ),
            typ = FormelTyp.ZAHL,
        )
        val formel = GebundeneFormel(
            wurzel = wurzel,
            quellen = listOf(
                FormelVariablenQuelle("gebunden-x", "x", FormelVariablenArt.GEBUNDEN, FormelTyp.ZAHL),
            ),
            variablenVorkommen = mapOf(
                innen.id to "gebunden-x",
                ausserhalb.id to "gebunden-x",
            ),
            quantoren = listOf(
                FormelQuantorBindung("forall-x", FormelQuantorArt.FUER_ALLE, "gebunden-x", teilbaum.id),
            ),
        )

        assertIs<FormelBindungsPruefung.Ungueltig>(FormelBindungsPruefer.pruefe(formel))
    }

    @Test
    fun `praedikat besitzt Aussageziel und stabile Argumentquellen`() {
        val variable = FormelAusdruck.Variable("x-vorkommen", "x", typ = FormelTyp.ZAHL)
        val quelle = FormelVariablenQuelle("x-quelle", "x", FormelVariablenArt.FREI, FormelTyp.ZAHL)
        val formel = GebundeneFormel(
            wurzel = variable,
            quellen = listOf(quelle),
            variablenVorkommen = mapOf(variable.id to quelle.id),
            praedikate = listOf(
                FormelPraedikatsVertrag("p", "P", listOf(quelle.id)),
            ),
        )

        assertEquals(FormelBindungsPruefung.Gueltig, FormelBindungsPruefer.pruefe(formel))
        assertEquals(FormelTyp.AUSSAGE, formel.praedikate.single().zielTyp)
    }
}
