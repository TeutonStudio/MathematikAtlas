# Aktueller verifizierter Projektzustand

> Stand dieser Datei: Befunde aus Quellcode, Gradle-Konfiguration und den unten genannten Diagnosebefehlen. Nicht ausgeführte Builds oder Tests werden ausdrücklich nicht als erfolgreich behandelt.

## Metadaten

- Zuletzt verifiziert: 2026-07-27
- Verifiziert durch: Codex; Quellcodeprüfung sowie Gradle-Tests und Debug-Build mit dem vorhandenen JDK 17
- Commit vor der Dokumentationsänderung: `718bab7cc1a4916acf9c91a28f95f46aac44f397` (`v2.0.12`)
- Arbeitsbaum vor der Dokumentationsänderung: sauber
- Verifikationsgrenze: Android-App wurde nicht auf einem Emulator oder Gerät gestartet. JVM-Tests und der Debug-Build wurden lokal ausgeführt.

## Start und Prüfung

| Zweck | Verifizierter Befehl oder Einstieg | Ergebnis oder Hinweis |
|---|---|---|
| Abhängigkeiten auflösen | Gradle Wrapper (`gradle/wrapper/gradle-wrapper.properties`, Gradle 8.13) | Kein separater Paketinstallationsbefehl. Gradle löst Abhängigkeiten beim ersten passenden Task auf. Nicht ausgeführt. |
| Anwendung starten | `MainActivity` setzt `MathematikAtlasApp` als Compose-Inhalt | Start auf Emulator/Gerät nicht verifiziert. Android Studio kann das Gradle-Projekt öffnen. |
| Architektur- und Strukturprüfung | `python3 scripts/pruefe_repository.py` | Erfolgreich: XML, Wrapper und Architekturprüfung bestanden. |
| Zusätzliche Kernprüfung | `python3 scripts/pruefe_kern.py` | Nicht ausführbar: `kotlinc` fehlt in der lokalen Umgebung. |
| JVM-Tests | `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew test` | Erfolgreich am 2026-07-27; Kern-, Graph-, Adapter-, Knoten- und App-Persistenztests bestanden. |
| Produktions-Build | `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew :app:assembleDebug` | Erfolgreich am 2026-07-27. |
| Lint | Projektweite Konfiguration durchsucht | Keine dedizierte ktlint-, detekt- oder Android-Lint-Task-Konfiguration in den Buildskripten gefunden. |

## Repository-Struktur

- Build: Android-Gradle-Multimodulprojekt mit Kotlin-DSL; Root in `settings.gradle.kts`.
- Module: `app`, `KnotenKartenVerwalter`, `MathematikRechenSystem`, `MathematikKartenAdapter`, `MathematikKnoten`.
- Anwendungseinstieg: `app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/MainActivity.kt`; die Compose-Wurzel ist `MathematikAtlasApp`.
- Graph-/Canvas-Modul: `KnotenKartenVerwalter`; Compose-Editor in `schnittstelle/KnotenKartenEditor.kt` und Editorzustand in `zustand/KartenEditorZustand.kt`.
- Nodes und Handles: persistierbare Modelle `KnotenDaten`, `AnschlussDaten` und `VerbindungDaten` in `KnotenKartenVerwalter/.../daten/`; mathematische Vorlagen in `MathematikKnoten/.../MathematikKnotenVorlagen.kt`.
- Edges und Validierung: `GraphPrüfung` im neutralen Kartenmodul; sie prüft Richtung, Typ-Hierarchie, belegte Eingänge und Zyklen.
- Inspector: Eigenschaftenbearbeitung ist als Compose-Bereich in `app/.../MathematikAtlasApp.kt` umgesetzt und schreibt über `KartenAktion` in `KartenEditorZustand`.
- mathematisches Modell: `MathematikRechenSystem/.../kern/`; Wurzeltyp ist `MathematischesObjekt` mit `Ausdruck`, `ZahlAusdruck` und `MengenAusdruck`.
- Auswertung: `MathematikKartenAdapter`; topologischer `KartenAuswerter` mit Cache und `MathematikAuswerterRegister`.
- Persistenz: `app/.../speicher/KartenJson.kt` und `KartenSpeicher.kt`; Format 2 liest Format-1-Karten mit leerer Eigenschaftsmap rückwärtskompatibel und speichert rekursiv typisierte Eigenschaften. Speicherort ist der App-interne Dateienbereich `MathematikAtlas/karten/<karten-id>/v<version>.json`.
- Tests: JVM-Unit-Tests in den vier Bibliotheksmodulen unter `src/test/kotlin`; im Repository wurden keine Testquellen für das App-Modul oder Android-Instrumentierungstests gefunden.

## Vorhandene Node-Typen

`MathematikKnotenVorlagen.alle` ist die statische Katalogquelle. Sie enthält Vorlagen für Zahlen und Terme, Aussagen und Prädikate, Mengen, iterative Operatoren, Abbildungen, Vektoren, Matrizen sowie Karten-Ein-/Ausgänge und Fallunterscheidungen. Die Vorlagen erzeugen pro Instanz neue Anschluss-IDs; ihre fachlichen Typen stammen aus `MathematikAnschlussArten`.

| Node-Familie | Fachlicher Zweck | Eingänge / Ausgänge | Registry- oder Typ-Schlüssel | zentrale Dateien |
|---|---|---|---|---|
| Rechnen, Algebra und Analysis | Zahl, Variable, Addition, Multiplikation, Division, Potenz, Gleichung lösen, Auswerten, Ableiten, Integrieren, Wurzel, Logarithmus | überwiegend Zahl oder allgemeines Objekt | `mathematik.*` | `MathematikKnotenVorlagen.kt`, `MathematikAuswerter.kt` |
| Zahlen, Mengen und Aussagen | Tupel, komplexe Zahlen, Mengenoperationen, Zahlbereiche, Vergleiche, Mengenprädikate und Aussagenlogik | Zahl, Menge, Objekt oder Aussage | `mathematik.*` | `MathematikKnotenVorlagen.kt`, `MathematikAuswerter.kt` |
| Operatoren und Abbildungen | Iterierte Summe/Produkt/Mengenoperationen, Abbild, Term-zu-Methode, Komposition, Iteration und Analysis von Methoden | typisierte Funktions-, Mengen- und Zahlanschlüsse | `mathematik.*` | `MathematikKnotenVorlagen.kt`, `MathematikAuswerter.kt` |
| Vektoren und Matrizen | orientierte Zeilen-/Spaltenvektoren, Matrixbildung, Produkte, Transposition und Inversion | Zahl, Vektor oder Matrix | `mathematik.*` | `MathematikKnotenVorlagen.kt`, `MathematikAuswerter.kt` |
| Wiederverwendbare Karten | öffentliche Karten-Ein-/Ausgänge, dynamisch erzeugte Gruppenknoten und Methodenkarten | aus der referenzierten Karte abgeleitet | statisch `mathematik.kartenEingang` / `mathematik.kartenAusgang`; dynamisch `gruppe.<karten-id>` und `methode.<karten-id>` | `MathematikKnotenVorlagen.kt`, `AtlasZustand.kt`, `KartenAuswerter.kt` |

## Zentrale Architekturpfade

- Node-Erzeugung: `KnotenVorlage.erzeuge` erzeugt `KnotenDaten`; `AtlasZustand.fügeKnotenEin` fügt sie über `KartenAktion.KnotenEinfügen` in den Editorzustand ein.
- Vorlagenkatalog: `MathematikKnotenVorlagen.alle`; `AtlasZustand` ergänzt daraus abgeleitete Gruppen- und Methodenvorlagen. Es gibt damit keinen einzelnen, universellen Registry-Typ für Darstellung, Vorlagen und Auswertung.
- Auswerter-Registry: `MathematikAuswerterRegister`, befüllt von `StandardMathematikAuswerter.erzeugeRegister` anhand stabiler `mathematik.*`-Schlüssel.
- Graphzustand: `KartenEditorZustand.karte` hält eine immutable `KartenDaten`-Instanz; Undo/Redo-Historien liegen im Editorzustand. `AtlasZustand` koordiniert Auswahl, Auswertung, Kartenliste und Speicherung.
- Handle-Vertrag: `AnschlussDaten` enthält stabile Instanz-ID, Richtung, Kante, `AnschlussArtId`, Reihenfolge sowie Kennzeichen für dynamische Eingänge.
- Verbindungsvalidierung: `GraphPrüfung.prüfe`; Typkompatibilität wird über die Elternhierarchie von `AnschlussArtRegister.istUnterart` bestimmt.
- Ausdrucksauswertung: `KartenAuswerter.auswerten` verarbeitet den Graph topologisch, sammelt Eingänge über Anschlüsse und ruft registrierte `MathematikKnotenAuswerter` auf.
- Formeldarstellung: jedes `MathematischesObjekt` liefert `zuLatex()`; `LatexText` rendert einen unterstützten Teilumfang nativ als Compose-Text. Es gibt keine gefundene KaTeX- oder WebView-Abhängigkeit.
- Serialisierung und Laden: `KartenJson` schreibt `formatVersion` 2 und alle Karten-, Knoten-, Anschluss-, Verbindungs- und rekursiven Eigenschaftsdaten; fehlende Eigenschaften aus Format 1 werden als leer gelesen.

## Bestätigte Einschränkungen

- Das Projekt verwendet Kotlin, Jetpack Compose und Gradle, nicht Vite, React, React Flow, shadcn/ui oder KaTeX.
- `MathematikRechenSystem` ist ein Kotlin/JVM-Modul ohne Android- oder Compose-Abhängigkeit; die Architekturprüfung bestätigt zudem, dass der neutrale Karteneditor und der Adapter keine verbotenen Modulimporte enthalten.
- Verbindungen sind azyklisch und für explizite Eingänge auf genau eine eingehende Verbindung beschränkt. Neutrale Anschlüsse sind vom allgemeinen Modell unterstützt.
- Persistenzdaten sind eigene Datenklassen und JSON-Werte; Compose-Laufzeitobjekte werden nicht serialisiert.
- `KartenJson` akzeptiert fehlende Eigenschaften aus Format 1 und schreibt Format 2. Die vorhandene UI-Migration in `AtlasZustand.aktualisiereAssoziativeKnoten` ergänzt zusätzlich bekannte Anschlüsse und normalisiert assoziative Knoten beim Öffnen.
- Der Kartenladepfad `KartenSpeicher.lade` ruft `KartenJson.lese` direkt auf; in diesem Pfad wurde keine nachträgliche `GraphPrüfung` gefunden.

## Bekannte Blocker und technische Schulden

| Befund | Evidenz | Auswirkung | betroffene Dateien | Blockiert diese Dokumentationsaufgabe? |
|---|---|---|---|---|
| Zusätzliche Kernprüfung lokal nicht möglich | `scripts/pruefe_kern.py` beendet sich mit Code 2, weil `kotlinc` fehlt | Die eigenständige Compiler-/Kernprüfung ist nicht bestätigt | lokale Laufzeitumgebung | Nein |

## Zuletzt abgeschlossene größere Änderungen

| Datum | Änderung | ExecPlan oder ADR | Prüfstatus |
|---|---|---|---|
| 2026-07-27 | Istzustand und Projektkontext erstmals gegen den vorhandenen Android-/Kotlin-Code abgeglichen | keiner | Repository- und Architekturprüfung erfolgreich; vollständige Gradle-/Kernprüfung lokal nicht ausführbar |
