package de.TeutonStudio.MathematikAtlas.speicher

import android.content.Context
import android.util.AtomicFile
import de.TeutonStudio.MathematikAtlas.knotenauswahl.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException

interface KnotenNutzungsDatei {
    fun lesen(maxBytes: Int): ByteArray?
    fun schreiben(bytes: ByteArray)
    fun löschen()
}

/** Ein Prozessdienst über Activity-Neuerstellungen hinweg verhindert konkurrierende Dateischreiber. */
object InstallationsKnotenNutzung {
    private var dienst: KnotenNutzungsDienst?=null
    @Synchronized fun für(context: Context): KnotenNutzungsDienst = dienst ?: KnotenNutzungsDienst(KnotenNutzungsSpeicher(AndroidKnotenNutzungsDatei(context.applicationContext))).also { dienst=it }
}

/** AtomicFile benötigt zusätzlich die Synchronisierung des umgebenden Speichers. */
class AndroidKnotenNutzungsDatei(context: Context): KnotenNutzungsDatei {
    private val datei=AtomicFile(File(context.noBackupFilesDir,"MathematikAtlas/nutzung/knotenauswahl.json"))
    override fun lesen(maxBytes: Int): ByteArray? = try {
        datei.openRead().use { input ->
            val out=ByteArrayOutputStream();val buffer=ByteArray(8192)
            while(true) { val n=input.read(buffer);if(n<0) break;require(out.size()+n<=maxBytes);out.write(buffer,0,n) }
            out.toByteArray()
        }
    } catch(_: FileNotFoundException) { null }
    override fun schreiben(bytes: ByteArray) {
        check(datei.baseFile.parentFile?.let { it.isDirectory || it.mkdirs() }==true)
        val stream=datei.startWrite()
        try { stream.write(bytes);datei.finishWrite(stream) } catch(e: Exception) { datei.failWrite(stream);throw e }
    }
    override fun löschen() { datei.delete();check(!datei.baseFile.exists()) }
}

data class NutzungsLadeErgebnis(val daten: KnotenNutzungsDaten,val geschützt: Boolean = false)

class KnotenNutzungsSpeicher(private val datei: KnotenNutzungsDatei,private val grenzen: NutzungsGrenzen = NutzungsGrenzen()) {
    private var geschützt=false
    @Synchronized fun laden(): NutzungsLadeErgebnis = try {
        val bytes=datei.lesen(grenzen.dateiBytes)
        val daten=bytes?.let { KnotenNutzungsCodec.lese(it.toString(Charsets.UTF_8)) } ?: KnotenNutzungsDaten()
        geschützt=false;NutzungsLadeErgebnis(daten)
    } catch(_: Exception) { geschützt=true;NutzungsLadeErgebnis(KnotenNutzungsDaten(),true) }
    @Synchronized fun speichern(daten: KnotenNutzungsDaten): KnotenNutzungsDaten {
        check(!geschützt) { "Statistikdatei ist geschützt." }
        var klein=daten;var bytes=KnotenNutzungsCodec.schreibe(klein).toByteArray(Charsets.UTF_8)
        while(bytes.size>grenzen.dateiBytes && klein.sitzungen.isNotEmpty()) {
            val index=klein.sitzungen.indexOfFirst { it.suchschritte.size>1 }
            klein=if(index>=0) klein.copy(sitzungen=klein.sitzungen.mapIndexed { i,s ->
                if(i!=index) s else {
                    val weg=s.suchschritte.first().id
                    s.copy(suchschritte=s.suchschritte.drop(1),auswahlen=s.auswahlen.map { if(it.suchschritt==weg) it.copy(suchschritt=null) else it })
                }
            }) else {
                val weg=klein.sitzungen.firstOrNull { it.abschluss!=SitzungsAbschluss.OFFEN } ?: break
                klein.copy(sitzungen=klein.sitzungen.filterNot { it.id==weg.id })
            }
            bytes=KnotenNutzungsCodec.schreibe(klein).toByteArray(Charsets.UTF_8)
        }
        while(bytes.size>grenzen.dateiBytes && klein.statistik.eintraege.size>1) {
            val ältester=klein.statistik.eintraege.minBy { it.value.zuletzt }.key
            klein=klein.copy(statistik=KnotenNutzungsStatistik(klein.statistik.eintraege-ältester))
            bytes=KnotenNutzungsCodec.schreibe(klein).toByteArray(Charsets.UTF_8)
        }
        require(bytes.size<=grenzen.dateiBytes) { "Statistik-Dateigrenze überschritten." }
        datei.schreiben(bytes);return klein
    }
    @Synchronized fun löschen() { datei.löschen();geschützt=false }
}

/** Strikt versionierter eigener Codec, ausdrücklich kein Teil von KartenJson. */
object KnotenNutzungsCodec {
    private fun obj(vararg p: Pair<String,Any?>) = JSONObject().apply { p.forEach { (k,v) -> put(k,v ?: JSONObject.NULL) } }
    private fun zustand(z: KnotenSuchZustand) = obj("text" to z.originaltext,"herkunft" to z.herkunft.name,"textTreffer" to z.textTreffer,
        "einfügbar" to z.einfügbareTreffer,"gewünscht" to z.gewünschterTab,"aktiv" to z.aktiverTab,"filter" to z.anschlussFilter,
        "angebot" to JSONArray(z.angebote.map { obj("key" to it.schluessel,"einfügbar" to it.einfügbar) }))
    fun schreibe(d: KnotenNutzungsDaten): String = obj(
        "schema" to d.schema,"erfassen" to d.einstellungen.erfassen,"suchtexte" to d.einstellungen.suchtexte,
        "sitzungen" to JSONArray(d.sitzungen.map { s -> obj("id" to s.id,"beginn" to s.beginn,"art" to s.öffnungsart,"version" to s.appVersion,
            "abschluss" to s.abschluss.name,"ende" to s.ende,
            "schritte" to JSONArray(s.suchschritte.map { x -> obj("id" to x.id,"folge" to x.reihenfolge,"zeit" to x.zeit,"zustand" to zustand(x.zustand),
                "normalisiert" to x.normalisiert,"anlass" to x.anlass.name,"sichtbar" to JSONArray(x.sichtbar.toList()),"textGespeichert" to x.textGespeichert,"gekürzt" to x.textGekürzt) }),
            "auswahlen" to JSONArray(s.auswahlen.map { a -> obj("id" to a.id,"eintrag" to a.eintrag,"schritt" to a.suchschritt,"rang" to a.rang,"zeit" to a.zeit,
                "erfolg" to a.erfolgreich,"kartenVersion" to a.kartenVersion,"erzeugt" to JSONArray(a.erzeugt.map { obj("art" to it.art,"variante" to it.variante) })) })) }),
        "eintraege" to JSONArray(d.statistik.eintraege.map { (key,e) -> obj("key" to key,"anzahl" to e.anzahl,"zuletzt" to e.zuletzt,"tage" to JSONObject(e.tage)) }),
    ).toString()
    private fun JSONObject.nullText(key: String) = if(isNull(key)) null else getString(key)
    private fun JSONArray.objekte(max: Int): List<JSONObject> { require(length()<=max);return (0 until length()).map(::getJSONObject) }
    private fun JSONObject.zustand(): KnotenSuchZustand = KnotenSuchZustand(getString("text"),SuchtextHerkunft.valueOf(getString("herkunft")),getInt("textTreffer"),getInt("einfügbar"),getString("gewünscht"),getString("aktiv"),
        getJSONArray("angebot").objekte(512).map { KnotenAngebot(it.getString("key"),it.getBoolean("einfügbar")) },nullText("filter"))
    fun lese(text: String): KnotenNutzungsDaten {
        val d=JSONObject(text);require(d.getInt("schema")==KNOTEN_NUTZUNG_SCHEMA)
        val sitzungen=d.getJSONArray("sitzungen").objekte(1000).map { s ->
            val schritte=s.getJSONArray("schritte").objekte(250).map { x ->
                val sichtbar=x.getJSONArray("sichtbar");require(sichtbar.length()<=100)
                KnotenSuchschritt(x.getString("id"),x.getInt("folge"),x.getLong("zeit"),x.getJSONObject("zustand").zustand(),x.nullText("normalisiert"),SuchAnlass.valueOf(x.getString("anlass")),
                    (0 until sichtbar.length()).map { sichtbar.getString(it) }.toSet(),x.getBoolean("textGespeichert"),x.getBoolean("gekürzt"))
            }
            val auswahl=s.getJSONArray("auswahlen").objekte(64).map { a -> KnotenAuswahlErgebnis(a.getString("id"),a.getString("eintrag"),a.nullText("schritt"),a.getInt("rang"),a.getLong("zeit"),a.getBoolean("erfolg"),
                a.getJSONArray("erzeugt").objekte(1000).map { ErzeugterKnotenTyp(it.getString("art"),it.nullText("variante")) },if(a.isNull("kartenVersion")) null else a.getInt("kartenVersion")) }
            KnotenAuswahlSitzung(s.getString("id"),s.getLong("beginn"),s.getString("art"),s.getString("version"),SitzungsAbschluss.valueOf(s.getString("abschluss")),if(s.isNull("ende")) null else s.getLong("ende"),schritte,auswahl)
        }
        val eintraege=d.getJSONArray("eintraege").objekte(4096).associate { e ->
            val tage=e.getJSONObject("tage");require(tage.length()<=90)
            e.getString("key") to EintragsNutzung(e.getLong("anzahl"),e.getLong("zuletzt"),tage.keys().asSequence().associateWith { tage.getLong(it) })
        }
        require(sitzungen.map { it.id }.distinct().size==sitzungen.size)
        require(sitzungen.sumOf { it.suchschritte.size }<=10_000)
        require(eintraege.values.all { it.anzahl>=0 && it.tage.values.all { n -> n>=0 } })
        return KnotenNutzungsDaten(einstellungen=KnotenNutzungsEinstellungen(d.getBoolean("erfassen"),d.getBoolean("suchtexte")),sitzungen=sitzungen,statistik=KnotenNutzungsStatistik(eintraege))
    }
}
