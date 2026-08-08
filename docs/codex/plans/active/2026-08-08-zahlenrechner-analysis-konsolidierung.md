# Zahlenrechner, Zahlenfunktionen und Analysis konsolidieren

## Status

Aktiv auf `samai/v2.28.2/zahlenrechner-konsolidierung`; Release `v2.28.2` ist als x-Version reserviert. Grundlage ist Issue #340 und der veröffentlichte Commit `c90ccf892eef9e977aad18faa95091978e5d0c37` (`v2.28.1`).

## Ziel und Nutzerwirkung

Der vorhandene Knoten `mathematik.zahlenrechner` soll endlichstellige zahlenwertige Methoden ebenso wie einzelne Zahlterme verarbeiten. Ableitungsfunktion, Differential und Integral werden fachlich getrennt, aber in diesem bestehenden Knoten zusammengeführt. Eigenständige Differential- und Integralknoten bleiben nur als ladekompatible historische Formen erhalten.

Der erste Commit dieses Branches schafft die gemeinsame Grundlage: eine signaturbasierte Zahlenfunktionsanforderung, eine zentrale Methodendarstellung in einer `cases`-Umgebung und die kanonische erste totale Ableitung `f'` einschließlich ihrer Termzeile.

## Nicht-Ziele des ersten Commits

- noch keine punktweise Hebung gewöhnlicher Zahlenoperatoren,
- noch keine gemischten Zahl-/Methodenanschlüsse,
- noch keine Übernahme der Differential- und Integralmodi in den Zahlenrechner-Inspector,
- noch keine Kartenmigration oder Entfernung historischer Vorlagen,
- keine allgemeine Stammfunktionsoperation.

## Untersuchter Istzustand

- `MethodenAnforderung` prüft Stelligkeit, Ergebnisart, Prädikate und Endomorphismen, kennt aber keine Zahlenfunktion.
- `Methode.zuLatex()` rendert nur `f(x)=...`.
- `MathematikKnotenRenderer.termZuMethodeFormel()` besitzt einen privaten Sonderrenderer für die `cases`-Darstellung.
- `AbleitungsMethodenAusdruck` rendert die erste totale Ableitung noch als `f^{\mathrm I}`.
- Mehrdimensionale totale Ableitungen bleiben bereits strukturiert und besitzen den Zielraum `\mathcal L(X,Y)`; ihre konkrete Berechnung ist für allgemeine Signaturen noch nicht implementiert.
- `FundamentalerZahlbereich.QUATERNION` ist im Zahlbereichsgraph registriert, wird aber von `fundamentalerZahlbereichOderNull()` noch nicht als Menge erkannt.

## Fachliche und mathematische Semantik

Eine Zahlenfunktion ist eine Methode

```math
f:W_1\times\cdots\times W_n\longrightarrow Z
```

mit endlich vielen Argumenten sowie `W_i\subseteq\mathbb H` und `Z\subseteq\mathbb H`. Die Prüfung nutzt strukturierte Mengenformen und den fundamentalen Zahlbereichsgraphen. Ein effektiver, nicht-kartesischer Gesamtbereich bleibt erhalten und ersetzt nicht die Prüfung der einzelnen Argumenträume.

Die erste totale Ableitungsfunktion heißt `f'`. Partielle Ableitungsfunktionen bleiben `\partial_i f`; Differentiale werden weiterhin getrennt als `df` beziehungsweise später `d_i f` modelliert. Höhere konkrete Ordnungen behalten die römische Schreibweise ab Ordnung zwei.

## Daten-, Knoten-, Anschluss- und Verbindungsvertrag

Der erste Commit ändert weder Knotenarten noch Anschlüsse oder Verbindungen. Er ergänzt ausschließlich:

- `MethodenAnforderung.Zahlenfunktion`,
- eine zentrale Erkennung bekannter Teilmengen von `\mathbb H`,
- eine wiederverwendbare `cases`-Darstellung einer `Methode`,
- Rendererregeln für die erste totale Ableitung.

Spätere Meilensteine erweitern den Zahlenrechneranschluss kontrolliert um „Zahl oder Methode mit Zahlenfunktionsanforderung“. `mathematik.objekt` wird nicht als ungeprüfter Ersatz eingesetzt.

## Architekturentscheidungen

- Die fachliche Zahlenfunktionsprüfung liegt im Compose-freien Rechenkern.
- Der gemeinsame Methodenrenderer liegt am Domänenobjekt und ist damit für Knotenrenderer, Auswertung und andere Darstellungen wiederverwendbar.
- Bekannte Mengenkonstruktionen werden konservativ als Teilmengen von `\mathbb H` erkannt; unbekannte benannte Mengen werden nicht anhand ihres sichtbaren Namens geraten.
- Der bestehende Typ `Methode` bleibt der einzige physische Methodenlaufzeittyp.

## Betroffene Dateien und Symbole

- `MethodenFundament.kt`: `MethodenAnforderung.Zahlenfunktion`, numerische Mengenerkennung.
- `BeschraenkteZahlmenge.kt`: Quaternionen als fundamentale Zahlmenge erkennen.
- `Methoden.kt`: gemeinsame `cases`-Darstellung.
- `DifferentialModell.kt`: `f'` und Termzeile der symbolischen Ableitungsfunktion.
- `MathematikKnotenRenderer.kt`: Verwendung des gemeinsamen Renderers.
- zugehörige Tests in `MathematikRechenSystem` und `MathematikKnoten`.

## Meilensteine

- [-] M1: Zahlenfunktionsvertrag und gemeinsamer Methodenrenderer.
- [ ] M2: Punktweise Hebung gewöhnlicher Zahlenoperatoren und dynamischer Ausgang.
- [ ] M3: Ableitungsfunktion, Differential sowie Methoden- und Termintegral im Zahlenrechner.
- [ ] M4: Idempotente Kartenmigration und Entfernung der eigenständigen Vorlagen aus dem Erstellen-Katalog.
- [ ] M5: Definitionskarten, Standardbeispiele, vollständige Regression und Releaseabschluss.

## Konkrete Umsetzungsschritte für M1

1. Bekannte fundamentale, beschränkte, endliche, definierte und zusammengesetzte Zahlmengen strukturell klassifizieren.
2. `MethodenAnforderung.Zahlenfunktion` mit positionsgenauer Diagnose ergänzen.
3. `Methode.zuFallunterscheidungsLatex()` als gemeinsame Darstellung einführen und `Methode.zuLatex()` darauf vereinheitlichen.
4. Den privaten Term-zu-Methode-Sonderrenderer auf die gemeinsame Darstellung umstellen; Prädikatskompaktdarstellung bleibt erhalten.
5. Ordnung eins als `f'`, Ordnung zwei und höher weiterhin römisch rendern.
6. Für symbolische mehrdimensionale Ableitungen die Vorschrift als ausgewerteten Term `f'(x,...)` anzeigen, ohne eine konkrete Gradientendarstellung zu erfinden.

## Tests und Validierung

- Unit-Tests für `R^2 -> R`, beschränkte und definierte Zahlmengen, Quaternionenziel und nichtnumerische Argument-/Zielräume.
- Unit-Tests für effektive nicht-kartesische Bereiche.
- Renderer-Tests für gewöhnliche Methoden und `f'` mit `x\mapsto f'(x)` beziehungsweise mehrparametriger Termzeile.
- Differentialtests für `f'`, höhere römische Ordnungen und unveränderte partielle Notation.
- `python3 scripts/pruefe_repository.py`.
- fokussierte Gradle-Tests, danach `./gradlew test` und `./gradlew :app:assembleDebug`, soweit die Laufzeitumgebung dies zulässt.
- `git diff --check` und Abschlussdiff gegen `v2.28.1`.

## Persistenz und Migration

M1 verändert keine persistierten Daten. Die späteren Migrationen erhalten Knoten-, Anschluss- und Verbindungsidentitäten soweit fachlich möglich und bleiben idempotent. Historische Knotentypen bleiben im Loader bekannt.

## Risiken und Rückfallstrategie

- Eine zu großzügige Zahlmengenerkennung könnte nichtnumerische Methoden zulassen. Deshalb werden nur strukturbeweisbare Fälle akzeptiert.
- Die zentrale Methodendarstellung kann viele Anzeigen beeinflussen. Fokussierte Renderer- und vollständige Regressionstests sichern die Umstellung ab.
- Die Schreibweise `f'` darf Differential und partielle Ableitung nicht vermischen. Getrennte Typen und Tests bleiben bestehen.
- Jeder Meilenstein wird als eigener Commit gehalten und kann ohne Schema-Rückmigration einzeln zurückgenommen werden.

## Fortschritt

- [x] Releasezustand geprüft und `v2.28.1` veröffentlicht.
- [x] `v2.28.2` als x-Version reserviert und SamAI-Branch erstellt.
- [-] M1 wird implementiert.

## Entscheidungsprotokoll

- 2026-08-08: Zahlenfunktionen dürfen beliebige endliche Stelligkeit mit numerischen Argumenträumen und numerischer Zielmenge besitzen. Alternative war eine Beschränkung auf einstellige Methoden; verworfen wegen `R^2 -> R`.
- 2026-08-08: `f'`, `df`, `\partial_i f` und `d_i f` bleiben verschiedene Fachobjekte. Eine automatische Gradientendarstellung wurde verworfen.
- 2026-08-08: Integrale werden später als Methoden- oder Termintegral modelliert; ein allgemeiner Stammfunktionsoperator ist kein Ziel.
- 2026-08-08: M1 ändert keine Knoten- oder Persistenzverträge und bildet einen eigenständig prüfbaren ersten Branchcommit.

## Abweichungen vom ursprünglichen Plan

Noch keine.

## Ergebnis und Verifikation

Wird nach Abschluss jedes Meilensteins fortgeschrieben.
