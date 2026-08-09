# Architekturrefactor des Mathematik Atlas

## 1. Titel und Status

**Status:** implementiert und CI-verifiziert; Release-PR #392 gegen `master` ist offen.

**Basis:** `c79400e1f8ff98ff654ee931848ddcbd9427f732` auf `master`, nach veröffentlichtem `v2.32.0`.

**Releasebranch:** `release/v2.32.1-architektur-refactor`.

**Versionsklassifikation:** `v2.32.1` (`x`-Version), da keine neuen separat erzeugbaren Knotentypen eingeführt werden.

## 2. Ziel und Nutzerwirkung

Die schnell gewachsene Architektur wird so geordnet, dass mathematische Knoten, Registrierungen, Plattformkataloge und spätere Modultrennungen nicht mehr über app-seitige Versionsadapter und zufällige Dateipfade gekoppelt sind. Android und Desktop verwenden denselben kanonischen Knotenkatalog. Die Reihenfolge der Auswerter-Erweiterungen ist als explizite, benannte Registrierungsphasen modelliert statt in einer fachfremden Datei unter `geometrie/` zu wachsen.

Langfristig sollen Graphmodell, Graph-UI, Mathematikknoten-Semantik, Mathematikknoten-UI und Anwendungsshell weiter getrennt werden. Die aktuelle Toolchain verhindert jedoch eine sofortige KMP-Migration der Android-/Desktop-Shared-UI: Kotlin 2.3.21 ist mit dem Kotlin-Multiplatform-Plugin nicht für AGP 9.3.x freigegeben. Ein AGP-Downgrade gehört ausdrücklich nicht in diesen Refactor.

## 3. Nicht-Ziele

- keine Änderung persistierter Knoten-, Anschluss-, Karten- oder Verbindung-IDs,
- keine neue mathematische Semantik,
- keine neuen Knotentypen,
- kein AGP-/Kotlin-Downgrade nur zur Ermöglichung von Kotlin Multiplatform,
- keine Änderung bestehender Kartenformate,
- keine Entfernung historischer Lade- oder Migrationspfade.

## 4. Untersuchter Istzustand

- `MathematikKnotenVorlagenV2300.kt` bereinigte den sichtbaren Android-Katalog app-seitig und ersetzte historische Vektorknoten durch kanonische Varianten.
- Desktop verwendete dagegen direkt `de.TeutonStudio.MathematikKnoten.alleMathematikKnotenVorlagen()` und konnte dadurch vom Android-Katalog abweichen.
- `GesamterMathematikAuswerter.kt` lag im Verzeichnis `geometrie/`, obwohl er globale Registrierungen aus Zahlen, Mengen, Aussagen, Analysis, linearer Algebra, Geometrie und Methoden orchestrierte.
- Die Registrierungsreihenfolge enthält bewusst ersetzende Wrapper und ist daher semantisch relevant, war aber nur durch Kommentare im ausführenden Block dokumentiert.
- Die Desktop-Brückenmodule kompilieren Quellordner anderer Module erneut. Das bleibt technische Schuld; eine saubere gemeinsame Compose-UI setzt mit der aktuellen AGP-9.3.x-Toolchain eine spätere Toolchainentscheidung voraus.

## 5. Fachliche und mathematische Semantik

Unverändert. Der Refactor verändert ausschließlich Registrierungs- und Katalogorganisation. Für dieselbe gespeicherte Karte bleiben dieselben Knotentypen, Anschlussverträge und Auswerter erreichbar.

## 6. Daten-, Node-, Handle- und Edge-Vertrag

Unverändert. Alle stabilen IDs und Parameter bleiben bestehen. Historische Typen bleiben ladbar, können aber weiterhin aus dem sichtbaren Erstellen-Katalog ausgeblendet werden.

## 7. Architekturentscheidungen

1. Der **kanonische sichtbare Mathematikknoten-Katalog** gehört in `MathematikKnoten`, nicht in `app`.
2. Android und Desktop beziehen denselben Katalog.
3. Versionsspezifische Namen wie `V2300` werden hinter kanonischen Fassaden isoliert; neue Produktpfade verwenden keine Versionssuffixe.
4. Auswerter-Erweiterungen werden in benannten, geordneten Phasen registriert.
5. Die KMP-/Desktop-Shadowmodul-Ablösung wird nicht durch ein AGP-Downgrade erzwungen. Sie bleibt ein nachfolgender Toolchain-Meilenstein.

Die dauerhafte Entscheidung ist unter `docs/codex/decisions/2026-08-09-kanonischer-knotenkatalog-und-registrierungsphasen.md` festgehalten.

## 8. Betroffene Dateien und Symbole

- `MathematikKnoten/.../katalog/KanonischerMathematikKnotenKatalog.kt`
- `MathematikKnoten/.../katalog/MathematikAuswerterPakete.kt`
- `MathematikKnoten/.../katalog/GesamterMathematikAuswerter.kt`
- kanonische Fassaden für Vektor-/Multinomimplementierungen
- `app/.../MathematikKnotenKatalog.kt`
- `desktopApp/.../DesktopAtlasZustand.kt`
- `scripts/pruefe_architektur.py`
- zugehörige JVM-Tests
- Architektur- und Release-Dokumentation

## 9. Meilensteine

- [x] Istzustand und Toolchain-Grenze geprüft.
- [x] Kanonischen plattformübergreifenden Knotenkatalog in `MathematikKnoten` eingeführt.
- [x] Registrierungsphasen aus `GesamterMathematikAuswerter` extrahiert.
- [x] Versionsspezifische Produktadapter hinter kanonischen Fassaden isoliert.
- [x] Android und Desktop auf dieselbe Katalogquelle umgestellt.
- [x] Architekturtests und Regressionstests ergänzt.
- [x] Release-Metadaten und Dokumentation auf `v2.32.1` aktualisiert.
- [x] GitHub-Actions-Validierung ausgewertet: alle vier Releasepfade erfolgreich.

## 10. Konkrete Umsetzungsschritte

1. `KanonischerMathematikKnotenKatalog` übernimmt die bisher app-seitige Filter-/Ersetzungslogik.
2. App-Funktion `alleMathematikKnotenVorlagen()` ist nur noch eine dünne Kompatibilitätsfassade ohne fachliche Kataloglogik.
3. `DesktopAtlasZustand` verwendet denselben kanonischen Katalog.
4. `StandardMathematikAuswerterPakete` gruppiert die Registrierung in Basisdomänen und absichtlich nachgelagerte Verfeinerungen.
5. `GesamterMathematikAuswerter` installiert nur noch diese geordnete Liste.
6. `pruefe_architektur.py` verbietet die Rückkehr des app-seitigen Versionskatalogs und prüft die kanonischen Katalog- und Orchestrierungspfade.
7. Regressionstests sichern Katalogkonsolidierung, Paketnamen, Paketordnung und zentrale Auswerterregistrierungen.
8. Globale Null-Distanz- und Vektor-/Multinomdateien wurden in fachlich passende Verzeichnisse verschoben, ohne ihre Packages oder Persistenzverträge zu ändern.

## 11. Tests und Validierung

Die lokale Connector-Umgebung besitzt keinen Checkout mit nutzbarem externem Netzwerk; lokale Gradle-Ausführung wurde daher nicht als erfolgt ausgegeben. Die vollständige Verifikation erfolgte über GitHub Actions auf Release-PR #392, Head `13e017c4e447930dd3ecdce7dc027f4a23c9b78e`:

- **Release-Guard**, Run `31330536989`: erfolgreich.
- **Mathematikkern prüfen**, Run `31330536952`: erfolgreich. Repositorystruktur, Standardkarten, Releaseplan und Kernprüfung bestanden; JVM-Tests und Android-Debug-Build innerhalb dieses Workflows bestanden.
- **Linux-Desktop**, Run `31330536981`: erfolgreich. Gemeinsame/Desktop-Tests sowie RPM-Paketierung und Paketprüfung bestanden.
- **Android-Build**, Run `31330536978`: erfolgreich. Architekturprüfung sowie Bauen und Testen bestanden.

Damit sind die vorgesehenen Architektur-, Kern-, JVM-, Android- und Desktoppfade für den implementierten Stand grün.

## 12. Persistenz und Migration

Keine Formatänderung. Historische Knotentypen bleiben im lade-kompatiblen Basiskatalog beziehungsweise in den bestehenden Migrationspfaden. Nur der sichtbare Erstellen-Katalog wurde zentralisiert. Stabile Knoten-, Anschluss-, Karten- und Verbindung-IDs wurden nicht geändert.

## 13. Risiken und Rückfallstrategie

Das zentrale Risiko war eine unabsichtliche Abweichung des sichtbaren Katalogs zwischen bisheriger Android-Logik und neuer zentraler Logik. Die neue Architekturprüfung und die Katalogregressionstests schützen diesen Vertrag. Bei einer später erkannten Regression kann die zentrale Katalogfassade ohne Datenmigration angepasst werden.

Die Desktop-Shadowmodule bleiben bewusst bestehen. Ihre spätere physische Ablösung ist ein separater Toolchain-Refactor und kein verdeckter Rest dieses Releases.

## 14. Fortschritt

Die Implementierung ist abgeschlossen und über die Release-CI verifiziert. Ausstehend ist ausschließlich die gesonderte Veröffentlichung des bereits geprüften Releasebranches nach `master`.

## 15. Entscheidungsprotokoll

### 2026-08-09: Kein KMP-Umbau unter AGP 9.3.x erzwingen

**Entscheidung:** Die Desktop-Shadowmodule werden in diesem Release nicht durch einen riskanten KMP-Umbau ersetzt.

**Alternativen:** AGP auf eine von Kotlin 2.3.21 für KMP unterstützte Version absenken; Kotlin/AGP gemeinsam auf einen kompatiblen zukünftigen Stand migrieren.

**Begründung:** Ein Buildtool-Downgrade ist kein neutraler Refactor und würde den Umfang mit Toolchainrisiken vermischen.

**Konsequenz:** Dieser Release beseitigt die fachlichen Plattformabweichungen und zentralen Registrierungsprobleme; die physische Shared-UI-Modulablösung bleibt separat.

### 2026-08-09: Sichtbaren Katalog in die Mathematikschicht verlagern

**Entscheidung:** Die bisherige app-seitige Vektor-/Multinomkonsolidierung ist in `KanonischerMathematikKnotenKatalog` überführt und wird von Android wie Desktop genutzt.

**Begründung:** Die Plattform darf nicht bestimmen, welche mathematischen Knoten fachlich kanonisch sind.

**Konsequenz:** Neue mathematische Katalogkonsolidierungen werden in `MathematikKnoten` vorgenommen; Plattformmodule ergänzen nur Plattform- oder Kartenwerkzeuge.

## 16. Abweichungen vom ursprünglichen Plan

Der ursprüngliche Zielentwurf sah die sofortige Ablösung der Desktop-Quellordnerbrücken durch Kotlin Multiplatform vor. Nach Prüfung der aktuellen Toolchain wurde dieser Schritt bewusst getrennt, weil die verwendete Kombination Kotlin 2.3.21 / AGP 9.3.1 außerhalb der freigegebenen KMP-Kompatibilität liegt.

Stattdessen wurde der unmittelbar sichere Teil vollständig umgesetzt: gemeinsame fachliche Katalogquelle, explizite Registrierungsphasen, versionsfreie Produktfassaden, fachliche Dateisortierung und automatisierte Architekturgrenzen.

## 17. Ergebnis und Verifikation

Der Releasebranch enthält einen verifizierten Architekturrefactor ohne neue Knotentypen oder Persistenzänderungen. Android und Desktop verwenden denselben sichtbaren Mathematikknoten-Katalog; die globale Auswerterregistrierung ist aus dem Geometriepfad gelöst und ihre Reihenfolge explizit modelliert. Alle vier GitHub-Actions-Releasepfade waren für den implementierten Code-Stand erfolgreich.

Release-PR #392 ist bereit zur gesonderten Veröffentlichung nach `master`.
