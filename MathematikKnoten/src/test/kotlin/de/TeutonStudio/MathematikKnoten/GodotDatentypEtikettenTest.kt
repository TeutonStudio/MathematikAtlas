package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKnoten.godot.GodotDatentyp
import de.TeutonStudio.MathematikKnoten.godot.godotDatentypEtikettenFür
import de.TeutonStudio.MathematikRechenSystem.kern.Matrix
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.SpaltenVektor
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import de.TeutonStudio.MathematikRechenSystem.kern.ZeilenVektor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GodotDatentypEtikettenTest {
    @Test
    fun vector3ErkenntTupelUndBeideVektorOrientierungen() {
        val werte = listOf(z(1), z(2), z(3))
        val objekte = listOf(Tupel(werte), SpaltenVektor(werte), ZeilenVektor(werte))

        objekte.forEach { objekt ->
            val typen = typenFür(objekt)
            assertTrue(GodotDatentyp.Vector3 in typen)
            assertTrue(GodotDatentyp.Vector3i in typen)
        }
    }

    @Test
    fun nichtGanzzahligerVektorBekommtKeineIntegerVariante() {
        val typen = typenFür(Tupel(listOf(z(1), RationaleZahl.von(1, 2), z(3))))

        assertTrue(GodotDatentyp.Vector3 in typen)
        assertTrue(GodotDatentyp.Vector3i !in typen)
    }

    @Test
    fun vierTupelZeigtAlleStrukturellPassendenVierkomponententypen() {
        val typen = typenFür(Tupel(listOf(z(1), z(2), z(3), z(4))))

        assertTrue(GodotDatentyp.Vector4 in typen)
        assertTrue(GodotDatentyp.Vector4i in typen)
        assertTrue(GodotDatentyp.Plane in typen)
        assertTrue(GodotDatentyp.Quaternion in typen)
        assertTrue(GodotDatentyp.Color in typen)
    }

    @Test
    fun rectUndAabbFolgenDerGodotKomponentenstruktur() {
        val rect = Tupel(listOf(
            Tupel(listOf(z(1), z(2))),
            ZeilenVektor(listOf(z(3), z(4))),
        ))
        val aabb = Tupel(listOf(
            SpaltenVektor(listOf(z(1), z(2), z(3))),
            Tupel(listOf(z(4), z(5), z(6))),
        ))

        assertEquals(
            setOf(GodotDatentyp.Rect2, GodotDatentyp.Rect2i),
            typenFür(rect),
        )
        assertEquals(setOf(GodotDatentyp.AABB), typenFür(aabb))
    }

    @Test
    fun transformUndBasisWerdenAusMatrixformenAbgeleitet() {
        assertEquals(
            setOf(GodotDatentyp.Transform2D),
            typenFür(matrix(2, 3)),
        )
        assertEquals(
            setOf(GodotDatentyp.Basis),
            typenFür(matrix(3, 3)),
        )
        assertEquals(
            setOf(GodotDatentyp.Transform3D),
            typenFür(matrix(3, 4)),
        )
        assertEquals(
            setOf(GodotDatentyp.Projection),
            typenFür(matrix(4, 4)),
        )
    }

    @Test
    fun transformformenWerdenAuchAlsTupelVonSpaltenErkannt() {
        val transform2D = Tupel(List(3) { Tupel(listOf(z(1), z(2))) })
        val basis = Tupel(List(3) { SpaltenVektor(listOf(z(1), z(2), z(3))) })
        val transform3D = Tupel(List(4) { ZeilenVektor(listOf(z(1), z(2), z(3))) })
        val projection = Tupel(List(4) { Tupel(listOf(z(1), z(2), z(3), z(4))) })

        assertEquals(setOf(GodotDatentyp.Transform2D), typenFür(transform2D))
        assertEquals(setOf(GodotDatentyp.Basis), typenFür(basis))
        assertEquals(setOf(GodotDatentyp.Transform3D), typenFür(transform3D))
        assertEquals(setOf(GodotDatentyp.Projection), typenFür(projection))
    }

    private fun typenFür(objekt: de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt): Set<GodotDatentyp> =
        godotDatentypEtikettenFür(objekt).map { it.typ }.toSet()

    private fun matrix(zeilen: Int, spalten: Int): Matrix = Matrix(
        List(zeilen) { zeile ->
            List(spalten) { spalte -> z((zeile * spalten + spalte + 1).toLong()) }
        },
    )

    private fun z(wert: Long) = RationaleZahl.von(wert)
}
