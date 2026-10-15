# Lokale Änderungen mit GitHub v3.3.0 abgleichen

Status: Implementierung abgeschlossen, Veröffentlichung offen.

## Ziel und Nutzerwirkung
GitHub-master aktualisieren und die dort noch fehlenden lokalen Änderungen erneut integrieren und veröffentlichen. Mengenquellen können kartesische Potenzräume liefern, Bool-Einertupel funktionieren als Prädikate, die Konzeptbibliothek erhält die lokal vorbereitete Fachhierarchie und Geometriekategorie.

## Nicht-Ziele
Keine parallele Geometry-Nodes-Implementierung; keine effektiven Methodensignaturen oder Blender-Bridge entwickeln. Persönliche IDE-Dateien und Cachedateien werden nicht veröffentlicht.

## Untersuchter Istzustand
Lokaler Ausgangspunkt e535190058014810617ea2989d27838afe54692b (v3.2.1), GitHub a789e606d0c0148bff954d87404fcfef82ada77e (v3.3.0). Zwei Fast-forward-Commits. Sämtliche lokalen Änderungen vor dem Pull als Stash und Archiv unter `.gradle/local-sync-backup-20261015/` gesichert; Archiv enthält auch IDE-Dateien und unfertigen Geometrieprototyp.

## Fachliche Semantik
M^n enthält geordnete n-Tupel über M; n=0 enthält das leere Tupel und n=1 bleibt ein Einertupelraum. Konkrete Dimensionen müssen nichtnegative ganze Zahlen sein; symbolische Dimensionen bleiben symbolisch. Nur skalare Aussagen oder genau ein Aussageelement werden als Prädikatsausgabe aufgelöst.

## Daten-, Knoten-, Anschluss- und Verbindungsvertrag
Kein neuer registrierter Knotentyp. Bestehende Mengenquellen bekommen einen optionalen Zahl-Eingang `dimension`; Gradmengen behalten `dim`. Ohne Verbindung bleibt die bisherige Menge erhalten. Lade- und Konfigurationspfade ergänzen fehlende Eingänge idempotent, bestehende Anschluss-IDs bleiben erhalten. Eigene Karten behalten versionsfeste Referenzen. Historische Fachpfade bleiben ladbar und werden für die Bibliothek normalisiert.

## Architekturentscheidungen
Mathematik bleibt im Rechenkern, Auswertung im vorhandenen Register. Der veröffentlichte Geometriepfad unter `kern/prozeduralegeometrie/` ersetzt den lokalen Parallelprototyp. Dessen Input liefert nur ein leeres Set, Output hat einen inkonsistenten Anschlussvertrag und RealizeInstances ist ein No-op mit bereits veröffentlichter Typ-ID; deshalb keine Übernahme dieser Stubs. Der Prototyp bleibt vollständig gesichert.

## Betroffene Dateien und Symbole
`Mengen.kt`, `MethodenFundament.kt`, `Methoden.kt`, `PraedikatsDarstellung.kt`, `AxiomOperatoren.kt`, `MathematischeTypen.kt`, `MathematischerWertCodec.kt`, `KonsolidierteKnoten.kt`, `FachKatalog.kt`, `KonzeptBibliothek.kt`/`Ui`, passende Tests und lokale Agenten-Skills. Release-Metadaten: Android, Desktop, Roadmap und Linux-Artefaktname.

## Meilensteine
- [x] Lokalen Stand sichern und GitHub vergleichen.
- [x] master per Fast-forward aktualisieren; SamAI-Branch erstellen.
- [x] Fehlende Änderungen integrieren und Integrationslücken schließen.
- [x] Unabhängiger Review durch master_verwalter; Bool-Consumer und Parameteranalyse korrigiert.
- [-] Finale Prüfungen und Veröffentlichung.

## Umsetzungsschritte
Archivdateien selektiv wiederherstellen, Kollisionen anhand veröffentlichter Implementierungen auflösen, Codec/Substitution/Parameteranalyse ergänzen und bestehende Migration erweitern. Fachpfad- und Anschlussannahmen der Tests aktualisieren. Finale Prüfung vor Commit und PR.

## Tests und Validierung
Repository-, Releaseplan-, Versionsfolgeprüfung und 14 Release-Guard-Regressionen erfolgreich. Synthetischer PR-Kontext gegen exakte Basis a789e606 erfolgreich. Finale vollständige JVM-Tests: 1864 ohne Fehler oder Überspringen; `:app:assembleDebug` erfolgreich. `:app:lintDebug` im Wiederholungslauf erfolgreich (gesamter finaler Lauf: BUILD SUCCESSFUL in 2m 8s); erster Versuch hatte einen internen FIR-Analyseabsturz. Alle 15 Standardkarten strukturell geprüft. Gezielte Regressionen prüfen leere/Einertupel, falsche Dimension, symbolische Bindung, Wert-/Formelcodec, Prädikatsdarstellung und Wahrheitstabelle sowie Anschlussmigration. `scripts/pruefe_kern.py` lokal nicht ausführbar: kotlinc fehlt im PATH; Gradle führt Kern-JVM-Tests aus. ADB verfügbar, aber kein Gerät oder Emulator angeschlossen; visuelle Abnahme der Bibliothek bleibt offen.

## Persistenz und Migration
Kartenformat bleibt 9. Neuer Mengenausdruck hat expliziten Wertcodec-Tag. Keine historische Datenvariante wird entfernt. Bestehender gemeinsamer Mengenmigrationspfad ergänzt den Dimensionseingang; eigene Karten bleiben unverändert.

## Risiken und Rückfallstrategie
Originaländerungen bleiben im Stash und Archiv erhalten. GitHub-Änderungen werden nicht überschrieben. Bei fehlgeschlagenen Pflichtchecks kein Merge. Visuelle Geräteprüfung konnte nicht ausgeführt werden.

## Fortschritt
2026-10-15: Vergleich, Sicherung, Pull, Wiederherstellung, Releaseklassifikation und Review abgeschlossen; finale Tests und Veröffentlichung folgen.

## Entscheidungsprotokoll
2026-10-15: master_verwalter bestätigt v3.3.1, Achse x, Basis a789e606. Der lokale Geometrieprototyp wird durch die bessere veröffentlichte Familie abgelöst; zusätzliche Stubtypen würden unbestätigte Semantik und ein paralleles System einführen.

## Abweichungen
Statt blindem Wiederaufspielen aller Dateien wird der überholte Geometrieprototyp gesichert. Notwendige Codec-, Migrations- und Prädikatsconsumer-Ergänzungen schließen Lücken des lokalen Ausgangsstands.

## Ergebnis und Verifikation
Veröffentlichung noch offen; finalen Prüfstatus und Commit nachtragen.
