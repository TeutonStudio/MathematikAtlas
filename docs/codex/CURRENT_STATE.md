## Releaseziel v3.4.12, 17. Oktober 2026

Der Releaseabschluss umfasst die vorhandenen lokalen Änderungen an Graphbedienung, Mengen-/Tensorräumen, Urbild, Vorzeichenausgabe und automatischer Visualisierungsverfeinerung. Nutzer hat weitere Leistungsverbesserung ausdrücklich aus diesem Abschluss ausgenommen. Das 16,7-ms-Ziel und getrennte Vorschau-/Present-Abnahme bleiben Folgearbeit; keine 60-Hz-Freigabe. Android/ Desktop-Metadaten beschreiben den Zielbaum 3.4.12/3004012, Veröffentlichung erst mit finalem Mastercommit und grünen Releaseprüfungen. Unfertige spätere Optimierungsversuche gehören nicht zum Release.

## Urbild im Mengenrechner, 17. Oktober 2026 (lokal)

- Vorhandener Operator `menge.urbild` ist in Suche/Erstellen/Operatorauswahl
  verfügbar. Einzelziel und Zielmenge liefern eine gemeinsame Urbildmenge;
  der Zieleingang folgt der Ergebnissignatur der Methode. Android und Desktop
  verwenden gemeinsame Eingangseinstellungen und Typableitung.
- Endliche Definitionsräume und unterstützte Gleichungen werden exakt berechnet;
  allgemeine Fälle bleiben strukturierte Mengen mit erhaltenen Bedingungen.
  Effektive Definitionsräume, verschachtelte Tupel und Mengenwerte bleiben erhalten.
  Historische Platzhalter migrieren idempotent mit stabilen IDs; mehrdeutige Rollen
  bleiben diagnostizierbar. Verlustvorschau, Undo/Redo und Kopieren sind geprüft.
- Vollständige JVM-/Desktoptests und reguläre APK-/Test-APK-/Desktopbuilds grün:
  2.380 Fälle, 0 Fehler, 2 bestehende Opt-in-Skips. Android-Lint erfolgreich.
  Gemeinsamer Inspector nativ auf Desktop und SM-X920/Android16 bedient,
  einschließlich Moduswechsel/Undo/Redo/großer Schrift und Themewechsel;
  Screenshots visuell geprüft. Isolierte Android-Prüf-App wieder entfernt.
- Repository-/Standardkarten-/Releaseplan-/Versionsfolgeprüfungen und 14
  Release-Guard-Regressionen grün. Zusätzliche Standalone-Kernprüfung mangels
  `kotlinc` nicht ausführbar; Gradle-Kerntests grün. Unabhängige Quellabnahme
  ohne offene Blocker. Keine neuen Typen, Abhängigkeiten oder Formatversion.
- Arbeitsbranch `samai/v3.4.12/pie-methoden-matrizen`, HEAD `20356f1c`
  unverändert; keine Commits, Pushes oder Veröffentlichung. Klassifikation x
  innerhalb des bestehenden v3.4.12-Arbeitsumfangs; fremde Änderungen erhalten.
  [Plan](plans/completed/2026-10-17-urbild-mengenrechner.md),
  [Entscheidung](decisions/2026-10-17-urbild-mengenrechner.md).

## Automatische Visualisierungsverfeinerung, 17. Oktober 2026 (lokal)

## Mengenräume und Vorzeichenausgabe, 17. Oktober 2026 (lokal)

- Mengenrechner bietet Tupelraum A^n und Tensorraum beliebiger endlicher Stufe
  mit Zahlentupel als Dimensionsquelle. Menge besitzt Dimensionsart Tupel/Tensor;
  Matrizenvarianten sind aus Erstellen und Suche entfernt. Zweidimensionale
  Tensorräume akzeptieren passende Matrix-/Tensorwerte, historische Matrixräume
  behalten ihren engeren Vertrag. Keine neuen Knotentypen oder Abhängigkeiten.
- Gemeinsame idempotente Migration führt historische n/m-Rollen über einen
  sichtbaren Tupelknoten zusammen und erhält IDs, Quellen und Standards.
  Nicht eindeutig auflösbare Methoden-/Fallbackdimensionen bleiben mit Diagnose
  im Original erhalten. Dimensionswechsel zeigen Verluste vorab und bilden einen
  Undo-Schritt; eigene versionsfeste Mengenkarten behalten ihren Vertrag.
- Summen stellen negative Terme als Subtraktion dar, mit Klammern für Summen,
  negative Potenzbasen und verschachtelte Vorzeichen. Formelausgabe erhält
  Bedingungen und Bearbeitungs-IDs; Vorzeichenroundtrips sind regressionsgeprüft.
  Native Mengenklammern und indizierte Kreuzprodukte sind rendererkompatibel.
- Finaler Gesamtprüflauf erfolgreich: 2.350 JVM-/Desktopfälle, 0 Fehler,
  2 bestehende Opt-in-Skips, Android-Lint, reguläre App-/Test-APKs und Desktop.
  Vier isolierte Androidtests am SM-X920/Android16 einschließlich Schalter und
  Formeln beider Themes bestanden; native Bilder visuell geprüft. Repository-,
  Standardkarten-, Releaseplan-/Versionsfolge- und 14 Guardprüfungen grün.
  Standalone-Kernskript mangels kotlinc nicht ausführbar; Gradle-Kerntests grün.
  Unabhängige Quellabnahme ohne offene Findings.
- Branch samai/v3.4.12/pie-methoden-matrizen und HEAD 20356f1c unverändert.
  Keine Commits, Versionsänderung, Pushes oder Veröffentlichung; vorherige lokale
  Änderungen erhalten. [Abschlussplan](plans/completed/2026-10-17-mengenraeume-vorzeichen.md),
  [Modellentscheidung](decisions/2026-10-17-mengenraeume-vorzeichen.md).


- Android/Desktop verwenden automatische fortsetzbare Samplingarbeit ohne UI-Gesamtzählbudget. Symbolische Normalisierung, Nachweise und geschätzter Speicher bleiben geschützt. Budgeteingabe entfernt, historische Eigenschaften erhalten; keine Kartenformat-/Typ-/Anschlussänderung.
- Faire kooperative Rechenquanten, geteilte Aufträge, Dialogreserve, gemeinsamer Geometrie-/Proben-LRU und vorreservierte Arbeits-/Zeichengeometrie. 2D verwendet vorbereitete relative Pfade; Touch-Nachladung verändert keine persistierte Kamera während der Geste. 3D nutzt zusammenhängende Interaktionsnetze einschließlich partieller Snapshots; bekannte Definitionslücken sperren veraltete Flächen.
- Finaler Gradle-Gesamtlauf mit JVM-/Desktoptests, Android-Lint, App-/Test-APKs und Desktopkompilierung erfolgreich: 2333 Fälle, 0 Fehler, 2 opt-in Skips. 19 neue Regressionen bestanden. Repository-/Architektur-/Whitelist-, Releaseplan-, Versionsfolge- und Diffprüfungen grün. Standalone-Kernprüfung nicht ausführbar; Gradle-Kerntests grün.
- Native Desktopmessung: drei Läufe pro Fall, je 20 Anfragen/300 Eingaben. 3D Canvas-CPU p95 3,61–3,72 ms, Eingabe-bis-Capture p95 14,21–14,58 ms mit 1024 Dreiecken. Keine Gleichsetzung mit vollständiger Display-Framezeit. Nach Entsperrung: SM-X920/Android16, 19/19 native Bild-/Gesten-/Typprüfungen und drei Leistungsläufe bestanden. Insgesamt 720 Geometrieanfragen/17737 Frames. Instrumentierte FrameMetrics p95 2D 25,36–26,66 ms; 3D 67,86–68,90 ms statt 499,18–508,86 ms. Vollständige 3D-Detailverfeinerung benötigt länger (p95 3,24–3,70 s statt 1,19–1,20 s); erste Vorschau nicht separat gemessen. 60-Hz-Ziel noch verfehlt; gesonderte Vorschau-/Present-Abnahme offen.
- Keine Commits, Pushes, PRs, Versionswechsel oder Veröffentlichung. Arbeitsbranch und HEAD unverändert; fremde Änderungen erhalten.
- [Aktiver Abnahmeplan](plans/active/2026-10-17-visualisierung-automatisch-verfeinern.md), [Architekturentscheidung](decisions/2026-10-17-automatische-visualisierungsverfeinerung.md), [Desktopmessung](measurements/2026-10-17-visualisierung-desktop.json), [Androidmessung](measurements/2026-10-17-visualisierung-android.json).

## Pie-Anordnung, Methodenfarben und Matrizenmengen, 17. Oktober 2026 (lokal)

- Nutzerbestätigter Umfang umgesetzt: vollständige Hexringe mit Zwischenplätzen,
  gemeinsame Geometrie beider Pie-Pfade, farbstabile Methodenanschlüsse mit eigener
  Inkompatibilitätsmarkierung und getrennte Legenden je sichtbarer Farbsignatur.
- Matrizenmengen sind Varianten von Menge (ℝ/ℂ mit n,m) und Mengenrechner
  (Grundmenge,n,m). Symbolische natürliche Dimensionsquellen behalten Bindungen
  und offene Positivitätsbedingungen. Die gemeinsame Migration erhält historische
  Anschluss-/Kanten-IDs und übernimmt Zeilen/Spalten als explizite Standardwerte.
  Tensorraum.matrixForm unterscheidet Matrizen von allgemeinen Tensoren zweiter
  Stufe; historische Wertformate bleiben lesbar. Katalog 120 gemeinsam/122 vollständig,
  kein neuer Knotentyp und keine neue Produktionsabhängigkeit.
- Vollständige lokale JVM-/Desktoptests, Android-Lint, Debug-/Test-APKs und
  Desktopkompilierung grün: /tmp/atlas-pmm-final.log, 2m43s/248 Tasks,
  2287 Fälle/0 Fehler/1 vorhandener Skip. Gezielter Kernnachlauf nach begrenzter
  Verbraucherpräzisierung ebenfalls grün. Abschlussnachlauf /tmp/atlas-pmm-abschluss.log:
  26s/154 Tasks, relevante Units und reguläre App-/Test-APKs erfolgreich.
  Repository/Architektur, 15 Standardkarten,
  Releaseplan, Versionsfolge und 14 Guardregressionen erfolgreich.
- SM-X920/Android16: 12/12 Pie-Gestentests und separat 2/2 neue Farben-/Legendentests
  unter isolierter Prüf-App-ID bestanden. Deckende Halbkreisfarben und lesender
  Legendendialog geprüft; helle/dunkle Screenshots visuell kontrolliert. Prüf-App
  entfernt. Manueller Hardware-Stifttest bleibt offen. Standalone-Kernskript mangels
  kotlinc nicht ausführbar, Gradle-Kerntests grün.
- Branch samai/v3.4.12/pie-methoden-matrizen, unveränderte Basis/HEAD 20356f1c
  (v3.4.11). Version 3.4.12 als x/implementing reserviert; aktuelle veröffentlichte
  Metadaten bleiben 3.4.11. Keine Commits, Pushes, PRs oder Veröffentlichung.
  Unabhängige statische Prüfung ohne offene Findings.
- [Abschlussplan](plans/completed/2026-10-17-pie-methoden-matrizenmengen.md),
  [Modellentscheidung](decisions/2026-10-17-matrizenmengen-und-symbolsignaturen.md).

## Aktueller Konsolidierungsstand, 17. Oktober 2026

M0–M6 und M7a vollständig veröffentlicht und abgenommen. Aktuelle Basis
v3.4.10, f2d06d3087a0f27bbe58fad81187e8c6ac0c3501. PR #464: lokaler
SamAI-Head 32f32e5c, alle sechs finalen PR-Gates grün, neuer isolierter
Emulatorbericht 68/68 ohne Fehler/Skips. Finaler Master: Android 39011501316,
Linux/RPM/Persistenz 39011501582, Kern/Emulator 39011501513 und Guard
39011501311 sämtlich grün. Lokale Suite 2256/0/1, 3 min 34 s/277 Tasks.
GitHub-Squashidentität Alexander/GitHub; lokale Commits SamAI/SamAI verifiziert.
Der fehlgeschlagene frühere Desktop-Bildvergleich und die Korrektur der
Sampling-/Zeichenpaket-Fertigbarriere sind im aktiven Plan dokumentiert.

M7b v3.4.11/x basiert direkt auf f2d06d30, Branch
samai/v3.4.11/konsolidierung-m7b, Reservierung 536ee028 SamAI/SamAI.
Die Umsetzung ist vollständig lokal geprüft: historische unabhängige Auswerter
entfernt, gültige Altarten über den gemeinsamen Codec migriert, mehrdeutige
Originale mit Diagnose gesperrt. Getrennte Operandenports und Joinbeiträge behalten
ihre IDs; historische Zeilen-/Basisquellen sowie eingebettete Methodenquellen
sind berücksichtigt. Polynommodus, Nullersatzdivision, Formelpfade und dynamische
Definitionskarten verwenden die kanonischen Verträge.

523 ausgelieferte Originalkarten sind samt SHA-Ledger unveränderlich gesichert.
Alle ursprünglichen Knoten-/Anschluss-/Verbindungs-IDs, Diagnosefreiheit und
Speicherrundläufe sind geprüft. Produktkarten sind kanonisch in Format 10,
mit stabilen Pfaden/Karten-IDs/Versionen und historischen Konzeptverweisen;
Quellenindex, Standardkartenhashes und Typanforderungen stimmen überein.
Katalog: 121 gemeinsame mathematische/123 vollständige Android-Arten. Keine neue
Produktionsabhängigkeit, Registry, Operator- oder Knotentypeinführung.

Finaler lokaler Produktlauf /tmp/atlas-m7b-produkt-final.log grün:
57 s/277 Tasks, 2273 Testfälle, 0 Fehler, 1 vorhandener Opt-in-Skip,
2272 bestanden; JVM-/Desktoptests, Android-Lint, App-/App-Test-/Editor-Test-APKs
und Desktopkompilierung. Repository/Architektur, 15 Standardkarten,
Releaseplan 117, Versionsfolge, 14 Guardregressionen und Diffprüfung grün.
Unabhängige Schlussprüfung ohne offene Blocker. Standalone-Kernskript mangels
kotlinc nicht ausführbar; Gradle-Kerntests grün. Lokale RPM-Paketierung scheitert
an fehlender jpackage-Unterstützung für rpm (/tmp/atlas-m7b-rpm.log).
Erste PR-SHA-Abnahme 9bb96999: fünf Gates einschließlich Linux-RPM/Persistenz
grün; Emulator 66/68, zwei Standardkarten-Updatefehler. Kein Merge/Retry.
Installer-Bekanntmenge fehlte mathematik.mengenrechner nach Kartenkanonisierung;
mit neuem 15-Karten-Manifesttest lokal reproduziert und durch additive Aufnahme
des gemeinsamen Katalogs korrigiert. Hash-/Originalschutz unverändert,
unabhängig geprüft; gezielte Installer-/Provenienztests grün. Neuer vollständiger
Produktlauf grün: /tmp/atlas-m7b-installer-produkt-final.log, 57 s/277 Tasks,
2274 Fälle/0 Fehler/1 vorhandener Skip; 2273 bestanden. Repository, 15 Standardkarten,
Releaseplan, Versionsfolge, 14 Guardregressionen und Diff ebenfalls grün.
Neue SHA-Abnahme erforderlich. Veröffentlichung und native Abnahme
erfordern sämtliche PR-/Master-Gates und isolierte Android-/Linux-Laufzeitbelege
des finalen SHA. Keine Gesamtfreigabe
der unverändert als Anforderungsevidenz geführten 77-Punkte-Matrix.

Nachfolgend historische Einträge.

> Releaseprüfung PR #454: Der schmale Operator-Auswahldialog wurde wegen des nachgewiesenen Höhenkonflikts gezielt begrenzt. Scrollbarer Steuerbereich höchstens45Prozent, eigener verbleibender Trefferbereich; Titel und Aktionen bleiben im AtlasDialog-Rahmen. Unabhängig statisch ohne Findings geprüft; Androidregression für Listenhöhe, Auswahl, Abbruch und Übernahme bei Schriftfaktor1/2 ergänzt. Lokale App-Tests/Lint/APKs grün48s; native Laufzeitfreigabe erst nach vollständiger CI auf neuem Head. b22267cc bleibt ohne Mergefreigabe. Optionale Hardwaremessungen bleiben separat und gelten nicht als ausgeführt.

## Releaseabschluss v3.4.0, 2026-10-16 (lokal geprüft)

- Exakte veröffentlichte Basis: `v3.3.1`, `c4b23c69c431370b600cd4bd810c8d5135c43ff7`. Ziel `v3.4.0` ist eine Knoten-Version y mit exakt einem neuen registrierten, separat erzeugbaren Typ `mathematik.methode`. Android `3.4.0/3004000`, Desktop/CI `3.4.0`.
- Vollständiger Umfang: Methodenquelle, Grundraumverträge, Visualisierung, Join-Mehrfacheingänge mit Format-10-Migration, Anschlussinfos/Pie, Einfügelayout, Inspectorbereiche und globale Meldungsebenen. Join-Ersetzung bewahrt Zielbeitragsmetadaten; Methodenquellen aus Join-Vorlagen besitzen feste Argumentrollen. Nachgewiesene Integrationsfehler korrigiert und unabhängig nachgeprüft, keine offenen Findings.
- Finaler Gesamtaufruf mit `--no-parallel`: JVM-/Desktoptests, Android-Lint, App-/App-Test-/Editor-Test-APKs und Desktopkompilierung erfolgreich (2m56s, 277 Tasks). **2112 Fälle, 0 Fehler, 1 vorhandener Opt-in-Skip = 2111 bestanden**. Repository-/Architektur-, Standardkarten-, Release-/PR-Kontext- und Diffprüfung sowie 14 Guardregressionen erfolgreich.
- Ein vorheriger 5000-ms-Timeout im Visualisierungs-Abbruchtest bestand anschließend isoliert mit `--rerun-tasks` und im vollständigen Nachlauf. Keine Produktionsänderung dafür; Ursache nicht nachgewiesen. Standalone-Kernskript mangels `kotlinc` lokal nicht ausführbar, Gradle-Kerntests grün.
- Veröffentlichung erfordert grüne Pflicht-CI einschließlich Android-Emulator und Linux-Paketierung sowie den finalen Squashcommit auf master; der Roadmapstatus `released` beschreibt bis dahin den Zielbaum. Physische Stiftabnahme und vollständiges 60-Hz-Ziel bleiben offen. IDE-/Kotlin-/Python-Artefakte ausgeschlossen.
- [Abschlussplan](plans/active/2026-10-16-v340-master-abschluss.md); Arbeitsbranch `samai/v3.4.0/master-abschluss`, Integration über `release/v3.4.0-methoden-grundraeume-visualisierung`.

## Anschlussbezogenes Pie und globale Meldungsebenen, 2026-10-16 (lokal)

- Anschlussdrag währenddessen per Stiftseitentaste bewaffnen; Anheben auf freiem
  Hintergrund öffnet passende Varianten direkt in konzentrischen Kreisen.
  Anschlusskontakte haben Vorrang vor dem allgemeinen Pie-Aufruf. Diskrete
  Fachvarianten und sämtliche Übersetzungsziele/Unteroptionen werden über die
  bestehenden Kataloge/Konfigurationsfunktionen enumeriert; konkrete Wege werden
  gegen den ganzen Graph geprüft. Filterberechnung ist abbrechbar/asynchron mit
  Lade-/Leerzustand und Schutz gegen veraltete Sitzungen.
- Tippen bleibt Definition; Herausziehen fügt ein. Bei mehreren Anschlüssen oder
  Wegen explizite Auswahl im gemeinsamen Atlas-Dialog. Einfügen/Platzieren/
  Verdrahten/Übersetzer bilden einen Undo-Schritt; Abbruch erhält alte Kante und
  vorherige Knoten-/Gruppen-/Verbindungsauswahl. Mehrfachbeiträge und deren
  Reihenfolge bleiben erhalten. Keine Speicherung unbestätigter Zwischenstände.
- Obere Meldungsfenster: nicht ausgewählt hinter allen Knotenkörpern, ausgewählt
  davor, bei Mehrfachauswahl aktive Meldung zuletzt. Globale Ebenen mit gleichen
  Weltkoordinaten und Gruppenalpha; Körperreihenfolge bleibt erhalten.
- Nachgewiesen: `/tmp/atlas-anschluss-final.log` vollständiger Gradle-Lauf grün,
  **2.108 Testfälle, 0 Fehler, 1 vorhandener Skip** (2.107 bestanden). Android-App/
  Test-APK, Lint und Desktopkompilierung erfolgreich; Repository-/Architektur-,
  Releaseplan-, Versionsfolge- und Diffprüfung grün. Tatsächliche Compose-Prüfungen
  für Überdeckung, Mehrfachauswahl, Bedienbarkeit, Schrift1/2, Zoom und beide Themes.
  Anschlussdialog bei niedrigem Fenster/vielen langen Einträgen geprüft; normale
  und große Dialogscreenshots selbst sowie unabhängig visuell kontrolliert.
- Vier neue Android-Gesten-/Dialogregressionen gebaut. Für diese Erweiterung
  kein Gerätelauf: ADB bei beiden Abfragen leer. Frühere sieben Geräteprüfungen
  des vorherigen Pie-Stands ersetzen die neue Laufzeit-/physische Stiftabnahme
  nicht. Unabhängige statische Abnahme ohne offene Produktions-Findings; Plan
  bleibt für Geräteprüfung aktiv.
- Keine Schema-, Versions-, Abhängigkeits- oder Desktop-Whiteliständerung. Keine
  Commits/Pushes; bestehender Releasebranch und HEAD04925fc1 (SamAI/SamAI) erhalten.
  [ExecPlan](plans/active/2026-10-16-node-pie.md),
  [Meldungsebenen](decisions/2026-10-16-knoten-anhaenge.md).

## Pie-Knotenauswahl rund um den Tipppunkt mit Zoom, 2026-10-16 (lokal)

## Anschlussinfos durch Hover und Tippen, 2026-10-16 (lokal)

- Gemeinsamer Android-/Desktop-Editor: Maus-/Stift-Hover zeigt nach 500 ms eine
  auf 320 dp begrenzte Kurzinfo; Tipp/Klick öffnet den lesenden Anschlussdialog.
  LongPress/Rechtsklick behalten Kontext; Drag behält Snap, Join, Umverbinden und
  Adapterauswahl. Infos verwenden Originalreferenzen, markieren getroffene
  Join-Beiträge und verändern weder Graph noch Undo/Redo. Gemeinsamer Provider
  liefert effektive Typverträge; keine Werte, Format-/Versions-/Abhängigkeitsänderung.
- Nachgewiesen: finaler Gradle-Gesamtlauf mit **2089 bestandenen JVM-/Desktoptests,
  0 Fehlern und 1 vorhandenem Opt-in-Skip**, Android-App-/App-Test-/Editor-Test-APKs
  und Android-Lint erfolgreich. Repository/Architektur, Releaseplan, Versionsfolge
  und Diffprüfung erfolgreich. Unabhängiger Review inklusive Nachprüfung abgenommen.
- Android SM-X920 / Android 16: **3/3 Info-/Touch-/Stift-MotionEvent-Tests und
  1/1 Join-Test bestanden**. Native Dialog-Backevents direkt geprüft; globale
  Tasteneingabe wegen fehlendem Systemfokus der Testfenster nicht abgenommen.
  Physischer Stiftlauf durch einen Menschen offen. Desktoplayouts 390×600/800×350,
  Schriftfaktor 2 und beide Themes visuell geprüft; Kurzinfo bei Zoom .5/1.25 geprüft.
- Standalone-Kernskript mangels kotlinc nicht ausführbar; Gradle-Kerntests grün.
  Branch `release/v3.4.0-methoden-grundraeume-visualisierung`, HEAD `04925fc1`
  unverändert; keine Commits/Pushes. [ExecPlan](plans/completed/2026-10-16-anschluss-info.md),
  [ADR](decisions/2026-10-16-anschluss-info.md).

- Die zweite Ebene verteilt Knoten über volle 360 Grad um den ursprünglichen
  Tipppunkt. Gruppen bleiben zusammen; definierte Karten belegen getrennte
  Hälften (Schnittstelle links, Methodenausgang rechts).
- Pinch/Mausrad zoomen die geöffnete Pie-Auswahl unabhängig von der gesperrten
  Kartenkamera. Vorschauen, Abstände und Gruppenlabels skalieren zusammen;
  Herausziehen berücksichtigt den Menüzoom beim Griffpunkt. Ein Finger nach
  Pinch bleibt in der Menügeste; Scroll bei gedrücktem Zeiger verändert sie nicht.
- Nachgewiesen: 47/47 gezielte JVM-/Compose-Regressionen, Android-App-/Test-APK,
  Desktop-Kompilierung und Repository-/Architekturprüfung erfolgreich.
  Unabhängige statische Nachprüfung ohne offene Findings. Sieben Androidtests
  auf SM-X920 erfolgreich, einschließlich Pinch/Fingerwechsel und Einfügedrag
  nach Menüzoom mit Undo/Redo. Achter Mausradtest gebaut, wegen anschließend
  getrenntem Gerät nicht ausgeführt. Physischer Stiftlauf und vollständige
  visuelle Abnahme bleiben offen; ältere Aussagen ohne Geräteprüfung sind
  durch diesen begrenzten Nachweis teilweise überholt.
- Keine Versionsänderung, Commits oder Pushes. Details im
  [Pie-ExecPlan](plans/active/2026-10-16-node-pie.md).

## Kartennavigation nach Pie-Integration korrigiert, 2026-10-16 (lokal)

- Der gemeinsame Editor akzeptiert den vom Tap-Handler konsumierten Erstkontakt
  wieder für Pan/Pinch. Menüsperre und Prüfung konsumierter Bewegungen bleiben
  erhalten. Die mittlere Maustaste wird auf Desktop unabhängig vom primären
  Compose-Tap-Einstieg erkannt; Sekundärmaus startet keine Kamerageste.
- Nachgewiesen: Red-Lauf mit 6 erwarteten Fehlern, danach 8/8 Navigationstests
  einschließlich Knoten-/Port-Drag ohne Kamerabewegung sowie Undo/Redo. Insgesamt
  43/43 gezielte Desktop-/Editor-/App-Regressionen grün; Android-Debug-/Test-APK,
  Desktop-Kompilierung, Repository-/Architektur- und Diffprüfung erfolgreich.
  Unabhängig statisch ohne Findings abgenommen.
- Zwei zusätzliche Android-Navigationstests kompiliert. Geräteprüfung weiterhin
  offen: Tablet nach zwischenzeitlicher Verbindung nicht mehr an ADB; isolierter
  Abschlusslauf endet mit `No connected devices`. Keine Geräteabnahme behauptet.
- Keine Versionsänderung/Commits/Pushes. Details und Prüfnachweise im
  [Pie-ExecPlan](plans/active/2026-10-16-node-pie.md).

## Android-Node-Pie, 2026-10-16 (lokal)

- Stift-Seitentaste + Tippen öffnet Kategorien am exakten Punkt; Kategorie öffnet
  einen verschiebbaren Fächer mit nativen Vorschauknoten im Kartenzoom. Definierte
  Karten erscheinen mit Schnittstelle und Methodenausgang. Tap öffnet eine reine
  Definition; Herausziehen setzt dieselbe Pointergeste als atomare Einfügeinteraktion
  fort. Speicherschutz und kennungsgebundene Rücknahme verhindern Zwischenstände.
- Nachgewiesen: vollständiger JVM-/Desktoplauf mit 2.060 Testfällen, 0 Fehlern,
  1 vorhandenem Skip; Android-App-/Test-/Editor-Test-APKs, Lint, Desktop-Kompilierung
  und Repository-/Releaseplan-/Versionsfolgeprüfung grün. Zusätzlich 2/2 gezielte
  Desktop-Compose-Prüfungen für Vorschauzoom und Eingabesperre; Screenshot mit
  Schriftfaktor2/dunklemTheme visuell geprüft. Unabhängig statisch abgenommen.
- Android-MotionEvent-Regressionen gebaut, Laufzeitabnahme offen: kein Gerät an ADB;
  temporäre API36-/API35-Emulatoren sterben mit Host-Segfault139 vor dem Boot.
  Ring-/Fächerdarstellung und echter Stiftablauf damit noch nicht bestätigt.
  Standalone-Kernskript mangels `kotlinc` nicht ausführbar; Gradle-Kerntests grün.
- Keine neue Version, kein Commit/Push. Bestehender Releasebranch und HEAD04925fc1
  erhalten. [ExecPlan](plans/active/2026-10-16-node-pie.md),
  [ADR](decisions/2026-10-16-node-pie.md).

## Join-Eingänge, 2026-10-16 (lokal)

- Geeignete vorhandene kommutative und assoziative Operatoren besitzen gemeinsame Mehrfacheingänge mit wachsender Kapsel und einzeln greifbaren Leitungen. Bereichsabhängige Produkte zeigen ohne Nachweis geordnete Einzelenden; Matrix-/Tensorprodukt bleiben geordnet. Eine Quelle ergibt nach Validierung Identität; null Beiträge bleiben unvollständig.
- Format 10, gemeinsame idempotente Migration, vollständige Beitragslisten, lokale Werte, Kartenfallbackrollen und stabile Reihenfolge. Graphbewusste Operatorwechsel übernehmen Beiträge oder zeigen Verluste vorab; Kopieren und Undo/Redo sind abgesichert. Kein neuer Knotentyp und keine neue Abhängigkeit.
- Nachgewiesen: 2.027 bestandene JVM-/Desktoptests, 0 Fehler, 1 vorhandene übersprungene Opt-in-Messung; 21 Join-Regressionen. Desktop-Touch/Maus bei Zoom, Schrift und beiden Themes ausgeführt, Screenshot visuell geprüft. Android-App-/Test- und Editor-Test-APKs sowie Lint erfolgreich. Repository/Architektur, Releaseplan, Versionsfolge und Diffprüfung erfolgreich; unabhängig codegeprüft.
- Android-Gestenregression kompiliert, Laufzeitabnahme offen: ADB ohne Geräte. Standalone-Kernskript mangels `kotlinc` nicht ausführbar; Gradle-Kern-JVM-Tests erfolgreich. Keine Commits oder Veröffentlichung; bestehender Branch `release/v3.4.0-methoden-grundraeume-visualisierung`, HEAD `04925fc1352ad551622c1916fbd8f36c9ab83105`; andere lokale Änderungen erhalten.
- [Abschlussplan](plans/completed/2026-10-16-join-eingaenge.md), [ADR](decisions/2026-10-16-join-eingaenge.md).

## Platzschaffen beim Ablegen auf Verbindungen, 2026-10-16 (lokal)

- Der gemeinsame Android-/Desktop-Editor schafft nach erfolgreicher Drag-Einfügung
  beidseitig 32 Graph-Einheiten Platz. Der eingefügte Knoten bleibt stehen;
  ausgelöste Kollisionen nehmen auch unverbundene Hindernisse mit. Gruppen bleiben
  als Blöcke zusammen, aktive Einfügecontainer erweitern ihre Rahmen. Effektive
  Inhaltsgrößen, eingeklappte Gruppen und Diagnose-/Zeitanhänge werden berücksichtigt.
- Verdrahtung, Bewegung und Layout bilden einen Undo-/Redo-Schritt. Mehrdeutige
  Anschlusspaare verschieben erst nach bestätigter Auswahl. Die 200-ms-Animation
  betrifft ausschließlich die gemeinsame Darstellung; Endpositionen sind sofort
  gespeichert. Keine Format-, Versions- oder Abhängigkeitsänderung.
- Finaler Gradle-Gesamtlauf: **2.005 bestandene Tests, 0 Fehler, 1 vorhandene
  übersprungene Opt-in-Messung**; Android-App-/Test-APKs, Editor-Test-APK und Lint
  erfolgreich. Repository-/Architektur- und Diffprüfung erfolgreich. 17 neue
  Layouttests und 3 neue Desktoptests bestanden; Maus/Touch, Zoom, große Schrift,
  Themes und Animationsunterbrechung tatsächlich geprüft, Screenshots visuell geprüft.
- Unabhängig abgenommen. Android-Laufzeitprüfung offen: ADB ohne angeschlossene
  Geräte, keine eingerichteten AVDs. Android-Regression für Zoom/Schrift/Themes gebaut.
  Branch `release/v3.4.0-methoden-grundraeume-visualisierung`, HEAD `04925fc1`
  unverändert; keine neuen Commits. [Abschlussplan](plans/completed/2026-10-16-einfuegen-platz-schaffen.md)
  und [ADR](decisions/2026-10-16-einfuege-layout.md).

## Doppelstrichschrift, 2026-10-16 (lokale Korrektur)

- `LatexText` verwendet ausdrücklich den bereits gebündelten TTF-Schriftsatz des nativen Renderers. Dessen eigene AMS-Schrift stellt `\mathbb{R}` und `\mathbb R` als Doppelstrichbuchstaben dar; auch `LatexFormel` und die Annotationen der Atlas-Integralglyphe nutzen diesen Pfad. Keine neue Abhängigkeit, API-, Versions- oder Persistenzänderung.
- Nachgewiesen: 13 gezielte LaTeX-Tests erfolgreich; vollständiger JVM-/Desktop-Prüfpfad mit **1.985 bestandenen Tests, 0 Fehlern und 1 vorhandener übersprungener Opt-in-Messung**; Repositoryprüfung, Android-Debug-/Test-APK und Desktop-Kompilierung erfolgreich. Der Gesamtstand enthält außerdem bereits vorhandene lokale Änderungen anderer Aufgaben.
- **3/3 Android-Compose-Prüfungen auf SM-X920 / Android 16 bestanden.** Bildvergleich prüft N/Z/Q/R/C/H gegen aufrechte normale Buchstaben und beide mathbb-Schreibweisen gegeneinander, jeweils hell/dunkel. Bilder des echten Mengen-Knotens, der Potenzen/Indizes, Brüche, Matrixklammern, Methodenformel und Atlas-Integral visuell geprüft. Gegenprobe mit ursprünglicher OTF-Konfiguration lässt beide Glyphentests erwartungsgemäß scheitern; korrigierter Abschlusslauf wieder grün. Erster Lauf bei ausgeschaltetem Bildschirm fand keine Compose-Hierarchie; nach Aufwecken erfolgreich.
- Prüfgrenze: Der bereits im ursprünglichen OTF-Pfad sichtbare Methodenklammerfehler (`lbrace` als Text) bleibt außerhalb dieser Schriftkorrektur bestehen. Desktop-Laufzeitabnahme speziell der Doppelstrichschrift nicht durchgeführt. Keine Commits oder Veröffentlichung aus dieser Aufgabe; bestehender Branch `release/v3.4.0-methoden-grundraeume-visualisierung`, HEAD `04925fc1352ad551622c1916fbd8f36c9ab83105`.

## Methodenquelle und Identität, 2026-10-15 (reserviertes Ziel v3.4.0)

- Auf `samai/v3.4.0/methodenknoten`, Basis `c4b23c69`/v3.3.1, ist genau ein neuer regulärer Typ `mathematik.methode` implementiert: Identität mit optionaler Menge, konfigurierte geeignete Knotenvorlagen/Rechneroperatoren und versionsfeste Karten als Methodenquellen. Vorhandene uncommittete Methoden-/Visualisierungsänderungen sind erhalten.
- Allgemeine Identität bewahrt Typ und Wert einschließlich Methodenwerten; mengengebundene Identität prüft Mitgliedschaft und erhält offene Bedingungen. Explizit optionale Methodeneingänge verwenden Identität nach Verbindungs-/Kartenfallbackprüfung; bestehende Pflichtanschlüsse bleiben Pflichtanschlüsse. Stabile Komponenten-/Anschluss-IDs, Persistenz, Kopieren und Undo/Redo sind geprüft.
- Root-Gesamtcheck erfolgreich: **1.959 JVM-/Desktop-Testfälle bestanden, 0 Fehler, 1 vorhandene Opt-in-Samplingmessung übersprungen**, Android-Debug-/Test-APK, Android-Lint und Desktop-Kompilierung grün. Repository/Architektur, Releaseplan, Versionsfolge und 14 Guardregressionen erfolgreich. Mathematische Prüfung und unabhängiger Taskdiff-Review ohne offene Findings.
- Desktop-Auswahl tatsächlich geprüft: Suche, Auswahl, Abbruch, Übernahme, Undo sowie 390×600/800×350 bei normaler/doppelter Schrift in hellem/dunklem Theme. Identitätsknoten zusätzlich in 32 Größen-/Zoom-/Schrift-/Auswahl-/Messzuständen geprüft, inklusive vollständiger gemeinsamer Δzeit-Fußzeile ohne Inhaltsüberlagerung. Android-Instrumentierung kompiliert, aber Laufzeitabnahme offen: drei temporäre API35-Emulatorstarts scheitern reproduzierbar mit Host-Segfault 139 vor abgeschlossenem Boot; kein physisches Gerät angeschlossen. Standalone-Kernskript mangels `kotlinc` nicht ausführbar; 585 Kern-JVM-Tests bestanden.
- v3.4.0 ist als **y** für exakt `mathematik.methode` reserviert, Manifest umfasst 146 Typen; veröffentlichte Metadaten bleiben 3.3.1/3003001. Keine Commits, Pushes oder Veröffentlichung aus dieser Aufgabe. [ExecPlan und Prüfgrenzen](plans/active/2026-10-15-methodenknoten.md), [Architekturentscheidung](decisions/2026-10-15-methodenquelle-und-identitaet.md).

## Abgleich lokaler Änderungen am 2026-10-15 (Releaseziel v3.3.1)

> Arbeitsstand 2026-10-15 auf `samai/methoden-grundraeume-praedikate` (Basis v3.3.1, uncommittet): Variablen übernehmen den angeschlossenen Grundraum und behalten eine gemeinsame Quellenidentität; historische Bereiche werden beim Laden zu expliziten Quellen. Tupelfilter, Produkt-/Potenzraumgleichheit, automatische Prädikatnamen und Signaturfarben korrigiert. Nachgewiesen: 1882 JVM-Tests ohne Fehler/Skips, Debug-/Test-APK grün, 2/2 Compose-Geräteprüfungen auf SM-X920 / Android 16 unter isolierter Testpaketkennung. Unabhängig abgenommen. Standalone-Kernskript mangels `kotlinc` nicht ausführbar; 581 Kern-JVM-Tests grün. Details: [Abschlussplan](plans/completed/2026-10-15-methoden-grundraeume-praedikate.md).

- Basis: veröffentlichte v3.3.0 a789e606. Fehlende lokale Potenzraum-, Bool-Einertupel-, Bibliotheks- und Skilländerungen integriert; kein neuer registrierter Knotentyp.
- Potenzräume bewahren null-/einstellige Tupel und symbolische Dimensionen; Wertcodec, Substitution, Parameteranalyse und Mengenquellenmigration ergänzt.
- Finale JVM-Tests: 1864 ohne Fehler oder Überspringen; Android-Debug-Build erfolgreich. Android-Lint im finalen Wiederholungslauf erfolgreich; erster Versuch hatte einen internen FIR-Analyseabsturz. Repository-, Releaseplan-, Versionsfolge- und Guardregressionen bestanden.
- Kein Android-Gerät/Emulator angeschlossen; visuelle Bibliotheksprüfung offen. Der ältere lokale Geometrieprototyp bleibt gesichert und wird durch die veröffentlichte Geometry-Nodes-Familie ersetzt.
- Integration nach master: [PR #453](https://github.com/TeutonStudio/MathematikAtlas/pull/453), Squash-Titel `v3.3.1`; Veröffentlichung setzt grüne PR- und Master-Pushchecks voraus.
- Detailnachweis: [Abgleichplan](plans/active/2026-10-15-lokale-aenderungen-abgleichen.md).

# Aktueller verifizierter Projektzustand

> Visualisierungsüberarbeitung 15.10.2026 (lokaler, uncommitteter Arbeitsstand): passive ziehbare Knotenvorschau und gemeinsamer Android-/Desktop-Arbeitsdialog mit Entwurf, atomarer Übernahme, Konflikt-/Löschschutz; zentrierte Kameratransformation mit Altdatenüberführung, Clipping, numerische Achsen, dynamische räumliche Abdeckung und begrenzte progressive Samplingkoordination. Nachgewiesen: **1.918 JVM-Tests ohne Fehler/Skips**, Lint/Debug-/Test-APK/Desktop-Kompilierung grün, **9/9 Desktop- und 9/9 physische Android-Compose-Prüfungen** auf SM-X920 / Android 16. Vorschau-Median etwa 133 ms, R³-Gesten-p95 15,25 ms. **R²-Gesamtframezeit-p95 26,85 ms: 60-Hz-Ziel bleibt offen.** Keine Veröffentlichung oder Versionsänderung. [ExecPlan und Messgrenzen](plans/active/2026-10-15-visualisierung-ueberarbeiten.md).

> Zweiter Release-Nachprüfstand 2026-10-15: Der Emulator machte einen echten bestehenden Tupel-Anschlussfehler sichtbar: `typ.tupel<…>` wurde am groben Tupeladapter abgewiesen, die Sammelaktion blieb ohne Einfügung. Eine minimale Mathematik-Typbrücke behebt das, ohne neutrale Struktur-/Längen-/Komponentenprüfungen zu lockern. Zwei neue Brückentests und eine echte Sammelgraphregression ergänzt; unabhängig abgenommen. Finaler lokaler Nachlauf auf3.3.0: **1.856 Tests ohne Fehler/Skips**, Debug-/Test-APK grün21s. Neuer vollständiger PR452-Emulator-/CI-Lauf vor Merge noch erforderlich. Vorhergehende Checkpoints dokumentieren den jeweiligen damaligen Stand.

> Release-Nachprüfung 2026-10-15: [PR452](https://github.com/TeutonStudio/MathematikAtlas/pull/452), erster SamAI-Branchcommit `cdaf3dc8`. Fünf von sechs CI-Checks grün; API35-Emulator führte zwölf Tests aus (Editor2/2, App9/10). Von fünf neuen Dialogtests bestanden vier, einer benötigte Scrollen über den LazyColumn-Container statt direkten Zugriff auf eine noch nicht komponierte Zeile. Test korrigiert, fachliche Assertions unverändert; zusätzliche JVM-Katalogregression ergänzt. Lokaler Nachlauf mit3.3.0: **1.853 Tests ohne Fehler/Überspringungen**, Debug-/Test-APK grün19s. Erfolgreicher Emulator-/PR-Nachlauf und Masterintegration stehen vor diesem Dokumentationscheckpoint noch aus.

> Releasevorbereitung 2026-10-15: Auf Nutzerauftrag wird der vollständige geprüfte Stand als **v3.3.0** nach `master` integriert: 15 neue Geometry-Nodes-Typen, verbesserte Geometrieansichten und lokale Android-Knotenauswahlhistorie. Basis `d435b4fc`/v3.2.2, Branch `samai/v3.3.0-geometry-nodes`, Achse y, Zielcode3003000. Vor der Metadatenanpassung 1.852 Tests ohne Fehler/Überspringungen, Debug-/Test-APK, Lint und Kernprüfung erfolgreich; unabhängige Codeabnahmen ohne offene Befunde. PR-/Master-CI und finale Remote-SHA werden separat nachgewiesen. **Keine Android-Laufzeitabnahme des neuen Stands**; der Nutzungshistorie-Plan bleibt dafür aktiv. [Integrationsplan](plans/active/2026-10-15-v3-3-0-master-integration.md).

Die folgenden Arbeitsstände dokumentieren die vorhergehenden Prüfzeitpunkte vor dieser Releasevorbereitung.

> Arbeitsstand 2026-10-15: Lokale Nutzungs-/Suchhistorie der Android-Standardknotenliste implementiert, unabhängig codegeprüft. Suche, Tabs und Sortierung bleiben unverändert. No-Backup-JSON mit atomarer IO, stabilen Eintrags-/Operator-IDs, expliziter Auswahlherkunft, Such-/Tab-/Ergebniskontext, Lösch-/Schema-/Größenschutz und Einstellungen. 32 neue JVM-Tests; Gesamtstand **1.852 Tests ohne Fehler oder Überspringungen**, Debug-APK, Android-Test-APK, Lint und separate Kernprüfung erfolgreich. Fünf Android-Dialogtests ergänzt, **noch nicht auf Gerät/Emulator ausgeführt**. Branch `samai/v3.3.0-geometry-nodes`, Basis `d435b4fc`; kein neuer Commit/Push/Release. [Funktionsvertrag](../KNOTENAUSWAHL_HISTORIE.md), [aktiver Plan](plans/active/2026-10-15-knotenauswahl-nutzungsstatistik.md).

> Geometrieansichten, 2026-10-15: Auf demselben reservierten 3.3.0-Arbeitsbranch wurden nach dem [Desmos-/GeoGebra-Vergleich](../GEOMETRIE_DESMOS_GEOGEBRA.md) robuste numerische 3D-Ansichten ergänzt: ausdehnungsbasiertes Einpassen, gleiche Achsenmaßstäbe, indizierte Renderprojektion, feste Blickrichtungen, Pan/Pinch/Mausrad und lokale Punkt-/Kantensichtbarkeit. Keine weiteren Typen oder Datenformatänderungen. 1.820 lokale JVM-/Desktoptests ohne Fehler oder Überspringen, Android-Debug-Build und Instrumentierungskompilierung erfolgreich. Mausrad und Zwei-Finger-Gesten einschließlich unveränderter Kartenansicht tatsächlich auf Desktop geprüft. Kein neuer Android-Laufzeitnachweis. [Abgeschlossener Plan](plans/completed/2026-10-15-desmos-geogebra-geometrie.md). Arbeitsstand ungecommittet; veröffentlichte Metadaten weiter 3.2.2.

> Arbeitsstand 2026-10-15: Geometry-Nodes-Grundlage auf `samai/v3.3.0-geometry-nodes`, Basis der veröffentlichten v3.2.2 `d435b4fcf62ce83409911f98c08f0a096e78c0dd`. **3.3.0 ist reserviert**, die veröffentlichten Produktmetadaten bleiben bis zur Releasevorbereitung 3.2.2. Genau 15 neue Typen unter `mathematik.geometryNodes.`: Mesh-/Punktquellen, Geometrieoperationen, lazy Skalar-/Vektor-/Auswahlfelder, Instanzierung/Realisierung und gemeinsame Android-/Desktop-Drahtvorschau. Kein Kartenformatwechsel. 1.807 lokale JVM-/Desktoptests ohne Fehler oder Überspringen; Android-Debug-Build und Instrumentierungskompilierung erfolgreich. Desktop-Vorschau inklusive Drehen/Reset und Konfiguration/Undo tatsächlich ausgeführt. Kein Android-Laufzeitnachweis für diesen neuen Stand. [Anleitung und Beispielkarte](../GEOMETRY_NODES.md); [Plan](plans/completed/2026-10-15-geometry-nodes.md). Unabhängige finale Codeabnahme und Versionsaudit erfolgreich. Kein neuer Commit, Push oder Release aus diesem Arbeitsstand erzeugt.

> Neuer Releasekandidat, 2026-10-15: **3.2.2** auf `samai/v3.2.2-verbindungen-vollbild`, Basis `e535190058014810617ea2989d27838afe54692b` (v3.2.1). Enthält Graphindizes/-cache, automatische Übersetzerketten, atomare Verbindungs-Drag-Aktionen und Android-Vollbild. Kein neuer Knotentyp oder Datenformatwechsel. 1.777 JVM-/Desktoptests und Android-Debug-Build erfolgreich; Android-Instrumentierung kompiliert, lokal nicht ausgeführt. Unabhängige Abnahme der beiden [Graph-](plans/completed/2026-10-15-graph-auswertung.md) und [Verbindungspläne](plans/completed/2026-10-15-verbindungsbearbeitung-und-vollbild.md). Veröffentlichung und PR-/Master-CI werden separat nachgewiesen. Der nachfolgende Abschnitt dokumentiert den historischen verifizierten Stand vom 2026-10-12.

> Stand: 2026-10-12, veröffentlichte V3 Alpha. Beide Releases und ihre PR-/Master-Push-Prüfungen sind abgeschlossen; lokales und GitHub-master wurden auf den veröffentlichten3.1.0-Commit synchronisiert. Vollständige Anforderungsmatrix: [V3_ALPHA_ABNAHME.md](../V3_ALPHA_ABNAHME.md).

## Metadaten

- Veröffentlichter Vorgänger: `v3.0.1`, GitHub-master-Commit `2842a03e18797d9689ebaf5fc3e389e1bb60ffc9`, PR#444; sämtliche PR- und Master-Push-Prüfungen grün.
- Veröffentlichte Alpha: `v3.1.0`, GitHub-master-Commit `811955f8c0bdd5f2bd59292cd9dea5ba634f1c51`, [PR#445](https://github.com/TeutonStudio/MathematikAtlas/pull/445); sämtliche PR- und Master-Push-Prüfungen grün.
- Produktstand: `3.1.0`, Android-Code `3001000`, Desktop `3.1.0`; Branch `samai/v3.1.0/alpha-erweiterungen`, Releasebranch `release/v3.1.0-alpha-erweiterungen`. Versionsachse `y`, exakt zwei neue Typen.
- Originalmaster per Fast-forward auf `811955f8c0bdd5f2bd59292cd9dea5ba634f1c51` synchronisiert; Produktarbeitsbaum sauber. Zehn persönliche IDE-Dateien bytegleich erhalten. Dauerhafter lokaler Nachweis: `Original/.gradle/alpha-konsolidierung/backup-20261011-094918/final-master-sync.json`. Persönliche IDE-Dateien gehören nicht zum Release.
- Karten-JSON-Writer: `KartenDatenJson.FORMAT_VERSION = 9`; neue Knoten benötigen keine Formaterhöhung.
- Statischer Erstellenkatalog: 138 Typen; unveränderte veröffentlichte3.0.0-Baseline136 plus `atlas.uebersetzer` und `mathematik.tupelAbflachen`.

## Verifizierte Prüfungen

| Prüfung | Ergebnis |
|---|---|
| Repository, Architektur, Methodenmodell, Desktop-Shadowmodule, Standardkarten15 | erfolgreich im finalen3.1.0-Stand |
| Releaseplan, Versionsfolge, 14 Guardregressionen | erfolgreich; PR-Ereignis gegen exakte3.0.1-Masterbasis geprüft; neue Typen exakt abgeglichen |
| Vollständige JVM-/Desktoptests | 1419 ohne Fehler oder Überspringen: TypSystem10, Kern433, Graph93, Adapter49, Knoten573, App258, Desktop3 |
| Android-Debug-Build und Lint | assembleDebug/lintDebug erfolgreich mit3.1.0-Metadaten; bestehende Lintwarnungen bleiben dokumentiert |
| Unabhängige Knotenverifikation | finale Codefreigabe ohne blockierende Befunde; mathematische Quellen-/Tupel-/Matrixverträge geprüft |
| Android API36 | SAF-JSON/.matlas für neue Knoten, Abflachenergebnis, Übersetzerzielwechsel mit Undo/Redo und Prozessneustart geprüft |
| Gemeinsamer Formeleditor | Desktop Tastatur/Stift/Foto-Modi; Footer bei1440×840 erreichbar. Android Zeichnen/Radieren, atomare Formelübernahme/Undo/Redo und gespeicherte Originalgesten geprüft;1,5facheSchrift/dunklesTheme |
| Persistenz und Import | beide neuen verbundenen Knoten über echte JSON/.matlas-Codecs mit erwarteter Auswertung geprüft; Zeichnungs-/AST-/Kontext-/Copy-/Undo-/Fehlerregressionen grün |
| Veröffentlichung3.1.0 | PR#445 und Master-Push-CI erfolgreich: Android38186386146, Linux38186386113, Mathematikkern38186386161, Release-Guard38186386173 |
| 3.0.1-Laufzeitnachweise | Importjournalwiederherstellung aller18Dateien byteidentisch; Operatorverlustbestätigung/Undo/Redo; Kapseltouch, Navigation, aktive Unterkarte und Prozessneustart |

Geräte mit physischem Stift, automatische Android-Instrumentierung, Frame-Timing und lokale Desktop-Installationspakete sind durch diese Tests nicht abgedeckt. Emulatorgesten und echte Desktop-Compose-Oberfläche ersetzen keine Hardwaremessung.

## Alpha-Erweiterungen in3.1.0

- Übersetzer mit gespeichertem Ziel, stabilen Anschlüssen und stark gerundeter Kapsel; historische Spezialknoten bleiben ladbar. Definitionen folgen dem gespeicherten Ziel.
- Heterogene Tupel, rekursives Abflachen und Term→Methode mit expliziter Quellenbindung, erhaltenen Außenquellen/Annahmen und kanonischem Tupelvertrag.
- Gemeinsame streng linksassoziative Matrixkette für beide Produktpfade; feste/innere Faktorverluste diagnostiziert, nur unverbundene Schlussreserve ausgelassen. Zusammenhängendes Falk-Schema auf Android/Desktop.
- Gemeinsamer Compose-Formeleditor und Konfigurationsplan; versionierte Originalzeichen-/Radiergesten bleiben vom AST getrennt. Automatische Handschrift-/Fotoerkennung ist sichtbar Beta; keine Fotopersistenz in Alpha.

## Bestätigter Aufbau

- Android-Anwendung: `app`; Einstieg über `MainActivity` und `MathematikAtlasApp`.
- Fachneutraler Graph und Editor: `KnotenKartenVerwalter`.
- Mathematischer Rechenkern: `MathematikRechenSystem`.
- Graph-/Rechenkernadapter: `MathematikKartenAdapter`.
- Knotenvorlagen, Auswerter, Renderer und mathematische Kartenmigration: `MathematikKnoten`.
- Der Android-Speicher liegt unter dem app-internen Pfad `MathematikAtlas/karten/<karten-id>/v<version>.json`; vorhandene Dateien werden vor dem Überschreiben zusätzlich unter `MathematikAtlas/sicherungen/` gesichert.
- Kotlin-JVM-Tests sind vorhanden. Android-Instrumentierungstestquellen fehlen trotz vorhandener Testabhängigkeiten; manuelle Emulatornachweise ersetzen keine automatisierte Gestenabdeckung.

## Bestätigte Stärken

- Repository-, Architektur- und Versionsfolgeprüfungen bestehen; 3.0.1 verwendet die vorhandene Reservierung. 3.1.0 wurde erst nach Veröffentlichung und grüner Master-CI des Vorgängers reserviert.
- Android-Debug-Build, Lint und vollständiger JVM-/Desktoplauf bestehen nach Registrykonsolidierung. Weitere Korrekturen müssen gezielt erneut geprüft werden.
- Die 15 ausgelieferten Standardkarten werden strukturell geprüft und durch den echten Auswerter ohne gemeldete Fehler ausgewertet.
- Repräsentative Tests decken Zahlenrechnung, Mengen, Aussagenlogik, Methoden/Analysis, Vektoren und Matrizen einschließlich fachlicher Randfälle ab.
- Editor-Unit-Tests decken Verbindungskompatibilität, belegte Eingänge, dynamische Anschlüsse, Kopieren, Löschen, Mehrfachaktionen sowie Undo/Redo ab.
- Der neue Querschnittstest verhindert sichtbare mathematische Knotenarten ohne Auswertungspfad.

## Mengenvisualisierung und Orbitmengen in Abschlussprüfung

- Die Mengenvisualisierung trennt exakte R1-Algebra, rationale Zellnachweise und
  numerische Punktvorschau. Enthaltene, gemischte und unbekannte Bereiche besitzen
  getrennte Status; unbekannte Bereiche bleiben in R1, R2, R3 und C sichtbar.
- Exakte rationale Grenzen und offene Endpunkte werden erst nach dem
  Fensterbeschnitt in Zeichenkoordinaten umgewandelt. N beginnt bei 1;
  Zeichentoleranzen verändern weder Ganzzahligkeit noch Mengentopologie.
- Feste Schnitte binden nicht dargestellte Variablen ausdrücklich. Eine optionale
  Farbdimension kann mehrere Werte pro Bildpunkt erhalten und markiert diese Fälle.
- Der Sampler arbeitet auf `Dispatchers.Default`, prüft den Coroutine-Abbruch und
  begrenzt Raster, Zertifikate und Orbits über separate Laufzeitbudgets. Gespeicherte Qualitätswerte bleiben beim Laden erhalten.
- `OrbitFamilie` und `OrbitBeschraenktheitsMenge` liegen im Compose-freien Kern.
  Allgemeine endliche Nichtflucht bleibt unbestimmt; exakte Zyklen beweisen
  Beschränktheit. Für die erkannte Familie `z²+c` mit Nullstart kommen sichere
  Kardioiden-, Periode-2- und Fluchtnachweise hinzu.
- Die neue Mandelbrot-Beispielkarte besteht aus regulären Variablen-, Potenz-,
  Additions-, Methoden-, Orbit-, Mengen- und Visualisierungsknoten. Historische
  Standardkartenmigrationen bleiben auf die bisherigen fünf Beispielkarten
  begrenzt; die neue Karte wird als Standardkarten-Asset im Ordner
  `Standardkarten/Dynamische Systeme/01 Iterationsmengen` installiert.
- Die 28 konkreten Zahltypen und 42 konkreten Mengentypen sind mit ihrem aktuellen
  Visualisierungsstatus in `plans/active/2026-10-01-cas-abdeckungsmatrix.md`
  dokumentiert. Nicht räumliche und nicht entscheidbare Fälle werden nicht als
  leere Mengen ausgegeben.
- Gezielte Kern-, Knoten-, Katalog-, Visualisierungs-, Beispielkarten- und
  JSON-Roundtriptests sowie der vollständige JVM-Testlauf und Debug-Build bestehen.
  Die unabhängige Knotenverifikation ist abgeschlossen; Veröffentlichung und grüne PR-/Master-Prüfungen sind durch PR#444 und PR#445 nachgewiesen.
- Ein isolierter API36-Emulator ist verfügbar. Erste Touch-/Inspector- und Lebenszyklusnachweise liegen vor; die hier dokumentierten Laufzeitnachweise ersetzen keine physische Geräteabnahme.

## Offene, priorisierte Android-Befunde

1. **Mittel – Start und Speichern können weiterhin umfangreiche Arbeit auf dem UI-Thread ausführen.** Importvalidierung, Transaktion und Listenaktualisierung laufen inzwischen auf Dispatchers.IO; automatische Speicherung wird während des Imports zurückgehalten. Verzögerungen sind noch nicht vollständig vermessen.
2. **Prüfgrenze – Navigation und alle Lebenszyklusübergänge.** Die aktive Karte wird aus gespeichertem Verweis wiederhergestellt und überlebt im Emulator einen Prozessneustart. Bibliothekswechsel, Unterkarte per Doppel-Tap, Brotkrumenrückkehr, Hintergrundwechsel und Neustart der aktiven Unterkarte sind im Emulator geprüft. Automatisierte Instrumentierungsabdeckung besteht weiterhin nicht.
3. **Prüfgrenze – Touch-Gesten.** Manuelle Emulatornachweise bestehen, Android-Instrumentierungstests fehlen weiterhin.
4. **Releaseintegration.** Operatordefinitionen, Signaturaliase, gemeinsamer Konfigurator, Verbrauchervereinheitlichung und explizite Fachauswerterkomposition sind implementiert und unabhängig ohne weitere Blocker geprüft. Der3.1.0-Umfang ist implementiert, unabhängig geprüft und als PR#445 veröffentlicht. Die grünen Release-PRs und der dokumentierte Git-Gleichstand schließen die Alpha-Konsolidierung ab.

## Abstraktionswerkzeug und Speicherstabilisierung

- Der fachneutrale Kartenkern besitzt eine gemeinsame Teilgraphanalyse sowie eine
  snapshot-validierte, atomare `TeilgraphErsetzen`-Aktion mit Undo/Redo.
- Das neue Abstraktionsregister erkennt beweisbar polynomiale Termgraphen und
  ersetzt sichere Kandidaten durch die vorhandenen Tupel- und Polynomknoten.
  Extern verwendete Zwischenknoten werden nicht gelöscht; nachgeschaltete
  `TermZuMethode`-Schnittstellen bleiben erhalten.
- Der App-Dialog sortiert positive Einsparungen deterministisch, kann Kandidaten
  im Graph markieren und analysiert nach jeder semantischen Kartenänderung neu.
- `.matlas` kann nun mit Größen-, Pfad-, Manifest- und Prüfsummenvalidierung
  importiert werden. Importfehler werden sichtbar behandelt; beschädigte neueste
  Kartenversionen fallen beim Listen und Laden auf die jüngste lesbare Version
  zurück.
- Ein Kartenwechsel und `ON_STOP` brechen nur die flüchtige Verbindungsvorschau
  ab und sichern danach den aktuellen Kartenstand.

## Kartenherkunft und Rechneroperatoren in v3.0.1

- Die Kartenbibliothek löst Herkunft mit der Priorität Standardprovenienz,
  Freigabequelle und app-lokaler Fremdimport auf und filtert eigene,
  vordefinierte und Fremdnutzerkarten samt ihren relevanten Ordnern.
- Datei- und HTTPS-Import verwenden denselben JSON-/Freigabepaket-/`.matlas`-
  Erkennungspfad. HTTPS bleibt auf sichere Weiterleitungen, Zeitlimits und
  höchstens 16 MiB begrenzt; ein Importjournal stellt gemeinsam veränderte
  Bibliotheksdateien nach Fehler oder Prozessabbruch wieder her.
- `RechnerOperatorRegistry` beschreibt Zahlen-, Vektor-, Matrix- und
  Tensoroperatoren mit Signaturen, Ports, Parametern, Fähigkeiten und Status.
  Der Operator-Suchindex und die abschließenden Familien-Dispatcher verwenden
  diese Registry; unbekannte persistierte IDs werden nicht umgeschrieben.

## Operandenreihenfolge im Arbeitsstand (2026-10-12)

- Redundante Matrix- und Aussagenoperationen werden beim Laden/Import über den gemeinsamen mathematischen Codec in vorhandene Rechner überführt; bisherige Zahlen-, Mengen-, Vektor- und Tensormigrationen bleiben eingebunden.
- Assoziative Operatoren beschreiben variadische Rollen ausdrücklich. Gespeicherte Anschlussreihenfolge steuert Auswertung, Formel und Falk-Schema.
- Android und Desktop verwenden dieselbe vertikale Operandenliste mit Ziehgriff und Auf-/Ab-Schaltflächen. Umordnung bewahrt IDs, Verbindungen und freie Schlussreserve und bildet einen Undo-Schritt.
- Keine neue Knotenart, Produktionsabhängigkeit, JSON-Formatversion oder veröffentlichte Produktversion. Siehe ADR 0044 und den ExecPlan vom 2026-10-12.

## Dokumentationszustand

- Die Dateien unter `docs/codex/plans/active/` sind keine verlässliche Liste laufender Arbeiten: Mehrere Einträge bezeichnen bereits veröffentlichte Versionen oder nennen sich selbst abgeschlossen.
- Architektur- und Releaseentscheidungen müssen weiterhin gegen Code, `release/roadmap.toml` und Git geprüft werden.
- Der abgeschlossene Audit enthält umsetzungsreife Reparaturpakete und klare Abnahmekriterien; er verändert noch kein Produktionsverhalten und keine Persistenzversion.

## Nachprüfung des separaten Umkehrmethoden-PR

[PR#436: Review auf3.1.0](https://github.com/TeutonStudio/MathematikAtlas/pull/436#issuecomment-6115383718). Der isolierte Zwei-Dateien-Patch ist konfliktfrei anwendbar. Die ursprünglichen fünf Tests scheitern am Variablennamen `null`; nach temporärer Umbenennung in `nullWert` sind sie grün. Die Einbindung des neuen Inversetyps in den aktuellen Methodenaufruf benötigt gesonderte Arbeit. Keine Umschreibung des alten Branches und keine Übernahme der Umkehrmethoden in Alpha.


## Mengenoperatorgrundlage – lokaler Arbeitsstand 2026-10-12

- Die zehn bestehenden Mengenquellen besitzen `MengenKonstante`-Definitionen im
  Kern mit stabilen Auswahlkennungen und nullstelligen `OperatorSignatur`-Verträgen.
  Mengenknoten und Mengenrechner nutzen dieselbe App-Dialogvorlage; mathematische
  Definitionen werden im Dialog mit `LatexText` angezeigt. Eigene versionsfeste
  Mengenkarten bleiben erhalten; unbekannte Quellen liefern keinen Ersatzwert.
- Mitgliedschaft, Inklusion, Gleichheit, Disjunktheit und sichere Mengenalgebra
  berücksichtigen N0, Primzahlen und gaußsche Mengen. Exakte Primzahltests haben
  eine Grenze von 10000 ungeraden Divisorkandidaten; danach bleibt die Aussage
  unbekannt. Kardinalitäts- und explizite Topologiepfade nutzen dieselbe Grundlage.
- Vollständiger Gradle-Testlauf, Android-Debug-Build, Desktop-Kompilierung sowie
  Repository-/Releaseplan-/Versionsfolgeprüfungen erfolgreich. Aktuelle Berichte:
  1447 Tests ohne Fehler/Überspringungen. Unabhängige statische Abnahme ohne Blocker.
- Keine neue Veröffentlichung; Änderungen uncommittet auf dem bestehenden
  Arbeitsbranch. Keine Dialog-Laufzeitprüfung mangels verbundenem Android-Gerät
  oder eingerichtetem AVD. Eigenständige Kern-Skriptprüfung benötigt fehlendes
  `kotlinc`; Gradle-Kernprüfung erfolgreich. Der DesktopInspektor besitzt weiterhin
  keinen Mengen-Auswahldialog. Details im abgeschlossenen
  [ExecPlan](plans/completed/2026-10-12-mengenoperatorgrundlage.md).


## Visuelle Gruppen – lokaler Implementierungsstand 2026-10-12

- Gemeinsamer Android-/Desktopeditor unterstützt verschachtelte Gruppen, Inspector-Faltung, abgeleitete Grenzanschlüsse und separat navigierbare Gruppenansichten auf demselben kanonischen Graphen. Auflösen erhält Inhalte; Ausschneiden entfernt den gewählten Teilbaum atomar. Undo/Redo, Kopieren, JSON/.matlas und eigene Gruppenkameras sind berücksichtigt.
- JSON-Format 9 ergänzt Faltzustand, Elternreferenz und Gruppenkamera; alte flache Gruppen werden beim Laden räumlich migriert. Keine neue registrierte Knotenart oder Produktionsabhängigkeit.
- Projektion wird zwischengespeichert, bei großen Karten im Hintergrund berechnet und bei überholtem Zustand verworfen. Eingeklappte Inhalte werden nach dem Übergang nicht weiter komponiert; Anschlüsse und Verbindungen verwenden Viewport-Culling.
- Desktop-Smoke-Test bestätigte Gruppenbaum, Inspector, Faltung, Gruppenansicht und Auswahl des Originalknotens über einen abgeleiteten Ausgang. JVM-Skalierungstest deckt 100/500/1000 Knoten und bis knapp 2000 Verbindungen ab. Android-Framezeiten sind mangels angeschlossenem Gerät/Emulator noch nicht gemessen.
- Architekturentscheidung: [ADR 0046](decisions/0046-visuelle-gruppenprojektion.md). Prüfdetails und Grenzen: [ExecPlan](plans/active/2026-10-12-visuelle-gruppen-faltung.md). Lokaler Arbeitsstand, kein Release oder neuer Commit.


## Einheitliches Dialogsystem im Arbeitsstand (nicht veröffentlicht)

- Gemeinsamer fachneutraler Compose-Dialograhmen mit festen Abständen für Kopf,
  Untertitel, Inhalt, Abschnitte, Einträge und umbrechende Aktionen. Android,
  Desktop und gemeinsame Formeleditoren verwenden dieselben Bausteine.
- Alle gefundenen direkten Compose-Dialogaufrufe liegen jetzt im gemeinsamen
  Rahmen; native Dateidialoge bleiben unverändert. Titel und Aktionen konkurrieren
  nicht um die Textbreite. Kopf und Fuß sind bei Überlänge begrenzt scrollbar.
- Gesamtprüfung aus JVM-Tests, Android-Debug-Build/Lint und Desktop-Kompilierung
  erfolgreich. Grafischer Desktoptest prüft zwölf Größen-/Schrift-/Themevarianten,
  Scrollen bis zum letzten Eintrag und aktiviertes/deaktiviertes Außenklickverhalten.
- Keine Android-Laufzeitabnahme: ADB meldet kein Gerät und es ist keine AVD installiert.
  Einzelheiten und Abnahmegrenzen stehen im
  [ExecPlan](plans/active/2026-10-12-einheitliches-dialogsystem.md) und in ADR 0047.


## Releasekandidat v3.1.1 – Abschlussprüfung 2026-10-12

- Der vollständige Releaseumfang umfasst Rechnerkonsolidierung und Operandenreihenfolge, Mengenoperatorgrundlage und Prädikatsauswertung, Gruppenprojektion mit Kartenformat 9 sowie gemeinsame Dialoge. Der Erstellenkatalog reduziert sich von 138 auf 128 Typen; es entsteht kein neuer Knotentyp (Versionsachse x).
- Android-Metadaten sind 3.1.1/3001001; Desktop-Paketmetadaten und Workflow-Artefaktname stimmen überein. Bestätigte GitHub-master-Basis ist e8f1c9d6 nach dem finalen v3.1.0-Commit 811955f8.
- Der Rechenweg-Desktoptest verwendet Compose-Semantik statt nativer AWT-Window-/Robotannahmen und prüft Öffnen, Zurück sowie das Verwerfen veralteter Details nach Ergebniswechsel. Der gezielte Test und der anschließende vollständige Gradle-Testlauf samt Android-Debug-Build sind erfolgreich. Die zusätzliche Compose-Testabhängigkeit ist auf den Desktop-Testclasspath beschränkt.
- Repository-, Architektur-, Methodenmodell-, Releaseplan-, Versionsfolgeprüfung und 14 Guard-Regressionen erfolgreich. Android-Release-Build erfolgreich, bisher ohne konfigurierte Produktionssignatur. Keine Android-Geräte-/Emulatorabnahme. Die separate RPM-Paketierung scheitert lokal an jpackage (unsupported type rpm); sie ist kein APK-Artefakt.
- Dies ist ein vorbereiteter Release-PR-Stand. Veröffentlichung und finale Commit-SHA sind ausschließlich anhand von GitHub-master und GitHub Release zu bestimmen.


## Rechner-Auswertungsschemata im lokalen Arbeitsstand

- Android-Rechnerinspektoren, dynamische Konzeptreiter und Desktop-Inspector öffnen gemeinsame Schema-Inhalte. Spezialdefinitionen von Formelmodi, Quadrat und Kubik bleiben erhalten.
- Matrixketten zeigen jedes Zwischenprodukt einmal. Ergebniszellen wählen Zeile/Spalte; auswählbare geordnete Einzelprodukte markieren die zugehörigen Faktoren. Ergebniswechsel setzt die atomare Auswahl zurück.
- Strukturwerte bieten achsenweise Komponentenauswahl und farblich zugeordnete Eingabe-/Ergebniskomponenten; Addition zeigt deren tatsächliche Summe. Methoden zeigen vorhandene Vorschriften, gemeinsame Parameter, Definitionsraum und Ergebnisannahmen. Methodenwertige Matrixprodukte verwenden den Methodenpfad.
- Registry-Abdeckung, Unbekannt-Diagnose, Tensorindizes, Zahlenformelparameter und Methodenrouting sind durch Modelltests geprüft; Compose prüft Auswahlwechsel auf P2 und Ergebniswechsel. Vollständiger Gradle-Testlauf, Android-Debug-Build/Lint, Desktop-Kompilierung und Repositoryprüfung erfolgreich. Unabhängige Diffprüfung ohne verbleibende Blocker.
- Keine Android-Laufzeitabnahme: ADB meldet keine Geräte. Keine visuelle Abnahme großer Schrift oder Themevarianten. Zusätzlicher direkter Kern-Smoke-Test mangels kotlinc/java im PATH nicht ausführbar; Gradle-Kerntests erfolgreich. Kein Release oder Commit. Details: [ExecPlan](plans/active/2026-10-12-rechner-auswertungsschemata.md), [ADR 0048](decisions/0048-rechner-auswertungsschemata.md).


## Polarkoordinaten im Arbeitsstand (2026-10-12)

- Bestehender Übersetzer unterstützt kartesisch↔polar für reelle Tupel; der Vektorrechner bietet Radius, Winkeltupel/Vorzeichen und kartesische Koordinaten aus Radius plus Winkeln.
- Gradkonvention: Azimut zuerst in [0°,360°), weitere Polarwinkel zu x₃,…,xₙ in [0°,180°]. R¹ verwendet Betrag und skalares Vorzeichen; flache Übersetzertupel benötigen für die R¹-Rückrichtung den gespeicherten Schalter.
- Numerische Umrechnung liegt gemeinsam im Rechenkern. Graphdimensionen, Formelpfad, stabile Übersetzeranschlüsse, JSON und Undo/Redo sind getestet; keine neue Knotenart, Produktionsabhängigkeit oder Formatversion.
- Vollständiger Gradle-Testlauf, Android-Debug-Build, Desktopkompilierung und Repositoryprüfung erfolgreich; unabhängige Verifikation ohne Blocker. Keine manuelle Geräteabnahme: ADB meldet kein verbundenes Gerät. Zusätzliches Kernskript benötigt ein fehlendes separates kotlinc; Gradle-Kerntests bestehen.
- Details und Prüfgrenzen: [ExecPlan Polarkoordinaten](plans/completed/2026-10-12-polarkoordinaten.md), ADR 0049. Keine Commits oder Veröffentlichung durch diese Aufgabe.


## Gradmenge und Definitionskomposition – lokaler Arbeitsstand 2026-10-12

- Die separate Gradmenge mit Winkeltyp identifiziert 360° und 0°. R↔Grad im bestehenden Übersetzer verwendet Bogenmaß und wählbare Hauptvertreter; der Inspector zeigt die bestehende Mengenintervallnotation. Allgemeine Parameter können explizit Winkel sein. Polarwinkel sind nun Winkelwerte, historische numerische Gradrepräsentanten bleiben rückwärtskompatibel.
- Der bestehende Zahlenrechner besitzt eine Grad-Sinus-Signatur aus dem gemeinsamen Register. Die reelle Sinusdefinition ist der ausführbare Graph q→Grad-Sinus→Kartenausgang; die geometrische Grunddefinition vermeidet einen Definitionszyklus. Die bisherigen numerischen/komplexen Auswertungsmodi bleiben erhalten.
- Tupel behalten gelieferte Komponentenobermengen und deren Vereinigung als gemeinsame Grundmenge. Strukturübersetzungen, Abflachen und leere Tupel berücksichtigen diese Metadaten; Tupel, Zeile und Spalte bleiben getrennte Strukturtypen.
- [Definitionsanalyse](DEFINITIONSKARTEN_ANALYSE.md): 465 konfigurierte Einträge und 131 statische Definitionsassets, davon 113 mit Regelknoten und 18 ohne. Weitere Zerlegungen sind priorisierte Vorschläge, noch nicht umgesetzt.
- 1525 Tests ohne Fehler/Überspringen, Android-Debug-Build und Desktopkompilierung erfolgreich. Repository-, Releaseplan- und Versionsfolgeprüfung erfolgreich. Unabhängige Produktcodefreigabe; keine manuelle Geräte-/UI-Abnahme. Separates Kernskript benötigt fehlende Werkzeuge, Gradle-Kerntests bestehen. Keine neue Version, Knotenart, Abhängigkeit oder JSON-Formatversion; kein Commit/Release.
- Nachweise: [ExecPlan](plans/completed/2026-10-12-gradmenge-definitionen.md), [ADR 0050](decisions/0050-gradmenge-und-definitionskomposition.md).

## Vektorpotenz, Unterraum und Geometrie – lokaler Arbeitsstand 2026-10-12

- Bestehender Vektorrechner besitzt Vektorpotenz mit nichtnegativem ganzzahligen Exponenten, ausgewähltem Skalarprodukt und paritätsabhängigem Zahl-/Vektorergebnis. p=0/1 berechnen kein Selbstprodukt. Tupel werden ausdrücklich als Spalten interpretiert.
- Bestehender Mengenrechner bildet linearen/affinen Spann als argumentabhängige Kernmenge. Rationale Rang-/Mitgliedschaftsprüfungen sind exakt; symbolische Pivots bleiben unbestimmt.
- Bestehender Übersetzer bietet Geometriekoordinatenmengen, Objektpunkte, Mittelpunkte und erkannte geometrische Rückrichtungen. Reellheit, Raum-/Dimensionsverträglichkeit, Ankerzugehörigkeit und symbolische Grenzen werden berücksichtigt.
- Skalarproduktprüfung akzeptiert bloße Referenztexte nicht mehr als Beweis. Nicht nachgewiesene eigene Produkte führen eine ausdrückliche Annahme weiter; widerlegte Produkte werden abgewiesen. Android und Desktop bieten Produktauswahl und Potenzvorgabe.
- Vollständiger Gradle-Testlauf erfolgreich: 319 Testsuiten, 1536 Tests ohne Fehler. Android-Debug-APK und Desktopkompilierung sowie Android-Lint erfolgreich; unabhängige Prüfung ohne verbleibende Blocker. Separates Kernskript benötigt fehlendes kotlinc, Gradle-Kerntests bestehen. Keine Geräteabnahme, kein Commit oder Release. Reservierte x-Version 3.1.2, Produktversion unverändert 3.1.1. Details: [ExecPlan](plans/completed/2026-10-12-vektorpotenz-unterraum-geometrie.md), [ADR 0051](decisions/0051-vektorpotenz-unterraum-geometrie.md).


## Zertifizierte Polynomlösungen im Arbeitsstand (2026-10-12)

- Der gemeinsame Langdruckdialog bietet analytische und numerische Tabs für erkannte Polynomgleichungen und Eigenwertanfragen. n=0..100 bezeichnet die ersten Dezimalziffern ohne Rundung; Konfiguration wird über vorhandene Kartenaktionen gespeichert.
- Reelle rationale Polynome verwenden Sturm-Isolation, komplex-rationale Polynome lokale Rouché-Scheiben mit unabhängig prüfbarer Einschließung. Existenz, Vollständigkeit, Dimension, Vielfachheit und Ziffernnachweis werden getrennt behandelt; fehlende Nachweise bleiben offen.
- Vorhandene Variablenknoten können quadratische Matrixunbekannte als geordnete Eintragsvariablen erzeugen. Matrix-/Vektorgleichheiten werden gemeinsam komponentenweise gelöst. Lineare Systeme liefern exakte Familien samt numerisch auswählbaren Mitgliedern; nichtlineare Systeme liefern exakt geprüfte Mitglieder oder lokale Krawczyk-Nachweise.
- Analytische Darstellungen bis Grad vier enthalten Zweigbedingungen. Eigenwerte rational-komplexer Matrizen bis Dimension 8 verwenden Faddeev–LeVerrier und denselben Wurzellöser. Verfahrensgrenzen und Architekturentscheidung: ADR 0052; vollständiger Nachweis im abgeschlossenen ExecPlan 2026-10-12-zertifizierte-polynomloesungen.
- Finaler Gesamtprüflauf: 1557 Tests ohne Fehler oder Überspringen, Android-Debug-Build erfolgreich. Zwei neue Desktop-Compose-Laufzeittests prüfen Tabs, Genauigkeit und Familienparameter. Repository-/Architekturprüfung, Releaseplan und Versionsfolge erfolgreich; unabhängige Abschlussprüfung ohne offene Findings.
- Prüfgrenzen: kein Android-Gerät/Emulator verbunden, daher keine Android-Gestenprüfung. Das zusätzliche Kernskript benötigt kotlinc, das im PATH fehlt; Kern-Gradletests bestehen. Keine neue Knotenart, Abhängigkeit oder Kartenformatversion; keine Veröffentlichung oder neuer Commit.

## Integrierter Stand v3.2.0 (Releaseprüfung, 2026-10-13)

TODO 22b56645 und master 7e11b711 sind im isolierten Arbeitsbereich dreiseitig integriert. Strukturierte Winkelparameter erhalten gemeinsame Herkunft, Wn-Domäne und öffentliche Argumentpackung bis zur Bildmenge. Polarkonvention wird explizit gespeichert und historisch migriert. Produktgitter beziehungsweise deterministische Halton-Abtastung berücksichtigen alle Winkelachsen und diskreten Vorzeichen; Ausgabekoordinaten werden erst nach vollständiger Auswertung projiziert. Das Indexdefinitionsinventar ist leer; symbolische Zerlegungsrezepte liefern ausführbare Kandidatenbedingungen mit ausgewiesenen Materialisierungsgrenzen.

Vollständige lokale Gradle-Tests, Android-Debug-Build, Lint, Desktop-Kompilierung und acht gezielte Desktop-Compose-Regressionen erfolgreich. Android-Instrumentierung im isolierten API35-CI-Emulator erfolgreich (PR #449, Lauf 38400142819); sämtliche PR-Prüfungen am Produkt-HEAD ae1669fc grün. Lokale isolierte Emulatorstarts scheitern im nativen Emulatorprozess; kein physisches Gerät verbunden. Der Releaseabschluss erfolgt als geprüfter Squash v3.2.0.

## Variablengrundraum und Tupelinspector (Arbeitsstand 2026-10-13)

- Variablen besitzen einen optionalen Mengeneingang `grundraum`; verbunden ersetzt er den Inspectorbereich einschließlich Intervall. Ohne Verbindung gilt der bisherige Bereich. Strukturierte Variablen verwenden ihn als Komponentenbereich; Winkelstrukturen behalten ihren festen Winkelraumvertrag.
- Der gemeinsame mathematische Codec ergänzt den Eingang beim Laden/Import und Schreiben idempotent, ohne Ausgangsreferenzen zu ändern.
- Android bietet für Tupel rationale Standardwerte und die vorhandene Operandenliste mit Ziehgriff und Auf-/Ab-Schaltflächen. Umordnung und Längenänderung erhalten bestehende Anschlussidentitäten und Reihenfolge und sind rückgängig machbar.
- Vollständiger Gradle-Testlauf und Android-Debug-Build erfolgreich; unabhängige Diffprüfung ohne verbliebene konkrete Findings. Keine lokale Geräteprüfung dieser Änderungen. v3.2.1 wird als Teilumfang auf dem SamAI-Branch vorbereitet; bis der finale Versionscommit auf `master` liegt, gilt sie nicht als veröffentlicht.

## Grad-Richtungsraum und Bildmenge (Arbeitsstand 2026-10-13)

- Gradmenge ohne `dim` bleibt die Winkelgruppe G. Mit `dim=d` (1–65) liefert sie den signierten Richtungsraum W_(d−1): ein Vorzeichen und d−1 Halbkreiswinkel. Diese Korrektur ersetzt die unveröffentlichte G^d-Erweiterung.
- Ein verbundener kanonischer Richtungsraum bestimmt die Winkelvariablendimension. Nachgewiesene endliche Richtungsprodukte der Definitionskarte werden beim strukturierten Verbraucher gepackt; allgemeine Funktionsprodukte bleiben unverändert.
- Die Grad-Definitionskarte verwendet die Fallunterscheidungsgruppe des Nutzers mit endlichen Indizes {0,…,d−1} und [0,180)-Winkelvertretern. Der Definitionsresolver übernimmt eine konkret angeschlossene Dimension.
- Der historische Tupel-zu-Vektor-Adapter erhält Variablenquellen und Bereichsmetadaten; dadurch kann Term→Methode die strukturierte Winkelquelle nach einem Matrixprodukt binden.
- Regressionen prüfen Dimensionen 1–4 sowie eine vollständige gespeicherte Definitions-/Diagonalmatrix-/Bildmengenkarte bis zum Sampler. Tensorstufe bleibt Tensorgrad; die Koordinatenanzahl muss ausdrücklich als Dimension verwendet werden.
- Gesamtprüflauf `./gradlew test :app:assembleDebug` erfolgreich; damalige Gradle-XML-Berichte: 1664 Tests ohne Fehler oder Überspringungen. Repository-/Releaseplan-/Versionsfolgeprüfung erfolgreich; unabhängige Codeabnahme ohne Blocker. Keine Geräteprüfung. Anschließend in Branchcommit `8465f6c9` integriert; nicht auf `master` veröffentlicht. Details im abgeschlossenen Plan `2026-10-13-grad-bildmenge.md`.


## Methoden-, Tupel- und Formelvertrag – implementierter Teilstand 2026-10-13

- Neue kanonische Methodenaufrufe liefern Tupelergebnis plus offene Voraussetzungen. Explizite Bindungs-IDs, gespeicherte Komponentenidentitäten, reduzierte partielle Methoden und mehrdimensionale Tupelkomposition sind ergänzt. Aufrufbedingungen bleiben über verschachtelte Vorschriften, Voraussetzungen und Methodenkopien erhalten.
- Formelgraph überträgt Bedingungen und optionalen Bindungskontext. Strukturcodec v3 verwendet typisierte Literale; Strukturformel-, Formelkontext-, Wert- und Wertpaketcodec akzeptieren ausschließlich ihre aktuelle Wire-Version und weisen ältere Payloads ab. Der Zwischenablagevertrag für übertragene Werte lautet `ATLAS-WERT-2`. Kontextcodec bewahrt Quellen, vollständige Typen, Methodensignaturen und Annahmen. Deklarierte Methodenvariablen lassen sich im Grundparser aufrufen. Nicht registrierte Wertvarianten werden ausdrücklich abgewiesen.
- Tupelkonkatenation unterstützt null/einen Operanden und leere Ergänzung. Mehrkomponentige Kartengrenzen erhalten gespeicherte Reihenfolgen. Neue Kernoperatoren ergänzen rational-affine Inversion, partielle Ableitung, Polynomintegration und begrenzte numerische Jacobi-Matrizen, lokale Inversion und bestimmte Quadratur; numerische Ergebnisse sind Approximationen.
- Finaler damaliger Gesamtprüflauf `./gradlew test :app:assembleDebug :app:lintDebug :desktopApp:compileKotlin` erfolgreich; 1689 Tests laut XML-Berichten ohne Fehler oder Überspringungen. Repository-, Releaseplan-, Versionsfolge- und Diffprüfung erfolgreich. Separates Kernskript wegen fehlendem standalone kotlinc nicht ausführbar. Keine UI-/Gerätelaufzeitabnahme. Anschließend in Branchcommit `8465f6c9` integriert; nicht auf `master` veröffentlicht.
- **Der Gesamtvertrag ist nicht abgeschlossen.** Die gemeinsame UI-Integration ist nachfolgend dokumentiert; alle Wertvarianten, sämtliche Binder/Operatorfamilien und die vollständige Ablösung der Legacy-Pfade bleiben offen. Einzelmatrix und Nachweise: [aktiver ExecPlan](plans/active/2026-10-13-methoden-tupel-formelvertrag.md); Architekturentscheidung: [Bindungen und Codecs](decisions/2026-10-13-methodenbindungen-und-formelcodecs.md).

## v3.2.1 Releasekandidat (Arbeitsstand 2026-10-14)

- Reservierter Änderungsumfang auf Basis v3.2.0 (`dc072189`); es kommen keine neuen registrierten Knotentypen hinzu. Der Gesamtvertrag mit 77 Prüfpunkten bleibt Folgearbeit.
- Formel-/Methoden-/Tupelteilumfang enthält deklarationsvorrangige Methodenaufrufe, verschachtelte methodenwertige Ergebnisse, nullstellige und mehrwertige Bildmengen sowie aktuelle-only Mathematikcodecs ohne Rückwärtskompatibilität.
- Android-Metadaten und Roadmap auf 3.2.1/3002001 gestellt. Lokale Repositoryprüfungen und vollständiger Gradle-Lauf sind nach den letzten Änderungen erfolgreich; PR-CI, Android-Emulator-/Desktop-Abnahme und PR-Freigabe stehen noch aus.


### Übersetzungszeugnisse (Arbeitsstand 13.10.2026)

- Gemeinsamer Kernvertrag `ÜbersetzungsAnfrage`/`ÜbersetzungsErgebnis`/`ÜbersetzungsZeugnis`: unabhängige Abbildungseigenschaften, Informationserhaltung und Rechengenauigkeit; strukturierte Nichtdefiniertheit und konservative offene Spezialfälle.
- Alle 14 bestehenden Übersetzerziele, Abflachen, frühe Index-/Definitionsbausteinrückgaben und historische Struktur-/Termübersetzer werden zentral bezeugt. Zielträger und symbolischer exakter Bildraum bleiben getrennt. Heterogene Komponentenrestriktionen und stabile Quellen bleiben erhalten; Abflachungspläne rekonstruieren auch leere Untertupel ausdrücklich.
- Formel-Literale, Graphkonvertierung, mathematische Methoden und selbsttragende Exportwerte führen Zeugnisse/Quellen/Bedingungen mit. Nachfolgende Berechnungen unterscheiden aktuelle Zeugnisse von Herkunft. Unbekannte Regelversionen bleiben mit ungeprüften Behauptungen erhalten und gelten nicht als bestätigte Nachweise.
- Gemeinsames Auswertungsschema zeigt die vollständigen Zeugnisdetails; Desktop-Compose-Prüfung umfasst schmale/niedrige Fenster, helle/dunkle Darstellung und 1x/2x Schrift. Android-Gerätetest ist ergänzt und kompiliert, aber mangels verbundenem Gerät und AVD noch nicht ausgeführt.
- Gesamttests, Android-Debug-Build, Test-APK, Lint und Desktop-Kompilierung wurden erfolgreich ausgeführt. Repository-, Releaseplan- und Versionsfolgeprüfungen sind grün. Die zusätzliche Standalone-Kernprüfung ist wegen fehlendem `kotlinc` nicht ausführbar; Gradle-Kerntests sind grün.
- Unabhängige statische Prüfung und gezielte Nachprüfung durch `node_verifier`: gefundene Rückführungs-, Bildraumpfad-, Literalbedingungs- und Quellenfehler korrigiert; keine weiteren Blocker in der gezielten Nachprüfung. Dies ist keine Freigabe sämtlicher 77 Punkte des übergeordneten Vertrags.
- Nachweisübersicht: [ExecPlan Übersetzungszeugnisse](plans/active/2026-10-13-uebersetzungszeugnisse.md), [ADR](decisions/2026-10-13-uebersetzungszeugnisse.md). Kein neuer Knotentyp, keine Produktionsabhängigkeit oder Veröffentlichung.
Erster PR-Head e66e0c92 ist wegen eines Desktop-Bildvergleichs im Push-Build
gesperrt (PR-Build und isolierte 68 Androidfälle erfolgreich). Codebelegte
Sampling-/Zeichenpaket-Barriere eng korrigiert; 17 Bedienungstests und
unabhängiger Nachreview grün. Erneute volle lokale Abnahme grün: 3 min 34 s,
277 Tasks, 2256/0/1. CI-Abnahme am korrigierten Head noch erforderlich;
kein blinder CI-Retry oder Merge.


### Gemeinsame UI-Vertragsintegration (Arbeitsstand 13.10.2026)

- Android und Desktop verwenden denselben gebundenen Formelbauer mit Symbolrollen, vollständigen Typen, Methodensignaturen, strukturierten Bereichen und Voraussetzungen. Noch nicht übernommene Symbol-/Komponentennamen und Typparameter sperren die globale Übernahme. Fehlerhafte Imports und beschädigte gespeicherte Kontexte überschreiben den Ausgangszustand nicht.
- Stabile fachliche Anschlusszuordnungen überleben Kopieren, Laden, Umbenennen und Reihenfolgeänderungen. Formel↔Karte und Methodenbildung verwenden vorhandene Knoten und atomare Kartenaktionen. Partielle Methodenbindung verwendet Komponentenidentitäten. Eigene und angeschlossene freie Quellen einschließlich Annahmen bleiben im aktuellen Ergebnissnapshot erhalten.
- Gemeinsame Ergebnisdetails zeigen Struktur, Signatur, Voraussetzungen, Bereiche, aktuelles Zeugnis und getrennte Herkunft. Selbsttragende Formel-/Wertpakete sind über Zwischenablage austauschbar; Wertcodec v2 ergänzt einen optionalen Formelkontext; alle Wertpaketversionen außer der aktuellen werden abgewiesen. Historische Werte mit nicht rekonstruierbaren Bindungen bleiben als Wert exportierbar, ihr Formeleditorimport wird diagnostiziert. Dateiwege verwenden weiterhin vollständige Karten.
- Rückführung ist ein Modus des bestehenden Übersetzers; frühere Indexparameter beeinflussen andere aktive Übersetzerziele nicht. Visualisierung erlaubt explizite Koordinaten und feste Parameter; verschachtelte Tupel benötigen eine ausdrücklich gewählte Blattprojektion.
- Q mit deklarierter Methodenquelle durchläuft Formel, Karte, Kartencodec, Methodenerzeugung und Anwendung: f(t)=t²,x=3,h=2 ergibt 8, h=0 wird zurückgewiesen. Eine allgemeine automatische symbolische Quotientenkürzung zu 2x+h ist keine neu implementierte Fähigkeit. Nicht unterstützte Binder und Graphknoten liefern Diagnosen.
- Desktop-Compose-Bedienungstests bestehen. Entsprechende Android-Gerätetests sind ergänzt; Android-Gerät/AVD ist nicht verfügbar, daher keine Android-Laufzeitfreigabe. Unabhängige statische Prüfung durch node_verifier meldet nach Korrektur der belegten Identitäts-/Metadatenfehler keine weiteren konkreten Blocker. Keine Gesamtfreigabe aller 77 Vertragsprüfpunkte.
- Nachweise und Grenzen: [UI-ExecPlan](plans/active/2026-10-13-ui-vertragsintegration.md), [ADR](decisions/2026-10-13-ui-formelkontext.md). Kein neuer Knotentyp, keine Produktionsabhängigkeit, kein Commit oder Release.

- Finaler UI-Gesamtprüflauf: 1721 Tests ohne Fehler/Überspringungen; Android-Debug-APK, Test-APK, Lint und Desktop-Kompilierung erfolgreich. Repository-, Releaseplan-, Versionsfolge- und Diffprüfungen grün. Zusätzliche Standalone-Kernprüfung mangels kotlinc nicht ausführbar; Gradle-Kerntests grün.

## Visualisierer-Folgekorrekturen vom 15.10.2026 (lokal, uncommittet)

- Neue Visualisierer besitzen zwei erweiterbare Mengeneingänge und keinen Ausgang. Historische Ausgänge bleiben bis zur expliziten Umstellung mit Verlustvorschau lesbar. Ebenen haben eigene Sichtbarkeit, Legende, Farbe, Deckkraft und fünf Füllarten; Vorschau und Dialog verfeinern progressiv.
- Graphmengen werden gesampelt; Graph-Termmodus verwendet den vorhandenen Übersetzer samt Quellenbindung und eine entsprechend ergänzte Definitionskarte. Zerlegen zeigt sämtliche Komponenten. Mengen-LaTeX normalisiert Doppelstrichargumente und Klammerbefehle; Notiz-Schriftfarbe ist optional persistierbar. Adapteranschlüsse teilen Hauptkörperkontur/Fußzeilenhöhe und verteilen sich nach Bogenlänge.
- Grad-Dimensionsmigration erhält verbundene IDs, entfernt nur unverbundene Duplikate und bewahrt mehrere verbundene Legacy-Eingänge bis zur bestätigten Bereinigung.
- Vollständige JVM-/Desktop-Tests, Android-Lint, Debug-/Test-APK und Desktop-Kompilierung erfolgreich; 14 native Desktop-Visualisierungsprüfungen bestanden. Repository-/Release-/Versionsguards grün. Einzelne JVM-Samplingmessungen einschließlich zweier Ebenen dokumentiert; keine Leistungszusage daraus.
- Kein Android-Gerät und kein eingerichtetes AVD verfügbar. Android-Interaktion und Vorher/Nachher-Frame-/Allokationsvergleich auf SM-X920 bleiben offen; vorheriges R²-60-Hz-Ziel nicht als erreicht bewertet. Details im aktiven ExecPlan und den Messdaten.

Numerische Prüfgrenze: getrennte Ungleichungsrandzüge werden derzeit nur für polynomiale Ausdrücke mit abgetastetem Vorzeichenwechsel erzeugt. Tangentiale beziehungsweise niederdimensionale Nullniveaus (z. B. x²≤0 oder x²>0) behalten die Zell-/Näherungsdarstellung; deren Linienrand ist nicht vollständig rekonstruiert. Unbekannte Zellen werden nicht als bewiesen enthalten ausgegeben.

## Mengenfilter- und Zeichenbereichskorrektur vom 16.10.2026 (lokal, uncommittet)

- Gefilterte Mengen über unterstützten Koordinatenräumen verwenden die bestehende definierte Region mit kanonischer Tupelargumentbindung. Gemeinsame/abhängige Definitionsräume und Voraussetzungen bleiben erhalten; externe freie Bindungen werden diagnostiziert. Numerische Flächen ergänzen unbekannte Blätter lokal, während bewiesene/gemischte Zellen maßgeblich bleiben. Keine globale unbekannte Schraffur über bereits nutzbarer Filterfläche.
- Zell-/Intervall-, Punkt-/Farb- und Randarbeit teilen ein gezähltes Budget. Nicht unterstützte Zertifikate erzeugen kein erfolgloses rekursives Gesamtraster. Plotdiagnose zeigt sichtbaren Bereich, 20 % Berechnungsreserve, Parameterfenster und Arbeit. Keine Kartenformat-/Anschlussänderungen; historischer Visualisiererausgang bleibt erhalten.
- 14 gezielte Filterregressionen bestanden. Vollständige JVM-/Desktop-Tests, Android-Lint, Debug-/Test-APK und Desktop-Kompilierung erfolgreich. Native Desktop-Visualisierersuite und Android-Visualisierersuite jeweils 15 Tests erfolgreich. Das SM-X920 war am 16.10. wieder verfügbar; Android-Prüfungen liefen mit separater temporärer Test-App-ID, da die installierte Anwendung eine andere Signatur besitzt.
- Unabhängige Nachprüfung ohne weitere konkrete Blocker. Neue Messdaten unter `measurements/2026-10-16-tupelfilter-*.json`: p95 auf SM-X920 24,27–28,32 ms; 60-Hz-Ziel bleibt offen. Hohe Qualität verursacht erhebliche kumulative Allokationen, kein Nachweis eines Live-Heaplimits. Kein identischer Filter-Vorherlauf. Details, Prüfpfade und Grenzen im aktiven Visualisierer-ExecPlan.

## v3.4.0 Releasevorbereitung vom 16.10.2026

- Vorhandener Gesamtstand auf Basis des finalen v3.3.1-Commits `c4b23c69` für den beauftragten Masterabschluss abgegrenzt: Methodenquelle/Identität, Variablengrundräume und Migration, Visualisierung sowie Anschluss-/Darstellungskorrekturen. Genau ein neuer erzeugbarer Typ `mathematik.methode`; deshalb Knoten-Version y/3.4.0. Prädikatindexplanung bleibt Vorbereitung.
- Android 3.4.0/3004000, Desktoppaket 3.4.0 und Roadmap-Zielbaum abgestimmt. Finale lokale Repository-, Architektur-, Standardkarten-, Releaseplan-, Versionsfolge- und 14 Guardregressionsprüfungen erfolgreich. Gradle-Gesamtlauf mit Tests, Lint, Debug-/Test-APK und Desktop-Kompilierung erfolgreich; XML: 1.974 bestandene JVM-/Desktop-Tests, 0 Fehler, 1 vorhandene Opt-in-Messung übersprungen.
- Zusätzliche lokale Android-Bedientests scheitern aktuell am gesperrten SM-X920 (`deviceLocked=1`, keine Compose-Hierarchie). Keine erfolgreiche aktuelle Android-Laufzeitabnahme behauptet. Standalone-`kotlinc` fehlt; Gradle-Kerntests erfolgreich. Vollständige PR-Pflicht-CI einschließlich Android-Laufzeit und Linux-Paketprüfung bleibt Mergevoraussetzung. Nachweis: [Masterabschlussplan](plans/active/2026-10-16-v340-master-abschluss.md).


## Angefügte Knotendiagnosen vom 16.10.2026 (lokaler Arbeitsstand)

- Die effektive Knotengröße beschreibt nur den Körper. Zeit hängt außen unten,
  vollständige Meldungen hängen außen oben. Auswahl und Größenänderung folgen dem
  Körper; Anschlüsse verwenden für Rechtecke und Adapter dieselbe Körpergeometrie.
- Gemeinsame Rendererhülle vereinigt Auswertungs- und lokale Renderdiagnosen,
  priorisiert Fehler und entfernt doppelte Texte. Leere Meldungen entfernen das
  obere Feld. Anhänge sind flüchtig; Gruppen/Culling berücksichtigen deren Maße.
- Vollständiger Gradle-Test-/Android-Buildlauf erfolgreich: 1.984 bestandene Tests,
  0 Fehler, 1 vorhandene Messprüfung übersprungen. 52 Desktop-Tests einschließlich
  großer Schrift, Zoom, Themes, Mausgriffe und langer Kopfzeile beim Pan.
  Debug- und Android-Test-APK gebaut, neue Android-Regression kompiliert.
- Android-Bedienabnahme offen: angeschlossenes SM-X920 gesperrt und im Ruhezustand.
  Standalone-Kernskript mangels kotlinc nicht ausführbar; 585 Gradle-Kerntests grün.
  Repository-/Architektur-/Releaseplan-/Versionsfolgeprüfungen erfolgreich,
  unabhängige Diffprüfung ohne offene Findings. Keine Version oder Commits geändert.
- Details: [ExecPlan](plans/completed/2026-10-16-knoten-anhaenge.md),
  [Darstellungsentscheidung](decisions/2026-10-16-knoten-anhaenge.md).


## Inspectorüberarbeitung vom 16.10.2026 (lokaler Arbeitsstand)

- Android und Desktop verwenden vier unabhängig klappbare Bereiche: Standarddaten,
  Knotendaten, Eingangsdaten, Ausgangsdaten. Leere Bereiche entfallen. Richtungsweite
  Optionen stehen in der ersten Zelle, individuelle Werte/Optionen am Anschluss.
  Sitzungsschlüssel erhalten Klappzustände bei Knotenwechsel und Portumordnung.
- Hintergrund- und Rahmenfarben sind separat automatisch oder individuell wählbar.
  Notizen bieten Rechteck, Kapsel, Ellipse, Dreieck und Raute mit sicherer
  Inhaltsfläche und konsistenter Treffergeometrie. Fachlich feste Formen bleiben
  lesbar. Format 10 und Version bleiben unverändert; keine neue Abhängigkeit.
- Standardwerte, vollständige Mehrfachbeiträge, Kartenfallbacks und Methodenprojektionen
  stehen am zugehörigen Port. Gemeinsame Rechner-/Tensor-/Übersetzeroptionen werden
  wiederverwendet; Verluste verwenden den Konfigurationsplan. Relationsparameter
  und Standardname wechseln atomar. Inspector-Skill aktualisiert und validiert.
- Vollständiger Gradle-Test-/Buildlauf erfolgreich: 2.045 bestandene JVM-/Desktoptests,
  0 Fehler, 1 vorhandene Opt-in-Messung übersprungen. 18 neue Regressionen einschließlich
  sechs nativer Desktop-Bedientests bestanden. App-/Editor-Lint ohne Fehler;
  Android-App-/Test-APKs, Editor-Test-APK und Desktopkompilierung erfolgreich.
- Repository-/Architektur-/Shadowmodul-/Releaseplan-/Versionsfolge-/Diffprüfungen
  erfolgreich; unabhängig quellgeprüft ohne offene Findings. Form-Screenshots
  visuell geprüft. Android-Laufzeitprüfung offen: ADB ohne Geräte, keine AVDs.
  Standalone-Kernskript mangels kotlinc nicht ausführbar; 585 Gradle-Kerntests grün.
- Zusatz-Lint des MathematikKnoten-Moduls verbleibt bei einem nachweislich vorhandenen
  API-Level-Befund in VisualisierungsSampler.kt:1850 (BigInteger.TWO, API 33 gegenüber
  minSdk 26). Kein Inspectorfehler; die Stelle wurde nicht geändert.
- Keine neuen Commits oder Veröffentlichung; bestehender Branch
  `release/v3.4.0-methoden-grundraeume-visualisierung`, HEAD `04925fc1` unverändert.
  Details: [ExecPlan](plans/completed/2026-10-16-inspektor-ueberarbeitung.md),
  [ADR](decisions/2026-10-16-inspektor-bereiche.md).

## Detailqualität während Visualisierer-Navigation (16.10.2026, lokal)

- Der gemeinsame Plot verwendet während Pan/Pinch/Rotation dieselben vorbereiteten Geometrien und Kanten wie in Ruhe. Initiale Grobgeometrie dient ausschließlich dem Erstaufbau. Kameraänderungen behalten passende Ergebnisse und fordern nach 120 ms Ruhe direkt Detail an; fachliche Änderungen invalidieren weiterhin zuverlässig.
- Zeichenpakete je Ebene werden erst nach vollständiger Hintergrundaufbereitung atomar ersetzt; keine leeren Zwischenlisten bei Navigation. Inspektion und ebenenübergreifende 3D-Tiefensortierung verwenden dieselben Daten. Arbeits-/Orbitgrenzen, zwei Aufträge und 32-MiB-Geometrie-LRU bleiben erhalten. Keine Persistenz-/Anschlussänderung.
- Sechs neue Anfrage-/Abbruch-/Schlüsselregressionen und zwei native offene-Gesten-Bildtests bestanden. Native Desktop-Visualisierersuite 17/17; beide neuen Bildtests gegen rekonstruierten Ausgangsrenderer erwartungsgemäß rot, gegen neue Implementierung grün. Vollständiger Gradle-Test-/App-Lint-/APK-/Desktoplauf erfolgreich; unabhängige Nachprüfung ohne Blocker.
- Android-Gegenstücke kompiliert. SM-X920 per ADB nicht verbunden; Nutzer bestätigt Nichtverfügbarkeit für heute. Android-Laufzeit und identischer Geräte-Vorher/Nachher-Vergleich bleiben offen. Kein neues Leistungsversprechen, 60-Hz-Ziel weiterhin offen. Nachweise im aktiven Visualisierer-ExecPlan und ergänzter ADR. Keine neuen Commits oder Releasearbeit.

## Knotenarchitektur: M0-Katalog und Migrationsschutz (17.10.2026)

- Der gemeinsame Erstellen-Katalog enthält die sieben zuvor nur auf Android
  ergänzten Mengen-/Raumtypen. Android ergänzt ausschließlich eigene Werkzeuge;
  acht historische Geometrieprädikate werden als konfigurierte Prädikate gesucht.
  M0-Zielbestand: 136 gemeinsame mathematische / 138 vollständige Androidtypen.
- Die Katalogtests prüfen exakte Typmengen statt bloßes Enthaltensein.
  Unklare Prädikatmigrationen bewahren Ports, Typverträge und Quellen; eindeutige
  Rollenwechsel remappen Standards und Kartenfallbacks mit stabilen IDs.
  Eine Migrationsdiagnose sperrt direkte und graphische Auswertung. Nachfolgende
  Mengen-/Methoden-/Join-/Dynamiknormalisierung respektiert diese Grenze.
- Unabhängige Quellprüfung einschließlich expliziter Legacyrollen und Sperre vor
  Kartenreferenzen/festen Ausgaben ohne offene Findings. Vollständige lokale
  JVM-/Desktop-/App-Lint-/APK-Prüfung erfolgreich: 2.125 Testfälle, 0 Fehler,
  1 vorhandener Opt-in-Messfall übersprungen; elf neue Regressionen bestanden.
  Sieben Editortests am SM-X920 bestanden; Appinstallation wegen abweichender
  Signatur blockiert, vorhandene App/Daten bewahrt. Standalone-kotlinc und lokale
  RPM-Erzeugung nicht verfügbar; CI-Nachweise und Veröffentlichung siehe Plan.
  Desktop sucht zusätzlich im vorhandenen Operatorindex; zwei Regressionen und
  der vollständige Desktopnachlauf bestanden. Keine Gesamtfreigabe M1–M7.
  [Aktiver Gesamtplan](plans/active/2026-10-17-knotenarchitektur-konsolidierung.md),
  [Entscheidung](decisions/2026-10-17-kanonischer-katalog-migrationsschutz.md).

M0 wurde als v3.4.2 (5813c0337b5c7b2284d882b785559dfeea4ae3cd, PR #456)
veröffentlicht. M1 folgte als v3.4.3 (e665a7fde78fe845dc7c62fa60fa00896d35f58a,
PR #457); alle Prüfungen am endgültigen PR-Head und master-Commit einschließlich
Emulator und Linux-RPM/Persistenz erfolgreich. M1 umfasst 132 gemeinsame
mathematische Arten und 134 Androidarten einschließlich Werkzeug/Gruppe.
Historische Methodenaufrufe verwenden den ID-basierten kanonischen Pfad mit
expliziter direkter Einzelresultatprojektion. Ordnungsrelationen und orientierte
Methodenanalysis werden als konfigurierte vorhandene Arten erzeugt. Details und
SHA-bezogene Prüfnachweise stehen im aktiven Gesamtplan.

M2 wurde als v3.4.4/x integriert (ec152625926698f58e1ddd1d1a1d6420f8b0c6cc). Die zunächst fehlgeschlagene Android-Schlussabnahme ist über die separat veröffentlichte enge Testreparatur v3.4.5 (e6760b9a41aaeb186dee7ee7c13254430cb47ee8) abgeschlossen; sämtliche finalen master-Gates sind grün. Der ursprüngliche Fehler bleibt historisch dokumentiert, seine genaue Ursache unbewiesen. M3 ist als v3.4.6/x reserviert.
Im aktuellen Produktbaum erzeugen Spur, Matrixdiagonale, Vektorradius und Kreuzprodukt
konfigurierte vorhandene Rechner; der Katalogtest bestätigt 128 gemeinsame Arten.
Reelle Norm, Kreuzproduktkomponenten, Basisvektorerzeugung und Transposition delegieren
gemeinsame Kernoperationen. Mehrdeutige Strukturmigrationen bewahren Originaldaten und
sperren Auswertung mit konkreter Diagnose. Gezielte Codec-/Metadatenregressionen und
der vollständige lokale Produktlauf bestehen: 2.180 Tests, 0 Fehler, 1 bestehender
Skip, Lint/APKs/Desktop. Unabhängige Prüfung ohne weitere Findings; abschließende
SHA-bezogene CI-/Emulator-/RPM-Abnahme durch v3.4.5 abgeschlossen; physische App-Geräteinstallation bleibt wegen fremder Signatur unverändert begrenzt.
Die vorhandene konjugierte Tensor-Norm bleibt ein getrennter Vertrag. Quellenarten,
polymorphes Transponieren und direkte Tupelstrukturknoten bleiben erhalten.


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

M3-Implementierungsstand 17.10.2026: v3.4.6/x reserviert auf e6760b9a,
Branch samai/v3.4.6/konsolidierung-m3. Vier Strukturadapter werden in vorhandene
Übersetzerziele integriert; Sollkatalog 124 mathematische/126 vollständige
Androidtypen. Gerichtete strenge Migration, Quellenidentitäten und vorhandene
Zeugnisse/Rückführungspläne werden geprüft. Gezielte Knoten-/App-Prüfungen
bestanden vor abschließender Identitätsregression; Gesamt-/Laufzeitfreigabe offen.
[ADR](decisions/2026-10-17-explizite-strukturuebersetzungen.md).

M3 lokale Gesamtprüfung abgeschlossen: 2.193 Tests ohne Fehler, ein bestehender
Opt-in-Skip, Lint, APK-/Test-APK- und Desktopprüfungen grün. APK 3.4.6/3004006;
Manifest 126, gemeinsamer Katalog 124. Unabhängiger Review ohne offene Findings.
Veröffentlichung und finale SHA-bezogene CI-Laufzeitabnahme weiterhin offen.

Vor M3-Veröffentlichung wurde zusätzlich Androids Revisionsschutz korrigiert:
notwendige Migration/unlesbare vorhandene Originalrevision erzeugt beim
Speichern eine neue freie Revision; Altbytes bleiben erhalten. Gewöhnliche
aktuelle Bearbeitungen unverändert, Desktop bereits revisionsbildend.
Gezielte JVM-Prüfungen und Kompilierung der drei neuen Geräteprüfungen bestehen,
unabhängiger Review ohne Findings. M3-PR #460 benötigt neuen finalen Head und CI.

M3-Revisionsschutz: vollständiger lokaler Abschlusslauf bestanden (49 s, 277 Tasks;
2195 Tests, 0 Fehler, 1 bestehender Skip), einschließlich Lint, APKs, Test-APKs
und Desktop. Protokoll: `/tmp/atlas-m3-revision-final.log`. CI am ergänzten
Release-Head und dessen Veröffentlichung stehen weiterhin aus.

M3 ist als v3.4.6/43726159 veröffentlicht; alle vier finalen Master-Workflows
(38997004505/38997004486/38997004522/38997004509) grün. M4 wurde erst danach
als v3.4.7/x reserviert und wird umgesetzt; noch keine M4-Produktfreigabe.

Visualisierungs-Typverträge, lokaler Stand 17.10.2026: Kompakte Potenzen wie ℝ²,
verschachtelte Produkte, orientierte Vektoren, Matrix-/Tensorräume und strukturierte
Methodendomänen verwenden eine gemeinsame numerische Rauminterpretation. Diskrete
Achsen bleiben auch in Filtern und Mengenoperationen vollständig budgetiert;
Zusatzbedingungen und ursprüngliche Mitgliedschaft bleiben erhalten. Explizite
Projektionen mit ein bis drei Koordinaten funktionieren auch für Matrix-/Tensor-
und Quaternionwerte und ändern die Raumkonfiguration atomar. Quotienten und
abstrakte Räume verlangen eine ausdrückliche numerische Abbildung; symbolische
Dimensionen und offene Nachweise werden nicht als leere Räume ausgegeben.

Lokale Abnahme: 2.313 JVM-Tests, 0 Fehler, 1 bestehender Skip; Android-Builds,
Lint, Desktop-Paket und Desktop-Darstellungs-/Bedientests bestanden. Repository-,
Standardkarten-, Releaseplan-/Versionsfolge- und 14 Release-Guard-Tests bestanden.
Unabhängige Codeprüfung ohne offene Findings. Kein neuer Commit oder Release.
[Entscheidung](decisions/2026-10-17-numerische-rauminterpretation.md).

Android-Laufzeitgrenze dieser Typkorrektur: Frühere zwei Geräteprüfungen bestanden,
finaler Wiederholungslauf inklusive Undo/Abbruch erreicht wegen geschützter
Gerätesperre keine Compose-Oberfläche. Testpakete entfernt, Originalinstallation
erhalten. Emulator endet mit Exit 139; zusätzliche CLI-Kernprüfung ohne `kotlinc`
nicht ausführbar. Finale Android-Laufzeitabnahme bleibt damit offen. Details und
Abnahmematrix im [abgeschlossenen Plan](plans/completed/2026-10-17-visualisierung-typenvertraege.md).
