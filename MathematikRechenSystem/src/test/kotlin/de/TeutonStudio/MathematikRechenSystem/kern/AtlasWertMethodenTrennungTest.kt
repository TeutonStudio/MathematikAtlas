package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypAusdruck
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AtlasWertMethodenTrennungTest {
    private class TestEngineWert : AtlasWert

    private class TestDarstellungsWert : DarstellungsWert

    private class TestScriptMethod(
        override val name: String,
        override val signatur: MethodenSignatur,
    ) : SignaturtragendeMethode

    private fun komponente(
        id: String,
        position: Int,
        typ: TypAusdruck,
    ) = MethodenKomponente(
        id = id,
        name = id,
        position = position,
        typ = typ,
    )

    @Test
    fun `mathematisches Objekt ist AtlasWert`() {
        val wert: AtlasWert = RationaleZahl.Eins
        assertIs<MathematischesObjekt>(wert)
    }

    @Test
    fun `DarstellungsWert ist transportierbar ohne mathematisches Objekt zu sein`() {
        val wert: AtlasWert = TestDarstellungsWert()
        assertIs<DarstellungsWert>(wert)
        assertFalse(wert is MathematischesObjekt)
    }

    @Test
    fun `Enginewert benoetigt weder Latex noch mathematische APIs`() {
        val wert: AtlasWert = TestEngineWert()
        assertFalse(wert is MathematischesObjekt)
        assertFalse(wert is Methode)
    }

    @Test
    fun `ScriptMethod kompiliert ohne mathematisches Objekt oder MengenAusdruck`() {
        val methode: Methode = TestScriptMethod(
            name = "script",
            signatur = MethodenSignatur(
                argumente = listOf(
                    komponente("node", 0, TypAusdruck.Atom(MathematischeTypen.AtlasWert)),
                    komponente("zahl", 1, TypAusdruck.Atom(MathematischeTypen.Zahl)),
                ),
                ergebnisse = listOf(
                    komponente("ergebnis", 0, TypAusdruck.Atom(MathematischeTypen.AtlasWert)),
                ),
            ),
        )

        assertIs<SignaturtragendeMethode>(methode)
        assertFalse(methode is MathematischesObjekt)
        assertFalse(methode is MathematischeSignaturtragendeMethode)
        assertEquals(2, methode.neutraleMethodenSignatur().argumente.size)
        assertNull(MethodenAnforderung.Stelligkeit(2).prüfe(methode))
    }

    @Test
    fun `neutrale Methodentypen bleiben auch fuer null und ein Komponenten Tupel`() {
        val methode = TestScriptMethod(
            name = "konstante",
            signatur = MethodenSignatur(
                argumente = emptyList(),
                ergebnisse = listOf(
                    komponente("wert", 0, TypAusdruck.Atom(MathematischeTypen.Zahl)),
                ),
            ),
        )

        val typ = assertIs<TypAusdruck.Parameterisiert>(methode.typAusdruck)
        val argumentTyp = assertIs<TypAusdruck.Parameterisiert>(typ.argumente[0])
        val ergebnisTyp = assertIs<TypAusdruck.Parameterisiert>(typ.argumente[1])
        assertEquals(MathematischeTypen.Tupel, argumentTyp.konstruktor)
        assertTrue(argumentTyp.argumente.isEmpty())
        assertEquals(MathematischeTypen.Tupel, ergebnisTyp.konstruktor)
        assertEquals(1, ergebnisTyp.argumente.size)
    }

    @Test
    fun `Scriptmethodenkomposition prueft ausschliesslich neutrale Tupeltypen`() {
        val zahl = TypAusdruck.Atom(MathematischeTypen.Zahl)
        val innen = TestScriptMethod(
            name = "innen",
            signatur = MethodenSignatur(
                argumente = listOf(komponente("eingang", 0, zahl)),
                ergebnisse = listOf(komponente("zwischenwert", 0, zahl)),
            ),
        )
        val aussen = TestScriptMethod(
            name = "aussen",
            signatur = MethodenSignatur(
                argumente = listOf(komponente("zwischenwert", 0, zahl)),
                ergebnisse = listOf(komponente("ausgang", 0, zahl)),
            ),
        )

        assertTrue(prüfeKompositionsKette(listOf(aussen, innen)).istGültig)
        val komposition = komponiere(listOf(aussen, innen))
        assertIs<KomponierteMethode>(komposition)
        assertFalse(komposition is MathematischesObjekt)
        assertEquals(innen.signatur.argumentTupelTyp, komposition.signatur.argumentTupelTyp)
        assertEquals(aussen.signatur.ergebnisTupelTyp, komposition.signatur.ergebnisTupelTyp)
    }
}
