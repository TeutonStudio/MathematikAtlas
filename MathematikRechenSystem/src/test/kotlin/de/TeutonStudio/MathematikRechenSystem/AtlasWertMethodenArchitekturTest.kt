package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AtlasWertMethodenArchitekturTest {
    private data class FremderEngineWert(val kennung: String) : AtlasWert

    private data class TestDarstellungsWert(val text: String) : DarstellungsWert {
        override fun zuLatex(): String = text
    }

    private data class TestScriptMethod(
        override val name: String,
        override val signatur: MethodenSignatur,
    ) : SignaturtragendeMethode

    @Test
    fun `mathematik darstellung und engine wert teilen nur AtlasWert`() {
        val mathematik: AtlasWert = RationaleZahl.Eins
        val darstellung: AtlasWert = TestDarstellungsWert("grafik")
        val engine: AtlasWert = FremderEngineWert("node")

        assertIs<MathematischesObjekt>(mathematik)
        assertIs<DarstellungsWert>(darstellung)
        assertFalse(darstellung is MathematischesObjekt)
        assertFalse(engine is MathematischesObjekt)
        assertFalse(engine is LatexDarstellbar)
    }

    @Test
    fun `scriptmethode besitzt neutrale Tupel Signatur ohne MengenCapability`() {
        val node3d = TypAusdruck.Atom(TypId("engine.node3d"))
        val float = TypAusdruck.Atom(TypId("engine.float"))
        val vector3 = TypAusdruck.Atom(TypId("engine.vector3"))
        val methode = TestScriptMethod(
            name = "move",
            signatur = MethodenSignatur(
                argumente = listOf(
                    MethodenKomponente("owner", "owner", 0, node3d),
                    MethodenKomponente("delta", "delta", 1, float),
                ),
                ergebnisse = listOf(
                    MethodenKomponente("position", "position", 0, vector3),
                ),
            ),
        )

        assertFalse(methode is MathematischesObjekt)
        assertFalse(methode is MathematischeSignaturtragendeMethode)
        assertEquals(
            TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, listOf(node3d, float)),
            methode.signatur.argumentTyp,
        )
        assertEquals(
            TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, listOf(vector3)),
            methode.signatur.ergebnisTyp,
        )
    }

    @Test
    fun `historische nullstellige LeereMenge wird als leeres Tupelprodukt gelesen`() {
        val alt = Methode(
            name = "c",
            parameter = emptyList(),
            vorschrift = RationaleZahl.Eins,
            zielMenge = ReelleZahlen,
            effektiverWerteVorrat = LeereMenge,
        )

        assertEquals(Tupelraum(emptyList()), alt.mathematischeSignatur.definitionsRaum)
        assertNotEquals(LeereMenge, alt.mathematischeSignatur.definitionsRaum)
    }

    @Test
    fun `historische Einzelausgabe wird sofort kanonisch gepackt`() {
        @Suppress("DEPRECATION")
        val alt = Methode(
            name = "alt",
            parameter = emptyList(),
            ausgaben = linkedMapOf("wert" to RationaleZahl.Eins),
            zielMengen = linkedMapOf("wert" to ReelleZahlen),
        )

        assertEquals(Tupel(listOf(RationaleZahl.Eins)), alt.ergebnisTupel)
        assertEquals(Tupelraum(listOf(ReelleZahlen)), alt.zielRaum)
        assertEquals(ReelleZahlen, alt.zielMengeFür("wert"))
    }

    @Test
    fun `Zielmenge bleibt vom tatsächlichen Bild getrennt`() {
        val x = Variable("x")
        val definitionsMenge = EndlicheMenge(setOf(RationaleZahl.Null, RationaleZahl.Eins))
        val methode = Methode(
            name = "null",
            parameter = listOf(x),
            vorschrift = RationaleZahl.Null,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to definitionsMenge),
        )

        val bild = bildeAb(definitionsMenge, methode)
        assertEquals(ReelleZahlen, methode.zielMengeFür("wert"))
        assertEquals(EndlicheMenge(setOf(RationaleZahl.Null)), bild)
        assertNotEquals(methode.zielMengeFür("wert"), bild)
    }

    @Test
    fun `null und Einerausgaben bleiben neutrale Tupeltypen`() {
        val wert = TypAusdruck.Atom(TypId("test.wert"))
        val ohne = MethodenSignatur(emptyList(), emptyList())
        val eins = MethodenSignatur(
            emptyList(),
            listOf(MethodenKomponente("wert", "wert", 0, wert)),
        )

        assertEquals(TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, emptyList()), ohne.ergebnisTyp)
        assertEquals(TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, listOf(wert)), eins.ergebnisTyp)
        assertTrue(eins.ergebnisTyp.argumente.size == 1)
    }
}
