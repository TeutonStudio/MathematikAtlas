package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.TypAusdruck
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MethodenAnforderungTest {
    @Test
    fun `ergebnisanforderung unterscheidet konkrete methoden semantisch`() {
        val x = Variable("x")
        val zahlMethode = Methode(
            name = "f",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to x),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )
        val mengenMethode = Methode(
            name = "A",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to MengenParameter("A_x")),
            zielMengen = mapOf("wert" to BenannteMenge("M", "\\mathfrak{M}")),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        assertNull(MethodenAnforderung.ErgebnisArt("mathematik.zahl").prüfe(zahlMethode))
        assertNull(MethodenAnforderung.ErgebnisArt("mathematik.menge").prüfe(mengenMethode))
        assertNotNull(MethodenAnforderung.ErgebnisArt("mathematik.zahl").prüfe(mengenMethode))
    }

    @Test
    fun `ergebnistyp nutzt semantische zahlbereichshierarchie`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        assertNull(
            MethodenAnforderung.ErgebnisTyp(
                TypAusdruck.Atom(MathematischeTypen.Komplex),
            ).prüfe(methode),
        )
        assertNotNull(
            MethodenAnforderung.ErgebnisTyp(
                TypAusdruck.Atom(MathematischeTypen.Ganz),
            ).prüfe(methode),
        )
    }

    @Test
    fun `methodensignatur bleibt auch bei einem argument ein tupeltyp`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        val typ = assertNotNull(methode.typAusdruck as? TypAusdruck.Parameterisiert)
        assertEquals(MathematischeTypen.Methode, typ.konstruktor)
        val argumentTyp = assertNotNull(typ.argumente.firstOrNull() as? TypAusdruck.Parameterisiert)
        assertEquals(MathematischeTypen.Tupel, argumentTyp.konstruktor)
        assertEquals(1, argumentTyp.argumente.size)
    }

    @Test
    fun `mathematische typvisualisierung nutzt kanonische glyphen`() {
        val visual = TypAusdruck.Parameterisiert(
            MathematischeTypen.Methode,
            listOf(
                TypAusdruck.Parameterisiert(
                    MathematischeTypen.Tupel,
                    listOf(TypAusdruck.Atom(MathematischeTypen.Reell)),
                ),
                TypAusdruck.Atom(MathematischeTypen.Komplex),
            ),
        ).mathematischeTypVisualisierung()

        assertEquals("(ℝ) → ℂ", visual.kurzLabel)
    }

    @Test
    fun `zahlenfunktion akzeptiert mehrere numerische Argumentraeume und effektiven Bereich`() {
        val x = Variable("x")
        val y = Variable("y")
        val diagonale = DefinierteMenge(
            variablen = listOf(
                GebundeneMengenVariable(x, ReelleZahlen),
                GebundeneMengenVariable(y, ReelleZahlen),
            ),
            bedingung = Gleichheit(x, y),
        )
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = addition(x, y),
            zielMenge = FundamentalerZahlbereich.QUATERNION.alsMenge(),
            werteVorräte = mapOf(
                x.name to reellesIntervall(RationaleZahl.Null, false, RationaleZahl.Eins, false),
                y.name to ReelleZahlen,
            ),
            effektiverWerteVorrat = diagonale,
        )

        assertNull(MethodenAnforderung.Zahlenfunktion.prüfe(methode))
        assertTrue(methode.methodenSignatur().effektiverWerteVorrat === diagonale)
    }

    @Test
    fun `zahlenfunktion lehnt nichtnumerischen Argumentraum positionsgenau ab`() {
        val v = AllgemeinerParameter("v")
        val methode = Methode(
            name = "f",
            parameter = listOf(v),
            vorschrift = RationaleZahl.Null,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(
                v.name to Vektorraum(VektorOrientierung.Spalte, 2, ReelleZahlen),
            ),
        )

        val diagnose = assertNotNull(MethodenAnforderung.Zahlenfunktion.prüfe(methode))
        assertTrue(diagnose.contains("1. Argument 'v'"))
        assertTrue(diagnose.contains("\\mathbb H"))
    }

    @Test
    fun `zahlenfunktion lehnt nichtnumerische Zielmenge ab`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = BenannteMenge("Farben", "\\mathcal F"),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        val diagnose = assertNotNull(MethodenAnforderung.Zahlenfunktion.prüfe(methode))
        assertTrue(diagnose.contains("Zielmenge"))
        assertTrue(diagnose.contains("\\mathcal F"))
    }

    @Test
    fun `zahlenmengenerkennung deckt strukturierte Mengenkonstruktionen ab`() {
        val x = Variable("x")
        val unbekannt = BenannteMenge("Farben", "\\mathcal F")
        val beschraenkt = BeschraenkteZahlmenge(
            traeger = FundamentalerZahlbereich.REELL,
            untereGrenze = RationaleZahl.Null,
            untereGrenzeEnthalten = true,
            obereGrenze = RationaleZahl.Eins,
            obereGrenzeEnthalten = true,
        )
        val definiert = DefinierteMenge(
            variablen = listOf(GebundeneMengenVariable(x, ReelleZahlen)),
            bedingung = Gleichheit(x, x),
        )
        val filter = Methode(
            name = "P",
            parameter = listOf(x),
            vorschrift = Gleichheit(x, x),
            zielMenge = WahrheitsMenge,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        assertTrue(beschraenkt.istZahlenmenge())
        assertTrue(definiert.istZahlenmenge())
        assertTrue(GefilterteMenge(ReelleZahlen, filter).istZahlenmenge())
        assertTrue(Vereinigung(listOf(ReelleZahlen, EndlicheMenge(setOf(RationaleZahl.Eins)))).istZahlenmenge())
        assertTrue(Schnitt(listOf(unbekannt, ReelleZahlen)).istZahlenmenge())
        assertTrue(MengenDifferenz(ReelleZahlen, unbekannt).istZahlenmenge())
        assertTrue(!unbekannt.istZahlenmenge())
    }
}
