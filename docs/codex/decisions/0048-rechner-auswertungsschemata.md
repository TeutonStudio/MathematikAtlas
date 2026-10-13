# ADR 0048: Gemeinsame Rechner-Auswertungsschemata

Datum: 2026-10-12
Status: Angenommen für den lokalen Arbeitsstand

Android und Desktop verwenden denselben Compose-Inhalt für Rechner-Auswertungsschemata. RechnerOperatorRegistry liefert die Definition, KnotenAuswertungsErgebnis die tatsächlichen Werte und vorhandenen Schritte. Spezialisierte Ansichten lesen bestehende fachliche Kernverträge (Tensorartig, detailliertesFalkSchema, Methodensignatur), statt eigene Rechenregeln einzuführen.

Ein Schema ist eine nicht persistierte Projektion; Auswahl gehört zum kurzlebigen UI-Zustand. Android unterscheidet den allgemeinen Schema-Reiter vom speziellen Falk-Reiter. Bestehende Definitionen bleiben erhalten.

Alternative getrennte Plattformrenderer wurde wegen doppelter Auswertungs- und Interaktionspfade verworfen. Neue universelle Herleitungsverfahren sind nicht Teil dieser Entscheidung. Operatoren ohne fachlichen Ablauf zeigen Definition, echte Eingaben, Ergebnis und vorhandene Bedingungen.
