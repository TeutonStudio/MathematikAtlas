# Geometrie: Vergleich mit Desmos und GeoGebra

Stand: 2026-10-15. Untersucht wurden die offiziellen Dokumentationen und der aktuelle Atlas-Code. Es wurden keine internen Desmos-Algorithmen oder Bildraten der drei Anwendungen gemessen.

## Ergebnisse und Umsetzung

| Beobachtung | Nutzen im Atlas | Ergebnis |
|---|---|---|
| Desmos: „Zoom Fit“ passt den Ausschnitt an Daten an. | Kleine Objekte müssen auch weit vom Ursprung sichtbar bleiben. | Geometry-Nodes-Vorschau anhand der Objektausdehnung normalisiert; „Einpassen“ setzt Pan und Zoom zurück. |
| Desmos API: Darstellung mit mathematisch erhaltenem Seitenverhältnis. | Kreise und geometrische Proportionen dürfen nicht durch unabhängige Achsenskalierung verzerrt werden. | Einheitlicher orthografischer Maßstab für alle drei Achsen. |
| GeoGebra: getrennte Ansichtsbewegung, Rotation, Mausradzoom und Standardansicht. | Auf Tablet und Desktop zuverlässig navigieren. | Drehen-/Verschieben-Modus, Shift-Ziehen, Zwei-Finger-Pan/Pinch, Mausradzoom am Zeigeranker und Reset. |
| GeoGebra: Blickrichtung und sichtbare Achsen. | Räumliche Beziehungen leichter erkennen. | 3D/XY/XZ/YZ-Blickrichtungen und beschriftete Orientierungshilfe. |
| Desmos-/GeoGebra-Darstellungsoptionen. | Dichte Drahtmodelle übersichtlich betrachten. | Punkte und Kanten lokal ein-/ausblendbar. |
| Eigene Codeprüfung: dieselben Kantenendpunkte wurden mehrfach projiziert. | Weniger wiederholte Arbeit bei Kameragesten. | Abgeleiteter indizierter Renderplan; pro eindeutiger Renderkoordinate eine Projektion je Frame, Sinus/Cosinus pro Kamera vorbereitet. |

Die Änderungen betreffen die gemeinsame **numerische Geometry-Nodes-Vorschau** auf Android und Desktop. Die vorhandene exakte mathematische Geometrie verwendet weiterhin ihr eigenes Koordinaten-/Achsenmodell; sie wird nicht implizit in numerische Meshes konvertiert.

## Konkrete Fehlerkorrekturen

Vorher bestimmte der Abstand zum Ursprung den Maßstab. Ein kleiner Würfel bei X=10¹² wurde dadurch fast unsichtbar. Nun bestimmt die Boundingbox-Ausdehnung den Maßstab. Die Zentrierung erfolgt zuerst achsenweise, mit einer überlaufsicheren Mittelpunktberechnung. Auch `(10³⁰⁸, 0, 0)` und `(10³⁰⁸, 10⁻²⁰, 0)` bleiben als Linie unterscheidbar. Ein nachfolgender Normalisierungsschritt hält subnormale und entartete Daten mittig im Bild.

Vorher wurden acht Würfeleckpunkte und zwölf Kanten mit je zwei Endpunkten insgesamt 32-mal projiziert. Der neue Renderplan benötigt dafür acht Projektionen. Diese Zahl ist eine geprüfte Arbeitsreduktion für diesen Fall, keine behauptete allgemeine Beschleunigung um einen festen Faktor oder gemessene FPS-Steigerung.

Der Renderplan dedupliziert nur Koordinaten für die Anzeige. Er verschweißt keine Mesh-Vertices, verändert keine Feldindizes und realisiert keine Instanzen. Kameragesten führen keine mathematische Geometrieauswertung aus. Harte Vorschaugrenzen und die sichtbare Kennzeichnung begrenzter Ansichten bleiben erhalten.

## Weitere sinnvolle Ausbauschritte

1. **Frei bewegliche Punkte mit klaren Abhängigkeiten.** GeoGebra zeigt die enge Kopplung zwischen algebraischer und geometrischer Darstellung. Im Atlas müsste Punktziehen eine atomare Parameteränderung am erzeugenden Knoten auslösen. Ein durch ein Feld oder eine Bedingung bestimmter Punkt darf nicht unabhängig vom Modell verschoben werden. Dafür braucht es einen eigenen Konstruktions-/Bearbeitungsvertrag.
2. **Fangen und Auswahl im Bildschirmraum.** Auswahlradien sollten in Bildschirmpixeln gelten; Punkt-/Kanten-/Schnittpunktprioritäten müssen eindeutig sein. Ein visuelles Einrasten darf keine unerwähnte mathematische Bedingung erzeugen.
3. **Koordinatenraster und Beschriftungen für numerische Ansichten.** Das vorhandene exakte Geometriemodell besitzt bereits Achsen und adaptive ganzzahlige Schritte. Ein gemeinsames Raster benötigt einen ausdrücklich definierten Weltmaßstab und kollisionsarme Labels; die neue Orientierungshilfe zeigt nur Richtungen.
4. **Stabiler Welt-Ausschnitt für Animationen.** Desmos-artige feste Ansichtsgrenzen können verhindern, dass eine sich ändernde Geometrie automatisch neu eingepasst wird. Dazu müssten ursprünglicher Weltmaßstab, explizites Einpassen und numerische Darstellungsgrenzen gemeinsam modelliert werden.

Diese Folgeschritte wurden analysiert und nicht als bereits implementierte Funktion dargestellt. Es wurden keine neuen Knoten oder gespeicherten Kartenformate eingeführt.

## Quellen

- [Desmos Tables](https://help.desmos.com/hc/en-us/articles/4405489674381-Tables): „Zoom Fit“ sowie Punkte/Linien.
- [Desmos API v1.11](https://www.desmos.com/api/v1.11/docs/index.html): `setMathBounds`, mathematisches Seitenverhältnis, Punkt-/Linien-/Sichtbarkeitseinstellungen.
- [GeoGebra 3D Graphics View](https://geogebra.github.io/docs/manual/en/3D_Graphics_View/): Navigation, Blickrichtung, Standardansicht und Darstellungsoptionen.
- [GeoGebra Move Graphics View](https://geogebra.github.io/docs/manual/en/tools/Move_Graphics_View/): Verschieben und Modifikatorbedienung.
- [GeoGebra Object Properties](https://geogebra.github.io/docs/manual/en/Object_Properties/): Sichtbarkeit, Fixierung und Auswahl von Objekten.
