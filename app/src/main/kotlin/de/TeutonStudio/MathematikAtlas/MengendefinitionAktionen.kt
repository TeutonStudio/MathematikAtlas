package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.AuswahlModus
import de.TeutonStudio.MathematikKartenAdapter.FALTUNG_PAAR
import de.TeutonStudio.MathematikKartenAdapter.MENGENDEFINITION_PAAR
import de.TeutonStudio.MathematikKnoten.erzeugeFaltungsPaar
import de.TeutonStudio.MathematikKnoten.erzeugeMengendefinitionsPaar
import de.TeutonStudio.MathematikKnoten.faltungsPaarId
import de.TeutonStudio.MathematikKnoten.mengendefinitionsPaarId
import java.util.UUID

fun AtlasZustand.kannMengendefinitionEinfügen(): Boolean = knotenAuswahlStart == null
fun AtlasZustand.kannFaltungEinfügen(): Boolean = knotenAuswahlStart == null

fun AtlasZustand.fügeMengendefinitionEin(position: GraphPunkt) {
    if (!kannMengendefinitionEinfügen()) return
    val paar = erzeugeMengendefinitionsPaar(position)
    fügeGekoppeltesPaarEin(listOf(paar.konstruktor, paar.definator), paar.konstruktor.id)
}

fun AtlasZustand.fügeFaltungEin(position: GraphPunkt) {
    if (!kannFaltungEinfügen()) return
    val paar = erzeugeFaltungsPaar(position)
    fügeGekoppeltesPaarEin(listOf(paar.konstruktor, paar.definator), paar.konstruktor.id)
}

private fun AtlasZustand.fügeGekoppeltesPaarEin(knoten: List<KnotenDaten>, hauptKnoten: KnotenId) {
    val ids = knoten.mapTo(linkedSetOf()) { it.id }
    editor.beginneInteraktion()
    editor.führeAus(
        KartenAktion.KnotenMehrfachEinfügen(knoten = knoten, verbindungen = emptyList()),
        mitHistorie = false,
    )
    editor.führeAus(KartenAktion.VisuelleGruppeErstellen(ids), mitHistorie = false)
    editor.beendeInteraktion()
    editor.stelleAuswahlWiederHer(ids, hauptKnoten)
    schließeKnotenAuswahl()
}

fun AtlasZustand.löscheAuswahlMitMengendefinition() {
    val basis = ausgewählteKnotenIds()
    if (basis.isEmpty()) {
        editor.löscheAuswahl()
        return
    }
    val erweitert = erweitereUmGekoppeltePaare(basis)
    if (erweitert == basis) {
        editor.löscheAuswahl()
        return
    }
    editor.führeAus(KartenAktion.KnotenMehrfachLöschen(erweitert))
    editor.wähleKnoten(null)
}

fun AtlasZustand.dupliziereAuswahlMitMengendefinition() {
    val basis = ausgewählteKnotenIds()
    if (basis.isEmpty()) return
    val ids = erweitereUmGekoppeltePaare(basis)
    val besitztKopplung = ids.any { id ->
        editor.karte.knoten.firstOrNull { it.id == id }?.gekoppeltePaarKennung() != null
    }
    if (ids == basis && !besitztKopplung) {
        editor.dupliziereAuswahl()
        return
    }

    val originale = editor.karte.knoten.filter { it.id in ids }
    val knotenIds = originale.associate { it.id to neueKnotenId() }
    val anschlussIds = originale.flatMap { knoten -> knoten.anschlüsse.map { it.id } }
        .associateWith { neueAnschlussId() }
    val neuePaarIds = originale.mapNotNull(KnotenDaten::gekoppeltePaarKennung)
        .distinct()
        .associateWith { UUID.randomUUID().toString() }

    val kopien = originale.map { original ->
        val kennung = original.gekoppeltePaarKennung()
        original.copy(
            id = knotenIds.getValue(original.id),
            name = "${original.name} Kopie",
            position = original.position + GraphPunkt(28f, 28f),
            anschlüsse = original.anschlüsse.map { anschluss ->
                anschluss.copy(id = anschlussIds.getValue(anschluss.id))
            },
            parameter = when {
                kennung == null -> original.parameter
                kennung.startsWith("menge:") -> original.parameter +
                    (MENGENDEFINITION_PAAR to neuePaarIds.getValue(kennung))
                else -> original.parameter + (FALTUNG_PAAR to neuePaarIds.getValue(kennung))
            },
        )
    }
    val interneVerbindungen = editor.karte.verbindungen.filter {
        it.von.knotenId in ids && it.zu.knotenId in ids
    }.map { verbindung ->
        VerbindungDaten(
            von = AnschlussVerweis(
                knotenIds.getValue(verbindung.von.knotenId),
                anschlussIds.getValue(verbindung.von.anschlussId),
            ),
            zu = AnschlussVerweis(
                knotenIds.getValue(verbindung.zu.knotenId),
                anschlussIds.getValue(verbindung.zu.anschlussId),
            ),
        )
    }

    editor.beginneInteraktion()
    editor.führeAus(KartenAktion.KnotenMehrfachEinfügen(kopien, interneVerbindungen), mitHistorie = false)
    kopien.groupBy(KnotenDaten::gekoppeltePaarKennung)
        .filterKeys { it != null }
        .values
        .map { gruppe -> gruppe.mapTo(linkedSetOf()) { it.id } }
        .filter { it.size == 2 }
        .forEach { gruppe -> editor.führeAus(KartenAktion.VisuelleGruppeErstellen(gruppe), mitHistorie = false) }
    editor.beendeInteraktion()
    editor.stelleAuswahlWiederHer(kopien.mapTo(linkedSetOf()) { it.id }, kopien.lastOrNull()?.id)
}

private fun AtlasZustand.ausgewählteKnotenIds(): Set<KnotenId> =
    if (editor.auswahlModus == AuswahlModus.Gruppe) editor.ausgewählteKnoten
    else setOfNotNull(editor.ausgewählterKnoten)

private fun AtlasZustand.erweitereUmGekoppeltePaare(ids: Set<KnotenId>): Set<KnotenId> {
    val paarKennungen = editor.karte.knoten
        .filter { it.id in ids }
        .mapNotNull(KnotenDaten::gekoppeltePaarKennung)
        .toSet()
    if (paarKennungen.isEmpty()) return ids
    return ids + editor.karte.knoten
        .filter { it.gekoppeltePaarKennung() in paarKennungen }
        .map { it.id }
}

private fun KnotenDaten.gekoppeltePaarKennung(): String? = when {
    mengendefinitionsPaarId() != null -> "menge:${mengendefinitionsPaarId()}"
    faltungsPaarId() != null -> "faltung:${faltungsPaarId()}"
    else -> null
}
