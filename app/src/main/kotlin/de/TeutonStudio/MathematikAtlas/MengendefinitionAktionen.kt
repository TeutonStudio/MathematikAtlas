package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.AuswahlModus
import de.TeutonStudio.MathematikKartenAdapter.MENGENDEFINITION_PAAR
import de.TeutonStudio.MathematikKnoten.erzeugeMengendefinitionsPaar
import de.TeutonStudio.MathematikKnoten.mengendefinitionsPaarId
import java.util.UUID

fun AtlasZustand.kannMengendefinitionEinfügen(): Boolean = knotenAuswahlStart == null

fun AtlasZustand.fügeMengendefinitionEin(position: GraphPunkt) {
    if (!kannMengendefinitionEinfügen()) return
    val paar = erzeugeMengendefinitionsPaar(position)
    val ids = setOf(paar.konstruktor.id, paar.definator.id)

    editor.beginneInteraktion()
    editor.führeAus(
        KartenAktion.KnotenMehrfachEinfügen(
            knoten = listOf(paar.konstruktor, paar.definator),
            verbindungen = emptyList(),
        ),
        mitHistorie = false,
    )
    editor.führeAus(KartenAktion.VisuelleGruppeErstellen(ids), mitHistorie = false)
    editor.beendeInteraktion()
    editor.stelleAuswahlWiederHer(ids, paar.konstruktor.id)
    schließeKnotenAuswahl()
}

fun AtlasZustand.löscheAuswahlMitMengendefinition() {
    val basis = ausgewählteKnotenIds()
    if (basis.isEmpty()) {
        editor.löscheAuswahl()
        return
    }
    val erweitert = erweitereUmMengendefinitionsPaare(basis)
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
    val ids = erweitereUmMengendefinitionsPaare(basis)
    if (ids == basis && basis.none { id -> editor.karte.knoten.firstOrNull { it.id == id }?.mengendefinitionsPaarId() != null }) {
        editor.dupliziereAuswahl()
        return
    }

    val originale = editor.karte.knoten.filter { it.id in ids }
    val knotenIds = originale.associate { it.id to neueKnotenId() }
    val anschlussIds = originale.flatMap { knoten -> knoten.anschlüsse.map { it.id } }
        .associateWith { neueAnschlussId() }
    val neuePaarIds = originale.mapNotNull { it.mengendefinitionsPaarId() }
        .distinct()
        .associateWith { UUID.randomUUID().toString() }

    val kopien = originale.map { original ->
        val altePaarId = original.mengendefinitionsPaarId()
        original.copy(
            id = knotenIds.getValue(original.id),
            name = "${original.name} Kopie",
            position = original.position + GraphPunkt(28f, 28f),
            anschlüsse = original.anschlüsse.map { anschluss ->
                anschluss.copy(id = anschlussIds.getValue(anschluss.id))
            },
            parameter = if (altePaarId == null) original.parameter else
                original.parameter + (MENGENDEFINITION_PAAR to neuePaarIds.getValue(altePaarId)),
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
    kopien.groupBy { it.mengendefinitionsPaarId() }
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

private fun AtlasZustand.erweitereUmMengendefinitionsPaare(ids: Set<KnotenId>): Set<KnotenId> {
    val paarIds = editor.karte.knoten
        .filter { it.id in ids }
        .mapNotNull { it.mengendefinitionsPaarId() }
        .toSet()
    if (paarIds.isEmpty()) return ids
    return ids + editor.karte.knoten
        .filter { it.mengendefinitionsPaarId() in paarIds }
        .map { it.id }
}
