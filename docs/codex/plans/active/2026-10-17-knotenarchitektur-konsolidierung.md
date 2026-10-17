# Knotenarchitektur konsolidieren, mathematische Verträge erhalten

## 1. Titel und Status

2026-10-17: Vom Nutzer beauftragter Gesamtplan M0–M7 in Umsetzung.
Ausgang: master 9fea31ddbaaef8bd26fa7a2730177eaaa9744909, v3.4.1.
M0: veröffentlicht als v3.4.2, Commit 5813c0337b5c7b2284d882b785559dfeea4ae3cd.
M1: veröffentlicht als v3.4.3, Commit e665a7fde78fe845dc7c62fa60fa00896d35f58a.
M2: integriert als v3.4.4, ec152625926698f58e1ddd1d1a1d6420f8b0c6cc;
ursprüngliche Android-Schlussabnahme fehlgeschlagen, durch enges Test-Reparaturrelease
v3.4.5/x abgeschlossen: e6760b9a41aaeb186dee7ee7c13254430cb47ee8, alle Master-Gates grün.
M3: veröffentlicht als v3.4.6/x, 43726159a87efb50f1adcc5fe7cdafa981a2edd2; alle finalen Master-Gates grün.
M4: v3.4.7 veröffentlicht, finaler Master 69d4e70a; PR- und Masterabnahme grün.
M5: v3.4.8 veröffentlicht, finaler Master fb1984f9; PR- und Masterabnahme grün.
M6: v3.4.9 veröffentlicht, finaler Master 49945974; PR- und Masterabnahme grün.
M7a: v3.4.10 veröffentlicht, finaler Master f2d06d30; PR- und Masterabnahme grün.
M7b: v3.4.11/x auf samai/v3.4.11/konsolidierung-m7b in Umsetzung, Basis f2d06d30; Produkt-/Releaseabnahme und Gesamtabschluss offen.
Jeder Meilenstein wird getrennt vollständig geprüft und veröffentlicht, bevor
sein Nachfolger reserviert wird. Keine vorab gestapelten Versionsbranches.

## 2. Ziel und Nutzerwirkung

Ein maßgeblicher Fachalgorithmus je Operation; 15 unnötige Spezialtypen durch
vorhandene Rechner-/Übersetzermodi ersetzen und acht auf Android erneut
angebotene historische Geometrieprädikate aus der Erzeugung entfernen.
Alle sinnvollen Operationen bleiben auffindbar und direkt vorkonfiguriert erzeugbar.
Ziel bei unverändertem Restbestand: 121 gemeinsame mathematische Typen und
123 vollständige Androidtypen einschließlich Notiz und Gruppentyp.

## 3. Nicht-Ziele

Keine neuen Typ-Schlüssel, Parallelregistry, Produktionsabhängigkeiten,
Desktop-Shadowmodulablösung oder Umsetzung offener Prädikatindexfunktionen.
Quellen Variable/allgemeiner Parameter/Wahr/Falsch sowie polymorphes
Transponieren, Tupelprojektion und Konkatenation bleiben erhalten.
Keine Gesamtfreigabe der offenen 77-Punkte-Matrix ohne Einzelnachweise.

## 4. Untersuchter Istzustand

129 gemeinsame Typen; Android ergänzt Mengenräume und Geometrie erneut und
besitzt 146 Manifesttypen. Sieben gültige Mengen-/Raumtypen fehlen gemeinsam;
acht bereits konsolidierte Geometrierelationen werden erneut erzeugbar.
Katalogtests bestanden trotz Abweichung, weil Kern/Desktop nur Enthaltensein
prüften. Androidmanifesttest prüft bereits Gleichheit.
43 gespeicherte MethodenAltaufrufe in 40 JSON-Dateien, vier Ordnungsrelationen,
fünf Einzelmengen, drei Abbildungsmengen, eine Matrixdiagonale nachgewiesen.
Private Nutzerkarten wurden nicht untersucht.
Historische Zahlen-/Mengen-/Vektor-/Aussagenoperationen und matrix.zerlegen
sind bereits konsolidiert; nicht erneut als neue Arbeit zählen.

## 5. Fachliche und mathematische Semantik

Skalar, Einertupel, verschachteltes Tupel, Zeile, Spalte, Matrix und Tensor
bleiben unterscheidbar. F((a,b),g) hat zwei logische Argumente.
Prädikat: eine boolesche Ausgabe oder ein boolesches Einertupel.
Offene Voraussetzungen bleiben erhalten; widerlegte verhindern Anwendung.
Reelle Norm nicht ungeprüft auf komplexe/nichtkommutative Bereiche übertragen.
Matrix-Nebendiagonale bleibt rechts oben verankert, auch rechteckig.
Abbildungsmenge ist nicht Bildmenge; Lösungsdefinition ist nicht Auflösen;
Integralobjekt ist nicht integrierte Methode; geometrische Felder sind keine
gewöhnlichen Rechnerwerte. Quellenrollen bleiben verschieden.

## 6. Daten-, Knoten-, Anschluss- und Verbindungsvertrag

Eindeutige Migration erhält Karten-/Knoten-/Anschluss-/Verbindungs-IDs,
fachliche Reihenfolge, Kardinalität, semantische IDs, Nutzername, Stil,
Gruppenmitgliedschaft, feste Kartenverweise und alle Joinbeiträge.
Rollenwechsel remappt Standardwerte, eingangsKartenVerweise und semantische
Referenzen gemeinsam. UI-Projektionen verändern keine logische Stelligkeit.
Mehrdeutige Originaldaten bleiben erhalten/exportierbar; Auswertung wird mit
konkreter Migrationsdiagnose gesperrt. Kein stiller Ersatzoperator.

## 7. Architekturentscheidungen

KanonischerMathematikKnotenKatalog ist einzige mathematische Erzeugungsquelle.
Plattformen ergänzen nur eigene Werkzeuge und dynamische Gruppen.
Vorhandene RechnerOperatorRegistry, Fachregister, OperatorKnotenSuchindex,
ÜbersetzungsDienst und geordnete StandardMathematikAuswerterPakete verwenden.
Mathematische Aufrufe binden Identitäten, partielle bindeIdentitäten;
allgemeine Methoden nur über vorhandene Capabilities, keine künstliche Mathematik.
Konzepte bleiben unabhängig von Implementierungstypen.

## 8. Betroffene Dateien und Symbole

M0: katalog/KanonischerMathematikKnotenKatalog, ErzeugbarerKnotenKatalog,
ReleaseKnotenKatalogTest/ReleaseAppKnotenKatalogTest/ReleaseDesktopKnotenKatalogTest,
PraedikatKnotenMigration, RechnerEinzeloperationMigration, GesamterMathematikAuswerter,
MathematikKartenCodec und Adapter-Auswertungsgrenzen; release/knotentypen.json.
M1: MethodenAufrufAuswerter, FaltungsAuswerter, KanonischeMethodenanwendung,
MethodenFundament, MethodenAufrufSynchronisierung, Analysis-/Prädikatpfade.
M2: MatrixdiagonaleKnoten, SpurKnoten, MatrixRechner, VektorRechner,
LineareAlgebra, VektorOrientierungsKnotenV2300, TransponierenAuswerter,
RechnerOperatorDefinitionen und StrukturFormelRechner.
M3: ÜbersetzerKnoten, ÜbersetzungsVertrag/-Dienst/-Codec/-ZeugnisAdapter,
Suchindex und VektorStrukturMigration. M4: MengenraumKnoten,
MengenOperatorKnoten, EndlicheMengeKonfiguration/-Auswerter und Inspector.
M5/M6: VariablenStruktur, WertebereichKonfiguration, Quellenregistrierungen,
Darstellungs-/Maß-/Raum-/Cauchy-/Richtungsvorlagen und Auswerter.
Alle: Konzeptbibliothek, gespeicherte Konzept-/Standardkarten, Tests und Docs.

## 9. Meilensteine

- [x] M0 Katalog und Migrationsschutz: 136 gemeinsame/138 vollständige Arten; v3.4.2 veröffentlicht, 5813c0337b5c7b2284d882b785559dfeea4ae3cd, PR #456 und alle PR-/master-Gates grün.
- [x] M1 Methodenaufrufe/Prädikate: v3.4.3 veröffentlicht, e665a7fde78fe845dc7c62fa60fa00896d35f58a, PR #457, alle PR-/master-Gates grün; 132 gemeinsame/134 Androidtypen.
- [x] M2 Matrix/Vektor/Tensor: v3.4.4 integriert; Schlussabnahme über separat veröffentlichte Testreparatur v3.4.5/e6760b9a vollständig grün, ursprünglicher Laufzeitfehler historisch erhalten.
- [x] M3 Tupel/Übersetzer: v3.4.6/43726159 veröffentlicht, PR #460; 124 gemeinsame/126 Androidarten, alle PR-/Master-Gates grün, PR-Geräteabnahme 62/62.
- [x] M4 Mengen: v3.4.7 veröffentlicht, 69d4e70a4f1bdbf69b23f0ee708f5893d5232947, PR #461; drei Spezialarten konsolidiert, 121 gemeinsame/123 Androidtypen, alle sechs jüngsten PR-Gates und vier finalen Master-Workflows grün.
- [x] M5 Quellenverträge und Konstanten ohne Typentfernung: v3.4.8 veröffentlicht, fb1984f9f83aa4e586b4e6bf22cb524ab0d9cb5a, PR #462; Katalog unverändert 121/123, alle sechs PR-Gates und vier finalen Master-Workflows grün.
- [x] M6 Begriffe und Verantwortlichkeiten: v3.4.9 veröffentlicht, 49945974dbe96778b70341f29a32bd96747d18d8, PR #463; Katalog unverändert 121/123, alle sechs PR-Gates und vier finalen Master-Workflows grün.
- [x] M7a Migrationsschutz, historische Rollen und Zeichenbarriere: v3.4.10 veröffentlicht, f2d06d3087a0f27bbe58fad81187e8c6ac0c3501, PR #464; alle sechs finalen PR-Gates und vier Master-Workflows grün.
- [-] M7b Alt-Auswerterbereinigung, Kartenkanonisierung und Gesamtabschlussprüfung: v3.4.11/x direkt vom vollständig geprüften v3.4.10-Master f2d06d30 auf samai/v3.4.11/konsolidierung-m7b reserviert; Produktarbeit und lokale Gesamtprüfung abgeschlossen; Veröffentlichung an die finalen SHA-Gates gebunden.

## 10. Konkrete Umsetzungsschritte

M0: Sieben Mengen-/Raumtypen zentral aufnehmen, Androidergänzungen entfernen.
Exakte Kataloggleichheit prüfen, acht Geometrierelationen nur über Prädikat
anbieten. Mehrdeutige Prädikat-/Rechnermigration sicher ablehnen.
M1: ID-basierte Bindung, metadataerhaltende Projektion und strukturierten
symbolischen Aufruf schließen. MethodenAnwendung auf methodeAufrufen,
ordnungsrelation nach relation.kleiner/kleinerGleich/groesser/groesserGleich,
orientierte Analysis auf methodenDifferentieren/Integrieren migrieren.
Einzelresultatprojektion ausdrücklich direkt; tupelwertige Einzelresultate erhalten.
M2: spur→matrix.spur (spur→wert); Diagonalmodi vollständig in vorhandene
Matrixoperatoren integrieren; Radius→vektor.norm (wert→norm), Kreuzprodukt
(a/b→links/rechts) mit Orientierungsvertrag. Kernformeln bündeln; Einheitsvektor
und Transponieren behalten und delegieren. Ungesicherte Normdomänen ablehnen.
M3: ABFLACHEN als Übersetzerziel freigeben; eine Ebene/rekursiv erhalten.
TupelZuSpalte: tupel/vektor→wert/ergebnis; Spalte/ZeileZuTupel:
vektor/tupel→wert/ergebnis; Abflachen nach Richtung remappen. Zeugnisse und
Rückführung erhalten. Unaufgerufene pauschale Vektorstrukturmigration entfernen.
M4: Potenzmenge grundmenge→a; Abbildungsmenge definitionsmenge→argumentmenge,
zielmenge/menge erhalten. EndlicheMenge um ausdrücklichen Graphmodus mit
1/n geordneten Elementeingängen erweitern; Einzelmenge dorthin migrieren.
Unverbundenes Pflichtelement ist Fehler, Tupel/Menge bleibt einzelnes Element.
M5: Typ-/Bereichsableitung gemeinsam, Parameter-/Variablenrollen verschieden.
Unbekannte Bereiche ablehnen, Namen keine Identität. Wahr/Falsch Quellen erhalten.
M6: mengenNorm→sichtbar Maß einer Menge; vektorraum→Tensorträger erster Stufe;
darstellungsoptimierung→LaTeX-Darstellung, registrierter metadataerhaltender
Durchreichpfad. Cauchy präzisieren, numerische Richtung als angenähert kennzeichnen.
M7: Verbraucherinventur über Code/JSON/Methodenquellen/UI/Installer/Tests;
Altalgorithmen löschen, eindeutige Migrationen und diagnostische Datenbewahrung
behalten. Kein historischer Mathematikfallback bei gesperrten Knoten.

## 11. Tests und Validierung

Pro Migration Alt-/Neu-Fixtures: Werte/Form, Bedingungen/Status, Räume/Typen,
Identitäten/Reihenfolge/Kardinalität, Zeugnisse/Genauigkeit, Graphreferenzen.
Altfehler bekommen explizite korrigierte Erwartung statt falscher Äquivalenz.
0/1/n, verschachtelte und höhere Methodenwerte, ID-Umordnung, Projektion,
partielle Bindung, falsche Typen, fehlende/überzählige Argumente prüfen.
Differenzenquotient Q(f,x,h): f=t²,x=3,h=2→8; h=0 ablehnen; symbolisch h≠0.
M2: rechteckige Diagonalen/Erzeugung/Ausgabeformen, Kreuzproduktbasis,
Normdomänen, Tensorpermutation. M3: leere/verschachtelte Tupel, Rückführung.
M4: leere/symbolische Mengen und Einzelmenge strukturierter Werte.
Alle: Suchbarkeit, Standardkarten, Gruppieren/Kopieren/Undo/Redo/Laden.

Pflichtbefehle je Release-Head:

```sh
python3 scripts/pruefe_repository.py
python3 scripts/pruefe_standardkarten.py
python3 scripts/pruefe_releaseplan.py
python3 scripts/pruefe_versionsfolge.py
python3 scripts/test_release_guards.py
python3 scripts/pruefe_kern.py
git diff --check
./gradlew --no-parallel test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :KnotenKartenVerwalter:assembleDebugAndroidTest :desktopApp:compileKotlin
./gradlew :KnotenKartenVerwalterDesktop:test :MathematikKartenAdapterDesktop:test :MathematikKnotenDesktop:test :desktopApp:test --stacktrace
./gradlew :desktopApp:packageRpm --stacktrace
./gradlew :KnotenKartenVerwalter:connectedDebugAndroidTest :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notClass=de.TeutonStudio.MathematikAtlas.VisualisierungsLeistungsTest --stacktrace
```

Linux-RPM installieren, zweimal mit gleichem XDG_DATA_HOME starten,
Persistenz und Deinstallation gemäß desktop-linux.yml prüfen.
Fehlende Toolchain/Geräte konkret als offen dokumentieren; Build ist kein Laufzeittest.

## 12. Persistenz und Migration

Format10 bleibt, solange bestehende Eigenschaften/Parameter genügen.
Mathematik-Wire akzeptiert weiter nur aktuelle Version; unabhängig davon
historische Graphkarten migrieren. Alle Codecphasen lese/lade/importiere/
vorSpeichern und eingebettete Methodenquellen testen. Idempotenz sowie
Speichern→Laden sichern; historische Revisionen nicht überschreiben.

## 13. Risiken und Rückfallstrategie

Hauptgefahren: verlorene Ports/Quellenmetadaten, implizite Tupelabflachung,
Parameterrollenwechsel, ungesicherte Norm, stille Ersatzwerte, Registryüberschreibung.
Originalrevisionen erhalten, mehrdeutige Knoten sperren, betroffene Änderung
gezielt korrigieren; keine Releasehistorie oder fremden IDE-Änderungen überschreiben.

## 14. Fortschritt

Analyse: acht gezielte Katalogtests, Repository-/Architektur-/Methodenmodell-/
Shadowprüfung, 15 Standardkarten, Releaseplan/Versionsfolge und 14 Guards grün.
Diese historischen Nachweise sind keine Freigabe der neuen Implementierung.
Neue Ergebnisse werden hier dem geprüften Commit zugeordnet.

M0-Nachlauf: 2.125 JVM-/Desktop-Testfälle, keine Fehler, ein vorhandener
Opt-in-Messfall übersprungen; elf neue Katalog-/Migrations-/Dispatchregressionen.
Unabhängige Nachprüfung nach den Pipeline-, Typ-, Rollen- und Dispatchkorrekturen
ohne offene Findings. Debug-/Test-APKs und Desktopkompilierung erfolgreich.
Repository-/Standardkarten-/Releaseplan-/Versionsfolge-/14Guard-/Diffprüfungen grün.
Android-Gerät SM-X920: sieben Editortests bestanden; Appprüfung konnte wegen
INSTALL_FAILED_UPDATE_INCOMPATIBLE (andere installierte Signatur) nicht starten.
Bestehende App/Daten wurden bewahrt. Gradle meldete dennoch Gesamt-Erfolg;
dies wird ausdrücklich nicht als bestandene App-Laufzeitprüfung gewertet.
Standalone-Kern lokal mangels kotlinc nicht ausführbar; RPM lokal wegen fehlendem
rpmbuild/unsupported rpm nicht erzeugbar. PR456 CI erbrachte am ersten Head
aed308cacc1cfd3d10fa04877ea82adde061a553 die Kern-/Emulator-/RPM-Installations-/
Neustartpersistenz-/Deinstallationsnachweise. Nachfolgende Rollen-/Dispatchhärtung
benötigt erneute CI am finalen Head; Veröffentlichung bis dahin gesperrt.
Android-Lint und endgültiger Gesamtprozessabschluss grün: kompletter obiger
Gradle-Test-/Lint-/APK-/Desktopbefehl ohne lokale Geräte-/RPM-Tasks erfolgreich
in 2m49s, 277 Tasks. Quellstand: aed308ca plus die abschließende explizite
Rollen-/Dispatchhärtung; der nachfolgende SamAI-Commit umfasst exakt diesen
geprüften Produktbaum. M0 bleibt bis grüner CI dieses Commits unveröffentlicht.

Abschließender Desktopbefund: Die bisherige Namens-/Kategoriesuche bot keine
Operatorvorlagen an. Der Einfügepfad verwendet jetzt zusätzlich den bestehenden
OperatorKnotenSuchindex und dedupliziert anhand der kanonischen Variantenkennung.
Zwei Regressionen sichern konfigurierte Geometrierelationen, Matrixspur und
unveränderten leeren Suchkatalog. Gezielte Katalogtests und vollständiger
Desktop-Test-/Kompilierlauf bestanden (37s); unabhängige Quellprüfung ohne
Findings. Produktbaum: 06120b57 plus dieser Desktop-Suchpatch.
Erneute CI aller Releasegates am nachfolgenden Commit bleibt verpflichtend.

## 15. Entscheidungsprotokoll

2026-10-17 Nutzer bestätigt: mehrdeutige Altknoten ausdrücklich ablehnen,
Originaldaten bewahren; nachgewiesene Mathematikfehler diagnostisch korrigieren;
Quellen erhalten; separate vollständig abgenommene Releases.
Alternative Legacy-Mathematik und gemeinsame Großveröffentlichung verworfen.
master_verwalter ordnet alte Drafts #433/#435/#436 als historische 2.33.0-Arbeit
außerhalb aktueller Integration ein; keine Inhalte pauschal übernehmen/PRs schließen.

## 16. Abweichungen vom ursprünglichen Plan

Basis v3.4.1 statt v3.4.0; zusätzliche Androidkatalogabweichung.
Weitere Abweichungen nur mit konkreter Evidenz und fachlicher Begründung.

## 17. Ergebnis und Verifikation

Offen. Erst abgeschlossen bei 121/123 konsistenten Typen, allen migrierten
Verbrauchern, keinem unabhängigen historischen Rechenfallback, vollständigen
Pflicht-/Laufzeitnachweisen und unabhängiger Abnahme.

M0-Veröffentlichung: PR #456, finaler GitHub-Squash 5813c0337b5c7b2284d882b785559dfeea4ae3cd
(v3.4.2), Autor Alexander/Committer GitHub web-flow; lokale Arbeitscommits SamAI/SamAI.
Finaler PR-Head 0d511f9d288893d2fd1cd90ce5e13406be47b416 vollständig grün.
Master-Nachweise: [Guard](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38978826218),
[Linux/RPM/Persistenz](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38978826212),
[Android-Build](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38978826208),
[Kern/Emulator](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38978826204).

M1-Bestand: 43 ausführbare Altaufrufe in 40 JSON-Dateien; 16 weitere knotenArt-
Beschriftungen gehören zu konzept.regel und sind keine ausführbaren Quellen.
Die vier gespeicherten Ordnungsrelationen verwenden sämtlich relation=kleiner;
Tests aller vier erlaubten Werte werden ergänzt. Portnamen bleiben bei bloßer
Signaturumordnung ebenso stabil wie IDs, damit lokale Quellen erhalten bleiben.

M1-Zustandsmatrix Analysis: allgemeine Analysis ohne Ergebnisanforderung folgt der
Orientierung der Eingabemethode und bietet keine wirkungslose Orientierungsauswahl.
Neue Vektorsuchvarianten deklarieren den vorhandenen semantischen Ergebnisvertrag;
Spalte/Zeile ist dort eine atomare Ausgangseinstellung über den KonfigurationsPlan.
Historische explizite Ergebnisanforderungen bleiben erhalten; bloße frühere
Orientierungsparameter führen nicht nachträglich eine Transposition oder neue
Auswertungsbedingung ein. Unbekannte gespeicherte Orientierungen werden angezeigt.
Der frühere Wrapper wählte tatsächlich identische Auswerter; dies ist die konkrete
Begründung für das Ausblenden einer fachlich wirkungslosen Auswahl.

M1-Wahrheitstabellen: bestehender AtlasDialog/Arbeitsdialog mit Schließen-, Escape-
und Außenklickpfad bleibt zuständig. Vergleich: vorhandener Nichtprädikat-Fehlerzustand
im selben Eingabebaustein und KartenExportFormatDialog/AtlasDialog-Rahmen. Fehlende
symbolische Mathematikfähigkeit erhält sichtbare Diagnose statt Legacyzugriff;
keine Kartenänderung oder neue Dialogaktion. Layout-/Laufzeitnachweis noch offen.

M1-Umsetzung: vier physische Spezialarten aus der Erzeugung entfernt; gemeinsame
132/Android 134 Arten. Auch die Definitionsvorlagen sind kanonisiert, historische
Konzept- und Karten-IDs bleiben auflösbar. 43 echte Altaufrufe aus 40 unveränderten
JSON-Dateien werden in allen vier Codecphasen verglichen; eingebettete Quellen
werden durch dieselbe Pipeline verarbeitet. ID-Bindung, offene mathematische
Capabilities, strukturierter symbolischer AST, direkte/äußere Tupelprojektion und
Metadatenherkunft sind durch neue Vertragsfälle abgesichert. Differenzenquotient
läuft durch Altkarte, Laden, Synchronisieren, Speichern und Import: 8 bei x=3/h=2,
Diagnose bei h=0, offene Voraussetzung h≠0 bei symbolischem h.

Unabhängige M1-Prüfung fand einen zusätzlichen Synchronisierungsverlust: unveränderte
Port-ID genügt nicht, wenn ihr Vertrag inkompatibel überschrieben wird. Feste
historische Ergebnisverträge und belegte Argumentverträge werden deshalb vor
Übernahme geprüft; Originalanschlüsse und Kanten bleiben bei Ablehnung erhalten.
ADR: decisions/2026-10-17-identitaetsbasierte-methodenaufrufe.md. Die 77-Punkte-Matrix
enthält ergänzende Evidenz, keine pauschale Erledigungserklärung.

M1-Nachprüfung der Synchronisierung: feste historische Aufrufe bewahren auch die
physischen Portarten/-verträge. Tatsächliche Eingangswerte und ihre Metadaten werden
gegen die neue Signatur geprüft; eine breite historische Objektart wird nicht mit
einem tatsächlich falschen Wert verwechselt. Positive Standard-Altaufrufregression
und negative Ergebnis-/Argumentvertragsfälle bestehen. Unabhängige read-only
Nachprüfung ohne weitere Findings. Vollständige Testresultate aktuell: 2.154 Fälle,
0 Fehler, 1 vorhandener Skip. Lint-/Buildprozess und Release-CI noch abschließen.
Lokale standalone-Kernprüfung weiterhin nicht ausführbar: kotlinc fehlt; Gradle-
Kernprüfung besteht. App-Gerätetest wegen vorhandener anders signierter Installation
nicht lokal ausführbar, deren Daten bleiben erhalten. RPM-Laufzeitabnahme erfolgt
über bestehende isolierte CI wie M0; lokales rpmbuild fehlt.

M1 lokaler Schlusslauf: vollständiger angegebener Gradle-Test-/Lint-/APK-/Test-APK-
und Desktop-Kompilierbefehl erfolgreich in 3m21s (277 Tasks). 2.154 Testfälle,
0 Fehler, 1 vorhandener Skip. Repository-/Standardkarten-/Releaseplan-/Versionsfolge-
und 14 Guardregressionen sowie Diffprüfung bestehen. Unabhängige Nachprüfung ohne
Findings. Frühere Fehlversuche (zu strenge Legacy-Portprüfung und überlappende
Desktop-Gradle-Läufe) sind durch diesen sequenziellen finalen Lauf ersetzt.
Freigabe zur Erstellung des eigenständigen Release-PRs v3.4.3; Veröffentlichung
weiter an alle Gates am finalen PR-Head und nachfolgenden master-SHA gebunden.

M1-Abnahmeergänzung vor Veröffentlichung: Ein gezielt reproduzierter Randfall
zeigte, dass ein symbolischer Eingang mit explizitem reellem Wertevorrat ohne
zusätzliche reelleVariablen-Map konservativ als komplex abgeleitet wurde. Die
Synchronisierung berücksichtigt nun Quellenmetadaten und den ausdrücklich
deklarierten Bereich einer direkten Variable. Konkrete Werte werden weiter anhand
ihres Objekts typisiert. Erweiterte Standard-Altaufrufregression für Zahl und reelles
Symbol sowie alle 14 Migrations-/Differenzenquotientfälle bestehen. Unabhängige
Zusatzprüfung ohne Findings. PR #457 erhält dafür einen neuen Head; Nachweise des
vorangegangenen f90a14b9 gelten nicht als CI-Freigabe des aktualisierten Heads.
Zusatz-Vollprüfung ebenfalls erfolgreich: 3m36s, 277 Tasks, alle Test-/Lint-/APK-
und Desktop-Kompilieraufgaben grün. Unabhängige Zusatzprüfung ohne Findings.
Der aktualisierte Produktbaum ist zur Commit-/PR-Aktualisierung freigegeben;
Veröffentlichung erst nach CI am dadurch erzeugten endgültigen SHA.


M1-Veröffentlichung: PR #457, endgültiger PR-Head a936f79cc7c1fd06be92acc4c21220c37da11e14,
finaler GitHub-Squash e665a7fde78fe845dc7c62fa60fa00896d35f58a / v3.4.3.
Squashautor Alexander, Committer GitHub/web-flow; lokale Commits SamAI/SamAI.
Alle PR-Gates grün; Master-Nachweise:
[Guard](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38985690143),
[Linux/RPM/Persistenz](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38985690140),
[Android](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38985690144),
[Kern/Emulator](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38985690135).

M2 begonnen nach vollständiger M1-Abnahme. Reservierung 15b0ff06717bfb048f9c1c34cd809f75d37e68f7,
nur Roadmap, SamAI/SamAI verifiziert. Untersuchung: Ein gespeicherter tatsächlicher
Matrixdiagonalenknoten; Spur, Radius und Kreuzprodukt haben Code-/Konzeptverbraucher.
Methodenhebung band bisher nach Namen statt Bindungs-ID und verlor eigene
Methodenvoraussetzungen. Korrektur und drei gezielte Hebungstests bestehen; mehrere
logische Ergebnisse werden ausdrücklich nicht als Einzeloperand gehoben.
Gemeinsame reale Norm, Kreuzproduktkomponenten und Basisvektorerzeugung sind in Arbeit.
Nichtreelle Normfälle werden ausdrücklich diagnostiziert; die vorhandene konjugierte
Tensor-Norm bleibt ein getrennter Vertrag. Transposition unterscheidet weiterhin
Vektororientierung und Tensorachsenpermutation.


M2-Zwischenstand: 128 mathematische Typen im gemeinsamen Katalogtest;
Spur/Matrixdiagonale/Radius/Kreuzprodukt werden als konfigurierte Rechner erzeugt.
Definitions- und Konzeptvorlagen sind kanonisiert; historische Wissens-IDs bleiben.
Gemeinsame Inspectoroptionen verwenden den bestehenden KonfigurationsPlan.
Die Migration prüft explizite Rollen/Typen/Kardinalitäten und remappt Defaults,
Kartenfallbacks sowie typisierte Rollenreferenzen bei unveränderten Instanz-IDs.
Lesen, Laden, Importieren, Vor-Speichern und erneutes Laden werden getrennt geprüft.
Unabhängig gefundener Metadatenverlust durch spätere Vektorrechner-Normalisierung
ist behoben und über alle Codecphasen regressionsgesichert.

Mathematische Nachprüfungen: Methodenhebung erhält Bindungsidentitäten,
Voraussetzungen aller Quellmethoden und eigene Operationsbedingungen. Effektive und
komponentenweise Definitionsräume werden gemeinsam geschnitten. Explizite
Argumenttupelräume werden auch als Schnitt nicht in eine zusätzliche Tupelschicht
eingepackt. Reelle Intervalle/beschränkte Bereiche liefern konservative Grundmengen;
Normnachweise akzeptieren gültige typisierte Quellkomponenten und lehnen unbekannte,
nichtreelle oder undefinierte Komponenten ab. Keine Rückwärtsableitung innerer
Variablenbereiche aus einem zusammengesetzten reellen Wert.

Unveränderte historische Matrixdiagonalen-Definitionskarte als Testfixture gesichert:
SHA-256 763ee69d0e1eb2dce3bc7d504a7eb735fc6d1e9ea5ef14923324751a9f9fe16f.
Kopieren/Duplizieren, externe Kartenverweise, Undo/Redo und vorab angezeigter
Verbindungsverlust bei Diagonalprojektion sind ergänzt. Android-Gerätetest für den
produktiven Inspector ergänzt; lokale Installation weiterhin durch fremde Signatur
blockiert, vorhandene App/Daten bleiben erhalten. Geräteabnahme erst am finalen CI-SHA.
Android-Knoten-/App-Unit-Tests bestanden (14 s); abschließender Gesamtlauf steht aus.
Formelprüfung fand fehlende Formelannahmen im Normcallback und inaktive äußere
Modusparameter in Formeloperationen. Korrekturen mit gezielten Regressionen in Prüfung.
[Architekturentscheidung](../../decisions/2026-10-17-gemeinsame-strukturoperationen.md).

M2 unabhängige Abschlussprüfung: keine weiteren Blocker im geprüften Produktdiff.
Die vorherigen Findings zu Methodenhebung, Normdomänen, Codec-Portmetadaten und
Formelbedingungen/-modusscope sind korrigiert. Geprüfte Reichweite: fachliche
Kernverträge, alle Codecphasen, Katalog/Definition/Konzepte, gemeinsame Inspectorpfade
und Formeladapter. Prüfer arbeitete ausschließlich lesend und führte keine Tests aus.
Die abschließende lokale Produktsuite und CI am endgültigen Release-SHA bleiben
Voraussetzung der Veröffentlichung. Releaseverwalter bestätigt x-Klassifikation,
Android 3.4.4/3004004, Desktop 3.4.4 und Manifest 130 ohne Neutypen.

M2 lokale Freigabe des Produktbaums: 2.180 Tests, 0 Fehler, 1 bestehender
Opt-in-Messfall übersprungen. Vollständiger Gradle-Lauf mit --no-parallel test,
:app:lintDebug, :app:assembleDebug, :app:assembleDebugAndroidTest,
:KnotenKartenVerwalter:assembleDebugAndroidTest, :desktopApp:compileKotlin sowie
allen vier ausdrücklich angeforderten Desktop-Testtasks erfolgreich (3 min 15 s,
277 Tasks). APK-Metadaten bestätigen 3.4.4/3004004. Logs lokal:
/tmp/atlas-m2-freigabe.log. Repository/Architektur/Shadowmodule, 15 Standardkarten,
Releaseplan/Versionsfolge und 14 Release-Guard-Regressionen sowie git diff --check
bestehen. Unabhängige Zusatzprüfungen bestätigen auch die tatsächliche
Einzelanschlussbelegung und den vorhandenen N₀-Zahlbereich ohne Findings.

Konkrete lokale Prüfgrenzen: scripts/pruefe_kern.py beendet mit Exit 2, weil
Standalone-kotlinc fehlt (Java vorhanden). :desktopApp:packageRpm scheitert an
jpackage „Invalid or unsupported type: [rpm]“; rpmbuild ist lokal nicht vorhanden.
Daher keine lokale RPM-Installation/-Deinstallation oder Prozess-Persistenzabnahme.
App-Geräteinstallation war bereits durch fremde Signatur blockiert; vorhandene
App/Daten bleiben erhalten. Diese Pfade müssen in CI am finalen PR-/master-SHA
bestehen; eine lokale Buildfreigabe allein ist keine Veröffentlichungsfreigabe.
Produktbaum zur SamAI-Commit-/PR-Erstellung freigegeben; Merge erst nach endgültigen
SHA-bezogenen CI-Nachweisen und Integrationsfreigabe.

M2 master-Abnahme: Release-Guard 38991354934, Linux/RPM/Persistenz 38991354962,
Android-Build 38991354922 und Kern-verify im Workflow 38991354976 erfolgreich.
Android-runtime desselben Workflows: Editor 7/7, App 51 Tests mit einem Fehler
in KnotenAuswahlNutzungGeraeteTest.resetUndAbbrechenErhaltenAuchNochNichtBeruhigteSuche:
GETIPPT statt ZURUECKGESETZT. Keine M3-Reservierung vor vollständiger Abnahme.
Historischer Master-Commit bleibt unverändert; Autor Alexander, Committer GitHub/web-flow.

Reparatur v3.4.5: Gerätetest scrollt Reset vor echtem Klick sichtbar und prüft
unmittelbar den leeren Suchtext. Unveränderte Erwartung ZURUECKGESETZT.
Unabhängige Lesekontrolle bestätigt die engere Prüfung; exakte ursprüngliche
Ursache ohne Semantikbaum im Fehlerzeitpunkt nicht bewiesen (verdrängter
Suchkopf oder erneuter Eingabe-Callback möglich). Kein Infrastrukturfehler
behauptet. CI muss tatsächliche Resetaktion und nachfolgende Protokollierung belegen.

Reparatur v3.4.5 lokale Freigabe: vollständige Gradle-Suite mit test, lintDebug,
Debug-APK, beiden Android-Test-APKs, Desktop-Kompilierung und allen vier
Desktop-Testtasks erfolgreich (59 s, 277 Tasks; 2.180 Tests, 0 Fehler,
1 bestehender Opt-in-Skip). APK bestätigt 3.4.5/3004005. Repository,
15 Standardkarten, Releaseplan, Versionsfolge, 14 Guardregressionen und
Diffprüfung bestehen. Standalone-Kernprüfung weiterhin ohne kotlinc nicht
ausführbar; lokale RPM-Werkzeuggrenze unverändert, keine lokale Laufzeitfreigabe.
Unabhängige Abschlussprüfung: keine blockierenden Findings.


## M2-Abschluss über Testreparatur v3.4.5 (2026-10-17)

M2 bleibt historisch als v3.4.4 auf `ec152625926698f58e1ddd1d1a1d6420f8b0c6cc`
integriert; sein ursprünglicher Android-master-Lauf mit einem Suchresetfehler
wird nicht nachträglich als erfolgreich ausgegeben. Die enge Testreparatur ist
separat als v3.4.5 auf `e6760b9a41aaeb186dee7ee7c13254430cb47ee8` veröffentlicht
(PR #459, geprüfter Head `89a1097a5c09531772bccd63934b9fb66df9d0a9`).
Kein Produktionsverhalten, Katalog, Format oder Gate wurde geändert. Die genaue
ursprüngliche Fehlerursache bleibt unbewiesen.

Alle sechs Reparatur-PR-Gates sind erfolgreich. Das gesicherte
[Laufzeitartefakt](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38992259351)
belegt 7 Editor- und 51 App-Tests ohne Fehler oder Skips; der Resettest wurde
in 6,266 s erfolgreich ausgeführt. Sämtliche finalen master-Workflows am
Reparaturcommit sind durch die Root-CLI als vollständig erfolgreich bestätigt:
[Release-Guard](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38992803512),
[Linux/RPM/Persistenz](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38992803457),
[Android-Build](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38992803546),
[Kern/Android-Laufzeit](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38992803470).
Der konkrete zusätzliche master-Artefaktexport ist noch nicht gesichert; die
Workflow-Erfolge sind diesem finalen SHA zugeordnet. Lokale Werkzeug- und
Gerätesignaturgrenzen bleiben unverändert dokumentiert.

Lokale Arbeitscommits sind SamAI/SamAI verifiziert. Beide finalen GitHub-Squashcommits
haben Autor Alexander und Committer GitHub/web-flow und sind keine lokalen SamAI-Commits.
M3 ist anschließend als v3.4.6/x auf `samai/v3.4.6/konsolidierung-m3` ab exakt
`e6760b9a41aaeb186dee7ee7c13254430cb47ee8` reserviert; Reservierungscommit
`63acb023c1bc33b8e38016834f480563faee8e3d`, ausschließlich Roadmap, SamAI/SamAI.
M4–M7 bleiben unreserviert.

M3 Implementierung: gemeinsame Strukturübersetzung über ÜbersetzungsDienst
für Tupel/Zeile/Spalte/Abflachen; vorhandene Ziele und Wire-Payloads bleiben.
Strenge gerichtete Rollenprüfung und vollständiges Remapping vor Darstellung,
alle Codecphasen und eingebetteten Methodenquellen. Unbekannte Tiefe,
mehrdeutige Rollen oder Einzelbelegungen bewahren Original plus Sperrdiagnose.
Ein-Ebenen-Modus öffnet direkte Tupel und orientierte Vektoren wie bisher;
rekursiver Modus öffnet nur Tupel. Automatik bietet Abflachen nicht an.
Katalog/Definitionen/Suchindex/Android-Auswahlidentitäten und dynamische
Sammel-/Definitionsbuilder auf vorhandene Übersetzer umgestellt.
Gemeinsamer Inspector bietet explizite Abflachtiefe, Gerätetest prüft Undo.
TupelAuflösen/Ergänzen bleiben; unaufgerufene pauschale Vektormigration entfernt.
Synthetische unveränderliche verbundene Altkarte unter
MathematikKnoten/src/test/resources/konsolidierung-m3/verbundene-altadapter-v10.json.
Im Repositoryinventar keine tatsächlichen JSON-Altinstanzen dieser vier Typen;
dynamische Erzeuger separat geprüft. Gemeinsamer Sollbestand 124, Android 126.
Gezielte Knoten-/App-Tests und Android-Test-APK bestanden vor abschließender
Identitätsregression. Unabhängiger Befund zur namensbasierten Bereichsinferenz
korrigiert über bestehende rechnerQuellBereiche; gleichnamige ℂ-/ℝ-Quellen
werden nun ausdrücklich getrennt geprüft. Abschlussprüfung/CI/Veröffentlichung offen.

Historische Tiefenbezeichnung alle ist durch den bestehenden Kern-/Wiretest
belegt und bleibt als rekursiver Alias gültig. Neuere explizite Auswahl verwendet
rekursiv; bestehende alle-Parameter werden nicht umgeschrieben. Unbekannte
Zahlen/Texte bleiben diagnostiziert. Der Wire-Zeugnisprüfer verwendet denselben
Tiefenvertrag; Payload-Version und Format bleiben unverändert.

M3 lokale Freigabe des eingefrorenen Produktstands: vollständige Gradle-Suite
mit --no-parallel test, app lintDebug/assembleDebug/assembleDebugAndroidTest,
Editor-Test-APK, Desktop-Kompilierung und allen vier Desktop-Testtasks
erfolgreich (3 min 5 s, 277 Tasks). 2.193 Tests, 0 Fehler, 1 bestehender
Opt-in-Skip. APK bestätigt 3.4.6/3004006. Log /tmp/atlas-m3-final.log.
Repository/Architektur/Shadowmodule, 15 Standardkarten, Releaseplan mit
112 Einträgen, Versionsfolge, 14 Guardregressionen und Diffprüfung bestehen.
Standalone-Kernprüfung Exit 2 wegen fehlendem kotlinc (Java vorhanden).
Lokale RPM-Werkzeuggrenze und fremde Android-App-Signatur unverändert;
keine lokale Geräte-/RPM-Freigabe behauptet. CI-Laufzeit am endgültigen
PR- und Master-SHA bleibt Voraussetzung. Unabhängige Abschlussprüfung
einschließlich Identitäts-, Tiefenalias- und Orientierungszusätzen ohne Findings.
Fixture-SHA256: 7e46c59edc9dfc744b8046e1a29ff5f1594b56ef0136bcb0698d66d0a5077dee.
Produktbaum zur selektiven SamAI-Commit-/PR-Erstellung freigegeben.

M3 nachträglicher Vertragsbefund vor Veröffentlichung: Android KartenSpeicher
überschrieb unreferenzierte Revisionen auch nach mathematischer Migration.
PR #460/3ddb687b bleibt deshalb ungemergt und ist nicht der finale Head.
Enger Schutz ergänzt: vorhandene Zielrevision fachneutral lesen, ID/Version
und Format prüfen, vollständige aktuelle Lade-/Schreibnormalisierung mit
Originalgraph vergleichen. Bei notwendiger Migration oder unlesbarem Original
neue Revision oberhalb aller physischen Versionen; Originaldateien unverändert.
Gewöhnliche aktuelle Bearbeitungen behalten bisherige Speichersemantik.
Desktop speichert bereits stets eine neue Revision. Zwei JVM-Regressionen
bestanden; drei neue echte Android-Speichertests kompiliert (Originalbytes,
beschädigte höhere Revision, aktueller Normalfall, defekte Zieldatei).
Unabhängige enge Prüfung ohne Findings. Neue vollständige Abnahme/CI erforderlich;
die vorherige lokale Freigabe und CI gelten nicht für diesen Zusatz.

M3-Revisionsschutz: vollständiger lokaler Abschlusslauf bestanden (49 s, 277 Tasks;
2195 Tests, 0 Fehler, 1 bestehender Skip), einschließlich Lint, APKs, Test-APKs
und Desktop. Protokoll: `/tmp/atlas-m3-revision-final.log`. CI am ergänzten
Release-Head und dessen Veröffentlichung stehen weiterhin aus.

### M3 veröffentlicht; M4 begonnen

M3-PR #460 wurde am exakten Head a4ea6cda33d4df8a7bcb8c5b04c0bb31b870460a
mit sechs grünen Gates integriert. Editor 7/7, App 55/55, insgesamt 62/62
Gerätetests bestanden, einschließlich aller drei Originalrevisionstests.
Finaler Master 43726159a87efb50f1adcc5fe7cdafa981a2edd2: Android 38997004505,
Linux/RPM/Persistenz 38997004486, Kern/Gerät 38997004522 und Guard 38997004509
jeweils erfolgreich. GitHub-Squash: Alexander/GitHub, ausdrücklich kein lokal
erzeugter SamAI-Commit. Lokale Produktcommits 3ddb687b/a4ea6cda SamAI/SamAI.

Erst anschließend M4 als v3.4.7/x reserviert. Vor M4 wurden acht echte
Altkarte-Dateien und ein M3-Format-10-Installationspayload gesichert. Letzterer
entstand über M3-KartenJson.schreibe plus damaligen Inhaltshash; keine Behauptung
einer damaligen Geräteinstallation. Payload-SHA256
21f93dcde52b9aa6a0eb655e6c72a57c8092db715c136a157f8f8159e481d70f,
Provenienzhash c9687628d041752314ae125004a5298fc6dca9033f7818efcb23d20664488761.
Die tatsächliche Installerprüfung wird auf diesen Originalbytes aufgebaut.

Graphmodus: explizite 0–64 einzelne Elementanschlüsse; 0 bedeutet ausdrücklich
leere Menge, jeder andere Anschluss ist Pflicht. Lokale v2-/CSV-Daten bleiben
inaktiv erhalten. Unbekannte Eingänge/Mehrfachbeiträge werden abgelehnt.
Inaktive Operatorparameter dürfen die Graphports nicht durch Joinnormalisierung
verändern. Modus-/Anzahlwechsel melden entfernte Verbindungen, Standardwerte
und Kartenfallbacks im bestehenden KonfigurationsPlan, gemeinsam rückgängig.
Standardquelltemplates behalten Format 7/Version 1; offizielle lokale Updates
schreiben eine neue freie Revision. Höchste beschädigte Originalrevision und
bereits nutzerbearbeitete/gelöschte Karten sind vor Aktualisierung geschützt.
M4-Produkt-/Laufzeitabnahme steht noch aus. M5–M7 weiterhin offen.

M4 Zwischenverifikation: gezielte Knoten-/App-/Test-APK-Prüfung bestanden
(24 s/127 Tasks), anschließend echte acht Altfixtures und Desktopprüfung grün
(1m28s/147 Tasks). Erste vollständige Suite 2208 Tests/0 Fehler/1 bestehender Skip,
Lint/APKs/Test-APKs/Desktop, 3m23s/277 Tasks (`/tmp/atlas-m4-full.log`).
Danach unabhängige Findings zur präzisen Graphdefinition und tatsächlichen
Methodenverdrahtung korrigiert; deshalb erneute Gesamtprüfung am endgültigen
Produktstand erforderlich (`/tmp/atlas-m4-final.log`, derzeit laufend).
Originalhash als einziges Provenienzbeweismittel, Rückmigration einer
Nutzeränderung bleibt geschützt. Keine M4-Laufzeit-/Releasefreigabe behauptet.
Alle Python-Struktur-/Standardkarten-/Releaseprüfungen sowie 14 Guardregressionen
bestehen; zusätzliche Standalone-Kernprüfung weiterhin nicht ausführbar, da
`kotlinc` fehlt. Gerätekonflikt mit fremd signierter Nutzerinstallation und
fehlendes lokales RPM-Werkzeug unverändert; isolierte CI-Abnahme erforderlich.

M4 endgültige lokale Produktabnahme bestanden: 2213 Fälle, 0 Fehler,
1 bestehender Skip; Lint, APKs, Test-APKs, Desktopkompilierung und alle vier
Desktoptestmodule erfolgreich, 3m27s/277 Tasks (`/tmp/atlas-m4-final.log`).
APK-Metadaten 3.4.7/3004007 geprüft. Unabhängige Abschlussreview ohne weitere
Findings. Erneute lokale RPM-Paketierung scheitert konkret mit
`Invalid or unsupported type: [rpm]` (2 s, `/tmp/atlas-m4-rpm.log`,
`jpackage-2026-10-17-05-40-35-err.txt`). Keine lokale RPM-Laufzeit behauptet.
Produktstand eingefroren für Commit und vollständige CI; M4 bleibt bis zur
SHA-bezogenen PR-/Masterabnahme unvollständig, M5–M7 offen.


M4 CI an 974544a8012be44771ba05eb1a90d576db4cdd4d: fünf Gates grün,
Android-Laufzeit 66 Fälle (Editor 7/7, App 59 mit zwei Fehlern). Kein Merge.
Run 38999807874: Android-EndlicheMengeInspektor rief die gemeinsame
Eingabeformsteuerung nicht auf; Produktaufruf vor der lokalen/Graph-Verzweigung
ergänzt. Standardkarten-Gerätetest hatte den JVM-Hash als Android-Provenienz
verwendet. org.json verwendet dort unterschiedliche Schlüsselreihenfolgen.
Testseed rekonstruiert den damaligen Hash auf Android aus dem unveränderten
Original mit fachneutralem Codec/Metadatennormalisierung; unveränderte Fixturebytes
und eingefrorener JVM-Hash bleiben erhalten. Produktionsschutz nicht gelockert.
Unabhängige Ursachenprüfung bestätigt diesen engen Fix. Fehlerarchiv
/tmp/atlas-m4-runtime-failed.zip; erneute SHA-bezogene CI erforderlich.

Gezielte lokale Korrekturprüfung bestanden: app:testDebugUnitTest, lintDebug,
assembleDebug und assembleDebugAndroidTest, 53 s/207 Tasks
(/tmp/atlas-m4-runtime-fix-local.log). Unabhängige Diffprüfung ohne Blocker.


### M4 veröffentlicht; M5 reserviert

PR #461 wurde am freigegebenen Head 91f5c953589008e9ad090bb358f4356bfc9a849a
regulär ohne Admin-/Bypassoption als v3.4.7 integriert. Finaler Mastercommit
69d4e70a4f1bdbf69b23f0ee708f5893d5232947 (Vorgänger 43726159) trägt die
GitHub-Squashidentität Alexander/GitHub; lokale Arbeitscommits 974544a8 und
91f5c953 sowie Reservierung c10ce42d sind SamAI/SamAI verifiziert.

Alle sechs maßgeblichen jüngsten PR-Prüfungen erfolgreich. Isolierter Emulator
bestätigt Editor 7/7 und App 59/59, insgesamt 66/66 ohne Fehler oder Skips;
Mengeneingabe-/Undo-Test und sämtliche drei Originalstandardkarten-Gerätetests
bestanden (/tmp/atlas-m4-runtime-fix-final.zip). Ursprüngliche Runtimefehler und
der root-classpath-Resolverfehler bleiben oben beziehungsweise im PR dokumentiert.
Genau ein PR-Android-Retry am identischen Head bestand (Run 39000698093,
Job 117303178680); der API-HTTP-500 hinterließ den historischen roten Check im
Rollup. Keine Dependency-, CI- oder Prüfungsumgehung. Regulärer Merge zulässig
und erfolgreich, historische rote Ergebnisse werden nicht als grün ausgegeben.

Finale Masterabnahme am exakten 69d4e70a: Android 39001388559,
Linux/RPM/Neustartpersistenz 39001388566, Kern/isolierte Android-Laufzeit
39001388536 und Release-Guard 39001388554 jeweils completed/success;
beide Kernworkflowjobs verify und android-runtime erfolgreich. M4 ist damit
abgeschlossen. Diese Abnahme ersetzt die vorherigen offenen M4-Statusangaben.

Erst danach wurde M5 als v3.4.8/x direkt auf dem finalen Mastercommit reserviert.
Gemeinsame Typ-/Bereichsableitung und Konstantenpflege betreffen vorhandene
Quellen; Parameter-/Variablenrollen, stabile Identitäten und Wahr/Falsch bleiben
erhalten. Keine neuen Knotentypen oder Typentfernung, Katalog weiterhin 121/123.
M6–M7 bleiben offen und unreserviert. Android/Desktop/Linux/current_version
bleiben bis zum M5-Zielabschluss kohärent auf der veröffentlichten 3.4.7.

M5 erste Umsetzung auf reservierter Basis: gemeinsame Quellen-Grundrauminformation,
ID-basierte allgemeine Parameter, strikter Wertebereichsdecoder mit ausdrücklich
versioniertem neuem Leertupelbereich und gemeinsamer Android-/Desktop-Editor.
Historische mehrdeutige Quellen bleiben vor Portnormalisierung diagnostisch erhalten.
Methodenbildung und skalare Lösungsnamen unterscheiden gleichnamige Quellen nach
Identität. Numerische Nullinferenz und numerischer Ergebnisbereichsschutz sind in
Umsetzung; der bestehende Mengenfamilien-Zielmengenvertrag bleibt ausdrücklich
unverändert (ADR 2026-07-27). Neuer ADR: stabile-quellen-und-strikte-bereiche.
Erster gezielter Gradle-Lauf läuft; noch keine M5-Produkt-/Laufzeitfreigabe.

M5 gezielte Prüfung bestanden (26 s/147 Tasks, /tmp/atlas-m5-fifth.log):
Kern/Knoten/App-Unit-Tests, Android-Test-APK und zwei neue native Desktoptests
für explizite Bereichsreparatur und vollständige Tiefengrenzvalidierung. Zwei
neue Android-Inspector-/Undo-Tests kompiliert; Geräteausführung noch offen.
Zusätzliche symbolische Tupelpackung erhält ein logisches Ergebnis, und explizite
strukturierte Quellen besitzen selbsttragende Formel-/Wire-Bindungen.
Historische allgemeine Parameterargumente behalten exakt ihre Knoten-ID; keine
Umstellung dieser bestehenden Argumentidentität auf ein neues Suffix.
Letzte Ergänzungen und vollständige Suite am kohärenten Zielstand 3.4.8 stehen aus.


M5 letzte Vertragskorrekturen: gebundene Formeln priorisieren Quellen-IDs vor
Namensaliasen und erhalten strukturierte Zielräume über Wire/Methodenbildung.
Konkrete Vektor-/Matrixbereichsmitgliedschaften prüfen Orientierung/Form und
Komponenten; Skalarbindungen werden abgewiesen, offene Strukturparameter bleiben
offen. Tensor Rang zwei wird nicht still zur Matrix. Achsenlängen einschließlich
leerer Tupel inferieren N₀. Gesonderte Quellenidentitätsdiagnosen bleiben auch bei
ausdrücklicher Bereichsreparatur gesperrt.

Gezielter Nachlauf erfolgreich, 26 s/141 Tasks
(/tmp/atlas-m5-membership-targeted.log). Unabhängige finale Diffprüfung ohne
blockierende Findings und ohne Änderungen/Builds. Vollständige Suite läuft am
kohärenten Zielbaum 3.4.8; noch kein Produktcommit/PR oder Laufzeitfreigabe.


M5 vollständige lokale Freigabeprüfung bestanden, 3 min 26 s/277 Tasks:
2237 JVM-/Desktopfälle, 0 Fehler, 1 vorhandener Opt-in-Skip = 2236 bestanden.
XML-Zählung ausschließlich build/test-results (keine älteren connected-XMLs).
16 neue Quellenregressionen, sechs neue Kerntests und zwei Desktop-UI-Tests grün;
zwei neue Android-Inspector-/Undo-Fälle kompiliert, 68 Gerätefälle erst erwartet.
Log /tmp/atlas-m5-final-verified.log. Repository/15 Standardkarten/Releaseplan
114/Versionsfolge/14 Guards/Diffprüfung erfolgreich. Zusätzliche Standalone-
Kernprüfung mangels kotlinc nicht ausführbar. Lokaler packageRpm-Lauf 2 s/22 Tasks
scheitert unverändert in jpackage: Invalid or unsupported type: [rpm]
(/tmp/atlas-m5-rpm.log). Kein lokaler Geräte-Neuinstallationsversuch mit Datenverlust;
fremde Nutzersignatur bleibt geschützt. Produktdiff unabhängig ohne Blocker.
Produktbaum für SHA-bezogene CI eingefroren; M5 noch nicht veröffentlicht.


### M5 veröffentlicht; M6 reserviert

PR #462 wurde am konkret freigegebenen Head d31f4cded72d1ecd4af98109dd77a89a2d32979c
regulär ohne Admin-/Bypassoption als v3.4.8 integriert. Finaler Mastercommit
fb1984f9f83aa4e586b4e6bf22cb524ab0d9cb5a folgt unmittelbar auf v3.4.7/69d4e70a.
GitHub-Squashidentität Alexander/GitHub; lokale Reservierung 584b59bd und
Produktcommit d31f4cde SamAI/SamAI verifiziert. Kein Connector-Produktcommit.

Die finale lokale Suite bestand mit 2237 Fällen/0 Fehlern/1 vorhandenem Skip,
also 2236 bestanden, 3m26s/277 Tasks. Nur aktuelle build/test-results-XMLs
inklusive TypSystem gezählt; ältere connected-Berichte ausgeschlossen.
Unabhängiger Abschlussreview ohne Blocker. Alle sechs finalen PR-Gates grün;
isolierter Emulator Editor 7/7 und App 61/61, insgesamt 68/68 ohne Fehler oder
Skips (/tmp/atlas-m5-runtime-final.zip). Neue Parameterbereich-/Dimensions-/Undo-
und explizite Diagnose-Reparaturtests tatsächlich ausgeführt und erfolgreich.
Lokales Standalone-kotlinc fehlt und RPM ist lokal unsupported; unveränderte
Kern- und Linux-RPM-/Neustartpersistenz-CI lief erfolgreich.

Finale Masterabnahme am exakten fb1984f9: Android 39005206217,
Linux/RPM/Neustartpersistenz 39005206228, Kern/isolierte Android-Laufzeit
39005206244 und Release-Guard 39005206226 jeweils completed/success;
beide Kernworkflowjobs verify und android-runtime erfolgreich. M5 ist damit
abgeschlossen. Diese Abnahme ersetzt vorherige offene M5-Statusangaben.

Erst danach wurde M6 als v3.4.9/x direkt vom finalen Mastercommit reserviert.
Der Umfang präzisiert Begriffe und Verträge bestehender Darstellungs-, Maß-,
Raum-, Cauchy- und Richtungsknoten. Keine neuen Knotentypen oder Typentfernung,
Katalog weiterhin 121/123. M7 bleibt offen und unreserviert. Android/Desktop/
Linux/current_version bleiben bis zum M6-Zielabschluss kohärent auf 3.4.8.


M6 erste Umsetzung: LaTeX-Darstellung registriert im vorhandenen mathematischen Basisregister;
Adapter-Sonderdispatch entfernt. Bezeugte Ausgaben behalten ihre eigene Anzeige
und ihren Kontext. Folgerechnungen erhalten konservativ numerische Annäherung,
aber keine veralteten Form-/Bereichskontexte und keine künstlichen Zeugnisse.
Richtungsquelle n=0 bleibt exakt; n>0 erhält bestehenden Genauigkeitskontext und
Hinweis. Nachgewiesene Kerngrenzabweichung 31 versus gemeinsame 64 Winkel auf
MAXIMALE_RICHTUNGS_WINKEL vereinheitlicht. Präzisierte sichtbare Begriffe bei
stabilen Typ-/Konzept-/Varianten-IDs und erhaltenen Suchaliasen. Sechs neue
Vertragsregressionen; gezielter Lauf gestartet. ADR Darstellung/Genauigkeit.


M6 gezielte Korrekturprüfung erfolgreich, 7 s/44 Tasks
(/tmp/atlas-m6-third.log): sieben neue Architektur-/Genauigkeitsregressionen und
bestehende Mengen-Methodenverträge. Die unabhängige Prüfung fand zuvor einen
Genauigkeitsverlust in Formel-Snapshots. Literal-Snapshots verwenden nun den
fortgeführten Kontext; Operations-ASTs erhalten rekursiv NUM aus Kindmetadaten
und Herkunft ohne veraltete Formräume. Beide Fälle sind über Wire und erneute
Formelauswertung geprüft. Genauigkeitstransport liest ausschließlich Metadaten
und erzwingt keine neue Signaturinferenz historischer Methoden. Nachreview ohne
weitere Blocker. Erste Gesamtsuite wurde durch diesen letzten Inferenzfehler
abgebrochen; korrigierter finaler Gesamtlauf gestartet. Zwei frühe Testfixtures
wurden an bestehende Verträge angepasst: Norm besitzt Kompatibilitätsaliasausgabe,
Cauchy erwartet einen numerischen Träger ohne eindeutige Standardmetrik (Z).
Die Darstellung registriert einmalig im bestehenden Standard-Basisregister,
damit auch dessen bestehende API erhalten bleibt; keine doppelte Paketregistrierung.


M6 vollständige finale lokale Suite erfolgreich: 3 min 47 s/277 Tasks,
2244 JVM-/Desktopfälle, 0 Fehler, 1 vorhandener Opt-in-Skip = 2243 bestanden
(/tmp/atlas-m6-final.log). Sieben neue Regressionen grün; bestehende 68 Gerätefälle
kompiliert, noch nicht an M6 ausgeführt. Repository/15 Standardkarten/Releaseplan
115/Versionsfolge/14 Guardregressionen/Diffprüfung grün. Unabhängiger Nachreview
nach Snapshotkorrektur ohne weitere Blocker. Standalone-Skript mangels kotlinc
weiterhin offen; lokales jpackage unterstützt rpm nicht (/tmp/atlas-m6-rpm.log).
User-App und Daten wegen fremder Signatur unverändert. Produktdiff für PR/CI
am kohärenten 3.4.9-Zielstand eingefroren; noch keine Veröffentlichung.


### M6 veröffentlicht; M7 reserviert

PR #463 wurde am konkret freigegebenen Head c2b539ef2224684ec76363e6e3b2ca10eb444c17
regulär ohne Admin-/Bypassoption als v3.4.9 integriert. Finaler Mastercommit
49945974dbe96778b70341f29a32bd96747d18d8 folgt unmittelbar auf v3.4.8/fb1984f9.
GitHub-Squashidentität Alexander/GitHub; lokale Reservierung c37069dd und
Produktcommit c2b539ef SamAI/SamAI verifiziert. Kein Connector-Produktcommit.

Finale lokale Suite: 2244 aktuelle JVM-/Desktopfälle, 0 Fehler, 1 vorhandener
Skip, also 2243 bestanden; 3m47s/277 Tasks. Sieben neue M6-Regressionen und
unabhängiger Nachreview nach Snapshotfix ohne Blocker. Alle sechs finalen
PR-Gates grün; isolierter Emulator Editor 7/7 und App 61/61, insgesamt 68/68
ohne Fehler oder Skips (/tmp/atlas-m6-runtime-final.zip). Standalone-kotlinc
fehlt und RPM ist lokal unsupported; unveränderte Kern- und Linux-RPM-/
Neustartpersistenz-CI lief erfolgreich.

Finale Masterabnahme am exakten 49945974: Android 39006842925,
Linux/RPM/Neustartpersistenz 39006843006, Kern/isolierte Android-Laufzeit
39006842966 und Release-Guard 39006843009 jeweils completed/success;
beide Kernworkflowjobs verify und android-runtime erfolgreich. M6 ist damit
abgeschlossen. Diese Abnahme ersetzt vorherige offene M6-Statusangaben.

Erst danach wurde M7 als v3.4.10/x direkt vom finalen Mastercommit reserviert.
Neben nachgewiesener Alt-Auswerterbereinigung gehören ältere belegte Fehler
bei Sperrdiagnosen, Ports, Standardwerten und Logarithmusbasis-Argumenten zum
beauftragten Migrationsvertrag. Gesperrte Knoten erhalten keinen historischen
Mathematikfallback. Der Gesamtabschluss bleibt bis zur vollständigen Verbraucher-
und Vertragsevidenz offen; Katalogzahlen allein sind kein Nachweis.
Keine neuen oder aus dem kanonischen Katalog entfernten Knotentypen, weiterhin
121/123. Keine Folgeversion reserviert. Android/Desktop/Linux/current_version
bleiben bis zum M7-Zielabschluss kohärent auf 3.4.9.


### M7 in kleine getrennte Änderungsreleases teilen

Die Verbraucherinventur hat vor der Auswerterbereinigung echte ältere
Migrationsfehler belegt: Sperrdiagnosen konnten verloren gehen, Ports und
Verbindungen verschwinden, falsche Defaults wirken und Logarithmusrollen
beziehungsweise Basis-/Argumentzuordnungen mathematisch abweichen. Sie sind
Voraussetzungen des beauftragten Datenvertrags. Gemäß dem Auftrag, kleine
Änderungsreleases getrennt vollständig abzunehmen, wird die bestehende
v3.4.10-Reservierung deshalb auf M7a eingegrenzt.

M7a umfasst ausschließlich Migrationsschutz und Logarithmuskorrekturen. Es
entfernt keine alten Auswerter und führt keine allgemeine Kartenkanonisierung
durch. Unveränderte Basis ist der vollständig grüne v3.4.9-Master
49945974dbe96778b70341f29a32bd96747d18d8; der vorhandene SamAI-Branch bleibt.
Keine neuen oder entfernten kanonischen Knotentypen, weiterhin 121/123;
Änderungs-Version x. Zielmetadaten Android 3.4.10/3004010, Desktop/Linux 3.4.10.
Roadmapstatus released bezeichnet ausschließlich den PR-Zielbaum und keine
bereits erfolgte Veröffentlichung.

M7b enthält die anschließende Alt-Auswerterbereinigung, Kartenkanonisierung
und Gesamtabschlussprüfung. Die nächste Version darf erst nach dem finalen
v3.4.10-Commit auf master und allen vier grünen Master-Workflows bestimmt und
reserviert werden. Jetzt wird keine Folgeversionsnummer reserviert. Der
Gesamtabschluss M0–M7 bleibt offen; frühere breitere M7-Reservierungsangaben
sind historische Planung und werden durch diese Umfangskorrektur präzisiert.


### M7a Umsetzung und unabhängige Schutzprüfung

Die alten Zahlen-, Mengen-, Einzelrechner- und Vektor-Konfigurationsmigrationen
laufen in allen Codecphasen vor Adapterklassifikation. Bekannte historische
Rollen werden ausdrücklich geprüft und einmalig gemeinsam umgesetzt; IDs,
Standardwerte, Kartenfallbacks einschließlich @karte-Suffix, semantische
Portverträge und Typinferenzreferenzen bleiben erhalten. Mehrdeutige Graphen
bekommen vor Normalisierung eine konkrete Diagnose. Nachgelagerte Codec- und
Androidnormalisierung stellt diagnostizierte Originalknoten und ihre Kanten
wieder her, einschließlich doppelter Verbindungsidentitäten.

Unabhängige Prüfung fand zusätzlich einen zweiten Default-Remap und globale
Android-Kantenfilter sowie ungesicherte Transpositionsmigration. Diese werden
mit Regressionen geschlossen. Gültige Vektortranspositionen verwenden die
strikte gemeinsame Rollenabbildung; Matrixtransposition verwendet den bestehenden
Matrixrechner. Bekannte historische Matrixeingänge behalten ihre dokumentierte
Vektor-/Methodenerweiterung; individuelle semantische Verträge bleiben bestehen.

Mathematische Korrekturen sind ausdrücklich: Logarithmus bindet basis→a und
argument→b, geprüft als strukturierter Logarithmus und numerisch log₂(8)=3.
Der historische einstellige Wurzelknoten bedeutet Quadratwurzel und erhält keinen
erfundenen zweiten Gradanschluss. Nicht eindeutig in einen bestehenden Rechner
abbildbare alte Divisionen mit wert und divisorNull bleiben vollständig erhalten,
diagnostiziert und nicht auswertbar; der bisherige verlustbehaftete Androidumbau
wird nicht als Fallback reaktiviert.

Neue Beispiel- und Indexdefinitionskarten benutzen unmittelbar den gemeinsamen
kanonischen Speicherpfad. Der unveränderte historische Fünfersatz der einmaligen
Beispielinstallation bleibt erhalten. Noch keine allgemeine Assetumschreibung,
Auswerterentfernung oder M7a-Gesamtfreigabe. Gezielte Tests werden nach den
nachgewiesenen Schutzkorrekturen erneut ausgeführt.


M7a gezielter Kartenvergleich erfolgreich: alle 523 produktiven Repositorykarten
(508 Konzept- und 15 Standardkarten) über vier Codecphasen geprüft. Jede Phase
ist für sich idempotent; der vollständige Laden-/Speichern-/Neuladen-Rundlauf
bleibt stabil. Keine neue Sperrdiagnose in diesen Karten. Die absichtlich
zusätzliche historische Anzeigenamenkorrektur beim vollständigen Laden wird
von den schema-nahen Lesephasen getrennt geprüft. Laufprotokoll:
/tmp/atlas-m7a-inventar-zweit.log, 2 s/42 Tasks, erfolgreich.

Gezielte Knotensuite einschließlich neun neuer historischer Schutzregressionen
und App-Suite einschließlich zwei Android-Nachmigrationstests zuvor erfolgreich,
bis auf den jetzt behobenen dynamischen Indexdefinitionspfad. Dieser erzeugt
Prädikate und Aussagenrechner unmittelbar kanonisch, statt generische Ports an
alte Spezialtypen anzuhängen. Vollständige lokale Gesamtsuite anschließend
gestartet. Repository, 15 Standardkarten, Releaseplan 116, Versionsfolge,
14 Guardregressionen und Diffprüfung erfolgreich. Zusätzlicher Standalone-Kernlauf
mangels kotlinc nicht ausführbar; Gradle-Kerntests und CI bleiben verbindlich.
Frühe gezielte Läufe scheiterten an den dokumentierten Schutz-/Builderfehlern
sowie zwei Testimport-/Indexdateikorrekturen; keine Freigabe aus Fehlversuchen.


M7a finale lokale Gesamtsuite erfolgreich: 3 min 32 s/277 Tasks,
2256 JVM-/Desktopfälle, 0 Fehler, 1 vorhandener Opt-in-Skip, also 2255 bestanden
(/tmp/atlas-m7a-final.log). Lint, App-/App-Test-/Editor-Test-APKs und Desktop-
kompilierung enthalten. Unabhängiger finaler Diff-/Testreview ohne weitere
Blocker; der 523-Kartenvergleich belegt Migration/Kompatibilität/Stabilität,
keine pauschale mathematische Äquivalenz sämtlicher Karten. Neun neue gezielte
Schutzregressionen, ein Repository-Kartenvergleich und zwei App-Nachmigrations-
regressionen grün. Keine Produktionsdateien nach diesem Lauf geändert.
PR-/CI-/isolierte Laufzeit- und finale Masterabnahme weiterhin erforderlich.

Lokale RPM-Prüfung versucht, 2 s/22 Tasks: vorhandenes jpackage unterstützt rpm
nicht (/tmp/atlas-m7a-rpm.log). Keine lokale Installation oder Änderung fremd
signierter Nutzerdaten. Linux-Paketierung, Installation, zwei Starts mit gleichem
XDG_DATA_HOME, Persistenz und Deinstallation müssen die unveränderten CI-Gates
am freigegebenen Head belegen. 68 bestehende isolierte Androidfälle erwartet.


### M7a CI-Zeichenbarriere korrigieren

Erster PR-Head e66e0c92d51f0dcc267bf81d14956bf52e4fed2a: fünf von sechs
Gates erfolgreich, Push-Androidlauf 39009524750/job117328827248 fehlgeschlagen.
Konkreter Fehler: desktopApp VisualisierungsBedienungTest,
offeneGesteErhaeltAlleDetailsMehrererEbenen, Zeile 301, exakte Bildgleichheit
vor/nach down. PR-Android am selben Head erfolgreich. Kein Resolverfehler,
kein blinder Retry oder Merge. Originalprotokoll:
/tmp/atlas-m7a-android-build-failure.log. Historische isolierte Androidabnahme
dieses Heads 68/68 ohne Fehler/Skips (/tmp/atlas-m7a-runtime-e66e0c92.zip);
sie ersetzt keine Abnahme des korrigierten Heads.

Codeprüfung belegt eine unvollständige Fertigbarriere: Samplingabschluss konnte
Fortschritt verbergen, obwohl Plot seine Zeichenpakete noch separat asynchron
aufbereitete. Die konkrete Auslösung im gescheiterten CI-Lauf ist ohne dortige
Screenshots nicht bewiesen. Die notwendige enge Korrektur koppelt Fertigmeldung
und Canvas-Zustandsbeschreibung an Samplingabschluss und passende veröffentlichte
Pakete aller aktiven Ebenen. Die Desktop-Testbarriere wartet auf diesen Zustand.
Sampling-, Geometrie- und Gestenalgorithmen sowie exakte Pixel-/Pan-/Pinchprüfungen
bleiben erhalten; keine sleeps, größeren Toleranzen oder Testabschaltungen.

Gezielte Desktop-Bedienungsprüfung 17/17 erfolgreich, 10 s/22 Tasks
(/tmp/atlas-m7a-zeichenstatus.log). Unabhängiger enger Nachreview ohne Blocker:
aktuelle Ergebnisbindung, Cancellation, leere/terminale Ebenen und fehlende
Readiness-Dauerschleifen geprüft. Vollständige lokale Abnahme erneut gestartet,
danach alle sechs CI-Gates am neuen Head zwingend. Diese Korrektur erweitert
M7a ausschließlich um die belegte notwendige Fertig-/Prüfbarriere.

Finale lokale M7a-Wiederabnahme nach Zeichenbarriere erfolgreich: 3 min 34 s,
277 Tasks, 2256 JVM-/Desktopfälle, 0 Fehler, 1 vorhandener Opt-in-Skip
(/tmp/atlas-m7a-zeichenstatus-final.log). Lint und alle angeforderten Builds
enthalten. Repository-/Architektur- und Diffprüfung erneut grün. Unabhängiger
Renderer-Nachreview ohne Blocker; keine Sampling-/Gestenalgorithmusänderung.
Neuer Head benötigt vollständig neue sechs PR-Gates und Geräte-/Linuxabnahme;
e66e0c92 bleibt ohne Mergefreigabe. Keine Produktänderung nach diesem Lauf.

RPM am korrigierten Stand erneut versucht: 2 s/22 Tasks,
/tmp/atlas-m7a-zeichenstatus-rpm.log, weiterhin Invalid or unsupported type rpm.
Lokale Paketinstallation nicht möglich; unveränderte Linux-CI-Abnahme erforderlich.


### M7a veröffentlicht; M7b reserviert

PR #464 wurde ausschließlich am freigegebenen Korrekturhead
32f32e5c67ba21f47665be59a881d935ea2f69f7 regulär ohne Admin-/Bypassoption
als v3.4.10 integriert. Finaler Mastercommit f2d06d3087a0f27bbe58fad81187e8c6ac0c3501
folgt unmittelbar auf v3.4.9/49945974. GitHub-Squash: Autor Alexander,
Committer GitHub. Lokale Reservierung 565ece41 und Produktcommits e66e0c92 /
32f32e5c sind SamAI/SamAI verifiziert; kein Connector-Produktcommit.

Korrigierte lokale Vollsuite: 2256 Fälle/0 Fehler/1 vorhandener Skip,
also 2255 bestanden, 3m34s/277 Tasks. 17 gezielte Desktoptests und unabhängiger
Renderer-Nachreview ohne Blocker. Alle 523 Repositorykarten über vier Phasen
idempotent und im Rundlauf stabil, ohne Diagnosen. Der ältere Push-Android-
Testfehler e66e0c92 bleibt dokumentiert. Code-Race bei Sampling gegenüber
asynchronen Zeichenpaketen belegt; konkrete Auslösung des früheren CI-Fehlers
mangels Bildartefakten nicht bewiesen. Die strikte Fertigbarriere wurde ohne
Sleep, Toleranzänderung oder Testretry korrigiert.

Alle sechs aktuellen PR-Gates am 32f32e5c bestanden, einschließlich des zuvor
roten Push-Gates. Neuer isolierter Androidbericht /tmp/atlas-m7a-runtime-32f32e5c.zip:
Editor 7/7 und App 61/61, insgesamt 68/68 ohne Fehler oder Skips. Die früheren
68 erfolgreichen Androidtests gelten ausschließlich als historische Evidenz.
Lokales Standalone-kotlinc fehlt und RPM ist unsupported; Kern und Linux-RPM/
Neustartpersistenz wurden durch unveränderte CI erfolgreich geprüft.

Finale Masterabnahme am exakten f2d06d30: Android 39011501316,
Linux/RPM/Neustartpersistenz 39011501582, Kern/isolierte Android-Laufzeit
39011501513 und Release-Guard 39011501311 jeweils completed/success;
beide Kernworkflowjobs verify und android-runtime erfolgreich. M7a ist damit
abgeschlossen. Diese Abnahme ersetzt vorherige offene M7a-Statusangaben.

Erst danach wurde M7b als v3.4.11/x direkt vom finalen Mastercommit reserviert.
Umfang: nachgewiesene Bereinigung historischer Auswerter und kanonische
Repositorykarten, mit vollständiger Verbraucher-, Fixture-, Plattform- und
Vertragsevidenz. Originalschutz, historische Ladbarkeit und diagnostische Sperren
bleiben Abnahmekriterien. Keine neuen oder aus dem kanonischen Katalog entfernten
Typen, weiterhin 121/123. Gesamtabschluss M0–M7 bleibt offen. Keine Folgeversion
reserviert. Android/Desktop/Linux/current_version bleiben bis zum M7b-Zielabschluss
kohärent auf der veröffentlichten 3.4.10.


### M7b-Zielumfang und Metadaten

M7b beginnt direkt auf der vollständig geprüften M7a-Basis f2d06d30.
Zielmetadaten sind Android 3.4.11/3004011 und Desktop/Linux 3.4.11;
current_version ist im PR-Zielbaum 3.4.11. Der Roadmapstatus released bezeichnet
keine vorzeitige Veröffentlichung; dafür sind finaler Mastercommit v3.4.11 und
grüne Releaseprüfungen erforderlich.

Zur sicheren Konsolidierung des historischen vektorZuPolynom-Pfads wird der
vorhandene Polynomoperator um einen ausdrücklichen Argumentmodus erweitert.
Das ist kein neuer Operator oder Knotentyp. Die Kernüberladung mit Variable
validiert und delegiert an den bestehenden Algorithmus; keine zweite Formel.
Historische Zeilenkonstruktoren erhalten die fehlenden Ladeumsetzungen, bevor
zugehörige Aliase entfernt werden. Gültige vorhandene Operator-IDs werden an
der gesamten kanonischen Registry geprüft, nicht ausschließlich an einem Enum.
Diese Voraussetzungen gehören zu nachgewiesener Verbrauchermigration vor der
Auswerterbereinigung und Kartenkanonisierung.

Unveränderter kanonischer Katalog 121 gemeinsame/123 Android-Arten,
Änderungs-Version x, keine Folgeversion reserviert. Gesamtabschluss und Produkt-
freigabe bleiben bis vollständiger unabhängiger Verifikation offen. Root bleibt
alleiniger Produktwriter; Releaseverwaltung ändert nur Metadaten und Plan.


M7b Produktstart nach final grüner M7a-Masterabnahme: 523 Originalkarten und
SHA-Ledger unter MathematikKnoten/src/test/resources/konsolidierung-m7/originalkarten
unveränderlich gesichert. Exporttest verifiziert alle Prüfsummen, kanonische
Gleichheit und Speicherrundläufe; optionaler Export ausschließlich nach /tmp.
Erster Exportlauf grün, 5 s/42 Tasks (/tmp/atlas-m7b-export.log). Danach 523
Produktkarten auf Format 10 gebracht und statische Referenzen, Quellenindex,
Standardmanifesthashes und requiredNodeTypes synchronisiert. Stabile Pfade und
Karten-/Versionsidentitäten bleiben; historische Aufruffixtures bleiben unverändert.
Repository-/Architektur- und 15-Standardkartenprüfung grün.

Zusätzliche eng notwendige Vertragslücke: VektorZuPolynom besitzt einen
intrinsischen Variablennamen statt zusätzlichem Argumentanschluss. Ein AST-Ersatz
wäre ohne neue Formelcapability nicht ausführbar und würde bindungslose symbolische
Koeffizienten abweisen. Deshalb erhält der vorhandene zahl.polynom ausdrücklich
die Modi eingang und variable; beide delegieren denselben Multinomkern. Die
öffentliche Variable-Overload behält ihre strengeren Validatoren und delegiert
danach ebenfalls. Kein neuer Operator oder Knotentyp, kein Stringauswerter,
keine erfundene Quellenidentität. Vorhandene erweiterte Zahlenoperatoren werden
anhand der vollständigen Registry statt einer zu engen Universeller-Enum geprüft.
Gezielte Verifikation gestartet; UI-Konfigurationsplan und Legacybereinigung offen.


M7b Umsetzung und Vertragsprüfung: unabhängige historische Registrierungen für
konsolidierte Operationen entfernt, Methoden-Anwendungssonderdispatch entfernt.
Explizite finale Rechnerkomposition weist fehlende Operator-IDs ab. Historische
Zeilenquellen und gespeicherte Basisindizes werden streng und identitätserhaltend
umgesetzt; öffentliche Kernbasis bleibt einsbasiert, Loaderposition nullbasiert.
Die gemeinsame Polynomkonfiguration inklusive Inspector, Verlustvorschau und Undo
ist umgesetzt. Quellen-ID-Kollisionen werden getrennt von intrinsischen Namen
behandelt; echte Quellbereiche bleiben bei Grad null erhalten.

Unabhängig gefundene echte Altvertragsverluste geschlossen: Division mit offenem
Nenner erhält den Nullersatz als Fall einschließlich Methodenhebung; freie und
gebundene Formeln teilen die Null-/Voraussetzungslogik, ohne äußere Knotenkonfiguration
zu erben. Anzeigealiase bleiben erhalten. Automatischer Joinumbau beim Laden
entfällt, weil er ursprüngliche Einzelport-IDs und Verträge entfernte; bestehende
Joinbeiträge bleiben, gemischte nicht unterstützte Verträge werden bewahrt und
diagnostiziert. Alle 523 Originalkarten bestehen nun zusätzlich die Prüfung jedes
ursprünglichen Anschluss-/Verbindungs-ID-Erhalts. Produktkarten erneut aus diesen
Originalen exportiert, Indizes und Standardhashes nachgezogen.

Echte JVM-Suiten von Knoten, Adapter und Rechenkern grün in 20 s/49 Tasks
(/tmp/atlas-m7b-suiten-real-siebte.log), anschließend reine Bereinigung privater
unbenutzter Legacyhilfen und weitere finale Abnahme. Desktop-Shadowtesttasks ohne
Testquellen wurden nicht als ausgeführte Tests gezählt. Repository, Architektur,
15 Standardkarten, Releaseplan 117, Versionsfolge und 14 Guardregressionen grün;
zusätzliche Standalone-Kernprüfung weiterhin wegen fehlendem kotlinc nicht
verfügbar. Gesamte Produktprüfung und unabhängige abschließende Freigabe laufen;
noch kein Produktcommit, PR oder Veröffentlichung. ADR:
../decisions/2026-10-17-kanonische-auswerter-und-identitaetserhalt.md.


M7b Schlussdelta: Dynamische Grad-, Unterraum-, Endliche-Menge- und
Multinom-Definitionskarten durchlaufen ebenfalls die vollständige nachLaden-
Pipeline. Insbesondere bleibt der historische Indexgrundraum explizit, und die
Subtraktion im Multinom verwendet unmittelbar zahl.subtraktion. Neue reale
Auswertungsfälle prüfen die leere lokale Menge sowie Multinome für Dimensionen
0/1/3, Tupel und beide Vektororientierungen. Konzeptausgangsmarker verwenden nur
im Test den vorhandenen Durchreichvertrag; keine produktive Ersatzregistrierung.
11 gezielte Appfälle grün (2 s/78 Tasks,
/tmp/atlas-m7b-dynamische-definitionen-viert.log). Unabhängige Schlussprüfung ohne
neue Blocker. Vollständiger Wiederholungslauf auf diesem Quellstand gestartet.


M7b lokale Schlussabnahme vollständig: /tmp/atlas-m7b-produkt-final.log,
57 s/277 Tasks, 2273 Fälle/0 Fehler/1 vorhandener Skip (2272 bestanden).
JVM-/Desktoptests, Lint, Android-App-/Instrumentierungs-APKs und Desktopkompilierung
grün. Repository/Architektur, 15 Standardkarten, Releaseplan 117, Versionsfolge,
14 Guardregressionen und git diff --check grün. Unabhängige Schlussprüfung ohne
offene Blocker. Zusätzliches Kernskript wegen fehlendem kotlinc nicht ausführbar;
Gradle-Kerntests bestanden. Lokale RPM-Prüfung /tmp/atlas-m7b-rpm.log scheitert
an nicht unterstütztem jpackage-rpm-Pakettyp. Native Emulator-/RPM-/Persistenz-
Abnahme und Veröffentlichung bleiben an die finalen PR-/Master-SHAs gebunden.


M7b erste SHA-Abnahme 9bb9699927fe1c2630dc47bd9e955047c17e92fc:
PR #465, fünf Gates grün einschließlich Linux-RPM/Installation/zwei Starts
mit derselben Karten-ID, Revision 1→2 und Deinstallation. Isolierte Emulator-
abnahme: 68 Fälle, 66 bestanden, zwei Fehler in StandardKartenMigrationGeraeteTest
(aktualisiert=0 statt 1). Kein Merge oder Retry. Fehlerartefakt
/tmp/atlas-m7b-runtime-9bb96999-failed.zip, SHA-256
22fb016cb3d3dc49bd4e5bc130cede70e0540786c1090bb09e1873b09e67b086;
Zähler aus HTML, keine XML im Archiv. Dauerhafte Belege:
https://github.com/TeutonStudio/MathematikAtlas/pull/465#issuecomment-6231690513.

Produktursache gezielt lokal reproduziert: Der Installer verwendete ausschließlich
historische Vorlagenlisten als Bekanntmenge; der separat im kanonischen Katalog
ergänzte mathematik.mengenrechner fehlte. Nach Kanonisierung der Produktkarten
übersprang der Installer die Karte „Hebbare Definitionslücke“ vor ihrer
Aktualisierung. Neuer Manifestabdeckungstest über alle 15 Standardkarten schlägt
vor Fix genau mit dieser fehlenden Art fehl (/tmp/atlas-m7b-installer-vorher.log).
Enger Fix ergänzt alle kanonischen Katalogarten zur vorhandenen Bekanntmenge;
historische Ladetypen, Provenienz-/Hash-/Originalschutz unverändert. Gezielte
Installer-/Provenienztests anschließend grün (1 s/78 Tasks,
/tmp/atlas-m7b-installer-nachher.log), unabhängig ohne neue Blocker geprüft.
Vollständige Produkt- und danach neue SHA-Abnahme erforderlich; alter Head bleibt
gesperrt.


M7b Installer-Schlussdelta vollständig lokal grün:
/tmp/atlas-m7b-installer-produkt-final.log, 57 s/277 Tasks, 2274 Fälle,
0 Fehler, 1 vorhandener Skip (2273 bestanden), einschließlich JVM-/Desktoptests,
Lint und sämtlicher APK-/Desktop-Builds. Repository/Architektur, 15 Standardkarten,
Releaseplan 117, Versionsfolge, 14 Guardregressionen und Diff erneut grün.
Nur zwei Produktionszeilen im Installer und der gezielte Abdeckungstest ergänzt;
Veröffentlichung weiterhin erst nach neuen SHA-Gates und Geräteabnahme.
