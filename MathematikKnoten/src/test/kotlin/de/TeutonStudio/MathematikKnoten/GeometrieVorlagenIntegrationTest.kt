package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import kotlin.test.*

class GeometrieVorlagenIntegrationTest {
    @Test
    fun `alle Geometrievorlagen besitzen eindeutige Arten und Auswerter`() {
        val arten = GeometrieKnotenVorlagen.alle.map { it.art }
        assertEquals(arten.size, arten.toSet().size, "Geometrie-Knotenarten müssen eindeutig sein.")

        val register = GesamterMathematikAuswerter.erzeugeRegister()
        assertEquals(emptyList(), arten.filter { register.finde(it) == null }, "Jede Geometrievorlage benötigt einen Auswerter.")
    }

    @Test
    fun `normales Tupel kann einen Punkt speisen`() {
        val tupel = MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero)
        val punkt = GeometrieKnotenVorlagen.PunktAusKoordinaten.erzeuge(GraphPunkt(300f, 0f))
        assertVerbindbar(tupel, "tupel", punkt, "koordinaten")
    }

    @Test
    fun `normales Tupel kann lineare und affine Punkttransformationen speisen`() {
        val tupel = MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero)
        val linear = GeometrieKnotenVorlagen.LinearePunkttransformation.erzeuge(GraphPunkt(300f, 0f))
        val affin = GeometrieKnotenVorlagen.AffinePunkttransformation.erzeuge(GraphPunkt(600f, 0f))
        val affineAbbildung = GeometrieKnotenVorlagen.AffineTransformation.erzeuge(GraphPunkt(900f, 0f))

        assertVerbindbar(tupel, "tupel", linear, "punkt")
        assertVerbindbar(tupel, "tupel", affin, "punkt")
        assertVerbindbar(tupel, "tupel", affin, "translation")
        assertVerbindbar(tupel, "tupel", affineAbbildung, "translation")
    }

    private fun assertVerbindbar(quelle: KnotenDaten, ausgang: String, ziel: KnotenDaten, eingang: String) {
        val karte = KartenDaten(name = "Geometrie-Integration", knoten = listOf(quelle, ziel))
        val von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single { it.name == ausgang }.id)
        val zu = AnschlussVerweis(ziel.id, ziel.anschlüsse.single { it.name == eingang }.id)
        val prüfung = GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle)).prüfe(karte, von, zu)
        assertIs<VerbindungsPrüfung.Erlaubt>(prüfung)
    }
}
