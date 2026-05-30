package com.TeutonStudio.KnotenKartenVerwalter.daten

import androidx.compose.ui.geometry.Offset
import java.util.UUID

/**
 * Datenpaket fuer Copy/Paste innerhalb einer Knotenkarte.
 *
 * Beim Einfuegen muessen IDs in der Regel neu vergeben werden. Deshalb speichert diese Struktur
 * nur die kopierten Elemente und die Ursprungsauswahl; die konkrete ID-Neuzuordnung gehoert in die
 * spaetere Einfuege-Logik.
 */
data class KartenZwischenablage(
    /** Kopierte Knoten mit ihren urspruenglichen IDs und Positionen. */
    val knoten: List<KnotenDaten> = emptyList(),

    /** Kopierte Verbindungen, die zwischen den kopierten oder bestehenden Knoten liegen koennen. */
    val verbindungen: List<VerbindungDaten> = emptyList(),

    /** Auswahl, aus der diese Zwischenablage erzeugt wurde. */
    val ursprungsAuswahl: AuswahlDaten = AuswahlDaten(),
) {
    val istLeer: Boolean
        get() = knoten.isEmpty() && verbindungen.isEmpty()
}

data class KartenEinfuegeErgebnis(
    val karte: KarteDaten,
    val auswahl: AuswahlDaten,
)

fun KarteDaten.kopiereAuswahl(auswahl: AuswahlDaten): KartenZwischenablage {
    if (auswahl.istLeer) return KartenZwischenablage()

    val kopierteKnoten = knoten.filter { it.id in auswahl.knotenIds }
    val kopierteKnotenIds = kopierteKnoten.mapTo(mutableSetOf()) { it.id }
    val kopierteVerbindungen = verbindungen.filter { verbindung ->
        verbindung.id in auswahl.verbindungIds ||
            (verbindung.quellKnotenId in kopierteKnotenIds && verbindung.zielKnotenId in kopierteKnotenIds)
    }

    return KartenZwischenablage(
        knoten = kopierteKnoten,
        verbindungen = kopierteVerbindungen,
        ursprungsAuswahl = auswahl,
    )
}

fun KarteDaten.fuegeEin(
    zwischenablage: KartenZwischenablage,
    zielPosition: Offset,
    neueId: () -> String = { UUID.randomUUID().toString() },
): KartenEinfuegeErgebnis {
    if (zwischenablage.knoten.isEmpty()) {
        return KartenEinfuegeErgebnis(this, AuswahlDaten())
    }

    val ursprung = zwischenablage.knoten.minimalePosition()
    val idZuNeu = zwischenablage.knoten.associate { it.id to neueId() }
    val neueKnoten = zwischenablage.knoten.map { knoten ->
        knoten.copy(
            id = idZuNeu.getValue(knoten.id),
            position = zielPosition + (knoten.position - ursprung),
            ausgewaehlt = false,
        )
    }
    val neueKnotenIds = neueKnoten.mapTo(mutableSetOf()) { it.id }
    val neueVerbindungen = zwischenablage.verbindungen.mapNotNull { verbindung ->
        val neueQuelle = idZuNeu[verbindung.quellKnotenId] ?: return@mapNotNull null
        val neuesZiel = idZuNeu[verbindung.zielKnotenId] ?: return@mapNotNull null
        verbindung.copy(
            id = neueId(),
            quellKnotenId = neueQuelle,
            zielKnotenId = neuesZiel,
            ausgewaehlt = false,
        )
    }

    return KartenEinfuegeErgebnis(
        karte = copy(
            knoten = knoten.map { it.copy(ausgewaehlt = false) } + neueKnoten,
            verbindungen = verbindungen.map { it.copy(ausgewaehlt = false) } + neueVerbindungen,
        ),
        auswahl = AuswahlDaten(
            knotenIds = neueKnotenIds,
            verbindungIds = neueVerbindungen.mapTo(mutableSetOf()) { it.id },
        ),
    )
}

private fun List<KnotenDaten>.minimalePosition(): Offset {
    val erster = first()
    var x = erster.position.x
    var y = erster.position.y
    drop(1).forEach { knoten ->
        x = minOf(x, knoten.position.x)
        y = minOf(y, knoten.position.y)
    }
    return Offset(x, y)
}
