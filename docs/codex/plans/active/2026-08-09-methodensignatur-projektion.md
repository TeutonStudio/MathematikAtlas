# Methodensignatur auslesen und Argumentanschlüsse projizieren

## Status

Implementierung auf `samai/v2.30.0-methoden-signaturprojektion` abgeschlossen; unabhängige Verifikation und CI stehen aus.

## Ziel und Nutzerwirkung

Methoden sollen auf Karten ihren Wertevorrat, ihre Zielmenge und ihre Argumentanzahl explizit auslesbar machen. Jeder produktive Methodenausgang soll außerdem rein auf Kartenebene festlegen können, ob die logischen Methodenargumente als getrennte Anschlüsse oder als ein einzelner Tupelanschluss dargestellt werden.

Die mathematische `Methode` bleibt von dieser Kartenprojektion vollständig unabhängig und wird intern immer gleich gespeichert.

## Nicht-Ziele

- Keine Speicherung einer Argumentprojektion in `Methode` oder `MethodenSignatur`.
- Keine Gleichsetzung von Argumentanzahl und Vektorraumdimension.
- Kein automatisches Hochstufen eines Skalars zu einem Einertupel.
- Kein Currying oder Ändern der logischen Methodenstelligkeit.
- Kein Release-/Master-Merge in diesem Implementierungsbranch.

## Untersuchter Istzustand

- `MethodenSignatur` besitzt geordnete `argumente`, `zielMenge` und `effektiverWerteVorrat`.
- `MethodenSignatur.werteVorrat` modelliert den Standardbereich als `Tupelraum(argumente.map { it.werteVorrat })`.
- `Methode.argumentRaum()` kollabierte vor dieser Änderung den Einertupel-Fall wieder zum einzelnen Wertebereich.
- `mathematik.methodenZielmenge` existiert bereits.
- `Methode aufrufen` synchronisierte bislang ausschließlich getrennte Argumentanschlüsse aus `methode.parameter.size`.
- `MathematikAnschlussArten.Methode` ist die einzige produktive Methodenanschlussart; spezialisierte historische IDs sind nur Lade-Kompatibilität.
- `Tupel` erlaubt bereits Listen mit genau einem Element; `Tupelraum` erlaubt jede nichtleere Komponentenliste.

## Fachliche und mathematische Semantik

Für

```math
f:W_1\times\cdots\times W_n\to Z
```

gilt:

- Wertevorrat: `W_1 × ... × W_n`, gegebenenfalls ersetzt durch `effektiverWerteVorrat`.
- Zielmenge: `Z`.
- Argumentanzahl / Stelligkeit: `n`, ausschließlich die Länge der geordneten Parameterliste.
- Dimension ist kein Synonym für Argumentanzahl und bleibt dem Vektorraumbegriff vorbehalten.

Beispiel: Eine Methode mit zwei komplexen Argumenten besitzt Argumentanzahl `2`. Wird ihr Argumentraum als reeller Vektorraum betrachtet, kann dieser Dimension `4` besitzen; daraus folgt keine andere Argumentanzahl.

## Daten-, Node-, Handle- und Edge-Vertrag

### Neue Knotentypen

`mathematik.methodenWertevorrat`

- Eingang `methode : mathematik.methode`
- Ausgang `menge : mathematik.menge`
- Auswertung über `methode.methodenSignatur().werteVorrat`

`mathematik.methodenArgumentanzahl`

- Eingang `methode : mathematik.methode`
- Ausgang `anzahl : mathematik.zahl`
- Auswertung über die rein strukturelle Ableitung `Methode.argumentAnzahl = parameter.size`

### Bestehender Knotentyp

`mathematik.methodenZielmenge` bleibt stabil und liest künftig `methode.methodenSignatur().zielMenge`.

### Kartenprojektion

Jeder produktive Methodenausgang persistiert optional:

```text
methodenAusgang.<anschlussName>.argumentprojektion = separiert | tupel
```

Fehlt der Parameter, gilt `separiert`.

`Methode aufrufen` folgt seiner konkreten Methodenkante zurück zum Quellausgang und synchronisiert:

- `separiert`: ein typisierter Argumentanschluss je logischem Parameter,
- `tupel`: genau ein `mathematik.tupel`-Anschluss für alle logischen Parameter,
- nullstellig: kein Argumentanschluss in beiden Modi.

Im Tupelmodus wird erst an der Aufruf-/Adaptergrenze positionsgetreu entpackt. Danach läuft dieselbe kanonische `Methode.wendeAn(...)`-Operation.

## Architekturentscheidungen

1. Die Kartenprojektion ist Knoten-/Anschlusskonfiguration und kein mathematischer Zustand.
2. Projektionsinformation wird nicht durch `BedingterWert` oder Methodentransformationen getragen. Der Aufruf ermittelt sie aus der tatsächlichen Graphkante.
3. `Methode.argumentAnzahl` hängt nicht von der Vollständigkeit der Wertebereiche ab. Historische Methoden mit fehlendem Parameter-Wertevorrat behalten daher eine auslesbare Argumentanzahl.
4. Einertupel bleiben strukturell Tupel; der Methoden-Wertevorrat kollabiert sie nicht.

## Betroffene Dateien und Symbole

- `MathematikRechenSystem/.../MethodenFundament.kt`: `Methode.argumentAnzahl`, Einertupel-Terminologie.
- `MathematikRechenSystem/.../MethodenGraph.kt`: kanonischer `argumentRaum`.
- `MathematikKnoten/.../FaltungsKnoten.kt`: neue Signatur-Abfrageknoten.
- `MathematikKartenAdapter/.../MethodenSignaturAuswerter.kt`: Abfrageauswertung.
- `MathematikKartenAdapter/.../MethodenAufrufAuswerter.kt`: Tupel-/Einzelprojektion an der Adaptergrenze.
- `MathematikKartenAdapter/.../Auswertung.kt`: Registry-Routing.
- `app/.../MethodenAufrufSynchronisierung.kt`: Quellenprojektion und dynamische Handles.
- `app/.../KnotenInspektorFenster.kt`: generischer Inspector pro Methodenausgang.
- Konzeptmetadaten unter `MathematikKnoten/.../konzeptknoten/`.
- Tests in Rechenkern, Adapter und App.

## Versionswirkung

Klassifikation: `y`-Version.

Ausgehend vom veröffentlichten `v2.29.1` ist die nächste zulässige Knoten-Version `v2.30.0`.

Neue Typ-Schlüssel:

- `mathematik.methodenWertevorrat`
- `mathematik.methodenArgumentanzahl`

Der Arbeitsbranch trägt deshalb `samai/v2.30.0-methoden-signaturprojektion`. `release/roadmap.toml` wird in diesem Implementierungsbranch nicht auf einen aktiven Status umgestellt, weil der aktuelle maschinenlesbare Release-Validator aktive `reserved/implementing/review/ready`-Einträge ausdrücklich ablehnt. Dieser dokumentierte Widerspruch zwischen Release-Dokumentation und Validator muss vor einem Release-PR durch den Master-Verwalter aufgelöst werden.

## Meilensteine

- [x] Istzustand von Methodensignatur, Aufruf und Inspector untersucht.
- [x] Begriffe Argumentanzahl und Vektorraumdimension getrennt.
- [x] Einertupel-Wertevorrat kanonisiert.
- [x] Wertevorrat- und Argumentanzahlknoten registriert und ausgewertet.
- [x] Zielmengenknoten auf Signaturvertrag vereinheitlicht.
- [x] Quellenbezogene Kartenprojektion `separiert | tupel` implementiert.
- [x] Generischen Inspector für produktive Methodenausgänge ergänzt.
- [x] Tupelaufruf einschließlich Einertupel, falscher Tupellänge und nullstelliger Methode implementiert.
- [x] Regressionstests ergänzt.
- [ ] CI-/Gradle-Prüfungen ausführen.
- [ ] Unabhängige Node-Verifikation durchführen.
- [ ] Release-Governance-Widerspruch für `v2.30.0` klären.

## Konkrete Umsetzungsschritte

1. `Methode.argumentAnzahl` direkt aus `parameter.size` ableiten.
2. `Methode.argumentRaum()` vollständig an `MethodenSignatur.werteVorrat` delegieren.
3. neue Knoten und Auswerter ergänzen; bestehenden Zielmengenauswerter vereinheitlichen.
4. allgemeinen Methodenaufruf vom Legacy-Einargumentpfad trennen.
5. Projektion am Quellausgang persistieren und beim nachfolgenden Aufruf aus der konkreten Kante ableiten.
6. dynamische Argumenthandles entsprechend synchronisieren.
7. generischen Inspector pro produktivem Methodenausgang ergänzen.
8. Kern-, Adapter- und App-Regressionstests ergänzen.
9. Konzeptmetadaten angleichen.

## Tests und Validierung

Ergänzte Tests decken ab:

- einstelligen `Tupelraum(listOf(W))`,
- effektiven nichtkartesischen Wertevorrat,
- Wertevorrat, Zielmenge und Argumentanzahl einer Methode mit zwei komplexen Argumenten,
- Argumentanzahl auch bei fehlendem Parameter-Wertevorrat,
- positionsgetreues Entpacken eines Tupels,
- echtes Einertupel,
- Ablehnung eines Skalars im Tupelmodus,
- Diagnose falscher Tupellänge,
- getrennte vs. Tupel-Handle-Synchronisierung,
- nullstellige Methoden,
- stabile Projektionsschlüssel pro Methodenausgang.

Nicht ausgeführt: Gradle-/Repository-Prüfungen. Der GitHub-Connector stellt keinen lokalen Checkout bereit; die vorhandene Kern-CI startet automatisch auf Pull Requests beziehungsweise `master`, nicht auf einem bloßen Feature-Branch.

## Persistenz und Migration

Die Projektionswahl wird als normaler Knotenparameter gespeichert. Bestehende Karten besitzen den Schlüssel nicht und laden daher im kompatiblen Standardmodus `separiert`.

`Methode` und `MethodenSignatur` erhalten kein neues persistiertes Feld. Eine Migration mathematischer Methodenobjekte ist daher nicht erforderlich.

Historische Methodenanschluss-IDs bleiben von der bestehenden Anschlussnormalisierung unberührt.

## Risiken und Rückfallstrategie

- Beim Umschalten einer bereits stark verbundenen Aufrufstruktur kann die bestehende dynamische Anschluss-Synchronisierung inkompatible Edges entfernen. Eine explizite Vorabdiagnose/Bestätigungs-UX für diesen Wechsel ist noch nicht umgesetzt und bleibt vor Release zu verifizieren.
- Die Release-Dokumentation und `scripts/pruefe_releaseplan.py` widersprechen sich bei aktiven Reservierungszuständen. Kein Release-Metadaten-Diff wird geraten, bevor dieser Vertrag geklärt ist.
- Bei Problemen kann die Kartenprojektion vollständig entfernt werden, ohne `Methode` oder `MethodenSignatur` migrieren zu müssen.

## Fortschritt

2026-08-09: Produktcode, Inspector und Regressionstests implementiert. Branchherkunft nach Prüfung von `AGENTS.md` von einem zunächst erzeugten `agent/`-Arbeitsbranch auf den zulässigen Branch `samai/v2.30.0-methoden-signaturprojektion` korrigiert. Der frühere Connector-Branch bleibt technische Zwischenhistorie und ist nicht der Übergabebranch.

## Entscheidungsprotokoll

### 2026-08-09: Dimension nicht für Argumentanzahl verwenden

Entscheidung: Argumentanzahl wird ausschließlich aus der geordneten Parameterliste abgeleitet.

Alternative: aus einer Raum-/Koordinatendimension ableiten.

Begründung: Vektorraumdimension und Anzahl logischer Methodenargumentplätze sind unabhängige Begriffe.

Konsequenz: `Methode.argumentAnzahl = parameter.size`.

### 2026-08-09: Projektion nicht in Methode speichern

Entscheidung: `separiert | tupel` ist ausschließlich Kartenkonfiguration am konkreten Methodenausgang.

Alternative: Feld in `Methode` oder `MethodenSignatur`.

Begründung: Dieselbe mathematische Methode soll an unterschiedlichen Kartenstellen unterschiedlich projiziert werden können, ohne mathematisch verschiedene Objekte zu erzeugen.

Konsequenz: der nachfolgende Aufruf liest die Einstellung über die konkrete Kante vom Quellausgang.

### 2026-08-09: Einertupel strukturell erhalten

Entscheidung: `(x)` bleibt `Tupel(listOf(x))`; `Tupelraum(listOf(W))` bleibt vom Skalarraum `W` unterscheidbar.

Alternative: einstellige Tupel automatisch kollabieren.

Begründung: Tupelstruktur und Vektorraumdimension sind unabhängige Konzepte und der Tupelmodus muss auch für einstellige Methoden funktionieren.

## Abweichungen vom ursprünglichen Plan

- Der zunächst vorgeschlagene Begriff „Argumentdimension“ wurde verworfen und vollständig durch „Argumentanzahl / Stelligkeit“ ersetzt.
- Ein zunächst erwogenes `argumentForm`-Feld im Methodenvertrag wurde verworfen. Die Projektion liegt ausschließlich auf Kartenebene.
- Die Argumentanzahl wird direkt aus `Methode.parameter` statt über `methodenSignatur().argumente` gelesen, damit sie nicht von vollständigen Wertevorratsmetadaten abhängt.

## Ergebnis und Verifikation

Der Implementierungsstand erfüllt die Kernsemantik von Issue #369. Die Code- und Teständerungen liegen im Branch `samai/v2.30.0-methoden-signaturprojektion`. CI, unabhängige Verifikation und die explizite Edge-Migrations-UX beim Projektionswechsel sind vor einem Release weiterhin offen.