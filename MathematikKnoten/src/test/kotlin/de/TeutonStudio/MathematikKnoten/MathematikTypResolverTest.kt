package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class MathematikTypResolverTest {
    @Test fun `fundamentale Zahlbereiche behalten ihre Inklusionsstufe`() {
        assertEquals(MathematikTypen.natürlicheZahl, MathematikTypResolver.elementTyp(NatürlicheZahlen))
        assertEquals(
            MathematikTypen.nichtnegativeGanzeZahl,
            MathematikTypResolver.elementTyp(NichtnegativeGanzeZahlenSemantik.menge),
        )
        assertEquals(MathematikTypen.reelleZahl, MathematikTypResolver.elementTyp(ReelleZahlen))
        assertEquals(
            MathematikTypen.quaternionZahl,
            MathematikTypResolver.elementTyp(FundamentalerZahlbereich.QUATERNION.alsMenge()),
        )
    }

    @Test fun `Vektorraum wird mit Orientierung Grundtyp und Dimension typisiert`() {
        val raum = Vektorraum(
            orientierung = VektorOrientierung.Spalte,
            dimension = 3,
            skalarMenge = ReelleZahlen,
        )

        assertEquals(
            MathematikTypen.spaltenVektor(MathematikTypen.reelleZahl, 3),
            MathematikTypResolver.elementTyp(raum),
        )
    }

    @Test fun `Matrizenraum bewahrt beide Achsen im Typ`() {
        val raum = Matrizenraum(
            zeilen = 3,
            spalten = 4,
            skalarMenge = KomplexeZahlen,
        )

        assertEquals(
            MathematikTypen.matrix(MathematikTypen.komplexeZahl, 3, 4),
            MathematikTypResolver.elementTyp(raum),
        )
    }

    @Test fun `Methodensignatur bleibt auch einstellig ein Tupel`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = KomplexeZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )

        assertEquals(
            MathematikTypen.methode(
                argumente = listOf(MathematikTypen.reelleZahl),
                ziel = MathematikTypen.komplexeZahl,
            ),
            MathematikTypResolver.methodenTyp(methode),
        )
    }

    @Test fun `Typvisualisierung liefert Mathematiknotation und Mehrfachsegmente`() {
        assertEquals("ℝ", MathematikTypVisualResolver.beschreibe(MathematikTypen.reelleZahl).kurztext)
        assertEquals("ℕ₀", MathematikTypVisualResolver.beschreibe(MathematikTypen.nichtnegativeGanzeZahl).kurztext)

        val union = MathematikTypVisualResolver.beschreibe(
            TypAusdruck.Vereinigung(listOf(MathematikTypen.reelleZahl, MathematikTypen.menge)),
        )
        assertTrue(union.istMehrfachTyp)
        assertContains(union.kurztext, "∨")

        val methode = MathematikTypVisualResolver.beschreibe(
            MathematikTypen.methode(listOf(MathematikTypen.reelleZahl), MathematikTypen.komplexeZahl),
        )
        assertContains(methode.kurztext, "→")
    }
}
