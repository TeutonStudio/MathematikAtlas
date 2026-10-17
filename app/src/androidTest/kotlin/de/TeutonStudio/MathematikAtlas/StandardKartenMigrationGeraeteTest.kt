package de.TeutonStudio.MathematikAtlas

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikAtlas.speicher.*
import de.TeutonStudio.MathematikKnoten.*
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.util.UUID
import java.security.MessageDigest

class StandardKartenMigrationGeraeteTest {
    private val source="standard.function.rational.removable-hole"
    // Vor-M4-Algorithmus auf Android, ohne aktuellen Mathematikcodec/Migration.
    // org.json ordnet Schlüssel auf JVM und Android unterschiedlich; der im
    // JVM-Test eingefrorene Hash ist deshalb keine Android-Vorinstallation.
    private fun vorM4InstallationsHash(original:KartenDaten):String {
        val normalisiert=original.copy(version=1,erstelltAm=0L,archiviert=false,ansicht=AnsichtsFenster.Standard)
        val bytes=KartenDatenJson.schreibe(normalisiert).toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {"%02x".format(it)}
    }
    private val assets="de/TeutonStudio/MathematikAtlas/standardkarten/"
    private fun isoliert(test:(Context,File)->Unit) {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val ordner=File(context.cacheDir,"standardmigration-${UUID.randomUUID()}").apply {mkdirs()}
        val lokal=object:ContextWrapper(context) {
            override fun getFilesDir()=File(ordner,"files").apply {mkdirs()}
            override fun getNoBackupFilesDir()=File(ordner,"no-backup").apply {mkdirs()}
        }
        try {test(lokal,File(lokal.filesDir,"MathematikAtlas/karten"))} finally {ordner.deleteRecursively()}
    }
    private fun seed(context:Context,basis:File,modus:String="alt"):Pair<File,String> {
        val speicher=KartenSpeicher(context)
        val manifest=JSONObject(context.assets.open(assets+"manifest.json").bufferedReader().use {it.readText()})
        val entries=manifest.getJSONArray("entries")
        val provenienz=mutableListOf<InstallierteStandardKarte>()
        val roh=InstrumentationRegistry.getInstrumentation().context.assets.open("konsolidierung-m4/installiert-vorher-v10.json").bufferedReader().use {it.readText()}
        var original=roh
        for(i in 0 until entries.length()) {
            val e=entries.getJSONObject(i)
            val ziel=e.getString("sourceId")==source
            val karte=if(ziel) KartenDatenJson.lese(roh) else KartenJson.lese(context.assets.open(assets+e.getString("path")).bufferedReader().use {it.readText()})
            val gespeichert=if(ziel) {
                if(modus=="aktuell") original=KartenJson.schreibe(karte)
                if(modus=="rückmigriert") original=KartenDatenJson.schreibe(karte.copy(knoten=karte.knoten.map {k->if(k.id.wert!="n4") k else k.copy(parameter=k.parameter+mapOf(ENDLICHE_MENGE_EINGABE to ENDLICHE_MENGE_LOKAL,ENDLICHE_MENGE_ANZAHL to "1"))}))
                if(modus=="bearbeitet") original=KartenDatenJson.schreibe(karte.copy(name="Meine Bearbeitung"))
                File(basis,"${karte.id.wert}/v7.json").apply {parentFile!!.mkdirs();writeText(original)}
                if(modus=="defekt") File(basis,"${karte.id.wert}/v12.json").writeText("{defekt")
                karte
            } else speicher.speichere(karte)
            provenienz+=InstallierteStandardKarte(sourceId=e.getString("sourceId"),sourceVersion=manifest.getString("version"),
                sourceHash=if(ziel) "vor-m4" else e.getString("sourceHash"),lokaleKartenId=gespeichert.id,
                installierterHash=if(ziel && modus !in setOf("aktuell","rückmigriert")) vorM4InstallationsHash(KartenDatenJson.lese(roh)) else standardKartenInhaltsHash(gespeichert),
                status=if(ziel && modus=="markiert") StandardKartenStatus.BENUTZERGEÄNDERT else StandardKartenStatus.AKTIV)
        }
        StandardKartenProvenienzSpeicher(context).speichere(provenienz)
        return File(basis,"m4-vorher-installiert/v7.json") to original
    }
    @Test fun unveraenderteMigrierteStandardkarteErhaeltNeueRevisionUndFestesOriginal()=isoliert {context,basis ->
        val (datei,original)=seed(context,basis)
        val speicher=KartenSpeicher(context)
        val originalDaten=KartenDatenJson.lese(original)
        assertEquals("mathematik.einzelmenge",originalDaten.knoten.single {it.id.wert=="n4"}.art)
        val alterHash=vorM4InstallationsHash(originalDaten)
        assertTrue(standardKarteUnverändert(originalDaten,alterHash))
        assertNotEquals(alterHash,standardKartenInhaltsHash(originalDaten))
        val bericht=StandardKartenInstaller(context,speicher,bekannteStandardKartenKnotenArten()).installiere()
        assertEquals(1,bericht.aktualisiert);assertEquals(0,bericht.alsGeändertErkannt)
        assertEquals(original,datei.readText())
        val neu=speicher.ladeAktuell(KartenId("m4-vorher-installiert"))!!
        assertEquals(8,neu.version)
        assertEquals(ENDLICHE_MENGE_ART,neu.knoten.single {it.id.wert=="n4"}.art)
        val alt=speicher.lade(KartenVerweis(neu.id,7))!!
        assertEquals(7,alt.version)
        assertEquals(KartenVerweis(KartenId("standard.function.linear.x-plus-1"),1),alt.knoten.single {it.id.wert=="n1"}.kartenVerweis)
        assertNotNull(speicher.lade(alt.knoten.single {it.id.wert=="n1"}.kartenVerweis!!))
    }
    @Test fun auchBereitsKanonischeOffizielleQuellenupdatesBewahrenVorgaengerrevision()=isoliert {context,basis ->
        val (datei,original)=seed(context,basis,"aktuell")
        val speicher=KartenSpeicher(context)
        assertEquals(1,StandardKartenInstaller(context,speicher,bekannteStandardKartenKnotenArten()).installiere().aktualisiert)
        assertEquals(original,datei.readText());assertEquals(8,speicher.ladeAktuell(KartenId("m4-vorher-installiert"))!!.version)
    }
    @Test fun bearbeiteteMarkierteUndHoechsteBeschaedigteStandardrevisionenWerdenGeschuetzt() {
        for(modus in listOf("bearbeitet","markiert","defekt","rückmigriert")) isoliert {context,basis ->
            val (datei,original)=seed(context,basis,modus)
            val bericht=StandardKartenInstaller(context,KartenSpeicher(context),bekannteStandardKartenKnotenArten()).installiere()
            assertEquals(0,bericht.aktualisiert);assertEquals(original,datei.readText())
            assertFalse(File(datei.parentFile,"v8.json").exists())
            if(modus=="defekt") assertEquals("{defekt",File(datei.parentFile,"v12.json").readText())
            assertEquals(StandardKartenStatus.BENUTZERGEÄNDERT,StandardKartenProvenienzSpeicher(context).liste().single {it.sourceId==source}.status)
        }
    }
}
