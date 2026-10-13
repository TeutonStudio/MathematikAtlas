package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

@Composable
fun FormelKartenAktionen(karte:KartenDaten,knoten:KnotenDaten,ändern:(KartenAktion)->Unit) {
    if(knoten.art!=ZAHLENRECHNER_ART && StrukturRechnerKnotenFamilie.fuerKnotenArt(knoten.art)==null) return
    var fehler by remember(knoten) {mutableStateOf<String?>(null)}
    fun versuch(a:()->KartenAktion) {runCatching {ändern(a())}.onFailure {fehler=it.message}}
    Text("Formel und Karte",style=MaterialTheme.typography.titleSmall)
    FlowRow {
        TextButton(onClick={versuch {KartenAktion.TeilgraphErsetzen(planeFormelKnotenEntfalten(karte,knoten))}}) {Text("Als Knotenkarte bearbeiten")}
        TextButton(onClick={versuch {KartenAktion.TeilgraphErsetzen(planeFormelKnotenZusammenfassen(karte,knoten))}}) {Text("Als Formel übernehmen")}
        TextButton(onClick={versuch {
            val aus=knoten.anschlüsse.single {it.richtung==AnschlussRichtung.Ausgang}
            val m=konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt(knoten.position.x+340f,knoten.position.y)),"methode")
            val v=VerbindungDaten(von=AnschlussVerweis(knoten.id,aus.id),zu=AnschlussVerweis(m.id,m.anschlüsse.single {it.name=="wert"}.id))
            KartenAktion.KartenInhaltErsetzen(karte.copy(knoten=karte.knoten+m,verbindungen=karte.verbindungen+v))
        }}) {Text("Methode bilden")}
    }
    fehler?.let {Text(it,color=MaterialTheme.colorScheme.error)}
}

@Composable
fun ErgebnisVertragsInhalt(ergebnis:KnotenAuswertungsErgebnis?) {
    if(ergebnis==null) return
    val clipboard=LocalClipboardManager.current
    ergebnis.fehler?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    ergebnis.ausgaben.forEach { (name,w) ->
        var offen by remember(name,w) {mutableStateOf(false)}
        var fehler by remember(name,w) {mutableStateOf<String?>(null)}
        TextButton(onClick={offen=!offen}) {Text("$name · Ergebnisdetails")}
        if(offen) {
            Text(strukturTitel(w.objekt))
            LatexText(w.anzeigeLatex())
            if(w.annahmen.isEmpty()) Text("Keine offenen Voraussetzungen") else {Text("Voraussetzungen");w.annahmen.forEach {LatexText(it.zuLatex())}}
            w.zielMenge?.let {Text("Zielträger");LatexText(it.zuLatex())}
            w.werteVorrat?.let {Text("Wertebereich");LatexText(it.zuLatex())}
            if(w.zielMenge==null && w.werteVorrat==null) Text("Wertebereich unbekannt")
            w.variablenQuellen.forEach {q -> Text("${q.name} · ${if(q.alsMethodenParameter) "freier Parameter" else "Außenquelle"}");LatexText(q.werteVorrat.zuLatex())}
            (w.objekt as? MathematischeMethode)?.let {MethodenVertragsInhalt(it)}
            w.übersetzungen.aktuell?.let {z -> Text("Aktuelles Übersetzungszeugnis");z.hinweise().forEach {Text(it)}}
            w.übersetzungen.herkunft.forEachIndexed { i,z ->
                var herkunftOffen by remember(z) {mutableStateOf(false)}
                TextButton(onClick={herkunftOffen=!herkunftOffen}) {Text("Herkunft ${i+1} · kein Nachweis für das aktuelle Ergebnis")}
                if(herkunftOffen) z.hinweise().forEach {Text(it)}
            }
            FlowRow {
                TextButton(onClick={runCatching {
                    val f=w.alsGebundeneErgebnisFormel()
                    clipboard.setText(AnnotatedString("ATLAS-FORMEL-1\n"+FormelKontextCodec.kodieren(f)))
                }.onFailure {fehler=it.message}}) {Text("Formelpaket kopieren")}
                TextButton(onClick={runCatching {clipboard.setText(AnnotatedString("ATLAS-WERT-1\n"+ÜbersetzungsCodec.kodieren(w.alsÜbertragenerMathematischerWert())))}.onFailure {fehler=it.message}}) {Text("Wertpaket kopieren")}
                TextButton(onClick={clipboard.setText(AnnotatedString(w.anzeigeLatex()))}) {Text("LaTeX als Darstellung kopieren")}
            }
            fehler?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        }
    }
}
@Composable
fun MethodenVertragsInhalt(m:MathematischeMethode) {
    Text("Methodensignatur",style=MaterialTheme.typography.titleSmall)
    val s=m.mathematischeSignatur
    s.argumente.forEachIndexed {i,k -> Text("Argument ${i+1}: ${k.parameter.name}");LatexText(k.definitionsMenge.zuLatex())}
    if(s.argumente.isEmpty()) Text("Nullstellige Methode · Eingabetupel ()")
    s.ergebnisse.forEachIndexed {i,k -> Text("Ergebnis ${i+1}: ${k.name}");LatexText(k.zielMenge.zuLatex())}
    if(s.ergebnisse.isEmpty()) Text("Ergebnistupel ()")
    Text("Gemeinsamer Definitionsraum");LatexText(s.definitionsRaum.zuLatex())
    m.voraussetzungen.forEach {LatexText(it.zuLatex())}
}
fun formelTypFürWert(w:AtlasWert):FormelTyp = when(w) {is ZahlAusdruck -> FormelTyp.ZAHL;is Tupel -> FormelTyp.TUPEL;is OrientierterVektor -> FormelTyp.VEKTOR;is Matrix -> FormelTyp.MATRIX;is Tensor -> FormelTyp.TENSOR;is MengenAusdruck -> FormelTyp.MENGE;is Aussage -> FormelTyp.AUSSAGE;is Methode -> FormelTyp.METHODE;else -> FormelTyp.OBJEKT}
fun strukturTitel(w:AtlasWert):String = when(w) {is Tupel -> "Tupel · ${w.anzahl} Komponenten";is ZeilenVektor -> "Zeilenvektor · ${w.werte.size} Komponenten";is SpaltenVektor -> "Spaltenvektor · ${w.werte.size} Komponenten";is Matrix -> "Matrix · ${w.zeilenAnzahl} × ${w.spaltenAnzahl}";is Tensor -> "Tensor · ${w.dimensionen.joinToString(" × ")}";is Methode -> "Methode";is ZahlAusdruck -> "Skalar";else -> "Mathematisches Objekt"}

@Composable
fun MethodenProjektionsEinstellungen(k:KnotenDaten,r:KnotenAuswertungsErgebnis?,ändern:(KnotenDaten)->Unit) {
    k.anschlüsse.filter {it.richtung==AnschlussRichtung.Ausgang && it.art==MathematikAnschlussArten.Methode.id}.forEach {p ->
        Text("${p.name} · Methodenschnittstelle",style=MaterialTheme.typography.titleSmall)
        Auswahl("Argumentanzeige",listOf(METHODEN_ARGUMENTPROJEKTION_SEPARIERT,METHODEN_ARGUMENTPROJEKTION_TUPEL),k.methodenAusgangArgumentprojektion(p.name),{if(it==METHODEN_ARGUMENTPROJEKTION_TUPEL) "Gesamttupel" else "Getrennte Argumente"}) {v ->ändern(k.copy(parameter=k.parameter+(methodenAusgangArgumentprojektionSchlüssel(p.name) to v)))}
        Auswahl("Ergebnisanzeige",listOf(METHODEN_ERGEBNISPROJEKTION_DIREKT,METHODEN_ERGEBNISPROJEKTION_TUPEL),k.methodenAusgangErgebnisprojektion(p.name),{if(it==METHODEN_ERGEBNISPROJEKTION_TUPEL) "Ergebnistupel" else "Einzelausgabe, sofern möglich"}) {v ->ändern(k.copy(parameter=k.parameter+(methodenAusgangErgebnisprojektionSchlüssel(p.name) to v)))}
    }
    if(k.art!=METHODEN_AUFRUF_ART) return
    val m=r?.eingänge?.get("methode")?.objekt as? MathematischeMethode
    val getrennt=k.parameter[METHODEN_AUFRUF_ARGUMENTPROJEKTION]!=METHODEN_ARGUMENTPROJEKTION_TUPEL
    Text("Argumentbindung",style=MaterialTheme.typography.titleSmall)
    Row {Checkbox(checked=k.parameter[METHODEN_AUFRUF_PARTIELL]=="true",enabled=k.parameter[METHODEN_AUFRUF_PARTIELL]=="true" || getrennt && m!=null,onCheckedChange={ändern(k.copy(parameter=k.parameter+(METHODEN_AUFRUF_PARTIELL to it.toString()))) });Text("Partiell binden · Ergebnis ist eine Methode")}
    if(!getrennt) Text("Partielle Bindung benötigt getrennte Argumente. Die Projektion wird am Methodenausgang gewählt.")
    if(m!=null) {
        m.signatur.argumente.forEachIndexed {i,c ->
            val port=k.anschlüsse.singleOrNull {it.semantischeId==c.id} ?: k.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang && it.name!="methode"}.sortedBy {it.reihenfolge}.getOrNull(i)
            Text("${c.name} · ${typTitel(c.typ)} · ${if(port?.name in r.eingänge) "gebunden" else "offen"}")
        }
        MethodenVertragsInhalt(m)
    }
}

@Composable
fun VisualisierungsVertragsEinstellungen(k:KnotenDaten,r:KnotenAuswertungsErgebnis?,ändern:(KnotenDaten)->Unit) {
    if(k.art!="mathematik.visualisierung") return
    val config=de.TeutonStudio.MathematikKnoten.visualisierung.modell.VisualisierungsKonfiguration.aus(k.eigenschaften)
    fun setze(c:de.TeutonStudio.MathematikKnoten.visualisierung.modell.VisualisierungsKonfiguration) {ändern(k.copy(eigenschaften=k.eigenschaften+c.zuEigenschaften()))}
    val w=r?.eingänge?.values?.firstOrNull()?.objekt
    Text("Koordinatenvertrag",style=MaterialTheme.typography.titleSmall)
    if(w!=null) Text(strukturTitel(w))
    Text("Die Koordinatenansicht verändert den ursprünglichen Wert nicht. Verschachtelte Strukturen benötigen eine ausdrücklich gewählte Projektion.")
    Auswahl("Methodendarstellung",de.TeutonStudio.MathematikKnoten.visualisierung.modell.MethodenDarstellungsModus.entries,config.methodenModus,{it.name}) {setze(config.copy(methodenModus=it))}
    var projektion by remember(k) {mutableStateOf(config.ausgabeKoordinaten.joinToString(","))}
    var schnitte by remember(k) {mutableStateOf(config.festeSchnitte.entries.joinToString(";") {"${it.key}=${it.value}"})}
    var fehler by remember(k) {mutableStateOf<String?>(null)}
    OutlinedTextField(projektion,{projektion=it},label={Text("Explizite Koordinaten · zwei oder drei Indizes ab 1")})
    TextButton(onClick={runCatching {
        val ids=if(projektion.isBlank()) emptyList() else projektion.split(',').map {it.trim().toInt()}
        require(ids.isEmpty() || ids.size in 2..3 && ids.distinct().size==ids.size && ids.all {it>0}) {"Zwei oder drei verschiedene positive Koordinatenindizes erforderlich."}
        setze(config.copy(ausgabeKoordinaten=ids,dimensionsModus=if(ids.isEmpty()) de.TeutonStudio.MathematikKnoten.visualisierung.modell.DimensionsModus.Automatisch else de.TeutonStudio.MathematikKnoten.visualisierung.modell.DimensionsModus.Manuell,dimension=if(ids.size==3) de.TeutonStudio.MathematikKnoten.visualisierung.modell.RaumDimension.R3 else if(ids.size==2) de.TeutonStudio.MathematikKnoten.visualisierung.modell.RaumDimension.R2 else config.dimension))
    }.onFailure {fehler=it.message}}) {Text("Koordinatenprojektion übernehmen")}
    OutlinedTextField(schnitte,{schnitte=it},label={Text("Feste Parameter · Name=Wert; …")})
    TextButton(onClick={runCatching {
        val paare=if(schnitte.isBlank()) emptyList() else schnitte.split(';').map {val p=it.split('=',limit=2);require(p.size==2 && p[0].trim().isNotEmpty());p[0].trim() to p[1].trim().toDouble().also {v ->require(v.isFinite())}}
        require(paare.map {it.first}.distinct().size==paare.size) {"Ein Parameter wurde mehrfach angegeben."}
        setze(config.copy(festeSchnitte=paare.toMap()))
    }.onFailure {fehler=it.message}}) {Text("Parameterbindung übernehmen")}
    fehler?.let {Text(it,color=MaterialTheme.colorScheme.error)}
}
