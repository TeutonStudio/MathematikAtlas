# ExecPlan: UI-Integration Methoden, Tupel und Formeln

## Status
2026-10-13, begonnen auf bestehendem uncommittetem Vertrags-/Zeugnisarbeitsstand. Basisdiff /tmp/atlas-ui-basis/vorher.patch. Kein neuer Knotentyp, Release oder Commit.

## Ziel und Nutzerwirkung
Gemeinsame Android-/Desktop-Bedienwege für gebundene Formeln, Methodensignaturen, strukturierte Anschlüsse, Kartenkonvertierung, Rückführung und selbsttragenden Austausch. Referenz Q(f,x,h) mit h≠0.

## Nicht-Ziele
Neue mathematische Algorithmen, Abhängigkeiten, Veröffentlichung. Fehlende Fähigkeiten werden diagnostiziert; keine Ersatzwerte.

## Untersuchter Istzustand
Gemeinsame Formelbauer übernehmen AST/LaTeX ohne GebundeneFormel. Bindungs-/Kontextcodec und kanonische Anwendung existieren im Kern. Android und Desktop konfigurieren Formeln teils durch erneutes Parsing. Zeugnisse werden angezeigt, Rückführung hat noch keinen Knotenmodus. Bestehende Kartenaktionen/Verbindungskonfliktpfade bleiben zuständig.

## Fachliche Semantik
Stabile Quellen- und Komponentenidentitäten; Anzeigenamen und Anschlussdarstellung sind Projektionen. Strukturierte Argumente bleiben eigenständig. Bedingungen wirken bei Auswertung und Methodenbildung. Unbekanntheit ist kein Nachweis.

## Daten-, Node-, Handle- und Edge-Vertrag
GebundeneFormel plus Zeichnung als gemeinsamer Entwurf. Versionierter Kontext in Knoteneigenschaften, bestehende AST/LaTeX-Felder als kompatible Projektionen. ID-basierte Anschlusszuordnung. Rückführung erweitert den vorhandenen Übersetzer; keine neue Art.

## Architekturentscheidungen
Mathematik/Entwurfsvalidierung im Kern; Konfiguration/Kartenkonvertierung in MathematikKnoten; gemeinsame Compose-Inhalte dort; Plattformen koordinieren Kartenaktionen. Keine neue Shadowmodule-Ausnahme.

## Betroffene Dateien und Symbole
FormelBindungen/FormelKontextCodec, gemeinsame Formelbauer, ZahlenRechnerErweiterungen/StrukturFormelRechner, ÜbersetzerKnoten/Inspector, Ergebnisdarstellung, Android-/Desktop-Aufrufer und Visualisierung.

## Meilensteine
- [x] Kontext, Validierung und Persistenz
- [x] Gemeinsamer Formelbauer und Methodendeklarationen
- [x] Kartenkonvertierung und Methodenschnittstellen
- [x] Rückführung, Ergebnisse, Austausch und Visualisierung
- [x] Regression und unabhängige statische Prüfung
- [ ] Android-Laufzeitabnahme auf Gerät/Emulator

## Umsetzungsschritte
1. Gebundenen Entwurf und zentrale Konfiguration mit stabilen Anschlüssen ergänzen.
2. Symbol-/Typ-/Signatur-/Voraussetzungseditor integrieren, Import kontextabhängig ausführen.
3. Android/Desktop-Aufrufer auf vollständige Übernahme umstellen; Graph- und Methodenpfade verbinden.
4. Rückführung im vorhandenen Übersetzer und vollständige Ergebnis-/Austauschwege integrieren.
5. Referenzfälle, Lebenszyklus, Compose, Build/Lint und unabhängige Prüfung ausführen.

## Tests und Validierung
Q, nullstellige/mehrwertige/strukturierte Methoden, gleiche Namen/verschiedene IDs, Typfehler, Voraussetzungen, AST/Graph/Karte/Codec, Rückführung, Portwechsel, Undo/Redo, Export. Desktop-Compose und Android-Gerätetest; tatsächliche Laufzeitbelege gesondert.

## Persistenz und Migration
Optionaler versionierter Formelkontext; alte ASTs mit nachweisbaren Quellen übernehmen, Bereiche unbekannt lassen. Beschädigte/unbekannte Rohdaten bleiben erhalten. Keine automatische überschreibende Reparatur.

## Risiken und Rückfallstrategie
Vorbestehende Änderungen bewahren. Kein Verlust durch LaTeX-Neuparsing. Kompatible APIs bleiben Adapter; nicht unterstützte Graphen erhalten Diagnose vor Übernahme.

## Fortschritt
Gemeinsamer Kontexteditor, Typ-/Signatur-/Bereichs-/Voraussetzungseditor, AST-Paletten und selbsttragende Zwischenablagepakete integriert. Android-Inspector/Toolbar und Desktop-Aufrufer übernehmen vollständigen Kontext. Gebundene Rechner verwenden strukturelle Anwendung; Graphentfaltung/-zusammenfassung und Methodenbildung verwenden vorhandene Knotentypen und Kartenaktionen. Getrennte und kombinierte Projektionen sowie partielle Bindung sind bedienbar. Rückführung, Zeugnisdetails/Herkunft und Visualisierungsprojektionen sind gemeinsam verfügbar.

Neue Regressionen decken Q mit f(t)=t², x=3,h=2→8 und h=0-Ablehnung über Graph/Karte/Methoden/Codec ab; identische Anzeigenamen, kollidierende generierte Namen, Umbenennen, Copy/Laden, freie Eingangsquellen und deren Annahmen, beschädigte Kontexte, Herkunftssynchronisation, Rückführung leerer Untertupel, explizite Blattprojektion und Undo/Redo. Partielle Bindung prüft Komponentenidentität trotz geänderter Anschlussreihenfolge. Desktop-Compose prüft ungültigen Import, unveränderten Abbruch und Sperre bei noch nicht übernommenem Symbolnamen. Äquivalente Android-Gerätetests sind ergänzt.

Unabhängige statische Prüfung durch node_verifier: Import-IDs, gleichnamige Quellen, verlorene Eingangsannahmen und Herkunft, Portnamenskollisionen sowie Umbenennen angeschlossener Quellen korrigiert und nachgeprüft. Keine weiteren belegten Blocker in der gezielten Abschlussprüfung.

## Entscheidungsprotokoll
Nutzer bestätigt Gesamtvertrag auf beiden Plattformen, notwendige Kernbrücken ohne neue Algorithmen und Rückführungsmodus im bestehenden Übersetzer. Gemeinsamer Arbeitsdialog mit atomarem Übernehmen, Abbrechen ohne Modelländerung; Vergleich FormelEditorDialogRahmen und bestehendes Auswertungsschema.

## Abweichungen
Android-Gerät/AVD erneut außerhalb der Sandbox geprüft: `adb devices` leer, `emulator -list-avds` leer. Android-Laufzeitabnahme bleibt offen. Keine neue Produktionsabhängigkeit und kein neuer Knotentyp.

Bestehende mathematische Fähigkeiten bleiben die Grenze: quantifizierte Bereichsauswertung, nicht unterstützte Graphknoten und mehrdeutige historische Bindungen liefern Diagnosen. Q bleibt strukturell exakt und unter h≠0 auswertbar; eine automatische allgemeine symbolische Kürzung zur Anzeige 2x+h ist keine vorhandene Capability und wird nicht im UI erfunden. Strukturpakete werden über Zwischenablage ausgetauscht; Dateiimport/-export verwendet den vorhandenen vollständigen Kartenweg.

## Ergebnis und Verifikation
Offen. Abschluss erfordert node_verifier und dokumentierte Abnahmegrenzen.
