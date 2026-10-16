package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKnoten.visualisierung.modell.SamplingKonfiguration
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.VisualisierungsKonfiguration
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.alsVorschau
import kotlin.test.Test
import kotlin.test.assertEquals

class VisualisierungsVorschauTest {
    @Test
    fun `Vorschau begrenzt hohe Qualitaet ohne die persistierte Konfiguration zu veraendern`() {
        val original = VisualisierungsKonfiguration(
            sampling = SamplingKonfiguration(
                auflösung1D = 2_000,
                auflösung2D = 240,
                auflösung3D = 64,
                toleranz = 0.01,
                maximalesRasterBudget = 250_000,
                maximaleOrbitSchritte = 10_000,
            ),
        )

        val vorschau = original.alsVorschau()

        assertEquals(48, vorschau.sampling.auflösung1D)
        assertEquals(14, vorschau.sampling.auflösung2D)
        assertEquals(5, vorschau.sampling.auflösung3D)
        assertEquals(192, vorschau.sampling.maximalesRasterBudget)
        assertEquals(32, vorschau.sampling.maximaleOrbitSchritte)
        assertEquals(250_000, original.sampling.maximalesRasterBudget)
    }

    @Test
    fun `Vorschau erhoeht kleine benutzerdefinierte Werte nicht`() {
        val original = VisualisierungsKonfiguration(
            sampling = SamplingKonfiguration(
                auflösung1D = 40,
                auflösung2D = 12,
                auflösung3D = 5,
                toleranz = 0.1,
                maximalesRasterBudget = 128,
                maximaleOrbitSchritte = 32,
            ),
        )

        assertEquals(original.copy(sampling = original.sampling.copy(vorschau = true)), original.alsVorschau())
    }

    @Test
    fun `historische Detailqualitaet bleibt beim Laden erhalten`() {
        val historisch = VisualisierungsKonfiguration(
            sampling = SamplingKonfiguration(72, 22, 0.08, maximalesRasterBudget = 250_000),
        )

        val geladen = VisualisierungsKonfiguration.aus(historisch.zuEigenschaften())

        assertEquals(historisch.sampling.maximalesRasterBudget, geladen.sampling.maximalesRasterBudget)
    }

    @Test
    fun `neue Visualisierungen verwenden sechstausend Auswertungen fuer die Detailansicht`() {
        assertEquals(6_000, VisualisierungsKonfiguration().sampling.maximalesRasterBudget)
    }
}
