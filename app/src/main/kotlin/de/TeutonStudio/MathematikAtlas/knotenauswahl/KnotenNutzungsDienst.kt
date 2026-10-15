package de.TeutonStudio.MathematikAtlas.knotenauswahl

import de.TeutonStudio.MathematikAtlas.speicher.KnotenNutzungsSpeicher
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class KnotenNutzungsStand(
    val bereit: Boolean = false,val geschützt: Boolean = false,val fehler: String? = null,
    val einstellungen: KnotenNutzungsEinstellungen = KnotenNutzungsEinstellungen(),
    val sitzungen: Int = 0,val schritte: Int = 0,val auswahlen: Long = 0,
)

/** Ein IO-Worker: Laden, Ereignisse, Schreiben und Löschen haben dieselbe Reihenfolge. */
class KnotenNutzungsDienst(
    private val speicher: KnotenNutzungsSpeicher,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val zeit: () -> Long = System::currentTimeMillis,
) {
    private data class Befehl(val anwenden: (KnotenAuswahlNutzungsVerwalter) -> Unit,val löschen: Boolean = false,val quittung: CompletableDeferred<Boolean>? = null)
    private val scope=CoroutineScope(SupervisorJob()+dispatcher)
    private val befehle=Channel<Befehl>(Channel.UNLIMITED)
    private val zustand=MutableStateFlow(KnotenNutzungsStand())
    val stand=zustand.asStateFlow()
    private val arbeit=scope.launch {
        val geladen=speicher.laden();var geschützt=geladen.geschützt
        val verwalter=KnotenAuswahlNutzungsVerwalter(geladen.daten,zeit)
        var gespeichert: KnotenNutzungsDaten? = geladen.daten
        fun veröffentlichen(fehler: String? = null) { val d=verwalter.daten;zustand.value=KnotenNutzungsStand(true,geschützt,fehler,d.einstellungen,d.sitzungen.size,d.sitzungen.sumOf { it.suchschritte.size },d.statistik.eintraege.values.sumOf { it.anzahl }) }
        veröffentlichen(if(geschützt) "Statistikdatei kann nicht sicher gelesen werden. Sie bleibt bis zum ausdrücklichen Löschen geschützt." else null)
        for(erster in befehle) {
            val batch=mutableListOf(erster)
            repeat(63) { befehle.tryReceive().getOrNull()?.let(batch::add) }
            var ok=true
            for(b in batch) try {
                if(b.löschen) { speicher.löschen();geschützt=false;gespeichert=null }
                if(!geschützt) b.anwenden(verwalter) else ok=false
            } catch(_: Exception) { ok=false }
            if(!geschützt && verwalter.daten!=gespeichert) try {
                val stand=speicher.speichern(verwalter.daten)
                verwalter.übernehmeSpeichergrenze(stand);gespeichert=stand
            } catch(_: Exception) { ok=false }
            veröffentlichen(if(geschützt) "Statistikdatei bleibt geschützt; bei Bedarf ausdrücklich löschen." else if(!ok) "Statistik konnte nicht vollständig gespeichert werden. Der Editor bleibt nutzbar." else null)
            batch.forEach { it.quittung?.complete(ok) }
        }
    }.also { job -> job.invokeOnCompletion { scope.cancel() } }
    private fun sende(b: Befehl) { if(!befehle.trySend(b).isSuccess) b.quittung?.complete(false) }
    fun öffnen(id: String,art: String,version: String,z: KnotenSuchZustand) { val t=zeit();sende(Befehl({ it.öffnen(id,art,version,z,t) })) }
    fun aktualisieren(id: String,z: KnotenSuchZustand) = sende(Befehl({ it.aktualisieren(id,z) }))
    fun sichtbar(id: String,z: KnotenSuchZustand,keys: Set<String>) = sende(Befehl({ it.sichtbar(id,z,keys) }))
    fun protokollieren(id: String,z: KnotenSuchZustand,a: SuchAnlass) { val t=zeit();sende(Befehl({ it.protokollieren(id,z,a,t) })) }
    fun schließen(id: String,z: KnotenSuchZustand?,g: SitzungsAbschluss) { val t=zeit();sende(Befehl({ it.schließen(id,z,g,t) })) }
    fun ergebnis(a: KnotenAuswahlAuftrag,erfolg: Boolean,knoten: List<ErzeugterKnotenTyp>) { val t=zeit();sende(Befehl({ it.auswählen(a,erfolg,knoten,t) })) }
    fun einstellungen(e: KnotenNutzungsEinstellungen) = sende(Befehl({ it.einstellungen(e) }))
    fun erfassungÄndern(aktiv: Boolean) = sende(Befehl({ it.einstellungen(it.daten.einstellungen.copy(erfassen=aktiv)) }))
    fun suchtexteÄndern(aktiv: Boolean) = sende(Befehl({ it.einstellungen(it.daten.einstellungen.copy(suchtexte=aktiv)) }))
    fun löschen() = sende(Befehl({ it.löschen() },löschen=true))
    fun sichern(): Deferred<Boolean> = CompletableDeferred<Boolean>().also { sende(Befehl({},quittung=it)) }
    /** Kanal schließen statt laufende IO-Jobs abzubrechen: bereits eingereihte Ereignisse werden abgearbeitet. */
    fun beenden() { befehle.close() }
    suspend fun warteAufEnde() { arbeit.join() }
}
