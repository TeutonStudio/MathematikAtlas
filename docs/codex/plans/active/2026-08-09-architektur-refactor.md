# Architekturrefactor des Mathematik Atlas

## 1. Status und Basis

**Status:** Implementierung abgeschlossen; finale Release-CI nach dem Abschlussmerge steht noch aus.

**Basis:** `c79400e1f8ff98ff654ee931848ddcbd9427f732` auf `master`, nach veröffentlichtem `v2.32.0`.

**Release:** `v2.32.1` auf `release/v2.32.1-architektur-refactor`.

**Klassifikation:** `x`-Version. Es entstehen keine neuen separat erzeugbaren Knotentypen.

## 2. Ziel

Die gewachsene Architektur wird so geordnet, dass mathematische Kataloge, Auswerterregistrierung, Kartenmigration und Laufzeitkomposition nur noch eine fachliche Quelle besitzen. Android und Desktop dürfen diese Kernverträge nicht separat nachbauen.

Die physische Entfernung der drei Desktop-Shadowmodule ist bewusst nicht Teil dieses Releases, weil dafür mit der aktuellen Kombination Kotlin 2.3.21 / AGP 9.3.1 kein unterstützter KMP-Android-Library-Pfad vorliegt. Das Folgevorhaben ist vollständig in GitHub-Issue #395 beschrieben.

## 3. Nicht-Ziele

- keine Änderung persistierter Knoten-, Anschluss-, Karten- oder Verbindung-IDs,
- keine neue mathematische Semantik,
- keine neuen Knotentypen,
- keine Änderung der JSON-`formatVersion`,
- kein AGP-Downgrade nur zur Erzwingung von KMP,
- keine Vermischung mit neuen mathematischen Features.

## 4. Phase A: Katalog, Registrierung und Ordnerstruktur

Umgesetzt:

- `KanonischerMathematikKnotenKatalog` als gemeinsame sichtbare mathematische Katalogquelle,
- Entfernung des app-seitigen `MathematikKnotenVorlagenV2300.kt`,
- Android und Desktop auf denselben Katalog umgestellt,
- `MathematikAuswerterPaket` und `StandardMathematikAuswerterPakete` für benannte Basis- und Verfeinerungsphasen,
- `GesamterMathematikAuswerter` aus dem Geometrieordner nach `katalog/` verschoben,
- versionsfreie Fassaden vor historischen `V2300`-Implementierungen,
- Null-Distanz sowie Vektor-/Multinomdateien fachlich unter `rechnen/` beziehungsweise `vektor/` einsortiert,
- Katalog- und Registrierungsregressionstests,
- Architekturguards gegen erneute app-seitige Kataloglogik.

ADR: `docs/codex/decisions/2026-08-09-kanonischer-knotenkatalog-und-registrierungsphasen.md`.

## 5. Phase B: gemeinsame Karten- und Migrationspipeline

Umgesetzt:

- Transpositionsmigration aus `app` nach `MathematikKnoten/migration`,
- Methodenanschlussmigration aus `app` nach `MathematikKnoten/migration`,
- `MathematikKartenCodec` als gemeinsame mathematische Serialisierungsfassade,
- `MathematikKartenMigrationen` mit expliziter Vorspeicher-, Dekodier- und Lade-/Importphase,
- strukturierte-Divisions-Normalisierung in die gemeinsame Vorspeicherphase übernommen,
- Android-`KartenJson` zu einer dünnen Fassade reduziert,
- Android- und Desktop-Dateispeicher auf dieselbe mathematische Schreib-/Lade-/Importpipeline umgestellt,
- Migrationstests in das Mathematikknoten-Modul verschoben,
- Roundtrip- und Phasentests ergänzt,
- Architekturguards gegen plattformlokale mathematische Migrationen.

Der fachneutrale `KartenDatenJson`-Codec bleibt unverändert im Graphmodul und kennt weiterhin keine Mathematikschicht.

ADR: `docs/codex/decisions/2026-08-09-gemeinsame-kartenmigrationspipeline.md`.

ExecPlan: `docs/codex/plans/active/2026-08-09-persistenz-pipeline.md`.

## 6. Phase C: gemeinsame Mathematik-Kartenlaufzeit

Umgesetzt:

- `MathematikKartenLaufzeit` unter `MathematikKnoten/laufzeit`,
- zentrale Zusammensetzung von `AnschlussArtRegister`, `GraphPrüfung`, `KartenAuswerter`, `GesamterMathematikAuswerter`, Cache und kanonischem Katalog,
- Android `AtlasZustand` auf die gemeinsame Laufzeit umgestellt,
- Desktop `DesktopAtlasZustand` auf dieselbe Laufzeit umgestellt,
- Android-spezifische Nachsynchronisierung dynamischer Restriktions- und Methodenanschlüsse unverändert erhalten,
- Laufzeit-/Cachetests ergänzt,
- Architekturprüfung verbietet erneute direkte Plattformverdrahtung der gemeinsamen Komponenten.

ADR: `docs/codex/decisions/2026-08-09-gemeinsame-mathematik-kartenlaufzeit.md`.

ExecPlan: `docs/codex/plans/active/2026-08-09-gemeinsame-kartenlaufzeit.md`.

## 7. Verbleibende Toolchain-Schuld

Noch physisch vorhanden:

- `KnotenKartenVerwalterDesktop`,
- `MathematikKartenAdapterDesktop`,
- `MathematikKnotenDesktop`.

Diese Module kompilieren gemeinsame Produktionsquellen derzeit über relative `srcDir`-Pfade erneut. Die Schuld ist nun begrenzt:

- GitHub-Issue #395 enthält Zielarchitektur, Toolchain-Gate und Abnahmekriterien,
- `scripts/pruefe_desktop_shadowmodule.py` erlaubt exakt die vier aktuell notwendigen relativen `srcDir`-Einträge,
- `scripts/pruefe_repository.py` führt diese Prüfung in jeder Repositoryprüfung aus,
- die Whitelist darf nur im Zuge von #395 kleiner werden; neue Source-Sharing-Pfade schlagen in CI fehl.

Damit bleibt die nicht sofort lösbare Toolchain-Schuld explizit, begrenzt und maschinell überprüft statt sich weiter auszubreiten.

## 8. Persistenz- und Kompatibilitätsvertrag

Unverändert bleiben:

- Kartenformat und `formatVersion`,
- `KartenId`, `KnotenId`, `AnschlussId`, `VerbindungsId`,
- persistierte Knotenart-Schlüssel,
- Anschlussnamen und historische Migrationsfähigkeit,
- vorhandene Kartenreferenzen und Versionierungssemantik.

Migrationen wurden verschoben und zentralisiert, nicht fachlich neu erfunden.

## 9. Tests

Neu beziehungsweise verschoben:

- `KanonischerMathematikKnotenKatalogTest`,
- `MathematikAuswerterPaketeTest`,
- `MethodenAnschlussMigrationTest`,
- `TranspositionsMigrationTest`,
- `MathematikKartenCodecTest`,
- `MathematikKartenLaufzeitTest`,
- erweiterte `pruefe_architektur.py`,
- `pruefe_desktop_shadowmodule.py`.

Releaseabnahme benötigt auf dem finalen Head:

1. Release-Guard,
2. Mathematikkern/JVM-Tests,
3. Android-Build,
4. Linux-Desktop inklusive Tests und Paketierung.

## 10. Frühere Verifikation

Der erste Refactorschnitt war auf Head `9dbe73c35504f251a94ad78e3127102fc3c87c2a` bereits vollständig grün:

- Release-Guard `31330804331`,
- Mathematikkern `31330804315`,
- Linux-Desktop `31330804338`,
- Android-Build `31330804309`.

Nach Übernahme der Persistenz- und Laufzeitphasen wird diese Abnahme auf dem finalen Release-Head vollständig wiederholt. Frühere grüne Läufe werden nicht als Ersatz für die Abschlussprüfung ausgegeben.

## 11. Abweichung vom ursprünglichen Zielentwurf

Der ursprüngliche Zielentwurf sah auch die sofortige physische Trennung von Graphkern/Compose-Editor sowie die vollständige Beseitigung der Desktop-Shadowmodule vor. Diese letzte physische Modulgrenze ist an die Toolchain gekoppelt und wird nicht mit einer nicht unterstützten KMP-Kombination oder einem verdeckten AGP-Downgrade erzwungen.

Stattdessen wurden in diesem Release alle unabhängig davon möglichen Architekturgrenzen umgesetzt: gemeinsame Katalogquelle, explizite Registrierungsphasen, gemeinsame Kartenmigration, gemeinsame Laufzeitkomposition, fachliche Dateisortierung und automatische Grenzen gegen neue Plattformduplikation. #395 ist der eindeutig abgegrenzte Rest für die physische Shared-UI-/Modulmigration.

## 12. Ergebnis

Der fachliche Architekturrefactor ist implementiert. Ausstehend ist nur noch die finale CI-Abnahme des Abschlussstands und anschließend die getrennte Veröffentlichung über Release-PR #392 nach `master`.
