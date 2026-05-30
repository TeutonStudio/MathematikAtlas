package com.TeutonStudio.KnotenKartenVerwalter.daten

import androidx.compose.ui.geometry.Offset
import java.util.UUID

/**
 * Beschreibt Bearbeitungsaktionen, die durch Tastatur oder Kontextmenue entstehen koennen.
 *
 * Die Aktionen enthalten nur Absicht und Ziel. Die eigentliche Aenderung an `KarteDaten` bleibt im
 * aufrufenden Code, damit Undo/Redo und Persistenz dort sauber kontrolliert werden koennen.
 */
sealed class KartenBearbeitungsAktion {
    /** Entfernt die aktuell ausgewaehlten Elemente. */
    data class AuswahlLoeschen(val auswahl: AuswahlDaten) : KartenBearbeitungsAktion()

    /** Kopiert die aktuell ausgewaehlten Elemente in eine fachliche Zwischenablage. */
    data class AuswahlKopieren(val auswahl: AuswahlDaten) : KartenBearbeitungsAktion()

    /** Fuegt eine vorher kopierte Auswahl an einer Zielposition wieder ein. */
    data class Einfuegen(val zielPosition: Offset) : KartenBearbeitungsAktion()

    /** Bricht die laufende Interaktion ab, zum Beispiel Verbindungserstellung oder Auswahlrechteck. */
    data object Abbrechen : KartenBearbeitungsAktion()

    /** Fordert den aufrufenden Code auf, den letzten Schritt rueckgaengig zu machen. */
    data object Rueckgaengig : KartenBearbeitungsAktion()

    /** Fordert den aufrufenden Code auf, einen rueckgaengig gemachten Schritt erneut auszufuehren. */
    data object Wiederholen : KartenBearbeitungsAktion()
}

fun KarteDaten.auswahlImBereich(
    bereich: KartenGrenzenDaten,
    bestehendeAuswahl: AuswahlDaten = AuswahlDaten(),
    erweitern: Boolean = false,
): AuswahlDaten {
    val knotenIds = knoten
        .filter { it.ueberschneidet(bereich) }
        .mapTo(mutableSetOf()) { it.id }
    val knotenIdsImBereich = knotenIds.toSet()
    val verbindungIds = verbindungen
        .filter { it.quellKnotenId in knotenIdsImBereich && it.zielKnotenId in knotenIdsImBereich }
        .mapTo(mutableSetOf()) { it.id }
    val neueAuswahl = AuswahlDaten(knotenIds, verbindungIds)
    return if (erweitern) bestehendeAuswahl.plus(neueAuswahl) else neueAuswahl
}

fun KarteDaten.loescheAuswahl(auswahl: AuswahlDaten): KarteDaten {
    if (auswahl.istLeer) return this
    return copy(
        knoten = knoten.filterNot { it.id in auswahl.knotenIds },
        verbindungen = verbindungen.filterNot {
            it.id in auswahl.verbindungIds ||
                it.quellKnotenId in auswahl.knotenIds ||
                it.zielKnotenId in auswahl.knotenIds
        },
    )
}

fun KarteDaten.dupliziereAuswahl(
    auswahl: AuswahlDaten,
    verschiebung: Offset = Offset(32f, 32f),
    neueId: () -> String = { UUID.randomUUID().toString() },
): KartenEinfuegeErgebnis {
    val zwischenablage = kopiereAuswahl(auswahl)
    if (zwischenablage.istLeer || zwischenablage.knoten.isEmpty()) {
        return KartenEinfuegeErgebnis(this, AuswahlDaten())
    }

    val zielPosition = zwischenablage.knoten.minOfPosition() + verschiebung
    return fuegeEin(zwischenablage, zielPosition, neueId)
}

private fun KnotenDaten.ueberschneidet(bereich: KartenGrenzenDaten): Boolean {
    val links = position.x
    val oben = position.y
    val rechts = position.x + fläche.x
    val unten = position.y + fläche.y
    return rechts >= bereich.links &&
        links <= bereich.rechts &&
        unten >= bereich.oben &&
        oben <= bereich.unten
}

private fun List<KnotenDaten>.minOfPosition(): Offset {
    val erster = first()
    var x = erster.position.x
    var y = erster.position.y
    drop(1).forEach { knoten ->
        x = minOf(x, knoten.position.x)
        y = minOf(y, knoten.position.y)
    }
    return Offset(x, y)
}
