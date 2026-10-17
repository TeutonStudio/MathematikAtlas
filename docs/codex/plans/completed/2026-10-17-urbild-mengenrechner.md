# Urbild im Mengenrechner

Status: abgeschlossen und unabhängig abgenommen am 17.10.2026. Nutzerbestätigter Plan vom 17.10.2026.

## Ziel und Nutzerwirkung

Urbild wird als vorhandener Operator `menge.urbild` von `mathematik.mengenrechner` erzeugbar. Methode und Einzelziel beziehungsweise Zielmenge liefern eine gemeinsame Urbildmenge. Der Einzelzieleingang folgt dem Ergebnistyp der Methode. Die letzte unabhängige Quellabnahme umfasst auch Vereinigungs-Typen und den Migrationsschutz bei Rollenkollisionen. Android und Desktop erhalten die Auswahl Einzelwert/Zielmenge unter den gemeinsamen Eingangseinstellungen.

## Nicht-Ziele

Keine neue Knotenart, numerische Suche, neue Abhängigkeit, Kartenformatänderung oder Veröffentlichung. Fremde lokale Änderungen bleiben erhalten.

## Istzustand

`MengenRechnerOperator.URBILD` existiert als ausgeblendeter symbolischer Platzhalter ohne Methodeneingang. Vergleichspfade sind iterierte Mengenoperatoren, Bildmengensynchronisierung, gemeinsame Inspector-Eingangseinstellungen und `planeKonfiguration`. Vor Beginn bestehender Diff in `/tmp/atlas-urbild-vorher.patch`, Index separat gesichert. Basis HEAD 20356f1c3c8e996fae03ed10ff10d9fe8005debe; Arbeitsbranch samai/v3.4.12/pie-methoden-matrizen.

## Semantik und Verträge

`f⁻¹[B]={x∈D | f(x)∈B}`. Einzelwert y entspricht B={y}; keine Injektivität nötig. Alle Treffer vereinigen. Effektiven Definitionsraum und Voraussetzungen erhalten. Endliche Domänen exakt prüfen, unterstützte Zahlgleichungen über bestehenden Prädikatslöser lösen; sonst strukturierte symbolische Menge. Keine Teilantwort als vollständiges Ergebnis. Strukturierte Einzelausgaben und das äußere Ergebnistupel unterscheiden.

Anschlüsse: methode (Methode), ziel (Zahl im unverbundenen Standard, sonst Ergebnistyp oder Menge im Zielmengenmodus), menge (Menge). IDs bleiben bei Modus-/Typwechsel erhalten. Eingabeform wird als Parameter gespeichert. Historische a-Mengenanschlüsse werden unter Erhalt von Referenzen als Zielmengen migriert; Mehrdeutigkeit diagnostizieren.

## Architektur und betroffene Symbole

Kern: neue gemeinsame `bildeUrbild`-Funktion und vorhandene Prädikats-/Methoden-/Mengenmodelle. Knoten: `konfiguriereMengenRechner`, `werteMengenRechnerAus`, sichtbare Operatoren, gemeinsame Synchronisierung und Migration. UI: `InspektorRechnerEingangsEinstellungen`, Android-Mengeninspektor und Desktop. Vorschau/Übernahme über `planeKonfiguration`, atomare Kartenaktionen, bestehender Verlustbestätigungsweg. Standardrenderer, Kontur und äußere Δzeit-Anhänge bleiben bestehen.

## Meilensteine und Schritte

- [x] Bestand, Nutzerentscheidungen und mathematische Semantik geprüft; math_reviewer und master_verwalter eingebunden.
- [x] Kernsemantik und Fachtests implementieren; 19 gezielte Kernregressionen grün.
- [x] Knoten, Anschlüsse, Typableitung, Migration und Inspector integriert.
- [x] Fach-, Graph-, Persistenz- und UI-Regressionen; vollständige Suite sowie Lint-/Migrationsabschlussnachlauf grün.
- [x] Unabhängige Quellabnahme ohne Blocker; Dokumentation und Abschlussdiff geprüft.

## Tests und Validierung

Gezielte Urbildtests (x²=4, {4,9}, leere Ziele, keine Treffer, konstante Methoden, Domänen, Bedingungen, Strukturwerte, mehrere Argumente/Ergebnisse); Katalog, Auswertung, Typwechsel, Vorschauverluste, Undo/Redo, Kopieren und Codec-Roundtrips. Anschließend Gradle-Tests, Android-Lint/Build und native Desktopbedienung. Skripte: pruefe_repository, pruefe_standardkarten, pruefe_releaseplan, pruefe_versionsfolge, test_release_guards, pruefe_kern sowie git diff --check. Tatsächliche Ergebnisse folgen unten.

## Persistenz und Migration

Bestehende Werttypen und Codecs verwenden. Historischen Platzhalter idempotent ergänzen, keine neue formatVersion. Unknowns bewahren und diagnostizieren. Moduswechsel dürfen lokale Werte nicht still umdeuten.

## Risiken und Rückfallstrategie

Großer vorbestehender uncommitteter Bestand; nur aufgabenbezogene Änderungen prüfen und nichts pauschal zurücksetzen/stagen. Unentscheidbare Mathematik bleibt symbolisch. Methoden ohne mathematische Capability liefern Diagnose. Android-Laufzeit benötigt zugänglichen ADB-Host; erste Sandboxprüfung kann keinen lokalen Daemon öffnen.

## Entscheidungsprotokoll

17.10.2026: Nutzer bestätigt gemeinsame Urbildmenge, Integration in Mengenrechner und exakte Berechnung mit symbolischem Fallback. Versionsprüfung bestätigt x-Änderung innerhalb bereits reservierter v3.4.12; keine neue Reservierung oder Versionsänderung durch Implementierer.

## Abweichungen

Die vorhandene PrädikatsMenge war im Wertcodec noch nicht unterstützt, anders als in der ersten Planung angenommen. Ihr strukturierter Codec-Tag wurde ergänzt. Für symbolische mehrstellige Mengen-/Aussagemethoden werden Voraussetzungen erst bei konkreter Mitgliedschaft geprüft; der gemeinsame Aufrufbedingungenpfad erhält ursprüngliche Definitionsausschlüsse vor Substitution. Numerische Anschlussverträge werden auf den vorhandenen Zahltyp normalisiert, um gültige Zahlenquellen nicht wegen Reell-/Rational-Untertypen zu trennen. Struktur und Dimensionen bleiben erhalten.

## Ergebnis und Verifikation

Nachweise:

- `/tmp/atlas-urbild-abschluss.log`: `./gradlew test :app:assembleDebug :app:assembleDebugAndroidTest :desktopApp:compileKotlin --offline`, erfolgreich in 52 s/179 Tasks. 2.380 Fälle, 0 Fehler/Errors, 2 bestehende Opt-in-Skips.
- 19 Kern-, 10 Knoten- und ein nativer Desktop-Urbildtest; Gerätetest separat 1/1 erfolgreich. Desktop- und Androidbilder in `/tmp/atlas-urbild-inspektor.png` und `/tmp/atlas-urbild-android.png`, visuell geprüft. Beide Themes und große Schrift bedient.
- `/tmp/atlas-urbild-android-test.log`: isolierte Android-Prüf-App am SM-X920/Android16, Moduswechsel und atomare Undo/Redo-Prüfung bestanden. Beide Prüf-Pakete anschließend entfernt, Originalinstallation bewahrt.
- `/tmp/atlas-urbild-lint.log`: `./gradlew :app:lintDebug --offline`, erfolgreich in 1m56s/145 Tasks. Nach letzter Migrationsabsicherung: `/tmp/atlas-urbild-migration-final.log` erfolgreich in 35s/149 Tasks (vollständige Knotentests, reguläre APKs, Desktopkompilierung); `/tmp/atlas-urbild-lint-final.log` erfolgreich in 1m47s/145 Tasks.
- Repository-, Methoden-/Architektur-/Desktop-Whitelist-, 15 Standardkarten-, Releaseplan- und Versionsfolgeprüfungen grün; 14 Release-Guard-Regressionen bestanden. `git diff --check` grün. Standalone-Kernskript benötigt hier nicht verfügbares kotlinc; Gradle-Kerntests bestanden.
- Unabhängige node_verifier-Abnahme ohne Blocker. Reviewbefunde zu Standardwerten, unbekannten Signaturen und strukturierten Voraussetzungen behoben und regressionsgeprüft.
- Index unverändert gegenüber `/tmp/atlas-urbild-vorher-index.patch`; vorbestehende Änderungen erhalten. Kein Commit oder Branchwechsel; HEAD/Basis unverändert 20356f1c3c8e996fae03ed10ff10d9fe8005debe, veröffentlichtes v3.4.11. Dessen vorhandene Identität ist Alexander/GitHub; es wurde kein neuer SamAI-Commit erzeugt.
- Releaseklassifikation x innerhalb reservierter v3.4.12. Keine Versionsänderung, Pushes oder Veröffentlichung. Kein neues Gesamtabnahmeversprechen für fremde laufende Visualisierungsarbeiten.
