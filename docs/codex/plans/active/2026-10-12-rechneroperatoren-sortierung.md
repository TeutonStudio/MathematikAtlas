# Rechneroperatoren und Operandenreihenfolge

Status: implementiert und unabhängig statisch abgenommen; Android-Laufzeitabnahme offen, 2026-10-12

## Ziel und Nutzerwirkung
Einzeloperationen verlustfrei in bestehende Rechner überführen; dynamische Operanden im Inspector mit Ziehgriff und Pfeilen ordnen.

## Nicht-Ziele
Keine neuen Knotentypen, Produktionsabhängigkeiten oder Veröffentlichung. Keine Änderung persönlicher IDE-Dateien.

## Untersuchter Istzustand
Zahlen-, Mengen-, Aussagen-, Tensor- und Vektormigrationen bestehen bereits. Matrixprodukt und Matrixinverse bleiben separate Erstellenknoten. Matrixaddition ist im Kern variadisch, ihr Anschlussvertrag bisher binär. Vektorauswertung sortiert teilweise Namen. Android und Desktop haben getrennte Inspectorhosts, aber gemeinsame Compose-Bibliotheken.

## Fachliche Semantik
Assoziativität erlaubt mehrere Operanden, keine Vertauschung. Matrixketten werden weiterhin linksweise ausgewertet; innere fehlende Operanden sind Fehler. Nichtassoziative Operationen bleiben feststellig.

## Daten-, Knoten-, Anschluss- und Verbindungsvertrag
Stabile IDs und Anschlussnamen bleiben bei Umordnung erhalten. Nur Reihenfolge ändert sich. Freie dynamische Schlussreserve bleibt zuletzt. Feste Zusatzrollen bleiben außerhalb der Gruppe. Änderungen bilden eine Historienaktion.

## Architekturentscheidungen
Fachneutraler Sortiervertrag und Compose-Liste im Karteneditor; mathematische Eignung und Migration in MathematikKnoten. Bestehende Registry und Codec-Pipeline erweitern. Dauerhafter Vertrag in ADR.

## Betroffene Bereiche
Operatorverträge, Konfiguratoren/Auswerter, kanonischer Katalog, gemeinsame Kartenmigration, Android-/Desktop-Inspector.

## Meilensteine und Schritte
- [x] Bestehende Pfade und vergleichbare Methodenargumentliste untersucht.
- [x] Operatorverträge und Auswertung vereinheitlicht; Drei-Operanden-, Fehlereingangs- und nichtkommutierende Matrixtests bestehen.
- [x] Migration und Katalogzuordnung ergänzt; IDs, Parameter, Quellen, persönliche Titel und wiederholte Codec-Normalisierung getestet.
- [x] Gemeinsame Liste und validierte Aktion integriert; Desktop-Pfeilklick, Ziehen, Scrollen und einzelner Undo praktisch geprüft.
- [x] Automatisierte Tests, Android-Build/Lint, unabhängige statische Verifikation und Abschlussdiff.
- [ ] Android-Gesten und Tastatur auf Emulator/Gerät praktisch prüfen; isolierter Emulator stürzt vor dem Booten ab.

## Tests und Validierung
Gezielte Kern-/Graph-/Knoten-/Codec-Tests, vollständige JVM-/Desktoptests, Repositoryprüfung, Android-Build/Lint. Laufzeitprüfung separat nachweisen; Umgebungshindernisse konkret festhalten.

## Persistenz und Migration
JSON-Format 8 bleibt. Lade-/Importmigration idempotent, keine verlorenen IDs oder Verbindungen. Unbekannte Typen bleiben unverändert.

## Risiken und Rückfallstrategie
Historische Parameter und Ausgangsrollen müssen exakt zugeordnet sein. Nur nachgewiesen äquivalente Pfade migrieren; andernfalls Altbestand erhalten. Kein pauschales Neuaufbauen der Anschlüsse.

## Fortschritt
Umgesetzt in Operatorverträgen des Rechenkerns, Rechnerkonfiguratoren und Auswertern, `RechnerEinzeloperationMigration`, `MathematikKartenCodec`, fachneutralem Sortierhelfer und gemeinsamer `DynamischeOperandenListe` sowie beiden Inspectorhosts.

Die freie Reserve bleibt bei reiner Umordnung auch in Undo-Snapshots erhalten. Der Android-Konfigurationsplan erhält sie ebenfalls, wenn abhängige Signaturen synchronisiert werden. Kartenquellen und explizite Standardwerte zählen als belegte Operanden; implizite Booleanwerte dürfen weder diese Quellen verdrängen noch eine freie Reserve füllen.

Historische Rollenumbenennungen nehmen `standardwert.*` und `eingangsKartenVerweise` mit. Mehrdeutige neue Quellenrollen werden diagnostiziert und behalten den alten Knoten. Tensorachsengruppen bleiben außerhalb der Operanden-Erhaltung, sodass ihre explizite Achsenzahl weiterhin geändert werden kann.

## Entscheidungsprotokoll
2026-10-12: Nutzer bestätigt alle Rechnerfamilien, Liste nur für dynamische Operationen, Ziehen und Pfeile sowie automatische Altkartenmigration.

2026-10-12: Gespeicherte Anschlussreihenfolge statt Namenssortierung als gemeinsame Wahrheit gewählt. Dadurch bleiben Vertauschungen nichtkommutierender Faktoren über Laden, Konfiguration, Cache und Falk-Vorschau stabil; siehe ADR 0044.

2026-10-12: Vorhandene mathematische Codec-Pipeline statt separater App-Migration gewählt, damit Desktop und Android dieselbe Konsolidierung anwenden.

## Abweichungen
Arbeitsbranch samai/v3.1.1/rechneroperatoren-konsolidierung trägt lediglich eine vorläufige Namensversion. Keine Version reserviert oder Produktmetadaten geändert, entsprechend Nutzerauftrag.

## Ergebnis und Verifikation
- `./gradlew test :app:assembleDebug :app:lintDebug --offline --continue`: erfolgreich, letzter Produktionsstand; zusätzliche Graph-/Knotenprüfungen für Kopie und Cache ebenfalls erfolgreich.
- 1.435 JVM-/Desktoptests, keine Fehler und keine übersprungenen Tests. Darin: 433 Kern-, 96 Graph-, 49 Adapter-, 586 Knoten-, 258 App-, 3 Desktop- und 10 TypSystemtests.
- `pruefe_repository.py`, `pruefe_releaseplan.py`, `pruefe_versionsfolge.py`, `pruefe_standardkarten.py` und `git diff --check`: erfolgreich. 99 freigegebene Knotentypen, 15 Standardkarten, Produktversion unverändert 3.1.0.
- `pruefe_kern.py`: Zusatzprüfung nicht ausführbar, weil `kotlinc` nicht im PATH liegt. Die Gradle-Kerntests bestehen.
- `node_verifier`: statische Freigabe ohne offene Findings nach Korrektur der Quellen-/Booleanreserve- und Zahlenrollenmigration.
- Desktop praktisch mit isolierten Testdaten unter `/tmp/atlas-runtime-data`: Pfeilklick, Langdruckziehen über drei Positionen, Scrollen und ein Undo pro Umordnung geprüft. Fensterinterner Prüfadapter und Screenshots liegen nur unter `/tmp/atlas-sort-ui`, kein Produktions-Prüfadapter.
- Android-Runtime bleibt offen: Kein bestehendes AVD. Ein isoliertes API-35-Tablet-AVD wurde unter `/tmp/atlas-sort-avd` aus dem vorhandenen Systemabbild erstellt. Emulator 36.5.11 stürzt sowohl mit SwiftShader als auch mit deaktivierter GPU/Vulkan vor der ADB-Verbindung mit Segmentation fault (Exit 139) ab. Logs: `/tmp/atlas-emulator.log`, `/tmp/atlas-emulator-software.log`. Auf persönliche Geräte wurde nichts installiert. Diese Einschränkung verhindert den Nachweis gerätespezifischer Gesten, nicht Build oder mathematische/Graph-Verifikation.
- Tastaturvertrag: Ziehgriff ist fokussierbar, 48 dp groß, mit sichtbarem Fokus und aussagekräftiger Semantik. ↑/↓ übernehmen nur bei KeyUp eine Aktion; Grenzbewegungen bleiben ohne Historieneintrag. Unabhängig statisch geprüft; native Tastaturinjektion in der Desktop-Sitzung lieferte keinen verlässlichen Fokusnachweis, daher kein behaupteter praktischer Tastaturnachweis.
- Persönliche `.idea`-Änderungen und lokale Werkzeugcaches gehören nicht zum Auftrag.
