package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.findeAnschluss
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.TypInferenzRegel

fun joinOperatorId(knoten: KnotenDaten): String? = when (knoten.art) {
    "mathematik.addition" -> "zahl.addition"
    "mathematik.multiplikation" -> "zahl.multiplikation"
    "mathematik.extremwert" -> "zahl." + knoten.parameter.getOrDefault("modus", "maximum")
    "mathematik.vereinigung" -> "menge.vereinigung"
    "mathematik.schnitt" -> "menge.schnitt"
    "mathematik.konjunktion" -> "aussage.konjunktion"
    "mathematik.disjunktion" -> "aussage.disjunktion"
    "mathematik.adjunktion" -> "aussage.adjunktion"
    "mathematik.äquivalenz" -> "aussage.aequivalenz"
    else -> knoten.parameter[RechnerOperatorRegistry.familieFür(knoten.art)?.operatorParameter ?: "operator"] ?: knoten.parameter["operator"]
}

/** Nur die Operandenrollen werden zusammengelegt, niemals Hilfs-/Achsen-/Methodenparameter. */
private fun joinPorts(knoten: KnotenDaten): List<AnschlussDaten> {
    if (!istJoinRechnerOperator(joinOperatorId(knoten).orEmpty())) return emptyList()
    val eingänge = knoten.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }
    return eingänge.filter { it.belegung == AnschlussBelegung.Mehrfach || it.kannSichErweitern }
        .ifEmpty { eingänge }.sortedBy { it.reihenfolge }
}

/** Idempotente fachliche Migration, ohne Dateien oder historische Kartenversionen zu schreiben. */
fun KartenDaten.normalisiereJoinEingänge(): KartenDaten {
    val ziele = mutableMapOf<AnschlussVerweis, Pair<AnschlussVerweis, Int>>()
    val fallbackVerbindungen = mutableMapOf<VerbindungsId, String>()
    val neu = knoten.map { k ->
        if (RECHNER_MIGRATION_DIAGNOSE in k.parameter) return@map k
        val ports = joinPorts(k)
        if (ports.isEmpty()) return@map k
        val join = ports.firstOrNull { it.belegung == AnschlussBelegung.Mehrfach } ?: ports.first()
        val namen = (ports.map { it.name } + k.parameter["join.rollen"].orEmpty().split(",")).filter { it.isNotBlank() }.toSet()
        val ids = ports.map { it.id }.toSet()
        val parameter = k.parameter.toMutableMap()
        parameter["join.rollen"] = namen.sorted().joinToString(",")
        ports.forEachIndexed { index, port ->
            ziele[AnschlussVerweis(k.id, port.id)] = AnschlussVerweis(k.id, join.id) to index
            // Nur explizite und tatsächlich unverbundene Standardwerte sind Operanden.
            val standard = parameter.remove("standardwert.${port.name}")
            if (!standard.isNullOrBlank() && verbindungen.none { it.zu == AnschlussVerweis(k.id, port.id) }) {
                parameter["join.lokal.${port.id.wert}"] = standard
                parameter["join.position.${port.id.wert}"] = index.toString()
            }
        }
        val fallback = k.eingangsKartenVerweise.toMutableMap()
        ports.forEachIndexed { index, port ->
            fallback.remove(port.name)?.let { verweis ->
                val name = join.name + "@karte:" + port.id.wert
                fallback[name] = verweis
                parameter["join.position.karte:" + port.id.wert] = index.toString()
                verbindungen.firstOrNull { it.zu == AnschlussVerweis(k.id, port.id) }?.let {
                    fallbackVerbindungen[it.id] = name
                }
            }
        }
        val aktualisiertePorts = k.anschlüsse.filterNot { it.id in ids && it.id != join.id }.map { port ->
            if (port.id == join.id) port.copy(belegung = AnschlussBelegung.Mehrfach, kannSichErweitern = false, dynamischErzeugt = false,
                nächsteBeitragsReihenfolge = maxOf(join.nächsteBeitragsReihenfolge, ports.size,
                    (verbindungen.filter { it.zu.knotenId == k.id && it.zu.anschlussId in ids }.maxOfOrNull { it.eingangsReihenfolge } ?: -1) + 1,
                    (parameter.filterKeys { it.startsWith("join.position.") }.values.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: -1) + 1))
            else port.copy(
                artVereinigtEingänge = port.artVereinigtEingänge.map { if (it in namen) join.name else it }.distinct(),
                artPriorisiertEingänge = port.artPriorisiertEingänge?.let { regel -> regel.copy(eingänge = regel.eingänge.map { if (it in namen) join.name else it }.distinct()) },
                typInferenz = when (val regel = port.typInferenz) {
                    is TypInferenzRegel.GemeinsameOberart -> regel.copy(eingänge = regel.eingänge.map { if (it in namen) join.name else it }.distinct())
                    is TypInferenzRegel.Priorisierung -> regel.copy(eingänge = regel.eingänge.map { if (it in namen) join.name else it }.distinct())
                    is TypInferenzRegel.Vereinigung -> regel.copy(eingänge = regel.eingänge.map { if (it in namen) join.name else it }.distinct())
                    else -> regel
                },
            )
        }
        k.copy(anschlüsse = aktualisiertePorts, parameter = parameter, eingangsKartenVerweise = fallback)
    }
    return copy(knoten = neu, verbindungen = verbindungen.map { v ->
        ziele[v.zu]?.let { (ziel, reihe) ->
            if (ziel == v.zu && findeAnschluss(v.zu)?.belegung == AnschlussBelegung.Mehrfach) v.copy(eingangsFallback = fallbackVerbindungen[v.id] ?: v.eingangsFallback)
            else v.copy(zu = ziel, eingangsReihenfolge = reihe, eingangsFallback = fallbackVerbindungen[v.id] ?: v.eingangsFallback)
        } ?: v
    })
}

fun joinVorlage(vorlage: KnotenVorlage): KnotenVorlage {
    val k = KartenDaten(name = "Join", knoten = listOf(vorlage.erzeuge(GraphPunkt.Zero))).normalisiereJoinEingänge().knoten.single()
    return vorlage.copy(anschlüsse = k.anschlüsse, standardParameter = k.parameter)
}

/** Der mathematische Nachweis ist von der generischen Anschlussdarstellung getrennt. */
fun zeigeJoinKapsel(
    karte: KartenDaten,
    knoten: KnotenDaten,
    ergebnisse: Map<KnotenId, KnotenAuswertungsErgebnis> = emptyMap(),
): Boolean {
    val id = joinOperatorId(knoten) ?: return true
    if (!benoetigtJoinKommutativitaetsNachweis(id)) return true
    val quellen = karte.verbindungen.filter { it.zu.knotenId == knoten.id }.map { it.von }
    val lokal = knoten.parameter.filterKeys { it.startsWith("join.lokal.") }.values
    if (quellen.isEmpty() && lokal.isEmpty()) return false
    fun kommutativ(objekt: AtlasWert, vorrat: MengenAusdruck?): Boolean = when (objekt) {
        is ZahlAusdruck -> inferiereZahlenRechnerBereich(objekt, vorrat).multiplikativKommutativ
        is OrientierterVektor -> objekt.werte.all { inferiereZahlenRechnerBereich(it).multiplikativKommutativ }
        is Matrix -> objekt.zeilen.flatten().all { inferiereZahlenRechnerBereich(it).multiplikativKommutativ }
        is Tensor -> objekt.werte.all { inferiereZahlenRechnerBereich(it).multiplikativKommutativ }
        else -> false
    }
    return quellen.all { ref ->
        val quelle = karte.knoten.firstOrNull { it.id == ref.knotenId } ?: return@all false
        val name = karte.findeAnschluss(ref)?.name ?: return@all false
        val wert = ergebnisse[quelle.id]?.takeIf { it.fehler == null }?.ausgaben?.get(name)
        if (wert != null) kommutativ(wert.objekt, wert.werteVorrat)
        else quelle.art == "mathematik.zahl" && runCatching { RationaleZahl.parse(quelle.parameter["wert"].orEmpty()) }.isSuccess
    } && lokal.all { runCatching { RationaleZahl.parse(it) }.isSuccess }
}

/** Die freie Andockreserve ist kein Operand der Formel. */
internal fun joinOperatorFormel(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, zeichen: String): String? {
    if (knoten.anschlüsse.none { it.belegung == AnschlussBelegung.Mehrfach }) return null
    val werte = ergebnis?.eingänge?.filterKeys { "@" in it }?.values.orEmpty()
    if (werte.isEmpty()) return "\\ldots"
    return werte.mapIndexed { index, wert ->
        if (knoten.parameter["operatorAnzeige"] == "name") eingabeLatex(index + 1) else wert.anzeigeLatex()
    }.joinToString(zeichen)
}
