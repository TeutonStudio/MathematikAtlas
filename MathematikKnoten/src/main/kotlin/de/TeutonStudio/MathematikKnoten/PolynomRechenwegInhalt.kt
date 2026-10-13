package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlinx.coroutines.*

@Composable
internal fun PolynomRechenwegInhalt(
    auswertung: KnotenAuswertungsErgebnis,
    modifier: Modifier = Modifier,
    konfigurationAendern: ((PolynomNumerikKonfiguration) -> Unit)? = null,
) {
    val problem = auswertung.polynomProblem ?: return
    val scope=rememberCoroutineScope()
    var familienMitglied by remember(problem) { mutableStateOf<ZertifiziertePolynomLoesung?>(null) }
    val belegungen=remember(problem) { mutableStateMapOf<String,String>() }
    var tab by remember(problem) { mutableIntStateOf(0) }
    var config by remember(problem,auswertung.polynomKonfiguration) { mutableStateOf(auswertung.polynomKonfiguration) }
    var stellen by remember(config) { mutableStateOf(config.stellen.toString()) }
    var radius by remember(config) { mutableStateOf(config.suchradius.toString()) }
    var neustart by remember(problem) { mutableIntStateOf(0) }
    var ergebnis by remember(problem,config) { mutableStateOf<PolynomLoesungsErgebnis?>(null) }
    var fehler by remember(problem,config) { mutableStateOf<String?>(null) }
    var laeuft by remember(problem,config) { mutableStateOf(false) }
    var auftrag by remember { mutableStateOf<Job?>(null) }
    var familienAuftrag by remember { mutableStateOf<Job?>(null) }
    var familienVersion by remember { mutableIntStateOf(0) }
    LaunchedEffect(problem,config,neustart) {
        auftrag = currentCoroutineContext()[Job]
        familienVersion++; familienAuftrag?.cancel()
        laeuft = true; fehler = null; familienMitglied=null
        try { ergebnis = PolynomNumerikDienst.berechne(problem,config) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { fehler = e.message ?: "Berechnung fehlgeschlagen." }
        finally { laeuft = false }
    }
    Column(modifier) {
        TabRow(selectedTabIndex=tab) {
            listOf("Analytische Lösung","Numerische Lösung").forEachIndexed { i,t ->
                Tab(selected=tab==i,onClick={tab=i},text={Text(t)})
            }
        }
        LazyColumn(Modifier.weight(1f,fill=false).fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                LatexText(problem.original.zuLatex())
                problem.bedingungen.forEach { LatexText(it.zuLatex()) }
                problem.eigenwertMatrix?.let { Text("Eigenwerte der Matrix"); LatexText(it.zuLatex()) }
                Text("Grundkörper: ${if(problem.komplex) "ℂ" else "ℝ"} · ${problem.variablen.size} Unbekannte")
                if (problem.variablen.size>1) Text("Reihenfolge: "+problem.variablen.joinToString { it.name },style=MaterialTheme.typography.bodySmall)
                if (laeuft) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    TextButton(onClick={auftrag?.cancel(); fehler="Abgebrochen; kein Unlösbarkeitsnachweis."}) { Text("Abbrechen") }
                }
                fehler?.let { Text(it,color=MaterialTheme.colorScheme.error) }
            }
            if(tab==0) {
                item {
                    Text("Exakte Herleitung",style=MaterialTheme.typography.titleMedium)
                    auswertung.schritte.forEach { schritt ->
                        Text(schritt.titel); LatexText(schritt.nachher.zuLatex()); Text(schritt.erklärung)
                    }
                    val r=ergebnis
                    if(r!=null) {
                        NachweisStatus(r)
                        r.analytisch.forEach { Text(it) }
                        r.analytischeFormeln.forEach { LatexText(it) }
                        if(r.analytischeFormeln.isNotEmpty()) Text("Alle zusammenpassenden Wurzelzweige berücksichtigen; Lösungen auf den angegebenen Grundkörper einschränken.",style=MaterialTheme.typography.bodySmall)
                        if(r.hinweis.isNotBlank()) Text(r.hinweis)
                        r.familie?.let { familie ->
                            Text("Affine Familie (reelle Komponenten):")
                            familie.forEachIndexed { i,w -> LatexText("u_{${i+1}}=${w.zuLatex()}") }
                            Text("Freie Parameter: "+r.parameter.joinToString { it.name+" ∈ ℝ" })
                            Text("Numerische Werte benötigen eine zulässige Parameterbelegung.")
                        }
                    }
                    Text("Eine nicht unterstützte Radikaldarstellung beweist nicht, dass eine solche Darstellung unmöglich ist.",style=MaterialTheme.typography.bodySmall)
                }
                itemsIndexed(ergebnis?.loesungen.orEmpty()) { i,l ->
                    Text("Algebraische Lösung ${i+1}"+(if(problem.variablen.size==1) " · Vielfachheit ${l.vielfachheit?:"offen"}" else ""),style=MaterialTheme.typography.titleSmall)
                    l.komponenten.forEachIndexed { j,k ->
                        LatexText("u_{${j+1}}\\in[${k.minimum.zuLatex()},${k.maximum.zuLatex()}]")
                    }
                    l.komplexeScheibe?.let { s ->
                        Text("Eindeutige komplexe Wurzel in der Scheibe um ${s.zentrum.alsAusdruck().zuLatex()} mit Radius ${s.radius}. Die Intervalle oben sind Koordinatenschranken.")
                    } ?: Text("Die Originalgleichung und der zugehörige Nachweis bestimmen die dargestellte Lösung.")
                }
            } else {
                item {
                    OutlinedTextField(stellen,{stellen=it},label={Text("Nachkommastellen n (0–100)")},singleLine=true,isError=stellen.toIntOrNull() !in 0..100,modifier=Modifier.fillMaxWidth())
                    if(problem.variablen.size>1) OutlinedTextField(radius,{radius=it},label={Text("Suchradius (1–1000)")},singleLine=true,isError=radius.toIntOrNull() !in 1..1000,modifier=Modifier.fillMaxWidth())
                    val n=stellen.toIntOrNull(); val s=radius.toIntOrNull()
                    Button(onClick={
                        val neu=PolynomNumerikKonfiguration(n!!,s!!)
                        konfigurationAendern?.invoke(neu); config=neu; neustart++
                    },enabled=n in 0..100 && s in 1..1000 && !laeuft) { Text("Berechnen") }
                    Text("Abschneiden gegen null: die ersten n Dezimalziffern, ohne Rundung. Über ℂ je Real- und Imaginärteil.")
                    ergebnis?.let { r ->
                        NachweisStatus(r); if(r.hinweis.isNotBlank()) Text(r.hinweis)
                        if(r.familie!=null) {
                            Text("Ein Familienmitglied auswählen (Parameter über ℝ):")
                            r.parameter.forEach { p -> OutlinedTextField(belegungen[p.name]?:"0",{familienVersion++; familienAuftrag?.cancel(); familienMitglied=null; belegungen[p.name]=it},label={Text(p.name+" (ganze Zahl oder Bruch)")},singleLine=true) }
                            Button(onClick={
                                familienVersion++; familienAuftrag?.cancel(); familienMitglied=null
                                val version=familienVersion
                                val angefordert=config
                                val werte=runCatching { r.parameter.associate { it.name to RationaleZahl.parse(belegungen[it.name]?:"0") } }
                                if(werte.isFailure) fehler="Parameter als ganze Zahl oder Bruch mit Nenner ungleich null eingeben."
                                else familienAuftrag=scope.launch {
                                    try {
                                        val mitglied=PolynomNumerikDienst.familienMitglied(problem,r,werte.getOrThrow(),angefordert.stellen)
                                        ensureActive()
                                        if(familienVersion==version && config==angefordert) familienMitglied=mitglied
                                    }
                                    catch(e:CancellationException) { throw e }
                                    catch(e:Exception) { fehler=e.message }
                                }
                            }) { Text("Familienmitglied berechnen") }
                            familienMitglied?.dezimalstellen?.forEachIndexed { j,t -> Text("${komponentenName(problem,j)} = $t") }
                        }
                    }
                }
                item {
                    var sucheOffen by remember(problem,config) { mutableStateOf(false) }
                    val suchschritte=ergebnis?.suchschritte.orEmpty()
                    if(suchschritte.isNotEmpty()) TextButton(onClick={sucheOffen=!sucheOffen}) { Text("${suchschritte.size} Suchschritte ${if(sucheOffen) "ausblenden" else "anzeigen"}") }
                    if(sucheOffen) suchschritte.forEach { s -> Text(s.begruendung+" "+s.einschliessung.joinToString { "[${it.minimum}, ${it.maximum}]" },style=MaterialTheme.typography.bodySmall) }
                }
                itemsIndexed(ergebnis?.loesungen.orEmpty()) { i,l ->
                    var offen by remember(problem,config,i) { mutableStateOf(false) }
                    Surface(color=MaterialTheme.colorScheme.surfaceContainerLow,shape=MaterialTheme.shapes.medium) {
                        Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text("Lösung ${i+1}",style=MaterialTheme.typography.titleMedium)
                            l.dezimalstellen?.forEachIndexed { j,t -> Text("${komponentenName(problem,j)} = $t") }
                                ?: Text("Existenz nachgewiesen; geforderte Dezimalziffern noch nicht nachgewiesen.")
                            TextButton(onClick={offen=!offen}) { Text("${l.schritte.size} Iterationsschritte ${if(offen) "ausblenden" else "anzeigen"}") }
                            if(offen) l.schritte.forEachIndexed { j,schritt ->
                                Text("${j+1}. ${schritt.begruendung}",style=MaterialTheme.typography.bodySmall)
                                schritt.einschliessung.forEachIndexed { k,v ->
                                    Text("${komponentenName(problem,k)}: [${v.minimum}, ${v.maximum}]",style=MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
private fun komponentenName(problem:PolynomGleichungsProblem,index:Int):String = if(problem.komplex)
    (if(index%2==0) "Re" else "Im")+"(${problem.variablen[index/2].name})" else problem.variablen[index].name

@Composable private fun NachweisStatus(r:PolynomLoesungsErgebnis) {
    fun status(s:LoesungsNachweis)=when(s) { LoesungsNachweis.Bewiesen -> "bewiesen"; LoesungsNachweis.Widerlegt -> "widerlegt"; LoesungsNachweis.Offen -> "offen" }
    Text("Existenz: ${status(r.existenz)} · Vollständigkeit: ${status(r.vollstaendigkeit)}")
    Text(if(r.existenz==LoesungsNachweis.Widerlegt) "Lösungsmenge: ∅" else "Dimension: ${r.dimension?.toString()?:"offen"}")
}
