package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.MathematikKnoten.historischeMengenKnotenArten
import de.TeutonStudio.MathematikKnoten.historischeSkalarproduktArten
import de.TeutonStudio.MathematikKnoten.historischeZahlenRechnerArten
import kotlin.test.Test
import kotlin.test.assertTrue
import java.io.File
import org.json.JSONObject

class StandardKartenIntegrationTest {
    @Test
    fun `alle ausgelieferten Standardkarten sind im Installer vollstaendig unterstuetzt`() {
        val bekannteArten = bekannteStandardKartenKnotenArten()
        val basis = File("src/main/assets/de/TeutonStudio/MathematikAtlas/standardkarten")
        val entries = JSONObject(File(basis, "manifest.json").readText()).getJSONArray("entries")
        for (i in 0 until entries.length()) {
            val entry = entries.getJSONObject(i)
            val erforderlich = entry.getJSONArray("requiredNodeTypes")
            val fehlend = (0 until erforderlich.length()).map(erforderlich::getString).toSet() - bekannteArten
            assertTrue(fehlend.isEmpty(), "${entry.getString("sourceId")}: nicht unterstützte Typen $fehlend")
        }
    }
    @Test
    fun `Standardkarten akzeptieren lade-kompatible historische Knotenarten`() {
        val bekannteArten = bekannteStandardKartenKnotenArten()

        assertTrue(bekannteArten.containsAll(historischeZahlenRechnerArten))
        assertTrue(bekannteArten.containsAll(historischeMengenKnotenArten))
        assertTrue(bekannteArten.containsAll(historischeSkalarproduktArten))
        assertTrue("mathematik.einheitsSpalte" in bekannteArten)
        assertTrue("mathematik.einheitsZeile" in bekannteArten)
    }

    @Test
    fun `aktuelle Standardkarten-Historientypen werden nicht mehr verworfen`() {
        val bekannteArten = bekannteStandardKartenKnotenArten()

        listOf(
            "mathematik.addition",
            "mathematik.multiplikation",
            "mathematik.potenz",
            "mathematik.reelleZahlen",
        ).forEach { art ->
            assertTrue(art in bekannteArten, "Standardkarten-Knotentyp $art muss lade-kompatibel sein.")
        }
    }
}
