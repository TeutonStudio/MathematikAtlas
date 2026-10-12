# Inhaltsminimum und Indexdefinitionen
Status: aktiv, 2026-10-12

## Ziel und Nutzerwirkung
Vollständig lesbare Knoten/Gruppen, zuverlässige Auswahl, Indexübersetzung und ausführbare Strukturdefinitionen; verschachtelte Polarkoordinaten und zentriertes Falk-Schema.

## Nicht-Ziele
Keine neue registrierte Knotenart, keine Produktionsabhängigkeit, kein Release/Merge.

## Untersuchter Istzustand
Vorhandener Übersetzer atlas.uebersetzer mit stabilen Ports und Zielen. Polarkoordinaten.polar erzeugt flache Tupel. StrukturRechnerDefinitionsKarten enthält Regelknoten. Editor verwendet gespeicherte Größe für sämtliche Geometrie. Umfangreicher uncommitteter Bestand wird erhalten.

## Fachliche und mathematische Semantik
Indexmethode indiziert Tupel/Vektor mit einem, Matrix mit zwei und Tensor mit rangvielen 1-basierten Indizes. Verschachtelte Tupel bleiben verschachtelt. Polar ab R²: (r,(φ,θ,…)); R¹: (r,Vorzeichen). Bestehende Operatorverträge gelten für Produktreihenfolge und Kontraktionen.

## Daten-, Node-, Handle- und Edge-Vertrag
Kein neuer Art-Schlüssel; Übersetzungsziel indexmethode. Bestehender wert-Eingang und ergebnis-Ausgang (Methode), stabile Anschluss-IDs. Renderer meldet fachneutrales Inhaltsminimum; alle Geometrie folgt effektiver Größe. Gruppen umschließen Inhalte mit 24 dp Rand und Kopfbereich.

## Architekturentscheidungen
Mathematik im Kern, Graphauswertung im Adapter, Inhalte in MathematikKnoten, generische Größen-/Auswahlregeln im Editor. ADR folgt Umsetzung.

## Betroffene Dateien und Symbole
KnotenRenderer/KnotenKartenEditor/KartenEditorZustand/GruppenDarstellung; Methoden und Polarkoordinaten; ÜbersetzerKnoten; StrukturRechnerDefinitionsKarten; MatrixproduktFalkSchema.

## Meilensteine
- [x] Versions-/Branchzuordnung: bestehende 3.1.2-x-Reservierung erweitert, Branch unverändert auf ff020f06. Releaseplan/Versionsfolge grün.
- [ ] Indexmethode und ausführbare Definitionskarten
- [x] Koordinatenkompatibilität und Absturzregression: Kern-/Knotenprüfungen einschließlich leerem Tupel und produktiver Inspector-Gerätetest erfolgreich.
- [x] Inhaltsminimum, Auswahl und Falk-Dialog: drei Geometriemodelltests sowie fünf Editor- und zwei Falk-Compose-Tests erfolgreich. Reviewbefunde korrigiert; unabhängige UI-Prüfung ohne weitere konkrete Blocker.
- [ ] Tests, ADR, unabhängige Abnahme

## Konkrete Umsetzungsschritte
Bestehende Methoden-/Strukturzugriffe verwenden; Definitionsgraphen zerlegen; Polar-Ausgabe verschachteln und alte Eingaben lesen; zentrale effektive Geometrie herstellen; Gestenregression isolieren; Layout zentrieren.

## Tests und Validierung
Gezielte Kern-/Knoten-/Graph-/Desktoptests, vollständiges Gradle test, app assembleDebug, Desktopkompilierung, Repository-/Releaseplan-/Versionsfolgeprüfung. Geräteabnahme nur bei verfügbarer Umgebung; Einschränkungen konkret berichten.

## Persistenz und Migration
Bestehende IDs und Verbindungen erhalten. Ziel als vorhandene Texteigenschaft speichern. Alte flache Polarwerte lesen. Inhaltsminimum ist abgeleitet, größere Benutzergrößen bleiben erhalten. JSON/Undo/Redo testen.

## Risiken und Rückfallstrategie
Vorhandene Änderungen nicht überschreiben. Größenmessung muss unbeschränkt und stabil sein; keine Mess-/Resize-Schleifen. Definitionsgraphen dürfen keine rekursive Selbstdefinition erzeugen. Fehler bleiben fachliche Diagnosen.

## Fortschritt
Editorgrößen und Gruppengeometrie implementiert. Konkrete Auswahlregression reproduziert: Klick auf Gruppentitel ergibt null, weil Verbindungscanvas darüber liegt. Nach Ebenenkorrektur und Titel-Tap/DoubleTap grün. Freie Inhaltsmessung zentral in Projektion eingebunden, Benutzergrößen bleiben gespeichert; Größenaktionen begrenzen am Minimum. Kapsel-Ecken werden als Treffer abgewiesen. Falk-Zentrierung durch tatsächliche Compose-Bounds bestätigt.

Prüfungen: JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2 ./gradlew :KnotenKartenVerwalter:testDebugUnitTest :desktopApp:test --tests '*EditorAuswahlRegressionTest' --tests '*RechnerSchemaDesktopTest' erfolgreich. Nach Erweiterung um Kapsel und Zentrierung Desktoptests erneut erfolgreich. Gradle benötigt Zugriff auf vorhandenen Benutzer-Cache außerhalb der Sandbox; genehmigte Eskalation verwendet.

## Entscheidungsprotokoll
2026-10-12: Nutzer bestätigt Erweiterung vorhandenen Übersetzers, alle Struktur-Operatoren, Winkelwerte im inneren Tupel, Gruppen mit Puffer und R¹-Sonderfall.

## Abweichungen vom ursprünglichen Plan
Unabhängige Review verlangt explizite Gruppenkopfskalierung, natürlich gemessene Fußzeile innerhalb des Mindestmaßes und dynamische Größenverträge für Visualisierungsrenderer. Diese Korrekturen gehören zum bestätigten Ziel. Der bisherige Außenfußzeilenvertrag wird deshalb bewusst angepasst; ADR 0053 dokumentiert die Änderung.

## Ergebnis und Verifikation
Mathematikimplementierung läuft. Editor/Falk gezielt geprüft. Nach Review sieben Desktop-Compose-Tests grün: Langtitel bei Zoom 0.5/Schrift 2, lange Fußzeile in Kapsel, produktive Geometrievisualisierung mit Diagnose, übrige Auswahl-/Geometrie-/Falktests. Android-MathematikKnoten compileDebugKotlin erfolgreich. Der Android-Visualisierungsrenderer ist im Desktop-Shadowmodul ausdrücklich ausgeschlossen; Laufzeittest nutzt produktiven plattformübergreifenden Geometriepfad, Androidrenderer kompiliert erfolgreich.

Vollständige Tests/Build und unabhängige Abnahme stehen noch aus. Kein Commit, kein Release.

Zwischenstand Mathematik: Matrixaddition und Matrixprodukt als echte Graphen einschließlich statischer Graphprüfung erfolgreich; weitere Grundoperationen und Achsentransformationen in Arbeit. Offene Operatorrezepte sind ausdrücklich noch keine Planerfüllung. Der konkrete leere-Tupel-Absturz in der Polar-Anschlussableitung (negative Listenlänge) wurde gefunden und wird regressionsgeprüft.

Umgebung: /home/alex/Android/Sdk/platform-tools/adb devices -l erkennt ein SM_X920 (R52Y3042QNJ). Geräteabnahme steht noch aus. python3 scripts/pruefe_kern.py endet mit Code 2, weil kotlinc nicht auf PATH vorhanden ist; Kernprüfungen über Gradle bleiben erforderlich. git diff --check erfolgreich.

Geräte-/Zoomnachprüfung: sechs Editor-Compose-Tests erfolgreich, einschließlich Gruppenverschiebung bei Zoom 0.5 und 2 (Displaydelta wird genau einmal in Graphkoordinaten umgerechnet). Zwei Instrumentierungstests in EditorGeraeteRegressionTest auf SM-X920/Android 16 erfolgreich: Gruppenkopf, enthaltener Knoten und gefalteter Block auswählbar; Kapsel-Ecke kein Treffer, Zentrum Treffer. Befehl: :desktopApp:test --tests '*EditorAuswahlRegressionTest' :KnotenKartenVerwalter:connectedDebugAndroidTest, Log /tmp/atlas-ui-device-test.log. Unabhängige UI-Prüfung meldet nach Korrekturen keine weiteren konkreten Blocker.

Produktiver Übersetzer-Inspector auf SM-X920/Android16 erfolgreich geprüft: Umschalten polar→kartesisch→indexmethode, verschachtelte Polarausgabe, stabile Anschluss-IDs und Undo. Regulärer app-Gerätetest konnte vorhandene anderssignierte Produktinstallation nicht überschreiben (INSTALL_FAILED_UPDATE_INCOMPATIBLE); kein Uninstall dieser Installation. Mit temporärem Gradle-Init-Skript /tmp/atlas-isolated-android-test.gradle wurde allein für den Testlauf die getrennte App-ID de.TeutonStudio.MathematikAtlas.instrumentierung gesetzt; eine tatsächlich ausgeführte Instrumentierung, null Fehler. Log /tmp/atlas-inspector-isolated-device-test.log. Keine persistente Änderung der App-ID. Zusätzliches Entfernen der isolierten Test-App meldete DELETE_FAILED_INTERNAL_ERROR; Paketauflistung für Benutzer 0 zeigt sie nicht, die allgemeine Auflistung ist wegen Benutzer 150 eingeschränkt. Bereinigung ist daher nicht ausdrücklich bestätigt.

Reproduzierbare isolierte Geräteprüfung: temporäres Groovy-Init-Skript mit `allprojects { pluginManager.withPlugin('com.android.application') { androidComponents.finalizeDsl { androidDsl -> androidDsl.defaultConfig.applicationId = 'de.TeutonStudio.MathematikAtlas.instrumentierung' } } }` anlegen, dann `./gradlew -I <init-skript> :app:connectedDebugAndroidTest --console=plain`. Testergebnis-XML ausdrücklich kontrollieren: dieser Gradle-Runner kann bei APK-Installationsfehlern trotzdem Exit 0 melden. Erfolgreiches XML: UebersetzerInspectorGeraeteTest, tests=1, failures=0, errors=0, skipped=0.

Mathematik-Zwischenprüfung: vollständige Tasks :MathematikRechenSystem:test :MathematikKnoten:testDebugUnitTest :app:testDebugUnitTest erfolgreich (Implementiererlauf88065). Sieben neue Definitionsgraphprüfungen umfassen JSON, Verbindungstypen, komplexes Skalarprodukt, L1/L∞-Distanz, Projektion, Leibnizdeterminante und Inverse. Leeres Tupel ist als Fehlerdiagnose der Auswertungspipeline regressionsgeprüft. Weitere Operatorrezepte bleiben in Arbeit, daher noch keine Abschlussabnahme.

Weitere Graphprüfung: zehn IndexRechnerDefinitionsKartenTest-Fälle grün mit tatsächlicher originaler Registerauswertung und rekursivem Typ-/Form-/Numerikvergleich (Log /tmp/index-direct-tests.log). Diagonalen wurden auf Tupel-Ergebnis und rechteckige min(m,n)-Semantik korrigiert. Symbolische Inverse erhält im produktiven Rechner bedingte Leibniz-/Kofaktorauswertung bis Dimension 7; 1×1[a] stimmt ohne Vergleichsausnahme überein. Hadamarddivision erweitert vorhandene Strukturrechner; Q-Anhebung und Nennerbedingungen sind unabhängig geprüft. Restliche Operatorgruppen bleiben offen.

Zertifizierter Skalarproduktgraph: Zeugnisextraktion und echter Methodenaufruf mit Originaloperanden, keine Standardproduktsubstitution. Gewichtete 1D-Regression Faktor 2 liefert in Graph und produktiver Auswertung 12. Elf bestehende Graphentests erfolgreich. Gemeinsame Frischebasis für symbolische Quellnamen implementiert, zusätzliche Capture-Regressionen in Prüfung. Numerische inverseRational bleibt oberhalb Dimension 6 unverändert nutzbar; nur neue symbolische Kofaktormaterialisierung und Definitionsauswahl haben das explizite Budget.

Nutzerergänzung: Matrixzerlegung ist Übersetzerziel matrixzerlegen statt sichtbarem Rechneroperator; Zeilen/Spalten wahlweise als orientierte Vektoren oder Tupel. Historischer Operator bleibt zur API-/Dateikompatibilität auflösbar, ist aus sichtbarer Registry und Inspectorwahl entfernt. Laden/Speichern migriert nach atlas.uebersetzer mit stabilen Knoten-/Anschluss-IDs. LaTeX-Einsetzung verwendet vorhandene Matrix-/Vektor-/Tupelklammern direkt und ansonsten left/right. Neue Kern-/Knotenregressionen und vollständige MathematikKnoten-/App-Tests sowie Falk-Desktoptests grün (/tmp/atlas-matrix-translator-tests.log). Operatorinventar nun 18 offene Rezepte.

Steering-Abnahme: 17 gezielte Matrixzerlegungs-/Polar-/Schema-Tests grün; 14 App-Graph-/Inventartests grün. Zusätzliche Capture-Regression des Zerlegers für Quelle i und Tupelausgabe erfolgreich. Interne Indexrollen haben separate lesbare LaTeX-Namen. Android-Inspector erneut mit isolierter App-ID erfolgreich (eine tatsächlich ausgeführte Instrumentierung, failures/errors/skipped=0), einschließlich Matrix zerlegen→Spalten→Tupel, drei Tupelausgänge und Undo (/tmp/atlas-matrix-inspector-device-test.log).

Bestandsbefund zu Eigen-/QR-/SVD-/Jordan-/Spektralzerlegung: Registry führt sie SYMBOLISCH_IMPLEMENTIERT; produktiver Auswerter erzeugt lediglich TensorOperation-ASTs und rollenbezogene LaTeX-Ausgaben, keinen numerischen Faktorisierungssolver. Die passenden Indexdefinitionen werden deshalb als implizite, ausführbare Gleichungs-/Bedingungsgraphen für Kandidaten spezifiziert. Abnahme über bekannte gültige und ungültige Faktorisierungen; numerische Eindeutigkeit oder kanonische Basis wird nicht vorausgesetzt. Diese notwendige Verifikation unterscheidet sich von direkten Zahlenvergleichen konkret implementierter Operatoren.

Matrixpotenz als echter generischer Faltungsgraph erfolgreich gegen direkte Auswertung geprüft: 0,1,2,3,−1,−2, singulär hoch0=I und singulär negativ als Fehler. Inversenzweig bekommt bei nichtnegativem Exponenten vor Materialisierung eine harmlose Quelle; 7×7I hoch0/1/2 regressionsgeprüft. Gebundene Strukturquelle bleibt bis zur abschließenden Methodenbindung symbolisch, nichtgewählte Annahmen bleiben ausgeschlossen. 16 App-Graph-/Inventartests inzwischen grün, zusätzlich kartesische Koordinaten und Polarwinkel R¹/R²/R³ als echte Graphen. Inventar: noch15 offene Rezepte; vollständige Abnahme bleibt ausstehend.

19 Graph-/Inventartests erfolgreich: Zusammenführen aller Ausgabearten mit exakt einer Abflachebene, symbolischer Vektorzerleger mit Indexmethode und konkreter Zerleger mit drei benannten Ausgängen (einschließlich verschachteltem Tupel/symbolischem x). JSON, Verbindungstypen und direkte Auswertung prüfen jetzt sämtliche Ausgänge. Symbolische Indexquellen/Strukturabstraktionen übernehmen Q/R/C/H aus Quellmetadaten statt pauschalem C; zugehörige Signaturregression erfolgreich. Negative Matrixpotenzen behalten den rationalen Bestandsvertrag und prüfen Originalquelle nur im gewählten Negativzweig; positive H- und Großmatrixfälle bleiben ausführbar.

Rangdefinition implementiert: pure Familien quadratischer Minoren, auf gleiche Dimension mit Einheit gepaddet, Komponentenprodukt/Permutationssumme, exakter Nichtnullindikator als ZahlFallAusdruck, Gradgewichtung und Maximum. Kein Rangoperatoraufruf im Graph; 21 App-Graph-/Inventartests grün (Rang0/1/voll/rechteckig im direkten Vergleich). Zusätzliche Grenz-/Symbolikprüfung und unabhängiger Review stehen aus. Determinante inferiert jetzt gezielt Quellbereiche und lehnt nichtkommutative H-Kofaktorformeln auch direkt ab. Inventar noch11 offene Rezepte.

Rang-Randfälle ergänzt: echter Tensor2×3 über reine 2D-Struktur→Matrix-Konversion und produktiven Tensor-Rangpfad, 2×10Rang1/2, rationale Nichtnullzahl1/10^100, symbolische Diagnose sowie5×5Definitionsbudget gegenüber weiterhin erfolgreichem Direktrang5. Minorpadding optimiert aufmin(m,n), kombinatorisches Budget perBigInteger vorMaterialisierung. 21 App-Graph-/Inventartests grün. WeitereTensor-2D-Permutationspfade werden auf echteTensorquellen geprüft; Inventar weiterhin11 offene Rezepte.
