package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class TupelAbbildungsObjektTest {
    @Test
    fun `Tupel implementiert endlichen Abbildungsvertrag mit mathematisch einsbasierten Indizes`() {
        val eins = RationaleZahl.von(11)
        val zwei = RationaleZahl.von(22)
        val tupel = Tupel(listOf(eins, zwei))
        val abbildung = assertIs<EndlichIndexiertesObjekt>(tupel)

        assertEquals(2, abbildung.anzahl)
        assertEquals(
            EndlicheMenge(setOf(RationaleZahl.von(1), RationaleZahl.von(2))),
            abbildung.indexMenge,
        )
        assertEquals(eins, abbildung.wertAn(RationaleZahl.von(1)))
        assertEquals(zwei, abbildung.wertAn(RationaleZahl.von(2)))
        assertEquals(eins, abbildung.wertAnPosition(0))
        assertEquals(zwei, abbildung.wertAnPosition(1))
    }

    @Test
    fun `leeres Tupel ist eindeutige leere Indexabbildung und kein Methodenwert`() {
        val tupel = Tupel(emptyList())

        assertIs<EndlichIndexiertesObjekt>(tupel)
        assertEquals(0, tupel.anzahl)
        assertEquals(LeereMenge, tupel.indexMenge)
        assertFailsWith<IllegalArgumentException> { tupel.wertAn(RationaleZahl.von(1)) }
        assertFailsWith<IndexOutOfBoundsException> { tupel.wertAnPosition(0) }
    }

    @Test
    fun `mathematischer Tupelindex null wird nicht mit technischer Position null verwechselt`() {
        val tupel = Tupel(listOf(RationaleZahl.Eins))

        assertEquals(RationaleZahl.Eins, tupel.wertAnPosition(0))
        assertFailsWith<IllegalArgumentException> { tupel.wertAn(RationaleZahl.Null) }
    }
}
