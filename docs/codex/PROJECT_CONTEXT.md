# Projektkontext: Mathematik Atlas

## Produktvision

Der Mathematik Atlas ist eine native Android-Anwendung zur Darstellung mathematischer Vorgänge als gerichtete Knotenkarten. Nutzer verbinden mathematische Objekte und Verarbeitungsschritte, lassen den azyklischen Graph topologisch auswerten und speichern Karten als versionierte, wiederverwendbare Gruppen.

## Gemeinsame Sprache

| Projektbegriff | Technischer Begriff | Bedeutung |
|---|---|---|
| Knoten | `KnotenDaten` | persistierbare Instanz innerhalb einer Karte |
| Anschluss | `AnschlussDaten` | typisierter Ein-, Aus- oder Neutralanschluss eines Knotens |
| Verbindung | `VerbindungDaten` | Verbindung zwischen zwei `AnschlussVerweis`-Instanzen |
| Karte | `KartenDaten` | versionierter gerichteter Graph mit Ansicht und visuellen Gruppen |
| Inspector | Eigenschaftenbereich | Compose-Oberfläche, die Knotendaten über Kartenaktionen verändert |
| mathematischer Ausdruck | `MathematischesObjekt` / `Ausdruck` | fachliche Repräsentation mit `zuLatex()`, nicht nur ein Formelstring |
| Darstellung | Compose-Renderer | visuelle Repräsentation über `KnotenRenderer` und spezialisierte Renderer |
| Gruppenknoten | `KartenVerweis` | feste Referenz auf eine bestimmte Version einer anderen Karte |

## Technischer Rahmen

### Bestätigte Fakten

- Build-System: Gradle Wrapper mit Kotlin-DSL.
- Sprache: Kotlin; JVM-Toolchain 17 in den Modulen.
- Plattform: Android mit Jetpack Compose und Material 3.
- Module: `app`, `KnotenKartenVerwalter`, `MathematikRechenSystem`, `MathematikKartenAdapter` und `MathematikKnoten`.
- `MathematikRechenSystem` bleibt Android- und Compose-frei.
- `KnotenKartenVerwalter` ist der fachneutrale Compose-Karteneditor.
- `MathematikKartenAdapter` führt Kartengraphen mit dem Rechenkern aus.
- `MathematikKnoten` enthält mathematische Vorlagen, Auswerter und spezialisierte Renderer.
- Der Rechenkern erzeugt LaTeX-Text; `MathematikKnoten/LatexText.kt` rendert einen unterstützten Teilumfang nativ in Compose.
- Die App speichert Karten über `KartenJson`; der aktuelle Schreibpfad verwendet `formatVersion` 5.
- Es existiert kein JavaScript-Paketmanager, keine `package.json` und keine Webanwendung.

### Nicht aus diesen Fakten ableiten

- Keine Vite-, React-, React-Flow-, shadcn/ui- oder KaTeX-Architektur annehmen.
- Einen dokumentierten oder früher erfolgreichen Gradle-Befehl nicht automatisch als im aktuellen Arbeitsstand ausgeführt melden.
- Einen erfolgreichen JVM-Test oder APK-Build nicht als Emulator- oder Geräteprüfung ausgeben.
- Nicht annehmen, dass jede alte Karte ohne Migration dieselben Anschlüsse wie eine aktuelle Vorlage besitzt.
- Nicht annehmen, dass `CURRENT_STATE.md` neuere Commits abdeckt als sein ausdrücklich genannter Stand.

## Produktprinzipien

1. **Mathematik ist strukturiert.** Der Kern modelliert mathematische Objekte als Kotlin-Typen statt nur als Strings.
2. **Darstellung leitet sich aus dem Modell ab.** Compose-Renderer erhalten Daten oder Auswertungsergebnisse; LaTeX stammt aus fachlichen Objekten.
3. **Verbindungen sind typisiert.** `GraphPrüfung` nutzt Richtung, Anschlussarthierarchie, Eingangskardinalität und Zyklusprüfung.
4. **Fehler werden modelliert.** Auswertungsergebnisse besitzen fachliche Fehler- und Entscheidungszustände.
5. **Bearbeitung ist nachvollziehbar.** Der Editor verwendet unveränderliche Kartendaten, Kartenaktionen und Undo/Redo.
6. **Karten sind persistierbar.** Karten besitzen stabile IDs, eigene Versionen und ein versioniertes JSON-Format.
7. **Module bleiben fachlich getrennt.** Der neutrale Editor kennt keine mathematischen Parameternamen; der Rechenkern kennt keine UI.
8. **Laufzeitzustand wird nicht persistiert.** Compose-Zustand, Renderer-Caches und berechnete Ergebnisse werden neu abgeleitet.

## Tatsächliche Quellverzeichnisse

| Bereich | Bestätigter Ort | Zentrale Symbole oder Aufgabe |
|---|---|---|
| Anwendungseinstieg | `app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/` | `MainActivity`, `MathematikAtlasApp`, `AtlasZustand` |
| Karteneditor | `KnotenKartenVerwalter/.../schnittstelle/` | `KnotenKartenEditor`, `KnotenRenderer` |
| Graphdaten | `KnotenKartenVerwalter/.../daten/` | `KartenDaten`, `KnotenDaten`, `AnschlussDaten`, `VerbindungDaten` |
| Graphlogik | `KnotenKartenVerwalter/.../logik/` und `.../zustand/` | `GraphPrüfung`, `KartenAktion`, `KartenEditorZustand` |
| Inspector und App-Dialoge | `app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/` | Eigenschaftenbearbeitung und App-Koordination |
| mathematische Domäne | `MathematikRechenSystem/src/main/kotlin/de/TeutonStudio/MathematikRechenSystem/kern/` | Zahlen, Mengen, Aussagen, Funktionen, Operatoren, Algebra, Geometrie und Umformungen |
| Auswertung | `MathematikKartenAdapter/src/main/kotlin/de/TeutonStudio/MathematikKartenAdapter/` | `KartenAuswerter`, Ergebnis- und Registertypen |
| mathematische Knoten | `MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/` | Vorlagen, Anschlussarten, Standardauswerter und Renderer |
| Persistenz | `app/.../speicher/` | `KartenJson`, `KartenSpeicher`, Kartenordnung |
| Migration | `app/.../AtlasMigrationen.kt` und Ladepfade | Normalisierung älterer Karten- und Anschlussdaten |
| Tests | `*/src/test/kotlin/` | Kotlin-/JUnit-Tests für Kern, Graph, Adapter, Knoten und App-Persistenz |

## Zentrale Symbole

| Aufgabe | Symbole | Einordnung |
|---|---|---|
| Vorlagenkatalog | `MathematikKnotenVorlagen.alle`, `KnotenVorlage.erzeuge` | statische mathematische Vorlagen; App ergänzt Gruppenvorlagen |
| Auswerterregister | `MathematikAuswerterRegister`, `StandardMathematikAuswerter.erzeugeRegister` | ordnet stabilen Knotenarten Auswerter zu |
| Graphzustand | `KartenEditorZustand`, `KartenDaten`, `AtlasZustand` | Editorzustand und App-Koordination |
| Anschlussarten | `AnschlussArtRegister`, `MathematikAnschlussArten` | hierarchische Typkompatibilität |
| Serialisierung | `KartenJson`, `KartenSpeicher` | JSON und dateibasierte Kartenversionierung |
| Ausdruckstyp | `MathematischesObjekt`, `Ausdruck`, `ZahlAusdruck`, `MengenAusdruck` | fachliche Datenmodelle des Rechenkerns |
| Auswertung | `KartenAuswerter`, `MathematikKnotenAuswerter`, `KnotenAuswertungsErgebnis` | topologische Auswertung, Cache und Fehleraggregation |
| Formelanzeige | `LatexText`, `MathematischesObjekt.zuLatex()` | nativer Compose-Teilrenderer, kein KaTeX |

## Arbeitsannahmen für Änderungen

Diese Punkte müssen am konkreten Pfad überprüft werden:

- ob ein neuer Knotentyp eine Migration bestehender Karten benötigt,
- ob ein Fachgebiet bereits durch vorhandene Vorlagen und Auswerter abgedeckt ist,
- ob eine Anschlussänderung bestehende Verbindungen und Gruppenknoten betrifft,
- ob der aktuelle Branch alle dokumentierten Prüfungen erfolgreich durchläuft,
- ob eine Compose-Interaktion auf Emulator oder Gerät verifiziert wurde,
- ob der native Formelrenderer jede benötigte Notation unterstützt,
- ob eine Änderung der Persistenz tatsächlich eine neue `formatVersion` benötigt.
