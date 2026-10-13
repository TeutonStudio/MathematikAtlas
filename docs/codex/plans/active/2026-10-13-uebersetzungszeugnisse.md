# ExecPlan: Übersetzungszeugnisse

## Status
2026-10-13, Implementierung und Abschlussprüfung; Android-Geräteabnahme offen. Basis dc072189 mit bestehenden uncommitteten Vertrags-/Variablenänderungen; Ausgangsdiff unter /tmp/atlas-zeugnis-basis. Kein Release, neuer Knotentyp oder Commit.

## Ziel und Nutzerwirkung
Alle bestehenden Übersetzer erhalten versionierte Zeugnisse über Abbildungseigenschaften, Informationserhaltung und Genauigkeit. Explizite Zielwahl darf Verlust auslösen; automatische Konvertierung benötigt nachgewiesene Verlustfreiheit. Zeugnisse und Bildräume bleiben mit dem Ergebnis ohne Ursprungskarte übertragbar.

## Nicht-Ziele
Keine universellen Bijektivitätsbeweise, neue Knotentypen, Produktionsabhängigkeiten oder Veröffentlichung. Offene Eigenschaften bleiben offen.

## Untersuchter Istzustand
StrukturÜbersetzung/TupelGrundmengen liefern Werte/Oberräume; individuelle Räume werden im BedingterWert teilweise gehalten, werteVorrat im Übersetzer gelöscht. Alle atlas.uebersetzer-Zweige einschließlich früher Rückgaben und Indexdefinitionsbausteine müssen umschlossen werden. Formel-/Wertcodecs und Bindungsmetadaten sind bereits im Arbeitsstand vorhanden. Bestehende NachweisStatus und numerische Polaralgorithmen werden wiederverwendet.

## Fachliche Semantik
Injektivität, Surjektivität auf Zielträger, Rückführbarkeit mit Kontext und numerische Genauigkeit sind unabhängig. Heterogene Vektoren haben einen Zielträger und einen genaueren Bildraum. Abflachen speichert einen Strukturplan einschließlich leerer Untertupel. Numerische Polarresultate sind keine exakten Bijektionen. Punktprojektionen verwerfen Objektinformation.

## Daten-, Node-, Handle- und Edge-Vertrag
Kern: ÜbersetzungsAnfrage/Ergebnis/Zeugnis/Kontext/Bildraum und StrukturRückführungsplan. BedingterWert führt aktuelles Zeugnis getrennt von Herkunft. Keine geänderten Anschlüsse oder neuen Knotenarten. Abgeleitete Berechnungen dürfen Herkunft nicht als aktuelles Zeugnis ausgeben.

## Architekturentscheidungen
Ein gemeinsamer typisierter Zielkatalog im Kern ersetzt die bestehende Stringliste als Quelle. Bestehende Rechner werden weiterverwendet, Spezialvarianten konservativ klassifiziert. Zeugnisse enthalten Daten und Regelversionen, keine Funktionen oder komplette Originalwerte. Unbekannte Versionen bleiben erhalten, aber ungeprüft. Darstellung verwendet gemeinsame Compose-Knotenanzeige und bestehendes Auswertungsschema. ADR: `docs/codex/decisions/2026-10-13-uebersetzungszeugnisse.md`.

## Betroffene Dateien
Kern: ÜbersetzungsVertrag, StrukturÜbersetzung/TupelGrundmengen, Wert-/Formelcodecs und Methodenmetadaten. Adapter: Auswertung/Register/KartenAuswerter. Knoten: ÜbersetzerKnoten, VektorOrientierungsKnoten, MathematikKnotenRenderer. Bestehender Vertrags-ExecPlan wird ergänzt.

## Meilensteine
- [x] Kernvertrag und Strukturplan/Bildraum
- [x] Alle Übersetzer-/Indexvarianten und Legacy-Strukturpfade
- [x] Herkunft, Formel-/Methoden-/Codecübertragung
- [x] Gemeinsame Anzeige und Desktop-Compose-Laufzeitnachweis
- [ ] Android-Geräte-/Emulator-Laufzeitnachweis (kein Gerät/AVD verfügbar)
- [x] Konformität, Gesamtprüfungen, unabhängige statische Verifikation

## Umsetzungsschritte
1. Typisierte Ziele, getrennte Nachweise und Rückführungsplan implementieren.
2. Alle bestehenden Übersetzerausgaben zentral bezeugen, Fehler strukturiert ausgeben.
3. Metadaten zuverlässig übertragen und aktuelle Zeugnisse bei Ableitungen invalidieren.
4. Explizite Codecvarianten/Versionen und gemeinsame Anzeige ergänzen.
5. Jede Variante mit Referenzfall prüfen, End-to-End-Roundtrips und Gesamttests ausführen.

## Persistenz und Migration
Neue optionale Metadatenfelder und versionierte Codecs lesen alte Varianten weiterhin. Karten rekonstruieren Zeugnisse bei Auswertung. Unbekannte Regelversionen werden erhalten und herabgestuft. Keine Rekonstruktion verlorener Altdaten. Strukturpläne speichern nur Form/Pfade.

## Tests und Validierung
Struktur-/Bereichsroundtrip, leere/verschachtelte Tupel, verlorene Periodeninformation, Polar-Singularitäten/Näherung, Geometrieprojektion, Methodensignaturen/Indexvarianten, abgeleitete Herkunft, Codec/Graph/Formel/Methodenexport, unbekannte Versionen; Gradle-Gesamttests, Android-Build/Lint, Desktoptests, Repository-/Releaseprüfungen. Laufzeitbelege gesondert.

## Risiken und Rückfallstrategie
Umfangreicher Ausgangsarbeitsstand bleibt unangetastet. Unbewiesene Eigenschaften werden nicht behauptet. Neue Metadaten dürfen bestehende Werttypen nicht durch Wrapper verändern. Codecvarianten sind explizit statt reflexiv.

## Fortschritt
Kernvertrag, Strukturpläne und symbolische Bildräume implementiert. Alle 14 bisherigen atlas.uebersetzer-Ziele, Abflachen und verfügbare historische Übersetzer zentral umschlossen. Referenzfälle und Codecroundtrips in ÜbersetzungsZeugnisTest/ÜbersetzungsZeugnisIntegrationTest. Historische Methoden-/Formelcodecs bleiben lesbar. Gemeinsamer Ergebnisdialog zeigt vollständige Details; Desktop-Compose-Test prüft schmale/niedrige Fenster, 1x/2x Schrift, hell/dunkel.

Unabhängiger node_verifier fand und überprüfte Korrekturen für unbekannte Rückführungsregeln, übergangene Literalbedingungen, erneut als frei ausgegebene gebundene Quellen und vertauschte Bildraumpfade. Nach gezielter statischer Nachprüfung keine weiteren Blocker; keine Freigabe des gesamten übergeordneten 77-Punkte-Vertrags.

## Entscheidungsprotokoll
Nutzer bestätigt alle bestehenden Übersetzer, keine zusätzliche Verlustfreigabe, selbsttragende Zeugnisübertragung und strukturelle Rückführung beim Abflachen.

## Abweichungen
Android-Gerätelaufzeit nicht ausführbar: `adb devices` ohne Geräte; `/home/alex/.android/avd` fehlt. Android-Instrumentierungstest ergänzt und separat kompiliert. Zusätzliche Standalone-Kernprüfung meldet fehlendes `kotlinc`; Gradle-Kerntests sind verfügbar. Keine Versionsreservierung/Veröffentlichung/Commits.

## Ergebnis und Verifikation
Implementierung für den abgegrenzten Übersetzervertrag abgeschlossen; Android-Gerätelaufzeit bleibt als ausdrücklicher Abnahmepunkt offen.

- `./gradlew test :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :desktopApp:compileKotlin`: erfolgreich; 1703 Tests, keine Fehler oder übersprungenen Tests. Desktop-Zeugnistest tatsächlich ausgeführt (1 Test, 0 übersprungen).
- Zusätzlicher kompletter Knotentestlauf nach Präzisierung der Minor-Referenzkennungen: siehe `/tmp/zeugnisse-inventur-final.log`.
- `python3 scripts/pruefe_repository.py`, `pruefe_releaseplan.py`, `pruefe_versionsfolge.py`: erfolgreich.
- `python3 scripts/pruefe_kern.py`: zusätzliche Standalone-Prüfung nicht ausführbar, `kotlinc` fehlt. Gradle-Kerntests erfolgreich.
- `git diff --check`: erfolgreich.
- Android-Instrumentierung: Test-APK erfolgreich gebaut; `adb devices` ohne Geräte und kein AVD vorhanden, daher nicht ausgeführt.
- Branch `samai/v3.2.1-variable-grundraum-tupel-ordnung`, Basis `dc072189446fb387af3b9ff04936e0bf12a4d7f7`. Keine Commits, kein Push/Release. Basisautor Alexander, Basiscommitter GitHub; keine neuen SamAI-Commitmetadaten erzeugt. Vorbestehender fremder Arbeitsstand bleibt erhalten.

## Varianteninventur und Referenzzuordnung

Alle Zweige werden außen am vorhandenen Auswerter umschlossen. Kein unabhängiger mathematischer Ersatzalgorithmus.

| Familie | Varianten | Referenz |
| --- | --- | --- |
| Struktur | tupel, zeile, spalte; historische Tupel-/Vektorumwandlungen | Kern-Struktur-/Bildraumtests, bestehende ÜbersetzungsKnotenTest |
| Abflachen | eine Ebene, alle Ebenen; (), (()), ((a,b),(),c), geschachteltes Einertupel | ÜbersetzungsZeugnisTest: Abflachungspläne |
| Winkel/Polar | grad, bogenmass; SIGNED/CLASSIC, getrennt/kombiniert, R1, Ursprung | Kern-Perioden-/Näherungstest; PolarkoordinatenKnotenTest und WinkelraumMethodenTest |
| Geometrie | koordinatenmenge, mittelpunkt, objektpunkt, geometrie; Anker/Zielpunkt/Verschiebung, historische Trägermenge | Integrationstest jedes Ziel; VektorUnterraumIntegrationTest |
| Methoden | methode, historische termZuMethode; nullstellig, strukturierte Bindungen, Außenquellen | ÜbersetzungsKnotenTest, Literalbedingungen nach Methodenbildung |
| Index | Standard, Achsen permutation/diagonale/hauptdiagonale/gegendiagonale/schnitt, matrixzerlegen | Integrationstest frühe Indexrückgaben und bestehende IndexDefinitions-Kartentests |
| Indexauswahl | nichtnullIndikator, rationaleMatrix, matrixQuelle, symbolischeMatrix, iterationsbereich, umformen, zyklus3, minoren, einheit, alternierend, permutation/minor mit Vorzeichen | Integrationstest frühe Indexrückgaben; jeweilige Variantenparameter im Zeugnis |
| Indexbausteine | materialisieren (tupel/zeile/spalte/matrix/tensor/vorbild), diagonalKomponenten, potenzQuotient/-Rest, kommutativerVektor, schnitte, polynomQuelle, spaltenSammeln, vektorisieren, partikulaereLoesung, monomIndex, alsMatrix, hermitesche/orthogonaleProjektoren, einheitSpalten, nullMatrix, konjugiertTransponiert, unteresDreieck, ausserdiagonal, nichtnullSpalten, vektorfeldKomponenten, jordanBedingungen, nichtnegativeDiagonale | Integrationstest erfolgreiche oder ausdrücklich nicht definierte Referenzen; erfolgreiche Ausgaben zusätzlich selbsttragender Codecroundtrip |
| Kandidaten | Q, R, U, Sigma, V, lambdas, Projektoren, Lambda, P, J | Integrationstest; vorhandene QR/SVD/Eigen/Jordan/Spektral-Kartentests |

Nicht nachgewiesene Indexabbildungseigenschaften bleiben offen. Negative Referenzen bestätigen die strukturierte Diagnose bei den jeweiligen unzulässigen Eingaben, nicht die Auswertbarkeit aller mathematischen Spezialfälle.
