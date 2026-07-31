package de.TeutonStudio.KnotenKartenVerwalter.daten

/**
 * Erzeugt aus einer vorhandenen Karte eine vollständig unabhängige Arbeitskopie.
 *
 * Sämtliche innerhalb der Karte vergebenen Identitäten werden ersetzt und alle
 * Verweise auf Knoten und Anschlüsse entsprechend umgeschrieben. Externe
 * [KartenVerweis]e bleiben erhalten.
 */
fun KartenDaten.alsNeueKarte(neuerName: String = "$name – Kopie"): KartenDaten {
    val kopierteKartenId = neueKartenId()
    val neueKnotenIds = knoten.associate { it.id to neueKnotenId() }
    val neueAnschlussIds = knoten
        .flatMap { knotenDaten ->
            knotenDaten.anschlüsse.map { anschluss ->
                AnschlussVerweis(knotenDaten.id, anschluss.id) to neueAnschlussId()
            }
        }
        .toMap()

    fun AnschlussVerweis.neu(): AnschlussVerweis = AnschlussVerweis(
        knotenId = neueKnotenIds.getValue(knotenId),
        anschlussId = neueAnschlussIds.getValue(this),
    )

    val kopierteKnoten = knoten.map { knotenDaten ->
        knotenDaten.copy(
            id = neueKnotenIds.getValue(knotenDaten.id),
            anschlüsse = knotenDaten.anschlüsse.map { anschluss ->
                anschluss.copy(
                    id = neueAnschlussIds.getValue(AnschlussVerweis(knotenDaten.id, anschluss.id)),
                )
            },
        )
    }
    val kopierteVerbindungen = verbindungen.map { verbindung ->
        verbindung.copy(
            id = neueVerbindungsId(),
            von = verbindung.von.neu(),
            zu = verbindung.zu.neu(),
        )
    }
    val kopierteGruppen = visuelleGruppen.map { gruppe ->
        gruppe.copy(
            id = neueVisuelleGruppenId(),
            knotenIds = gruppe.knotenIds.mapTo(linkedSetOf()) { neueKnotenIds.getValue(it) },
        )
    }

    return copy(
        id = kopierteKartenId,
        name = neuerName,
        version = 1,
        erstelltAm = System.currentTimeMillis(),
        knoten = kopierteKnoten,
        verbindungen = kopierteVerbindungen,
        visuelleGruppen = kopierteGruppen,
        archiviert = false,
    )
}
