# Zentraler Koordinatenadapter und Darstellbarkeitsdiagnosen

## Status

Umsetzung für `v2.28.1` abgeschlossen. Die Veröffentlichung erfolgt über
`release/v2.28.1-koordinatenadapter`; vollständige Gradle- und Android-Prüfungen bleiben vor dem
Squash-Merge der GitHub-CI vorbehalten.

## Ziel und Nutzerwirkung

Issue #180 wird vollständig umgesetzt. Kartesische Tupel, Zeilenvektoren, Spaltenvektoren und Koordinatenbilder verwenden denselben geprüften Weg in die reellen Räume R¹, R² und R³. Der Visualisierer verwirft nicht-reelle, symbolisch offene oder überdimensionierte Objekte nicht mehr pauschal, sondern unterscheidet darstellbar, bedingt darstellbar, nicht darstellbar und Projektion erforderlich.

## Nicht-Ziele

- kein komplexer Achsenmodus,
- kein Projektionseditor,
- kein R⁴-Renderer,
- keine automatische Realteil-, Norm- oder Dimensionsprojektion,
- kein neuer Knotentyp und keine Änderung persistierter Knotenverträge.

## Untersuchter Istzustand

- `VisualisierungsSampler` besitzt eine private zweite Koordinatenextraktion für Zahl, Tupel und Vektoren.
- Der Entwurf aus PR #346 führt `KoordinatenAdapter` und isolierte Tests ein, basiert aber auf einem vor dem finalen `v2.28.0` liegenden Branch.
- `VisualisierungsErgebnis` modelliert Qualität bereits als exakt, angenähert, teilweise, mathematisch leer oder ohne Fenstertreffer.
- Fensterbegrenzte Prädikatsmengen werden bereits ausdrücklich konfiguriert und mit einem Hinweis versehen.
- Der Rechenkern besitzt mit `DomaenenAuswerter` und `alsReelleKoordinate()` strukturierte Diagnosen für komplexe Zahlen und Quaternionen.
- `KoordinatenBild` trägt sein `GeometrischesKoordinatensystem`; der allgemeine Mengensampler erkennt diesen Typ bislang nicht.

## Fachliche und mathematische Semantik

Eine Komponente darf nur dann als reelle Koordinate erscheinen, wenn der domänenerhaltende Auswerter einen reellen Wert liefert. Reell eingebettete komplexe Werte dürfen als reelle Koordinate gelten; echte komplexe und quaternionische Werte verlangen eine ausdrückliche Projektion. Ein Objekt mit mehr Komponenten als der gewählte Raum wird als projektionsbedürftig ausgewiesen. Bei weniger Komponenten ist es in diesem Raum nicht darstellbar. Eine unentscheidbare Fallbedingung oder unbelegte Variable bleibt bedingt darstellbar.

Ein `KoordinatenBild(objekt, system)` wird ausschließlich gegen die Dimension und den Raum seines gespeicherten Koordinatensystems ausgewertet. Stimmen gewählter Visualisierungsraum und Koordinatensystemdimension nicht überein, wird keine Koordinate still verworfen oder ergänzt.

## Daten-, Knoten-, Anschluss- und Verbindungsvertrag

Es entstehen nur kurzlebige, nicht persistierte Adapter- und Ergebniswerte. Knotenarten, Anschluss-IDs, Anschlussrichtungen, Kardinalitäten, Verbindungen, Kartenformat und Migrationen bleiben unverändert.

## Architekturentscheidungen

- Der gemeinsame Adapter liegt im Modul `MathematikKnoten`, weil er die Visualisierergrenze zwischen Rechenkernobjekten und räumlichem Sampling bildet.
- Komponentenstruktur wird aus `NumerischeKomponentenAnsicht` abgeleitet.
- Zahlwerte werden domänenerhaltend ausgewertet; der Visualisierer erfindet keine Projektion.
- Der Sampler übersetzt Adapterzustände zentral in `VisualisierungsDefinition` und `VisualisierungsErgebnis`.

## Betroffene Dateien und Symbole

- `visualisierung/koordinaten/KoordinatenAdapter.kt`
- `visualisierung/sampling/VisualisierungsSampler.kt`
- `KoordinatenAdapterTest.kt`
- `VisualisierungsNormalisierungTest.kt`
- `app/build.gradle.kts`
- `release/roadmap.toml`

## Meilensteine

- [x] PR-Entwurf auf dem veröffentlichten `v2.28.0` rekonstruiert.
- [x] Adapter auf domänenerhaltende Auswertung und Koordinatenbilder vervollständigen.
- [x] Private Samplerextraktion vollständig durch den Adapter ersetzen.
- [x] Strukturierte Diagnosen und Ergebnisqualität durchreichen.
- [x] Release-Metadaten auf `2.28.1` synchronisieren.
- [-] Lokale Release-Prüfungen ausführen; vollständige Gradle-/Android-Prüfungen in CI ausführen.
- [x] Abschlussdiff und lokale SamAI-Git-Identität prüfen.

## Konkrete Umsetzungsschritte

1. Den Adapter auf `DomaenenAuswerter` umstellen und strukturierte Ursachen erhalten.
2. Eine neutrale Abbildung von Adapterzuständen in Samplerdiagnosen ergänzen.
3. Endliche Mengen und Methodenausgaben über den Adapter auswerten.
4. `KoordinatenBild` anhand seines Koordinatensystems normalisieren.
5. Projektionsbedarf und bedingte Darstellbarkeit bis zum öffentlichen Ergebnis sichtbar machen.
6. Releaseplan und Android-Version für die x-Version `2.28.1` aktualisieren.

## Tests und Validierung

- isolierte Adaptertests für Tupel/Vektoren, heterogene Tupel, Skalare, Dimensionen, Fallausdrücke, echte komplexe Werte und Quaternionen,
- Samplerregression für R¹/R²/R³, teilweise endliche Mengen, Projektionsbedarf und Koordinatenbilder,
- `python3 scripts/pruefe_repository.py`,
- `python3 scripts/pruefe_releaseplan.py`,
- `python3 scripts/pruefe_versionsfolge.py`,
- `python3 scripts/pruefe_kern.py`,
- `./gradlew test`,
- `./gradlew :app:assembleDebug`.

## Persistenz und Migration

Keine persistierten Daten ändern sich. Adapterzustände, Samplingqualität und Hinweise werden bei jeder Auswertung neu erzeugt. Eine Migration ist nicht erforderlich.

## Risiken und Rückfallstrategie

Das größte Risiko ist eine unbeabsichtigte Änderung der bestehenden R¹/R²/R³-Samplingresultate. Die Umstellung erfolgt deshalb hinter demselben Samplervertrag und wird durch bestehende sowie neue Regressionstests abgesichert. Bei nicht entscheidbaren Komponenten liefert der Adapter eine Diagnose statt eines Ersatzwertes.

## Fortschritt

Der fehlerhafte Branchstapel von #346 wurde nicht übernommen. Nur die zwei Adapterdateien wurden auf dem veröffentlichten `master` rekonstruiert; alle weiteren Änderungen entstehen auf dem SamAI-Branch.

## Entscheidungsprotokoll

- 2026-08-08: `v2.28.1` ist eine x-Version, weil kein neuer registrierter Knotentyp entsteht.
- 2026-08-08: PR #346 kann wegen seiner veralteten Basis nicht direkt veröffentlicht werden; sein fachlicher Umfang wird auf einem regelkonformen SamAI-Branch ersetzt.
- 2026-08-08: Echte komplexe Zahlen und Quaternionen werden nie automatisch reell projiziert.

## Abweichungen vom ursprünglichen Plan

Der PR-Entwurf plante die Samplermigration erst nach dem isolierten Adapter. Für den Release wird Issue #180 vollständig abgeschlossen, damit kein unbenutzter Parallelpfad veröffentlicht wird.

## Ergebnis und Verifikation

Der zentrale Adapter verarbeitet Zahlen, Tupel, Zeilen- und Spaltenvektoren sowie Koordinatenbilder
über denselben domänenerhaltenden Pfad. `VisualisierungsSampler` verwendet keine parallele private
Koordinatenextraktion mehr. Bedingte Darstellbarkeit, Projektionsbedarf und endgültige
Nichtdarstellbarkeit werden bis zur sichtbaren Visualisierungsdiagnose erhalten. Android-Version und
Releaseplan sind auf `2.28.1` beziehungsweise `2028001` synchronisiert; es entsteht kein neuer
Knotentyp und keine Persistenzmigration.

Lokal erfolgreich ausgeführt:

- `python3 scripts/pruefe_repository.py`,
- `python3 scripts/pruefe_releaseplan.py`,
- `python3 scripts/pruefe_versionsfolge.py`,
- `bash scripts/samai-git.sh verify HEAD` für den vorbereiteten lokalen SamAI-Stand.

Lokal nicht ausführbar:

- `python3 scripts/pruefe_kern.py`, weil `kotlinc` in der Laufzeit fehlt,
- `./gradlew test` und `./gradlew :app:assembleDebug`, weil der Wrapper die Gradle-Distribution
  wegen fehlender DNS-Auflösung für `services.gradle.org` nicht laden kann.

Release-Guard, vollständige Tests und Android-Build müssen deshalb vor dem Squash-Merge in GitHub CI
grün sein. Der über das GitHub-Plugin rekonstruierte Releasecommit ist ein Connector-Commit und kann
die lokale SamAI-Autor-/Committeridentität technisch nicht übernehmen.
