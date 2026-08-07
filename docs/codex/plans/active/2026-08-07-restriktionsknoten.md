# Restriktionsknoten mit priorisierter Bereichserweiterung

## 1. Status

Stand: Implementierung, Tests und unabhängiger Diff-Abgleich auf `samai/v2.27.0/notiz-knoten`; finale CI-Verifikation des aktuellen Heads und Releaseintegration noch offen.

Zugehörig: #334, PR #336. Der Knotentyp `mathematik.restriktion` ist Teil derselben neuen y-Version v2.27.0 wie `karte.notiz`.

## 2. Ziel und Nutzerwirkung

Der Atlas erhält einen Knoten, der eine Methode `f: W -> Z` auf eine gewünschte Menge `M` restringiert. Liegt `M` nicht vollständig im bisherigen Wertevorrat, fordert der Knoten nacheinander Ergänzungsmethoden an, bis jeder Punkt von `M` durch die Basismethode oder eine Ergänzung abgedeckt ist.

Die Ausgabe bleibt eine Methode mit unveränderter Zielmenge `Z`, effektivem Wertevorrat `M` und strukturierter Herkunft der Bereichsanpassung.

## 3. Nicht-Ziele

- keine neue Anschlussart für Restriktionen,
- keine separate Erweiterungsknotenfamilie,
- kein Überschreiben früher definierter Werte durch spätere Ergänzungen,
- keine Approximation symbolisch unbekannter Mengenbeziehungen,
- keine Änderung des allgemeinen Graph- oder Edge-Vertrags.

## 4. Untersuchter Istzustand

- `Methode` speicherte bisher nur einzelne Parameter-Wertevorräte und konnte einen nichtkartesischen Gesamtdefinitionsbereich nicht ausdrücken.
- `MethodenAufrufSynchronisierung.kt` zeigt das bestehende Muster für abgeleitete dynamische Anschlüsse mit stabilen IDs.
- `KartenAuswerter` stellt Eingänge topologisch anhand der Anschlussnamen bereit.
- `MathematikKnotenRenderer` kann den resultierenden LaTeX-Ausgang ohne eigenen Restriktionsrenderer darstellen.
- Der Inspector besitzt einen spezialisierten Registrierungs-/Dispatchpfad.

## 5. Fachliche und mathematische Semantik

Für `f: W -> Z` und gewünschtes `M` gilt zunächst die Basismethode auf `M ∩ W`.

Für Ergänzungsmethoden `g_i: W_i -> Z_i` werden sukzessive Restmengen verwendet:

`R_0 = M \ W`

`A_i = R_{i-1} ∩ W_i`

`R_i = R_{i-1} \ W_i`

Nur `A_i` ist der effektive Bereich der Ergänzung `g_i`. Frühere Zweige besitzen Priorität. Eine spätere Ergänzung kann daher einen früher definierten Wert nicht überschreiben.

Zielverträglichkeit wird zuerst über `Z_i ⊆ Z` geprüft. Ist das nicht beweisbar oder sogar falsch, wird nur der tatsächlich verwendete Bereich betrachtet: `g_i[A_i] ⊆ Z`. Endliche effektive Bereiche werden punktweise geprüft. Symbolisch unentscheidbare Abdeckungs- oder Zielbedingungen werden als Aussagen weitergereicht, nicht geraten.

Die resultierende Methode darf nur gebildet werden, wenn die Abdeckung nicht widerlegt ist und keine Ergänzung auf ihrem effektiven Bereich nachweislich außerhalb von `Z` abbildet.

## 6. Daten-, Knoten-, Anschluss- und Edge-Vertrag

Knotenart: `mathematik.restriktion`.

Feste Anschlüsse:

- Eingang `methode`, Art `mathematik.methode`, Reihenfolge 0,
- Eingang `menge`, Art `mathematik.menge`, Reihenfolge 1,
- Ausgang `methode`, Art `mathematik.methode`.

Dynamische Ergänzungseingänge heißen `ergänzung.<n>` und sind Methodeneingänge. Ist die Abdeckung nicht bewiesen vollständig, existiert genau ein unverbundener nächster Ergänzungseingang. Verbundene Ergänzungseingänge sowie ein bereits sichtbarer freier Folgeanschluss behalten ihre ID, solange der fachliche Zustand keinen anderen Anschlussvertrag verlangt.

Die ausgegebene `Methode` trägt zusätzlich eine `MethodenBereichsanpassung` mit:

- ursprünglicher Basismethode,
- gewünschtem Wertevorrat `M`,
- geordneten Ergänzungsmethoden,
- jeweiligem deklariertem `W_i`,
- jeweiligem effektivem Bereich `A_i`.

Damit ist `f\vert_M` nur die kompakte Darstellung; die vollständige stückweise Semantik bleibt rekonstruierbar.

## 7. Architekturentscheidungen

- Der fachliche Restriktionsalgorithmus liegt im Compose-freien `MathematikRechenSystem`.
- `Methode` erhält optional `effektiverWerteVorrat`, damit insbesondere nichtkartesische Restriktionen mehrstelliger Methoden korrekt darstellbar sind.
- `Methode` erhält optional `bereichsanpassung`, damit Basis und geordnete Ergänzungszweige downstream strukturiert erhalten bleiben.
- Dynamische Anschlussableitung liegt in `MathematikKnoten`, nicht im fachneutralen Editor.
- App-Code koordiniert Synchronisierung und Inspector, enthält aber keine Restriktionsmathematik.
- Die existierende Methoden-Anschlussart wird wiederverwendet.

## 8. Betroffene Dateien und Symbole

- `MathematikRechenSystem/.../MethodenFundament.kt`: `MethodenSignatur.effektiverWerteVorrat`.
- `MathematikRechenSystem/.../Methoden.kt`: `Methode.effektiverWerteVorrat`, `Methode.bereichsanpassung` und Propagation.
- `MathematikRechenSystem/.../MethodenRestriktion.kt`: Restriktionsmodell, strukturierte Bereichsanpassung und Algorithmus.
- `MathematikKnoten/.../RestriktionsKnoten.kt`: Vorlage, Auswerter, dynamische Anschlüsse.
- `MathematikKnoten/.../geometrie/GesamterMathematikAuswerter.kt`: Registrierung.
- `MathematikKnoten/.../AussagenLogikKnoten.kt`: sichtbarer Vorlagenkatalog.
- `app/.../AtlasZustand.kt`: Anschluss-Synchronisierung.
- `app/.../RestriktionsKnotenInspektor.kt`: Diagnose.
- zugehörige Kern-, Knoten- und Persistenztests.

## 9. Meilensteine

- [x] Fachlichen Bereichsvertrag und effektiven Gesamtwertebereich implementiert.
- [x] Strukturierte Bereichsanpassung in der Ausgangsmethode erhalten.
- [x] Restriktionsvorlage und Auswerter implementiert.
- [x] Priorisierte dynamische Ergänzungseingänge mit stabilen IDs implementiert.
- [x] Inspector-Diagnose implementiert.
- [x] Kern-, Knoten- und JSON-Persistenztests ergänzt.
- [x] Abschlussdiff auf Modulgrenzen und unbeabsichtigte Änderungen geprüft.
- [-] Vollständige CI-Verifikation des aktuellen Branch-Heads.
- [ ] Releaseintegration.

## 10. Konkrete Umsetzungsschritte

1. Optionalen effektiven Gesamtwertebereich in `Methode` und `MethodenSignatur` ergänzen.
2. Reine Restriktion und schrittweise Ergänzung im Rechenkern modellieren.
3. Strukturierte Herkunft von Basis, M und Ergänzungszweigen an der Ausgangsmethode erhalten.
4. `mathematik.restriktion` mit Methode-/Menge-Eingang und Methodenausgang registrieren.
5. Restmengenabhängig dynamische Ergänzungseingänge ableiten und freie/verbundene IDs stabil halten.
6. Synchronisierung nach erster Auswertung in `AtlasZustand.werteAus()` durchführen und bei Graphänderung erneut auswerten.
7. W/M/Z, Abdeckung, Rest und Ergänzungsbereiche im Inspector darstellen.
8. Tests für Mathematik, strukturierte Herkunft, Anschlussidentität und Persistenz ausführen.

## 11. Tests und Validierung

Abgedeckte Fälle:

- echte Restriktion `M ⊆ W`,
- schrittweise vollständige Erweiterung,
- überlappende Ergänzungsbereiche mit stabiler Priorität,
- strukturierte Erhaltung von Basis, `M`, Ergänzungsmethoden und effektiven Bereichen,
- unvollständige Abdeckung mit Restmenge,
- Ergänzung außerhalb der Zielmenge,
- größere deklarierte Ergänzungszielmenge bei dennoch gültigem effektivem Bild,
- leere gewünschte Menge,
- mehrstellige Methode mit nichtkartesischem Gesamtwertebereich,
- genau ein dynamischer nächster Ergänzungseingang,
- kein Ergänzungseingang bei bewiesen vollständiger Abdeckung,
- stabile ID des freien Folgeanschlusses bei reiner Neuauswertung,
- Erhalt verbundener Anschluss-IDs,
- JSON-Roundtrip dynamischer Anschlüsse.

CI-Ziel: `python3 scripts/pruefe_architektur.py` sowie `./gradlew --stacktrace test :app:assembleDebug`.

Ein bereits abgeschlossener Vorläufer-Head nach der Smart-Cast-Korrektur bestand diese vollständige Android-Build-Kette. Maßgeblich für die Abnahme bleibt der aktuelle End-Head inklusive strukturierter Bereichsanpassung.

Der separate Workflow `Mathematikkern prüfen` ist derzeit kein fachliches Prüfsignal: Er bricht vor den Repositorybefehlen in `gradle/actions/setup-gradle@v4` an der Validierung des unveränderten vorhandenen `gradle/wrapper/gradle-wrapper.jar` ab.

## 12. Persistenz und Migration

Der Restriktionsknoten verwendet bestehende `KnotenDaten` und `AnschlussDaten`. Dynamisch verbundene Ergänzungseingänge werden normal im Karten-JSON gespeichert. `effektiverWerteVorrat` und `bereichsanpassung` sind Laufzeitdomäne mathematischer Methoden und erfordern keine neue Karten-JSON-Version. Bestehende Methoden besitzen standardmäßig `null` und behalten damit das bisherige Verhalten.

## 13. Risiken und Rückfallstrategie

Risiken:

- symbolische Mengenbeziehungen können unentscheidbar bleiben,
- dynamische Anschlussableitung darf verbundene oder sichtbare freie IDs nicht unnötig verlieren,
- neue Methodendomäne muss durch Kopieren/Substitution propagiert werden.

Rückfall: Der neue Knotentyp und die optionalen Methodeneigenschaften sind additiv. Bei blockierenden Findings kann der Restriktionsumfang entfernt werden, ohne bestehende Methodenkarten umzuschreiben.

## 14. Fortschritt

2026-08-07: Bestand und #334 geprüft; Implementierung auf bestehendem v2.27.0-Branch begonnen.

2026-08-07: `effektiverWerteVorrat`, Restriktionskern, Knoten, Synchronisierung, Inspector sowie Tests umgesetzt.

2026-08-07: Ein früher Zwischenbuild fand einen Kotlin-Smart-Cast im Restriktionsauswerter; der Wert wird seitdem lokal gebunden. Ein späterer Vorläufer-Head bestand Architekturprüfung, JVM-Tests und `:app:assembleDebug` vollständig.

2026-08-07: Unabhängiger Diff-Abgleich ergänzte zwei Vertragsdetails vor der Endabnahme: stabile ID des freien Folgeanschlusses sowie strukturierte `MethodenBereichsanpassung` an der ausgegebenen Methode.

## 15. Entscheidungsprotokoll

### 2026-08-07: Gemeinsamer effektiver Wertevorrat

Entscheidung: `Methode` erhält ein optionales Feld für den Gesamtdefinitionsbereich.

Alternative: `M` auf einzelne Parameterbereiche projizieren.

Begründung: Eine Diagonale oder andere gekoppelte Teilmenge von `R²` ist kein kartesisches Produkt einzelner Parameterbereiche. Die Alternative wäre mathematisch falsch.

Konsequenz: Bestehende Methoden bleiben über den `null`-Default kompatibel; Restriktionen können ihren tatsächlichen Bereich exakt tragen.

### 2026-08-07: Ergänzungen als priorisierte Zweige

Entscheidung: Spätere Methoden wirken nur auf dem noch offenen Rest.

Alternative: Vereinigte Fallunterscheidung mit potenziell überschneidenden Zweigen.

Begründung: Der Nutzervertrag verlangt, dass frühere Definitionen nicht überschrieben werden.

### 2026-08-07: Strukturierte Bereichsherkunft statt reiner Fallausdruck

Entscheidung: Die Ausgangsmethode erhält `MethodenBereichsanpassung` zusätzlich zur ausführbaren Fallvorschrift.

Alternative: Nur die entfaltete `FallAusdruck`-Vorschrift und den Namen `f\vert_M` behalten.

Begründung: #334 verlangt, dass Basis, M, Reihenfolge und effektive Ergänzungsbereiche rekonstruierbar bleiben. Darstellung darf Semantik nicht ersetzen.

### 2026-08-07: Freier Folgeanschluss behält seine ID

Entscheidung: Ein bereits sichtbarer unverbundener Ergänzungseingang wird wiederverwendet.

Alternative: Bei jeder Auswertung einen neuen Anschluss erzeugen.

Begründung: Eine reine Neuauswertung darf den Graphzustand nicht ohne Nutzeraktion verändern.

## 16. Abweichungen vom ursprünglichen Plan

Keine fachliche Abweichung. Zwei notwendige Konkretisierungen entstanden beim Bestands- und Diff-Abgleich:

- `Methode.effektiverWerteVorrat` ist nötig, weil der vorhandene Parameter-Wertevorratsvertrag den allgemeinen mehrstelligen Fall nicht korrekt ausdrücken kann.
- `Methode.bereichsanpassung` ist nötig, weil eine bloß entfaltete Fallvorschrift die in #334 verlangte strukturierte Herkunft nicht vollständig repräsentiert.

## 17. Ergebnis und Verifikation

Produktcode, Tests, Inspector, dynamische Anschlusslogik und strukturierte Methodensemantik sind implementiert. Die fachliche Diff-Prüfung ist abgeschlossen. Der Plan bleibt bis zum grünen vollständigen Android-Build des aktuellen End-Heads und zur späteren Releaseintegration unter `plans/active`.
