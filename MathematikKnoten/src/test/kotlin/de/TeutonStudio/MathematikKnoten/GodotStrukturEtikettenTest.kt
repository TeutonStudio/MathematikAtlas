package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
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

        kandidaten.forEach { assertEquals(GodotVariantTyp.VECTOR2, it.godotStrukturVariantTyp()) }
    }

    @Test
    fun `Vector3 erkennt dreidimensionales Tupel Zeile und Spalte`() {
        val werte = variablen(3)
        listOf(
            Tupel(werte),
            ZeilenVektor(werte),
            SpaltenVektor(werte),
        ).forEach { assertEquals(GodotVariantTyp.VECTOR3, it.godotStrukturVariantTyp()) }
    }

    @Test
    fun `Vector4 erkennt vierdimensionale Koordinatenstruktur`() {
        val werte = variablen(4)
        assertEquals(GodotVariantTyp.VECTOR4, Tupel(werte).godotStrukturVariantTyp())
        assertEquals(GodotVariantTyp.VECTOR4, ZeilenVektor(werte).godotStrukturVariantTyp())
        assertEquals(GodotVariantTyp.VECTOR4, SpaltenVektor(werte).godotStrukturVariantTyp())
    }

    @Test
    fun `ganzzahlige Vektoren erhalten Integer Godot Typen`() {
        assertEquals(GodotVariantTyp.VECTOR2I, Tupel(ganze(2)).godotStrukturVariantTyp())
        assertEquals(GodotVariantTyp.VECTOR3I, ZeilenVektor(ganze(3)).godotStrukturVariantTyp())
        assertEquals(GodotVariantTyp.VECTOR4I, SpaltenVektor(ganze(4)).godotStrukturVariantTyp())
    }

    @Test
    fun `Color und Quaternion bleiben semantisch von Vector4 getrennt`() {
        val vierer = Tupel(variablen(4, "q"))

        assertEquals(GodotVariantTyp.VECTOR4, vierer.godotStrukturVariantTyp())
        assertEquals(GodotVariantTyp.COLOR, vierer.godotStrukturVariantTyp("Farbe RGBA"))
        assertEquals(GodotVariantTyp.QUATERNION, vierer.godotStrukturVariantTyp("Quaternion Rotation"))
    }

    @Test
    fun `Rect2 und Rect2i werden als Paar zweidimensionaler Vektoren erkannt`() {
        val rect = Tupel(listOf(Tupel(variablen(2, "p")), Tupel(variablen(2, "s"))))
        val rectI = Tupel(listOf(Tupel(ganze(2)), Tupel(ganze(2))))

        assertEquals(GodotVariantTyp.RECT2, rect.godotStrukturVariantTyp())
        assertEquals(GodotVariantTyp.RECT2I, rectI.godotStrukturVariantTyp())
    }

    @Test
    fun `AABB wird als Paar dreidimensionaler Vektoren erkannt`() {
        val aabb = Tupel(listOf(Tupel(variablen(3, "p")), Tupel(variablen(3, "s"))))
        assertEquals(GodotVariantTyp.AABB, aabb.godotStrukturVariantTyp())
    }

    @Test
    fun `Plane wird als Normalenvektor und Abstand erkannt`() {
        val plane = Tupel(listOf(Tupel(variablen(3, "n")), Variable("d")))
        assertEquals(GodotVariantTyp.PLANE, plane.godotStrukturVariantTyp())
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

        assertEquals(GodotVariantTyp.TRANSFORM2D, affin.godotStrukturVariantTyp())
        assertEquals(GodotVariantTyp.TRANSFORM2D, komponenten.godotStrukturVariantTyp())
    }

    @Test
    fun `Basis erkennt allgemeine drei mal drei Matrix`() {
        val basis = Matrix(List(3) { zeile -> List(3) { spalte -> Variable("b${zeile}${spalte}") } })
        assertEquals(GodotVariantTyp.BASIS, basis.godotStrukturVariantTyp())
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

        assertEquals(GodotVariantTyp.TRANSFORM3D, affin.godotStrukturVariantTyp())
        assertEquals(GodotVariantTyp.TRANSFORM3D, komponenten.godotStrukturVariantTyp())
    }

    @Test
    fun `Projection erkennt allgemeine vier mal vier Matrix`() {
        val projektion = Matrix(List(4) { zeile -> List(4) { spalte -> Variable("p${zeile}${spalte}") } })
        assertEquals(GodotVariantTyp.PROJECTION, projektion.godotStrukturVariantTyp())
    }

    @Test
    fun `atomare Atlas Ausgaben erhalten bool int und float Etiketten`() {
        val ergebnis = KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "aussage" to BedingterWert(WahrheitsKonstante(true)),
                "ganz" to BedingterWert(RationaleZahl.von(42)),
                "bruch" to BedingterWert(RationaleZahl.von(1, 2)),
            ),
        )
        val typen = ergebnis.godotVariantEtiketten(KnotenDaten(art = "test", name = "Atomar"))
            .associate { etikett -> etikett.ausgangName to etikett.typ }

        assertEquals(GodotVariantTyp.BOOL, typen["aussage"])
        assertEquals(GodotVariantTyp.INT, typen["ganz"])
        assertEquals(GodotVariantTyp.FLOAT, typen["bruch"])
    }

    @Test
    fun `langes homogenes Zahlentupel wird PackedInt64Array`() {
        val ergebnis = KnotenAuswertungsErgebnis(
            mapOf("werte" to BedingterWert(Tupel(ganze(5)))),
        )
        val etikett = ergebnis.godotVariantEtiketten(KnotenDaten(art = "test", name = "Werte")).single()

        assertEquals(GodotVariantTyp.PACKED_INT64_ARRAY, etikett.typ)
    }

    @Test
    fun `langes bool Tupel behält Array mit Elementtyp`() {
        val aussagen = List(5) { index -> AussagenParameter("p$index") }
        val ergebnis = KnotenAuswertungsErgebnis(
            mapOf("werte" to BedingterWert(Tupel(aussagen))),
        )
        val etikett = ergebnis.godotVariantEtiketten(KnotenDaten(art = "test", name = "Werte")).single()

        assertEquals(GodotVariantTyp.ARRAY, etikett.typ)
        assertEquals(GodotVariantTyp.BOOL, etikett.elementTyp)
    }

    @Test
    fun `Katalog entspricht allen sichtbaren eingebauten Godot Variant Typicons`() {
        assertEquals(
            setOf(
                "Variant",
                "bool", "int", "float", "String",
                "Vector2", "Vector2i", "Rect2", "Rect2i", "Vector3", "Vector3i",
                "Transform2D", "Vector4", "Vector4i", "Plane", "Quaternion", "AABB",
                "Basis", "Transform3D", "Projection",
                "Color", "StringName", "NodePath", "RID", "Object", "Callable", "Signal",
                "Dictionary", "Array",
                "PackedByteArray", "PackedInt32Array", "PackedInt64Array",
                "PackedFloat32Array", "PackedFloat64Array", "PackedStringArray",
                "PackedVector2Array", "PackedVector3Array", "PackedColorArray", "PackedVector4Array",
            ),
            GodotVariantTyp.entries.mapTo(mutableSetOf(), GodotVariantTyp::godotName),
        )
        assertEquals(39, GodotVariantTyp.entries.size)
    }
}