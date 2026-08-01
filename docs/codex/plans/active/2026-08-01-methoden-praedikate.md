# Methodenfundament und Prädikatsdarstellung

- Datum: 2026-08-01
- Status: aktiv
- Release: v2.12.3, Änderungs-Version
- Branch: `agent/v2.12.3-methoden-praedikate`
- Issues: #67, anschließend #64

## Ziel und Nutzerwirkung

Methoden werden intern über einen gemeinsamen fachlichen Vertrag und einen gemeinsamen Anschluss transportiert. Die Nutzeroberfläche zeigt die daraus berechneten Aliase Funktion, Abbildung und Prädikat. Prädikate erhalten anschließend eine kompakte, argumentabhängige Darstellung und Wahrheitstabellen behandeln unbekannte Ergebnisse als reguläre Zustände.

Die Umsetzung besteht bewusst aus zwei unmittelbar aufeinanderfolgenden Arbeitscommits:

1. Methodenfundament für #67.
2. Prädikatsauflösung, Darstellung und Wahrheitstabellen für #64.

Der Pull Request beansprucht nur die Releaseversion v2.12.3. Auf `master` entsteht durch Squash genau ein finaler Releasecommit.

## Nicht-Ziele

- keine neuen Knotentypen,
- keine neue Persistenz-`formatVersion`,
- keine geometrische Klassifikation,
- kein zusätzlicher Begriff für Mengenfamilientransformationen,
- keine automatische Umbenennung doppelter Prädikatsnamen,
- kein Emulator- oder Geräte-Smoke-Test ohne verfügbare Android-Laufzeitumgebung.

## Untersuchter Istzustand

- `Funktion` enthält bereits Parameter, Wertevorräte, Ausgaben und Zielmengen.
- Kartenmethoden können mehrere öffentliche Ausgänge als benannte Ausgabemap exportieren.
- Methodenanschlüsse sind als `mathematik.funktion` und mehrere Unterarten registriert.
- Wahrheitsmengen werden an mehreren Stellen lokal erzeugt.
- `BedingterWert.variablenQuellen` erfasst bisher nur gewöhnliche Variablenquellen.
- Der Renderer erzwingt für `mathematik.termZuMethode` eine allgemeine Funktionsdarstellung.
- Wahrheitstabellen besitzen bereits einen unbekannten Entscheidungszustand, verwenden aber noch keine zentrale Prädikatsauflösung.

## Fachliche Semantik

- Eine Methode besitzt eine geordnete Argumentliste, einen daraus gebildeten Tupelwertevorrat, eine Vorschrift und eine Zielmenge.
- Nullstellige Methoden besitzen gemäß Produktfestlegung den Wertevorrat `∅` und werden mit einer leeren Argumentbelegung angewendet.
- Historische Mehrfachausgaben werden als ein geordnetes Tupel interpretiert.
- Für jedes Element des Wertevorrats muss ein Ergebnis entstehen.
- Unbekannte und unentscheidbare Aussagen sind gültige Ergebnisse.
- Funktion, Abbildung und Prädikat sind berechnete Aliase unter Methode.

## Daten-, Knoten-, Anschluss- und Verbindungsvertrag

- Produktive Methodenanschlussart: `mathematik.methode`.
- Alte `mathematik.funktion.*`-IDs werden beim Laden unter Erhalt von Knoten-, Anschluss- und Verbindungs-IDs ersetzt.
- `Term zu Methode` und `Aussage zu Methode` bleiben Vorlagen desselben Knotentyps.
- Eine Karte im Methodenzustand liefert einen Methodenanschluss. Mehrere öffentliche Ausgänge bilden ein Ergebnistupel.
- Verbindungen zwischen Methodenanschlüssen sind graphseitig zulässig; semantische Anforderungen prüft der jeweilige Auswerter.

## Architekturentscheidungen

Die dauerhafte Entscheidung ist in `docs/codex/decisions/2026-08-01-einheitliches-methodenmodell.md` dokumentiert. Der Rechenkern enthält Klassifikation und Prädikatssemantik, der Adapter Argumentquellen und Kartenexport, `MathematikKnoten` die Darstellung und `app` Lade-Migration, Namensprüfung und Wahrheitstabellenkoordination.

## Betroffene Dateien und Symbole

### Commit 1

- `MathematikRechenSystem/.../MethodenFundament.kt`
- `MathematikRechenSystem/.../Wertebereiche.kt`
- `MathematikKartenAdapter/.../KartenEingangAuswerter.kt`
- `MathematikKartenAdapter/.../KartenAuswerter.kt`
- `MathematikKnoten/.../MathematikAnschlussArten.kt`
- `app/.../speicher/MethodenAnschlussMigration.kt`
- `app/.../speicher/KartenSpeicher.kt`
- Kern- und Migrationstests

### Commit 2

- `MathematikRechenSystem/.../PraedikatsDarstellung.kt`
- `MathematikKartenAdapter/.../Auswertung.kt`
- `MathematikKartenAdapter/.../KartenAuswerter.kt`
- `MathematikKnoten/.../MathematikKnotenRenderer.kt`
- `app/.../KartenWahrheitstabellen*.kt`
- Prädikats-, Darstellungs- und Wahrheitstabellentests
- Releaseplan und Android-Version

## Meilensteine

- [-] M1: Methodenmodell, eine Zielmenge und Alias-Klassifikation.
- [-] M2: gemeinsamer Anschluss, Lade-Migration und Kartenmethoden-Tupel.
- [ ] M3: Argumentquellen und zentrale Prädikatsauflösung.
- [ ] M4: kompakte Darstellung, Namenseindeutigkeit und Wahrheitstabellen.
- [ ] M5: Releasemetadaten, vollständige Prüfungen und unabhängige Diffkontrolle.

## Konkrete Umsetzungsschritte

1. Kanonischen Methodennamen, Signatur und Wahrheitsmenge im Rechenkern ergänzen.
2. Bestehende Ausgabemaps über eine kanonische Vorschrift und Zielmenge lesen; Kartenexport auf ein Ergebnisobjekt umstellen.
3. Neue Vorlagen auf `mathematik.methode` umleiten und alte IDs nur als Lade-Aliase registrieren.
4. Idempotente Migration in alle dateibasierten Kartenladepfade integrieren.
5. Allgemeine Werte- und Aussageargumentquellen mit stabiler Identität einführen.
6. Prädikate rekursiv beziehungsweise bindungsbewusst auflösen und kompakt formatieren.
7. Renderer und Wahrheitstabellen auf dieselbe Auflösung umstellen.
8. Doppelte offene Prädikatsnamen als fachlichen Kartenfehler melden.
9. Tests, Releaseplan und Android-Version aktualisieren.

## Tests und Validierung

Geplant:

```text
python3 scripts/pruefe_repository.py
python3 scripts/pruefe_releaseplan.py
python3 scripts/pruefe_versionsfolge.py
python3 scripts/pruefe_kern.py
./gradlew test
./gradlew :app:assembleDebug
```

Zusätzlich gezielte Tests für:

- geordnete Methodensignatur und nullstellige Methode,
- Tupelergebnis und Alias-Klassifikation,
- Anschlussmigration mit stabilen IDs und Verbindungen,
- reine und gemischte Prädikatsdarstellung,
- fehlende Wertevorräte,
- Deduplizierung semantisch identischer Argumente,
- unbekannte beziehungsweise unentscheidbare Wahrheitstabellenzellen,
- doppelte Prädikatsnamen.

## Persistenz und Migration

Das JSON-Schema bleibt unverändert. Beim Laden werden ausschließlich Anschlussart-IDs normalisiert. Instanz-IDs, Kartenverweise und Verbindungen bleiben bestehen. Alte IDs bleiben im Anschlussregister, damit auch noch nicht durch einen App-Ladepfad gelaufene Karten verständlich fehlschlagen statt unbekannte Typen zu erzeugen.

## Risiken und Rückfallstrategie

- Grobe Methodenanschlüsse verschieben Fehler von der Graphprüfung in die fachliche Auswertung. Fehlertexte müssen daher konkret sein.
- Quellkompatibilitätsnamen können den Eindruck mehrerer Typen erwecken. ADR und neue Produktionspfade verwenden deshalb kanonisch `Methode`.
- Rekursive Prädikate können Zyklen bilden. Die Auflösung erhält eine Besuchsmenge und meldet Zyklen als Fachfehler.
- Bei einem Migrationsfehler kann auf die registrierten Legacy-Anschlussarten zurückgefallen werden, ohne persistierte IDs zu verlieren.

## Fortschritt

- [x] Istzustand, Issues, Master-HEAD, offene PRs und Releaseklassifikation geprüft.
- [-] Commit 1 vorbereitet.
- [ ] Commit 1 durch CI verifiziert.
- [ ] Commit 2 umgesetzt.
- [ ] vollständige CI und Abschlussdiff geprüft.

## Entscheidungsprotokoll

### 2026-08-01: eine Releaseversion trotz zweier Arbeitscommits

- Entscheidung: Beide Issues werden in v2.12.3 veröffentlicht, bleiben im Branch aber als zwei aufeinanderfolgende Commits getrennt.
- Alternative: v2.13.0 und v2.13.1.
- Begründung: Es entstehen keine neuen Knotentypen; nach Repositoryvertrag ist ausschließlich die nächste Änderungs-Version zulässig. Ein PR darf nur eine Releaseversion beanspruchen.
- Konsequenz: Releasemetadaten werden erst im zweiten Commit auf v2.12.3 gesetzt; der Squash-Merge erzeugt den einzigen Releasecommit.

### 2026-08-01: Quellkompatibilitätsalias statt paralleler Klasse

- Entscheidung: `Methode` bezeichnet denselben Laufzeittyp wie die bestehende Klasse `Funktion`.
- Alternative: zweite Datenklasse oder sofortige repositoryweite mechanische Umbenennung.
- Begründung: Eine zweite Klasse widerspricht dem Ziel; eine reine Komplettumbenennung erhöht Migrationsrisiko ohne fachlichen Gewinn.
- Konsequenz: Neue Semantik und neue Produktionspfade verwenden `Methode`; alter Quellcode bleibt kompilierbar.

## Abweichungen vom ursprünglichen Plan

Die ursprünglich vorgeschlagenen Versionen v2.13.0 und v2.13.1 wurden verworfen, weil sie der im Repository verbindlich definierten Versionsklassifikation widersprechen. Die fachliche Trennung in zwei Commits bleibt unverändert.

## Ergebnis und Verifikation

Wird nach Abschluss von Commit 2 und den CI-Läufen ergänzt.
