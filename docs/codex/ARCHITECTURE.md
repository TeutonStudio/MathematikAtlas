# Architektur des Mathematik Atlas

## Zweck

Dieses Dokument beschreibt die beabsichtigten Verantwortungsgrenzen der nativen Android- und Desktop-Anwendung. Es behauptet nicht, dass der aktuelle Code sie bereits vollständig erfüllt. Abweichungen werden im ExecPlan und gegebenenfalls als technische Schuld dokumentiert.

## Module und Verantwortungen

### 1. `MathematikRechenSystem`

Verantwortlich für:

- mathematische Objekte und Ausdrücke,
- Operationen, Relationen, Mengen, Funktionen und Geometrie,
- Typen, Gültigkeitsbedingungen und fachliche Fehlerzustände,
- symbolische oder exakte Auswertung,
- strukturierte Umformungen,
- LaTeX-Ausgabe oder eine neutrale Darstellungsrepräsentation.

Nicht verantwortlich für:

- Android oder Jetpack Compose,
- Kartenpositionen und Größen,
- Auswahl- oder Gestenzustand,
- Inspector-Layout,
- Dateidialoge und App-Navigation.

Der Rechenkern bleibt ein reines Kotlin/JVM-Modul.

### Mengenwertige Iterationsmethoden

Eine einwertige mengenwertige Methode verwendet ihre deklarierte Zielmenge als feste Grundmenge ihrer Mengenausgaben. Das Modell liest `A : I -> G` damit als Familie von Teilmengen von `G`, auch wenn `A : I -> P(G)` mathematisch präziser wäre. Die Grundmenge wird ausschließlich aus der Zielmenge abgeleitet, nie separat gespeichert oder über einen weiteren Anschluss gesetzt.

Die Zielmenge darf nicht vom einzigen Iterationsparameter abhängen. Der Schnitt über eine leere Indexmenge ergibt diese feste Grundmenge. Die Rechenkernvalidierung prüft diese Invariante zentral; Kartenadapter und Renderer verwenden nur die abgesicherte abgeleitete Grundmenge.

### 2. `KnotenKartenVerwalter`

Verantwortlich für:

- fachneutrale persistierbare Graphdaten,
- Knoten-, Anschluss-, Verbindungs- und Gruppen-IDs,
- Positionen, Größen und Ansichtsdaten,
- Verbindungskompatibilität anhand registrierter Anschlussarten,
- Auswahl, Drag, Zoom und Kontextinteraktionen,
- Kartenaktionen und Undo/Redo,
- generische Renderer-Verträge,
- den fachneutralen `KartenDatenJson`-Codec ohne mathematische Migrationen.

Nicht verantwortlich für:

- mathematische Auswertung,
- mathematische Knotenschlüssel,
- knotenspezifische Parameternamen wie `festeEingänge`,
- mathematische Kartenmigrationen,
- LaTeX-Erzeugung,
- App-Persistenz und Kartenbibliothek.

Das Modul darf fachliche Regeln höherer Module über generische Verträge ermöglichen, aber nicht selbst als zweite mathematische Wahrheit implementieren.

### 3. `MathematikKartenAdapter`

Verantwortlich für:

- Zuordnung zwischen Kartengraph und mathematischer Domäne,
- topologische Auswertung azyklischer Karten,
- Einsammeln und Binden von Anschlusswerten,
- Auswerterregister,
- Ergebnis-Cache und Fehleraggregation,
- Auswertung wiederverwendbarer Karten als Gruppenknoten.

Der Adapter kennt keine App-Dialoge und besitzt keine Compose-Darstellung.

### 4. `MathematikKnoten`

Verantwortlich für:

- mathematische `KnotenVorlage`-Definitionen,
- mathematische Anschlussarten,
- den kanonischen plattformübergreifenden Erstellen-Katalog,
- Standardauswerter und deren geordnete Registrierungsphasen,
- die gemeinsame mathematische Kartenserialisierungsfassade `MathematikKartenCodec`,
- historische mathematische Karten- und Knotenmigrationen,
- spezialisierte Compose-Renderer,
- native Darstellung des vom Rechenkern erzeugten LaTeX-Teilumfangs,
- fachbezogene Konfigurationen wie Matrix- oder Visualisierungsparameter.

Renderer dürfen fachliche Ergebnisse darstellen und Interaktionen melden, aber keine unabhängige mathematische Semantik pflegen.

### 5. `app`

Verantwortlich für:

- Anwendungseinstieg und Material-3-Oberfläche,
- Kartenbibliothek, Ordner und Navigation,
- Inspector und anwendungsspezifische Dialoge,
- Ergänzung des kanonischen Mathematikkatalogs um App-Werkzeuge und dynamische Gruppenvorlagen,
- Android-Dateispeicherung, Backups, Papierkorb, Freigaben sowie Import-/Export-UI,
- Koordination von Editorzustand und Auswertung.

Die App darf keine zweite mathematische Katalog-, Auswerter- oder Migrationslogik pflegen. Ihre `KartenJson`-Fassade delegiert an `MathematikKartenCodec`.

### 6. `desktopApp`

Verantwortlich für:

- Desktop-Einstieg, Fenster und Menüs,
- Dateidialoge und XDG-nahe Speicherorte,
- Desktop-spezifische Eingabeverträge,
- Verwendung desselben kanonischen Mathematikkatalogs, Auswerterregisters und mathematischen Karten-Codecs wie Android.

## Abhängigkeitsrichtung

Die tatsächlichen Gradle-Abhängigkeiten bilden die erlaubte Richtung:

```text
app
├── MathematikKnoten
├── MathematikKartenAdapter
├── KnotenKartenVerwalter
└── MathematikRechenSystem

MathematikKnoten
├── MathematikKartenAdapter
├── KnotenKartenVerwalter
└── MathematikRechenSystem

MathematikKartenAdapter
├── KnotenKartenVerwalter
└── MathematikRechenSystem
```

Verboten sind insbesondere:

- Android-, Compose- oder Editorabhängigkeiten im `MathematikRechenSystem`,
- Mathematikabhängigkeiten im `KnotenKartenVerwalter`,
- App-Abhängigkeiten in Bibliotheksmodulen,
- zyklische Modulabhängigkeiten.

## Registrierungswege

Der aktuelle Bestand verwendet bewusst getrennte Mechanismen:

- `MathematikKnotenVorlagen.alle` als lade-kompatiblen historischen Basiskatalog,
- `KanonischerMathematikKnotenKatalog` als einzige fachliche Quelle für den sichtbaren mathematischen Erstellen-Katalog auf Android und Desktop,
- dynamisch aus Karten abgeleitete Gruppenvorlagen im App-Zustand,
- `MathematikAuswerterRegister` für mathematische Auswerter,
- `StandardMathematikAuswerterPakete` für die explizite Reihenfolge additiver Registrierungen und nachgelagerter Verfeinerungen,
- Renderer-Zuordnung über die vorhandenen App- und Knotenpfade,
- `AnschlussArtRegister` für die Typkompatibilität von Anschlüssen.

Historische Vorlagen dürfen zum Laden und Migrieren erhalten bleiben, ohne im sichtbaren Katalog angeboten zu werden. Plattformmodule dürfen keine eigene mathematische Ersetzungslogik neben `KanonischerMathematikKnotenKatalog` aufbauen.

## Ordner- und Paketregeln

Neue globale Orchestrierungsdateien werden nach Verantwortung einsortiert. Insbesondere liegen Katalog- und Registrierungsorchestrierung unter `MathematikKnoten/.../katalog/`, mathematische Kartenmigrationen unter `MathematikKnoten/.../migration/`, Rechenadapter unter `.../rechnen/` und Vektor-/Multinomcode unter `.../vektor/`.

Versionssuffixe wie `V2300` sind nur für historische Implementierungs- oder Migrationspfade zulässig. Neue Produktpfade verwenden versionsfreie Fassaden. Eine veröffentlichte Versionsnummer ist kein Fachgebiet und deshalb auch kein dauerhafter Architekturordner.

## Zustandsführung

Trenne:

- persistierte `KartenDaten`, `KnotenDaten`, `AnschlussDaten` und `VerbindungDaten`,
- abgeleitete mathematische Ergebnisse,
- Editorinteraktion und Auswahl,
- kurzlebigen Compose-Zustand,
- anwendungsspezifische Navigations- und Dialogzustände.

Doppelte, unabhängig veränderbare Kopien derselben Information sind zu vermeiden. Änderungen persistierter Kartendaten erfolgen über die vorgesehenen Kartenaktionen, damit Undo/Redo und Speicherung denselben Zustand sehen.

## Persistenz

Persistierte Daten enthalten ausschließlich serialisierbare eigene Datentypen und rekursive `KnotenEigenschaft`-Werte. Nicht erlaubt sind:

- Composables oder Funktionsreferenzen,
- Compose-`State`, `MutableState` oder `Modifier`,
- Android-Kontexte,
- Canvas- oder Layoutobjekte,
- Auswertungscaches und abgeleitete Renderdaten.

`KartenDatenJson` dekodiert und kodiert ausschließlich die fachneutralen Graphdaten. `MathematikKartenCodec` legt darüber die gemeinsame mathematische Normalisierung für Android und Desktop. Die Pipeline unterscheidet bewusst zwischen dekodiernahen Migrationen (`lese`) und der stärkeren Lade-/Importphase (`lade`/`importiere`).

Plattform-Dateispeicher sind für Pfade, Atomizität, Backups, Papierkorb oder XDG-Verzeichnisse zuständig, nicht für eigene mathematische Migrationsketten.

Änderungen am Schema müssen ältere Karten, stabile Anschlussreferenzen und unbekannte Knotentypen berücksichtigen.

## Plattformbrücken und bekannte technische Schuld

Die Desktopmodule `KnotenKartenVerwalterDesktop`, `MathematikKartenAdapterDesktop` und `MathematikKnotenDesktop` verwenden derzeit noch gemeinsame Quellverzeichnisse der Android-orientierten Bibliotheksmodule. Dieser Zustand ist eine bekannte Übergangsarchitektur und kein Vorbild für neue Module.

Eine Ablösung soll über eine offiziell unterstützte gemeinsame Android-/Desktop-Toolchain erfolgen. Solange die verwendete Kotlin-/AGP-Kombination keinen freigegebenen KMP-Android-Library-Weg bietet, wird dafür weder AGP beiläufig herabgestuft noch Produktionscode dupliziert. Fachliche Quellen wie Knotenkatalog, Auswerterregistrierung und Kartenmigration werden unabhängig davon bereits plattformneutral gehalten.

## Fehlerzustände

Fehler werden mindestens unterschieden in:

1. ungültige Knotenkonfiguration,
2. inkompatible Verbindung,
3. fehlende Eingabe,
4. mathematisch undefinierter Zustand,
5. unbekanntes oder unentscheidbares Ergebnis,
6. Auswertungsfehler,
7. unbekannter oder nicht migrierbarer persistierter Typ.

Ein Knoten soll einen fachlich verständlichen Fehlerzustand anzeigen, ohne den gesamten Graph zu beschädigen.

## Erweiterungsregel

Eine neue Abstraktion ist gerechtfertigt, wenn mindestens eines gilt:

- mehrere vorhandene und neue Knoten benötigen denselben fachlichen Vertrag,
- ein aktueller Typ kann die neue Semantik nicht korrekt ausdrücken,
- eine bestehende Kopplung verletzt eine Modulgrenze,
- die Kopplung verhindert Tests oder Persistenz,
- eine neue Versionierungs- oder Kompatibilitätsgrenze ist erforderlich.

„Die Datei war lang“ genügt weiterhin nicht als Architekturbegründung. Dateien sind keine verängstigten Tiere, die durch bloße Größe ein neues Ökosystem verdienen.
