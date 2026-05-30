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
)
