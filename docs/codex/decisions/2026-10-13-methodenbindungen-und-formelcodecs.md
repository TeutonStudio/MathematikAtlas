# Bindungsidentitäten, bedingte Methodenaufrufe und Formelcodecs

Datum: 2026-10-13. Status: eingeführte Grundlage; Gesamtvertrag noch offen.

## Kontext
Positionsabhängige Komponenten-IDs und Namenssubstitution verhindern stabile Umordnung/Umbenennung. Formelgraph und Strukturcodec Version 1 verlieren Operationsbedingungen beziehungsweise rekonstruieren Literale über LaTeX.

## Entscheidung
- Methoden speichern Argument-/Ergebnisidentitäten getrennt von Reihenfolge und Anzeigenamen. Explizite `bindungsId` hat bei Substitution Vorrang; historische Parameter ohne ID behalten den Legacy-Namenspfad.
- Kanonische neue Anwendung liefert `MathematischeAnwendung(ergebnis: Tupel, bedingungen)`; widerlegte Voraussetzungen verhindern die Anwendung. Voraussetzungen verschachtelter Aufrufe werden vor Substitution aus Vorschrift und Metadaten gesammelt.
- Gebundene Formeln tragen vollständige Quellentypen, Rollen und optionale Methodensignaturen. Deklarierte Methodenaufrufe werden strukturell dargestellt.
- Strukturcodec Version 3 schreibt typisierte Literale, Übersetzungszeugnisse und Operationsbedingungen. Strukturformel-, Formelkontext- und Wertcodec akzeptieren ausschließlich ihre aktuelle Wire-Version; ältere Payloads werden ausdrücklich abgelehnt. Es gibt keinen LaTeX-Migrationsfallback. Der ergänzende Kontextcodec bewahrt Bindungen, Typen und Annahmen. LaTeX-Austauschpakete bestehen aus Text plus Kontext.
- Nicht registrierte Wertvarianten werden ausdrücklich abgewiesen; kein LaTeX-Fallback und keine erfundene Rekonstruktion.
- Mehrkomponentige Kartengrenzen erhalten bei Migration explizite Reihenfolgen. Neu erzeugte Indexdefinitionskarten werden bereits vor Speicherung normalisiert.
- Numerische Methodenoperatoren liefern ausdrücklich Approximationen mit Arbeitsgrenzen und heuristischen Fehlerabschätzungen, keine globale Inversions- oder Existenzzertifizierung.

## Alternativen
Reine Namensbindung, Layoutsortierung bei jeder Auswertung und LaTeX als Literalpersistenz wurden wegen nachgewiesener Bedeutungsverluste verworfen. Eine vollständige sofortige Ablösung aller Legacy-APIs wurde nicht vorgenommen: bestehende Aufrufer müssen schrittweise migriert werden.

## Konsequenzen und Grenzen
Die neuen APIs sind noch nicht vollständig systemweit integriert. Scalar-Legacyspeicher, nicht registrierte Wertvarianten, vollständige Binder-/Operatorintegration, allgemeine Übersetzungszeugnisse und die durchgängige UI-Integration bleiben offene Arbeit. Diese Entscheidung bedeutet ausdrücklich keinen Abschluss der 77-Punkte-Abnahme. Der aktive ExecPlan enthält Fortschritt und Prüfgrenzen.

## Ergänzung: deklarierte Aufrufe und höherwertige Signaturen

Datum: 2026-10-14. Status: im lokalen Arbeitsstand implementiert; systemweite Vertragsabnahme offen.

- Deklarierte Quellen lösen gleichnamige Konstanten und eingebaute Funktionsnamen auf. Ein unbekanntes `f(x)` wird als mehrdeutig diagnostiziert; Multiplikation muss explizit eingegeben werden.
- Klammern mit Kommas bilden strukturierte Tupel; leere Klammern stehen für das leere Tupel und einfache Klammern gruppieren.
- Methodensignaturkomponenten können ihre vollständige verschachtelte Methodensignatur mitsamt IDs tragen. Formelkontextcodec ist Version 2, Wertcodec Version 3. Signatur- und Mathematikcodecs lesen ausschließlich die aktuelle Wire-Version; alte Mathematikformate werden nicht migriert.
- Eine methodenwertige Ergebniskomponente bleibt ein signaturtragender Methodenwert, sodass symbolische und gruppierte Folgeaufrufe möglich sind.
- Nullstellige Bildmengen verwenden `{()}` als Gesamtdefinitionsargument. Mehrere öffentliche Ergebnisse werden bei endlicher Bildmengenbildung als ein Ergebnistupel erhalten. Ungeklärte Voraussetzungen verhindern eine voreilige endliche Materialisierung.
- Ein effektiver Gesamtraum ersetzt wie bisher die Komponentenprüfung für die Zugehörigkeit und darf diese Bereiche erweitern. Komponentenbereiche bleiben als Signaturmetadaten gespeichert.

Abdeckung: gezielte und vollständige Gradle-Tests bestehen. Der verbleibende Legacy- und UI-Aufruferbestand ist noch nicht vollständig migriert; diese Ergänzung ist keine Gesamtfreigabe des übergeordneten Vertrags.
