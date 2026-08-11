package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals

class GodotStrukturEtikettenTest {
    private fun variablen(anzahl: Int, präfix: String = "x") =
        List(anzahl) { index -> Variable("$präfix${index + 1}") }

    private fun ganze(anzahl: Int) =
        List(anzahl) { index -> RationaleZahl.von((index + 1).toLong()) }

    @Test
    fun `Vector2 erkennt Tupel Zeile Spalte und eindimensionale Matrix`() {
        val werte = variablen(2)
        val kandidaten: List<MathematischesObjekt> = listOf(
            Tupel(werte),
            ZeilenVektor(werte),
            SpaltenVektor(werte),
            Matrix(listOf(werte)),
            Matrix(werte.map { wert -> listOf(wert) }),
        )

        kandidaten.forEach { assertEquals(GodotStrukturTyp.VECTOR2, it.godotStrukturTyp()) }
    }

    @Test
    fun `Vector3 erkennt dreidimensionales Tupel Zeile und Spalte`() {
        val werte = variablen(3)
        listOf(
            Tupel(werte),
            ZeilenVektor(werte),
            SpaltenVektor(werte),
        ).forEach { assertEquals(GodotStrukturTyp.VECTOR3, it.godotStrukturTyp()) }
    }

    @Test
    fun `Vector4 erkennt vierdimensionale Koordinatenstruktur`() {
        val werte = variablen(4)
        assertEquals(GodotStrukturTyp.VECTOR4, Tupel(werte).godotStrukturTyp())
        assertEquals(GodotStrukturTyp.VECTOR4, ZeilenVektor(werte).godotStrukturTyp())
        assertEquals(GodotStrukturTyp.VECTOR4, SpaltenVektor(werte).godotStrukturTyp())
    }

    @Test
    fun `ganzzahlige Vektoren erhalten Integer Godot Typen`() {
        assertEquals(GodotStrukturTyp.VECTOR2I, Tupel(ganze(2)).godotStrukturTyp())
        assertEquals(GodotStrukturTyp.VECTOR3I, ZeilenVektor(ganze(3)).godotStrukturTyp())
        assertEquals(GodotStrukturTyp.VECTOR4I, SpaltenVektor(ganze(4)).godotStrukturTyp())
    }

    @Test
    fun `Rect2 und Rect2i werden als Paar zweidimensionaler Vektoren erkannt`() {
        val rect = Tupel(listOf(Tupel(variablen(2, "p")), Tupel(variablen(2, "s"))))
        val rectI = Tupel(listOf(Tupel(ganze(2)), Tupel(ganze(2))))

        assertEquals(GodotStrukturTyp.RECT2, rect.godotStrukturTyp())
        assertEquals(GodotStrukturTyp.RECT2I, rectI.godotStrukturTyp())
    }

    @Test
    fun `AABB wird als Paar dreidimensionaler Vektoren erkannt`() {
        val aabb = Tupel(listOf(Tupel(variablen(3, "p")), Tupel(variablen(3, "s"))))
        assertEquals(GodotStrukturTyp.AABB, aabb.godotStrukturTyp())
    }

    @Test
    fun `Plane wird als Normalenvektor und Abstand erkannt`() {
        val plane = Tupel(listOf(Tupel(variablen(3, "n")), Variable("d")))
        assertEquals(GodotStrukturTyp.PLANE, plane.godotStrukturTyp())
    }

    @Test
    fun `Quaternion wird bei semantischem Hinweis aus vier Koordinaten erkannt`() {
        val quaternion = Tupel(variablen(4, "q"))
        assertEquals(GodotStrukturTyp.QUATERNION, quaternion.godotStrukturTyp("Quaternion Rotation"))
    }

    @Test
    fun `Transform2D erkennt homogene affine Matrix und Godot Komponentenform`() {
        val affin = Matrix(
            listOf(
                listOf(Variable("a"), Variable("b"), Variable("tx")),
                listOf(Variable("c"), Variable("d"), Variable("ty")),
                listOf(RationaleZahl.Null, RationaleZahl.Null, RationaleZahl.Eins),
            ),
        )
        val komponenten = Tupel(
            listOf(
                Tupel(variablen(2, "x")),
                Tupel(variablen(2, "y")),
                Tupel(variablen(2, "o")),
            ),
        )

        assertEquals(GodotStrukturTyp.TRANSFORM2D, affin.godotStrukturTyp())
        assertEquals(GodotStrukturTyp.TRANSFORM2D, komponenten.godotStrukturTyp())
    }

    @Test
    fun `Basis erkennt allgemeine drei mal drei Matrix`() {
        val basis = Matrix(List(3) { zeile -> List(3) { spalte -> Variable("b${zeile}${spalte}") } })
        assertEquals(GodotStrukturTyp.BASIS, basis.godotStrukturTyp())
    }

    @Test
    fun `Transform3D erkennt homogene affine Matrix und Basis Ursprung Tupel`() {
        val affin = Matrix(
            listOf(
                listOf(Variable("a"), Variable("b"), Variable("c"), Variable("tx")),
                listOf(Variable("d"), Variable("e"), Variable("f"), Variable("ty")),
                listOf(Variable("g"), Variable("h"), Variable("i"), Variable("tz")),
                listOf(RationaleZahl.Null, RationaleZahl.Null, RationaleZahl.Null, RationaleZahl.Eins),
            ),
        )
        val basis = Matrix(List(3) { zeile -> List(3) { spalte -> Variable("m${zeile}${spalte}") } })
        val komponenten = Tupel(listOf(basis, Tupel(variablen(3, "o"))))

        assertEquals(GodotStrukturTyp.TRANSFORM3D, affin.godotStrukturTyp())
        assertEquals(GodotStrukturTyp.TRANSFORM3D, komponenten.godotStrukturTyp())
    }

    @Test
    fun `Projection erkennt allgemeine vier mal vier Matrix`() {
        val projektion = Matrix(List(4) { zeile -> List(4) { spalte -> Variable("p${zeile}${spalte}") } })
        assertEquals(GodotStrukturTyp.PROJECTION, projektion.godotStrukturTyp())
    }

    @Test
    fun `Katalog enthält alle Orchestrator Struct Pin Typen`() {
        assertEquals(
            setOf(
                "Vector2", "Vector2i", "Vector3", "Vector3i", "Vector4", "Vector4i",
                "Rect2", "Rect2i", "Transform2D", "Transform3D", "Plane", "Quaternion",
                "Projection", "AABB", "Basis",
            ),
            GodotStrukturTyp.entries.mapTo(mutableSetOf(), GodotStrukturTyp::godotName),
        )
    }
}