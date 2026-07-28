package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.*

class GeometrieTest {
    private val r2 = EuklidischerRaum("E", 2)
    private fun punkt(name: String, x: Long, y: Long) = GeometriePunkt(
        name,
        r2,
        Tupel(listOf(RationaleZahl.von(x), RationaleZahl.von(y))),
    )

    @Test fun `Geometrische Objekte sind keine Mengen`() {
        val objekt: MathematischesObjekt = punkt("A", 1, 2)
        assertIs<GeometrischerAusdruck>(objekt)
        assertFalse(objekt is MengenAusdruck)
        assertIs<MengenAusdruck>(GeometrischeTrägermenge(objekt))
    }

    @Test fun `Punktkoordinaten sind Tupel und werden nur explizit zur Spalte`() {
        val koordinaten = Tupel(listOf(RationaleZahl.von(2), RationaleZahl.von(3)))
        assertEquals(listOf(RationaleZahl.von(2), RationaleZahl.von(3)), koordinaten.alsSpaltenVektor().werte)
        assertEquals(koordinaten, koordinaten.alsSpaltenVektor().alsTupel())
    }

    @Test fun `Umgekehrte Punktereihenfolge bezeichnet dieselbe Gerade`() {
        val a = punkt("A", 0, 0)
        val b = punkt("B", 2, 2)
        assertEquals(Wahrheitswert.Wahr, GeometrischeGleichheit(GeometrieGerade(a, b), GeometrieGerade(b, a)).entscheide().wahrheitswert)
    }

    @Test fun `Matrix transformiert Koordinatentupel über eine Spalte`() {
        val matrix = Matrix(listOf(
            listOf(RationaleZahl.von(2), RationaleZahl.Null),
            listOf(RationaleZahl.Null, RationaleZahl.von(3)),
        ))
        val bild = transformierePunkt(Tupel(listOf(RationaleZahl.von(4), RationaleZahl.von(5))), matrix)
        assertEquals(listOf(RationaleZahl.von(8), RationaleZahl.von(15)), bild.elemente)
    }

    @Test fun `Polygonstruktur enthält Ecken Kanten und Fläche`() {
        val polygon = GeometriePolygon(listOf(punkt("A", 0, 0), punkt("B", 2, 0), punkt("C", 0, 2)))
        val struktur = strukturVon(polygon)
        assertEquals(listOf(0, 1, 2), struktur.stufen.map { it.dimension })
        assertEquals(listOf(3, 3, 1), struktur.stufen.map { it.zellen.size })
    }

    @Test fun `Koordinatenbild und Trägermenge bleiben verschiedene Darstellungen`() {
        val p = punkt("A", 1, 2)
        val system = GeometrischesKoordinatensystem(r2)
        val träger: Any = GeometrischeTrägermenge(p)
        val koordinaten: Any = KoordinatenBild(p, system)
        assertFalse(träger == koordinaten)
    }

    @Test fun `Objekte verschiedener Räume dürfen nicht kombiniert werden`() {
        val a = punkt("A", 0, 0)
        val andererRaum = EuklidischerRaum("F", 2)
        val b = GeometriePunkt("B", andererRaum, Tupel(listOf(RationaleZahl.Null, RationaleZahl.Null)))
        assertFailsWith<IllegalArgumentException> { GeometrieGerade(a, b) }
    }
}
