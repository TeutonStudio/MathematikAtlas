package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenVerweis
import de.TeutonStudio.MathematikAtlas.speicher.KartenJson
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikKartenAdapter.KartenQuelle
import de.TeutonStudio.MathematikKnoten.GesamterMathematikAuswerter
import de.TeutonStudio.MathematikKnoten.synchronisiereRestriktionsAnschlüsse
import org.json.JSONObject
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class StandardKartenAuswertungTest {
    @Test
    fun `alle produktiven Standardkarten laufen durch den echten Auswerter`() {
        val assetRoot = findeAssetRoot()
        val manifest = JSONObject(File(assetRoot, "manifest.json").readText())
        val einträge = manifest.getJSONArray("entries")
        val karten = List(einträge.length()) { index ->
            val pfad = einträge.getJSONObject(index).getString("path")
            KartenJson.lese(File(assetRoot, pfad).readText())
        }
        val nachVerweis = karten.associateBy { Karte -> KartenVerweis(Karte.id, Karte.version) }
        val auswerter = KartenAuswerter(
            register = GesamterMathematikAuswerter.erzeugeRegister(),
            kartenQuelle = KartenQuelle(nachVerweis::get),
            nichtAuswertbareKnotenArten = KartenWerkzeugVorlagen.nichtAuswertbareArten,
        )

        karten.forEach { karte ->
            auswerter.leereCache()
            val ersterLauf = auswerter.auswerten(karte)
            val synchronisiert = synchronisiereRestriktionsAnschlüsse(karte, ersterLauf)
            val ergebnis = if (synchronisiert == karte) {
                ersterLauf
            } else {
                auswerter.leereCache()
                auswerter.auswerten(synchronisiert)
            }
            assertTrue(
                ergebnis.fehler.isEmpty(),
                "Standardkarte '${karte.name}' (${karte.id.wert}) ist nicht ausführbar:\n${ergebnis.fehler.joinToString("\n")}",
            )
        }
    }

    private fun findeAssetRoot(): File = sequenceOf(
        File("src/main/assets/de/TeutonStudio/MathematikAtlas/standardkarten"),
        File("app/src/main/assets/de/TeutonStudio/MathematikAtlas/standardkarten"),
    ).firstOrNull(File::isDirectory)
        ?: error("Standardkarten-Assetordner wurde vom JVM-Test nicht gefunden.")
}
