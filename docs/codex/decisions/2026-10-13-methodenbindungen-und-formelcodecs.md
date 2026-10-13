# Bindungsidentitäten, bedingte Methodenaufrufe und Formelcodecs

Datum: 2026-10-13. Status: eingeführte Grundlage; Gesamtvertrag noch offen.

## Kontext
Positionsabhängige Komponenten-IDs und Namenssubstitution verhindern stabile Umordnung/Umbenennung. Formelgraph und Strukturcodec Version 1 verlieren Operationsbedingungen beziehungsweise rekonstruieren Literale über LaTeX.

## Entscheidung
- Methoden speichern Argument-/Ergebnisidentitäten getrennt von Reihenfolge und Anzeigenamen. Explizite `bindungsId` hat bei Substitution Vorrang; historische Parameter ohne ID behalten den Legacy-Namenspfad.
- Kanonische neue Anwendung liefert `MathematischeAnwendung(ergebnis: Tupel, bedingungen)`; widerlegte Voraussetzungen verhindern die Anwendung. Voraussetzungen verschachtelter Aufrufe werden vor Substitution aus Vorschrift und Metadaten gesammelt.
- Gebundene Formeln tragen vollständige Quellentypen, Rollen und optionale Methodensignaturen. Deklarierte Methodenaufrufe werden strukturell dargestellt.
- Strukturcodec Version 2 schreibt typisierte Literale und Operationsbedingungen. Version 1 bleibt lesbar. Der ergänzende Kontextcodec bewahrt Bindungen, Typen und Annahmen. LaTeX-Austauschpakete bestehen aus Text plus Kontext.
- Nicht registrierte Wertvarianten werden ausdrücklich abgewiesen; kein LaTeX-Fallback und keine erfundene Rekonstruktion.
- Mehrkomponentige Kartengrenzen erhalten bei Migration explizite Reihenfolgen. Neu erzeugte Indexdefinitionskarten werden bereits vor Speicherung normalisiert.
- Numerische Methodenoperatoren liefern ausdrücklich Approximationen mit Arbeitsgrenzen und heuristischen Fehlerabschätzungen, keine globale Inversions- oder Existenzzertifizierung.

## Alternativen
Reine Namensbindung, Layoutsortierung bei jeder Auswertung und LaTeX als Literalpersistenz wurden wegen nachgewiesener Bedeutungsverluste verworfen. Eine vollständige sofortige Ablösung aller Legacy-APIs wurde nicht vorgenommen: bestehende Aufrufer müssen schrittweise migriert werden.

## Konsequenzen und Grenzen
Die neuen APIs sind additive Übergangsgrundlagen. Scalar-Legacyspeicher, nicht registrierte Wertvarianten, vollständige Binder-/Operatorintegration, allgemeine Übersetzungszeugnisse und die durchgängige UI-Integration bleiben offene Arbeit. Diese Entscheidung bedeutet ausdrücklich keinen Abschluss der 77-Punkte-Abnahme. Der aktive ExecPlan enthält Fortschritt und Prüfgrenzen.
