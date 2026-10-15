package de.TeutonStudio.MathematikAtlas.knotenauswahl

import java.text.Normalizer
import java.util.UUID

const val KNOTEN_NUTZUNG_SCHEMA = 1
const val SUCH_RUHE_MS = 400L
fun nutzungsId(): String = UUID.randomUUID().toString()

enum class SitzungsAbschluss { OFFEN, AUSWAHL, ABBRUCH, MODUSWECHSEL, UNTERBROCHEN, DEAKTIVIERT }
enum class SuchtextHerkunft { OHNE_FILTER, GETIPPT, UEBERNOMMEN, ZURUECKGESETZT }
enum class SuchAnlass { OEFFNEN, SUCHRUHE, AUSWAHL, ZURUECKSETZEN, TABWECHSEL, SCHLIESSEN, MODUSWECHSEL, UNTERBRECHUNG }
data class KnotenNutzungsEinstellungen(val erfassen: Boolean = true,val suchtexte: Boolean = true)
data class KnotenAngebot(val schluessel: String,val einfügbar: Boolean)
data class KnotenSuchZustand(
    val originaltext: String,
    val herkunft: SuchtextHerkunft,
    val textTreffer: Int,
    val einfügbareTreffer: Int,
    val gewünschterTab: String,
    val aktiverTab: String,
    val angebote: List<KnotenAngebot>,
    val anschlussFilter: String? = null,
) {
    fun ohneText() = copy(originaltext="")
}
data class KnotenSuchschritt(
    val id: String,val reihenfolge: Int,val zeit: Long,val zustand: KnotenSuchZustand,
    val normalisiert: String?,val anlass: SuchAnlass,val sichtbar: Set<String> = emptySet(),
    val textGespeichert: Boolean = true,val textGekürzt: Boolean = false,
) {
    fun ohneText() = copy(zustand=zustand.ohneText(),normalisiert=null,textGespeichert=false)
}
data class ErzeugterKnotenTyp(val art: String,val variante: String? = null)
data class KnotenAuswahlAuftrag(
    val id: String,val sitzung: String,val eintrag: String,val rang: Int,val zustand: KnotenSuchZustand,
    val kartenVersion: Int? = null,
)
data class KnotenAuswahlErgebnis(
    val id: String,val eintrag: String,val suchschritt: String?,val rang: Int,val zeit: Long,
    val erfolgreich: Boolean,val erzeugt: List<ErzeugterKnotenTyp>,val kartenVersion: Int? = null,
)
data class KnotenAuswahlSitzung(
    val id: String,val beginn: Long,val öffnungsart: String,val appVersion: String,
    val abschluss: SitzungsAbschluss = SitzungsAbschluss.OFFEN,val ende: Long? = null,
    val suchschritte: List<KnotenSuchschritt> = emptyList(),val auswahlen: List<KnotenAuswahlErgebnis> = emptyList(),
)
data class EintragsNutzung(val anzahl: Long,val zuletzt: Long,val tage: Map<String,Long>)
data class KnotenNutzungsStatistik(val eintraege: Map<String,EintragsNutzung> = emptyMap())
data class DirekteSuchAuswahl(val normalisiert: String,val eintrag: String,val anzahl: Int)
data class KnotenNutzungsDaten(
    val schema: Int = KNOTEN_NUTZUNG_SCHEMA,
    val einstellungen: KnotenNutzungsEinstellungen = KnotenNutzungsEinstellungen(),
    val sitzungen: List<KnotenAuswahlSitzung> = emptyList(),
    val statistik: KnotenNutzungsStatistik = KnotenNutzungsStatistik(),
) {
    /** Ausschließlich direkte Zuordnung, niemals vorausgehende Umformulierungen als Synonyme behandeln. */
    fun direkteSuchAuswahlen(): List<DirekteSuchAuswahl> = sitzungen.flatMap { s ->
        val schritte=s.suchschritte.associateBy { it.id }
        s.auswahlen.filter { it.erfolgreich }.mapNotNull { a ->
            schritte[a.suchschritt]?.normalisiert?.takeIf { it.isNotBlank() }?.let { it to a.eintrag }
        }
    }.groupingBy { it }.eachCount().map { (k,n) -> DirekteSuchAuswahl(k.first,k.second,n) }
}
data class NutzungsGrenzen(val tage: Int = 90,val schritte: Int = 10_000,val sitzungen: Int = 1000,val dateiBytes: Int = 2*1024*1024) {
    init { require(tage>0 && schritte>0 && sitzungen>0 && dateiBytes>=1024) }
}
fun normalisiereSuchtext(text: String): String = Normalizer.normalize(text,Normalizer.Form.NFC).trim()
