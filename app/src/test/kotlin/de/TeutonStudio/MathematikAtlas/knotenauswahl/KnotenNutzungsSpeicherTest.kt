package de.TeutonStudio.MathematikAtlas.knotenauswahl

import de.TeutonStudio.MathematikAtlas.speicher.*
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.*

class KnotenNutzungsSpeicherTest {
    private val zeit=1_800_000_000_000L
    private fun z(text: String="∪")=KnotenSuchZustand(text,SuchtextHerkunft.GETIPPT,1,1,"Alle","Alle",listOf(KnotenAngebot("vorlage/test/standard",true)))
    private class Datei: KnotenNutzungsDatei {
        var bytes: ByteArray?=null;var schreibvorgänge=0;var fehler=false;var sperre: CountDownLatch?=null;val schreibt=CountDownLatch(1)
        override fun lesen(maxBytes: Int): ByteArray? { require((bytes?.size ?: 0)<=maxBytes);return bytes }
        override fun schreiben(bytes: ByteArray) { schreibvorgänge++;schreibt.countDown();sperre?.await(5,TimeUnit.SECONDS);check(!fehler);this.bytes=bytes.copyOf() }
        override fun löschen() { bytes=null }
    }
    private fun daten(): KnotenNutzungsDaten {
        val m=KnotenAuswahlNutzungsVerwalter(jetzt={zeit});m.öffnen("s","frei","3.2.2",z())
        m.auswählen(KnotenAuswahlAuftrag("a","s","vorlage/test/standard",1,z()),true,listOf(ErzeugterKnotenTyp("test","op")))
        return m.daten
    }
    @Test fun `eigener Codec hat vollstaendigen Roundtrip ohne Kartendaten`() {
        val d=daten();assertEquals(d,KnotenNutzungsCodec.lese(KnotenNutzungsCodec.schreibe(d)))
        assertFalse(KnotenNutzungsCodec.schreibe(d).contains("verbindungen"))
    }
    @Test fun `neueres Schema bleibt bis zum ausdruecklichen Loeschen unangetastet`() {
        val f=Datei();f.bytes="{\"schema\":99,\"inhalt\":\"behalten\"}".toByteArray();val original=f.bytes!!.copyOf()
        val s=KnotenNutzungsSpeicher(f);assertTrue(s.laden().geschützt);assertFails { s.speichern(daten()) };assertContentEquals(original,f.bytes)
        s.löschen();s.speichern(KnotenNutzungsDaten());assertFalse(s.laden().geschützt)
    }
    @Test fun `beschaedigte und uebergrosse Dateien blockieren den Editor nicht`() {
        val f=Datei();f.bytes="kein json".toByteArray();val s=KnotenNutzungsSpeicher(f)
        assertTrue(s.laden().geschützt);assertFails { s.speichern(daten()) }
        f.bytes=ByteArray(3000);assertTrue(KnotenNutzungsSpeicher(f,NutzungsGrenzen(dateiBytes=1024)).laden().geschützt)
    }
    @Test fun `Dateigrenze reduziert alte Details statt erfolgreiche Zaehler zu verlieren`() {
        val f=Datei();val m=KnotenAuswahlNutzungsVerwalter(jetzt={zeit});m.öffnen("s","frei","v",z())
        repeat(20) { m.protokollieren("s",z("Begriff $it"),SuchAnlass.SUCHRUHE) }
        val klein=KnotenNutzungsSpeicher(f,NutzungsGrenzen(dateiBytes=2500)).speichern(m.daten)
        assertTrue(f.bytes!!.size<=2500);assertTrue(klein.sitzungen.single().suchschritte.size<20)
        assertEquals(SitzungsAbschluss.OFFEN,klein.sitzungen.single().abschluss)
    }
    @Test fun `Suchtexte gelangen bei Abschaltung nicht in die Datei`() {
        val m=KnotenAuswahlNutzungsVerwalter(jetzt={zeit});m.einstellungen(KnotenNutzungsEinstellungen(suchtexte=false));m.öffnen("s","frei","v",z("privater Suchtext"))
        val json=KnotenNutzungsCodec.schreibe(m.daten);assertFalse(json.contains("privater"));assertNull(KnotenNutzungsCodec.lese(json).sitzungen.single().suchschritte.single().normalisiert)
    }
    @Test fun `IO Dienst laedt vor Ereignissen und bewahrt alle zusammengefassten Zaehler`() = runBlocking {
        val f=Datei();f.bytes=KnotenNutzungsCodec.schreibe(daten()).toByteArray()
        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { dispatcher ->
            val dienst=KnotenNutzungsDienst(KnotenNutzungsSpeicher(f),dispatcher) { zeit }
            repeat(12) { i ->
                val id="s-$i";dienst.öffnen(id,"frei","v",z())
                dienst.ergebnis(KnotenAuswahlAuftrag("a-$i",id,"vorlage/test/standard",1,z()),true,listOf(ErzeugterKnotenTyp("test")))
            }
            assertTrue(dienst.sichern().await());dienst.beenden();dienst.warteAufEnde()
            assertEquals(13L,KnotenNutzungsCodec.lese(f.bytes!!.toString(Charsets.UTF_8)).statistik.eintraege.values.single().anzahl)
        }
    }
    @Test fun `Schreibfehler verlieren Zaehler im laufenden Dienst nicht`() = runBlocking {
        val f=Datei();f.fehler=true
        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { dispatcher ->
            val d=KnotenNutzungsDienst(KnotenNutzungsSpeicher(f),dispatcher) { zeit };d.öffnen("s","frei","v",z())
            d.ergebnis(KnotenAuswahlAuftrag("a","s","key",1,z()),true,listOf(ErzeugterKnotenTyp("test")))
            assertFalse(d.sichern().await());assertNotNull(d.stand.value.fehler)
            f.fehler=false;assertTrue(d.sichern().await());d.beenden();d.warteAufEnde()
            assertEquals(1L,KnotenNutzungsCodec.lese(f.bytes!!.toString(Charsets.UTF_8)).statistik.eintraege.getValue("key").anzahl)
        }
    }
    @Test fun `Loeschbefehl gewinnt gegen bereits laufenden Schreibauftrag und alte Sitzung`() = runBlocking {
        val f=Datei();val frei=CountDownLatch(1);f.sperre=frei
        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { dispatcher ->
            val d=KnotenNutzungsDienst(KnotenNutzungsSpeicher(f),dispatcher) { zeit };d.öffnen("s","frei","v",z())
            assertTrue(f.schreibt.await(5,TimeUnit.SECONDS))
            d.löschen();d.ergebnis(KnotenAuswahlAuftrag("alt","s","key",1,z()),true,listOf(ErzeugterKnotenTyp("test")))
            frei.countDown();assertTrue(d.sichern().await());d.beenden();d.warteAufEnde()
            val neu=KnotenNutzungsCodec.lese(f.bytes!!.toString(Charsets.UTF_8));assertTrue(neu.sitzungen.isEmpty());assertTrue(neu.statistik.eintraege.isEmpty())
        }
    }
    @Test fun `fluechtige Tippentwuerfe schreiben unveraenderte Datei nicht erneut`() = runBlocking {
        val f=Datei()
        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { dispatcher ->
            val d=KnotenNutzungsDienst(KnotenNutzungsSpeicher(f),dispatcher) { zeit }
            d.öffnen("s","frei","v",z());assertTrue(d.sichern().await())
            val vorher=f.schreibvorgänge
            repeat(20) { d.aktualisieren("s",z("Entwurf $it")) }
            assertTrue(d.sichern().await());assertEquals(vorher,f.schreibvorgänge)
            d.protokollieren("s",z("Entwurf 19"),SuchAnlass.SUCHRUHE)
            assertTrue(d.sichern().await());assertTrue(f.schreibvorgänge>vorher)
            d.beenden();d.warteAufEnde()
        }
    }
    @Test fun `schnelle unterschiedliche Einstellungsaenderungen ueberschreiben sich nicht`() = runBlocking {
        val f=Datei()
        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { dispatcher ->
            val d=KnotenNutzungsDienst(KnotenNutzungsSpeicher(f),dispatcher) { zeit }
            d.erfassungÄndern(false);d.suchtexteÄndern(false);assertTrue(d.sichern().await())
            assertEquals(KnotenNutzungsEinstellungen(false,false),d.stand.value.einstellungen)
            d.beenden();d.warteAufEnde()
        }
    }
}
