# Visuelle Gruppen: Faltung und Teilgraphansichten

Status: implementiert und unabhängig geprüft, 2026-10-12.

## Ziel und Nutzerwirkung
Gruppen einklappen, mit richtigen Grenzanschlüssen verbinden und hierarchisch als gemeinsame Teilgraphansichten bearbeiten. Android und Desktop teilen Modell und Editor.

## Nicht-Ziele
Keine neuen registrierten mathematischen Typen, keine duplizierten Unterkarten, kein Release/Push.

## Untersuchter Istzustand
VisuelleKnotenGruppeDaten besitzt Rahmen und direkte KnotenIds. Die automatische Zuordnung erfolgt bisher flach bei jeder Aktion. KartenEditorZustand hält Undo/Redo und Auswertungsrevision. KnotenKartenEditor besitzt Viewport-Culling; KartenDatenJson schreibt Format 8. AuswahlZuKarte ist der Vergleichspfad für Kartenschnittstellen. Der Arbeitsbaum enthält fremde Mengenoperatoränderungen, die erhalten bleiben. Bestehender Branch: samai/v3.1.1/rechneroperatoren-konsolidierung.

## Fachliche und mathematische Semantik
Der kanonische Graph bleibt unverändert; Auswertung erfolgt auf der Hauptkarte. Faltung und Navigation ändern keine Mathematik.

## Daten-, Node-, Handle- und Edge-Vertrag
Gruppen erhalten Eltern-ID, Faltzustand und eigene Ansicht. Anschlussprojektionen sind flüchtig und bilden auf Original-AnschlussVerweise ab. Originalverbindungen behalten IDs. Gruppenhierarchie ist azyklisch; Knoten haben eine direkte Gruppe. Format 9 liest alte flache Gruppen.

## Architekturentscheidungen
Neutrale Projektionsschicht statt persistierter Ersatzknoten. Räumliche Elternbildung anhand ausgeklappter Rahmen; kompakte Geometrie verändert keine Mitgliedschaft. Teilgraphansicht schreibt über denselben Editor. ADR dokumentiert Details.

## Betroffene Dateien und Symbole
Graphdaten/VisuelleGruppen, KartenAktion, KartenEditorZustand, KartenDatenJson; gemeinsamer KnotenKartenEditor; Android VisuelleGruppenEbene und KartenListenModell/VerwaltungsFenster; Desktop Main.

## Meilensteine und konkrete Umsetzungsschritte
- [x] Hierarchie, Faltung, Aktionen und Formatmigration implementieren.
- [x] Gemeinsame Projektion mit Anschlussabbildung, Viewport und Interaktionen integrieren.
- [x] Gruppenansicht, Inspector, Ordnerhierarchie und Navigation integrieren.
- [x] Übergang, Tests, unabhängige Verifikation und Abschlussdiff.

## Tests und Validierung
Gezielte Graph-/Persistenz-/Projektions-/Historientests; vollständige Gradle-JVM-Tests, Repository/Kernprüfungen und Android-Build. Laufzeit- und Frametimingnachweise getrennt dokumentieren. Referenzgrößen 100/500/1000 Knoten, 2000 Verbindungen, drei Ebenen; Ziel 95% Frames <=16,7 ms und kein Wechselstillstand >100 ms auf dokumentierter Hardware.

## Persistenz und Migration
Format 9 mit ausgeklappten Defaults; alte Hierarchie einmalig räumlich ableiten. Elternzyklen und ungültige Referenzen ablehnen. Ansichtsobjekte nie serialisieren.

## Risiken und Rückfallstrategie
Große Gruppen können viele Ports besitzen. Projektion einmal pro Strukturrevision, nicht pro Animationsframe. Kein pauschales Versprechen über mathematische Rechenlast. Bei fehlender Hardware bleiben Frameziele unbestätigt. Fremde Änderungen nicht überschreiben.

## Fortschritt
Modell, Migration, Projektionsschicht und gemeinsamer Editor implementiert. Inspector und Gruppenbaum sind auf Android und Desktop integriert. Kopieren/Ausschneiden/Auflösen, eigene Kameras und aktive Gruppenansicht werden gespeichert. 200-ms-Übergang verwendet getrennte Ebenen und interpolierte Verbindungspunkte; Projektion und Anschlussgeometrie werden nicht pro Frame neu erstellt.

Unabhängige Prüfung fand und korrigierte: fehlende Fan-out-Ausgänge, Aufnahme in unsichtbare Kindrahmen, schrumpfende Gruppen bei Einfügen sowie einen Cachevergleich nach Listenidentität. Regressionstests decken die Korrekturen ab.

## Entscheidungsprotokoll
2026-10-12: Nutzer bestätigt gemeinsame Hauptkartenbearbeitung, freie und außen verbundene Ports, räumliche Verschachtelung und Inhaltserhalt beim Auflösen.

## Abweichungen vom ursprünglichen Plan
Gruppenansichten bleiben Projektionen statt echter Unterkarten. Die abgeleiteten Eingangs-/Ausgangsportale wählen den Originalknoten; ihre künstlichen Anschlüsse sind nicht direkt verdrahtbar. Android-Framebudget kann ohne Laufzeithardware nicht bestätigt werden.

## Ergebnis und Verifikation
Desktop-Smoke-Test mit isoliertem Datenspeicher: kompakter Gruppenblock mit Außenverbindungen, verschachtelte Baumansicht, Inspector-Faltung, einzelne Gruppenansicht und Originalknotenauswahl über Ausgangsportal erfolgreich. Keine Android-Laufzeitaussage: adb meldet kein Gerät; kein verfügbarer AVD/KVM.

JVM-Projektionsmessung (30 Wiederholungen, 100/500/1000 Knoten, drei Ebenen, bis 1997 Verbindungen): bei 1000 Knoten rund 5 bis 18 ms p95 zwischen Einzel- und parallelen Gesamttestläufen; keine belastbare Gerätebenchmark. Das misst Modellberechnung, keine Renderframes. `pruefe_kern.py` ist mangels `kotlinc` im PATH nicht ausführbar; die Kerntests laufen über Gradle.

Repository-, Releaseplan- und Versionsfolgeprüfungen erfolgreich. Vollständiger Gradle-Testlauf: 1472 Tests ohne Fehler oder Überspringen; Android assembleDebug und lintDebug erfolgreich. Unabhängige Read-only-Prüfung bestätigt alle Findings behoben; zusätzlich wurde die dp/px-Umrechnung der automatischen Gruppen-Kamera korrigiert. Bestehende fremde Änderungen bleiben erhalten; Branch und HEAD bleiben unverändert, kein Commit/Push.
