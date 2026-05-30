package com.TeutonStudio.KnotenKartenVerwalter.daten

/**
 * Regeln fuer das Erstellen neuer Verbindungen.
 *
 * Die Struktur bildet den spaeteren Ersatz fuer fest verdrahtete Pruefungen. Dadurch kann die App
 * steuern, ob Selbstverbindungen, doppelte Verbindungen oder mehrere Verbindungen pro Anschluss
 * erlaubt sind.
 */
data class VerbindungsRegeln(
    /** Erlaubt eine Verbindung von einem Knoten zu sich selbst. */
    val selbstVerbindungErlaubt: Boolean = false,

    /** Erlaubt mehrere identische Verbindungen zwischen denselben Anschluessen. */
    val doppelteVerbindungenErlaubt: Boolean = false,

    /** Erlaubt eine Verbindung nur von Ausgang nach Eingang. */
    val nurAusgangZuEingang: Boolean = true,

    /** Prueft optionale mathematische Anschluss-Typen auf Kompatibilitaet. */
    val pruefeZahlenTypen: Boolean = true,
) {
    fun darfErstellen(
        vorhandeneVerbindungen: List<VerbindungDaten>,
        neueVerbindung: VerbindungDaten,
        quellRichtung: AnschlussRichtung = AnschlussRichtung.Ausgang,
        zielRichtung: AnschlussRichtung = AnschlussRichtung.Eingang,
        quellTyp: ZahlenTyp? = null,
        zielTyp: ZahlenTyp? = null,
    ): Boolean {
        if (!selbstVerbindungErlaubt && neueVerbindung.quellKnotenId == neueVerbindung.zielKnotenId) {
            return false
        }

        if (nurAusgangZuEingang && (quellRichtung != AnschlussRichtung.Ausgang || zielRichtung != AnschlussRichtung.Eingang)) {
            return false
        }

        if (!doppelteVerbindungenErlaubt && vorhandeneVerbindungen.any { it.gleicheAnschluesseWie(neueVerbindung) }) {
            return false
        }

        if (pruefeZahlenTypen && quellTyp != null && zielTyp != null && !quellTyp.istKompatibelMit(zielTyp)) {
            return false
        }

        return true
    }
}

fun VerbindungDaten.mitTypPruefung(
    quellTyp: ZahlenTyp?,
    zielTyp: ZahlenTyp?,
): VerbindungDaten {
    val fehler = if (quellTyp != null && zielTyp != null && !quellTyp.istKompatibelMit(zielTyp)) {
        "${quellTyp.kurzform} passt nicht zu ${zielTyp.kurzform}"
    } else {
        null
    }
    return copy(zahlenTyp = quellTyp, fehler = fehler)
}

private fun VerbindungDaten.gleicheAnschluesseWie(andere: VerbindungDaten): Boolean =
    quellKnotenId == andere.quellKnotenId &&
        quellAnschlussId == andere.quellAnschlussId &&
        zielKnotenId == andere.zielKnotenId &&
        zielAnschlussId == andere.zielAnschlussId
