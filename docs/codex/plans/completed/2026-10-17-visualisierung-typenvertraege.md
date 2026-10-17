# Einheitliche numerische Typinterpretation der Visualisierung

Status: abgeschlossen mit dokumentierter Android-Laufzeitgrenze, 2026-10-17.

## Ziel und Nutzerwirkung
Alle im Typaudit gefundenen Darstellungsprobleme beheben: kompakte Potenzen,
Tupel, orientierte Vektoren, Matrizen, Tensoren, verschachtelte Produkte,
komplexe Argumente, diskrete Zahlenmengen und explizite Projektionen.
Nicht räumliche Typen erhalten eine verbindliche, handlungsorientierte Diagnose.

## Nicht-Ziele
Keine neuen Knotentypen, Produktionsabhängigkeiten oder automatische Abbildung
abstrakter Funktionen, Mengen oder Folgen auf willkürliche Zahlen.

## Untersuchter Istzustand
`VisualisierungsSampler`, `StrukturierteBildmengen`, `KoordinatenAdapter` und
`VisualisierungsDimensionsAbleitung` besitzen unterschiedliche Typfalllisten.
Die temporäre Untersuchung von 31 Mengen, sieben Methodendomänen und zehn
Elementformen reproduziert die Unterschiede. R² scheitert, R×R funktioniert.
Der vorhandene Arbeitsstand enthält zusätzlich die beauftragten Pie-, Methoden-
und Matrizenmengenänderungen. Diese und fremde IDE-Änderungen bleiben erhalten.

## Fachliche und mathematische Semantik
Ein gemeinsamer numerischer Raum beschreibt geordnete reelle Blattkoordinaten
und die Rückverpackung zum ursprünglichen Wert. Komplexe Skalare verwenden Re/Im,
Matrizen Zeilenreihenfolge, Tensoren die bestehende Speicherreihenfolge.
Verschachtelung, Vektororientierung und Matrix-/Tensorform bleiben für die
Methodenanwendung erhalten. Projektionen erfolgen erst am vollständigen Ergebnis.
Diskrete Grundmengen werden im Fenster vollständig und budgetiert aufgezählt.
Primzahlentscheidungen kommen aus dem Kern; fehlende Nachweise bleiben offen.
Winkel und Restklassen benötigen eine explizite Vertreterabbildung; rohe Mengen
werden mit dieser Erklärung abgelehnt. Funktionen-, Folgen- und Potenzmengen
benötigen eine explizite numerische Abbildung. Symbolische Dimensionen bleiben
bedingt; der nulldimensionale Raum enthält (), ist aber kein 1D/2D/3D-Raum.

## Daten-, Node-, Handle- und Edge-Vertrag
Keine Änderungen an persistierten Karten, Anschluss-IDs oder registrierten Typen.
Vorhandene Ausgabekoordinaten konfigurieren die Projektion. Kein neuer UI-Zustand.

## Architekturentscheidungen
Numerische Rauminterpretation liegt plattformneutral unter der vorhandenen
Visualisierung im Modul MathematikKnoten. Mathematische Mitgliedschaft wird an
den Kern delegiert. Keine mathematischen Regeln im neutralen Editor.

## Betroffene Dateien und Symbole
Neue gemeinsame Rauminterpretation unter visualisierung/koordinaten;
VisualisierungsSampler.normalisiere/normalisiereFilter, StrukturierteBildmengen,
KoordinatenAdapter.extrahiere, VisualisierungsDimensionsAbleitung; zugehörige Tests.

## Meilensteine
[x] Gemeinsame Raumstruktur, Dimensionen und Koordinatenprojektion; Typmatrix und Projektionsregressionen.
[x] Direkte Mengen, Filter, Mitgliedschaft und diskrete Domänen vereinheitlicht; diskrete Wahr-Filter und gaußsche Primzahlen geprüft.
[x] Strukturierte Bildmengen und verständliche Typdiagnosen vervollständigt; Identitätsbilder erhalten Argumentformen.
[x] Regression der gelieferten Kartenstruktur; R² und R×R liefern identische Fläche, Rand und Statistik.
[x] Repository-, JVM- und Desktopprüfungen, Androidbuild und Lint sowie unabhängige Prüfung; finaler Android-Laufzeitnachweis wegen Gerätesperre nicht verfügbar.

## Konkrete Umsetzungsschritte
1. Gemeinsame strukturtreue Zerlegung/Rückverpackung mit Größenbegrenzung.
2. Raumgeometrie in Normalisierung und Filterbedingungen verwenden.
3. Diskrete Fensterpunkte ohne kontinuierliche Rasterverluste verarbeiten.
4. Bildmengen mit denselben Strukturen aufbauen, Projektionen vervollständigen.
5. Abstrakte/symbolische/0D-Fälle ausdrücklich und konsistent diagnostizieren.
6. Fachliche Regressionen und alle passenden Projektprüfungen ausführen.

## Tests und Validierung
Gleiche Punktmengen und Mitgliedschaften für äquivalente R²-Darstellungen;
Originalpredicate x₁<x₂²; Orientierung/Form bei Methodenanwendung; komplexe
Eingänge; diskrete Wahr-Filter; Projektionsreihenfolge und Grenzen; Budget,
symbolische und 0D-Räume sowie abstrakte Typen. Bestehende Visualisierungstests,
gesamte JVM-Suite, Repository/Kern/Releaseplan und Plattformbuilds.

## Persistenz und Migration
Keine Schemaänderung. Bestehende Matrix-/Tensorwerte und gespeicherte
Projektionsindizes werden weiterverwendet und durch Codec-Regression geprüft.

## Risiken und Rückfallstrategie
Keine stillschweigende Formkonvertierung oder Gleichsetzung von Quotienten mit
reellen Zahlen. Hohe Dimensionen und große diskrete Produkte werden vor der
Materialisierung budgetiert. Bei offenen Dimensionen kein falscher Ersatzwert.

## Fortschritt
2026-10-17: Istzustand und temporäre Auditresultate geprüft. Gemeinsame Raumstruktur,
Samplingpfade, diskrete Aufzählung und Projektionen implementiert. Unabhängige
Prüfung fand einen fehlenden Schutz verschachtelter Zusatzbedingungen im
Zellnachweis sowie einen unvollständigen Dimensionschipwechsel. Beide behoben,
mit Regressionen versehen und erneut unabhängig abgenommen. Zusätzlich werden
Einzelprojektionen nun in Eingabeprüfung und Codec erhalten; notwendige
Raum-/Achseneinstellungen ändern sich atomar. Ein ursprünglicher UI-Testvergleich
mit der gesamten Konfiguration wurde auf Projektionserhalt präzisiert, weil der
bestehende Codec absichtlich Standardbereiche ergänzt und Samplingwerte begrenzt.

## Entscheidungsprotokoll
2026-10-17: Für Quotienten und abstrakte Räume explizite Abbildung statt eines
willkürlichen Standardbilds. Damit bleibt mathematische Identität erhalten.

## Abweichungen vom ursprünglichen Plan
Im Abschlussaudit wurden Mengenoperationen zusätzlich kompositionell abgesichert:
`(Z \ {0}) × R`, Vereinigungen gemischter Achsen sowie eingeschränkte Matrixräume
und verschachtelte Matrixfaktoren. Sie sind Ausprägungen derselben Typ- und
Rasterlücken und gehören deshalb zum Reparaturumfang. Sichere Trägerachsen
liefern nur Kandidaten; die Originalbedingungen bleiben maßgeblich.

## Ergebnis und Verifikation
Unabhängige Codeabnahme durch `typen_verifier`: keine verbleibenden Findings.
Die aktuelle vollständige JVM-Suite umfasst 2.313 Tests, 0 Fehler, 1 bestehenden
Skip (437 XML-Berichte). Repository-/Architekturprüfung, Standardkarten,
Releaseplan und Versionsfolge sind erfolgreich. Keine neuen registrierten
Knotentypen, keine Produktionsabhängigkeit und keine Schemaänderung.

Die zusätzliche CLI-Kernprüfung `python3 scripts/pruefe_kern.py` endet mit Code 2:
`kotlinc` fehlt in der Umgebung; die Kernprüfungen über Gradle sind erfolgreich.
Emulatorstarts endeten auch ohne GPU/Beschleunigung mit Exit 139. Deshalb wurden
die Android-Darstellungs- und Bedienprüfungen auf dem physischen Samsung SM-X920
(R52Y3042QNJ) mit einer temporären separaten Application-ID ausgeführt. Die
vorhandene Installation bleibt erhalten; Testpakete werden anschließend entfernt.
Der vorherige Gerätelauf bestand mit zwei Tests (`/tmp/atlas-types-android-final.log`).
Der erneute Lauf am finalen Produktionsstand mit zwei zusätzlichen bestehenden
Übernahme-/Abbruchtests schlug vor erreichbarer Compose-Oberfläche fehl. Diagnose:
Tablet zunächst `mWakefulness=Dozing`; nach Aufwecken liegt `mCurrentFocus=Bouncer`
und `mDreamingLockscreen=true` vor. Die geschützte Sperre verhindert daher den
finalen Android-Laufzeitnachweis. Vier Fehler „No compose hierarchies found“ sind
kein bestandener Test. Protokolle `/tmp/atlas-types-device-final.log` und
`/tmp/atlas-types-device-awake-final.log`. Beide Testpakete wurden entfernt und
die vorhandene Originalinstallation anschließend nachgewiesen. Kein Sperrschutz
wurde umgangen. Das ist eine verbleibende Umgebungsgrenze; aktuelle Android-
Darstellung/Undo/Abbruch können am finalen Stand nicht als laufzeitgeprüft gelten.

Arbeitsbranch `samai/v3.4.12/pie-methoden-matrizen`, Basis/HEAD `20356f1c`.
Kein neuer Commit, Push oder Release. HEAD stammt von Alexander/GitHub; es wurde
kein neuer SamAI-Commit erzeugt. Release-Metadaten bleiben auf der veröffentlichten
3.4.11 gemäß bestehender Branchregel. Fremde und vorherige Änderungen bleiben erhalten.


## Abnahmematrix

| Anforderung aus dem Typaudit | Konkreter Nachweis |
| --- | --- |
| Kompakte Potenz statt R×R | KompakterRaumKartenRegressionTest: vollständiger Graph, Codec, gleiche Mitgliedschaft, Fläche, Rand und Statistik; enthaltene und ausgeschlossene Zellen |
| Tupel-, Vektor-, Matrix-, Tensorräume direkt und gefiltert | VisualisierungsTypVertragTest.aequivalenteZweidimensionaleRaeumeWerdenDirektUndGefiltertGleichGesampelt |
| Verschachtelte Produkte und Argumentformen | verschachtelteProdukteErhaltenAlleDreiAchsen, packungErhaeltFormOrientierungUndKomplexeKomponenten |
| N0, Z, N, Primzahlen, gaußsche Zahlen/Primzahlen | diskreteWahrFilterVerlierenKeineFensterpunkte; Vergleich gegen exakte Kernmitgliedschaft |
| Gemischte diskrete Achsen, Mengenalgebra und eingeschränkte Matrizen | gemischteDiskreteAchsenBleibenAuchImFilterVollstaendig, mengenalgebraBewahrtDiskretePunkteUndGemischteAchsen, eingeschraenkteMatrixraeumeErhaltenFormMitgliedschaftUndBildpunkte |
| Komplexe/strukturierte Methodendomänen | bildmengenAkzeptierenAlleStrukturiertenDefinitionsraeume, identitaetsBildmengenNutzenTatsaechlicheKomplexeMatrixUndTensorArgumente |
| Matrix-, Tensor-, Quaternionprojektion | matrixTensorUndQuaternionProjektionenVerwendenDieAusgewaehlteReihenfolge |
| Projektionen mit festen Schnitten; Erhalt beim Laden | filterProjektionenVerwendenFesteSchnitteStattUnbelegterKoordinaten, projektionsEingabeSpeicherungUndDarstellungsraumBleibenKonsistent |
| Endliche Vektoren und zusammengesetzte komplexe Ausdrücke | endlicheVektorenLeitenIhreDimensionAb, zusammengesetzteKomplexeAusdrueckeLeitenDieTatsaechlicheDimensionAb, endlicheKomplexeBildmengenErhaltenAuchReelleElementeUndEndlicheArgumente |
| Eindeutige Grenzen für Quotienten/abstrakte Räume, symbolische/0D-Dimensionen | nichtRaeumlicheTypenErhaltenHandlungsorientierteDiagnosen, symbolischeUndNullDimensionenWerdenNichtAlsLeereFlaecheAusgegeben |
| Keine falschen Zusatzbedingungs- oder Primzahlbeweise; Budgets | verschachtelteZusatzbedingungenWerdenNichtAlsVollerRaumZertifiziert, offenePrimzahlNachweiseSindBedingtStattNichtUnterstuetzt, grosseRaeumeUndDiskreteFensterWerdenVorMaterialisierungBegrenzt |
| Atomare gültige Raumwechsel | raumwechselErgaenzenAchsenUndEntfernenUnpassendeProjektionenAtomar; Android-/Desktop-Bedienungstest |
| Darstellung bei kleinem Fenster, beiden Themes, Schriftfaktor 2 | VisualisierungsTypenGeraeteTest und VisualisierungsTypenDesktopTest mit Pixelprüfung für sechs Raumformen |

Finaler vollständiger Lauf:

```text
./gradlew test :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :desktopApp:createDistributable --no-parallel
BUILD SUCCESSFUL in 3m 33s; 252 Tasks.
2.313 Tests, 0 Failures, 0 Errors, 1 bestehender Skip.
```

Protokoll `/tmp/atlas-types-abnahme.log`. Die Desktop-Laufzeittests prüfen
beide Themes, 360×280, Schriftfaktor 2, sechs Raumformen per Pixelnachweis
sowie die Projektionsbedienung. Android-Test-APK kompiliert; Lint erfolgreich.

Die 14 negativen Release-Guard-Tests bestehen ebenfalls. Abschlussdiff mit
`git diff --check` geprüft. Vorhandene fremde IDE-Dateien und frühere
Pie-/Methoden-/Matrizenänderungen bleiben unangetastet.

Normale APKs nach Geräteprüfung wiederhergestellt: `:app:assembleDebug
:app:assembleDebugAndroidTest --no-parallel` erfolgreich, Application-IDs
`de.TeutonStudio.MathematikAtlas` und `.test` in den Metadaten bestätigt.
Repositoryprüfung und `git diff --check` auch nach Dokumentationsabschluss grün.
