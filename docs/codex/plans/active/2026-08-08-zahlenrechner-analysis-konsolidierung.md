# Zahlenrechner, Zahlenfunktionen und Analysis konsolidieren

## Status

Aktiv für Issue #340. Release `v2.28.2` schließt M1 als x-Version ab; Release `v2.28.3` schließt M2 auf dem Branch `samai/v2.28.3/zahlenfunktionen-punktweise` ab. Grundlage für M2 ist der veröffentlichte Commit `118c1f163ceada52b68e82e6073389a4d81562fc` (`v2.28.2`). M3–M5 bleiben für nachfolgende Releases offen.

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

- [x] M1: Zahlenfunktionsvertrag und gemeinsamer Methodenrenderer — in `v2.28.2` abgeschlossen.
- [x] M2: Punktweise Hebung gewöhnlicher Zahlenoperatoren und dynamischer Ausgang — in `v2.28.3` abgeschlossen.
- [ ] M3: Ableitungsfunktion, Differential sowie Methoden- und Termintegral im Zahlenrechner — Folgerelease, weiterhin Teil von #340.
- [ ] M4: Idempotente Kartenmigration und Entfernung der eigenständigen Vorlagen aus dem Erstellen-Katalog — Folgerelease, weiterhin Teil von #340.
- [ ] M5: Definitionskarten, Standardbeispiele und vollständige End-to-End-Regression — Folgerelease, weiterhin Teil von #340.

## Konkrete Umsetzungsschritte für M1

1. Bekannte fundamentale, beschränkte, endliche, definierte und zusammengesetzte Zahlmengen strukturell klassifizieren.
2. `MethodenAnforderung.Zahlenfunktion` mit positionsgenauer Diagnose ergänzen.
3. `Methode.zuFallunterscheidungsLatex()` als gemeinsame Darstellung einführen und `Methode.zuLatex()` darauf vereinheitlichen.
4. Den privaten Term-zu-Methode-Sonderrenderer auf die gemeinsame Darstellung umstellen; Prädikatskompaktdarstellung bleibt erhalten.
5. Ordnung eins als `f'`, Ordnung zwei und höher weiterhin römisch rendern.
6. Für symbolische mehrdimensionale Ableitungen die Vorschrift als ausgewerteten Term `f'(x,...)` anzeigen, ohne eine konkrete Gradientendarstellung zu erfinden.

## Konkrete Umsetzungsschritte für M2

1. Standard- und erweiterte Zahlenoperatoren vollständig als punktweise, methodenspezifisch oder Analysis klassifizieren.
2. Gewöhnliche Zahlenoperatoren kontrolliert für Zahl- oder Methodeneingänge öffnen und den Ausgang zentral auf `Methode` priorisieren, sobald mindestens eine Zahlenfunktion verbunden ist.
3. Gleichstellige Zahlenfunktionen auf die Parameter der ersten Methode alpha-umbenennen und ihre Argumenträume komponentenweise schneiden; Skalare bleiben konstante Operanden.
4. Die skalare Operatorauswertung wiederverwenden und ihr Ergebnis mit gemeinsamer Signatur, Zielmenge und strukturierten Definitionsbedingungen in eine `Methode` heben.
5. Division, Kehrwert, Logarithmen, reelle Wurzeln, Arkusfunktionen, Modulo und erweiterte Quotientenoperatoren mit strukturierten Bedingungen versehen.
6. Quaternionische Faktor- und Divisionsreihenfolge erhalten sowie die bestehende strukturierte Divisionsseite auch in punktweisen Methoden fortführen.
7. Bestehende Zahlenrechner idempotent auf die erweiterten Anschlussverträge migrieren, Anschluss- und Verbindungs-IDs erhalten und die neue Ausgangsregel im Karten-JSON roundtrippen.

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
- [x] M1 implementiert und für `v2.28.2` als eigener Releaseumfang abgegrenzt.
- [x] M2 als x-Version `v2.28.3` klassifiziert und SamAI-Branch vom veröffentlichten `v2.28.2`-Commit erstellt.
- [x] Punktweise Operatorhebung, dynamische Ausgangsart, strukturierte Definitionsbedingungen und idempotente Anschlussmigration implementiert.

## Entscheidungsprotokoll

- 2026-08-08: Zahlenfunktionen dürfen beliebige endliche Stelligkeit mit numerischen Argumenträumen und numerischer Zielmenge besitzen. Alternative war eine Beschränkung auf einstellige Methoden; verworfen wegen `R^2 -> R`.
- 2026-08-08: `f'`, `df`, `\partial_i f` und `d_i f` bleiben verschiedene Fachobjekte. Eine automatische Gradientendarstellung wurde verworfen.
- 2026-08-08: Integrale werden später als Methoden- oder Termintegral modelliert; ein allgemeiner Stammfunktionsoperator ist kein Ziel.
- 2026-08-08: M1 ändert keine Knoten- oder Persistenzverträge und bildet einen eigenständig prüfbaren ersten Branchcommit.
- 2026-08-08: M2 verwendet keinen neuen physischen Methoden- oder Knotentyp. Der bestehende Zahlenrechner erhält kontrollierte Zahl-/Methodenanschlüsse; die effektive Ausgangsart wird zentral aus allen methodenfähigen Eingängen priorisiert.
- 2026-08-08: Punktweise Operanden müssen dieselbe Stelligkeit besitzen. Abweichende Parameternamen werden alpha-umbenannt, die jeweiligen Argumenträume werden komponentenweise geschnitten und nicht-kartesische effektive Bereiche zusätzlich erhalten.
- 2026-08-08: Iterierte Summe und iteriertes Produkt bleiben methodenspezifische Operatoren; Integral und Differential bleiben Analysisoperatoren. Nur explizit klassifizierte gewöhnliche Operatoren werden punktweise gehoben.

## Abweichungen vom ursprünglichen Plan

Noch keine.

## Ergebnis und Verifikation

M1 liefert eine signaturbasierte `MethodenAnforderung.Zahlenfunktion`, die gemeinsame `cases`-Darstellung für Methoden sowie die kanonische erste totale Ableitungsfunktion (f') einschließlich Termzeile. Konkrete Methodenaufrufe verwenden weiterhin ausschließlich die Methodenreferenz, beispielsweise (f(4)), und hängen Argumente nicht an die vollständige Methodendefinition.

Verifiziert wurden:

- Repository- und Architekturprüfung;
- vollständige JVM-Tests und `:app:assembleDebug` im GitHub-Actions-Lauf `31262381674`;
- Regressionstests für ein- und mehrstellige Methodenaufrufe;
- strukturierte Zahlmengen einschließlich beschränkter, definierter, gefilterter und zusammengesetzter Mengen;
- Abschlussdiff gegen `v2.28.1`: keine neuen Knotentypen und keine Persistenz- oder Anschlussmigration.

Die Veröffentlichung `v2.28.2` umfasst bewusst nur M1. Die damals noch offene punktweise Operatorhebung wird getrennt als M2 in `v2.28.3` veröffentlicht.

M2 in `v2.28.3` ergänzt die punktweise Hebung für gewöhnliche Standard- und Erweiterungsoperatoren. Mehrstellige Zahlenfunktionen, gemischte Zahl-/Methodenoperanden, gemeinsame Definitionsräume, dynamische Methodenausgänge, strukturierte Definitionsbedingungen und quaternionische Reihenfolge sind durch fokussierte Regressionstests abgedeckt. Die Anschlussregel ist optional persistiert; alte Karten werden idempotent unter Erhalt ihrer Identitäten erweitert. M3–M5 bleiben in Issue #340 offen.
