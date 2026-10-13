# Inhaltsminimum und Indexdefinitionen
Status: Produkt und Abnahme abgeschlossen, v3.2.0, 2026-10-13

## Ziel und Nutzerwirkung
Vollständig lesbare Knoten/Gruppen, zuverlässige Auswahl, Indexübersetzung und ausführbare Strukturdefinitionen; verschachtelte Polarkoordinaten und zentriertes Falk-Schema.

## Nicht-Ziele
Keine zusätzliche Knotenart über die bereits auf master vorhandenen Typen richtungWinkel und halbesIntervall hinaus. Keine neue Produktionsabhängigkeit. Der beauftragte Releaseabschluss erfolgt als v3.2.0 nach erfolgreicher Abnahme.

## Untersuchter Istzustand
Die Ausgangslage und ältere Zwischenstände sind im Fortschrittsprotokoll festgehalten. Maßgeblicher integrierter Stand ist TODO 22b56645 mit master 7e11b711; Produktabschluss a97ce9f7, Release-PR #449. Persönlicher IDE-/Shelf-Bestand und separater Komplex-Worktree bleiben erhalten.

## Fachliche und mathematische Semantik
Indexmethode indiziert Tupel/Vektor mit einem, Matrix mit zwei und Tensor mit rangvielen 1-basierten Indizes. Verschachtelte Tupel bleiben verschachtelt. Polar ab R²: (r,(φ,θ,…)); R¹: (r,Vorzeichen). Bestehende Operatorverträge gelten für Produktreihenfolge und Kontraktionen.

## Daten-, Node-, Handle- und Edge-Vertrag
Kein neuer Art-Schlüssel; Übersetzungsziel indexmethode. Bestehender wert-Eingang und ergebnis-Ausgang (Methode), stabile Anschluss-IDs. Renderer meldet fachneutrales Inhaltsminimum; alle Geometrie folgt effektiver Größe. Gruppen umschließen Inhalte mit 24 dp Rand und Kopfbereich.

## Architekturentscheidungen
Mathematik im Kern, Graphauswertung im Adapter, Inhalte in MathematikKnoten, generische Größen-/Auswahlregeln im Editor. ADR folgt Umsetzung.

## Betroffene Dateien und Symbole
KnotenRenderer/KnotenKartenEditor/KartenEditorZustand/GruppenDarstellung; Methoden und Polarkoordinaten; ÜbersetzerKnoten; StrukturRechnerDefinitionsKarten; MatrixproduktFalkSchema.

## Meilensteine
- [x] Dreiseitige Integration und v3.2.0-y-Zuordnung; unveröffentlichte 3.1.2-Reservierung ersetzt, Komplexreservierung zurückgestellt.
- [x] Inhaltsminimum, Gruppenwahl, Kapselkontur, Falk-Zentrierung und Matrixzerlegung als Übersetzer mit Vektor-/Tupelausgabe.
- [x] Indexmethode, vollständiges ausführbares Rezeptinventar und Bedingungen für symbolische Kandidaten.
- [x] Wn-Parameter, explizite Polarkonvention, historische Aufrufpackung, Produkt-Sampling und persistierte Projektion.
- [x] Vollständige lokale Gradle-Tests, Builds, Lint, ADRs und unabhängiger Produktreview.
- [x] Android-Instrumentierung im isolierten API35-CI-Emulator erfolgreich; physische Geräteprüfung separat nicht verfügbar (nicht blockierend: Emulator-Laufzeitabnahme vorhanden).
- [x] PR-Abnahme #449 am Produkt-HEAD ae1669fc: sämtliche CI-Jobs grün, unabhängiger Produkt- und Releasecheck bestanden. Veröffentlichung erfolgt als freigegebener Squash mit Titel v3.2.0; maßgeblicher Veröffentlichungsstatus ist PR #449.

## Konkrete Umsetzungsschritte
Bestehende Methoden-/Strukturzugriffe verwenden; Definitionsgraphen zerlegen; Polar-Ausgabe verschachteln und alte Eingaben lesen; zentrale effektive Geometrie herstellen; Gestenregression isolieren; Layout zentrieren.

## Tests und Validierung
Gezielte Kern-/Knoten-/Graph-/Desktoptests, vollständiges Gradle test, app assembleDebug, Desktopkompilierung, Repository-/Releaseplan-/Versionsfolgeprüfung. Geräteabnahme nur bei verfügbarer Umgebung; Einschränkungen konkret berichten.

## Persistenz und Migration
Bestehende IDs und Verbindungen erhalten. Ziel als vorhandene Texteigenschaft speichern. Alte flache Polarwerte lesen. Inhaltsminimum ist abgeleitet, größere Benutzergrößen bleiben erhalten. JSON/Undo/Redo testen.

## Risiken und Rückfallstrategie
Vorhandene Änderungen nicht überschreiben. Größenmessung muss unbeschränkt und stabil sein; keine Mess-/Resize-Schleifen. Definitionsgraphen dürfen keine rekursive Selbstdefinition erzeugen. Fehler bleiben fachliche Diagnosen.

## Historisches Fortschrittsprotokoll

Die folgenden Einträge beschreiben den jeweiligen damaligen Zwischenstand. Aktuelle Abnahme und offene Punkte stehen in den Meilensteinen und im Abschlussabschnitt.
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


## Genehmigte Fortsetzung v3.2.0 (2026-10-13)

Arbeitsverzeichnis ausschließlich `/home/alex/.codex/worktrees/v320-winkelraum-abschluss/MathematikAtlas`, Branch `samai/v3.2.0/winkelraum-abschluss`. Ausgangspunkt origin/master 7e11b711; Integration des TODO-Stands 22b56645 als dreiseitiger Merge. Keine Änderungen in Originalcheckout oder weiteren Worktrees. IDE-Konfigurationen und Shelves werden ausschließlich hier auf HEAD zurückgesetzt. Neuere Winkel- und Diagonalmatrizenverträge auf master bleiben erhalten. Kein Push, Release oder Integration nach master vor Abnahme.

### Verbindliche Erweiterungen

- Strukturierter Winkelraum Wn = {−1,+1} × [0°,180°)^n; eine öffentliche strukturierte Methodenvariable statt unabsichtlicher Komponentenparameter. Optionale Strukturbindung in VariablenQuelle enthält gemeinsame Identität, Parametername, Gesamtdomain und Komponentenpfad. Laufzeitquellen werden aus persistiertem Zustand rekonstruiert. Unabhängige Skalare bleiben eigenständige Parameter.
- Komponenten-ASTs bleiben durch Sinus, Cosinus, Matrix-/Strukturoperationen substituierbar. Koordinatentupel bilden genau ein kanonisch umhülltes Ergebnis. Öffentliche Definitionsmenge entfernt ausschließlich die Argumentpackung, nicht Wn selbst. Historische Komponentenaufrufe werden nur bei eindeutiger Zuordnung mit stabilen Ports gepackt; ansonsten Migrationsdiagnose.
- Persistierte explizite Polarkonvention: neue Daten SIGNED; Migration eindeutig vorhandener Wn-Daten zu SIGNED, sonst CLASSIC. Keine nur aus beiläufigen Laufzeitmetadaten erratene Konvention. Vollständige kanonische Rückrichtung der Halbwinkelkonvention, Nullradius mit Vorzeichen +1 und Nullwinkeln. Numerische und symbolische Pfade teilen Dimensionsgrenzen und Semantik.
- Strukturierte Bildmenge und Sampling über alle Produktachsen einschließlich Vorzeichen. Endliche vollständige Intervalle statt kameraabhängiger Abschneidung; Gitter innerhalb Budget, sonst deterministisches Halton-Sampling mit Vorzeichenpaaren und ausdrücklich approximativer Kennzeichnung. Keine falschen Linien zwischen unterschiedlichen Vorzeichenzweigen.
- Für R4 und höher persistierte ausdrückliche 2D-/3D-Ausgangskoordinatenprojektion im Inspector nach vollständiger Auswertung. Mathematische volle Bildmenge bleibt unverändert.
- Verbleibende neun Indexrezepte werden ausführbar fertiggestellt. Eigen-/QR-/SVD-/Jordan-/Spektraldefinitionen sind ausführbare Kandidatenrelationen; keine Selbstoperatoren, Regelplatzhalter oder vorgetäuschten numerischen Solver.
- Bedingungen der Inverse und komponentenweisen Division einschließlich Quellannahmen in direkten sowie Definitionsgraph-Ausgaben erhalten.

### Reihenfolge und Abnahme

1. [x] ExecPlan aktualisiert; isolierte IDE-Dateien auf HEAD; 17 Mergekonflikte fachlich kombiniert. Kern/Knoten/App-Kompilierung erfolgreich (2026-10-13, /tmp/atlas-v320-integration-compile.log). Tests des kombinierten Stands folgen.
2. [ ] Wn-Quelle, Strukturbindung, Methodenpacking, Polarkonvention und Migration implementieren.
3. [ ] Symbolische/numerische gemeinsame Semantik, Bildmenge, Sampling und Projektion implementieren.
4. [ ] Neun offene Indexrezepte und Bedingungserhalt fertigstellen.
5. [ ] End-to-end-Tests Variable→Polar→Term→Methode→Parameter/Definitionsmenge→Bildmenge→Visualisierung einschließlich Definitionsraum durchführen.
6. [ ] Randfälle n=0..3/höher, beide Vorzeichen, Intervallgrenzen, Radius0, ungültige Werte, Namenskollisionen, Ellipsoid sowie JSON/IDs/Migration/Kopie/UndoRedo und CLASSIC-Kompatibilität prüfen.
7. [ ] Vollständige Tests, Lint, Android-Build, Desktopkompilierung, Compose-/Geräteprüfung und unabhängige Abnahme durchführen. Release-Metadaten übernimmt später master_verwalter.

Die bisherigen Testergebnisse gehören zum TODO-Ausgangsstand und ersetzen keine Prüfung des kombinierten v3.2.0-Stands.


## Vollständige genehmigte Ausführungsreferenz

# Genehmigter Plan v3.2.0 – Ausführungsreferenz

TODO22b56645 dreiseitig mit master7e11b711 integrieren; gemeinsame Basis ff020f06 (v3.1.1). Isolierter SamAI-Worktree, OriginalIDE/Shelves/fremdeWorktrees und /tmp/matlas-v320-komplex unverändert. Persönliche TODO-Shelves nicht übernehmen. Alle offenen Originalabnahmen schließen. Version3.2.0/3002000, y wegen mathematik.richtungWinkel und mathematik.halbesIntervall. Keine neue zusätzliche Knotenart nötig.

## Struktur und Polar
W_n={-1,+1}×[0°,180°)^n. Winkeltupelvariable erzeugt EINEN strukturierten Parameter w. VariablenQuelle optional gemeinsame Strukturidentität, Parametername, Gesamtwertebereich, Komponentenpfad; Runtime aus persistierter Konfiguration rekonstruieren. Komponenten bleiben substituierbare Kernausdrücke; stabile Herkunft, Pfade, Bereiche und Annahmen durch sin/cos/Matrix bewahren. Term→Methode gruppiert gemeinsame Struktur einmal und behält unabhängige Skalare, keine alphabetische Komponentenabflachung. Koordinatentupel EIN kanonisch gepacktes Ergebnis, explizite Mehrfachausgaben bleiben gesondert. Öffentliche Definitionsmenge für F(w) ist Wn, nicht internes Argumentpackungstupel.

Polar-Konvention PERSISTIERT im Inspector wählbar. Neue Übersetzer default Vorzeichen-Halbkreis. Historisch klar erkennbare Winkeltupel-Konstruktionen migrieren signiert, andere klassisch explizit. Keine spätere Laufzeitmetadaten-Heuristik. Alte flache Tupel lesbar, neu verschachtelt. u0=s; uk=(u(k-1)*cos(alpha_k),s*sin(alpha_k)); F=r*un. Symbolisch/numerisch identische Reihenfolge und Grad/Rad. Inverse gültige [0,180)-Winkel, Nullvektor kanonisch; R1 Radius+Vorzeichenfall erhalten. Dimensionlimits Variable/Richtung/Konverter vereinheitlichen. Negative Radius/Sign0/NaN/Inf/180 fachliche Diagnose. Quellannahmen erhalten.

## Methoden / Bildmenge / Sampling / Projektion
Strukturierte Argumente durch bestehende Methodenverträge. Historische Komponentenaufrufe bei eindeutiger Zuordnung explizit packen und PortIDs erhalten, sonst Migrationsdiagnose. Sampler: öffentliche Argumentzahl von kontinuierlichen Achsen trennen; Tupel als Produktdomänenwerte sampeln; beide Vorzeichen, ALLE Winkelachsen variieren; endliche Intervalle nicht Kamerafensterbeschneiden. Vollständiges Produktgitter innerhalb Budget, sonst deterministische Halton-Abtastung mit gepaarten Vorzeichen und Approximationskennzeichnung. Diskrete Zweige niemals künstlich Linien/Flächen verbinden. n0 beide Zeichen exakt. Höhere Dimensionen nicht durch feste Zusatzwinkel reduzieren.
R4+ Ergebnis erfordert persistierte Auswahl zwei/drei Ausgabekoordinaten im Inspector. VOLLSTÄNDIGES Ergebnis erst auswerten, dann projizieren. Mathematische Bildmenge E={D F(w)|w∈Wn} unabhängig Anzeige erhalten.

## Originalauftrag
Mindestgrößenvertrag fachneutral inkl Titel/Formel/Ergebnis/Ports/Controls/Footer/Padding, effektivesmax UserMinimum/Inhaltsminimum gleiche Geometrie Zeichnung/Ports/Edges/Hittest/Selection, größereUsergröße erhalten. Gruppen24dp+Kopf inklusive verschachtelt/leer/folded, Einzelklickwahl/Doppelklicköffnen, direkteInnenknotenwahl. Kapsel gleiche sichtbare/Hit/Selektionskontur. Falk bei Platz horizontalzentriert, überbreit komplettscrollbar mitZellwahl. Matrixzerlegen Übersetzer stattOperator mitZeilen/Spalten undVektor/Tupel-Ausgabe; IDs/Undo/JSON. LaTexEinsetzung keineüberflüssigenMatrixklammern; notwendigeKlammern mit left/right.
NeunfehlendeRezepte: VEKTORvektor.potenz,vektor.vektorfeldIntegral; MATRIXmatrix.minimalpolynom; TENSORmatrix.eigen,jordan,qr,spektral,svd,tensor.zerlegen. Reale Komponenten/Indexsummen/Faltungs/LGSgraphen ohneSelbstoperator/Regelplaceholder. SymbolischeZerlegungen durchausführbareIndexgleichungen+Kandidatenbedingungen, keinvorgegaukelterNumeriksolver. DirekteRechnerfähigkeiten nichtdurchDefinitionsmaterialisierungsbudgetbeschränken, GrenzenklarDiagnose. Inverse det!=0 und komponentenweiseDivisionNenner!=0 bisAusgangsannahmen inklQuellannahmen, direkteCore/Node/GraphPfade. Inventartestabschluss ERSTbeiLEERER Offenliste.

## Tests und Release
E2E Variable→Polar→Term→Methode→Parameter/Definitionsmenge→Bildmenge→Visualisierer einschließlichDF. n0,1,2,3+higher, beideVorzeichen, Winkelgrenzen, r0, invalid, Namenskollisionen w/w_1, numerischsymbolischgleich, Ellipsoidgleichung nichtsinguläresD. JSONRoundtrip/IDs/Migration/Kopieren/UndoRedo/Projektion/Budget/classiccompatibility. Regressionwahl Haupt+Gruppe nested/folded/resize/zoom/font/capsule; KonverterInspectorCrash; FalkCompose.
Gezielte+vollständigeGradleTests, Repository/Release/Versionsfolge/Core, AndroidDebug/Lint/Desktopcompile, Compose+AndroidGerät. Nichtausführbarkeit konkret ausweisen, unabhängig node_verifier final. ADR Inhaltminimum/Indexmethode/Strukturbindung ergänzen, ExecPlan nachweislich schließen. Manifest/Roadmap/Produktmetadaten zusammen3.2.0/3002000. Gegenaktuellenmasterintegrieren, PRgenaueineVersion, nachgrünenPrüfungenSquashmaster exaktTitel v3.2.0. LokaleSamAIcommits nurProjektskript+Verify, historischeCommitsnichtumschreiben.

## Releaseguard-Audit
Master hat keinen3.1.2Roadmapeintrag. Keinezusätzlichehistorische3.1.2Zeile einfügen (Guardblock), stattdessen einzige3.2.0Zeile reason benennt Ersetzung unveröffentlichter3.1.2TODOreservierung und Zurückstellung branchlokalerKomplex3.2.0Reservierung, derenCode/Worktreeunverändert. Basis letzterfinalerReleaseff020f06, Integrationsmaster7e11b711. Lokaleghkeyringtimeout, GitHubConnectorlesen funktioniert. ADB zurZeit KEINGERÄT; xvfb-run nichtPATH; JDK21 /home/alex/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2, SDK /home/alex/Android/Sdk, isoliert local.properties fehlt. InitScript /tmp/atlas-isolated-android-test.gradle erlaubtisolierteTestappinstallationohneProduktappzulöschen, niemalsproduktappuninstall.

2026-10-13: integrierte Desktop-UI-Abnahme 6 Editor- und 2 Falktests erfolgreich; vollständige Graphsuite 122 Tests nach Gruppenrahmenkorrektur erfolgreich. Größere Benutzerrahmen und leere Untergruppen bleiben erhalten. Logs /tmp/atlas-v320-ui-regression.log, /tmp/atlas-v320-gruppen-fix.log. Wn-Quellen-/Strukturbindungsimplementierung begonnen.


2026-10-13, Wn-Zwischencheckpoint: Neue Winkelquelle erzeugt ein intern quellidentisches strukturiertes Argument mit numerischen Komponenten-ASTs und optionaler StrukturQuellenBindung. Term→Methode benennt ganze Struktur capturefrei gemeinsam mit zusätzlichen Parameterquellen und Annahmen um; unabhängige w/w_1 bleiben erhalten. Neue Koordinatentupel sind eine kanonisch gepackte Ausgabe, historische fehlende Flags werden explizit auf Mehrfachausgabe migriert. Öffentliche Wn-Definitionsmenge entfernt nur die äußere Argumentpackung.

Polarkonvention ist explizit persistiert und im Inspector wählbar: neue Übersetzer SIGNED, historische fehlende Konvention CLASSIC außer eindeutig angeschlossener Winkeltupelvariable. Der konkrete Richtungsknoten liefert kartesische Einheitsvektoren und ist ausdrücklich kein Wn-Migrationsnachweis. Signierte inverse Umrechnung berücksichtigt gemischte Quadranten, Pole und Nullradius; GradWinkel bleiben konkrete Ausgaben. Wn-Methodenargumente normalisieren typisierte Winkel gezielt aufgrund des kanonischen Domainnachweises und prüfen danach Sign/Intervallgrenzen. Gemeinsame Grenze 64 Winkel wahrt vorhandene Variable-Konfigurationen.

Kern-/Knoten-/App-Kompilierung erfolgreich. Sieben neue gezielte Kern-/Knotentests erfolgreich (/tmp/atlas-v320-winkelraum-test.log): signierte Rundreisen bis R65, typisierte Winkel in List/Map/Tupelanwendung, ungültige Sign-/Grenzwerte, Variable→Polar→Methode→öffentlicheDefinitionsmenge für n0/1/2/3/64, unabhängige skalare Namenskollisionen, gemeinsame Zusatzquellen und historische explizite Konvention/Mehrfachausgaben.

Offen in diesem Meilenstein: historische Komponenten-Methodenaufrufe mit eindeutiger Portpackung und stabilen IDs bzw Migrationsdiagnose; vollgraphische JSON-/Kopie-/UndoRedo-Abnahme und Aktualisierung der alten Polar-/Variablen-Regressionen auf explizite Konventionen. Die vollständige Baseline enthielt zusätzlich bereits bekannte Diagonalmatrix-/Reshape-/Inventar- und Bedingungstransportfehler. Der Gesamtplan bleibt offen. Als nächstes übernimmt Root sequentiell Sampling/Projektion, der Implementierer plant die verbleibenden Rezepte und historische Aufrufpackung nur lesend.

## Verifikation der strukturierten Bildmenge am 2026-10-13

Root ergänzte einen Packungsbaum für Produktdomänen, budgetierte Produktgitter und deterministische Halton-Abtastung mit gepaarten diskreten Zweigen. Projektion speichert zwei oder drei 1-basierte Ausgabekoordinaten und folgt erst auf die vollständige Auswertung, auch bei benannten Methodenausgaben. Der Inspector verändert die vorhandenen Knoteneigenschaften über Kartenaktionen. Ein E2E-Ellipsoidtest fand fehlende Variablenquellen am Strukturrechnerausgang; diese werden einschließlich reeller Metadaten nun weitergereicht.

30 gezielte Knotentests ohne Fehler: WinkelraumSamplingTest (11), MethodenVisualisierungTest und VisualisierungsDimensionsAbleitungTest; Log /tmp/atlas-v320-sampling-test.log. Der echte Kartengraph Variable → Polar → Term → Methode → Definitionsmenge → Bildmenge → Visualisierer bleibt nach JSON-Roundtrip und Kopieren ausführbar. Weitere Tests prüfen W₀, beide Vorzeichen, alle sechs Kugelachsen, DF-Ellipsoidgleichung, R⁴-Projektionspflicht, Halton bis R⁶⁵, alle acht inneren Winkelachsen, Budget, Undo/Redo und Abbruch. Zusätzlich Graphsuite 122 und acht Desktop-Compose-Regressionen grün. Diese gezielten Nachweise ersetzen weder den abschließenden vollständigen Testlauf noch Android-Gerätetests.

Android-Laufzeitumgebung: kein physisches Gerät verbunden. Vier isolierte Emulatorstarts (API36, Softwaregrafikvarianten einschließlich Qt-Startpfad) endeten im nativen qemu-Prozess mit SIGSEGV/Exit139, bevor ein Gerät online war. Logs /tmp/atlas-v320-emulator*.log. Keine installierte Produktanwendung verändert. Zusätzliche direkte Kern-Skriptprüfung weiterhin wegen fehlendem kotlinc auf PATH nicht ausführbar; Gradle-Kernprüfung erforderlich.

2026-10-13, Rezepte: 28 IndexRechnerDefinitionsKartenTest erfolgreich (/tmp/atlas-v320-index-recipes-test.log), darunter vollständige Inventarliste ohne offene Rezepte, Potenzen 0–4, Minimalpolynome von Identität/Jordanblock/Nullmatrix, komponentenweise skalare Integrale sowie gültige und ungültige Kandidaten für QR/SVD/Eigen/Jordan/Spektralzerlegung. Tensor-Parametersubstitution und Freiparameterermittlung ergänzt, damit Projektorbedingungen tatsächlich gebunden werden. Ganzzahlige Potenzen bewahren den rationalen Wertebereich. Vollsuite noch in Reparaturprüfung; keine Releasefreigabe behauptet.

## Abschlussprüfung des integrierten Produktstands

2026-10-13: Vollständiges `./gradlew test` erfolgreich (Log `/tmp/atlas-v320-tests-final.log`); nach dem zusätzlichen Annahmen-Regressionsfall erneut `test` erfolgreich (`/tmp/atlas-v320-abnahme.log`). Android-Debug-Build, LintDebug und Desktop-Kompilierung erfolgreich (`/tmp/atlas-v320-build-final.log`). Sechs Editor-Compose- und zwei Falk-Compose-Regressionen erneut erfolgreich im Abschlusslauf. Historische Aufrufpackung geprüft: eindeutige Namen erhalten IDs/Verbindungen und werden ausführbar gepackt; uneindeutige Namen führen zur Diagnose. Inverse und Division bewahren Quellannahmen und neue Definitionsbedingungen. Releaseplan, Versionsfolge, Repositoryprüfung und 14 Releaseguard-Tests erfolgreich. Unabhängiger read-only Produktreview durch node_verifier ohne weitere konkrete Blocker.

Die frühere Offenliste der neun Definitionsrezepte ist geschlossen. Die historischen Hinweise auf offene Wn-Implementierung und Rezeptinventar oben dokumentieren Zwischenstände und sind durch diese Nachweise ersetzt. Die Metadaten sind gemeinsam auf 3.2.0/3002000 vorbereitet; Veröffentlichung wird erst nach grüner PR-CI behauptet.

Noch offener Abnahmepunkt: Android-Instrumentierung. Auch isolierte API35- und API37-Emulatoren beenden ihren nativen Prozess vor betriebsbereitem ADB. Es wurde kein Gerätetest als erfolgreich verbucht, keine Produkt-App deinstalliert und kein fremder AVD geändert. Ein eigener CI-Job startet einen isolierten Emulator und führt die vorhandenen Editor- und Inspector-Instrumentierungstests aus. Ein physisches Android-Gerät ist nicht verbunden. Direkter kotlinc-Smoke-Test lokal mangels CLI nicht ausführbar; Android-freie Gradle-Kernprüfung ist erfolgreich, zusätzliche direkte Prüfung läuft in bestehender CI.

Zusätzlicher Tensorzerlegungsnachweis: 29 Definitionskartentests grün, alle Achsen eines 2×3×2-Tensors einschließlich jeder Scheibe und Indexgrenzen gegen direkte Auswertung geprüft. Dabei wurde der statische Methodenanschluss für die Schnittfamilie berichtigt. Die Emulator-CI benötigt neben absolutem SDK-Pfad auch eine ausdrücklich angelegte AVD-INI im isolierten AVD-Verzeichnis; beide Startverträge sind jetzt im Job festgelegt.

## Endzustand und Abschlussnachweise

Die Implementierung und Abnahme sind abgeschlossen. Nach dem letzten Schnittfamilienfix sind 1.640 lokale Tests, Android-Debug-Build, Lint und Desktop-Kompilierung erfolgreich (`/tmp/atlas-v320-final-after-slices.log`). 29 Definitionskartentests einschließlich jeder Scheibe aller Achsen und der Indexgrenzen eines 2×3×2-Tensors sind grün. Node-Verifier hat den abschließenden Produktdiff unabhängig freigegeben; master_verwalter bestätigt die echte PR-Kontextprüfung, Basis und Identität.

PR #449, Produkt-HEAD ae1669fc7736d55434c050ec1b9d901562c7ad79: Release-Guard 38400143187, Linux-Desktop 38400142830, Android-Build 38400142891 sowie Mathematikkern/Android-Runtime 38400142819 sämtlich erfolgreich. Android-Laufzeitjob 115305356166 führte die vorhandenen Editor- und Inspector-Instrumentierungstests auf atlas-ci/API35 erfolgreich aus. Die direkte kotlinc-Kernprüfung ist ebenfalls in CI erfolgreich. Lokale Emulator-SIGSEGVs sind damit keine offene Abnahmeblockade; ein physisches Gerät bleibt nicht verfügbar und wird nicht als geprüft ausgegeben.

Der abschließende Dokumentationscommit verändert keine Produktdateien. Release-Metadaten, ADR und Prüfbelege beschreiben denselben freigegebenen Stand. Persönliche IDE-/Shelf-Dateien und der separate Komplex-Worktree sind unverändert. Der finale Squash nach master wird ausschließlich nach erneut grünen Checks des tatsächlichen PR-HEAD durchgeführt.
