package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VisualisierungsDimensionsAbleitungTest {
    @Test fun `skalare komplexe und Produktmengen leiten den Darstellungsraum ab`() {
        assertEquals(RaumDimension.R1, empfohleneRaumDimension(ReelleZahlen))
        assertEquals(RaumDimension.C, empfohleneRaumDimension(KomplexeZahlen))
        assertEquals(RaumDimension.R2, empfohleneRaumDimension(KartesischesProdukt(listOf(ReelleZahlen, ReelleZahlen))))
        assertEquals(RaumDimension.R3, empfohleneRaumDimension(Tupelraum(listOf(ReelleZahlen, ReelleZahlen, ReelleZahlen))))
        assertNull(empfohleneRaumDimension(Tupelraum(List(4) { ReelleZahlen })))
    }

    @Test fun `endliche Tupelmengen benötigen eine eindeutige Dimension`() {
        val zweidimensional = EndlicheMenge(
            setOf(
                Tupel(listOf(RationaleZahl.Null, RationaleZahl.Eins)),
                Tupel(listOf(RationaleZahl.Eins, RationaleZahl.Null)),
            ),
        )
        val gemischt = EndlicheMenge(setOf(RationaleZahl.Null, Tupel(listOf(RationaleZahl.Null, RationaleZahl.Eins))))

        assertEquals(RaumDimension.R2, empfohleneRaumDimension(zweidimensional))
        assertNull(empfohleneRaumDimension(gemischt))
    }

    @Test fun `endliche komplexe Mengen und komplexe Vektorraeume behalten ihre Semantik`() {
        val komplex = EndlicheMenge(
            setOf(KomplexeZahl(RationaleZahl.Eins, RationaleZahl.Eins)),
        )

        assertEquals(RaumDimension.C, empfohleneRaumDimension(komplex))
        assertEquals(
            RaumDimension.C,
            empfohleneRaumDimension(Vektorraum(VektorOrientierung.Spalte, 1, KomplexeZahlen)),
        )
        assertNull(empfohleneRaumDimension(Vektorraum(VektorOrientierung.Spalte, 2, KomplexeZahlen)))
    }

    @Test fun `vektorfoermige Matrizen leiten ihre Koordinatendimension ab`() {
        val zeilenMatrix = Matrix(
            listOf(listOf(RationaleZahl.Eins, RationaleZahl.von(2), RationaleZahl.von(3))),
        )
        val spaltenMatrix = Matrix(
            listOf(
                listOf(RationaleZahl.Eins),
                listOf(RationaleZahl.von(2)),
                listOf(RationaleZahl.von(3)),
            ),
        )

        assertEquals(RaumDimension.R3, empfohleneRaumDimension(EndlicheMenge(setOf(zeilenMatrix))))
        assertEquals(RaumDimension.R3, empfohleneRaumDimension(EndlicheMenge(setOf(spaltenMatrix))))
        assertEquals(RaumDimension.R3, empfohleneRaumDimension(Matrizenraum(1, 3, ReelleZahlen)))
        assertEquals(RaumDimension.R3, empfohleneRaumDimension(Matrizenraum(3, 1, ReelleZahlen)))
        assertNull(empfohleneRaumDimension(Matrizenraum(2, 2, ReelleZahlen)))
    }

    @Test fun `Automatik wirkt nur ohne manuellen Override`() {
        val automatisch = VisualisierungsKonfiguration().mitWirksamerDimension(ReelleZahlen)
        val manuell = VisualisierungsKonfiguration(
            dimension = RaumDimension.R3,
            dimensionsModus = DimensionsModus.Manuell,
        ).mitWirksamerDimension(ReelleZahlen)

        assertEquals(RaumDimension.R1, automatisch.dimension)
        assertEquals(RaumDimension.R3, manuell.dimension)
    }

    @Test fun `Legacy Dimension bleibt manuell und leere Konfiguration wird automatisch`() {
        val legacy = VisualisierungsKonfiguration.aus(mapOf("dimension" to KnotenEigenschaft.Text(RaumDimension.R3.name)))
        val leer = VisualisierungsKonfiguration.aus(emptyMap())

        assertEquals(DimensionsModus.Manuell, legacy.dimensionsModus)
        assertEquals(DimensionsModus.Automatisch, leer.dimensionsModus)
        assertEquals(leer, VisualisierungsKonfiguration.aus(leer.zuEigenschaften()))
    }
}
