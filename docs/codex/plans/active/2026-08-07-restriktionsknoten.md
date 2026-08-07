# Restriktionsknoten mit priorisierter Bereichserweiterung

## 1. Status

Stand: Implementierung und Tests auf `samai/v2.27.0/notiz-knoten`; Releaseintegration noch offen.

Zugehörig: #334, PR #336. Der Knotentyp `mathematik.restriktion` ist Teil derselben neuen y-Version v2.27.0 wie `karte.notiz`.

## 2. Ziel und Nutzerwirkung

Der Atlas erhält einen Knoten, der eine Methode `f: W -> Z` auf eine gewünschte Menge `M` restringiert. Liegt `M` nicht vollständig im bisherigen Wertevorrat, fordert der Knoten nacheinander Ergänzungsmethoden an, bis jeder Punkt von `M` durch die Basismethode oder eine Ergänzung abgedeckt ist.

Die Ausgabe bleibt eine Methode mit unveränderter Zielmenge `Z` und effektivem Wertevorrat `M`.

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

Die resultierende Methode darf nur gebildet werden, wenn die Abdeckung nicht widerlegt ist und keine Ergänzung nachweislich außerhalb von `Z` abbildet. Symbolisch unentscheidbare Abdeckungs- oder Zielbedingungen werden als Aussagen weitergereicht, nicht geraten.

## 6. Daten-, Knoten-, Anschluss- und Edge-Vertrag

Knotenart: `mathematik.restriktion`.

Feste Anschlüsse:

- Eingang `methode`, Art `mathematik.methode`, Reihenfolge 0,
- Eingang `menge`, Art `mathematik.menge`, Reihenfolge 1,
- Ausgang `methode`, Art `mathematik.methode`.

Dynamische Ergänzungseingänge heißen `ergänzung.<n>` und sind Methodeneingänge. Ist die Abdeckung nicht bewiesen vollständig, existiert genau ein unverbundener nächster Ergänzungseingang. Verbundene Ergänzungseingänge bleiben mit ihrer ID erhalten.

## 7. Architekturentscheidungen

- Der fachliche Restriktionsalgorithmus liegt im Compose-freien `MathematikRechenSystem`.
- `Methode` erhält optional `effektiverWerteVorrat`, damit insbesondere nichtkartesische Restriktionen mehrstelliger Methoden korrekt darstellbar sind.
- Dynamische Anschlussableitung liegt in `MathematikKnoten`, nicht im fachneutralen Editor.
- App-Code koordiniert Synchronisierung und Inspector, enthält aber keine Restriktionsmathematik.
- Die existierende Methoden-Anschlussart wird wiederverwendet.

## 8. Betroffene Dateien und Symbole

- `MathematikRechenSystem/.../MethodenFundament.kt`: `MethodenSignatur.effektiverWerteVorrat`.
- `MathematikRechenSystem/.../Methoden.kt`: `Methode.effektiverWerteVorrat` und Propagation.
- `MathematikRechenSystem/.../MethodenRestriktion.kt`: Restriktionsmodell und Algorithmus.
- `MathematikKnoten/.../RestriktionsKnoten.kt`: Vorlage, Auswerter, dynamische Anschlüsse.
- `MathematikKnoten/.../geometrie/GesamterMathematikAuswerter.kt`: Registrierung.
- `MathematikKnoten/.../AussagenLogikKnoten.kt`: sichtbarer Vorlagenkatalog.
- `app/.../AtlasZustand.kt`: Anschluss-Synchronisierung.
- `app/.../RestriktionsKnotenInspektor.kt`: Diagnose.
- zugehörige Kern-, Knoten- und Persistenztests.

## 9. Meilensteine

- [x] Fachlichen Bereichsvertrag und effektiven Gesamtwertebereich implementiert.
- [x] Restriktionsvorlage und Auswerter implementiert.
- [x] Priorisierte dynamische Ergänzungseingänge implementiert.
- [x] Inspector-Diagnose implementiert.
- [x] Kern-, Knoten- und JSON-Persistenztests ergänzt.
- [-] Vollständige CI-Verifikation des aktuellen Branch-Heads.
- [ ] Unabhängige Verifikation und Releaseintegration.

## 10. Konkrete Umsetzungsschritte

1. Optionalen effektiven Gesamtwertebereich in `Methode` und `MethodenSignatur` ergänzen.
2. Reine Restriktion und schrittweise Ergänzung im Rechenkern modellieren.
3. `mathematik.restriktion` mit Methode-/Menge-Eingang und Methodenausgang registrieren.
4. Restmengenabhängig dynamische Ergänzungseingänge ableiten.
5. Synchronisierung nach erster Auswertung in `AtlasZustand.werteAus()` durchführen und bei Graphänderung erneut auswerten.
6. W/M/Z, Abdeckung, Rest und Ergänzungsbereiche im Inspector darstellen.
7. Tests für Mathematik, Anschlussidentität und Persistenz ausführen.

## 11. Tests und Validierung

Abgedeckte Fälle:

- echte Restriktion `M ⊆ W`,
- schrittweise vollständige Erweiterung,
- überlappende Ergänzungsbereiche mit stabiler Priorität,
- unvollständige Abdeckung mit Restmenge,
- Ergänzung außerhalb der Zielmenge,
- leere gewünschte Menge,
- mehrstellige Methode mit nichtkartesischem Gesamtwertebereich,
- genau ein dynamischer nächster Ergänzungseingang,
- kein Ergänzungseingang bei bewiesen vollständiger Abdeckung,
- Erhalt verbundener Anschluss-IDs,
- JSON-Roundtrip dynamischer Anschlüsse.

CI-Ziel: `python3 scripts/pruefe_architektur.py` sowie `./gradlew --stacktrace test :app:assembleDebug`.

## 12. Persistenz und Migration

Der Restriktionsknoten verwendet bestehende `KnotenDaten` und `AnschlussDaten`. Dynamisch verbundene Ergänzungseingänge werden normal im Karten-JSON gespeichert. Der neue optionale Methoden-Gesamtwertebereich ist Laufzeitdomäne und erfordert keine neue Karten-JSON-Version. Bestehende Methoden besitzen standardmäßig `null` und behalten damit das bisherige Verhalten.

## 13. Risiken und Rückfallstrategie

Risiken:

- symbolische Mengenbeziehungen können unentscheidbar bleiben,
- dynamische Anschlussableitung darf verbundene IDs nicht verlieren,
- neue Methodendomäne muss durch Kopieren/Substitution propagiert werden.

Rückfall: Der neue Knotentyp und die optionale Methodeneigenschaft sind additiv. Bei blockierenden Findings kann der Restriktionsumfang entfernt werden, ohne bestehende Methodenkarten umzuschreiben.

## 14. Fortschritt

2026-08-07: Bestand und #334 geprüft; Implementierung auf bestehendem v2.27.0-Branch begonnen.

2026-08-07: `effektiverWerteVorrat`, Restriktionskern, Knoten, Synchronisierung, Inspector sowie Tests umgesetzt.

2026-08-07: Ein früher Zwischenbuild bestand Architekturprüfung, fiel aber im Build-/Testschritt. Spätere Commits enthalten Korrekturen und zusätzliche Tests; maßgeblich ist der CI-Status des aktuellen Heads.

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

## 16. Abweichungen vom ursprünglichen Plan

Keine fachliche Abweichung. Die notwendige Erweiterung von `Methode` um einen effektiven Gesamtwertebereich wurde beim Bestandsabgleich konkretisiert, weil der vorhandene Parameter-Wertevorratsvertrag nicht ausreichte, um den in #334 geforderten allgemeinen mehrstelligen Fall korrekt auszudrücken.

## 17. Ergebnis und Verifikation

Produktcode und Tests sind implementiert. Die endgültige Abnahme dieses Plans erfolgt erst nach grünem vollständigem Android-Build des aktuellen Branch-Heads, unabhängiger Diff-Prüfung sowie synchronisierten Issues #294/#334. Der Plan bleibt deshalb bis dahin unter `plans/active`.
