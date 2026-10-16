# Angefügte Zeit- und Meldungsfelder

## Status und Ziel

Umgesetzt und unabhängig verifiziert. Zeit unten und vollständige Meldungen oben hängen außen am Knoten.
Auswahl, Größenänderung, Anschlüsse und Trefferflächen verwenden nur den Körper.
Android und Desktop teilen die Implementierung.

## Nicht-Ziele

Keine mathematischen Änderungen, neuen Knotentypen, Migrationen, Versionierung oder
Veröffentlichung. Inspector- und Dialogmeldungen bleiben erhalten. Persönliche
IDE-Dateien und vorhandene unversionierte Dateien gehören nicht zur Aufgabe.

## Untersuchter Istzustand

Ausgangscommit: 04925fc1352ad551622c1916fbd8f36c9ab83105, Branch release/v3.4.0-methoden-grundraeume-visualisierung.
KnotenKartenEditor reserviert die Zeitzeile innerhalb der Größe; die Karte zeichnet
einen verkleinerten Körper, der Größenänderungsgriff folgt der Gesamtgröße.
KnotenGeometrie zieht die Zeilenhöhe nur bei Adaptern ab. Fehlertexte liegen im
MathematikKnotenRenderer und spezialisierten Viewports. mitAuswertungszeit ergänzt
die gemeinsame Fußzeile. InhaltsGeometrie erweitert Gruppen um Inhaltsminima.
RenderAuswahl berücksichtigt bisher nur Knotenrechtecke.

## Fachliche Semantik und Verträge

Mathematische Ergebnisse und Anschluss-IDs bleiben unverändert. Gespeicherte Größen
bezeichnen künftig den Körper; außen angefügte Flächen werden nicht gespeichert.
KnotenRendererAktionen erhält meldungenMelden mit einer leeren Standardimplementierung.
Meldungen enthalten Text und Schweregrad. Leere Listen entfernen das Meldungsfeld.
Fehler stehen vor Warnungen und Hinweisen; identische Texte erscheinen einmal.
Zeit- und Meldungsfelder besitzen keine eigene Knoteninteraktion.

## Architekturentscheidung

Fachneutrale Meldungen und obere/untere Anhangmaße sind flüchtiger Editorzustand.
Die mathematische Rendererhülle ergänzt Auswertungsdiagnosen und übernimmt lokale
Renderdiagnosen. Alle Anhänge verwenden den Transformationslayer des Körpers,
bleiben aber außerhalb seiner Auswahl- und Anschlussgeometrie. Gruppen und Culling
verwenden die sichtbare Umhüllung. Bestehende öffentliche Signaturen bleiben
soweit möglich kompatibel. Keine neue Produktionsabhängigkeit.

## Betroffene Bereiche und Schritte

1. Editor: Rendereraktionen, KnotenGeometrie, InhaltsGeometrie, KartenEditorZustand,
   KnotenKartenEditor und gemeinsame Anhangflächen.
2. Mathematik: mitAuswertungszeit, Standardrenderer, geometrische und numerische
   Visualisierer melden Diagnose statt Text im Körper.
3. Tests: Geometrie/Gruppen/Culling, Diagnosezusammenführung und Desktop-Compose.
4. Dokumentation: Renderer-Vertrag und belegte Prüfungen nachführen.

## Meilensteine und Fortschritt

- [x] Aufrufpfade, ähnliche Zeitfußzeile, Modulgrenzen und vorhandene Tests untersucht.
- [x] Körpergeometrie, Anhänge und Meldungsvertrag implementiert; Auswahl/Griffe und Körpermaße in Compose geprüft.
- [x] Standardrenderer und spezialisierte Renderer melden Diagnosen oberhalb des Körpers; doppelte Texte entfernt.
- [x] Vollständige Tests, Android-Build und Desktop-Bedientests erfolgreich; Android-Gerätegrenze unten dokumentiert.
- [x] node_verifier hat den finalen Produktionsdiff ohne offene Findings abgenommen; git diff --check erfolgreich.

## Tests und Validierung

Gezielte Android-Modultests, Desktop-Compose-Regressionen für Zeit ein/aus,
Meldungen ein/mehrere/behoben, Schrift 1x/2x, Zoom 0,5x/2x, Themes und Adapter.
Körpergröße, Griffposition, Anschlusspositionen, Gruppen und Culling separat prüfen.
Repository-/Architektur-/Desktop-Shadowmodulprüfung, anschließend Android-Build.
Geräte-/Emulatorstatus ermitteln; Build und Laufzeitabnahme getrennt dokumentieren.

## Persistenz und Migration

Keine Formatänderung und keine Migration. Meldungen und Anhangmaße gehören nicht
in Karten-JSON oder Undo-Schritte. Vorhandene Benutzermaße/Positionen bleiben
erhalten; automatische Gruppenumhüllung berücksichtigt sichtbare Anhänge.

## Risiken und Rückfallstrategie

Außerhalb liegende Flächen dürfen weder vom Körper abgeschnitten werden noch
Anschlussgriffe verdecken. Lange Diagnosen müssen bei Culling sichtbar bleiben.
Änderung bei einem Fehler lokal korrigieren; keine fremden Änderungen zurücksetzen.

## Entscheidungsprotokoll

- 2026-10-16: Nutzer bestätigt vollständige mehrzeilige Meldungen statt Kürzung.
- 2026-10-16: Kein neuer Branch oder Commit für die beauftragte lokale Änderung;
  vorhandener Releasebranch bleibt ausgecheckt, Versionsverwaltung ist außerhalb
  des ausdrücklich bestätigten Umfangs.

## Abweichungen

- Der vorhandene GeometryNodes-Fehlertest verwendet jetzt den produktiven Editor
  statt nur Renderer.Inhalt, da dieser die externe Meldungsfläche zeichnet.
- Der neue Desktop-Test nutzt ein explizites 1500×1400-Fenster für die Matrix;
  der Standardtestviewport ist 1024×768. Es werden 16 Form-/Schrift-/Zoom-/Theme-
  Kombinationen jeweils ohne, mit und nach Entfernen der Diagnosen geprüft.
- Keine Hostvergrößerung erforderlich: Compose zeichnet und prüft Kinder des
  ungeclippten Hosts auch außerhalb seiner Maße. Tatsächliche Griffrechtecke
  ersetzen den pauschalen Hintergrund-Trefferpuffer.
- Ein isolierter Secondary-Maustest traf eine vorbestehende Desktop-Einschränkung:
  Compose 1.11.1 awaitFirstDown reagiert dort nur auf die primäre Maustaste,
  der bestehende sekundärKlick-Pfad verwendet diese Funktion. Keine Reparatur
  dieses unabhängigen Eingabepfads im Rahmen dieser Aufgabe; Primary-Drags sind geprüft.

## Ergebnis und Verifikation

- `./gradlew test :app:assembleDebug :app:assembleDebugAndroidTest --offline`:
  erfolgreich, 179 Tasks (25 ausgeführt, 154 aktuell). XML-Ergebnisse:
  1.985 Tests, 0 Fehler, 1 vorhandene Opt-in-Messprüfung übersprungen;
  somit 1.984 bestandene Tests. Davon 52 Desktop-Tests.
- Desktop-Compose: vollständige Meldungen, Priorität/Deduplizierung, Entfernen,
  stabile Körper-/Griffmaße, Zoom, große Schrift, Themes, Adapter, lokale
  Renderdiagnosen, äußere Anschluss-/Resize-Mausgriffe, Undo/Redo sowie Pan mit
  90-zeiliger Kopfzeile bei außerhalb des Renderfensters liegendem Körper geprüft.
- Visuelle Prüfung der Screenshots `/tmp/knoten-anhaenge-rechteck.png` und
  `/tmp/knoten-anhaenge-adapter.png`: Anhänge schließen direkt an, ausschließlich
  Körper ausgewählt, Vergrößerungsgriff am Körper. Screenshots sind temporäre
  Prüfartefakte und gehören nicht zum Repository.
- `python3 scripts/pruefe_repository.py`, `pruefe_architektur.py`,
  `pruefe_desktop_shadowmodule.py`, `pruefe_releaseplan.py` und
  `pruefe_versionsfolge.py`: erfolgreich. Versionen unverändert.
- `python3 scripts/pruefe_kern.py`: zusätzliche Standalone-Prüfung nicht
  ausführbar, da `kotlinc` fehlt. Die 585 Gradle-Kerntests bestehen.
- Android-Test-APK einschließlich KnotenAnhaengeGeraeteTest erfolgreich kompiliert.
  Das angeschlossene SM-X920 (R52Y3042QNJ) zeigt laut `dumpsys window policy`
  `showing=true`, `SCREEN_STATE_OFF`, `INTERACTIVE_STATE_SLEEP`. Daher keine
  Android-Bedienabnahme möglich und keine Installation über die bestehende App.
  Nicht blockierende offene Geräteprüfung; gemeinsame UI-Pfade auf Desktop geprüft.
- Unabhängige statische Abnahme durch node_verifier; keine offenen Findings.
- Abschlussdiff geprüft, keine Debugausgaben, keine unbeabsichtigten Produktänderungen.
  Vorbestehende `.idea`-Änderungen und `scripts/__pycache__` unverändert belassen.
- Lokale Änderungen bleiben uncommitted auf dem vorhandenen Releasebranch.
  Kein neuer Commit, keine Autor-/Committermetadaten erzeugt, kein Connector-Commit,
  kein Push, kein Release und keine Versionsänderung.
