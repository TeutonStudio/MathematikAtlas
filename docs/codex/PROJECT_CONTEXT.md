# Projektkontext: Mathematik Atlas

## Produktvision

Der Mathematik Atlas ist eine Android-Anwendung zur Darstellung mathematischer Vorgänge als gerichtete Knotenkarten. Nutzer können mathematische Objekte und Verarbeitungsschritte als Knoten verbinden, Ergebnisse im Graph auswerten und Karten als versionierte, wiederverwendbare Gruppen speichern.

## Gemeinsame Sprache

| Projektbegriff | Technischer Begriff | Bedeutung |
|---|---|---|
| Knoten | `KnotenDaten` | persistierbare Karte eines Graphen |
| Anschluss | `AnschlussDaten` | definierter Ein-, Aus- oder Neutralanschluss eines Knotens |
| Verbindung | `VerbindungDaten` | Verbindung zwischen zwei `AnschlussVerweis`-Instanzen |
| Karte | `KartenDaten` | versionierter gerichteter Graph mit Ansichtsdaten |
| Inspector | Eigenschaftenbereich | Compose-Bereich in `MathematikAtlasApp`, der Knotenparameter und Anschlüsse über `KartenAktion` verändert |
| mathematischer Ausdruck | `MathematischesObjekt` / `Ausdruck` | fachliche Repräsentation mit `zuLatex()`, nicht nur ein Formelstring |
| Darstellung | Compose-Renderer | visuelle Repräsentation durch `KnotenRenderer` bzw. `MathematikKnotenRenderer` |

## Technischer Rahmen

### Bestätigte Fakten

- Build-System: Gradle 8.13 Wrapper mit Kotlin-DSL.
- Sprache: Kotlin 2.3.21; JVM-Toolchain 17 in allen Modulen.
- Android: Android Gradle Plugin 8.13.2, `compileSdk` und `targetSdk` 36, `minSdk` 26 im App-Modul.
- UI: Jetpack Compose und Material 3; Compose-BOM `2026.06.00`.
- Module: `app`, `KnotenKartenVerwalter`, `MathematikRechenSystem`, `MathematikKartenAdapter` und `MathematikKnoten`.
- Abhängigkeitsrichtung: Das App-Modul verwendet alle Bibliotheksmodule. `MathematikKartenAdapter` verwendet den neutralen Karteneditor und das Rechensystem. `MathematikKnoten` verwendet Adapter, Karteneditor und Rechensystem. Das Rechensystem bleibt Android-/Compose-frei.
- Formeldarstellung: Der Rechenkern erzeugt LaTeX-Text; `MathematikKnoten/LatexText.kt` rendert einen unterstützten Teilumfang nativ in Compose.
- Paketmanager: Gradle Wrapper; es gibt keine Lockdatei oder `package.json` eines JavaScript-Paketmanagers.

### Nicht aus diesen Fakten ableiten

- Keine Vite-, React-, React-Flow-, shadcn/ui- oder KaTeX-Architektur annehmen.
- Keine erfolgreiche lokale Android-Kompilation oder Laufzeit annehmen: Die vorhandene Diagnoseumgebung konnte Gradle nicht starten.
- Keine allgemeine JSON-Schemamigration annehmen: `formatVersion` 1 wird geschrieben, aber beim Lesen nicht ausgewertet.

## Produktprinzipien

Die folgenden Prinzipien sind durch die vorhandenen Verträge und weite Teile des Codes gestützt; sie bleiben dennoch Zielvorgaben, wenn ein konkreter Pfad nicht getestet ist.

1. **Mathematik ist strukturiert.** Der Kern modelliert mathematische Objekte als Kotlin-Typen statt nur als Strings.
2. **Darstellung leitet sich aus dem Modell ab.** Die Compose-Darstellung erhält Auswertungsergebnisse; LaTeX stammt aus `MathematischesObjekt.zuLatex()`.
3. **Verbindungen sind typisiert.** `GraphPrüfung` nutzt Richtungen und eine Hierarchie von `AnschlussArt`.
4. **Fehler werden modelliert.** Auswertungsergebnisse besitzen ein Fehlerfeld; der Adapter sammelt Fehler pro Karte.
5. **Bearbeitung ist nachvollziehbar.** Der Editor verwendet unveränderliche Kartendaten, Aktionen und eine begrenzte Undo/Redo-Historie.
6. **Karten sind persistierbar.** Karten werden als JSON mit stabilen IDs und Versionen im App-Dateibereich gespeichert.

## Tatsächliche Quellverzeichnisse

| Bereich | Bestätigter Ort | Zentrale Symbole oder Aufgabe |
|---|---|---|
| Anwendungseinstieg | `app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/` | `MainActivity`, `MathematikAtlasApp`, `AtlasZustand` |
| Node-Komponenten | `KnotenKartenVerwalter/.../schnittstelle/` und `MathematikKnoten/.../` | `KnotenKartenEditor`, `KnotenRenderer`, `MathematikKnotenRenderer` |
| Node-Datentypen | `KnotenKartenVerwalter/.../daten/` | `KnotenDaten`, `KnotenVorlage`, `KartenDaten` |
| Handle- und Edge-Typen | `KnotenKartenVerwalter/.../daten/` und `.../logik/` | `AnschlussDaten`, `VerbindungDaten`, `GraphPrüfung`, `AnschlussArtRegister` |
| Inspector | `app/.../MathematikAtlasApp.kt` | Eigenschaftenbereich für Auswahl, Parameter, Anschlüsse und Kartenverweis |
| mathematisches Domänenmodell | `MathematikRechenSystem/src/main/kotlin/de/TeutonStudio/MathematikRechenSystem/kern/` | Zahlen, Mengen, Aussagen, Funktionen, Operatoren, lineare Algebra und Umformungen |
| Auswertung/Adapter | `MathematikKartenAdapter/src/main/kotlin/de/TeutonStudio/MathematikKartenAdapter/` | `KartenAuswerter`, Ergebnis- und Registertypen |
| mathematische Knotenvorlagen | `MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/` | Vorlagen, Anschlussarten, Standardauswerter und Renderer |
| Persistenz | `app/.../speicher/` | `KartenJson`, `KartenSpeicher` |
| Tests | `*/src/test/kotlin/` der vier Bibliotheksmodule | Kotlin-/JUnit-Tests für Kern, Graph, Adapter und mathematische Knoten |

## Tatsächliche zentrale Symbole

| Aufgabe | Bestätigte Symbole | Einordnung |
|---|---|---|
| Node-Vorlagenkatalog | `MathematikKnotenVorlagen.alle`, `KnotenVorlage.erzeuge` | Statische mathematische Vorlagen; `AtlasZustand` ergänzt gespeicherte Karten als dynamische Gruppen- und Methodenvorlagen. |
| Auswerter-Registry | `MathematikAuswerterRegister`, `StandardMathematikAuswerter.erzeugeRegister` | Ordnet stabilen Node-Art-Schlüsseln konkrete mathematische Auswerter zu. |
| Graphzustand | `KartenEditorZustand`, `KartenDaten`, `AtlasZustand` | Editorzustand mit Auswahl, Verbindungsvorschau und Undo/Redo; Appzustand koordiniert Persistenz und Auswertung. |
| Serialisierungsschema | `KartenJson`, `KartenSpeicher`, `formatVersion` 1 | JSON für Karte, Knoten, Anschlüsse, Verbindungen, Ansicht und Kartenverweise; dateibasierte Versionierung. |
| Ausdruckstyp | `MathematischesObjekt`, `Ausdruck`, `ZahlAusdruck`, `MengenAusdruck` | Fachliche Datenmodelle des reinen Kotlin-Rechenkerns. |
| Validierungsmechanismus | `GraphPrüfung`, `AnschlussArtRegister` | Prüft Richtung, hierarchische Typkompatibilität, Eingangskardinalität und Zyklen. |
| Auswertung | `KartenAuswerter`, `MathematikKnotenAuswerter`, `KnotenAuswertungsErgebnis` | Topologische Auswertung mit cachebaren Knotenergebnissen und Fehleraggregation. |

## Bestätigte Abweichungen von der Codex-Zielarchitektur

- Die älteren, technologieoffenen Hinweise auf React Flow, shadcn/ui und KaTeX sind für diesen Bestand nicht zutreffend; die Implementierung ist native Android/Compose.
- Es existieren getrennte Katalog- und Auswerterregister statt eines einzigen Registers, das zugleich Vorlage, Renderer, Inspector und Auswerter enthält.
- Es gibt Persistenz und eine begrenzte Knoten-Datenmigration, aber keine festgestellte allgemeine schema versionsabhängige Migrationspipeline.
- Es gibt Undo/Redo im `KartenEditorZustand`; Persistenz-Roundtrips und Migrationen sind derzeit nicht durch gefundene Unit-Tests abgesichert.

## Arbeitsannahmen für spätere Änderungen

Diese Annahmen sind keine bestätigten Fakten und müssen vor einer Änderung am konkreten Pfad überprüft werden:

- Ob ein neuer Knotentyp eine Migration bestehender gespeicherter Karten benötigt.
- Ob ein Fachgebiet bereits durch eine vorhandene Vorlage und einen registrierten Auswerter abgedeckt ist.
- Ob die App auf einem Android-Gerät aktuell baut, startet und alle Compose-Interaktionen wie vorgesehen ausführt.
- Ob die vorhandene Teilmenge des nativen LaTeX-Renderers jede neue Formelnotation korrekt darstellt.
