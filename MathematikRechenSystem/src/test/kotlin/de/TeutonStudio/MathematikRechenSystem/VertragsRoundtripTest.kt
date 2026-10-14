package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.Base64
import kotlin.test.*

class VertragsRoundtripTest {
    @Test fun `Tupelkonkatenation hat leere und einstellige Identitaet`() {
        val t = Tupel(listOf(Tupel(listOf(RationaleZahl.Eins))))
        assertEquals(Tupel(emptyList()), ergänzeTupel(emptyList()))
        assertEquals(t, ergänzeTupel(listOf(t)))
        assertEquals(t, ergänzeTupelUmElemente(t, emptyList()))
    }

    @Test fun `typisierte Literale unterscheiden Skalar Einertupel und Vektoren`() {
        val x = Variable("x", "\\xi")
        val werte = listOf<MathematischesObjekt>(
            x, RationaleZahl.Eins, Tupel(emptyList()), Tupel(listOf(x)),
            Tupel(listOf(Tupel(listOf(x)), EndlicheMenge(setOf(RationaleZahl.Eins)))),
            SpaltenVektor(listOf(x)), ZeilenVektor(listOf(x)), Matrix(listOf(listOf(x))),
            Tensor(listOf(1, 1, 1), listOf(x)),
            Tupelraum(listOf(ReelleZahlen, EndlicheMenge(setOf(RationaleZahl.Eins)))),
            ReellesIntervall(RationaleZahl.Null, true, RationaleZahl.Eins, false),
        )
        werte.forEach { wert ->
            assertEquals(wert, MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(wert)))
            val ast = FormelAusdruck.Literal("literal", wert, FormelTyp.OBJEKT)
            assertEquals(ast, StrukturFormelCodec.dekodieren(StrukturFormelCodec.kodieren(ast)))
            assertEquals(ast, (GraphZuFormel.konvertiere(FormelZuGraph.konvertiere(ast)) as GraphZuFormelErgebnis.Erfolg).wurzel)
        }
    }

    @Test fun `Bedingung h ungleich null ueberlebt Graph und Codec`() {
        val h = Variable("h")
        val bedingung = Ungleichheit(h, RationaleZahl.Null)
        val ast = FormelAusdruck.Operation("division", "zahl.division", listOf(
            FormelArgument("zaehler", 0, FormelAusdruck.Literal("eins", RationaleZahl.Eins, FormelTyp.ZAHL)),
            FormelArgument("nenner", 1, FormelAusdruck.Variable("h-vorkommen", "h", typ = FormelTyp.ZAHL)),
        ), FormelTyp.ZAHL, listOf(bedingung))
        assertEquals(ast, StrukturFormelCodec.dekodieren(StrukturFormelCodec.kodieren(ast)))
        assertEquals(ast, (GraphZuFormel.konvertiere(FormelZuGraph.konvertiere(ast)) as GraphZuFormelErgebnis.Erfolg).wurzel)
    }

    @Test fun `alte mathematische Wire-Versionen werden abgelehnt`() {
        fun fixture(version: Int): String = Base64.getUrlEncoder().withoutPadding().encodeToString(
            ByteArrayOutputStream().also { bytes -> DataOutputStream(bytes).use { it.writeInt(version) } }.toByteArray(),
        )
        assertFailsWith<IllegalArgumentException> { StrukturFormelCodec.dekodieren(fixture(1)) }
        assertFailsWith<IllegalArgumentException> { MathematischerWertCodec.dekodieren(fixture(1)) }
        assertFailsWith<IllegalArgumentException> { FormelKontextCodec.dekodieren(fixture(1)) }
    }

    @Test fun `nicht unterstuetzte Werte werden nicht als Latex gespeichert`() {
        val objekt = object : MathematischesObjekt { override fun zuLatex() = "1" }
        assertFailsWith<IllegalStateException> { MathematischerWertCodec.kodieren(objekt) }
    }
}
