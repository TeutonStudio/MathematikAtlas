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
) {
    fun darfErstellen(
        vorhandeneVerbindungen: List<VerbindungDaten>,
        neueVerbindung: VerbindungDaten,
        quellRichtung: AnschlussRichtung = AnschlussRichtung.Ausgang,
        zielRichtung: AnschlussRichtung = AnschlussRichtung.Eingang,
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

        return true
    }
}

private fun VerbindungDaten.gleicheAnschluesseWie(andere: VerbindungDaten): Boolean =
    quellKnotenId == andere.quellKnotenId &&
        quellAnschlussId == andere.quellAnschlussId &&
        zielKnotenId == andere.zielKnotenId &&
        zielAnschlussId == andere.zielAnschlussId
