package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*

internal object MethodenQuellenKarten {
    private var zustand: AtlasZustand? = null
    fun installieren(atlas: AtlasZustand) { zustand = atlas }
    fun lade(verweis: KartenVerweis): KartenDaten? = zustand?.speicher?.lade(verweis)
    fun signatur(karte: KartenDaten): String = methodenQuellenSignatur(KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister(), KartenQuelle(::lade)).exportiereKarteAlsMethode(karte).ausgaben["methode"]?.objekt as? de.TeutonStudio.MathematikRechenSystem.kern.SignaturtragendeMethode)
    fun prüfe(karte: KartenDaten): String? {
        val atlas = zustand ?: return "Kartenquelle fehlt."
        fun zyklisch(k: KartenDaten, besucht: MutableSet<KartenVerweis>): Boolean = k.knoten.flatMap { it.alleKartenVerweise() }.any { ref ->
            ref.kartenId == atlas.editor.karte.id || besucht.add(ref) && lade(ref)?.let { zyklisch(it, besucht) } == true
        }
        if (zyklisch(karte, mutableSetOf())) return "Die Karte erzeugt einen rekursiven Kartenverweis."
        return KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister(), KartenQuelle(::lade)).exportiereKarteAlsMethode(karte).fehler
    }
    fun karten(): List<KartenDaten> = zustand?.let { atlas ->
        atlas.karten.filter { !it.archiviert && it.id != atlas.editor.karte.id }
            .flatMap { atlas.kartenVersionen(it.id) }
    }.orEmpty()
}

internal object MethodenQuellenInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
        val quellen = remember { methodenKnotenQuellen() }
        val karten = MethodenQuellenKarten.karten()
        val dekodiert = remember(knoten.eigenschaften) { runCatching { methodenQuellKnoten(knoten) } }
        val quelle = dekodiert.getOrNull()
        val quelleFehler = dekodiert.exceptionOrNull()?.message
        val aktuell = knoten.eigenschaften.text(METHODEN_QUELLE_ID, "").ifBlank { when {
            knoten.kartenVerweis != null -> "karte:${knoten.kartenVerweis}"
            quelle != null -> "knoten:${quelle.art}:${quelle.parameter}"
            else -> "identitaet"
        } }
        var geöffnet by remember(knoten.id) { mutableStateOf(false) }
        var konfigurationsFehler by remember(knoten.id) { mutableStateOf<String?>(null) }
        val einträge = remember(knoten, karten) {
            listOf(RechnerOperatorAuswahlEintrag("identitaet", "Identität", "\\hookrightarrow\\dots", "Spezielle Methoden", "Gibt ihr Argument unverändert zurück.",
                kandidat = konfiguriereMethodenQuelle(knoten), definitionLatex = "x\\mapsto x")) +
                quellen.map { q -> RechnerOperatorAuswahlEintrag(q.id, q.titel, "\\operatorname{Methode}", "Knoten: ${q.kategorie}",
                    q.fehler ?: "Konfigurierter Knoten als Methode mit freien Eingängen. ${q.signatur}",
                    kandidat = if (q.fehler == null) konfiguriereMethodenQuelle(knoten, q.knoten, quelleId = q.id) else null,
                    art = if (q.fehler == null) RechnerOperatorAuswahlArt.OPERATOR else RechnerOperatorAuswahlArt.UNBEKANNT) } +
                karten.map { karte ->
                    val fehler = MethodenQuellenKarten.prüfe(karte)
                    RechnerOperatorAuswahlEintrag("karte:${KartenVerweis(karte.id, karte.version)}", "${karte.name} · v${karte.version}", "\\operatorname{Methode}", "Gespeicherte Karten", "Versionsfest referenzierte Karte als Methode. ${if (fehler == null) MethodenQuellenKarten.signatur(karte) else fehler}",
                    kandidat = if (fehler == null) konfiguriereMethodenQuelle(knoten, karte = KartenVerweis(karte.id, karte.version)) else null,
                    art = if (fehler == null) RechnerOperatorAuswahlArt.OPERATOR else RechnerOperatorAuswahlArt.UNBEKANNT, status = fehler) }
        }
        Text("Methodenquelle", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(onClick = { geöffnet = true }, modifier = Modifier.fillMaxWidth()) {
            Text(quelleFehler?.let { "Ungültige Knotenquelle" } ?: quelle?.name ?: knoten.kartenVerweis?.let { ref -> karten.firstOrNull { it.id == ref.kartenId && it.version == ref.version }?.let { "${it.name} · v${it.version}" } ?: "Fehlende Karte · v${ref.version}" } ?: "Identität")
        }
        if (quelle == null && quelleFehler == null && knoten.kartenVerweis == null) LatexText("x\\mapsto x")
        if (geöffnet) RechnerOperatorAuswahlDialog("Methode", einträge, aktuell,
            { it.kandidat?.let(aktionen::vorschauKnotenErsetzen) }, { geöffnet = false },
            { it.kandidat?.let(aktionen::knotenNachVerlustbestätigung); geöffnet = false }, {})
        if (quelle != null) {
            val inspektor = KnotenInspektorRegister.finde(quelle.art)
            fun übernehme(neu: KnotenDaten) {
                val probe = runCatching { KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister()).exportiereKarteAlsMethode(methodenQuellKarte(neu, knoten.id)) }
                val fehler = probe.exceptionOrNull()?.message ?: probe.getOrNull()?.fehler
                konfigurationsFehler = fehler
                if (fehler == null) aktionen.knoten(konfiguriereMethodenQuelle(knoten, neu))
            }
            Text("Konfiguration: ${quelle.name}", style = MaterialTheme.typography.titleSmall)
            if (quelle.anschlüsse.any { it.richtung == AnschlussRichtung.Eingang && it.kannSichErweitern }) {
                var anzahl by remember(quelle) { mutableStateOf(quelle.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang }.toString()) }
                val gültig = anzahl.toIntOrNull()?.let { it in 2..128 } == true
                OutlinedTextField(anzahl, { anzahl = it }, label = { Text("Anzahl Argumente") },
                    isError = !gültig, supportingText = { if (!gültig) Text("Ganze Zahl zwischen 2 und 128 eingeben.") })
                TextButton(enabled = gültig, onClick = {
                    runCatching { konfiguriereMethodenQuellParameter(quelle, "festeEingänge", anzahl) }
                        .onSuccess(::übernehme).onFailure { konfigurationsFehler = it.message }
                }) { Text("Argumentanzahl übernehmen") }
            }
            inspektor?.Inhalt(quelle, null, object : KnotenInspektorAktionen {
                override fun parameter(schlüssel: String, wert: String) = runCatching { konfiguriereMethodenQuellParameter(quelle, schlüssel, wert) }.onSuccess(::übernehme).onFailure { konfigurationsFehler = it.message }.let { Unit }
                override fun name(wert: String) = übernehme(quelle.copy(name = wert))
                override fun eigenschaften(eigenschaften: Map<String, KnotenEigenschaft>) = übernehme(quelle.copy(eigenschaften = eigenschaften))
                override fun anschlussArt(verweis: AnschlussVerweis, art: AnschlussArtId) = übernehme(quelle.copy(anschlüsse = quelle.anschlüsse.map { if (it.id == verweis.anschlussId) it.copy(art = art) else it }))
                override fun knoten(knoten: KnotenDaten) = übernehme(knoten)
            })
        }
        (quelleFehler ?: konfigurationsFehler ?: ergebnis?.fehler)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
