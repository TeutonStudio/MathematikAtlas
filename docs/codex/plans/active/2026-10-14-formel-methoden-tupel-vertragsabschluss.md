# ExecPlan: Formel-, Methoden- und Tupelvertrag abschließen

## 1. Titel und Status
2026-10-14; in Arbeit. Ausgangsbasis 8465f6c9, Branch samai/v3.2.1-variable-grundraum-tupel-ordnung. Benutzerauftrag erweitert: Vertragsabschluss vervollständigen und nächsten Master-Release vorbereiten. Direkter Commit auf `master` bleibt gemäß Git-Identitäts- und Releaseworkflow ausgeschlossen; Veröffentlichung nur nach Releasebranch-, Versions-, Prüfsummen- und Integrationsabnahme.

## 2. Ziel und Nutzerwirkung
Ein gemeinsamer Vertrag erhält Identitäten, Argumentstruktur, Typen, Räume und Voraussetzungen über Eingabe, Formel, Karte, Methoden und Speicherung. Deklarationen entscheiden; unbekanntes f(x) darf nicht still Multiplikation werden.

## 3. Nicht-Ziele
Keine neuen Knotentypen, Produktionsabhängigkeiten, zusätzlichen Analysisverfahren oder globale Inversion. Kein pauschaler Merge älterer Draft-PRs. Keine Gesamtfreigabe aller 77 Anforderungen des Vorgängerplans. Dieser Releaseabschluss beansprucht ausschließlich den in der v3.2.1-Roadmap reservierten Teilumfang.

## 4. Untersuchter Istzustand
Lokaler Stand enthält Methodenbindungen, kanonische Anwendung, Übersetzungszeugnisse und gemeinsame UI. StrukturFormelCodec schreibt Version 3, FormelKontextCodec Version 2 und MathematischerWertCodec Version 3; sie akzeptieren ausschließlich die jeweilige aktuelle Version. Parserpriorität, explizite Mehrdeutigkeitsdiagnose, höherwertige symbolische Aufrufe und null-/mehrwertige Bildmengen sind umgesetzt. Offen bleiben laut 77-Punkte-Matrix unter anderem Scalar-Legacyprojektionen und namensbasierte Altpfade, vollständige ID-only-Aufrufermigration, allgemeine Binder-/Operatorintegration sowie UI-Laufzeitabnahme. Historische CI-Befunde zu c0566f08 sind kein Nachweis für diesen Stand.

## 5. Fachliche und mathematische Semantik
Leere/einstellige/verschachtelte Tupel bleiben verschieden. Kanonische Anwendung liefert Ergebnistupel und offene Voraussetzungen. Widerlegte Bedingungen verhindern Berechnung. Komponentenräume und tatsächlicher effektiver Gesamtraum bleiben getrennt gespeichert; wenn ein effektiver Gesamtraum vorliegt, ist er für die Zugehörigkeitsprüfung maßgeblich und darf Komponentenbereiche ausdrücklich erweitern. Numerische Vorschauen beweisen keine Abbildungseigenschaften.

## 6. Daten-, Node-, Handle- und Edge-Vertrag
Bestehende Knotentypen und Register verwenden. Quellen-, Bindungs-, Ausdrucks- und Komponenten-ID getrennt halten; Umbenennen/Umordnen ändert keine Identität. Kopieren remappt lokale Referenzen konsistent. Anschlussprojektionen sind explizite Grenzen, keine implizite Änderung logischer Argumente.

## 7. Architekturentscheidungen
E01 Deklaration entscheidet, sonst Mehrdeutigkeitsdiagnose. E02 ältere Mathematik-Wire-Versionen werden nicht unterstützt oder migriert; Decoder akzeptieren nur die jeweils aktuelle Version und lehnen ältere Payloads ab. E03 kanonisches Ergebnistupel mit expliziter Projektion. E04 offene Voraussetzungen erhalten. E05 Abbildungseigenschaften und Informationsverlust deklarieren. E06 Fundament vor weiteren Operatoren. Vorhandene ASTs, Typen, Methoden und Codecs erweitern; keine parallele Semantik.

## 8. Betroffene Dateien und Symbole
Kern: LatexFormelParser/Primaer, GebundeneFormelAuswertung, StrukturellerMethodenAufruf, Methoden, KanonischeMethodenanwendung, MathematischerWertCodec und zugehörige Tests. Knoten: Bildmengen-Auswerter und Konfiguration. Bestehende Formel-/Graph-/Kontextcodecs und UI-Aufrufpfade auf Vertrag prüfen.

## 9. Meilensteine
- [x] M0 Ausgangsbasis reproduzieren; Repository-/Release-Guards bestehen. Historische CI-Fehler reproduzieren sich am lokalen Stand nicht.
- [x] M1 E01–E06 protokolliert und ADR um Deklarationsvorrang, verschachtelte Signaturen sowie neue Codecversionen ergänzt; 77-Punkte-Matrix bleibt offen.
- [-] M2 Verschachtelte Komponenten-IDs werden nun in der Methodensignatur getragen und persistiert; allgemeine ID-only-Migration bleibt offen.
- [-] M3 Formelkontext-/Wertcodec erhalten verschachtelte Signaturen; gezielte Formel-, Wert- und Graph-Roundtrips bestehen. Vollständige Kartenmigration bleibt offen.
- [-] M4 Deklarationsvorrang (auch bei expliziten LaTeX-Befehlen wie `\\sin`), explizite Mehrdeutigkeitsdiagnose, Klammerntupel und benannte/gruppierte Methodenfolgeaufrufe implementiert. Gesamte Rechner-/UI-Palette bleibt offen.
- [-] M5 Bildmengenbildung unterstützt nullstellige und mehrwertige Methoden und erhält offene Bedingungen. Übrige Verbraucher und allgemeine Operatoren bleiben offen.
- [ ] M6 Legacy nach vollständiger Aufrufermigration entfernen und Gesamtabnahme ausführen. Die 77-Punkte-Matrix aus dem ExecPlan vom 2026-10-13 ist hierfür maßgeblich; fehlende UI-Laufzeit und nicht migrierte Aufrufer verhindern derzeit den Abschluss.
- [-] M7 Den reservierten v3.2.1-Teilumfang als `x`-Release vorbereiten. Der Release-PR muss auf `dc072189` basieren und grüne Release-, Android-Emulator- und Linux-Desktop-Workflows nachweisen; erst der finale Commit `v3.2.1` auf `master` veröffentlicht die Version.

## 10. Konkrete Umsetzungsschritte
1. Pflichtprüfungen dem exakten Ausgangsstand zuordnen.
2. Deklarationsvorrang, Mehrdeutigkeitsdiagnose und Tupel-/Folgeaufrufe absichern.
3. Typisierte methodenwertige Projektion samt Substitution und strukturellem Codec ergänzen.
4. Nullstellige/mehrwertige Bildmengen und gleichzeitige Komponenten-/Gesamtraumprüfung absichern.
5. Aktive Aufrufer und Legacygrenzen prüfen; nur nach abgeschlossener Migration entfernen.
6. End-to-End-Regressionsmatrix und unabhängige Prüfung abschließen.

## 11. Tests und Validierung
Q mit f(t)=t²,x=3,h=2 ergibt 8; h=0 abweisen, symbolische Quellen und h≠0 erhalten. F((a,b),g) besitzt zwei Argumente. Nullstellige Methode über {()} bildet einen vollständigen Ergebniswert. Konkrete/symbolische Methodenrückgaben erneut aufrufen. Codec-/Graph-Roundtrips einschließlich IDs, Typen, Räume, Reihenfolge, Bedingungen. Kopieren/Umordnen/Undo/Redo und gespeicherte Kartenpfade. Pflichtbefehle: python3 scripts/pruefe_repository.py, pruefe_releaseplan.py, pruefe_versionsfolge.py, pruefe_kern.py; ./gradlew test :app:assembleDebug :app:lintDebug :desktopApp:compileKotlin. Laufzeitabnahme separat.

## 12. Persistenz und Migration
Mathematikcodecs akzeptieren nur die aktuelle Wire-Version; alte Versionen und alte Signaturblöcke werden klar abgelehnt. Keine erfundene Rekonstruktion verlorener Information, kein LaTeX-Fallback. Entfernten Tupelvariablen-Knoten nicht reaktivieren. Neue Codecversion nur bei neuer Wire-Struktur.

## 13. Risiken und Rückfallstrategie
Großer bereits implementierter Teilstand und veraltete Fortschrittsmatrix; Fähigkeiten nicht doppelt implementieren. Fremde Änderungen unangetastet lassen. Aufgabenspezifische Rücknahme statt Arbeitsbaumreset. Nicht ausführbare Geräteabnahme bleibt Freigabegrenze. Keine Releasehistorie umschreiben.

## 14. Fortschritt
- [x] Ausgangsstand und einschlägige Architektur-/Plan-/Testdokumente gelesen.
- [x] Repository-, Releaseplan- und Versionsfolgeprüfung am Ausgangsstand erfolgreich.
- [x] Vollständiger Ausgangslauf `./gradlew test :app:assembleDebug :app:lintDebug :desktopApp:compileKotlin` erfolgreich: 1721 Tests auf der Basis, keine Fehler/Überspringungen.
- [x] Unabhängige statische Erstprüfung und Nachprüfung: parserseitige Mehrdeutigkeit und fehlende höherwertige Rückgaben bestätigt und im getesteten Pfad geschlossen.
- [x] Vollsuite nach den aktuellen Codec-Ablehnungstests vollständig und ohne Filter neu ausgeführt: `./gradlew test :app:assembleDebug :app:lintDebug :desktopApp:compileKotlin --rerun-tasks` erfolgreich (219 Tasks); XML-Berichte summieren 1729 Tests, 0 Fehler, 0 Überspringungen.
- [x] `python3 scripts/pruefe_repository.py`, `pruefe_releaseplan.py`, `pruefe_versionsfolge.py` und `git diff --check` erneut erfolgreich; zusätzliche `pruefe_kern.py` bleibt mangels `kotlinc`/`java` im PATH nicht ausführbar.
- [x] Releaseaudit durch `master_verwalter`: bestehende 3.2.1-x-Reservierung ist auf derselben Vorgängerbasis, aber beschreibt nur Variablengrundraum/Tupelreihenfolge; tatsächlicher Umfang und Roadmaptitel/-begründung stimmen nicht überein. Keine Freigabe für Mastercommit.
- [x] Roadmapreservierung durch `master_verwalter` als Teilabschluss des Vertragsfundaments präzisiert; Umfang, `x`-Klassifikation, Vorgänger `3.2.0`, Basis `dc072189` und Branch bleiben festgehalten.
- [x] Tests weisen ältere Strukturformel-, Formelkontext- und Wertcodec-Versionen zurück. Fokussierter Test `:MathematikRechenSystem:test --tests '*VertragsRoundtripTest'` erfolgreich.
- [x] Nutzerentscheidung umgesetzt: keine Kompatibilität zu älteren Mathematikcodec-Versionen. Strukturformel-, Formelkontext- und Wertdecoder weisen alte Wire-Versionen ab; der Strukturformel-LaTeX-Lesepfad und das alte Signaturblockformat sind entfernt. Ablehnungstests für Version 1 bestehen.
- [x] Releaseaudit bestätigt `x`-Klassifikation und unveränderte Basis; der GitHub-PR-Audit fand nur die bestehenden Drafts #433/#435/#436, keinen PR für den v3.2.1-Branch.
- [-] Android-Metadaten, Roadmapzielversion und Desktop-Artefaktname auf v3.2.1 abgestimmt; lokale Prüfungen und abschließender unabhängiger Diffreview laufen. PR-CI und Merge sind noch offen.

## 15. Entscheidungsprotokoll
2026-10-14: Benutzer wählt jeweils Empfehlung 1 für E01–E06 und stellt klar, dass keine Kompatibilität zu älteren Mathematikcodec-Versionen erforderlich ist. Der nächste Master-Release soll den in der Roadmap reservierten v3.2.1-Teilumfang veröffentlichen; die vollständige 77-Punkte-Matrix bleibt Folgearbeit. Bestehende fachliche Modelle und Register sind maßgeblich.

## 16. Abweichungen vom ursprünglichen Plan
Lokaler Stand ist weiter als c0566f08 und schreibt Strukturcodec v3. Daher Implementierungslücken statt Neuaufbau bearbeiten. M0 ist eine erneute Prüfung, keine vorab unterstellte Reparatur.

## 17. Ergebnis und Verifikation
Lokaler Teilabschluss der Parser-, höherwertigen Methoden-, Codec- und Bildmengenlücken. Mathematikcodecs akzeptieren nur ihre aktuelle Version; ältere Formel-/Wertpayloads werden ohne Migrationspfad zurückgewiesen. Der Gesamtvertrag bleibt aktiv: verbleibende Legacy-Aufrufer, allgemeine Operatorintegration und vollständige 77-Punkte-Evidenz sind Folgearbeit und keine Behauptung dieses Teilreleases. Der Releasekandidat ist noch nicht veröffentlicht.

- `./gradlew test :app:assembleDebug :app:lintDebug :desktopApp:compileKotlin`: erfolgreich, 219 Tasks, 1729 Tests laut XML-Berichten ohne Fehler oder Überspringungen auf dem Produktstand vor der abschließenden Release-Metadatenänderung; erneuter Lauf nach Metadatenanpassung erforderlich.
- Nach dem letzten Parser-Fix für deklarierte explizite LaTeX-Befehle: fokussierte Rechenkerntests für Formelverträge, Methoden-Kontext, Bildmenge/Matrixprodukt und Rechenkern erfolgreich.
- `python3 scripts/pruefe_repository.py`, `pruefe_releaseplan.py` und `pruefe_versionsfolge.py`: erfolgreich. `git diff --check`: erfolgreich.
- `python3 scripts/pruefe_kern.py`: nicht ausführbar, das Skript erwartet `kotlinc` im PATH; Gradle-Kerntests bestehen.
- Android-Laufzeitabnahme nicht erfolgt: ADB meldet kein Gerät und kein AVD. Keine UI-Laufzeitfreigabe behauptet.
- Unabhängige Prüfung bestätigt die korrigierten Aufruffälle und nennt als Rest: vollständige Legacy-Ablösung sowie vollständige Umstellung aller aktiven Methodenpfade auf ID-only.
- Frühere V1-Fixtureprüfung wurde mit der Nutzerentscheidung gegen Rückwärtskompatibilität überholt; Fixtures dienen nun ausschließlich dazu, die Ablehnung alter Mathematik-Wire-Versionen nachzuweisen.
- Kein Commit oder Release. Ausgangsbranch bleibt `samai/v3.2.1-variable-grundraum-tupel-ordnung`; HEAD bleibt der vorhandene Commit. Fremde Arbeitsänderungen blieben unangetastet.
