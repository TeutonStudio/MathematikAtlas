# Inspectorüberarbeitung

## Titel und Status
16.10.2026, umgesetzt und unabhängig quellgeprüft. Android-Geräteabnahme offen
(kein Gerät/AVD); zusätzliche Modul-Lintgrenze siehe Ergebnis.

## Ziel und Nutzerwirkung
Vier unabhängig klappbare Bereiche: Standarddaten, Knotendaten, Eingangsdaten,
Ausgangsdaten. Leere Bereiche entfallen. Richtungsweite Optionen stehen in der
ersten Anschlusszelle, individuelle Optionen und Daten beim tatsächlichen Port.
Hintergrund und Rahmen sind separat färbbar; Notizen erhalten fünf Grundformen.

## Nicht-Ziele
Keine neuen Knotentypen, Abhängigkeiten, Veröffentlichung, Version oder Commits.
Keine Sammelbearbeitung von Anschlüssen. Mathematische Semantik bleibt erhalten.

## Untersuchter Istzustand
Android: KnotenInspektorFenster und KnotenInspektorRegister; Desktop: Main.kt.
Gemeinsame Fach-Composables in MathematikKnoten, gemeinsame Editorhülle im
KnotenKartenVerwalter. Aktuell Rechteck und Adapterkapsel, keine Körperfarboverrides.
Viele vorhandene lokale Änderungen einschließlich Join und Diagnoseanhängen.
Baseline unter /tmp/atlas-inspektor-baseline, einschließlich Ausgangsstatus/HEAD.

## Fachliche und mathematische Semantik
Keine neue Mathematik. Standardwerte bleiben inaktiv bei verbundener Quelle;
Mehrfachbeiträge werden vollständig und geordnet angezeigt. Übersetzungsziel
gehört zu gemeinsamen Ausgangseinstellungen, Operator zu Knotendaten.

## Daten-, Node-, Handle- und Edge-Vertrag
Stabile IDs und bisherige Anschlussnamen erhalten. Farben als optionale typisierte
Eigenschaften, zusätzliche Werte der bestehenden Formeigenschaft. Klappzustand
nur Sitzung; keine Formatänderung. Fremde/fehlende Formwerte ergeben Rechteck.

## Architekturentscheidungen
Gemeinsame neutrale Abschnitts-/Zellenbausteine und Darstellungsgeometrie.
Fachliche Zuordnung und Validierung oberhalb des Editors. Keine neuen Shadowquellen.

## Betroffene Dateien und Symbole
KnotenInspektorFenster, KnotenInspektoren und spezialisierte Inspectoren;
DesktopInspektor; gemeinsame Fach-Einstellungsfunktionen; KnotenGeometrie,
KnotenKartenEditor; .agents/skills/inspektor-aendern/SKILL.md.

## Meilensteine
- [x] Bestand, Aufrufpfade, Vergleichsinspector und Prüfkonventionen untersucht.
- [x] Gemeinsame Inspectorbausteine, vollständige Anschlusswerte und Darstellung.
- [x] Android- und Desktopintegration; gemeinsame Fachoptionen pro Richtung,
  Standardwerte/Methodenprojektionen/Kartenfallbacks am zugehörigen Anschluss.
- [x] JVM-/Desktoptests, APKs, App-/Editor-Lint, unabhängige Nachprüfung und
  Dokumentation. Android-Geräteabnahme mangels Gerät/AVD konkret offen.

## Konkrete Umsetzungsschritte
Gemeinsame Hülle mit stabilem Sitzungsschlüssel; validierte Standarddaten;
Anschlussdaten aus Graph und Auswertung; bestehende Fach-Editoren passend aufteilen.
Formen mit gemeinsamem Pfad für Kontur, Treffer und Inhalt; Farben in Editorhülle.
Fachänderungen über vorhandene Konfigurationspläne/Verlustbestätigung übernehmen.

## Tests und Validierung
Gezielte Geometrie-, Persistenz-, Inspector- und Konfigurationsregressionen;
Compose-Bedienung auf Desktop und Android soweit verfügbar. Gesamttests, Lint,
Debug-/Test-APK, Desktopkompilierung und Repository-/Release-/Shadowguards.
Skill mit quick_validate.py prüfen. Tatsächliche Ergebnisse unten dokumentieren.

## Persistenz und Migration
Bestehende Eigenschaftsserialization verwenden; alte Karten ohne Overrides bleiben
optisch unverändert. Farben/Formen bei Roundtrip, Kopieren und Undo/Redo prüfen.

## Risiken und Rückfallstrategie
Umfangreiche bestehende lokale Änderungen nicht überschreiben. Neue Formen dürfen
Text nicht abschneiden oder Auswahl-/Anschlussgeometrie auseinanderlaufen lassen.
Bei Fehlern nur eigenen Diff gegen gesicherte Baseline korrigieren.

## Fortschritt
18 neue JVM-/Desktopregressionen bestanden: fünf Darstellungs-/Persistenztests,
sieben Anschluss-/Konfigurationstests, sechs native Compose-Bedientests. Dazu
zwei neue Android-Regressionen für den produktiven Inspector gebaut. Bestehende
direkte Inspector-Aufrufer und Vorschauen bleiben kompatibel.

## Entscheidungsprotokoll
16.10.: Benutzer bestätigt beide Plattformen, Daten und Optionen je Anschluss,
gemeinsame Optionen je Richtung, zwei Farben und zusätzliche Grundformen.
Alle Bereiche initial offen, Zellen zu; Zustand pro Knoten in der Sitzung.
Leere Bereiche ausblenden; bestehenden Skill anwenden und aktualisieren.

## Abweichungen vom ursprünglichen Plan
Die unabhängige Prüfung fand zunächst einen nicht atomaren Relations-/Namenswechsel
und Clipping in Notizkapseln. Beide wurden korrigiert und durch Regressionen
abgesichert. Rechtsklicks werden ausdrücklich am sekundären Press-Ereignis mit
aktueller Formprüfung erkannt; Formwechsel ohne ID-/Größenwechsel ist getestet.
Desktopnotizen sind wie Androidnotizen von mathematischer Auswertung ausgenommen.
Bei geringer IME-Resthöhe entfallen äußere Inspectorabstände; MainActivity zieht
den Tastaturbereich bereits ab. Notizform, Körperfarben und Schriftfarbe bleiben
unabhängig. Kein neuer Knotentyp oder Formatwechsel.

## Ergebnis und Verifikation
- Erfolgreicher Gesamtlauf: `./gradlew test :app:lintDebug
  :KnotenKartenVerwalter:lintDebug :app:assembleDebug
  :app:assembleDebugAndroidTest :KnotenKartenVerwalter:assembleDebugAndroidTest
  :desktopApp:compileKotlin --quiet` (Exit 0). XML: 2.045 bestanden, 0 Fehler,
  1 bestehende Opt-in-Samplingmessung übersprungen. App-/Editor-Lint ohne Fehler;
  bestehende Warnungen sind keine neue Freigabe aller Altprobleme.
- Repository-/Architektur-/Shadowmodulprüfung, Releaseplan, Versionsfolge,
  Skillvalidierung und Diffprüfung erfolgreich.
- Zusätzlicher `:MathematikKnoten:lintDebug` deckte neben behobenen Einrückungen
  einen bereits in der gesicherten Baseline vorhandenen API-Verstoß auf:
  `VisualisierungsSampler.kt:1850`, `BigInteger.TWO` benötigt API 33 bei minSdk 26.
  Der Visualisierer wurde in dieser Aufgabe nicht verändert. Dieser Altbefund
  ist keine Inspectorregression und wird ausdrücklich nicht als grüner Lintlauf
  ausgewiesen.
- Standalone-Kernprüfung nicht ausführbar, da `kotlinc` fehlt. Die 585
  Gradle-Kerntests bestanden. ADB meldet keine Geräte; keine AVD-Konfigurationen
  vorhanden. Android-Laufzeit-/Tastaturprüfung bleibt offen, kein Geräteerfolg behauptet.
- Native Desktopabnahme: alle fünf Formen, Touch/Maus, Formwechsel, Rechtsklick,
  Vergrößern/Undo, Zoom, Schrift bis 2-fach, beide Themes; schmaler Inspector
  300×480 mit großer Schrift; Knotenwechsel, Anschlussumordnung, Klappzustand,
  Farben, Name und Übersetzerwechsel mit Undo/Redo. Screenshots unter
  `/tmp/atlas-inspektor-desktop.png` und `/tmp/atlas-notiz-*-desktop.png` visuell
  geprüft, auch unabhängig durch den Verifizierer.
- `node_verifier` hat den finalen Quellstand ohne offene Findings abgenommen.
  Die Android-Gerätegrenze und der bestehende zusätzliche Modul-Lintbefund sind
  dokumentierte, für diesen Inspectorumbau nicht blockierende Grenzen.
- Branch unverändert `release/v3.4.0-methoden-grundraeume-visualisierung`, HEAD
  `04925fc1352ad551622c1916fbd8f36c9ab83105`. Keine neuen Commits oder Veröffentlichung.
  Vorhandener HEAD: Autor und Committer jeweils SamAI
  `<46108494+TeutonStudio@users.noreply.github.com>`. Version 3.4.0 unverändert;
  zusätzliche Releaseguards erfolgreich, keine Releaseintegration beauftragt.
