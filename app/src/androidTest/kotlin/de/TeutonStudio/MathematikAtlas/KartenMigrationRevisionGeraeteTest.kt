package de.TeutonStudio.MathematikAtlas

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikAtlas.speicher.KartenSpeicher
import de.TeutonStudio.MathematikKnoten.*
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.util.UUID

class KartenMigrationRevisionGeraeteTest {
    private fun isoliert(test:(Context,File)->Unit) {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val ordner=File(context.cacheDir,"migrationsrevision-${UUID.randomUUID()}").apply {mkdirs()}
        val lokal=object:ContextWrapper(context) {
            override fun getFilesDir()=File(ordner,"files").apply {mkdirs()}
            override fun getNoBackupFilesDir()=File(ordner,"no-backup").apply {mkdirs()}
        }
        try {test(lokal,File(lokal.filesDir,"MathematikAtlas/karten"))} finally {ordner.deleteRecursively()}
    }
    @Test fun migrierteUnreferenzierteAltkarteErzeugtNeueRevisionUndBewahrtAlleOriginaldateien()=isoliert {context,basis ->
        val alt=KartenDaten(id=KartenId("alt"),name="Alt",version=7,knoten=listOf(ÜbersetzungsKnotenVorlagen.Abflachen.erzeuge(GraphPunkt.Zero)))
        val datei=File(basis,"alt/v7.json").apply {parentFile!!.mkdirs()}
        val original=KartenDatenJson.schreibe(alt);datei.writeText(original)
        val beschädigt=File(datei.parentFile,"v12.json").apply {writeText("{defekt")}
        val speicher=KartenSpeicher(context)
        val geladen=requireNotNull(speicher.lade(KartenVerweis(alt.id,7)))
        assertEquals(ÜBERSETZER_ART,geladen.knoten.single().art)
        val neu=speicher.speichere(geladen.copy(name="Bearbeitet"))
        assertEquals(13,neu.version);assertEquals(original,datei.readText());assertEquals("{defekt",beschädigt.readText())
        assertEquals("Alt",speicher.lade(KartenVerweis(alt.id,7))!!.name)
        assertEquals("Bearbeitet",speicher.lade(KartenVerweis(alt.id,13))!!.name)
        assertEquals(alt.knoten.single().id,neu.knoten.single().id)
        assertEquals(alt.knoten.single().anschlüsse.map {it.id},neu.knoten.single().anschlüsse.map {it.id})
    }
    @Test fun unveraenderterAktuellerCodecErhaeltBisherigeGewoehnlicheSpeichersemantik()=isoliert {context,basis ->
        val karte=KartenDaten(id=KartenId("aktuell"),name="Aktuell",version=7)
        val datei=File(basis,"aktuell/v7.json").apply {parentFile!!.mkdirs();writeText(KartenDatenJson.schreibe(karte))}
        val neu=KartenSpeicher(context).speichere(karte.copy(name="Bearbeitet"))
        assertEquals(7,neu.version);assertFalse(File(datei.parentFile,"v8.json").exists())
    }
    @Test fun beschaedigteZielrevisionWirdBeimSpeichernNichtUeberschrieben()=isoliert {context,basis ->
        val datei=File(basis,"defekt/v7.json").apply {parentFile!!.mkdirs();writeText("{defekt")}
        val neu=KartenSpeicher(context).speichere(KartenDaten(id=KartenId("defekt"),name="Wiederhergestellt",version=7))
        assertEquals(8,neu.version);assertEquals("{defekt",datei.readText())
        assertTrue(File(datei.parentFile,"v8.json").exists())
    }
}
