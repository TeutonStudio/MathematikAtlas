package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import java.io.File
import kotlin.test.*

class RepositoryKartenMigrationsVertragTest {
    @Test fun `alle produktiven Repositorykarten sind nach allen Codecphasen erneut stabil ladbar`() {
        val wurzeln=listOf(File("src/main/assets/de/TeutonStudio/MathematikKnoten/konzeptkarte"),File("../app/src/main/assets/de/TeutonStudio/MathematikAtlas/standardkarten"))
        val dateien=wurzeln.flatMap { it.walkTopDown().filter {f -> f.extension=="json" && f.name !in setOf("manifest.json","index.json")}.toList() }
        assertEquals(523,dateien.size)
        val diagnosen=mutableListOf<String>()
        for(f in dateien) {
            val text=f.readText();val alt=KartenDatenJson.lese(text)
            val phasen=listOf<(KartenDaten)->KartenDaten>(
                {MathematikKartenCodec.lese(KartenDatenJson.schreibe(it))},
                {MathematikKartenCodec.lade(KartenDatenJson.schreibe(it))},
                {MathematikKartenCodec.importiere(KartenDatenJson.schreibe(it))},
                MathematikKartenMigrationen::vorSpeichern)
            for(phase in phasen) {
                val neu=phase(alt)
                assertEquals(alt.id,neu.id,f.path);assertEquals(alt.version,neu.version,f.path)
                assertEquals(neu,phase(neu),"${f.path}: wiederholte Codecphase")
                val erneut=MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(neu))
                assertEquals(erneut,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(erneut)),"${f.path}: Speicherrundlauf")
                neu.knoten.filter {RECHNER_MIGRATION_DIAGNOSE in it.parameter}.forEach {k ->
                    diagnosen += "${f.path}: ${k.id}: ${k.parameter[RECHNER_MIGRATION_DIAGNOSE]}"
                }
            }
        }
        assertTrue(diagnosen.isEmpty(),diagnosen.distinct().joinToString("\n"))
    }
}
