package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikRechenSystem.kern.*

fun konfiguriereAllgemeinenParameterBereich(knoten:KnotenDaten, bereich:WertebereichKonfiguration):KnotenDaten {
    require(knoten.art=="mathematik.allgemeinerParameter")
    bereich.zuMenge()
    val neu=knoten.copy(eigenschaften=knoten.eigenschaften+(WertebereichKonfiguration.EIGENSCHAFT to bereich.zuEigenschaft()))
    allgemeinerParameterWert(neu)
    return if(neu.parameter[RECHNER_MIGRATION_DIAGNOSE]?.startsWith(QUELLEN_BEREICH_DIAGNOSE_PREFIX)==true)
        neu.copy(parameter=neu.parameter-RECHNER_MIGRATION_DIAGNOSE) else neu
}

@Composable
fun InspektorAllgemeinerParameter(knoten:KnotenDaten,übernehmen:(KnotenDaten)->Unit) {
    val gelesen=runCatching {WertebereichKonfiguration.vonEigenschaft(knoten.eigenschaften[WertebereichKonfiguration.EIGENSCHAFT])}
    OutlinedTextField(value=knoten.parameter["name"] ?: "a",onValueChange={übernehmen(knoten.copy(parameter=knoten.parameter+("name" to it.trim())))},
        enabled=gelesen.isSuccess,label={Text("Parametername")},singleLine=true,modifier=Modifier.fillMaxWidth())
    gelesen.exceptionOrNull()?.let {Text(it.message.orEmpty(),color=MaterialTheme.colorScheme.error)}
    if(gelesen.isFailure) Text("Wähle den gewünschten Bereich ausdrücklich neu. Die gespeicherte Konfiguration bleibt bis dahin erhalten.")
    val prüfen:(WertebereichKonfiguration)->String?={runCatching {konfiguriereAllgemeinenParameterBereich(knoten,it)}.exceptionOrNull()?.let {fehler -> fehler.message ?: "Der Wertebereich ist ungültig."}}
    WertebereichEditor(gelesen.getOrNull(),prüfen) {neu -> übernehmen(konfiguriereAllgemeinenParameterBereich(knoten,neu))}
    Text("Der Bereich beschreibt den allgemeinen Parameter. Seine Quellenidentität bleibt beim Umbenennen erhalten.",style=MaterialTheme.typography.bodySmall)
}

@Composable private fun WertebereichEditor(
    bereich:WertebereichKonfiguration?,
    prüfen:(WertebereichKonfiguration)->String?,
    ändern:(WertebereichKonfiguration)->Unit,
) {
    Text("Wertebereich",style=MaterialTheme.typography.titleSmall)
    val arten=listOf(
        "Zahl" to WertebereichKonfiguration.Zahl(),
        "Aussage" to WertebereichKonfiguration.Aussage,
        "Menge" to WertebereichKonfiguration.Menge(),
        "Tupel" to WertebereichKonfiguration.Tupel(),
        "Spalte" to WertebereichKonfiguration.Vektor(),
        "Zeile" to WertebereichKonfiguration.Vektor(orientierung=VektorOrientierung.Zeile),
        "Matrix" to WertebereichKonfiguration.Matrix(),
        "Winkel (Gradmenge)" to WertebereichKonfiguration.Winkel,
    )
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
        arten.forEach {(name,wert) ->
            val fehler=prüfen(wert)
            FilterChip(selected=bereich!=null && bereich::class==wert::class &&
                (bereich !is WertebereichKonfiguration.Vektor || wert !is WertebereichKonfiguration.Vektor || bereich.orientierung==wert.orientierung),
                enabled=fehler==null,onClick={ändern(wert)},label={Text(name)})
        }
    }
    arten.mapNotNull {(name,wert) -> prüfen(wert)?.let {"$name: $it"}}.distinct().forEach {Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
    when(bereich) {
        null -> Unit
        WertebereichKonfiguration.Winkel -> LatexText(Gradmenge.zuLatex())
        WertebereichKonfiguration.Aussage -> Text("{⊤, ⊥}")
        is WertebereichKonfiguration.Zahl -> GrundmengenAuswahl("Zahlgrundmenge",bereich.grundmenge,
            {prüfen(bereich.copy(grundmenge=it))}) {ändern(bereich.copy(grundmenge=it))}
        is WertebereichKonfiguration.Menge -> {
            Text("Elementbereich")
            WertebereichEditor(bereich.elementBereich,{prüfen(bereich.copy(elementBereich=it))}) {ändern(bereich.copy(elementBereich=it))}
        }
        is WertebereichKonfiguration.Tupel -> {
            bereich.komponenten.forEachIndexed {index,komponente ->
                fun kandidat(neu:WertebereichKonfiguration)=bereich.copy(komponenten=bereich.komponenten.mapIndexed {i,alt -> if(i==index) neu else alt})
                Text("Komponente ${index+1}")
                WertebereichEditor(komponente,{prüfen(kandidat(it))}) {ändern(kandidat(it))}
                val entfernt=bereich.copy(komponenten=bereich.komponenten.filterIndexed {i,_ -> i!=index})
                TextButton(enabled=prüfen(entfernt)==null,onClick={ändern(entfernt)}) {Text("Komponente ${index+1} entfernen")}
            }
            val ergänzt=bereich.copy(komponenten=bereich.komponenten+WertebereichKonfiguration.Zahl())
            val fehler=prüfen(ergänzt)
            OutlinedButton(enabled=fehler==null,onClick={ändern(ergänzt)}) {Text("Komponente hinzufügen")}
            fehler?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        }
        is WertebereichKonfiguration.Vektor -> {
            PositiveGanzzahlFeld("Dimension",bereich.dimension,{prüfen(bereich.copy(dimension=it))}) {ändern(bereich.copy(dimension=it))}
            GrundmengenAuswahl("Skalarmenge",bereich.skalarMenge,{prüfen(bereich.copy(skalarMenge=it))}) {ändern(bereich.copy(skalarMenge=it))}
        }
        is WertebereichKonfiguration.Matrix -> {
            PositiveGanzzahlFeld("Zeilen",bereich.zeilen,{prüfen(bereich.copy(zeilen=it))}) {ändern(bereich.copy(zeilen=it))}
            PositiveGanzzahlFeld("Spalten",bereich.spalten,{prüfen(bereich.copy(spalten=it))}) {ändern(bereich.copy(spalten=it))}
            GrundmengenAuswahl("Skalarmenge",bereich.skalarMenge,{prüfen(bereich.copy(skalarMenge=it))}) {ändern(bereich.copy(skalarMenge=it))}
        }
    }
}

@Composable private fun PositiveGanzzahlFeld(label:String,wert:Int,prüfen:(Int)->String?,ändern:(Int)->Unit) {
    var text by remember(label,wert) {mutableStateOf(wert.toString())}
    val zahl=text.toIntOrNull()?.takeIf {it>0}
    val fehler=if(zahl==null) "Eine positive ganze Zahl eingeben; der gespeicherte Wert bleibt erhalten." else prüfen(zahl)
    OutlinedTextField(value=text,onValueChange={neu ->
        text=neu
        neu.toIntOrNull()?.takeIf {it>0 && prüfen(it)==null}?.let(ändern)
    },label={Text(label)},isError=fehler!=null,supportingText={fehler?.let {Text(it)}},singleLine=true,modifier=Modifier.fillMaxWidth())
}

@Composable private fun GrundmengenAuswahl(label:String,aktuell:String,prüfen:(String)->String?,ändern:(String)->Unit) {
    Text(label,style=MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("N","Z","Q","R","C").forEach {menge -> FilterChip(selected=aktuell==menge,enabled=prüfen(menge)==null,onClick={ändern(menge)},label={Text(menge)})}
    }
}
