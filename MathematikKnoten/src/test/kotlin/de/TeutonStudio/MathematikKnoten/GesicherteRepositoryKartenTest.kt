package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import java.io.File
import java.security.MessageDigest
import org.json.JSONArray
import kotlin.test.*

class GesicherteRepositoryKartenTest {
    @Test fun `gesicherte Originalkarten bleiben unveraendert und kanonisch gleichwertig ladbar`() {
        val basis=File("src/test/resources/konsolidierung-m7/originalkarten")
        val ledger=JSONArray(File(basis,"originale.json").readText())
        assertEquals(523,ledger.length())
        val export=System.getenv("ATLAS_M7_EXPORT_DIR")?.let {File(it).canonicalFile.also {f -> require(f.toPath().startsWith(File("/tmp").canonicalFile.toPath()))}}
        for(i in 0 until ledger.length()) {
            val eintrag=ledger.getJSONObject(i);val path=eintrag.getString("datei")
            val fixture=File(basis,path);val bytes=fixture.readBytes()
            assertEquals(eintrag.getString("sha256"),MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)},path)
            val original=KartenDatenJson.lese(bytes.toString(Charsets.UTF_8))
            val aktuell=MathematikKartenCodec.lade(bytes.toString(Charsets.UTF_8))
            assertEquals(original.id,aktuell.id);assertEquals(original.version,aktuell.version)
            for(k in original.knoten) {
                val neu=aktuell.knoten.single {it.id==k.id}
                assertTrue(neu.anschlüsse.map {it.id}.containsAll(k.anschlüsse.map {it.id}),"$path: Anschlussverlust bei ${k.art}")
            }
            assertTrue(aktuell.verbindungen.map {it.id}.containsAll(original.verbindungen.map {it.id}),"$path: Verbindungsverlust")
            assertTrue(aktuell.knoten.none {RECHNER_MIGRATION_DIAGNOSE in it.parameter},path)
            assertEquals(aktuell,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(aktuell)),path)
            export?.let {ziel -> File(ziel,path).also {f -> f.parentFile.mkdirs();f.writeText(KartenDatenJson.schreibe(aktuell))}}
        }
    }
    @Test fun `ausgelieferte Karten entsprechen den migrierten Originalen`() {
        val basis=File("src/test/resources/konsolidierung-m7/originalkarten")
        val ledger=JSONArray(File(basis,"originale.json").readText())
        for(i in 0 until ledger.length()) {
            val path=ledger.getJSONObject(i).getString("datei")
            assertEquals(MathematikKartenCodec.lade(File(basis,path).readText()),
                MathematikKartenCodec.lade(File("..",path).readText()),path)
        }
    }
}
