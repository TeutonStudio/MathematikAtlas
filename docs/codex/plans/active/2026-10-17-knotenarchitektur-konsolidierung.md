# Knotenarchitektur konsolidieren, mathematische Verträge erhalten

## 1. Titel und Status

2026-10-17: Vom Nutzer beauftragter Gesamtplan M0–M7 in Umsetzung.
Ausgang: master 9fea31ddbaaef8bd26fa7a2730177eaaa9744909, v3.4.1.
M0: veröffentlicht als v3.4.2, Commit 5813c0337b5c7b2284d882b785559dfeea4ae3cd.
M1: veröffentlicht als v3.4.3, Commit e665a7fde78fe845dc7c62fa60fa00896d35f58a.
M2: integriert als v3.4.4, ec152625926698f58e1ddd1d1a1d6420f8b0c6cc;
abschließende Android-Laufzeitabnahme fehlgeschlagen. Enges Test-Reparaturrelease
v3.4.5/x auf samai/v3.4.5/such-reset-reparatur in Umsetzung; M3 unreserviert.
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
- [-] M2 Matrix/Vektor/Tensor: v3.4.4/x reserviert, Branch samai/v3.4.4/konsolidierung-m2, Basis e665a7fd; zuerst Methodenhebung und Fachkerne absichern, vier Spezialarten entfernen.
- [ ] M3 Tupel/Übersetzer: vier Spezialarten entfernen.
- [ ] M4 Mengen: drei Spezialarten entfernen.
- [ ] M5 Quellenverträge und Konstanten ohne Typentfernung vereinheitlichen.
- [ ] M6 Begriffe und Verantwortlichkeiten korrigieren.
- [ ] M7 unabhängige Alt-Auswerter entfernen, Gesamtabschluss prüfen.

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
