# v3.0.1 – Alpha-Erweiterungen für Kartenherkunft und Operatorverträge

**Status:** implementiert und geprüft
**Releasebranch:** `release/v3.0.1-alpha-erweiterungen`
**Arbeitsbranches:** `samai/v3.0.1/kartenbibliothek-herkunft`, `samai/v3.0.1/operatorabstraktion`
**Basis:** `origin/master` bei Beginn, Commit `104f9cb4668463f62e32964e966ec1b4997a3147`

## Ziel und Nutzerwirkung

Die Kartenbibliothek trennt eigene, ausgelieferte und fremd importierte Karten anhand stabiler app-lokaler Herkunftsdaten. Alle Importwege verwenden dieselbe validierte Pipeline; eigene Karten und Sammlungen werden über einen zentralen Veröffentlichungsdienst geteilt. Parallel werden die vorhandenen Zahlen-, Vektor-, Matrix- und Tensorrechner auf einen gemeinsamen deklarativen Operatorvertrag umgestellt, ohne bestehende Knotenarten oder Operator-IDs zu ändern.

## Nicht-Ziele

- keine neuen persistierbaren oder separat erzeugbaren Knotentypen,
- keine Änderung des neutralen `KartenDaten`-Formats allein für Herkunft,
- kein öffentlicher Kartenserver,
- keine Umsetzung der in `docs/V3_BETA.md` geplanten SVG-/TeX-Erweiterungen,
- keine beiläufige Bereinigung der vorhandenen `.idea`-Dateien.

## Untersuchter Istzustand

- `KartenListe` zeigt alle Karten und Ordner gemeinsam und bietet immer dieselben Aktionen.
- Standardkarten und Freigabepakete besitzen getrennte Provenienzspeicher; direkte JSON- und `.matlas`-Importe besitzen keine Herkunftsmetadaten.
- Toolbar-Import erkennt ZIP über die Signatur und ruft getrennte Importpfade auf; Linkimport existiert nicht.
- Karten-, Ordnungs- und Freigabedaten werden einzeln atomar geschrieben, aber nicht als gemeinsame Transaktion.
- Operatorwissen ist auf Rechenkern-Enums, `StrukturRechnerOperatoren`, `RechnerFamilienKatalog`, `TensorOperationRegistry`, Konfiguratoren, Inspektoren und eine reihenfolgeabhängige Auswerterkette verteilt.

## Fachliche Semantik und Datenverträge

### Herkunft

`KartenHerkunft` besitzt `EIGEN`, `VORDEFINIERT` und `FREMD`. Standardkartenprovenienz hat Vorrang; eine Freigabe der lokalen `ProfilId` ist eigen, eine andere Freigabe fremd; sonstige Importe sind fremd; unbekannte Bestandskarten sind eigen. Bearbeitung und Ordnerwechsel ändern die Herkunft nicht. Neue und lokal kopierte Karten sind eigen.

Herkunft bleibt außerhalb von `KartenDaten`. Eine versionierte app-lokale Datei ergänzt nur direkte externe Karten und importierte Ordner. Standardkarten- und Freigabequellen bleiben ihre kanonischen Quellen.

### Operatoren

Der gemeinsame Laufzeitvertrag beschreibt Familie, Operator-ID, Signaturen, Ports, Parameter, Fähigkeiten und Unterstützungsstatus. Persistierte Knotenarten, Operatorparameter und Anschlussnamen bleiben bestehen. Anschluss-IDs werden nur bei gleicher semantischer Rolle, Richtung und kompatibler Art erhalten; explizite Legacy-Aliase und stabile dynamische Identitäten werden berücksichtigt, eine positionsbasierte Notfallzuordnung ist verboten.

Unbekannte Operator-IDs bleiben unverändert geladen und werden als Fehlerzustand angezeigt. Jede Rechner-Knotenart erhält genau einen Dispatcher, der an einen spezialisierten Fachauswerter delegiert.

## Architekturentscheidungen

- Herkunft, Importtransaktion, Linkabruf und Veröffentlichung liegen in `app`; der neutrale Graph bleibt unverändert.
- Import wird vollständig geplant und validiert, dann über Staging, Journal, atomare Ersetzungen und Start-Rollback als eine Transaktion übernommen.
- Der allgemeine Operatorvertrag und seine Registry liegen in `MathematikKnoten`; mathematische Berechnung bleibt in Rechenkern beziehungsweise Fachauswertern.
- Matrix, Vektor, Zahl und Tensor werden nacheinander umgestellt und je Familie per Paritätstest abgesichert.
- Die dauerhafte Registry- und Dispatcherentscheidung wird als ADR dokumentiert.

## Betroffene Bereiche

- Android-Kartenverwaltung, `KartenSpeicher`, Ordnung, Standardkarten/Freigaben und Toolbar-Import,
- Rechnerkonfiguration, Operator-Suchindex, Enzyklopädie, Definitionskarten und Inspektoren,
- Auswerterregistrierung und Familienauswerter,
- Release-, Architektur- und Zustandsdokumentation.

## Meilensteine und konkrete Schritte

- [x] A: Release v3.0.1 reservieren, sauberen Worktree und diesen ExecPlan anlegen.
- [x] B: Herkunftsauflösung und externe Herkunftsmetadaten einschließlich Ordnerherkunft implementieren und migrieren.
- [x] C: gemeinsames Format-Sniffing, crash-sichere Importtransaktion und sicheren HTTPS-Abruf implementieren.
- [x] D: Bibliotheksfilter, bereichsabhängige Aktionen, gemeinsamen Importdialog und Veröffentlichung umsetzen.
- [x] E: allgemeinen Operator-, Signatur-, Port-, Parameter- und Familienvertrag samt Registry und generischem Konfigurator einführen.
- [x] F: Matrix-, Vektor-, Zahlen- und Tensorfamilie registrieren; Suchindex und Auswerter-Dispatch auf die Registry umstellen.
- [x] G: einen abschließenden Dispatcher je Rechnerfamilie etablieren; Alt-IDs und historische Karten weiter unterstützen.
- [x] H: Dokumentation, ADR, Release-Metadaten und vollständige Prüfungen abschließen.

## Tests und Validierung

- Herkunftspriorität, lokale Reimporte, geänderte Standard-/Fremdkarten, Fallback sowie leere und gefilterte Ordner.
- JSON-, Freigabepaket-, `.matlas`- und HTTPS-Import, Format-Sniffing, Größen-/Timeout-/Redirectregeln, defekte Container sowie Rollback und Startwiederherstellung.
- Registry-Eindeutigkeit, vollständige Familienabdeckung, Signatur-Port-Abbildung, alternative/variadische Signaturen, Mehrfachausgänge und unbekannte IDs.
- Parität aller vorhandenen Rechneroperatoren, Methodenhebung, Formeln, dynamische Tensorachsen, Anschluss-ID-Erhalt, Verlustdiagnose und Undo/Redo.
- `python3 scripts/pruefe_repository.py`, `python3 scripts/pruefe_releaseplan.py`, `python3 scripts/pruefe_versionsfolge.py`, `python3 scripts/pruefe_kern.py`, `./gradlew test`, `./gradlew :app:lintDebug`, `./gradlew :app:assembleDebug`.
- Dateiauswahl, Linkimport, Teilen und Bedienung auf Emulator oder Gerät; nicht ausführbare Geräteprüfungen werden ausdrücklich dokumentiert.

## Persistenz und Migration

Die app-lokale Herkunftsdatei beginnt mit Formatversion 1. Bestehende Karten werden aus Standardprovenienz und Freigabequellen abgeleitet; übrige Karten benötigen keinen gespeicherten Eintrag und gelten als eigen. `KartenDatenJson.FORMAT_VERSION` bleibt 8, solange kein Graphschema geändert wird. Historische Operatorparameter und Anschlussreferenzen bleiben gültig.

## Risiken und Rückfallstrategie

- Bei Importfehler oder Prozessabbruch stellt das Transaktionsjournal Karten, Ordnung und Herkunft auf den Vorzustand zurück.
- Jede Rechnerfamilie bleibt bis bestandener Parität auf dem bisherigen Auswertungspfad; erst danach wird der jeweilige Altpfad entfernt.
- Erscheint ein neuer separat erzeugbarer Knotentyp im Abschlussdiff, stoppt v3.0.1 und wird als `v3.1.0` neu klassifiziert.
- Draft-PRs #433 und #435 berühren verwandte Methodenverträge; vor Integration nach `master` wird der aktuelle Remotezustand erneut geprüft.

## Fortschritt

- 2026-10-05: Umfang als gemeinsame x-Version v3.0.1 bestätigt; keine neuen Knotentypen geplant.
- 2026-10-05: Live-Fetch und Audit offener PRs abgeschlossen; bestehende Draft-PRs reservieren keine Version.
- 2026-10-05: Kartenherkunft, gemeinsame Importoberfläche, HTTPS-Grenzen, Importjournal sowie Bibliotheksfilter implementiert.
- 2026-10-05: Kanonische Registry und abschließende Familien-Dispatcher implementiert; gezielte Paritätstests bestehen.

## Entscheidungsprotokoll

- 2026-10-05: Beide offenen Alpha-Erweiterungen werden gemeinsam als v3.0.1 umgesetzt. Alternative: getrennte Releases. Begründung: gemeinsamer Alpha-Abschluss bei weiterhin getrennten Implementierungsmeilensteinen.
- 2026-10-05: Herkunft bleibt app-lokal. Alternative: Erweiterung von `KartenDaten`. Begründung: Herkunft ist Bibliotheks-/Importkontext und kein neutraler Graphvertrag.
- 2026-10-05: Operator-Metadaten werden in einer Registry zentralisiert, die Fachauswertung bleibt familienweise. Alternative: universeller Rechner. Begründung: gemeinsame Knotentechnik ohne Vermischung mathematischer Semantik.

## Abweichungen vom ursprünglichen Plan

- Die vorhandenen spezialisierten Familienauswerter bleiben bewusst bestehen.
  Statt sie in einen universellen Auswerter umzuschreiben, validiert je
  Knotenart genau ein abschließender Registry-Dispatcher und delegiert an den
  Fachauswerter. Das erhält die mathematische Modultrennung und die Altparität.

## Ergebnis und Verifikation

Implementierung abgeschlossen. Repository-, Releaseplan- und Versionsfolgeprüfung,
vollständiger Gradle-Testlauf, Android-Lint und Debug-APK-Build bestehen. Die
zusätzliche Direktprüfung `scripts/pruefe_kern.py` war nicht ausführbar, weil
`kotlinc` in der Umgebung fehlt; die Kernmodule wurden durch `./gradlew test`
kompiliert und getestet. Eine Geräte-/Emulatorprüfung wurde nicht ausgeführt.
