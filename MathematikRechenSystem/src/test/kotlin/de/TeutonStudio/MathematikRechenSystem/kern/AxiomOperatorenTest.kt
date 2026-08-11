package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AxiomOperatorenTest {
    private val zweiElemente = EndlicheMenge(setOf(RationaleZahl.Null, RationaleZahl.Eins))

    private fun gleichheitsPraedikat(): Methode {
        val x = Variable("x")
        val y = Variable("y")
        return Methode(
            name = "R",
            parameter = listOf(x, y),
            vorschrift = Gleichheit(x, y),
            zielMenge = WahrheitsMenge,
            werteVorräte = mapOf(x.name to zweiElemente, y.name to zweiElemente),
        )
    }

    private fun identitaet(name: String = "S"): Methode {
        val x = Variable("x")
        return Methode(
            name = name,
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = zweiElemente,
            werteVorräte = mapOf(x.name to zweiElemente),
        )
    }

    @Test
    fun `Axiomregister enthaelt Peano ZF ZFC und algebraische Strukturen`() {
        val ids = AxiomOperatoren.alle.mapTo(setOf()) { it.stabileId }

        assertTrue("axiom.peano.induktion" in ids)
        assertTrue("axiom.zf.extensionalitaet" in ids)
        assertTrue("axiom.zfc.auswahl" in ids)
        assertTrue("axiom.algebra.ring" in ids)
        assertTrue("axiom.algebra.schiefkoerper" in ids)
        assertTrue("axiom.algebra.koerper" in ids)
    }

    @Test
    fun `Relationsaxiom ist dasselbe auswertbare Praedikat das der Nutzer einsetzen kann`() {
        val definition = checkNotNull(AxiomOperatoren.vonIdOderNull("axiom.relation.reflexiv"))
        val aussage = definition.werteAus(
            mapOf(
                "menge" to zweiElemente,
                "relation" to gleichheitsPraedikat(),
            ),
        )

        assertEquals(Wahrheitswert.Wahr, aussage.entscheide().wahrheitswert)
        assertTrue(aussage.zuLatex().contains("\\forall x\\in"))
    }

    @Test
    fun `Peano Nachfolgerabschluss wird auf endlichem Traeger ausgewertet`() {
        val definition = checkNotNull(AxiomOperatoren.vonIdOderNull("axiom.peano.nachfolgerAbgeschlossen"))
        val aussage = definition.werteAus(
            mapOf(
                "menge" to zweiElemente,
                "nachfolger" to identitaet(),
            ),
        )

        assertEquals(Wahrheitswert.Wahr, aussage.entscheide().wahrheitswert)
    }

    @Test
    fun `Peano Induktion akzeptiert ein einstelliges Praedikat`() {
        val x = Variable("x")
        val p = Methode(
            name = "P",
            parameter = listOf(x),
            vorschrift = Gleichheit(x, x),
            zielMenge = WahrheitsMenge,
            werteVorräte = mapOf(x.name to zweiElemente),
        )
        val definition = checkNotNull(AxiomOperatoren.vonIdOderNull("axiom.peano.induktion"))
        val aussage = definition.werteAus(
            mapOf(
                "menge" to zweiElemente,
                "null" to RationaleZahl.Null,
                "nachfolger" to identitaet(),
                "praedikat" to p,
            ),
        )

        assertEquals(Wahrheitswert.Wahr, aussage.entscheide().wahrheitswert)
        assertTrue(aussage.zuLatex().contains("P"))
    }

    @Test
    fun `Praedikateingang prueft Methodenart und Stelligkeit semantisch`() {
        val x = Variable("x")
        val keineRelation = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = zweiElemente,
            werteVorräte = mapOf(x.name to zweiElemente),
        )
        val definition = checkNotNull(AxiomOperatoren.vonIdOderNull("axiom.relation.symmetrisch"))

        assertFailsWith<IllegalArgumentException> {
            definition.werteAus(mapOf("menge" to zweiElemente, "relation" to keineRelation))
        }
    }

    @Test
    fun `Ring auf trivialer Einermenge ist auswertbar waehrend Integritaetsbereich Nichttrivialitaet fordert`() {
        val m = EndlicheMenge(setOf(RationaleZahl.Null))
        val x = Variable("x")
        val y = Variable("y")
        val op = Methode(
            name = "op",
            parameter = listOf(x, y),
            vorschrift = RationaleZahl.Null,
            zielMenge = m,
            werteVorräte = mapOf(x.name to m, y.name to m),
        )
        val inv = Methode(
            name = "inv",
            parameter = listOf(x),
            vorschrift = RationaleZahl.Null,
            zielMenge = m,
            werteVorräte = mapOf(x.name to m),
        )

        val ring = checkNotNull(AxiomOperatoren.vonIdOderNull("axiom.algebra.ring")).werteAus(
            mapOf(
                "menge" to m,
                "addition" to op,
                "multiplikation" to op,
                "null" to RationaleZahl.Null,
                "eins" to RationaleZahl.Null,
                "negation" to inv,
            ),
        )
        val integritaetsbereich = checkNotNull(AxiomOperatoren.vonIdOderNull("axiom.algebra.integritaetsbereich")).werteAus(
            mapOf(
                "menge" to m,
                "addition" to op,
                "multiplikation" to op,
                "null" to RationaleZahl.Null,
                "eins" to RationaleZahl.Null,
                "negation" to inv,
            ),
        )

        assertEquals(Wahrheitswert.Wahr, ring.entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, integritaetsbereich.entscheide().wahrheitswert)
    }

    @Test
    fun `Axiomensysteme bilden ZFC und Koerperhierarchie ab`() {
        val zfc = AxiomOperatoren.systeme.single { it.stabileId == "zfc" }
        val koerper = AxiomOperatoren.systeme.single { it.stabileId == "koerper" }

        assertTrue("zf" in zfc.vorausgesetzteSysteme)
        assertTrue("schiefkoerper" in koerper.vorausgesetzteSysteme)
        assertTrue("kommutativer-ring" in koerper.vorausgesetzteSysteme)
    }
}
