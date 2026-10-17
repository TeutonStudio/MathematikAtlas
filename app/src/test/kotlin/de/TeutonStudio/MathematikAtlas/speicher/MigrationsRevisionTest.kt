package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.ÜbersetzungsKnotenVorlagen
import java.nio.file.Files
import kotlin.test.*

class MigrationsRevisionTest {
    @Test fun `Migration wird allein aus unveraenderten Originalbytes erkannt`() {
        val ordner=Files.createTempDirectory("atlas-revision").toFile()
        try {
            val karte=KartenDaten(id=KartenId("alt"),name="Alt",version=7,knoten=listOf(ÜbersetzungsKnotenVorlagen.Abflachen.erzeuge(GraphPunkt.Zero)))
            val datei=ordner.resolve("v7.json");val text=KartenDatenJson.schreibe(karte)
            datei.writeText(text)
            assertTrue(mussOriginalrevisionBewahren(datei,KartenVerweis(karte.id,7)))
            assertEquals(text,datei.readText())
            val aktuell=karte.copy(knoten=emptyList());datei.writeText(KartenDatenJson.schreibe(aktuell))
            assertFalse(mussOriginalrevisionBewahren(datei,KartenVerweis(karte.id,7)))
        } finally {ordner.deleteRecursively()}
    }
    @Test fun `Formatwechsel defekte und widerspruechliche Originale bleiben geschuetzt`() {
        val ordner=Files.createTempDirectory("atlas-revision-defekt").toFile()
        try {
            val karte=KartenDaten(id=KartenId("original"),name="Original",version=7)
            val datei=ordner.resolve("v7.json");val ref=KartenVerweis(karte.id,7)
            datei.writeText(KartenDatenJson.schreibe(karte).replace("\"formatVersion\": 10","\"formatVersion\": 7"))
            assertTrue(mussOriginalrevisionBewahren(datei,ref))
            datei.writeText("{defekt");assertTrue(mussOriginalrevisionBewahren(datei,ref))
            datei.writeText(KartenDatenJson.schreibe(karte.copy(id=KartenId("fremd"))))
            assertTrue(mussOriginalrevisionBewahren(datei,ref))
            datei.writeText(KartenDatenJson.schreibe(karte.copy(version=8)))
            assertTrue(mussOriginalrevisionBewahren(datei,ref))
            datei.delete();assertFalse(mussOriginalrevisionBewahren(datei,ref))
        } finally {ordner.deleteRecursively()}
    }
}
