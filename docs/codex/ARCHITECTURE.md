# Architektur des Mathematik Atlas

## Zweck

Dieses Dokument beschreibt die beabsichtigten Verantwortungsgrenzen der nativen Android-Anwendung. Es behauptet nicht, dass der aktuelle Code sie bereits vollständig erfüllt. Abweichungen werden im ExecPlan und gegebenenfalls als technische Schuld dokumentiert.

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
- generische Renderer-Verträge.

Nicht verantwortlich für:

- mathematische Auswertung,
- mathematische Knotenschlüssel,
- knotenspezifische Parameternamen wie `festeEingänge`,
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
- Standardauswerter und deren Registrierung,
- spezialisierte Compose-Renderer,
- native Darstellung des vom Rechenkern erzeugten LaTeX-Teilumfangs,
- fachbezogene Konfigurationen wie Matrix- oder Visualisierungsparameter.

Renderer dürfen fachliche Ergebnisse darstellen und Interaktionen melden, aber keine unabhängige mathematische Semantik pflegen.

### 5. `app`

Verantwortlich für:

- Anwendungseinstieg und Material-3-Oberfläche,
- Kartenbibliothek, Ordner und Navigation,
- Inspector und anwendungsspezifische Dialoge,
- Zusammenstellung der Vorlagen- und Renderer-Kataloge,
- Laden, Speichern, Import, Export und Migration,
- Koordination von Editorzustand und Auswertung.

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

- `MathematikKnotenVorlagen.alle` als statischen Vorlagenkatalog,
- dynamisch aus Karten abgeleitete Gruppenvorlagen im App-Zustand,
- `MathematikAuswerterRegister` für mathematische Auswerter,
- Renderer-Zuordnung über die vorhandenen App- und Knotenpfade,
- `AnschlussArtRegister` für die Typkompatibilität von Anschlüssen.

Agenten erweitern den jeweils zuständigen vorhandenen Mechanismus. Sie führen kein paralleles Register ein, nur weil mehrere bestehende Register unterschiedliche Verantwortungen besitzen.

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

Änderungen am Schema müssen ältere Karten, stabile Anschlussreferenzen und unbekannte Knotentypen berücksichtigen.

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