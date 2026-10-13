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

    @Test fun `historische Version eins bleibt lesbar`() {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use {
            it.writeInt(1); it.writeByte(0); it.writeUTF("alt"); it.writeUTF("ZAHL"); it.writeUTF("42")
        }
        val ast = StrukturFormelCodec.dekodieren(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray()))
        assertEquals(FormelAusdruck.Literal("alt", RationaleZahl.von(42), FormelTyp.ZAHL), ast)
    }

    @Test fun `nicht unterstuetzte Werte werden nicht als Latex gespeichert`() {
        val objekt = object : MathematischesObjekt { override fun zuLatex() = "1" }
        assertFailsWith<IllegalStateException> { MathematischerWertCodec.kodieren(objekt) }
    }
}
