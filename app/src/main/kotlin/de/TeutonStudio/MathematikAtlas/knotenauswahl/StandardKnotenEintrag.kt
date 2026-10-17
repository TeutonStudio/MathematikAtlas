package de.TeutonStudio.MathematikAtlas.knotenauswahl

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.MathematikKnoten.katalog.OperatorKnotenSuchtreffer
import de.TeutonStudio.MathematikKnoten.ÜBERSETZER_ART
import de.TeutonStudio.MathematikKnoten.ÜBERSETZER_ZIEL
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft

enum class StandardSammelAktion { FALTUNG, MENGENDEFINITION, TUPEL_SPALTE, TUPEL_ZEILE }
data class StandardKnotenEintrag(
    val schluessel: String,val vorlage: KnotenVorlage? = null,val aktion: StandardSammelAktion? = null,
    val einfügbar: Boolean = true,val operatorId: String? = null,
) {
    val name: String get() = vorlage?.name ?: when(aktion) {
        StandardSammelAktion.FALTUNG -> "Endliche Faltung"
        StandardSammelAktion.MENGENDEFINITION -> "Mengendefinition"
        StandardSammelAktion.TUPEL_SPALTE -> "Spaltenvektor erzeugen"
        StandardSammelAktion.TUPEL_ZEILE -> "Zeilenvektor erzeugen"
        null -> error("Eintrag ohne Aktion")
    }
    val beschreibung: String get() = vorlage?.beschreibung ?: when(aktion) {
        StandardSammelAktion.FALTUNG -> "Erzeugt einen gekoppelten Faltungskonstruktor und Faltungsdefinator für Index, neutrales Element und Akkumulator."
        StandardSammelAktion.MENGENDEFINITION -> "Erzeugt einen gekoppelten Mengenkonstruktor und Mengendefinator ohne ungültige Direktverbindung."
        StandardSammelAktion.TUPEL_SPALTE -> "Erzeugt ein Tupel und verbindet es mit „Tupel zu Spalte“."
        StandardSammelAktion.TUPEL_ZEILE -> "Erzeugt ein Tupel und verbindet es mit „Tupel zu Zeile“."
        null -> ""
    }
    companion object {
        fun vorlage(v: KnotenVorlage,operator: String? = null,variante: String? = null): StandardKnotenEintrag {
            val key=v.kartenVerweis?.let { "karte/${it.kartenId.wert}" }
                ?: "vorlage/${v.art}/"+(operator?.let { "operator/$it" } ?: variante?.let { "variante/$it" } ?: "standard")
            return StandardKnotenEintrag(key,v,operatorId=operator)
        }
    }
}

/** Ausschließlich stabile Modus-/Operator-IDs, niemals Anzeigenamen oder vollständige Parameter. */
fun stabileAuswahlVariante(parameter: Map<String,String>): String? =
    if (parameter["mengeAuswahl"]?.endsWith(".matrizen") == true) "mengeAuswahl=${parameter.getValue("mengeAuswahl")}" else listOf(
    "operator","eingabeModus","eingabeArt","orientierung","svgOperator","praedikatOperator","struktur","ziel",
    "abflachen.ebenen","relation","methodenAnwendung.ergebnisArt","methodenBereich.operator",
).mapNotNull { key -> parameter[key]?.let { "$key=$it" } }.takeIf { it.isNotEmpty() }?.joinToString(";")

private fun termVariante(art: String,termArt: String?): String? = if(art=="mathematik.termZuMethode") termArt?.let { "termart=$it" } else null
private fun auswahlParameter(art:String,p:Map<String,String>,e:Map<String,KnotenEigenschaft>) =
    if (art == ÜBERSETZER_ART) p + ("ziel" to ((e[ÜBERSETZER_ZIEL] as? KnotenEigenschaft.Text)?.wert ?: "spalte")) else p
fun stabileKnotenVariante(k: KnotenDaten): String? = termVariante(k.art,k.anschlüsse.firstOrNull { it.richtung==AnschlussRichtung.Eingang && it.name=="term" }?.art?.wert) ?: stabileAuswahlVariante(auswahlParameter(k.art,k.parameter,k.eigenschaften))

fun standardVorlagenEinträge(basis: List<KnotenVorlage>,operatoren: List<OperatorKnotenSuchtreffer> = emptyList()): List<StandardKnotenEintrag> {
    val mehrfach=basis.groupingBy { it.art }.eachCount()
    return (operatoren.map { StandardKnotenEintrag.vorlage(it.vorlage,it.operatorId) }+
        basis.map { v -> StandardKnotenEintrag.vorlage(v,variante=if(mehrfach.getValue(v.art)>1) termVariante(v.art,v.anschlüsse.firstOrNull { it.richtung==AnschlussRichtung.Eingang && it.name=="term" }?.art?.wert) ?: stabileAuswahlVariante(auswahlParameter(v.art,v.standardParameter,v.standardEigenschaften)) else null) })
        .distinctBy { e -> e.vorlage!!.let { v -> v.art to auswahlParameter(v.art,v.standardParameter,v.standardEigenschaften).toSortedMap().entries.joinToString("|") { "${it.key}=${it.value}" } } }
}

data class StandardAuswahlTab(val name: String,val eintraege: List<StandardKnotenEintrag>) { val anzahl get()=eintraege.size }
data class StandardAuswahlZeile(val titel: String? = null,val eintrag: StandardKnotenEintrag? = null)
data class StandardKnotenErgebnis(
    val tabs: List<StandardAuswahlTab>,val aktiverTab: StandardAuswahlTab,
    val zeilen: List<StandardAuswahlZeile>,val zustand: KnotenSuchZustand,
)

/** Einzige Ergebnisdarstellung für sichtbare Standardliste UND Statistik. Reihenfolge wie bisher. */
fun standardKnotenErgebnis(
    basis: List<StandardKnotenEintrag>,text: String,herkunft: SuchtextHerkunft,gewünscht: String,
    kompatibel: (KnotenVorlage) -> Boolean,kannTupel: Boolean,kannMenge: Boolean,kannFaltung: Boolean,filter: String?,
): StandardKnotenErgebnis {
    val textTreffer=basis.filter { e -> e.vorlage!!.let { text.isBlank() || it.name.contains(text,true) || it.kategorie.contains(text,true) || it.beschreibung.contains(text,true) } }
    val sichtbar=textTreffer.filter { kompatibel(it.vorlage!!) }
    fun passt(vararg begriffe: String) = text.isBlank() || begriffe.any { it.contains(text,true) }
    val tupel=passt("Spaltenvektor erzeugen","Zeilenvektor erzeugen","Erzeugt ein Tupel und verbindet es mit Tupel zu Spalte oder Tupel zu Zeile")
    val menge=passt("Mengendefinition","Mengenkonstruktor","Mengendefinator","Erzeugt ein gekoppeltes Paar für eine symbolische Menge mit Prädikat")
    val faltung=passt("Endliche Faltung","Faltungskonstruktor","Faltungsdefinator","Bindet Index und Akkumulator für iterierte Operatoren")
    fun aktion(key: String,a: StandardSammelAktion,enabled: Boolean) = StandardKnotenEintrag("aktion/$key",aktion=a,einfügbar=enabled)
    val m=if(menge) listOf(aktion("mengendefinition",StandardSammelAktion.MENGENDEFINITION,kannMenge)) else emptyList()
    val f=if(faltung) listOf(aktion("endliche-faltung",StandardSammelAktion.FALTUNG,kannFaltung)) else emptyList()
    val t=if(tupel) listOf(aktion("tupel-spalte",StandardSammelAktion.TUPEL_SPALTE,kannTupel),aktion("tupel-zeile",StandardSammelAktion.TUPEL_ZEILE,kannTupel)) else emptyList()
    fun filterVorlagen(p: (KnotenVorlage) -> Boolean) = sichtbar.filter { p(it.vorlage!!) }
    val tabs=listOf(
        StandardAuswahlTab("Alle",f+m+filterVorlagen { it.kategorie !in kartenKategorien }),
        StandardAuswahlTab("Rechnen",f+filterVorlagen(::istRechenVorlage)),
        StandardAuswahlTab("Zahlen",filterVorlagen { it.art in setOf("mathematik.zahl","mathematik.variable") }),
        StandardAuswahlTab("Mengen",m+filterVorlagen { it.kategorie=="Mengen" && it.art !in mengenrechnungsArten }),
        StandardAuswahlTab("Tupel",t+filterVorlagen { it.art=="mathematik.tupel" }),
        StandardAuswahlTab("Abbildungen",filterVorlagen { it.kategorie in abbildungsKategorien }),
        StandardAuswahlTab("Vektoren",filterVorlagen { it.kategorie=="Vektoren" }),
        StandardAuswahlTab("Matrizen",filterVorlagen { it.kategorie=="Matrizen" }),
        StandardAuswahlTab("Geometrie",filterVorlagen { it.kategorie.startsWith("Geometrie:") }),
        StandardAuswahlTab("Aussagen",filterVorlagen { it.kategorie=="Aussage" || it.kategorie.startsWith("Aussagen:") }),
        StandardAuswahlTab("Karten",filterVorlagen { it.kategorie in kartenKategorien }),
    )
    val aktiv=tabs.firstOrNull { it.name==gewünscht && it.anzahl>0 } ?: tabs.firstOrNull { it.anzahl>0 } ?: tabs.first()
    val zeilen=buildList<StandardAuswahlZeile> {
        for(e in aktiv.eintraege.filter { it.aktion!=null }) {
            val titel=when(e.aktion) { StandardSammelAktion.FALTUNG -> "Iterierte Operatoren";StandardSammelAktion.MENGENDEFINITION -> "Mengendefinition";else -> "Vektor aus Tupel" }
            if(lastOrNull()?.eintrag?.aktion!=StandardSammelAktion.TUPEL_SPALTE || e.aktion!=StandardSammelAktion.TUPEL_ZEILE) add(StandardAuswahlZeile(titel=titel))
            add(StandardAuswahlZeile(eintrag=e))
        }
        val vorlagen=aktiv.eintraege.filter { it.vorlage!=null }
        if(aktiv.name=="Alle") {
            val gruppen=vorlagen.groupBy { kategorieAnzeige(it.vorlage!!) }
            gruppen.keys.sortedWith(compareBy({ kategorienReihenfolge.indexOf(it).let { n -> if(n<0) Int.MAX_VALUE else n } },{ it })).forEach { gruppe ->
                add(StandardAuswahlZeile(titel=gruppe));addAll(gruppen.getValue(gruppe).sortedBy { it.name }.map { StandardAuswahlZeile(eintrag=it) })
            }
        } else addAll(vorlagen.sortedBy { it.name }.map { StandardAuswahlZeile(eintrag=it) })
    }
    val angebote=zeilen.mapNotNull { it.eintrag }.map { KnotenAngebot(it.schluessel,it.einfügbar) }
    return StandardKnotenErgebnis(tabs,aktiv,zeilen,KnotenSuchZustand(text,herkunft,textTreffer.size+m.size+f.size+t.size,
        sichtbar.size+(m+f+t).count { it.einfügbar },gewünscht,aktiv.name,angebote,filter))
}
private val mengenrechnungsArten=setOf("mathematik.vereinigung","mathematik.schnitt","mathematik.differenz","mathematik.kartesischesProdukt","mathematik.mächtigkeit","mathematik.iterierteVereinigung","mathematik.iterierterSchnitt","mathematik.iteriertesKartesischesProdukt","mathematik.abbild")
private val rechnenKategorien=setOf("Rechnen","Analysis","Algebra","Zahlen","Operatoren","Steuerung")
private val abbildungsKategorien=setOf("Methoden","Abbildungen")
private val kartenKategorien=setOf("Gruppen","Gespeicherte Karten")
private fun istRechenVorlage(v: KnotenVorlage)=v.kategorie in rechnenKategorien || v.art in mengenrechnungsArten
private fun kategorieAnzeige(v: KnotenVorlage): String = when {
    istRechenVorlage(v) -> "Rechnen"
    v.kategorie.startsWith("Aussagen:") -> v.kategorie.substringAfter(": ")
    v.kategorie in abbildungsKategorien -> "Abbildungen"
    v.kategorie in kartenKategorien -> "Karten"
    else -> v.kategorie
}
private val kategorienReihenfolge=listOf("Rechnen","Mengen","Abbildungen","Vektoren","Matrizen","Geometrie: Räume","Geometrie: Grundobjekte","Geometrie: Konstruktionen","Geometrie: Relationen","Geometrie: Struktur","Geometrie: Mengen","Geometrie: Transformationen","Geometrie: Darstellung","Aussagenlogik","Mengenprädikate","Zahlenprädikate","Aussagenprädikate","Aussage","Karten")
