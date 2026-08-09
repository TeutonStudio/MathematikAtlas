# Architekturrefactor des Mathematik Atlas

## 1. Titel und Status

**Status:** in Umsetzung auf `samai/v2.32.1/architektur-refactor`

**Basis:** `c79400e1f8ff98ff654ee931848ddcbd9427f732` auf `master`, nach veröffentlichtem `v2.32.0`.

**Versionsklassifikation:** `v2.32.1` (`x`-Version), da keine neuen separat erzeugbaren Knotentypen eingeführt werden.

## 2. Ziel und Nutzerwirkung

Die schnell gewachsene Architektur wird so geordnet, dass mathematische Knoten, Registrierungen, Plattformkataloge und spätere Modultrennungen nicht mehr über app-seitige Versionsadapter und zufällige Dateipfade gekoppelt sind. Android und Desktop sollen denselben kanonischen Knotenkatalog verwenden. Die Reihenfolge der Auswerter-Erweiterungen wird als explizite, benannte Registrierungsphasen modelliert statt in einer fachfremden Datei unter `geometrie/` zu wachsen.

Langfristig sollen Graphmodell, Graph-UI, Mathematikknoten-Semantik, Mathematikknoten-UI und Anwendungsshell getrennt werden. Die aktuelle Toolchain verhindert jedoch eine sofortige KMP-Migration der Android-/Desktop-Shared-UI: Kotlin 2.3.21 ist mit dem Kotlin-Multiplatform-Plugin nicht für AGP 9.3.x freigegeben. Ein AGP-Downgrade gehört ausdrücklich nicht in diesen Refactor.

## 3. Nicht-Ziele

- keine Änderung persistierter Knoten-, Anschluss-, Karten- oder Verbindung-IDs,
- keine neue mathematische Semantik,
- keine neuen Knotentypen,
- kein AGP-/Kotlin-Downgrade nur zur Ermöglichung von Kotlin Multiplatform,
- keine Änderung bestehender Kartenformate,
- keine Entfernung historischer Lade- oder Migrationspfade.

## 4. Untersuchter Istzustand

- `MathematikKnotenVorlagenV2300.kt` bereinigt den sichtbaren Android-Katalog app-seitig und ersetzt historische Vektorknoten durch kanonische Varianten.
- Desktop verwendet dagegen direkt `de.TeutonStudio.MathematikKnoten.alleMathematikKnotenVorlagen()` und kann dadurch vom Android-Katalog abweichen.
- `GesamterMathematikAuswerter.kt` liegt im Verzeichnis `geometrie/`, obwohl er globale Registrierungen aus Zahlen, Mengen, Aussagen, Analysis, linearer Algebra, Geometrie und Methoden orchestriert.
- Die Registrierungsreihenfolge enthält bewusst ersetzende Wrapper und ist daher semantisch relevant, aber nur durch Kommentare im ausführenden Block dokumentiert.
- Die Desktop-Brückenmodule kompilieren Quellordner anderer Module erneut. Das ist technische Schuld; eine saubere gemeinsame Compose-UI setzt mit der aktuellen AGP-9.3.x-Toolchain eine spätere Toolchainentscheidung voraus.

## 5. Fachliche und mathematische Semantik

Unverändert. Der Refactor darf ausschließlich Registrierungs- und Katalogorganisation verändern. Für dieselbe gespeicherte Karte müssen dieselben Knotentypen, Anschlussverträge und Auswerter erreichbar bleiben.

## 6. Daten-, Node-, Handle- und Edge-Vertrag

Unverändert. Alle stabilen IDs und Parameter bleiben bestehen. Historische Typen bleiben ladbar, können aber weiterhin aus dem sichtbaren Erstellen-Katalog ausgeblendet werden.

## 7. Architekturentscheidungen

1. Der **kanonische sichtbare Mathematikknoten-Katalog** gehört in `MathematikKnoten`, nicht in `app`.
2. Android und Desktop beziehen denselben Katalog.
3. Versionsspezifische Namen wie `V2300` werden hinter kanonischen Fassaden isoliert; neue Produktpfade verwenden keine Versionssuffixe.
4. Auswerter-Erweiterungen werden in benannten, geordneten Phasen registriert.
5. Die KMP-/Desktop-Shadowmodul-Ablösung wird nicht durch ein AGP-Downgrade erzwungen. Sie bleibt ein nachfolgender Toolchain-Meilenstein.

## 8. Betroffene Dateien und Symbole

- `MathematikKnoten/.../katalog/KanonischerMathematikKnotenKatalog.kt`
- `MathematikKnoten/.../katalog/MathematikAuswerterPakete.kt`
- `MathematikKnoten/.../katalog/GesamterMathematikAuswerter.kt`
- kanonische Fassaden für Vektor-/Multinomimplementierungen
- `app/.../MathematikKnotenKatalog.kt`
- `desktopApp/.../DesktopAtlasZustand.kt`
- `scripts/pruefe_architektur.py`
- zugehörige JVM-Tests
- Architektur- und Entwicklungsdokumentation

## 9. Meilensteine

- [x] Istzustand und Toolchain-Grenze geprüft.
- [-] Kanonischen plattformübergreifenden Knotenkatalog in `MathematikKnoten` einführen.
- [ ] Registrierungsphasen aus `GesamterMathematikAuswerter` extrahieren.
- [ ] Versionsspezifische Produktadapter hinter kanonischen Fassaden isolieren.
- [ ] Android und Desktop auf dieselbe Katalogquelle umstellen.
- [ ] Architekturtests und Regressionstests ergänzen.
- [ ] Release-Metadaten und Dokumentation auf `v2.32.1` aktualisieren.
- [ ] GitHub-Actions-Validierung auswerten.

## 10. Konkrete Umsetzungsschritte

1. `KanonischerMathematikKnotenKatalog` übernimmt die bisher app-seitige Filter-/Ersetzungslogik.
2. App-Funktion `alleMathematikKnotenVorlagen()` wird zu einer dünnen Kompatibilitätsfassade und verliert sämtliche fachliche Kataloglogik.
3. `DesktopAtlasZustand` verwendet denselben kanonischen Katalog.
4. `MathematikAuswerterPakete` gruppiert die Registrierung in Basisdomänen, fachliche Erweiterungen und absichtlich nachgelagerte Verfeinerungen.
5. `GesamterMathematikAuswerter` installiert nur noch diese geordnete Liste.
6. `pruefe_architektur.py` verbietet die Rückkehr des app-seitigen Versionskatalogs und prüft den kanonischen Katalogpfad.
7. Tests sichern eindeutige sichtbare Knotentypen und kritische Registrierungen.

## 11. Tests und Validierung

Geplant:

- `python3 scripts/pruefe_repository.py`
- `python3 scripts/pruefe_releaseplan.py`
- `python3 scripts/pruefe_versionsfolge.py`
- `python3 scripts/pruefe_kern.py`
- `./gradlew test`
- `./gradlew :app:assembleDebug`
- `./gradlew :desktopApp:test`

Die aktuelle Ausführungsumgebung besitzt keinen externen DNS-Zugriff und keinen lokalen Checkout. Deshalb müssen Build- und Gradle-Prüfungen über GitHub Actions erfolgen; diese Einschränkung wird nicht als erfolgreicher lokaler Test ausgegeben.

## 12. Persistenz und Migration

Keine Formatänderung. Historische Knotentypen bleiben im lade-kompatiblen Basiskatalog bzw. in den bestehenden Migrationspfaden. Nur der sichtbare Erstellen-Katalog wird zentralisiert.

## 13. Risiken und Rückfallstrategie

Größtes Risiko ist eine unabsichtliche Abweichung des sichtbaren Katalogs zwischen bisheriger Android-Logik und neuer zentraler Logik. Regressionstests vergleichen deshalb Typ-Eindeutigkeit und die ausgeblendeten historischen Varianten. Bei Problemen kann die Katalogfassade ohne Datenmigration auf die bisherige Filterfunktion zurückgesetzt werden.

## 14. Fortschritt

Siehe Meilensteine. Fortschritt wird während der Umsetzung aktualisiert.

## 15. Entscheidungsprotokoll

### 2026-08-09: Kein KMP-Umbau unter AGP 9.3.x erzwingen

**Entscheidung:** Die Desktop-Shadowmodule werden in diesem Release nicht durch einen riskanten KMP-Umbau ersetzt.

**Alternativen:** AGP auf eine von Kotlin 2.3.21 für KMP unterstützte Version absenken; Kotlin/AGP gemeinsam auf einen kompatiblen zukünftigen Stand migrieren.

**Begründung:** Ein Buildtool-Downgrade ist kein neutraler Refactor und würde den Umfang mit Toolchainrisiken vermischen.

**Konsequenz:** Dieser Release beseitigt die fachlichen Plattformabweichungen und zentralen Registrierungsprobleme; die physische Shared-UI-Modulablösung bleibt separat.

## 16. Abweichungen vom ursprünglichen Plan

Der ursprüngliche Zielentwurf sah die sofortige Ablösung der Desktop-Quellordnerbrücken durch Kotlin Multiplatform vor. Nach Prüfung der aktuellen Toolchain wird dieser Schritt bewusst getrennt, weil die verwendete Kombination Kotlin 2.3.21 / AGP 9.3.1 außerhalb der freigegebenen KMP-Kompatibilität liegt.

## 17. Ergebnis und Verifikation

Wird nach Abschluss ergänzt.
