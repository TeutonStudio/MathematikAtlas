package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class GeometrieKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test fun `Geometrieanschlüsse sind keine Mengenanschlüsse`() {
        val typen = AnschlussArtRegister(MathematikAnschlussArten.alle)
        assertTrue(typen.istUnterart(GeometrieAnschlussArten.Punkt.id, GeometrieAnschlussArten.Objekt.id))
        assertFalse(typen.istUnterart(GeometrieAnschlussArten.Punkt.id, MathematikAnschlussArten.Menge.id))
    }

    @Test fun `Alle Geometrievorlagen besitzen registrierte Auswerter`() {
        assertTrue(GeometrieKnotenVorlagen.alle.all { register.finde(it.art) != null })
    }

    @Test fun `Punkt aus Koordinaten bewahrt das Tupel`() {
        val vorlage = GeometrieKnotenVorlagen.PunktAusKoordinaten.erzeuge(GraphPunkt.Zero)
        val raum = EuklidischerRaum("E", 2)
        val system = GeometrischesKoordinatensystem(raum)
        val tupel = Tupel(listOf(RationaleZahl.von(1), RationaleZahl.von(2)))
        val ergebnis = register.finde(vorlage.art)!!.auswerten(KnotenAuswertungsKontext(
            vorlage,
            mapOf("system" to BedingterWert(system), "koordinaten" to BedingterWert(tupel)),
            RechenKontext(),
        ))
        val punkt = assertIs<GeometriePunkt>(ergebnis.ausgaben.getValue("punkt").objekt)
        assertEquals(tupel, punkt.koordinaten)
        assertEquals(raum, punkt.raum)
    }

    @Test fun `Lineare Punkttransformation liefert wieder ein Tupel`() {
        val knoten = GeometrieKnotenVorlagen.LinearePunkttransformation.erzeuge(GraphPunkt.Zero)
        val matrix = Matrix(listOf(
            listOf(RationaleZahl.von(1), RationaleZahl.von(2)),
            listOf(RationaleZahl.von(3), RationaleZahl.von(4)),
        ))
        val tupel = Tupel(listOf(RationaleZahl.von(5), RationaleZahl.von(6)))
        val ergebnis = register.finde(knoten.art)!!.auswerten(KnotenAuswertungsKontext(
            knoten,
            mapOf("punkt" to BedingterWert(tupel), "matrix" to BedingterWert(matrix)),
            RechenKontext(),
        ))
        assertEquals(
            listOf(RationaleZahl.von(17), RationaleZahl.von(39)),
            assertIs<Tupel>(ergebnis.ausgaben.getValue("bild").objekt).elemente,
        )
    }

    @Test fun `Geometrievisualisierung reicht das Objekt unverändert durch`() {
        val knoten = GeometrieKnotenVorlagen.Visualisierung.erzeuge(GraphPunkt.Zero)
        val raum = EuklidischerRaum("E", 1)
        val punkt = GeometriePunkt("A", raum, Tupel(listOf(RationaleZahl.von(2))))
        val eingang = BedingterWert(punkt)
        val ergebnis = register.finde(knoten.art)!!.auswerten(KnotenAuswertungsKontext(
            knoten, mapOf("objekt" to eingang), RechenKontext(),
        ))
        assertSame(punkt, ergebnis.ausgaben.getValue("objekt").objekt)
    }
}
