# Geometrieansichten nach Desmos und GeoGebra verbessern

## 1. Status

Abgeschlossen und unabhängig geprüft. Analyse der offiziellen Dokumentation und des vorhandenen Codes, anschließend gezielte Optimierung vorhandener Ansichten. Bestehende ungecommittete Geometry-Nodes-Grundlage bleibt erhalten.

## 2. Ziel und Nutzerwirkung

Kleine oder weit vom Ursprung entfernte Geometrie bleibt sichtbar. Nutzer können einpassen, orthogonale Blickrichtungen wählen, drehen, verschieben und per Pinch beziehungsweise Mausrad zoomen. Kameraänderungen benötigen pro Renderpunkt nur eine Projektion; fachliche Geometrie bleibt unverändert.

## 3. Nicht-Ziele

Kein vollständiger Desmos-/GeoGebra-Nachbau, keine neuen mathematischen Knoten, keine direkte Punktmanipulation oder neuen Constraints. Untersucht, aber nicht ohne eigenen fachlichen Vertrag ergänzt: algebraisch gekoppelte Punktbewegung, Schnittpunkt-Snapping, dynamische Konstruktionsbedingungen. Bestehende exakte Geometrieachsen nicht ungeprüft in ein numerisches Geometry-Set umdeuten.

## 4. Istzustand und Quellen

Arbeitsbranch `samai/v3.3.0-geometry-nodes`, Basis `d435b4fcf62ce83409911f98c08f0a096e78c0dd`; 15 neue Typen aus dem vorherigen Auftrag, keine zusätzlichen Typen. `GeometryNodesVorschauRenderer` normiert am absoluten Ursprung statt an der Objektausdehnung: ein Würfel bei x=10^12 wird winzig. Jede Kantenreferenz berechnet dieselbe Projektion erneut. Es gibt Drehen und Zoomknöpfe, aber weder Pan noch Pinch/Mausrad oder feste Blickrichtungen. Die ältere `GeometrieVisualisierungsKnotenRenderer` besitzt bereits Achsen und Pan/Pinch, bleibt eine getrennte exakte Domäne.

Offizielle Quellen am 2026-10-15 per HTTPS gelesen:

- [Desmos Tables](https://help.desmos.com/hc/en-us/articles/4405489674381-Tables): Zoom Fit, Punkt-/Linienstile.
- [Desmos API v1.11](https://www.desmos.com/api/v1.11/docs/index.html): setMathBounds, mathematisches Seitenverhältnis bei contain, Sichtbarkeit und Darstellungsparameter. Keine Aussagen über den internen Desmos-Renderer aus diesen öffentlichen APIs ableiten.
- [GeoGebra 3D Graphics View](https://geogebra.github.io/docs/manual/en/3D_Graphics_View/): getrennte Navigation, Standardansicht, Blickrichtung, Achsen und Mausradzoom.
- [GeoGebra Move Graphics View](https://geogebra.github.io/docs/manual/en/tools/Move_Graphics_View/): Verschieben und Modifikatorbedienung.
- [GeoGebra Object Properties](https://geogebra.github.io/docs/manual/en/Object_Properties/): Sichtbarkeit und Fixierung von Objekten; Anregung für lokale Darstellungsoptionen.

Der Vergleich beruht auf dokumentierten Funktionen, nicht auf einer gemessenen internen Implementierung oder interaktivem Browserbenchmark.

## 5. Semantik

Geometrieänderungen und Navigationszustand bleiben getrennt. Kamera und Sichtbarkeit sind lokale Vorschauoptionen. Orthografische Projektion mit gleichem Maßstab auf allen Achsen; Normalisierung aus Boundingbox-Ausdehnung statt Abstand zum Ursprung. Achsenweise vorzentrieren; bei überlaufendem Spannbereich den Mittelpunkt aus halben Extremwerten bilden. Erst die Differenzen gemeinsam normieren, damit große Translationen keine kleinen orthogonalen Ausdehnungen verschlucken. Nachzentrieren erhält subnormale Asymmetrien. Entartete und leere Geometrie bleiben darstellbar.

## 6. Daten-/Knotenvertrag

Keine neuen Typ-Schlüssel oder Anschlüsse. Keine Änderung der Mesh-/Punkt-/Instanzdaten oder ihrer fachlichen Auswertung. Ein abgeleiteter Renderplan fasst identische Renderkoordinaten zu Indizes zusammen; diese Indizes sind keine fachlichen Vertex-IDs. Feld- und Topologieidentität bleiben unverändert.

## 7. Architektur

Reine Darstellungs-/Kamerafunktionen in `MathematikKnoten/geometrynodes`, keine Compose-Abhängigkeit im Rechenkern. Renderplan bei Geometrieänderung vorbereiten, pro Draw eine Projektion je eindeutiger Renderkoordinate. Keine Realisierung von Instanzen für Kameraaktionen. Bestehende Vorschau-/Traversierungsgrenzen bleiben erhalten.

## 8. Dateien

Neue `GeometryAnsicht.kt`, angepasster `GeometryNodesVorschauRenderer.kt`; gezielte JVM-/Desktoptests. Analyse/Anleitung, CURRENT_STATE und dieser Plan. Alte Tests und Geometry-Nodes-Code bleiben Ausgangsbasis.

## 9. Meilensteine

- [x] Offizielle Quellen und konkretes Fehlverhalten untersucht.
- [x] Ausdehnungsbasierte Normalisierung und indizierter Renderplan; zehn neue Regressionstests grün.
- [x] Navigation und Ansichtsoptionen im gemeinsamen Renderer; sechs Desktop-Tests inklusive drei neuer Fälle grün.
- [x] Gesamtsuite mit 1.820 Tests, Android-Build/Instrumentierungskompilierung und unabhängige Delta-Prüfung erfolgreich.

## 10. Umsetzung

Zuerst reine Projektions-/Kameraoperationen mit Tests. Danach gemeinsame Compose-Steuerung und Gesten. Ein schreibender Implementierer; Verifizierer liest nur. Ein vollständiger Prüflauf nach gezielten Regressionen.

## 11. Validierung

Verschobener Würfel behält dieselbe Bildausdehnung wie am Ursprung. Sehr kleine/große, entartete und leere Geometrie liefern endliche Bildschirmkoordinaten. XY/XZ/YZ bilden passende Achsen ab, Zoom hält den Ankerpunkt, Pan verändert nur Bildlage. Ein Würfel verwendet acht statt 32 redundanter Punktprojektionen. Desktop-Gesten und Reset sowie Schalter tatsächlich ausführen. Gesamtsuite und Android-Build ausführen; keinen Android-Laufzeitnachweis aus Kompilierung ableiten.

## 12. Persistenz

Keine Änderungen an gespeicherten Karten, Formatversion oder Undo. Kamera-/Darstellungsoptionen bleiben flüchtig.

## 13. Risiken

Viewportgesten dürfen den Karteneditor nicht verschieben; Änderungen konsumieren, Kopf bleibt zum Knotenziehen erhalten. Zoomen/Drehen darf den Geometrie-Renderplan nicht neu auswerten. Numerische Genauigkeit bleibt durch die Eingabe-Doubles beschränkt.

## 14. Fortschritt

Quellenprüfung, gezielte Implementierung, Regressionsprüfung und vollständige Tests abgeschlossen. Die unabhängige Prüfung fand einen gemischten numerischen Extremfall; die Korrektur und zwei zusätzliche Regressionsfälle sind abgenommen.

## 15. Entscheidungen

2026-10-15: Öffentliche Desmos-/GeoGebra-Ansichtsmerkmale auf die numerische 3D-Vorschau anwenden. Punktziehen/Constraints benötigen eigene fachliche Verträge und werden als Analyseempfehlung dokumentiert. Keine neue Versionsreservierung: additive Verbesserungen innerhalb der bereits reservierten 3.3.0.

## 16. Abweichungen

Optimierungen betreffen die numerische Geometry-Nodes-Vorschau. Die exakte mathematische Geometrie und direkte Punktmanipulation wurden untersucht und mit konkreten Folgeempfehlungen dokumentiert, statt deren fachliche Verträge beiläufig zu verändern.

## 17. Ergebnis

Die offizielle Dokumentation liefert unmittelbar übertragbare Ansichtsverbesserungen; vollständige Analyse unter `docs/GEOMETRIE_DESMOS_GEOGEBRA.md`. Implementiert sind robuste ausdehnungsbasierte Zentrierung, maßstabstreue orthografische Projektion, vorbereiteter indizierter Renderplan, Einpassen, 3D/XY/XZ/YZ, getrennte Navigation, Pan/Pinch/Mausrad und lokale Punkt-/Kantensichtbarkeit. Keine zusätzlichen Knoten, Kartenformat- oder Geometriesemantikänderungen.

Zehn neue reine Projektions-/Kameratests und drei neue Desktop-Tests erfolgreich; Desktop führt Mausrad und Zwei-Finger-Zoom innerhalb eines echten Karteneditors aus, ohne Kartenansicht/Knotenposition zu verändern. Frühere Dreh-/Reset-/Inspector-Tests bestehen. Voller Lauf `test :app:assembleDebug :app:compileDebugAndroidTestKotlin` erfolgreich in 38 s: XML-Summe 1.820 Tests, keine Fehler und keine übersprungenen Tests. Repository-/Architektur-/Releaseplan-/Versionsfolge- und Diffprüfungen erfolgreich. Screenshot `/workspace/artifacts/geometry-ansicht-optimiert.png` visuell geprüft. Android-Instrumentierung kompiliert; neuer Stand nicht auf Android ausgeführt.

Unabhängige Delta-Prüfung ohne weitere Findings nach Fix des gemischten Extremfalls. Branch `samai/v3.3.0-geometry-nodes`, Basis `d435b4fcf62ce83409911f98c08f0a096e78c0dd`; Arbeitsstand bleibt ungecommittet, 3.3.0 reserviert, veröffentlichte Produktmetadaten 3.2.2.


### Nachfolgender Integrationsauftrag

2026-10-15: Nach Abschluss dieses Funktionsplans hat der Nutzer einen neuen Master-Commit und Push beauftragt. Die vorstehenden Angaben zu Reservierung/ungecommittetem Stand sind historische Prüfstände. Die nachfolgende3.3.0-Releasevorbereitung mit vollständigem Funktionsumfang und Remote-Prüfungen wird im [Integrationsplan](../active/2026-10-15-v3-3-0-master-integration.md) dokumentiert.
