package de.TeutonStudio.KnotenKartenVerwalter.zustand

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion

enum class AnschlussEinfügePosition { Davor, Danach }

fun KartenEditorZustand.kannAnschlussRelativEinfügen(ref: AnschlussVerweis): Boolean {
    val knoten = karte.knoten.firstOrNull { it.id == ref.knotenId } ?: return false
    val anschluss = knoten.anschlüsse.firstOrNull { it.id == ref.anschlussId } ?: return false
    return anschluss.richtung == AnschlussRichtung.Eingang && anschluss.kannSichErweitern
}

/**
 * Fügt einen festen Eingang unmittelbar vor oder nach dem gewählten Anschluss ein.
 * Bestehende Anschluss-IDs und damit sämtliche vorhandenen Verbindungen bleiben erhalten.
 */
fun KartenEditorZustand.fügeAnschlussRelativEin(
    ref: AnschlussVerweis,
    position: AnschlussEinfügePosition,
) {
    val knoten = karte.knoten.firstOrNull { it.id == ref.knotenId } ?: return
    val ziel = knoten.anschlüsse.firstOrNull { it.id == ref.anschlussId } ?: return
    if (ziel.richtung != AnschlussRichtung.Eingang || !ziel.kannSichErweitern) return

    val aufKante = knoten.anschlüsse
        .filter { it.kante == ziel.kante }
        .sortedBy { it.reihenfolge }
    val zielIndex = aufKante.indexOfFirst { it.id == ziel.id }
    if (zielIndex < 0) return

    val vorhanden = knoten.anschlüsse.mapTo(mutableSetOf()) { it.name }
    val neuerAnschluss = ziel.copy(
        id = neueAnschlussId(),
        name = eindeutigerErweiterungsName(knoten, vorhanden),
        dynamischErzeugt = false,
    )
    val einfügeIndex = zielIndex + if (position == AnschlussEinfügePosition.Danach) 1 else 0
    val normalisierteKante = aufKante.toMutableList().apply {
        add(einfügeIndex, neuerAnschluss)
    }.mapIndexed { index, anschluss -> anschluss.copy(reihenfolge = index) }

    val neueAnschlüsse = knoten.anschlüsse.filterNot { it.kante == ziel.kante } + normalisierteKante
    val festeEingänge = neueAnschlüsse.count {
        it.richtung == AnschlussRichtung.Eingang && !it.dynamischErzeugt
    }
    führeAus(
        KartenAktion.KnotenKonfigurationErsetzen(
            id = knoten.id,
            parameter = knoten.parameter + ("festeEingänge" to festeEingänge.toString()),
            anschlüsse = neueAnschlüsse,
        ),
    )
}

private fun eindeutigerErweiterungsName(knoten: KnotenDaten, vorhanden: Set<String>): String {
    val eingangsAnzahl = knoten.anschlüsse.count { it.richtung == AnschlussRichtung.Eingang }
    val höchsteInputNummer = vorhanden.asSequence()
        .mapNotNull { name -> name.removePrefix("input").takeIf { name.startsWith("input") }?.toIntOrNull() }
        .maxOrNull()
        ?: 0
    var nummer = maxOf(eingangsAnzahl + 1, höchsteInputNummer + 1)
    var kandidat = "input$nummer"
    while (kandidat in vorhanden) {
        kandidat = "input${++nummer}"
    }
    return kandidat
}
