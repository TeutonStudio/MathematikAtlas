package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class IdentitaetsMethodeTest {
    @Test fun `allgemeine Identitaet bewahrt Zahlen Aussagen Mengen Tupel und neutrale Methoden`() {
        val neutral = object : Methode { override val name = "Neutral" }
        val werte: List<AtlasWert> = listOf(RationaleZahl.Eins, WahrheitsKonstante(true), ReelleZahlen, Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2))), neutral)
        werte.forEach { wert ->
            val ergebnis = IdentitaetsMethode.rufeAuf(mapOf(IdentitaetsMethode.ARGUMENT to wert)).getValue(IdentitaetsMethode.ERGEBNIS)
            assertSame(wert, ergebnis.wert)
            assertEquals(IdentitaetsMethode.ARGUMENT, ergebnis.durchgereichtesArgument)
        }
        assertEquals(IdentitaetsMethode.signatur.argumente.single().typ, IdentitaetsMethode.signatur.ergebnisse.single().typ)
        assertFailsWith<IllegalArgumentException> { IdentitaetsMethode.rufeAuf(emptyMap()) }
    }

    @Test fun `eingeschraenkte Identitaet prueft Mitgliedschaft und behaelt ein strukturiertes Ergebnis`() {
        val menge = EndlicheMenge(setOf(RationaleZahl.Eins, RationaleZahl.von(2)))
        val id = identitaetAuf(menge)
        assertEquals(Tupel(listOf(RationaleZahl.Eins)), id.wendeMitBedingungenAn(Tupel(listOf(RationaleZahl.Eins))).ergebnis)
        assertFails { id.wendeMitBedingungenAn(Tupel(listOf(RationaleZahl.von(3)))) }
        assertFails { identitaetAuf(LeereMenge).wendeMitBedingungenAn(Tupel(listOf(RationaleZahl.Eins))) }
        val paar = Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2)))
        val paarId = identitaetAuf(Tupelraum(listOf(ReelleZahlen, ReelleZahlen)))
        assertEquals(1, paarId.signatur.ergebnisse.size)
        assertEquals(Tupel(listOf(paar)), paarId.wendeMitBedingungenAn(Tupel(listOf(paar))).ergebnis)
        val mengenId = identitaetAuf(Potenzmenge(ReelleZahlen))
        val teilmenge = EndlicheMenge(setOf(RationaleZahl.Eins))
        assertEquals(Tupel(listOf(teilmenge)), mengenId.wendeMitBedingungenAn(Tupel(listOf(teilmenge))).ergebnis)
    }
    @Test fun `offene Mitgliedschaft bleibt Bedingung und freie Domaenenvariable wird nicht eingefangen`() {
        val a = MengenParameter("x")
        val id = identitaetAuf(a)
        val ergebnis = id.wendeMitBedingungenAn(Tupel(listOf(RationaleZahl.Eins)))
        assertTrue(ergebnis.bedingungen.isNotEmpty())
        assertEquals(Tupelraum(listOf(a)), id.zielRaum)
        assertEquals(Tupel(listOf(RationaleZahl.Eins)), ergebnis.ergebnis)
    }
    @Test fun `typisierte Identitaet akzeptiert komplexe Zahlen aber keine Menge`() {
        val id = TypisierteIdentitaetsMethode(de.TeutonStudio.TypSystem.TypAusdruck.Atom(MathematischeTypen.Zahl))
        val z = KomplexeZahl(RationaleZahl.Eins, RationaleZahl.Eins)
        assertSame(z, id.rufeAuf(mapOf(IdentitaetsMethode.ARGUMENT to z)).getValue(IdentitaetsMethode.ERGEBNIS).wert)
        assertFailsWith<IllegalArgumentException> { id.rufeAuf(mapOf(IdentitaetsMethode.ARGUMENT to ReelleZahlen)) }
        assertIs<AussagenParameter>(identitaetAuf(WahrheitsMenge).parameter.single())
        val aussagenId = identitaetAuf(WahrheitsMenge)
        assertTrue(aussagenId.istPrädikat())
        assertEquals(aussagenId, MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(aussagenId)))
        val mengenId = identitaetAuf(Potenzmenge(ReelleZahlen))
        assertEquals(mengenId, MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(mengenId)))
    }
}
