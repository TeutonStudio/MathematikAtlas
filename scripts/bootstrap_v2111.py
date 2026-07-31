from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding='utf-8')


def write(path: str, text: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text, encoding='utf-8')


def replace_once(path: str, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f'{path}: erwartete genau einen Treffer, gefunden {count}\n--- OLD ---\n{old[:500]}')
    write(path, text.replace(old, new, 1))


def append_once(path: str, marker: str, addition: str) -> None:
    text = read(path)
    if marker in text:
        return
    write(path, text.rstrip() + '\n\n' + addition.strip() + '\n')

# 1. Atomarer Knotenersatz für Karten-/Zustandswechsel.
replace_once(
    'KnotenKartenVerwalter/src/main/kotlin/de/TeutonStudio/KnotenKartenVerwalter/logik/KartenAktion.kt',
    '''    data class KnotenKonfigurationErsetzen(\n        val id: KnotenId,\n        val parameter: Map<String, String>,\n        val anschlüsse: List<AnschlussDaten>,\n    ) : KartenAktion\n    data class KnotenLöschen(val id: KnotenId) : KartenAktion\n''',
    '''    data class KnotenKonfigurationErsetzen(\n        val id: KnotenId,\n        val parameter: Map<String, String>,\n        val anschlüsse: List<AnschlussDaten>,\n    ) : KartenAktion\n    /** Ersetzt einen vollständigen Knoten atomar und entfernt nur Verbindungen zu entfallenen Anschlüssen. */\n    data class KnotenErsetzen(val knoten: KnotenDaten) : KartenAktion\n    data class KnotenLöschen(val id: KnotenId) : KartenAktion\n''',
)
replace_once(
    'KnotenKartenVerwalter/src/main/kotlin/de/TeutonStudio/KnotenKartenVerwalter/logik/KartenAktion.kt',
    '''    is KartenAktion.KnotenKonfigurationErsetzen -> {\n        val gültigeAnschlüsse = aktion.anschlüsse.map { it.id }.toSet()\n        copy(\n            knoten = knoten.map {\n                if (it.id == aktion.id) it.copy(parameter = aktion.parameter, anschlüsse = aktion.anschlüsse) else it\n            },\n            verbindungen = verbindungen.filterNot { verbindung ->\n                (verbindung.von.knotenId == aktion.id && verbindung.von.anschlussId !in gültigeAnschlüsse) ||\n                    (verbindung.zu.knotenId == aktion.id && verbindung.zu.anschlussId !in gültigeAnschlüsse)\n            },\n        )\n    }\n    is KartenAktion.KnotenLöschen -> copy(\n''',
    '''    is KartenAktion.KnotenKonfigurationErsetzen -> {\n        val gültigeAnschlüsse = aktion.anschlüsse.map { it.id }.toSet()\n        copy(\n            knoten = knoten.map {\n                if (it.id == aktion.id) it.copy(parameter = aktion.parameter, anschlüsse = aktion.anschlüsse) else it\n            },\n            verbindungen = verbindungen.filterNot { verbindung ->\n                (verbindung.von.knotenId == aktion.id && verbindung.von.anschlussId !in gültigeAnschlüsse) ||\n                    (verbindung.zu.knotenId == aktion.id && verbindung.zu.anschlussId !in gültigeAnschlüsse)\n            },\n        )\n    }\n    is KartenAktion.KnotenErsetzen -> {\n        val gültigeAnschlüsse = aktion.knoten.anschlüsse.map { it.id }.toSet()\n        copy(\n            knoten = knoten.map { if (it.id == aktion.knoten.id) aktion.knoten else it },\n            verbindungen = verbindungen.filterNot { verbindung ->\n                (verbindung.von.knotenId == aktion.knoten.id && verbindung.von.anschlussId !in gültigeAnschlüsse) ||\n                    (verbindung.zu.knotenId == aktion.knoten.id && verbindung.zu.anschlussId !in gültigeAnschlüsse)\n            },\n        )\n    }\n    is KartenAktion.KnotenLöschen -> copy(\n''',
)

# 2. Öffentliche Anschlüsse werden durch die Kartenposition geordnet.
replace_once(
    'app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/AtlasMigrationen.kt',
    '''internal fun öffentlicheKartenAnschlüsse(\n    karte: KartenDaten,\n    interneArt: String,\n    richtung: AnschlussRichtung,\n    kante: AnschlussKante,\n): List<AnschlussDaten> = karte.knoten.asSequence()\n    .filter { it.art == interneArt }\n    .mapNotNull { intern -> intern.anschlüsse.firstOrNull { it.name == "wert" }?.let { wert -> öffentlicherKartenName(intern) to wert.art } }\n    .distinctBy { it.first }\n    .mapIndexed { index, (name, art) -> AnschlussDaten(name = name, richtung = richtung, kante = kante, art = art, reihenfolge = index) }\n    .toList()\n''',
    '''internal fun öffentlicheKartenAnschlüsse(\n    karte: KartenDaten,\n    interneArt: String,\n    richtung: AnschlussRichtung,\n    kante: AnschlussKante,\n): List<AnschlussDaten> = karte.knoten.asSequence()\n    .filter { it.art == interneArt }\n    .mapNotNull { intern ->\n        intern.anschlüsse.firstOrNull { it.name == "wert" }?.let { wert ->\n            Triple(intern, öffentlicherKartenName(intern), wert.art)\n        }\n    }\n    .sortedWith(compareBy({ it.first.position.y }, { it.first.position.x }, { it.first.id.wert }))\n    .distinctBy { it.second }\n    .mapIndexed { index, (_, name, art) ->\n        AnschlussDaten(name = name, richtung = richtung, kante = kante, art = art, reihenfolge = index)\n    }\n    .toList()\n''',
)

# 3. Kartenmethoden: beliebig viele typisierte Parameter und benannte Ausgaben.
replace_once(
    'MathematikKartenAdapter/src/main/kotlin/de/TeutonStudio/MathematikKartenAdapter/KartenAuswerter.kt',
    '''        val interneEingänge = intern.knoten.filter { it.art == "mathematik.kartenEingang" }\n        val vorgaben = mutableMapOf<KnotenId, Map<String, BedingterWert>>()\n        val freie = mutableListOf<Variable>()\n        interneEingänge.forEach { eingang ->\n            val name = öffentlicherKartenName(eingang)\n            val wert = außen[name] ?: BedingterWert(Variable(name)).also { freie += it.objekt as Variable }\n            vorgaben[eingang.id] = mapOf("wert" to wert)\n        }\n        val internErgebnis = auswertenIntern(intern, vorgaben, kartenPfad + verweis)\n        if (internErgebnis.fehler.isNotEmpty()) return KnotenAuswertungsErgebnis(emptyMap(), fehler = internErgebnis.fehler.joinToString())\n        val ausgänge = intern.knoten.filter { it.art == "mathematik.kartenAusgang" }.distinctBy(::öffentlicherKartenName)\n        val werte = ausgänge.mapNotNull { ausgang ->\n            val name = öffentlicherKartenName(ausgang)\n            internErgebnis.knoten[ausgang.id]?.ausgaben?.get("wert")?.let { name to it }\n        }.toMap()\n        if (!knoten.art.startsWith("methode.")) return KnotenAuswertungsErgebnis(werte)\n        if (interneEingänge.size != 1) return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Eine Methode benötigt genau einen öffentlichen Karten-Eingang.")\n        if (ausgänge.size != 1 || werte.size != 1) return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Eine Methode benötigt genau einen öffentlichen Karten-Ausgang mit Wert.")\n        val zielMengen = werte.mapValues { (name, wert) -> wert.zielMenge ?: return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Für die Methodenausgabe '$name' fehlt die Zielmenge.") }\n        val funktion = Funktion(knoten.name, freie.distinctBy { it.name }, werte.mapValues { it.value.objekt }, zielMengen)\n        if (funktion.einzigeAusgabe().second is MengenAusdruck) funktion.prüfeAlsIterationsMethode(erwartetMengenwert = true)\n        return KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(funktion)))\n''',
    '''        val sortierung = compareBy<KnotenDaten>({ it.position.y }, { it.position.x }, { it.id.wert })\n        val interneEingänge = intern.knoten.filter { it.art == "mathematik.kartenEingang" }.sortedWith(sortierung)\n        val eingangsNamen = interneEingänge.map(::öffentlicherKartenName)\n        if (eingangsNamen.distinct().size != eingangsNamen.size) {\n            return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Öffentliche Karten-Eingänge benötigen eindeutige Namen.")\n        }\n        val vorgaben = mutableMapOf<KnotenId, Map<String, BedingterWert>>()\n        val freie = mutableListOf<FunktionsParameter>()\n        val werteVorräte = linkedMapOf<String, MengenAusdruck>()\n        interneEingänge.forEach { eingang ->\n            val name = öffentlicherKartenName(eingang)\n            val ausgangsArt = eingang.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }?.art\n                ?: AnschlussArtId("mathematik.objekt")\n            val wert = außen[name] ?: symbolischerEingangswert(ausgangsArt, name, eingang.id).also { symbolisch ->\n                val parameter = symbolisch.objekt as? FunktionsParameter\n                    ?: return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Karteneingang '$name' ist kein Methodenparameter.")\n                freie += parameter\n                symbolisch.werteVorrat?.let { werteVorräte[name] = it }\n            }\n            vorgaben[eingang.id] = mapOf("wert" to wert)\n        }\n        val internErgebnis = auswertenIntern(intern, vorgaben, kartenPfad + verweis)\n        if (internErgebnis.fehler.isNotEmpty()) return KnotenAuswertungsErgebnis(emptyMap(), fehler = internErgebnis.fehler.joinToString())\n        val ausgänge = intern.knoten.filter { it.art == "mathematik.kartenAusgang" }.sortedWith(sortierung)\n        val ausgangsNamen = ausgänge.map(::öffentlicherKartenName)\n        if (ausgangsNamen.distinct().size != ausgangsNamen.size) {\n            return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Öffentliche Karten-Ausgänge benötigen eindeutige Namen.")\n        }\n        val werte = ausgänge.mapNotNull { ausgang ->\n            val name = öffentlicherKartenName(ausgang)\n            internErgebnis.knoten[ausgang.id]?.ausgaben?.get("wert")?.let { name to it }\n        }.toMap(LinkedHashMap())\n        if (!knoten.art.startsWith("methode.")) return KnotenAuswertungsErgebnis(werte)\n        if (ausgänge.isEmpty()) return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Eine Kartenmethode benötigt mindestens einen öffentlichen Ausgang.")\n        if (werte.size != ausgänge.size) return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Nicht alle öffentlichen Kartenausgänge liefern einen Wert.")\n        val zielMengen = werte.mapValues { (name, wert) ->\n            wert.zielMenge ?: return KnotenAuswertungsErgebnis(emptyMap(), fehler = "Für die Methodenausgabe '$name' fehlt die Zielmenge.")\n        }\n        val funktion = Funktion(\n            name = knoten.name,\n            parameter = freie.distinctBy { it.name },\n            ausgaben = werte.mapValues { it.value.objekt },\n            zielMengen = zielMengen,\n            werteVorräte = werteVorräte,\n        )\n        return KnotenAuswertungsErgebnis(mapOf("methode" to BedingterWert(funktion)))\n''',
)

# 4. Neue KartenKnoten-Domänenlogik.
write('app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/KartenKnoten.kt', r'''package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.MathematikKnoten.MathematikAnschlussArten

internal enum class KartenKnotenZustand { Schnittstelle, Methode }

internal data class KartenKnotenKompatibilität(
    val karte: KartenDaten,
    val kompatibel: Boolean,
    val grund: String? = null,
)

internal fun KnotenDaten.kartenKnotenZustand(): KartenKnotenZustand =
    if (art.startsWith("methode.")) KartenKnotenZustand.Methode else KartenKnotenZustand.Schnittstelle

internal fun AtlasZustand.kartenVorlage(
    karte: KartenDaten,
    zustand: KartenKnotenZustand = KartenKnotenZustand.Schnittstelle,
): KnotenVorlage {
    val anschlüsse = when (zustand) {
        KartenKnotenZustand.Schnittstelle ->
            öffentlicheKartenAnschlüsse(karte, "mathematik.kartenEingang", AnschlussRichtung.Eingang, AnschlussKante.Links) +
                öffentlicheKartenAnschlüsse(karte, "mathematik.kartenAusgang", AnschlussRichtung.Ausgang, AnschlussKante.Rechts)
        KartenKnotenZustand.Methode -> listOf(
            AnschlussDaten(
                name = "methode",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Funktion.id,
            ),
        )
    }
    return KnotenVorlage(
        art = if (zustand == KartenKnotenZustand.Methode) "methode.${karte.id.wert}" else "gruppe.${karte.id.wert}",
        name = karte.name,
        kategorie = "Gespeicherte Karten",
        beschreibung = if (zustand == KartenKnotenZustand.Methode) {
            "Karte als Methode, fest auf Version ${karte.version} verwiesen."
        } else {
            "Wiederverwendbare Karte, fest auf Version ${karte.version} verwiesen."
        },
        standardGröße = GraphGröße(240f, maxOf(100f, 54f + anschlüsse.groupBy { it.richtung }.values.maxOfOrNull { it.size }.orEmpty() * 28f)),
        anschlüsse = anschlüsse,
        kartenVerweis = KartenVerweis(karte.id, karte.version),
    )
}

private fun Int?.orEmpty(): Int = this ?: 0

internal fun AtlasZustand.fügeKartenKnotenEin(karte: KartenDaten, position: GraphPunkt): KnotenDaten? {
    if (!karteIstAlsKnotenZulässig(karte)) return null
    val knoten = kartenVorlage(karte).erzeuge(position)
    editor.führeAus(KartenAktion.KnotenEinfügen(knoten))
    editor.wähleKnoten(knoten.id)
    return knoten
}

internal fun AtlasZustand.kartenVersionen(id: KartenId): List<KartenDaten> {
    val höchste = (karten.firstOrNull { it.id == id } ?: speicher.ladeAktuell(id))?.version ?: return emptyList()
    return (1..höchste).mapNotNull { version -> speicher.lade(KartenVerweis(id, version)) }.sortedByDescending { it.version }
}

internal fun AtlasZustand.kartenKandidaten(knoten: KnotenDaten): List<KartenKnotenKompatibilität> =
    karten.asSequence()
        .filter { !it.archiviert && it.id != editor.karte.id }
        .map { karte ->
            val versionen = kartenVersionen(karte.id)
            val beste = versionen.firstOrNull { prüfeKartenKandidat(knoten, it).kompatibel }
            if (beste != null) KartenKnotenKompatibilität(karte, true)
            else prüfeKartenKandidat(knoten, versionen.firstOrNull() ?: karte)
        }
        .sortedBy { it.karte.name.lowercase() }
        .toList()

internal fun AtlasZustand.prüfeKartenKandidat(
    knoten: KnotenDaten,
    karte: KartenDaten,
): KartenKnotenKompatibilität {
    if (!karteIstAlsKnotenZulässig(karte)) {
        return KartenKnotenKompatibilität(karte, false, "Die Karte würde einen rekursiven Kartenverweis erzeugen.")
    }
    schnittstellenFehler(karte, knoten.kartenKnotenZustand())?.let {
        return KartenKnotenKompatibilität(karte, false, it)
    }
    val kandidat = knotenFürKarte(knoten, karte, knoten.kartenKnotenZustand(), anschlussIdsErhalten = true)
    val incident = editor.karte.verbindungen.filter { it.von.knotenId == knoten.id || it.zu.knotenId == knoten.id }
    val kandidatIds = kandidat.anschlüsse.mapTo(mutableSetOf()) { it.id }
    incident.firstOrNull { verbindung ->
        (verbindung.von.knotenId == knoten.id && verbindung.von.anschlussId !in kandidatIds) ||
            (verbindung.zu.knotenId == knoten.id && verbindung.zu.anschlussId !in kandidatIds)
    }?.let { return KartenKnotenKompatibilität(karte, false, "Mindestens ein verbundener Anschluss fehlt in dieser Karte.") }

    val prüfung = GraphPrüfung(anschlussArten)
    var probe = editor.karte.copy(
        knoten = editor.karte.knoten.map { if (it.id == knoten.id) kandidat else it },
        verbindungen = editor.karte.verbindungen.filterNot { it in incident },
    )
    incident.forEach { verbindung ->
        when (val ergebnis = prüfung.prüfe(probe, verbindung.von, verbindung.zu)) {
            VerbindungsPrüfung.Erlaubt -> probe = probe.copy(verbindungen = probe.verbindungen + verbindung)
            is VerbindungsPrüfung.Abgelehnt -> return KartenKnotenKompatibilität(karte, false, ergebnis.grund)
        }
    }
    return KartenKnotenKompatibilität(karte, true)
}

internal fun AtlasZustand.setzeKartenKnotenKarte(knoten: KnotenDaten, karte: KartenDaten): Boolean {
    if (!prüfeKartenKandidat(knoten, karte).kompatibel) return false
    editor.führeAus(KartenAktion.KnotenErsetzen(
        knotenFürKarte(knoten, karte, knoten.kartenKnotenZustand(), anschlussIdsErhalten = true),
    ))
    return true
}

internal fun AtlasZustand.setzeKartenKnotenZustand(knoten: KnotenDaten, zustand: KartenKnotenZustand): Boolean {
    val karte = knoten.kartenVerweis?.let(speicher::lade) ?: return false
    schnittstellenFehler(karte, zustand)?.let { return false }
    editor.führeAus(KartenAktion.KnotenErsetzen(
        knotenFürKarte(knoten, karte, zustand, anschlussIdsErhalten = false),
    ))
    return true
}

private fun AtlasZustand.knotenFürKarte(
    alt: KnotenDaten,
    karte: KartenDaten,
    zustand: KartenKnotenZustand,
    anschlussIdsErhalten: Boolean,
): KnotenDaten {
    val neu = kartenVorlage(karte, zustand).erzeuge(alt.position)
    val ids = if (anschlussIdsErhalten) alt.anschlüsse.associateBy({ it.richtung to it.name }, { it.id }) else emptyMap()
    return neu.copy(
        id = alt.id,
        name = alt.name,
        position = alt.position,
        parameter = alt.parameter,
        eigenschaften = alt.eigenschaften,
        anschlüsse = neu.anschlüsse.map { anschluss ->
            ids[anschluss.richtung to anschluss.name]?.let { anschluss.copy(id = it) } ?: anschluss
        },
    )
}

private fun AtlasZustand.karteIstAlsKnotenZulässig(karte: KartenDaten): Boolean =
    karte.id != editor.karte.id && !karte.archiviert && !referenziertKarte(karte, editor.karte.id, mutableSetOf())

private fun AtlasZustand.referenziertKarte(
    karte: KartenDaten,
    gesuchteId: KartenId,
    besucht: MutableSet<KartenVerweis>,
): Boolean {
    val refs = karte.knoten.mapNotNull { it.kartenVerweis }
    if (refs.any { it.kartenId == gesuchteId }) return true
    return refs.any { ref -> !besucht.add(ref) || speicher.lade(ref)?.let { referenziertKarte(it, gesuchteId, besucht) } == true }
}

private fun schnittstellenFehler(karte: KartenDaten, zustand: KartenKnotenZustand): String? {
    fun namen(art: String) = karte.knoten.filter { it.art == art }.map(::öffentlicherKartenName)
    val eingänge = namen("mathematik.kartenEingang")
    val ausgänge = namen("mathematik.kartenAusgang")
    if (eingänge.distinct().size != eingänge.size) return "Öffentliche Karten-Eingänge benötigen eindeutige Namen."
    if (ausgänge.distinct().size != ausgänge.size) return "Öffentliche Karten-Ausgänge benötigen eindeutige Namen."
    if (zustand == KartenKnotenZustand.Methode && ausgänge.isEmpty()) return "Eine Kartenmethode benötigt mindestens einen Ausgang."
    return null
}
''')

# 5. Vorlagen verwenden die zentrale KartenKnoten-Erzeugung.
replace_once(
    'app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/AtlasZustand.kt',
    '''    private fun gruppenVorlagen(): List<KnotenVorlage> = karten.asSequence()\n        .filter { it.id != editor.karte.id && !it.archiviert && !referenziertKarte(it, editor.karte.id, mutableSetOf()) }\n        .flatMap { karte ->\n            val eingänge = öffentlicheKartenAnschlüsse(karte, "mathematik.kartenEingang", AnschlussRichtung.Eingang, AnschlussKante.Links)\n            val ausgänge = öffentlicheKartenAnschlüsse(karte, "mathematik.kartenAusgang", AnschlussRichtung.Ausgang, AnschlussKante.Rechts)\n            listOf(KnotenVorlage(\n                art = "gruppe.${karte.id.wert}",\n                name = karte.name,\n                kategorie = "Gespeicherte Karten",\n                beschreibung = "Wiederverwendbare Karte, fest auf Version ${karte.version} verwiesen.",\n                standardGröße = GraphGröße(240f, maxOf(100f, 54f + maxOf(eingänge.size, ausgänge.size) * 28f)),\n                anschlüsse = eingänge + ausgänge,\n                kartenVerweis = KartenVerweis(karte.id, karte.version),\n            ))\n        }.toList()\n''',
    '''    private fun gruppenVorlagen(): List<KnotenVorlage> = karten.asSequence()\n        .filter { it.id != editor.karte.id && !it.archiviert && !referenziertKarte(it, editor.karte.id, mutableSetOf()) }\n        .map { kartenVorlage(it) }\n        .toList()\n''',
)

# 6. Karten erscheinen ausschließlich im Karten-Tab.
replace_once(
    'app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/KnotenAuswahlFenster.kt',
    '                    KnotenAuswahlTab("Alle", sichtbareVorlagen, zusätzlicheEinträge = mengenZusatz),\n',
    '                    KnotenAuswahlTab("Alle", sichtbareVorlagen.filterNot { it.kategorie in kartenKategorien }, zusätzlicheEinträge = mengenZusatz),\n',
)

# 7. Inspector für Zustand, Karte, Version und manuelles Update.
write('app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/KartenKnotenInspektor.kt', r'''package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KartenKnotenInspektor(knoten: KnotenDaten, zustand: AtlasZustand) {
    val ref = knoten.kartenVerweis ?: return
    val referenziert = zustand.speicher.lade(ref)
    val versionen = zustand.kartenVersionen(ref.kartenId)
    val kartenOptionen = zustand.kartenKandidaten(knoten)
    var karteGeöffnet by remember(knoten.id, ref) { mutableStateOf(false) }
    var versionGeöffnet by remember(knoten.id, ref) { mutableStateOf(false) }
    val aktuellerZustand = knoten.kartenKnotenZustand()
    val verbindungen = zustand.editor.karte.verbindungen.count { it.von.knotenId == knoten.id || it.zu.knotenId == knoten.id }

    HorizontalDivider()
    Text("KartenKnoten", style = MaterialTheme.typography.titleSmall)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = aktuellerZustand == KartenKnotenZustand.Schnittstelle,
            onClick = { zustand.setzeKartenKnotenZustand(knoten, KartenKnotenZustand.Schnittstelle) },
            label = { Text("Schnittstelle") },
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            selected = aktuellerZustand == KartenKnotenZustand.Methode,
            onClick = { zustand.setzeKartenKnotenZustand(knoten, KartenKnotenZustand.Methode) },
            label = { Text("Methode") },
            modifier = Modifier.weight(1f),
        )
    }
    if (verbindungen > 0) {
        Text(
            "Ein Zustandswechsel entfernt $verbindungen bestehende ${if (verbindungen == 1) "Verbindung" else "Verbindungen"} atomar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    ExposedDropdownMenuBox(expanded = karteGeöffnet, onExpandedChange = { karteGeöffnet = it }) {
        OutlinedTextField(
            value = referenziert?.name ?: ref.kartenId.wert,
            onValueChange = {},
            readOnly = true,
            label = { Text("Karte") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = karteGeöffnet) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = karteGeöffnet, onDismissRequest = { karteGeöffnet = false }) {
            kartenOptionen.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(option.karte.name)
                            option.grund?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    },
                    enabled = option.kompatibel,
                    onClick = {
                        val ziel = zustand.kartenVersionen(option.karte.id)
                            .firstOrNull { zustand.prüfeKartenKandidat(knoten, it).kompatibel }
                        if (ziel != null) zustand.setzeKartenKnotenKarte(knoten, ziel)
                        karteGeöffnet = false
                    },
                )
            }
        }
    }

    ExposedDropdownMenuBox(expanded = versionGeöffnet, onExpandedChange = { versionGeöffnet = it }) {
        OutlinedTextField(
            value = "Version ${ref.version}",
            onValueChange = {},
            readOnly = true,
            label = { Text("Kartenversion") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = versionGeöffnet) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = versionGeöffnet, onDismissRequest = { versionGeöffnet = false }) {
            versionen.forEach { version ->
                val prüfung = zustand.prüfeKartenKandidat(knoten, version)
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Version ${version.version}")
                            prüfung.grund?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    },
                    enabled = prüfung.kompatibel,
                    onClick = {
                        zustand.setzeKartenKnotenKarte(knoten, version)
                        versionGeöffnet = false
                    },
                )
            }
        }
    }

    val neueste = versionen.maxByOrNull { it.version }
    val updatePrüfung = neueste?.let { zustand.prüfeKartenKandidat(knoten, it) }
    Button(
        onClick = { neueste?.let { zustand.setzeKartenKnotenKarte(knoten, it) } },
        enabled = neueste != null && neueste.version > ref.version && updatePrüfung?.kompatibel == true,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (neueste != null && neueste.version > ref.version) "Auf Version ${neueste.version} aktualisieren" else "Aktuelle Version") }
    if (neueste != null && neueste.version > ref.version && updatePrüfung?.kompatibel == false) {
        Text(updatePrüfung.grund ?: "Die neueste Version ist nicht kompatibel.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    OutlinedButton(onClick = { zustand.öffne(ref) }, modifier = Modifier.fillMaxWidth()) { Text("Unterkarte öffnen") }
}
''')
replace_once(
    'app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/KnotenInspektorFenster.kt',
    '            StandardwerteEditor(knoten, zustand)\n',
    '            StandardwerteEditor(knoten, zustand)\n            if (knoten.kartenVerweis != null) KartenKnotenInspektor(knoten, zustand)\n',
)
replace_once(
    'app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/KnotenInspektorFenster.kt',
    '''            knoten.kartenVerweis?.let { ref ->\n                HorizontalDivider()\n                Text("Kartenverweis: ${ref.kartenId.wert.take(8)}, Version ${ref.version}")\n                Button(onClick = { zustand.öffne(ref) }) { Text("Unterkarte öffnen") }\n            }\n''',
    '',
)

# 8. Drag-and-drop aus der Kartenliste auf freien Kartenraum.
write('app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/KartenDrag.kt', r'''package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import java.util.WeakHashMap

internal class KartenDragZustand {
    var karte by mutableStateOf<KartenDaten?>(null)
        private set
    var positionImFenster by mutableStateOf<Offset?>(null)
        private set
    var editorBereich by mutableStateOf<Rect?>(null)
    var dichte by mutableFloatStateOf(1f)

    fun beginne(karte: KartenDaten, position: Offset) {
        this.karte = karte
        positionImFenster = position
    }

    fun verschiebe(delta: Offset) {
        positionImFenster = positionImFenster?.plus(delta)
    }

    fun abbrechen() {
        karte = null
        positionImFenster = null
    }

    fun ablegen(zustand: AtlasZustand) {
        val gezogen = karte
        val position = positionImFenster
        val bereich = editorBereich
        if (gezogen != null && position != null && bereich != null && position in bereich) {
            val lokal = position - bereich.topLeft
            val ansicht = zustand.editor.karte.ansicht
            val faktor = (dichte * ansicht.zoom).coerceAtLeast(0.0001f)
            val welt = GraphPunkt(
                (lokal.x - ansicht.verschiebung.x) / faktor,
                (lokal.y - ansicht.verschiebung.y) / faktor,
            )
            val aufKnoten = zustand.editor.karte.knoten.any { knoten ->
                welt.x >= knoten.position.x && welt.x <= knoten.position.x + knoten.größe.breite &&
                    welt.y >= knoten.position.y && welt.y <= knoten.position.y + knoten.größe.höhe
            }
            if (!aufKnoten) zustand.fügeKartenKnotenEin(gezogen, welt - GraphPunkt(120f, 50f))
        }
        abbrechen()
    }
}

private val dragZustände = WeakHashMap<AtlasZustand, KartenDragZustand>()
internal val AtlasZustand.kartenDragZustand: KartenDragZustand
    get() = synchronized(dragZustände) { dragZustände.getOrPut(this) { KartenDragZustand() } }

internal fun Modifier.kartenDragQuelle(zustand: AtlasZustand, karte: KartenDaten): Modifier = composed {
    var ursprung by remember { mutableStateOf(Offset.Zero) }
    onGloballyPositioned { ursprung = it.boundsInWindow().topLeft }
        .pointerInput(karte.id, karte.version) {
            detectDragGesturesAfterLongPress(
                onDragStart = { lokal -> zustand.kartenDragZustand.beginne(karte, ursprung + lokal) },
                onDrag = { änderung, delta ->
                    änderung.consume()
                    zustand.kartenDragZustand.verschiebe(delta)
                },
                onDragEnd = { zustand.kartenDragZustand.ablegen(zustand) },
                onDragCancel = { zustand.kartenDragZustand.abbrechen() },
            )
        }
}

internal fun Modifier.kartenDropZiel(zustand: AtlasZustand, dichte: Float): Modifier =
    onGloballyPositioned {
        zustand.kartenDragZustand.editorBereich = it.boundsInWindow()
        zustand.kartenDragZustand.dichte = dichte
    }
''')
replace_once(
    'app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/VerwaltungsFenster.kt',
    '''                        modifier = Modifier.padding(start = (eintrag.tiefe * 12).dp)\n                            .clip(MaterialTheme.shapes.medium)\n                            .clickable { zustand.öffne(eintrag.karte) }\n                            .background(\n''',
    '''                        modifier = Modifier.padding(start = (eintrag.tiefe * 12).dp)\n                            .clip(MaterialTheme.shapes.medium)\n                            .kartenDragQuelle(zustand, eintrag.karte)\n                            .clickable { zustand.öffne(eintrag.karte) }\n                            .background(\n''',
)
replace_once(
    'app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/MathematikAtlasApp.kt',
    '''                Modifier.weight(1f).fillMaxWidth()\n                    .clipToBounds()\n                    .onSizeChanged { editorGröße = it },\n''',
    '''                Modifier.weight(1f).fillMaxWidth()\n                    .clipToBounds()\n                    .kartenDropZiel(zustand, dichte.density)\n                    .onSizeChanged { editorGröße = it },\n''',
)

# 9. Tests.
write('KnotenKartenVerwalter/src/test/kotlin/de/TeutonStudio/KnotenKartenVerwalter/KnotenErsetzenTest.kt', r'''package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import kotlin.test.*

class KnotenErsetzenTest {
    @Test fun `Knotenersatz entfernt nur Verbindungen zu entfallenen Anschlüssen`() {
        val art = AnschlussArtId("test")
        val quelle = KnotenDaten(art = "quelle", name = "Quelle", anschlüsse = listOf(
            AnschlussDaten(id = AnschlussId("q"), name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art),
        ))
        val alt = KnotenDaten(id = KnotenId("karte"), art = "gruppe.a", name = "Karte", anschlüsse = listOf(
            AnschlussDaten(id = AnschlussId("behalten"), name = "x", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art),
            AnschlussDaten(id = AnschlussId("entfernen"), name = "y", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art),
        ))
        val verbindungen = alt.anschlüsse.mapIndexed { index, anschluss -> VerbindungDaten(
            id = VerbindungsId("v$index"),
            von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
            zu = AnschlussVerweis(alt.id, anschluss.id),
        ) }
        val neu = alt.copy(art = "gruppe.b", anschlüsse = listOf(alt.anschlüsse.first()))

        val ergebnis = KartenDaten(name = "Test", knoten = listOf(quelle, alt), verbindungen = verbindungen)
            .wendeAn(KartenAktion.KnotenErsetzen(neu))

        assertEquals(listOf(VerbindungsId("v0")), ergebnis.verbindungen.map { it.id })
        assertEquals("gruppe.b", ergebnis.knoten.single { it.id == alt.id }.art)
    }
}
''')
write('app/src/test/kotlin/de/TeutonStudio/MathematikAtlas/KartenKnotenTest.kt', r'''package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.*

class KartenKnotenTest {
    @Test fun `öffentliche Anschlüsse folgen der Position auf der Karte`() {
        fun eingang(id: String, name: String, x: Float, y: Float) = KnotenDaten(
            id = KnotenId(id),
            art = "mathematik.kartenEingang",
            name = name,
            position = GraphPunkt(x, y),
            parameter = mapOf("name" to name),
            anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = AnschlussArtId("mathematik.zahl"))),
        )
        val karte = KartenDaten(name = "Test", knoten = listOf(
            eingang("c", "c", 0f, 200f),
            eingang("b", "b", 200f, 100f),
            eingang("a", "a", 100f, 100f),
        ))

        val anschlüsse = öffentlicheKartenAnschlüsse(karte, "mathematik.kartenEingang", AnschlussRichtung.Eingang, AnschlussKante.Links)

        assertEquals(listOf("a", "b", "c"), anschlüsse.map { it.name })
        assertEquals(listOf(0, 1, 2), anschlüsse.map { it.reihenfolge })
    }
}
''')
write('MathematikKartenAdapter/src/test/kotlin/de/TeutonStudio/MathematikKartenAdapter/KartenMethodenTest.kt', r'''package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class KartenMethodenTest {
    @Test fun `Kartenmethode unterstützt mehrere Argumente und Ausgaben`() {
        val zahlArt = AnschlussArtId("mathematik.zahl")
        fun eingang(id: String, name: String, y: Float) = KnotenDaten(
            id = KnotenId(id), art = "mathematik.kartenEingang", name = name, position = GraphPunkt(0f, y),
            parameter = mapOf("name" to name),
            anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = zahlArt)),
        )
        fun ausgang(id: String, name: String, y: Float) = KnotenDaten(
            id = KnotenId(id), art = "mathematik.kartenAusgang", name = name, position = GraphPunkt(400f, y),
            parameter = mapOf("name" to name),
            anschlüsse = listOf(AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = zahlArt)),
        )
        val x = eingang("x", "x", 0f)
        val y = eingang("y", "y", 100f)
        val summe = ausgang("summe", "summe", 0f)
        val erster = ausgang("erster", "erster", 100f)
        val intern = KartenDaten(
            id = KartenId("intern"), name = "Paar", version = 1,
            knoten = listOf(x, y, summe, erster),
            verbindungen = listOf(
                VerbindungDaten(von = AnschlussVerweis(x.id, x.anschlüsse.single().id), zu = AnschlussVerweis(summe.id, summe.anschlüsse.single().id)),
                VerbindungDaten(von = AnschlussVerweis(x.id, x.anschlüsse.single().id), zu = AnschlussVerweis(erster.id, erster.anschlüsse.single().id)),
            ),
        )
        val methode = KnotenDaten(
            art = "methode.intern", name = "f", kartenVerweis = KartenVerweis(intern.id, intern.version),
            anschlüsse = listOf(AnschlussDaten(name = "methode", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = AnschlussArtId("mathematik.funktion"))),
        )
        val register = MathematikAuswerterRegister().apply {
            registriere("mathematik.kartenAusgang") { kontext ->
                val wert = kontext.eingänge.getValue("wert")
                KnotenAuswertungsErgebnis(mapOf("wert" to wert.copy(zielMenge = ReelleZahlen)))
            }
        }

        val ergebnis = KartenAuswerter(register, KartenQuelle { if (it == methode.kartenVerweis) intern else null })
            .auswerten(KartenDaten(name = "Außen", knoten = listOf(methode)))
        val funktion = assertIs<Funktion>(ergebnis.knoten.getValue(methode.id).ausgaben.getValue("methode").objekt)

        assertEquals(listOf("x", "y"), funktion.parameter.map { it.name })
        assertEquals(listOf("summe", "erster"), funktion.ausgaben.keys.toList())
        assertEquals(setOf("summe", "erster"), funktion.zielMengen.keys)
    }
}
''')

# 10. Release-Metadaten und Dokumentation.
replace_once('app/build.gradle.kts', '        versionCode = 2011000\n        versionName = "2.11.0"\n', '        versionCode = 2011001\n        versionName = "2.11.1"\n')
replace_once('release/roadmap.toml', 'current_version = "2.11.0"\n', 'current_version = "2.11.1"\n')
append_once('release/roadmap.toml', 'version = "2.11.1"', '''[[releases]]
version = "2.11.1"
title = "Versionierte KartenKnoten und Karten-Drag-and-drop"
roadmap = "v2.11.x Wiederverwendbare Karten und Methodenknoten"
status = "released"
previous_release = "2.11.0"
branch = "agent/v2.11.1-kartenknoten"
kind = "feature"
version_axis = "x"
reason = "Erweitert den vorhandenen Kartenverweis-Knoten um Schnittstellen- und Methodenzustand, Versionswechsel, Kompatibilitätsprüfung und Drag-and-drop, ohne einen neuen registrierten Knotentyp einzuführen."
''')
append_once('docs/DATEIFORMAT.md', '## KartenKnoten-Zustände', '''## KartenKnoten-Zustände

Ein fest versionierter Kartenverweis besitzt zwei kompatible Darstellungen, ohne das JSON-Schema zu erweitern:

- `gruppe.<karten-id>` spiegelt die öffentlichen `KartenEingang`- und `KartenAusgang`-Knoten als Anschlüsse.
- `methode.<karten-id>` besitzt keine Eingänge und genau einen Funktionsausgang. Alle öffentlichen Karteneingänge werden geordnet nach ihrer Kartenposition zu Methodenparametern; mehrere benannte Kartenausgänge bleiben erhalten.

Karten- und Versionswechsel behalten Anschluss-IDs nur bei gleicher Richtung und gleichem öffentlichen Namen. Dadurch bleiben ausschließlich weiterhin gültige Verbindungen bestehen. Ein Zustandswechsel ersetzt die Schnittstelle atomar und entfernt die betroffenen Verbindungen in einem Undo-Schritt.
''')
write('docs/codex/plans/completed/2026-08-01-v2.11.1-kartenknoten.md', '''# ExecPlan v2.11.1: Versionierte KartenKnoten

## Ziel

Gespeicherte Karten werden als fest versionierte KartenKnoten im eigenen Dialogtab und per Drag-and-drop wiederverwendbar. Ein KartenKnoten kann seine öffentliche Schnittstelle spiegeln oder die Karte als Methode mit Werte- und Zielmengeninformationen bereitstellen.

## Umsetzung

- öffentliche Schnittstellen ausschließlich aus KartenEingang und KartenAusgang, geordnet nach Kartenposition
- Schnittstellenzustand `gruppe.*` und Methodenzustand `methode.*` auf dem bestehenden Kartenverweis-Vertrag
- atomarer Knotenersatz mit Verbindungserhalt bei kompatiblen Karten-/Versionswechseln
- deaktivierte inkompatible Karten und Versionen im Inspector
- manuelles Aktualisieren auf die neueste kompatible Version
- langes Ziehen aus der Kartenliste auf freien Kartenraum
- Karten ausschließlich im Tab Karten des Knoten-erstellen-Dialogs
- Methoden mit beliebig vielen Parametern und benannten Ausgaben

## Rückwärtskompatibilität

Bestehende `gruppe.*`-Knoten bleiben unverändert lesbar. Der Zustand benötigt keine neue JSON-Eigenschaft und keine Datenmigration. Feste Kartenverweise werden weiterhin nicht automatisch aktualisiert.

## Verifikation

- Release- und Versionsfolgeprüfung
- Architekturprüfung
- vollständige JVM-Tests
- `:app:assembleDebug`
''')

print('v2.11.1-Patch angewendet')
