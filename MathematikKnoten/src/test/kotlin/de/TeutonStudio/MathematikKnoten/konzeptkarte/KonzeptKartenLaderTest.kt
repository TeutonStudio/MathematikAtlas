package de.TeutonStudio.MathematikKnoten.konzeptkarte

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDatenJson
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KonzeptKartenLaderTest {
    @Test
    fun `Loader liest manifestierte Karte und prüft Karten-ID`() {
        val id = KonzeptKartenId("test.karte")
        val text = KartenDatenJson.schreibe(KartenDaten(id = KartenId(id.wert), name = "Test", erstelltAm = 0L))
        val manifest = KonzeptKartenManifest(
            listOf(KonzeptKartenManifestEintrag(id, "test-v1.json")),
        )
        val lader = KonzeptKartenLader(
            quelle = KonzeptKartenQuelle { pfad ->
                text.takeIf { pfad == "$ASSET_BASISPFAD/test-v1.json" }
            },
            manifest = manifest,
        )

        val ergebnis = assertIs<KonzeptKartenLadeErgebnis.Erfolg>(lader.lade(id))
        assertEquals("Test", ergebnis.karte.name)
        assertEquals(emptyList(), lader.validierungsFehler())
    }

    @Test
    fun `index json ist die einzige Manifestquelle`() {
        val index = """
            {
              "manifestVersion": 1,
              "karten": [
                { "id": "test.karte", "datei": "test-v1.json", "formatVersion": 7 }
              ]
            }
        """.trimIndent()
        val quelle = KonzeptKartenQuelle { pfad -> index.takeIf { pfad == MANIFEST_ASSET_PFAD } }

        val manifest = quelle.ladeManifest().getOrThrow()

        val eintrag = assertIs<KonzeptKartenManifestEintrag>(manifest.finde(KonzeptKartenId("test.karte")))
        assertEquals("test-v1.json", eintrag.datei)
        assertEquals(7, eintrag.formatVersion)
    }

    @Test
    fun `unbekannte Manifestversion wird abgelehnt`() {
        val index = """{ "manifestVersion": 2, "karten": [] }"""
        val fehler = runCatching { KonzeptKartenManifestJson.lese(index) }.exceptionOrNull()

        assertIs<IllegalArgumentException>(fehler)
    }

    @Test
    fun `fehlendes Asset und falsche Karten-ID liefern strukturierte Fehler`() {
        val id = KonzeptKartenId("test.karte")
        val manifest = KonzeptKartenManifest(
            listOf(KonzeptKartenManifestEintrag(id, "test-v1.json")),
        )
        val fehlt = KonzeptKartenLader(KonzeptKartenQuelle { null }, manifest)
        val fehltFehler = assertIs<KonzeptKartenLadeErgebnis.Fehler>(fehlt.lade(id))
        assertEquals("asset_fehlt", fehltFehler.code)

        val falscherText = KartenDatenJson.schreibe(
            KartenDaten(id = KartenId("andere.karte"), name = "Falsch", erstelltAm = 0L),
        )
        val falsch = KonzeptKartenLader(KonzeptKartenQuelle { falscherText }, manifest)
        val idFehler = assertIs<KonzeptKartenLadeErgebnis.Fehler>(falsch.lade(id))
        assertEquals("karten_id", idFehler.code)
        assertTrue(idFehler.nachricht.contains("andere.karte"))
    }

    @Test
    fun `Manifest verbietet doppelte IDs und Dateien`() {
        val id = KonzeptKartenId("test.karte")
        val fehler = runCatching {
            KonzeptKartenManifest(
                listOf(
                    KonzeptKartenManifestEintrag(id, "a.json"),
                    KonzeptKartenManifestEintrag(id, "b.json"),
                ),
            )
        }.exceptionOrNull()

        assertIs<IllegalArgumentException>(fehler)
    }
}
