# Interaktionen, Leistung und visuelle Gruppen stabilisieren

## Status

Abgeschlossen am 2026-10-04. Die Umsetzung erfolgte auf dem vorhandenen Arbeitsstand; fremde Änderungen unter `.idea/` und `docs/codex/plans/completed/` blieben unberührt.

## Ziel und Nutzerwirkung

Visualisierungsgesten sollen kontinuierlich reagieren, Kabelenden exakt dem Zeiger folgen und beim Abziehen bestehender Verbindungen keinen Knotendialog öffnen. Karten-Pan und -Zoom dürfen keine mathematische Neuauswertung pro Pointer-Ereignis auslösen. Visuelle Gruppen ordnen Knoten nach Abschluss einer Geste automatisch anhand mehrheitlicher Flächenüberdeckung zu und bleiben bei Zoom optisch stabil.

## Nicht-Ziele

- Keine neuen Knotentypen oder mathematischen Operationen.
- Keine Änderung des Kartenformat-Schemas oder der Anschlussverträge.
- Kein beiläufiges Refactoring außerhalb der betroffenen Interaktionspfade.

## Untersuchter Istzustand

- Beide Visualisierungsrenderer verwenden den veränderlichen Kamerazustand als `pointerInput`-Schlüssel; dadurch wird die laufende Pointer-Coroutine nach einem Update abgebrochen.
- `MathematikAtlasApp` wertet bei jeder Änderung von `editor.karte`, einschließlich `AnsichtÄndern`, den gesamten Graphen aus.
- Ein auf Hintergrund beendeter Anschlussdrag öffnet unabhängig von einer Neuverdrahtung den Knotendialog.
- Gruppenbereinigung entfernt ausgetretene Knoten, nimmt neue Knoten jedoch nur über eine manuelle Aktion auf.

## Fachliche Semantik und Verträge

- Der Visualisierer besitzt einen persistierten Dimensionsmodus `Automatisch` oder `Manuell`; die effektive Dimension wird nur im Automatikmodus aus der Eingabemenge abgeleitet.
- Eine Gruppe enthält einen Knoten, wenn mehr als 50 Prozent seiner Fläche im Inhaltsbereich unterhalb der Gruppenkopfzeile liegen. Bei mehreren Gruppen gewinnt der größte Anteil; bei Gleichstand bleibt die bisherige Mitgliedschaft, sonst entscheidet die stabile Gruppenreihenfolge.
- Der Abschluss eines Kabeldrags unterscheidet neue Ablage auf Hintergrund, gelöste bestehende Verbindung und sonstigen Abschluss.

## Architekturentscheidungen

- Kameraänderungen bleiben während der Geste lokaler UI-Zustand und werden am Ende einmal persistiert.
- Der Editor führt eine auswertungsrelevante Revision, die Layout-, Gruppen- und Viewportaktionen nicht verändern.
- Gruppenmitgliedschaften werden während einer Editorinteraktion aufgeschoben und in `beendeInteraktion` vor dem gemeinsamen Undo-Snapshot aktualisiert.

## Betroffene Bereiche

- `MathematikKnoten`: Visualisierungskonfiguration, Dimensionsableitung und beide Renderer.
- `KnotenKartenVerwalter`: Editorzustand, Anschlussdrag und Gruppenmitgliedschaft.
- `app`: Inspector, Auswertungs-/Autosave-Effekte und Gruppenrendering.

## Meilensteine

- [x] Dimensionsmodus, Ableitung und stabile Visualisierungsgesten.
- [x] Schneller Viewportpfad und Auswertungsrevision.
- [x] Typisierter Verbindungsabschluss und exakte Pointerkoordinaten.
- [x] Automatische Gruppenmitgliedschaft und zoomstabile Darstellung.
- [x] Tests, vollständige Prüfungen und Abschlussdiff.

## Umsetzungsschritte

1. Konfiguration rückwärtskompatibel erweitern und eine reine, getestete Dimensionsableitung ergänzen.
2. Kamera-Gesten mit stabiler Pointer-Lebensdauer und einmaligem Commit implementieren.
3. Editoraktionen klassifizieren und App-Effekte für Auswertung und Autospeichern trennen.
4. Verbindungsabschluss typisieren und Root-zu-Welt-Koordinaten verwenden.
5. Gruppenüberdeckung zentral berechnen und Zuordnung am Interaktionsende anwenden.
6. Gruppenbedienelemente in konstanter Bildschirmgröße zeichnen.

## Tests und Validierung

- Gezielte JVM-Tests für Konfiguration, Dimension, Gruppen, Verbindung und Revision.
- Repository-, Releaseplan-, Versionsfolge- und Kernprüfung.
- Vollständige Gradle-Tests und `:app:assembleDebug`.
- Geräteprüfung bleibt separat auszuweisen, falls kein Android-Gerät verfügbar ist.

## Persistenz und Migration

Keine Formatversionserhöhung. Fehlt der Dimensionsmodus, gilt eine vorhandene explizite `dimension` als manuell; vollständig fehlende Visualisierungseigenschaften verwenden den neuen automatischen Standard.

## Risiken und Rückfallstrategie

- Uneindeutige Mengen dürfen nicht stillschweigend projiziert werden; sie verwenden den gespeicherten Fallback und bestehende Diagnosen.
- Gestenänderungen werden durch reine Transformations-/Koordinatenfunktionen getestet und zusätzlich als Geräte-Smoke-Test ausgewiesen.
- Bei Leistungsregression kann die Revisionskopplung unabhängig von den fachlichen Änderungen zurückgenommen werden.

## Fortschritt

- [x] Istzustand und vorhandene Tests untersucht.
- [x] Produktionsänderungen umgesetzt.
- [x] Gezielte und vollständige JVM-Tests bestanden.
- [x] Repository-, Releaseplan- und Versionsfolgeprüfungen bestanden.
- [x] Debug-APK erfolgreich gebaut.

## Entscheidungsprotokoll

- 2026-10-04: Auto-Dimension mit manuellem Override; Legacy-Dimensionen bleiben manuell.
- 2026-10-04: Gruppenmitgliedschaft ab strikt mehr als 50 Prozent Überdeckung.
- 2026-10-04: Zuordnung am Gestenende; größte Überdeckung gewinnt.
- 2026-10-04: Knotendialog nur bei einem neu begonnenen Kabeldrag.

## Abweichungen vom ursprünglichen Plan

Die eigenständige Kernprüfung war nicht ausführbar, da `kotlinc` und `java` für das Skript nicht im PATH lagen. Gradle kompilierte und testete den Rechenkern im vollständigen Testlauf erfolgreich. Ein Geräte-Smoke-Test war mangels installiertem `adb` beziehungsweise verbundenem Android-Gerät nicht möglich.

## Ergebnis und Verifikation

Der Dimensionsmodus ist rückwärtskompatibel persistiert und leitet sichere skalare, komplexe und zwei- beziehungsweise dreidimensionale Eingaben automatisch ab. Beide Visualisierer halten die Pointersequenz über beliebig viele Bewegungsereignisse stabil und schreiben ihre Kamera erst am Gestenende zurück.

Ansichts-, Layout- und Gruppenänderungen erhöhen die Auswertungsrevision nicht. Ansichtsgesten verwenden einen schnellen Editorpfad ohne Bereinigung oder Undo-Eintrag pro Frame; Autospeichern bleibt separat debounced. Verbindungsdrags verwenden Root- und Weltkoordinaten und unterscheiden neue Hintergrundablage von gelösten bestehenden Verbindungen. Gruppenmitgliedschaften werden anhand strikt mehr als 50 Prozent Inhaltsüberdeckung atomar am Interaktionsende aktualisiert.

Verifikation: gezielte Modultests, `./gradlew test`, `python3 scripts/pruefe_repository.py`, `python3 scripts/pruefe_releaseplan.py`, `python3 scripts/pruefe_versionsfolge.py` und `./gradlew :app:assembleDebug` erfolgreich; `git diff --check` ohne Befund.
