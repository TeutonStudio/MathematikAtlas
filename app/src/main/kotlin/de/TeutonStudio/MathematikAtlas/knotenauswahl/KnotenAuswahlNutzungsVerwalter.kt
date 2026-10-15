package de.TeutonStudio.MathematikAtlas.knotenauswahl

import java.time.Instant
import java.time.ZoneOffset

/** Reiner, seriell verwendeter Zustandsautomat. Keine Dateien, UI- oder Graphabhängigkeiten. */
class KnotenAuswahlNutzungsVerwalter(
    initial: KnotenNutzungsDaten = KnotenNutzungsDaten(),
    private val jetzt: () -> Long = System::currentTimeMillis,
    private val id: () -> String = ::nutzungsId,
    private val grenzen: NutzungsGrenzen = NutzungsGrenzen(),
) {
    var daten=initial
        private set
    private data class Entwurf(val zustand: KnotenSuchZustand,val sichtbar: Set<String> = emptySet())
    private val entwürfe=mutableMapOf<String,Entwurf>()
    // Nur flüchtig: auch ohne gespeicherte Texte bleiben Umformulierungen unterscheidbar.
    private val letzteZustände=mutableMapOf<String,KnotenSuchZustand>()
    init {
        daten=daten.copy(sitzungen=daten.sitzungen.map { if(it.abschluss==SitzungsAbschluss.OFFEN) it.copy(abschluss=SitzungsAbschluss.UNTERBROCHEN,ende=jetzt()) else it })
        bereinigen()
    }
    fun öffnen(sitzung: String,art: String,version: String,zustand: KnotenSuchZustand,zeit: Long = jetzt()) {
        if(!daten.einstellungen.erfassen || daten.sitzungen.any { it.id==sitzung }) return
        daten=daten.copy(sitzungen=daten.sitzungen+KnotenAuswahlSitzung(sitzung,zeit,art,version))
        aktualisieren(sitzung,zustand);protokollieren(sitzung,zustand,SuchAnlass.OEFFNEN,zeit)
    }
    private fun offen(sitzung: String) = daten.einstellungen.erfassen && daten.sitzungen.any { it.id==sitzung && it.abschluss==SitzungsAbschluss.OFFEN }
    fun aktualisieren(sitzung: String,zustand: KnotenSuchZustand) {
        if(!offen(sitzung)) return
        val alt=entwürfe[sitzung]
        entwürfe[sitzung]=if(alt?.zustand==zustand) alt else Entwurf(zustand)
    }
    fun sichtbar(sitzung: String,zustand: KnotenSuchZustand,schlüssel: Set<String>) {
        if(!offen(sitzung)) return
        val angeboten=zustand.angebote.mapTo(hashSetOf()) { it.schluessel }
        val neu=schlüssel.filterTo(linkedSetOf()) { it in angeboten }.take(100).toSet()
        // Veraltete Scrollbeobachter dürfen keinen neueren Suchentwurf zurücksetzen.
        if(entwürfe[sitzung]?.zustand!=zustand) return
        val entwurf=entwürfe.getValue(sitzung)
        entwürfe[sitzung]=entwurf.copy(sichtbar=(entwurf.sichtbar+neu).take(100).toSet())
        ändern(sitzung) { s ->
            val letzter=s.suchschritte.lastOrNull()
            if(letzter!=null && entspricht(sitzung,letzter,zustand)) s.copy(suchschritte=s.suchschritte.dropLast(1)+letzter.copy(sichtbar=(letzter.sichtbar+neu).take(100).toSet())) else s
        }
    }
    private fun begrenzt(z: KnotenSuchZustand): KnotenSuchZustand = z.copy(originaltext=z.originaltext.take(2048),angebote=z.angebote.take(512))
    private fun entspricht(sitzung: String,s: KnotenSuchschritt,z: KnotenSuchZustand): Boolean =
        letzteZustände[sitzung]==z && s.zustand==begrenzt(z).let { if(s.textGespeichert) it else it.ohneText() }
    fun protokollieren(sitzung: String,zustand: KnotenSuchZustand,anlass: SuchAnlass,zeit: Long = jetzt()) {
        if(!offen(sitzung)) return
        aktualisieren(sitzung,zustand)
        val entwurf=entwürfe.getValue(sitzung)
        ändern(sitzung) { s ->
            val letzter=s.suchschritte.lastOrNull()
            if(letzter!=null && entspricht(sitzung,letzter,zustand)) return@ändern s
            val text=daten.einstellungen.suchtexte
            val kompakt=begrenzt(zustand)
            val schritt=KnotenSuchschritt(id(),(letzter?.reihenfolge ?: -1)+1,zeit,if(text) kompakt else kompakt.ohneText(),
                if(text) normalisiereSuchtext(kompakt.originaltext) else null,anlass,entwurf.sichtbar,text,zustand.originaltext.length>2048)
            s.copy(suchschritte=(s.suchschritte+schritt).takeLast(250))
        }
        letzteZustände[sitzung]=zustand
        bereinigen()
    }
    fun auswählen(auftrag: KnotenAuswahlAuftrag,erfolgreich: Boolean,erzeugt: List<ErzeugterKnotenTyp>,zeit: Long = jetzt()) {
        if(!offen(auftrag.sitzung)) return
        val s=daten.sitzungen.first { it.id==auftrag.sitzung }
        if(s.auswahlen.any { it.id==auftrag.id } || s.auswahlen.size>=64) return
        protokollieren(auftrag.sitzung,auftrag.zustand,SuchAnlass.AUSWAHL,zeit)
        val schritt=daten.sitzungen.first { it.id==auftrag.sitzung }.suchschritte.lastOrNull()?.id
        val erfolg=erfolgreich && erzeugt.isNotEmpty()
        ändern(auftrag.sitzung) { it.copy(auswahlen=it.auswahlen+KnotenAuswahlErgebnis(auftrag.id,auftrag.eintrag,schritt,auftrag.rang,zeit,erfolg,if(erfolg) erzeugt else emptyList(),auftrag.kartenVersion)) }
        if(erfolg) {
            val alt=daten.statistik.eintraege[auftrag.eintrag]
            val tag=Instant.ofEpochMilli(zeit).atZone(ZoneOffset.UTC).toLocalDate().toString()
            val tage=alt?.tage.orEmpty().toMutableMap();tage[tag]=(tage[tag] ?: 0)+1
            val eintrag=EintragsNutzung((alt?.anzahl ?: 0)+1,zeit,tage.toSortedMap().entries.toList().takeLast(90).associate { it.toPair() })
            daten=daten.copy(statistik=KnotenNutzungsStatistik((daten.statistik.eintraege+(auftrag.eintrag to eintrag)).entries.sortedByDescending { it.value.zuletzt }.take(4096).associate { it.toPair() }))
            schließen(auftrag.sitzung,auftrag.zustand,SitzungsAbschluss.AUSWAHL,zeit)
        }
    }
    fun schließen(sitzung: String,zustand: KnotenSuchZustand?,grund: SitzungsAbschluss,zeit: Long = jetzt()) {
        if(!offen(sitzung)) return
        (zustand ?: entwürfe[sitzung]?.zustand)?.let { protokollieren(sitzung,it,when(grund) {
            SitzungsAbschluss.MODUSWECHSEL -> SuchAnlass.MODUSWECHSEL
            SitzungsAbschluss.UNTERBROCHEN -> SuchAnlass.UNTERBRECHUNG
            else -> SuchAnlass.SCHLIESSEN
        },zeit) }
        ändern(sitzung) { it.copy(abschluss=grund,ende=zeit) };entwürfe.remove(sitzung);letzteZustände.remove(sitzung)
        bereinigen()
    }
    fun einstellungen(neu: KnotenNutzungsEinstellungen) {
        if(!neu.erfassen) daten.sitzungen.filter { it.abschluss==SitzungsAbschluss.OFFEN }.forEach { schließen(it.id,null,SitzungsAbschluss.DEAKTIVIERT) }
        daten=daten.copy(einstellungen=neu,sitzungen=if(neu.suchtexte) daten.sitzungen else daten.sitzungen.map { s -> s.copy(suchschritte=s.suchschritte.map { it.ohneText() }) })
        if(!neu.erfassen) { entwürfe.clear();letzteZustände.clear() }
    }
    fun löschen() { daten=KnotenNutzungsDaten(einstellungen=daten.einstellungen);entwürfe.clear();letzteZustände.clear() }
    fun übernehmeSpeichergrenze(stand: KnotenNutzungsDaten) {
        daten=stand
        letzteZustände.keys.retainAll(stand.sitzungen.filter { it.abschluss==SitzungsAbschluss.OFFEN }.map { it.id }.toSet())
        entwürfe.keys.retainAll(stand.sitzungen.filter { it.abschluss==SitzungsAbschluss.OFFEN }.map { it.id }.toSet())
    }
    private fun ändern(id: String,f: (KnotenAuswahlSitzung) -> KnotenAuswahlSitzung) { daten=daten.copy(sitzungen=daten.sitzungen.map { if(it.id==id) f(it) else it }) }
    fun bereinigen() {
        val grenze=jetzt()-grenzen.tage*86_400_000L
        var rest=grenzen.schritte
        val sitzungen=daten.sitzungen.filter { (it.ende ?: it.beginn)>=grenze }.takeLast(grenzen.sitzungen).asReversed().map { s ->
            val schritte=s.suchschritte.filter { it.zeit>=grenze }.takeLast(rest);rest-=schritte.size
            val ids=schritte.mapTo(hashSetOf()) { it.id }
            s.copy(suchschritte=schritte,auswahlen=s.auswahlen.filter { it.zeit>=grenze }.map { if(it.suchschritt in ids) it else it.copy(suchschritt=null) })
        }.asReversed()
        daten=daten.copy(sitzungen=sitzungen)
        letzteZustände.keys.retainAll(sitzungen.filter { it.abschluss==SitzungsAbschluss.OFFEN }.map { it.id }.toSet())
        entwürfe.keys.retainAll(sitzungen.filter { it.abschluss==SitzungsAbschluss.OFFEN }.map { it.id }.toSet())
    }
}
