package de.TeutonStudio.KnotenKartenVerwalter

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.zuKartenGrenzenDaten
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KartenGrenzenTest {
    @Test
    fun leereKnotenlisteHatKeineGrenzen() {
        assertNull(emptyList<KnotenDaten>().zuKartenGrenzenDaten())
    }

    @Test
    fun grenzenUmfassenAlleKnotenMitPadding() {
        val grenzen = listOf(
            KnotenDaten(
                id = "a",
                name = "A",
                position = Offset(10f, 20f),
                fläche = Offset(100f, 50f),
            ),
            KnotenDaten(
                id = "b",
                name = "B",
                position = Offset(-30f, 80f),
                fläche = Offset(40f, 20f),
            ),
        ).zuKartenGrenzenDaten(padding = 5f)

        requireNotNull(grenzen)
        assertEquals(-35f, grenzen.links, 0.001f)
        assertEquals(15f, grenzen.oben, 0.001f)
        assertEquals(115f, grenzen.rechts, 0.001f)
        assertEquals(105f, grenzen.unten, 0.001f)
        assertEquals(150f, grenzen.breite, 0.001f)
        assertEquals(90f, grenzen.hoehe, 0.001f)
    }
}
