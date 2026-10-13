package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.*
import java.util.UUID

private val LocalOffenerFormelEntwurf=compositionLocalOf<(Any,Boolean)->Unit> { {_,_->} }
@Composable
private fun OffenerFormelEntwurf(offen:Boolean) {
    val id=remember {Any()};val melden=LocalOffenerFormelEntwurf.current
    SideEffect {melden(id,offen)}
    DisposableEffect(id) {onDispose {melden(id,false)}}
}

/** Gemeinsamer Kontexteditor; Änderungen bleiben bis Übernehmen im Entwurf. */
@Composable
fun GebundenerFormelBauerDialog(start:GebundeneFormel,startZeichnung:KnotenEigenschaft?,schließen:()->Unit,übernehmen:(GebundeneFormel,KnotenEigenschaft)->Unit) {
    val clipboard=androidx.compose.ui.platform.LocalClipboardManager.current
    val entwurf=remember(start) { FormelBearbeitungsEntwurf(start) }
    var revision by remember { mutableIntStateOf(0) }
    var text by remember(start) { mutableStateOf(FormelLatexCodec.exportiere(start.wurzel)) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var auswahl by remember { mutableStateOf(start.wurzel.id) }
    val zeichnung=remember(startZeichnung) { FormelZeichnungsEntwurf(startZeichnung) }
    val f=remember(revision) { entwurf.formel }
    fun geändert() { revision++;text=FormelLatexCodec.exportiere(entwurf.formel.wurzel);fehler=null }
    fun versuch(aktion:()->Unit) { runCatching(aktion).onSuccess { geändert() }.onFailure { fehler=it.message } }
    fun ersetzeTeil(neu:FormelAusdruck, bindungen:Map<String,String> = emptyMap()) {
        fun ändere(e:FormelAusdruck):FormelAusdruck = if(e.id==auswahl) neu else if(e is FormelAusdruck.Operation) e.copy(argumente=e.argumente.map { it.copy(ausdruck=ändere(it.ausdruck)) }) else e
        entwurf.mitWurzel(ändere(f.wurzel),bindungen);auswahl=neu.id
    }
    fun id()=UUID.randomUUID().toString()
    fun platz(typ:FormelTyp)=FormelAusdruck.Platzhalter(id(),"argument","Ausdruck",typ)
    val offene=remember {mutableStateMapOf<Any,Boolean>()}
    val gültig=offene.isEmpty() && fehler==null && runCatching { entwurf.geprüft() }.isSuccess
    CompositionLocalProvider(LocalOffenerFormelEntwurf provides {id,offen -> if(offen) offene[id]=true else offene.remove(id)}) {
    FormelEditorDialogRahmen("Formel und Bindungen","Symbole, Typen und Voraussetzungen gehören zur Formel.",zeichnung,gültig,schließen,
        übernehmen={ übernehmen(entwurf.geprüft(),zeichnung.übernehmen()) },
        vorschau={ LatexText(FormelLatexCodec.exportiere(f.wurzel)) },inhalt={
            OutlinedTextField(text,onValueChange={ text=it; runCatching { entwurf.importiere(it) }.onSuccess { revision++;fehler=null;auswahl=entwurf.formel.wurzel.id }.onFailure { e -> fehler=e.message } },label={Text("LaTeX mit deklarierten Symbolen")},modifier=Modifier.fillMaxWidth())
            FlowRow {
                TextButton(onClick={versuch { val paket=clipboard.getText()?.text ?: error("Die Zwischenablage ist leer.")
                    when {paket.startsWith("ATLAS-FORMEL-1\n") -> entwurf.übernehme(FormelKontextCodec.dekodieren(paket.substringAfter('\n')))
                        paket.startsWith("ATLAS-WERT-1\n") -> {val w=ÜbersetzungsCodec.dekodiereWert(paket.substringAfter('\n'));require(w.formel!=null || (w.wert.freieMethodenParameter()+w.übersetzungen.kontext?.annahmen.orEmpty().flatMap {it.freieMethodenParameter()}).isEmpty()) {"Dieses historische Wertpaket besitzt freie Quellen ohne vollständigen Formelbindungsvertrag. Der bestehende Entwurf bleibt erhalten."};entwurf.übernehme(w.formel ?: GebundeneFormel(FormelAusdruck.Literal(id(),w.wert,formelTypFürWert(w.wert),w.übersetzungen),emptyList(),emptyMap(),annahmen=w.übersetzungen.kontext?.annahmen.orEmpty()))}
                        else -> error("Kein unterstütztes Formel- oder Wertpaket.") }
                    auswahl=entwurf.formel.wurzel.id
                }}) {Text("Formel- oder Wertpaket einfügen")}
                TextButton(enabled=gültig,onClick={versuch {clipboard.setText(androidx.compose.ui.text.AnnotatedString("ATLAS-FORMEL-1\n"+FormelKontextCodec.kodieren(entwurf.geprüft())))}}) {Text("Formelpaket kopieren")}
            }
            fehler?.let { Text(it,color=MaterialTheme.colorScheme.error) }
            Text("Ausdruck auswählen",style=MaterialTheme.typography.titleSmall)
            fun teile(e:FormelAusdruck):List<FormelAusdruck> = listOf(e)+(if(e is FormelAusdruck.Operation) e.argumente.flatMap { teile(it.ausdruck) } else emptyList())
            teile(f.wurzel).forEach { e -> TextButton(onClick={auswahl=e.id}) { Text((if(e.id==auswahl) "✓ " else "")+when(e) { is FormelAusdruck.Platzhalter -> "Offener Ausdruck · ${e.typ}";else -> FormelLatexCodec.exportiere(e) }) } }
            Text("Operator einsetzen",style=MaterialTheme.typography.titleSmall)
            var kategorie by remember { mutableStateOf(FormelTastenKategorie.GRUNDRECHNUNG) }
            Auswahl("Operatorfamilie",FormelTastenKategorie.entries,kategorie,{it.name}) {kategorie=it}
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FormelTastatur.standard.filter {it.kategorie==kategorie}.forEach { taste ->
                    TextButton(onClick={versuch {
                        val literal=taste.literal
                        if(literal!=null) ersetzeTeil(FormelAusdruck.Literal(id(),literal,taste.ergebnisTyp))
                        else ersetzeTeil(FormelAusdruck.Operation(id(),requireNotNull(taste.operatorId),taste.argumentRollen.mapIndexed { i,r -> FormelArgument(r,i,platz(FormelTyp.ZAHL)) },taste.ergebnisTyp))
                    }}) {Text(taste.beschriftung)}
                }
            }
            var strukturfamilie by remember {mutableStateOf(StrukturFormelFamilie.VEKTOR)}
            Auswahl("Strukturrechner",StrukturFormelFamilie.entries,strukturfamilie,{it.name}) {strukturfamilie=it}
            FlowRow {
                StrukturFormelTastatur.fuer(strukturfamilie).forEach {taste ->
                    TextButton(onClick={versuch {ersetzeTeil(FormelAusdruck.Operation(id(),taste.operatorId,taste.argumente.mapIndexed {i,a ->FormelArgument(a.rolle,i,platz(a.typ))},taste.ergebnisTyp))}}) {Text(taste.beschriftung)}
                }
            }
            Text("Struktur einsetzen",style=MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={versuch { ersetzeTeil(FormelAusdruck.Operation(id(),"tupel.konstruktion",emptyList(),FormelTyp.TUPEL)) }}) { Text("Leeres Tupel") }
                OutlinedButton(onClick={versuch { ersetzeTeil(FormelAusdruck.Operation(id(),"tupel.konstruktion",listOf(FormelArgument("komponente",0,platz(FormelTyp.OBJEKT))),FormelTyp.TUPEL)) }}) { Text("Einertupel") }
                for(n in listOf(2,3)) OutlinedButton(onClick={versuch { ersetzeTeil(FormelAusdruck.Operation(id(),"tupel.konstruktion",List(n) { FormelArgument("komponente${it+1}",it,platz(FormelTyp.OBJEKT)) },FormelTyp.TUPEL)) }}) { Text("Tupel mit $n Komponenten") }
            }
            var literal by remember { mutableStateOf("0") }
            OutlinedTextField(literal,{literal=it},label={Text("Zahl einsetzen")})
            TextButton(onClick={versuch { ersetzeTeil(FormelAusdruck.Literal(id(),RationaleZahl.parse(literal),FormelTyp.ZAHL)) }}) { Text("Zahl übernehmen") }
            f.quellen.forEach { q ->
                FlowRow {
                    TextButton(onClick={versuch { val v=FormelAusdruck.Variable(id(),q.anzeigeName,typ=q.typ);ersetzeTeil(v,mapOf(v.id to q.id)) }}) { Text("${q.anzeigeName} einsetzen") }
                    if(q.methodenSignatur!=null) TextButton(onClick={versuch {
                        val signatur=q.methodenSignatur!!
                        val m=FormelAusdruck.Variable(id(),q.anzeigeName,typ=FormelTyp.METHODE)
                        ersetzeTeil(FormelAusdruck.Operation(id(),"methode.anwenden",listOf(FormelArgument("methode",0,m))+signatur.argumente.mapIndexed { i,k -> FormelArgument(k.id,i+1,platz(formelTypFürTyp(k.typ))) },FormelTyp.TUPEL),mapOf(m.id to q.id))
                    }}) { Text("${q.anzeigeName} anwenden") }
                }
            }
            var index by remember { mutableStateOf("1") };var projektionsTyp by remember { mutableStateOf(FormelTyp.ZAHL) }
            OutlinedTextField(index,{index=it},label={Text("Tupelkomponente (ab 1)")})
            Auswahl("Komponententyp",FormelTyp.entries,projektionsTyp,{it.name}) { projektionsTyp=it }
            TextButton(onClick={versuch { val i=index.toLong();require(i>0);ersetzeTeil(FormelAusdruck.Operation(id(),"tupel.projektion",listOf(FormelArgument("tupel",0,platz(FormelTyp.TUPEL)),FormelArgument("index",1,FormelAusdruck.Literal(id(),RationaleZahl.von(i),FormelTyp.ZAHL))),projektionsTyp)) }}) { Text("Komponentenprojektion einsetzen") }
            Text("Symbole und Voraussetzungen",style=MaterialTheme.typography.titleMedium)
            f.quellen.forEachIndexed { i,q ->
                key(q.id) {SymbolEditor(q,{versuch { entwurf.quelle(it) }})}
                if(q.art==FormelVariablenArt.GEBUNDEN) {
                    var quantor by remember(q.id) {mutableStateOf(f.quantoren.singleOrNull {it.quelleId==q.id}?.quantor ?: FormelQuantorArt.FUER_ALLE)}
                    Auswahl("Quantor",FormelQuantorArt.entries,quantor,{if(it==FormelQuantorArt.FUER_ALLE) "Für alle" else "Es existiert"}) {quantor=it}
                    TextButton(onClick={versuch {entwurf.bindeQuelle(q.id,quantor,auswahl)}}) {Text("An ausgewählten Ausdruck binden")}
                    Text("Gebundene Bereiche bleiben strukturell erhalten; nicht unterstützte Auswertung wird diagnostiziert.")
                }
                FlowRow {
                    TextButton(enabled=i>0,onClick={versuch { entwurf.verschiebeQuelle(q.id,-1) }}) {Text("${q.anzeigeName} nach oben")}
                    TextButton(enabled=i+1<f.quellen.size,onClick={versuch { entwurf.verschiebeQuelle(q.id,1) }}) {Text("${q.anzeigeName} nach unten")}
                    TextButton(enabled=q.id !in f.variablenVorkommen.values,onClick={versuch { entwurf.entferneQuelle(q.id) }}) {Text("${q.anzeigeName} entfernen")}
                }
            }
            var name by remember { mutableStateOf("") }
            OutlinedTextField(name,{name=it},label={Text("Neues Symbol")})
            TextButton(enabled=name.isNotBlank(),onClick={versuch { entwurf.quelle(FormelVariablenQuelle(id(),name.trim(),FormelVariablenArt.FREI,FormelTyp.ZAHL,ReelleZahlen));name="" }}) { Text("Symbol deklarieren") }
            f.annahmen.forEach { a -> Row { LatexText(a.zuLatex(),modifier=Modifier.weight(1f));TextButton(onClick={versuch { entwurf.voraussetzungen(f.annahmen-a) }}) {Text("Entfernen")} } }
            var links by remember { mutableStateOf("h") };var rechts by remember { mutableStateOf("0") };var vergleich by remember { mutableStateOf("≠") }
            OutlinedTextField(links,{links=it},label={Text("Voraussetzung · linke Seite")})
            Auswahl("Vergleich",listOf("=","≠","<","≤",">","≥"),vergleich,{it}) {vergleich=it}
            OutlinedTextField(rechts,{rechts=it},label={Text("Voraussetzung · rechte Seite")})
            TextButton(onClick={versuch {
                entwurf.fügeVoraussetzungHinzu(links,vergleich,rechts)
            }}) { Text("Voraussetzung hinzufügen") }
        })
    }
}

@Composable
internal fun <T> Auswahl(label:String,werte:List<T>,aktuell:T,titel:(T)->String,ändern:(T)->Unit) {
    var offen by remember {mutableStateOf(false)}
    Box { OutlinedButton(onClick={offen=true}) {Text("$label: ${titel(aktuell)}")};DropdownMenu(offen,{offen=false}) {werte.forEach { v -> DropdownMenuItem(text={Text(titel(v))},onClick={offen=false;ändern(v)}) }} }
}
fun formelTypFürTyp(t:TypAusdruck):FormelTyp = when(t) {
    is TypAusdruck.Parameterisiert -> when(t.konstruktor) {MathematischeTypen.Tupel -> FormelTyp.TUPEL;MathematischeTypen.Methode -> FormelTyp.METHODE;MathematischeTypen.ZeilenVektor,MathematischeTypen.SpaltenVektor -> FormelTyp.VEKTOR;MathematischeTypen.Matrix -> FormelTyp.MATRIX;MathematischeTypen.Tensor -> FormelTyp.TENSOR;MathematischeTypen.Menge -> FormelTyp.MENGE;else -> FormelTyp.OBJEKT}
    else -> when {MathematischeTypen.typSystem.prüfe(t,TypAusdruck.Atom(MathematischeTypen.Zahl))==TypPrüfung.Kompatibel -> FormelTyp.ZAHL;t==TypAusdruck.Atom(MathematischeTypen.Menge) -> FormelTyp.MENGE;t==TypAusdruck.Atom(MathematischeTypen.Aussage) -> FormelTyp.AUSSAGE;else -> FormelTyp.OBJEKT}
}
@Composable
private fun SymbolEditor(q:FormelVariablenQuelle,ändern:(FormelVariablenQuelle)->Unit) {
    var name by remember(q.id,q.anzeigeName) {mutableStateOf(q.anzeigeName)}
    OffenerFormelEntwurf(name!=q.anzeigeName)
    OutlinedTextField(name,{name=it},label={Text("Symbolname")},isError=name.isBlank())
    TextButton(enabled=name.isNotBlank() && name!=q.anzeigeName,onClick={ändern(q.copy(anzeigeName=name.trim()))}) {Text("Namen übernehmen")}
    Auswahl("Bindungsart",FormelVariablenArt.entries,q.art,{it.name}) {ändern(q.copy(art=it))}
    Auswahl("Rolle",FormelSymbolRolle.entries,q.rolle,{it.name}) { rolle ->
        val typ=when(rolle) {FormelSymbolRolle.METHODE -> FormelTyp.METHODE;FormelSymbolRolle.MENGE -> FormelTyp.MENGE;FormelSymbolRolle.AUSSAGE -> FormelTyp.AUSSAGE;FormelSymbolRolle.STRUKTUR -> FormelTyp.TUPEL;else -> FormelTyp.ZAHL}
        val sig=if(rolle==FormelSymbolRolle.METHODE) q.methodenSignatur ?: MethodenSignatur(listOf(MethodenKomponente(UUID.randomUUID().toString(),"x",0,ReelleZahlen.elementTypAusdruck())),listOf(MethodenKomponente(UUID.randomUUID().toString(),"wert",0,ReelleZahlen.elementTypAusdruck()))) else null
        ändern(q.copy(art=if(rolle==FormelSymbolRolle.KONSTANTE) FormelVariablenArt.EXTERN else q.art,rolle=rolle,typ=typ,werteVorrat=if(rolle in listOf(FormelSymbolRolle.WERT,FormelSymbolRolle.KONSTANTE)) q.werteVorrat else null,methodenSignatur=sig,vollstaendigerTyp=sig?.typAusdruck ?: if(typ==FormelTyp.TUPEL) TypAusdruck.Parameterisiert(MathematischeTypen.Tupel,emptyList()) else TypAusdruck.Unbekannt))
    }
    val räume=if(q.rolle==FormelSymbolRolle.METHODE) listOf(null,q.werteVorrat).distinct() else listOf<MengenAusdruck?>(null,ReelleZahlen,RationaleZahlen,GanzeZahlen,NatürlicheZahlen,KomplexeZahlen)+listOfNotNull(q.werteVorrat).filterNot { it in listOf(ReelleZahlen,RationaleZahlen,GanzeZahlen,NatürlicheZahlen,KomplexeZahlen) }
    Auswahl("Wertebereich",räume,q.werteVorrat,{it?.zuLatex() ?: "Unbekannt"}) {ändern(q.copy(werteVorrat=it,vollstaendigerTyp=q.methodenSignatur?.typAusdruck ?: it?.elementTypAusdruck() ?: q.vollstaendigerTyp))}
    BereichEditor(q.werteVorrat,q.vollstaendigerTyp) {ändern(q.copy(werteVorrat=it))}
    if(q.methodenSignatur!=null) SignaturEditor(q.methodenSignatur!!) { s -> ändern(q.copy(methodenSignatur=s,vollstaendigerTyp=s.typAusdruck)) }
    else TypEditor("Typ",q.vollstaendigerTyp) { ändern(q.copy(vollstaendigerTyp=it,typ=formelTypFürTyp(it))) }
}
@Composable
internal fun SignaturEditor(s:MethodenSignatur,ändern:(MethodenSignatur)->Unit) {
    for(argumente in listOf(true,false)) {
        val komponenten=if(argumente) s.argumente else s.ergebnisse
        val label=if(argumente) "Argument" else "Ergebnis"
        fun setze(k:List<MethodenKomponente>) { val neu=k.mapIndexed { i,c -> c.copy(position=i) };ändern(if(argumente) s.copy(argumente=neu) else s.copy(ergebnisse=neu)) }
        Text("${label}e · ${komponenten.size}")
        komponenten.forEachIndexed { i,k ->
            var n by remember(k.id,k.name) {mutableStateOf(k.name)}
            OffenerFormelEntwurf(n!=k.name)
            OutlinedTextField(n,{n=it},label={Text("$label ${i+1} · Name")})
            TextButton(enabled=n.isNotBlank() && n!=k.name,onClick={setze(komponenten.map {if(it.id==k.id) it.copy(name=n.trim()) else it})}) {Text("Komponentenname übernehmen")}
            TypEditor("$label ${i+1} · Typ",k.typ) { typ -> setze(komponenten.map {if(it.id==k.id) it.copy(typ=typ) else it}) }
            FlowRow {TextButton(onClick={setze(komponenten.filterNot {it.id==k.id})}) {Text("$label entfernen")};TextButton(enabled=i>0,onClick={val l=komponenten.toMutableList();l[i]=l[i-1].also { l[i-1]=l[i] };setze(l)}) {Text("$label nach oben")}}
        }
        TextButton(onClick={setze(komponenten+MethodenKomponente(UUID.randomUUID().toString(),"${label.lowercase()}${komponenten.size+1}",komponenten.size,ReelleZahlen.elementTypAusdruck()))}) {Text("$label hinzufügen")}
    }
}
@Composable
internal fun TypEditor(label:String,t:TypAusdruck,tiefe:Int=0,ändern:(TypAusdruck)->Unit) {
    Text("$label: ${typTitel(t)}",style=MaterialTheme.typography.labelLarge)
    if(tiefe>=16) {Text("Weitere Typstruktur bleibt erhalten.");return}
    val strukturTypen=listOf(
        TypAusdruck.Parameterisiert(MathematischeTypen.SpaltenVektor,listOf(ReelleZahlen.elementTypAusdruck(),TypAusdruck.Literal("2"))),
        TypAusdruck.Parameterisiert(MathematischeTypen.ZeilenVektor,listOf(ReelleZahlen.elementTypAusdruck(),TypAusdruck.Literal("2"))),
        TypAusdruck.Parameterisiert(MathematischeTypen.Matrix,listOf(ReelleZahlen.elementTypAusdruck(),TypAusdruck.Literal("2"),TypAusdruck.Literal("2"))),
        TypAusdruck.Parameterisiert(MathematischeTypen.Tensor,listOf(ReelleZahlen.elementTypAusdruck(),TypAusdruck.Literal("2"))),
        TypAusdruck.Parameterisiert(MathematischeTypen.Menge,listOf(ReelleZahlen.elementTypAusdruck())),
        TypAusdruck.Vereinigung(listOf(ReelleZahlen.elementTypAusdruck(),TypAusdruck.Atom(MathematischeTypen.Aussage))),
        TypAusdruck.Variable(TypVariablenId("T")),TypAusdruck.Literal("2"))
    val typen=strukturTypen+listOf<TypAusdruck>(TypAusdruck.Unbekannt,TypAusdruck.Beliebig,ReelleZahlen.elementTypAusdruck(),TypAusdruck.Atom(MathematischeTypen.Menge),TypAusdruck.Atom(MathematischeTypen.Aussage),TypAusdruck.Parameterisiert(MathematischeTypen.Tupel,emptyList()),MethodenSignatur(emptyList(),emptyList()).typAusdruck)
    Auswahl("Typ wählen",typen+listOf(t).filterNot {it in typen},t,{typTitel(it)}) {ändern(it)}
    if(t is TypAusdruck.Literal || t is TypAusdruck.Variable) {
        var text by remember(t) {mutableStateOf(if(t is TypAusdruck.Literal) t.wert else (t as TypAusdruck.Variable).id.wert)}
        OffenerFormelEntwurf(text!=(if(t is TypAusdruck.Literal) t.wert else (t as TypAusdruck.Variable).id.wert))
        OutlinedTextField(text,{text=it},label={Text(if(t is TypAusdruck.Literal) "Dimension oder Typparameter" else "Typvariable")})
        TextButton(enabled=text.isNotBlank(),onClick={ändern(if(t is TypAusdruck.Literal) TypAusdruck.Literal(text.trim()) else TypAusdruck.Variable(TypVariablenId(text.trim())))}) {Text("Typparameter übernehmen")}
    }
    if(t is TypAusdruck.Vereinigung) {
        t.alternativen.forEachIndexed {i,a -> TypEditor("Alternative ${i+1}",a,tiefe+1) {v -> ändern(t.copy(alternativen=t.alternativen.mapIndexed {j,k -> if(i==j) v else k}))}}
        TextButton(onClick={ändern(t.copy(alternativen=t.alternativen+TypAusdruck.Unbekannt))}) {Text("Alternative hinzufügen")}
        TextButton(enabled=t.alternativen.size>1,onClick={ändern(t.copy(alternativen=t.alternativen.dropLast(1)))}) {Text("Letzte Alternative entfernen")}
    }
    if(t is TypAusdruck.Parameterisiert) {
        t.argumente.forEachIndexed { i,a -> TypEditor("Komponente ${i+1}",a,tiefe+1) { neu -> ändern(t.copy(argumente=t.argumente.mapIndexed { j,v -> if(i==j) neu else v })) } }
        if(t.konstruktor==MathematischeTypen.Tupel) FlowRow {TextButton(onClick={ändern(t.copy(argumente=t.argumente+ReelleZahlen.elementTypAusdruck()))}) {Text("Typkomponente hinzufügen")};TextButton(enabled=t.argumente.isNotEmpty(),onClick={ändern(t.copy(argumente=t.argumente.dropLast(1)))}) {Text("Letzte Typkomponente entfernen")}}
    }
}

@Composable
internal fun FormelKontextFehlerDialog(diagnose:String,schließen:()->Unit) {
    de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AtlasBestätigungsDialog(onDismissRequest=schließen,
        title={Text("Formeldaten nicht bearbeitbar")},text={Text("Die gespeicherten Formeldaten bleiben erhalten. $diagnose")},
        confirmButton={TextButton(onClick=schließen) {Text("Schließen")}})
}

fun typTitel(t:TypAusdruck):String=when(t) {
    TypAusdruck.Beliebig -> "Beliebiger Wert";TypAusdruck.Unbekannt -> "Unbekannt"
    is TypAusdruck.Atom -> when(t.id) {MathematischeTypen.Zahl -> "Zahl";MathematischeTypen.Reell -> "Reelle Zahl";MathematischeTypen.Komplex -> "Komplexe Zahl";MathematischeTypen.Rational -> "Rationale Zahl";MathematischeTypen.Ganz -> "Ganze Zahl";MathematischeTypen.Natuerlich -> "Natürliche Zahl";MathematischeTypen.Menge -> "Menge";MathematischeTypen.Aussage -> "Aussage";else -> "Benannter Objekttyp"}
    is TypAusdruck.Parameterisiert -> (when(t.konstruktor) {MathematischeTypen.Tupel -> "Tupel";MathematischeTypen.Methode -> "Methode";MathematischeTypen.Matrix -> "Matrix";MathematischeTypen.Tensor -> "Tensor";MathematischeTypen.ZeilenVektor -> "Zeilenvektor";MathematischeTypen.SpaltenVektor -> "Spaltenvektor";MathematischeTypen.Menge -> "Menge";else -> "Struktur"})+t.argumente.joinToString(prefix="⟨",postfix="⟩") {typTitel(it)}
    is TypAusdruck.Literal -> t.wert
    is TypAusdruck.Variable -> "Typvariable ${t.id.wert}"
    is TypAusdruck.Vereinigung -> t.alternativen.joinToString(" oder ") {typTitel(it)}
}

@Composable
private fun BereichEditor(raum:MengenAusdruck?,typ:TypAusdruck,ändern:(MengenAusdruck?)->Unit) {
    var fehler by remember(raum) {mutableStateOf<String?>(null)}
    var links by remember {mutableStateOf("0")};var rechts by remember {mutableStateOf("1")}
    var linksOffen by remember {mutableStateOf(false)};var rechtsOffen by remember {mutableStateOf(false)}
    var elemente by remember {mutableStateOf("0")}
    fun versuch(block:()->MengenAusdruck) {runCatching(block).onSuccess(ändern).onFailure {fehler=it.message}}
    var erweitert by remember {mutableStateOf(false)}
    TextButton(onClick={erweitert=!erweitert}) {Text("Wertebereich strukturieren")}
    if(!erweitert) return
    OutlinedTextField(links,{links=it},label={Text("Intervall · linke Grenze")});OutlinedTextField(rechts,{rechts=it},label={Text("Intervall · rechte Grenze")})
    Row {Checkbox(linksOffen,{linksOffen=it});Text("Links offen")};Row {Checkbox(rechtsOffen,{rechtsOffen=it});Text("Rechts offen")}
    TextButton(onClick={versuch {ReellesIntervall(RationaleZahl.parse(links),linksOffen,RationaleZahl.parse(rechts),rechtsOffen)}}) {Text("Intervall übernehmen")}
    OutlinedTextField(elemente,{elemente=it},label={Text("Endliche Zahlenmenge · durch Semikolon getrennt")})
    TextButton(onClick={versuch {EndlicheMenge(if(elemente.isBlank()) emptySet() else elemente.split(';').map {RationaleZahl.parse(it.trim())}.toSet())}}) {Text("Endliche Menge übernehmen")}
    TextButton(enabled=raum!=null,onClick={raum?.let {ändern(MengenDifferenz(it,EndlicheMenge(setOf(RationaleZahl.Null))))}}) {Text("Null ausschließen")}
    TextButton(onClick={versuch {bereichFürTyp(typ)}}) {Text("Strukturierten Raum aus Typ übernehmen")}
    if(raum is Tupelraum) raum.komponenten.forEachIndexed {i,r ->
        val komponentenTyp=(typ as? TypAusdruck.Parameterisiert)?.argumente?.getOrNull(i) ?: TypAusdruck.Unbekannt
        Text("Komponentenraum ${i+1}");Auswahl("Bereich",listOf<MengenAusdruck?>(null,ReelleZahlen,RationaleZahlen,GanzeZahlen,KomplexeZahlen,r).distinct(),r,{it?.zuLatex() ?: "Unbekannt"}) {v ->ändern(raum.copy(komponenten=raum.komponenten.mapIndexed {j,k -> if(i==j) v ?: FehlendeObermenge("Komponente") else k}))}
        BereichEditor(r,komponentenTyp) {v ->ändern(raum.copy(komponenten=raum.komponenten.mapIndexed {j,k ->if(i==j) v ?: FehlendeObermenge("Komponente") else k}))}
    }
    fehler?.let {Text(it,color=MaterialTheme.colorScheme.error)}
}
