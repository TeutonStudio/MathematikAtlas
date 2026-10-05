# ExecPlan: Adaptive Flächen- und Volumenvisualisierung

## Status

- Datum: 2026-10-05
- Verantwortlicher Workflow: SamAI auf `samai/v3.0.0/alpha`
- Zustand: verifiziert

## Ziel und Nutzerwirkung

Der bestehende Visualisierungsknoten verbindet numerische Stützpunkte zu Kurven,
Flächen und Volumendarstellungen. Die eingestellte Auflösung beschreibt eine
Zieldichte im normalisierten Bildraum; ein separates Auswertungsbudget begrenzt
die adaptive Verfeinerung. Schnell durchlaufene oder stark gekrümmte Bildbereiche
erhalten mehr Stützpunkte als nahezu konstante Bereiche.

## Nicht-Ziele

- Kein neuer Knotentyp und keine Änderung mathematischer Kernobjekte.
- Keine neue Produktionsabhängigkeit und keine Release- oder Android-Version.
- Keine Änderung des getrennten Geometrievisualisierungsknotens.

## Untersuchter Istzustand

`VisualisierungsSampler` erzeugt für Methoden ausschließlich Punktlisten. Regionen
besitzen bereits beweisbewusste adaptive Zellen, der Renderer zeichnet R³ jedoch
nur als Punktwolke beziehungsweise Drahtzellen. Sampling läuft abbrechbar auf
`Dispatchers.Default`; Kameraänderungen gehören nicht zur Sampling-Signatur.

## Fachliche und mathematische Semantik

Numerische Stichproben bleiben Approximationen und werden von bewiesen enthaltenen,
gemischten und unbekannten Zellen getrennt. Undefinierte Werte und Singularitäten
werden nicht durch Flächen überbrückt. Exakte endliche Mengen und Intervalle bleiben
exakt und werden nicht interpoliert.

## Architekturentscheidungen

- Persistierte Qualitätswerte bleiben in `VisualisierungsKonfiguration`.
- Laufzeitgeometrie und Statistik liegen im Compose-freien Samplingpaket.
- Canvas-Projektion, Tiefensortierung, Schattierung und Transparenz bleiben im Renderer.
- Vorhandene Eigenschaftsschlüssel bleiben ladefähig; keine Formatmigration.

## Meilensteine

- [x] M1: Ergebnisgeometrie, adaptive Methodenabtastung und Statistiken.
- [x] M2: adaptive Regionsfüllung, R³-Randfläche und Volumenzellen.
- [x] M3: Renderer und Inspector auf Flächen-/Qualitätsvertrag umstellen.
- [x] M4: fokussierte und vollständige Verifikation, Dokumentation und Diffprüfung.

## Tests und Validierung

| Prüfung | Befehl oder Methode | erwartetes Ergebnis |
|---|---|---|
| Visualisierung | `./gradlew :MathematikKnoten:testDebugUnitTest` | adaptive Kurven, Flächen, Regionen und Budgets bestehen |
| App | `./gradlew :app:testDebugUnitTest` | Inspector/Persistenzpfade bestehen |
| Gesamt | `./gradlew test :app:assembleDebug` | JVM-Tests und Debug-APK erfolgreich |
| Repository | `python3 scripts/pruefe_repository.py` | Architektur- und Repositoryverträge erfolgreich |
| Releaseguard | `python3 scripts/pruefe_releaseplan.py` und `python3 scripts/pruefe_versionsfolge.py` | Alpha verändert keine Releaseversion |

## Risiken und Rückfallstrategie

Adaptive Netze können ihr Budget vor der Zielauflösung ausschöpfen. Solche Ergebnisse
bleiben als teilweise sichtbar und melden den Budgetstatus. Der bisherige Punktpfad
bleibt für exakte, diskrete und nicht triangulierbare Ergebnisse erhalten.

## Entscheidungsprotokoll

| Datum | Entscheidung | Alternativen | Begründung |
|---|---|---|---|
| 2026-10-05 | Geeignete Daten immer verbunden darstellen | persistierter Punkte-/Gittermodus | Nutzer wünscht automatische Flächendarstellung |
| 2026-10-05 | Zieldichte plus separates Budget | exakte Punktzahl oder Qualitätsstufen | adaptive Verteilung benötigt eine harte, unabhängige Laufzeitgrenze |
| 2026-10-05 | R³-Randfläche plus schwaches Volumen | nur Rand oder nur Volumenzellen | bestätigte gewünschte Kombination |

## Abnahmekriterien

- [x] Geeignete Methodenflächen sind keine reine Punktwolke mehr.
- [x] Hohe lokale Bildänderung erzeugt mehr Unterteilungen als langsame Änderung.
- [x] R²-Regionen sind gefüllt; R³-Mengen zeigen Randfläche und Volumen.
- [x] Unbekannte Bereiche und Singularitäten werden nicht fälschlich gefüllt.
- [x] Auswertungsbudget und Orbit-Schritte sind getrennt einstellbar und sichtbar.
- [x] Kameraänderungen lösen kein Resampling aus.

## Fortschritt

- 2026-10-05: Istzustand, vorhandene Qualitätsfelder und Rendererpfad geprüft.
- 2026-10-05: Adaptive Kurven und Methodenflächen, Rasterkonturen, R³-Isoflächen,
  numerische Flächen-/Volumenzellen, Tiefensortierung und Statistik umgesetzt.
- 2026-10-05: MathematikKnoten-, App- und Desktoptests, vollständiger Gradle-Testlauf,
  Debug-APK, Repository-, Releaseplan- und Versionsfolgeprüfung erfolgreich.

## Abweichungen vom Plan

- Das bestehende Gesamtbudget wird weiterhin zwischen analytischen Zellnachweisen
  und numerischem Raster geteilt. Die angezeigte Statistik zählt die numerischen
  Raster- beziehungsweise Methodenauswertungen; analytische Zertifikatschritte
  bleiben in ihren vorhandenen Budgets und Diagnosen sichtbar.

## Ergebnis und Verifikation

Die fokussierten Visualisierungstests und insgesamt 521 MathematikKnoten-Tests
bestanden. `:app:testDebugUnitTest`, Desktoptests, `./gradlew test
:app:assembleDebug`, Repositoryprüfung, Releaseplanprüfung und Versionsfolgeprüfung
bestanden. `scripts/pruefe_kern.py` war nicht ausführbar, weil in der Umgebung kein
`kotlinc` installiert ist; der Rechenkern wurde im vollständigen Gradle-Testlauf
erfolgreich kompiliert und getestet. Eine Emulator-/Geräteprüfung wurde mangels
verbundenem Gerät nicht ausgeführt.
