# Methodenquelle und Identität

## Status und Ziel

Umsetzung und verfügbare Gesamtprüfungen abgeschlossen am 2026-10-15 nach ausdrücklichem Implementierungsauftrag. Android-Laufzeitabnahme wegen reproduzierbarem Emulator-Hostabsturz noch offen; der Plan bleibt dafür aktiv. Ein regulärer Knoten „Methode“ liefert eine Methode aus der Identität, einer konfigurierten Knotenvorlage/einem Rechneroperator oder einer versionsfesten Karte. Die Bedienung folgt dem vorhandenen Mengenknoten.

## Nicht-Ziele

Keine Referenzen auf vorhandene Knoteninstanzen, keine pauschale Umdeutung bestehender Pflichtanschlüsse, keine Veröffentlichung, keine beiläufige Überarbeitung der bereits veränderten Methoden-/Visualisierungspfade.

## Istzustand

- Basis lokal v3.3.1, HEAD c4b23c69c431370b600cd4bd810c8d5135c43ff7; ursprünglicher Branch samai/methoden-grundraeume-praedikate.
- Umfangreiche uncommittete Änderungen sind bereits vorhanden. Eine Dateikopie des Startzustands einschließlich Produktdateien und Status liegt in /tmp/mathematikatlas-methodenknoten-baseline-20261015. Diese Kopie dient der Diffabgrenzung, nicht als dauerhafte Produktquelle.
- Mengenquelle: KonsolidierteKnoten.kt / MengenKnotenInspektor.kt, gemeinsame RechnerOperatorAuswahlDialog-Auswahl.
- Kartenmethoden: KartenAuswerter.werteReferenzierteKarteAus; historischer Export erkennt methode.*. Der neue stabile Typ muss explizit integriert werden.
- Methode ist ein neutraler AtlasWert; mathematische Signatur und Auswertung sind zusätzliche Capabilities. Das TypSystem besitzt Typvariablen, aber keine vollständige Unifikation.
- Es gibt keine allgemeine optionale-Methodenport-Deklaration. Bestehende Verbraucher verlangen meist ausdrücklich eine Methode.

## Fachliche Semantik

Identität ohne Menge hat genau ein Argument x und ein unverändertes Ergebnis, mit derselben Typvariable T. Bei konkretem Aufruf folgt der Ergebnistyp dem Argument. Ein Tupel bleibt ein einzelner strukturierter Wert. Methodenwerte werden ebenfalls unverändert durchgereicht.

Mit angeschlossener Menge A gilt id_A:A→A. Nachweislich falsche Mitgliedschaft erzeugt einen Fehler, unentscheidbare Mitgliedschaft bleibt als Bedingung erhalten. Keine erfundene Universalmenge und keine Verwendung von FehlendeObermenge als tatsächlicher Domäne. Mathematische Materialisierung verwendet einen zum Elementtyp passenden Parameter.

Identität übernimmt sämtliche Argumentmetadaten; zusätzliche Voraussetzungen des Methodenwerts werden vereinigt. Standardauflösung: echte Verbindung, dann explizite Kartenmethode, dann Identität ausschließlich bei ausdrücklich optional deklarierten Methodenports. Fehler angeschlossener oder konfigurierter Quellen dürfen den Standard nicht auslösen.

## Daten-, Knoten- und Anschlussvertrag

- Neuer Art-Schlüssel mathematik.methode, Name „Methode“, reguläre Kontur, Kategorie „Methoden“; Defaultauswahl Identität.
- Rechts Ausgang methode mit bestehender Methodenanschlussart, stabile ID über Quellenwechsel.
- Links optionaler Mengeneingang menge nur im Identitätsmodus, höchstens eine Verbindung. Keine Methodenargumente als Eingänge am Quellknoten: sie gehören zur ausgegebenen Methodensignatur.
- Konfigurierte Vorlagen: fachliche Eingänge in stabiler Reihenfolge werden Methodenargumente, Ausgänge in stabiler Reihenfolge Methodenergebnisse. Semantische Identitäten stammen aus Originalports/Grenzknoten und bleiben bei Umbenennung erhalten.
- Kartenauswahl verwendet vorhandene KartenVerweis-Daten mit fester Version. Mehrwertige Ergebnisse folgen dem bestehenden kanonischen Tupelvertrag.
- Entfernte/inkompatible verbundene Anschlüsse nutzen bestehende Verlustbestätigung; eine Übernahme ist eine atomare Kartenaktion mit Undo/Redo.

## Architekturentscheidungen

Identitätssemantik und allgemeine Aufruf-Capability liegen im Android-/Compose-freien Kern. `AllgemeinAuswertbareMethode.rufeAuf(Map<String, AtlasWert>)` liefert Ergebnisse nach semantischen IDs; `MethodenAufrufErgebnis.durchgereichtesArgument` benennt erhaltene Argumentmetadaten. `IdentitaetsMethode`, `TypisierteIdentitaetsMethode` und `identitaetAuf(A)` decken freie, typenbeschränkte und mengenbeschränkte Identitäten ab. Typisierte Varianten weisen nachweislich inkompatible konkrete Werte zurück, ohne Zahlen auf ℝ zu beschränken. Der Adapter dispatcht diese Capability und erhält BedingterWert-Metadaten; mathematische Methoden bleiben im bisherigen Pfad.

Vorlagen-/Operatorauswahl stammt aus bestehenden Katalogen und Registern. Eignung verlangt erfolgreichen symbolischen Export mit gültiger Signatur, unterstützten Symbolparametern und Zielmengen. Reine UI-, Schnittstellen- und Gruppenknoten sind ausgeschlossen. Es entsteht kein paralleler Operatorenkatalog.

Knotenexport verwendet einen transienten Ein-Knoten-Kartengraphen und den gemeinsamen Kartenmethodenexport; transienter und persistierter Auswertungscache bleiben getrennt. Fehlende Karten, rekursive Verweise, unbekannte Quellen und ungültige Exportkonfigurationen erhalten klare Diagnosen.

Optionalität wird über `MathematikAuswerterRegister.registriereOptionaleMethodenEingänge` und das standardmäßig falsche Operator-Portflag `identitaetBeiFehlenderMethode` deklariert; keine mathematische Parameterauslegung im neutralen Karteneditor. Vor Injektion werden explizite Anschlussverträge auf einstelliges typgleiches Ein-/Ergebnis geprüft. In diesem Umfang wird kein bestehender Pflichtport optional.

## Betroffene Bereiche und Symbole

- Rechenkern: neue zentrale Identitätsimplementierung, gebundene Aussagen-/Mengenparameter und MathematischerWertCodec.
- Adapter: KartenAuswerter, MethodenAufrufAuswerter, Auswerterregister und optionaler Methodenstandard.
- MathematikKnoten: neue Quellvorlage/-konfiguration/-auswertung, MathematikKnotenVorlagen, Standardregister, MethodenAufrufSynchronisierung, Renderer, LatexText.
- App/Desktop: Methodeninspektor und vorhandener Quellen-/Bibliotheks-/Inspectorpfad, gemeinsamer Auswahldialog.
- Tests: Kern, Adapter, Knoten, App-Persistenz/Kartenaktionen und UI-Prüfungen.

## Inspector und Darstellung

Gemeinsame Auswahl mit Kategorien Identität, Knoten/Operatoren und Karten, Suche und Signaturvorschau. Nur relevante Konfiguration anzeigen; vorhandene fachliche Konfigurationsfunktionen wiederverwenden. Ungültige Entwürfe dürfen nicht übernommen werden. Abbruch verändert keine Daten.

Identität zeigt das Symbol \\hookrightarrow\\dots und die Definition x\\mapsto x; native Unterstützung für hookrightarrow ergänzen. Gemeinsamer Renderer-/Δzeit-Fußzeilenpfad, kein eigener Zeittext. Visuelle Abnahme umfasst kleine/große Knoten, Zoom, vergrößerte Schrift und Auswahlzustand sowie Wechsel ohne/mit Messung.

## Persistenz und Migration

Quellenwahl wird als Eigenschaft `methodenQuelle.id` gespeichert; `methodenQuelle.knoten` enthält ausschließlich die bestehende KartenDatenJson-Darstellung eines einzelnen konfigurierten Quellknotens ohne Verbindungen. Feste Kartenverweise verwenden das vorhandene Feld. Beide Quellenfelder, Modus und Anschlussstruktur werden vor Dispatch validiert. Keine Funktionen, Composables, Caches oder Methodenlaufzeitobjekte persistieren. Bestehendes Kartenformat beibehalten; historische methode.*-Knoten unverändert ladbar. Neue Knoten mit fehlender Auswahl verwenden Identität; unbekannte explizite Auswahl erzeugt eine Diagnose und keinen stillen Standard.

Für AussagenParameter und MengenParameter ist bindungsId optional mit bisherigem Default null ergänzt. Neue separate Wertcodec-Tags speichern ausschließlich die gebundenen Varianten; bestehende Tags werden unverändert gelesen und geschrieben. Roundtriptests sichern beide neuen Varianten ab.

## Versionswirkung

Genau ein neuer separat erzeugbarer Typ mathematik.methode: Achse y. v3.4.0, Code 3004000, Vorgänger v3.3.1 wurden durch master_verwalter nach Remote-/PR-Abgleich reserviert; Branch samai/v3.4.0/methodenknoten, Basis c4b23c69. Produktmetadaten bleiben bis zur Releasevorbereitung unverändert 3.3.1; dieser Auftrag veröffentlicht nichts.

## Meilensteine und Umsetzung

1. [x] Releasebasis/Reservierung geprüft: Connector bestätigt Remote-master c4b23c69/v3.3.1, keine konkurrierende aktive Reservierung; roadmap 3.4.0 reserved, Branch samai/v3.4.0/methodenknoten. Releaseplan und Versionsfolge erfolgreich.
2. [x] Kernidentität, Aufrufcapability und fachlicher Methodenstandard ergänzt; gezielte Modelltests grün, mathematische Nachprüfung freigegeben.
3. [x] Gemeinsamer Knoten-/Kartenexport und registrierte Quellvorlage integriert; Originalport-IDs, dynamische/mehrwertige Ergebnisse und Persistenz gezielt geprüft.
4. [x] Android-/Desktop-Quelleninspektor, Konfiguration, Renderer und LaTeX integriert; echter Desktop-Bedientest, acht Dialogkonfigurationen und 32 Knotenrendererzustände erfolgreich geprüft.
5. [-] Alle verfügbaren Modell-/Desktop-/Build-/Lintprüfungen und unabhängige Verifikation erfolgreich. Ausschließlich Android-Laufzeitprüfung bleibt wegen Host-Segfault 139 offen.
6. [x] Fakten und Aufgabendifferenzen dokumentiert; keine unbeabsichtigten Taskänderungen, kein Commit/Push/Release.

## Tests und Validierung

- Identität: Zahlen, Aussagen, Mengen, Tupel, Methodenwerte; Argumentanzahl und IDs; Typ-/Metadatenerhalt; Mitgliedschaft inklusive leerer Menge und Unentscheidbarkeit.
- Export: feste/dynamische/konfigurierte/mehrwertige Signaturen; Originalidentitäten nach Umbenennung; ungeeignete Quellen; fehlende Karten und Rekursion; Cachetrennung.
- Standard: Verbindung > Kartenfallback > explizit optionale Identität; Pflichtports bleiben Pflicht; Quellfehler bleiben Fehler.
- Registrierung, frische Instanz-IDs, JSON-Roundtrip, Kopieren, Quellenwechsel, Verlustbestätigung und Undo/Redo.
- Auswahl/Abbruch, lange Listen, Formelanzeige, große Schrift und Δzeit-Geometrie unter Android/Desktop mit tatsächlich ausgeführten Prüfungen.
- Gezielte Modulprüfungen, ./gradlew test, ./gradlew :app:assembleDebug; Repository-, Releaseplan-, Versionsfolge- und Kernprüfung. Nicht ausführbare Prüfungen konkret dokumentieren.

## Risiken und Rückfallstrategie

Vorhandene Änderungen an gemeinsam genutzten Dateien dürfen nicht überschrieben werden; Abschlussdiff gegen die Startkopie prüfen. Keine pauschalen Commits dieser fremden Änderungen. Typvariablen dürfen keine behauptete statische Unifikation vortäuschen. Exportierbarkeit unterscheidet sich je Knotenkonfiguration und wird fachlich validiert.

Bei nachgewiesenen Fehlern nur aufgabenbezogene Änderungen reparieren. Startkopie erlaubt gezielte Abgrenzung; kein pauschales Zurücksetzen des Arbeitsbaums.

## Fortschritt und Entscheidungsprotokoll

- 2026-10-15: Nutzer bestätigte Vorlagen/Operatoren statt Instanzreferenzen, allgemeine typbewahrende Identität, alle geeigneten Vorlagen und ausschließlich explizit optionale Methodeneingänge.
- 2026-10-15: Nutzer autorisierte Implementierung des vorgeschlagenen Plans.
- 2026-10-15: node_planner kartierte den Bestand; math_reviewer bestätigte die Architektur mit Auflagen zu Metadaten, Typkonkretisierung, echter Domäne und stabilen Exportidentitäten.
- 2026-10-15: Baselinekopie angelegt, Releaseaudit erneut beauftragt. Ein Produktimplementierer arbeitet; Koordinator bearbeitet Dokumentation, master_verwalter nur Roadmap/Branch.
- 2026-10-15: master_verwalter bestätigte Remote-/PR-Zustand über den Connector und reservierte v3.4.0. Vorhandene historische Draft-PRs #433/#435/#436 beanspruchen keine aktive Version und werden nicht integriert. Branchwechsel per SamAI-Skript erhielt den Arbeitsbaum; keine Commits/Pushes.
- 2026-10-15: Repositoryprüfung vor Produktintegration erfolgreich. Standalone-Kernskript nicht ausführbar (kotlinc fehlt); Gradle-Kerntests sind der maßgebliche ausführbare Kernnachweis. ADB-Liste außerhalb der Sandbox leer: kein Android-Gerät. Keine vorhandene AVD-Konfiguration; Android-Laufzeitprüfung aktuell offen. DISPLAY :0 vorhanden, Desktopprüfung wird versucht.
- 2026-10-15: Erster Implementierungscheckpoint: :MathematikRechenSystem:test, :MathematikKnotenDesktop:compileKotlin und :app:compileDebugKotlin erfolgreich; neue Regressionstests zu diesem Zeitpunkt noch nicht ergänzt. Desktop-X11 ist außerhalb der Sandbox nachweisbar erreichbar (xdpyinfo -display :0 erfolgreich).
- 2026-10-15: Mathematische Zwischenprüfung fand fehlende Widerspruchsprüfung allgemeiner Aufrufannahmen, fehlenden Metadatenerhalt der eingeschränkten Identität und falsche Metadaten beim Entpacken von Argumenttupeln. Implementierer meldet Korrekturen; gezielte Regressionstests und unabhängiger Nachlauf stehen aus.
- 2026-10-15: Nach Prüfung außerhalb der Sandbox ist KVM nutzbar. Vorhandenes API35-Tablet-Systemimage ermöglicht einen isolierten temporären Emulator ohne SDK-Download; AVD und Nutzerdaten liegen unter /tmp/mathematikatlas-methoden-avd. emulator-5580 ist per ADB verbunden. Android-Instrumentierung wird nach Fertigstellung der Tests ausgeführt; keine persönlichen Geräte oder Daten betroffen.
- 2026-10-15: Unabhängiger node_verifier prüft ausschließlich lesend gegen die Startkopie. Zwischenfindings zu unbekannten Quellenkennungen, Typableitung aus durchgereichten Metadaten und fehlender Methodensignaturvorschau sind an den Implementierer zurückgegeben. Noch keine finale Freigabe.
- 2026-10-15: Android-Laufzeitversuch scheitert an reproduzierbarem Emulator-Segfault (Exit139) vor erfolgreichem Boot: Swiftshader/Vulkan, Swiftshader ohne Vulkan sowie GPU-aus/1GB/2CPU. KVM ist nutzbar, ADB meldete den Gast kurz als verbunden, sys.boot_completed wurde nie erreicht. Temporäre Test-AVD anschließend entfernt; Logs /tmp/mathematikatlas-methoden-emulator.log und /tmp/mathematikatlas-methoden-emulator-off.log erhalten. Android-Laufzeitabnahme bleibt ausdrücklich offen; Test-APK-Bau und Desktop-Laufzeittests werden unabhängig fortgesetzt.
- 2026-10-15: Stabiler Implementierercheckpoint /tmp/methoden-final-check.log erfolgreich: gezielte Kern-/Adapter-/Knoten-/Sync-Tests, echter Desktop-Auswahldialogtest und Android-Debug-/Test-APK. Desktopaufnahme /tmp/methodenquelle-desktop-dialog.png durch Koordinator visuell geprüft: aktive Potenz-Auswahl mit Signatur (a,b)→(wert), Titel und Aktionen sichtbar. Größen-/Schriftmatrix und Δzeit-Aufnahme folgen.
- 2026-10-15: math_reviewer erteilte mathematische Freigabe nach Korrektur aller konkreten Findings einschließlich Ableitungs-Metadatenregression, typisierter Zahlenidentität ohne ℝ-Domäne und gebundener Aussagen-/Mengenparameter. node_verifier bestätigt die Korrekturen lesend; vollständiger Root-Testcheckpoint und finale Verifikation stehen noch aus.
- 2026-10-15: Root-Bildprüfung fand eine echte Layoutregression trotz zunächst grüner assertIsDisplayed-Tests: abgeschnittener Footer bei 800×350/2x und verdrängte Liste bei 390×600/2x. Auf gemeinsame AtlasDialog-Kopf-/Footerüberladung und begrenzt scrollenden Such-/Kategorie-/Signaturbereich umgestellt. Neue Boundsprüfungen verlangen vollständig sichtbare Aktionsflächen, Liste mindestens 40 px und reale Auswahlklicks; alle 8 Größen-/Schrift-/Themekombinationen bestanden. Korrigierte PNGs durch Root geprüft.
- 2026-10-15: Erster vollständiger Root-Prüflauf deckte fehlende neue Typzeile in release/knotentypen.json auf (Android/Knoten/Desktop-Katalogtests). master_verwalter ergänzte alphabetisch exakt mathematik.methode, jetzt 146 Typen. Wiederholung sämtlicher Root-Prüfungen erfolgreich.
- 2026-10-15: Finaler Root-Gesamtcheck /tmp/mathematikatlas-methodenknoten-gesamtcheck-final.log erfolgreich in 1 min 42 s, 248 Tasks. XML: 1.960 Testfälle, 0 Fehler, 1 Skip; 1.959 bestanden. Skip ausschließlich vorhandene VisualisierungsSamplingMessungTest mit fehlendem Opt-in-Systemproperty atlas.measureSampling. Neue Tests: 17 Modelltests + 3 Desktoptests grün. Keine Tests für den neuen Knoten übersprungen.
- 2026-10-15: Unabhängige finale Diffabnahme umfasst 31 Taskdateien gegen Startkopie, ohne offene Findings. Releaseaudit bestätigt genau einen neuen Typ/y-Reservierung; Releaseplan, Versionsfolge und 14 Guardregressionen erfolgreich. Keine Commits/Pushes/Veröffentlichung. Root ergänzt noch die explizite Desktop-Darstellungsmatrix des Knotens (Größe/Zoom/Schrift/Auswahl/ohne → mit Messung).

- 2026-10-15: Desktop-Knotenrendererprüfung um 32 Zustände erweitert: zwei Knotengrößen, Zoom 0,75/1,25, Schrift 1/1,6, ausgewählt/normal sowie ohne/mit Messwert. Vollständiger Zeittext, sichtbare Footerfläche und überlagerungsfreier Inhalt/Fußzeile erfolgreich geprüft; gezielter Lauf /tmp/methoden-identitaet-renderermatrix.log grün. Root prüfte Extremaufnahmen mit/ohne Messung unter /tmp/methodenquelle-identitaet-klein-z075-f16-ausgewaehlt-{ohne,mit}-zeit.png und /tmp/methodenquelle-identitaet-gross-z125-f16-normal-{ohne,mit}-zeit.png. Produktionscode blieb dabei unverändert.

- 2026-10-15: Abschließende Wiederholung mit erweiterter Renderer-Testmatrix ebenfalls erfolgreich: /tmp/mathematikatlas-methodenknoten-abschluss.log, 59 s, 248 Tasks. Gesamter Gradle-Test-/APK-/Lint-/Desktopprüfpfad grün; XML unverändert 1.959 bestanden, 0 Fehler, 1 vorhandene Opt-in-Messung übersprungen. Der aktive Plan hat ausschließlich die dokumentierte Android-Laufzeitabnahme offen.

- 2026-10-15: node_verifier erteilte nach Root-Abschlusslauf und ergänzter Rendererprüfung die Gesamtfreigabe ohne offene Reviewfindings unter der ausdrücklich dokumentierten Android-Laufzeitgrenze. Finale Abgrenzung umfasst 32 Aufgaben-Dateien einschließlich CURRENT_STATE; kein Produktionscode nach dem erfolgreichen Prüfstand geändert.

## Abweichungen

Desktop nutzt die gemeinsamen AtlasDialog-Bausteine direkt, da der Android-RechnerOperatorAuswahlDialog in der separaten Anwendungsschicht liegt. Die Gestaltung folgt demselben Kopf-/Inhalts-/Aktionsvertrag; es entsteht keine neue Produktionsabhängigkeit.

Für ältere Standardauswerter ohne explizite Zielmenge wird diese ausschließlich am markierten transienten Quellkartengrenzknoten aus bestehender Kerninferenz abgeleitet. Der Exportvertrag gewöhnlicher gespeicherter Karten wird dadurch nicht gelockert.

Android-Laufzeitabnahme kann trotz drei dokumentierter Emulatorvarianten nicht ausgeführt werden. Dies ist eine konkrete Umgebungsgrenze; Android-spezifische Interaktion wird nicht als laufzeitverifiziert behauptet. Der aktive Plan bewahrt diesen verbleibenden Abnahmepunkt.

## Ergebnis und Verifikation

| Prüfung | Ergebnis |
| --- | --- |
| ./gradlew test :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :desktopApp:compileKotlin | erfolgreich; 1.959 Testfälle bestanden, 1 vorhandene Opt-in-Messung übersprungen |
| Kern / Adapter / Knoten / Editor / TypSystem / App / Desktop | 585 / 71 / 772 bestanden + 1 Skip / 137 / 10 / 338 / 46 Testfälle |
| Neue Modelltests / Desktoptests | 17 / 3 erfolgreich; Dialogmatrix 8 Kombinationen, Knotenrenderermatrix 32 Zustände |
| Repository und Architektur | erfolgreich |
| Releaseplan / Versionsfolge / Guardregressionen | erfolgreich / erfolgreich / 14 erfolgreich |
| git diff --check und unabhängiger Taskdiff-Review | erfolgreich; keine offenen Findings |
| Mathematische Nachprüfung | freigegeben |
| python3 scripts/pruefe_kern.py | nicht ausführbar: kotlinc fehlt; Gradle-Kerntests erfolgreich |
| Android-Instrumentierung auf Gerät/Emulator | nicht ausgeführt: kein physisches Gerät, Emulator-Hostsegfault 139 vor Boot |

Artefakte: app/build/outputs/apk/debug/app-debug.apk und app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk. Logs und Desktopaufnahmen unter den oben genannten /tmp-Pfaden. Code-/Teststand ist uncommittet; HEAD bleibt c4b23c69 (v3.3.1), Branch samai/v3.4.0/methodenknoten. Es wurden keine lokalen SamAI- oder Connector-Commits erzeugt. Release-Guards sind für Reservierung/Umsetzung grün; ein Release-PR mit aktualisierten 3.4.0-Metadaten und Pflicht-CI ist nicht Bestandteil dieses Auftrags.
