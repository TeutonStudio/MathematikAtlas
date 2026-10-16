# Knotenanhänge außerhalb der Körpergeometrie

Status: angenommen, 2026-10-16.

## Kontext und Entscheidung

Die innerhalb der Knotengröße reservierte Zeitzeile koppelte Diagnose an
Größenänderung und führte zu unterschiedlichen Anschlussgeometrien für Rechtecke
und Adapter. Die effektive Größe bezeichnet künftig ausschließlich den Körper.
Zeit unten und vollständige Meldungen oben hängen in demselben Transformationslayer
außen an. Auswahl, Anschlusspositionen und Körpertreffer verwenden Körpermaße.
Anschluss- und Vergrößerungsgriffe behalten ihre eigenen äußeren Trefferflächen.

Renderer melden fachneutrale Texte mit Schweregrad über
`KnotenRendererAktionen.meldungenMelden`. Die mathematische Rendererhülle vereinigt
Auswertungsdiagnosen und lokale Renderdiagnosen, priorisiert Fehler und dedupliziert
Texte. Leere Listen entfernen die Anzeige. Inspector und Dialoge bleiben eigenständig.

Meldungen und gemessene Anhanghöhen sind flüchtiger Editorzustand. Gruppen und
Viewport-Culling berücksichtigen die sichtbare Umhüllung, ohne Knotenmaße zu ändern.
Der historische Wert `darstellungsFußzeilenHöhe` bleibt quellkompatibel vorhanden,
beeinflusst aber keine Körpergeometrie mehr und bleibt außerhalb des Karten-Codecs.

## Alternativen und Konsequenzen

Eine weiterhin vergrößerte Gesamtknotenfläche würde Auswahl, Anschlüsse und
Größenänderung erneut an Diagnose koppeln. Separate persistierte Diagnoseobjekte
würden Karten und Undo mit Laufzeitdaten belasten. Deshalb werden weder Schema noch
Mathematik geändert. Lange Meldungen können außerhalb des aktuellen Bildausschnitts
liegen; sie werden vollständig umgebrochen, die Karte lässt sich dorthin verschieben.


## Ergänzung: Auswahlabhängige globale Meldungsebenen, 2026-10-16

Obere Meldungsfenster nicht ausgewählter Knoten werden vor allen Knotenkörpern
gezeichnet und von diesen überdeckt. Ausgewählte Meldungsfenster werden nach allen
Körpern gezeichnet; bei Mehrfachauswahl die aktive Meldung zuletzt. Die Körper
werden dabei nicht umsortiert. Ein lokaler zIndex im Knoten könnte diese Regel
gegenüber anderen Knotencontainern nicht erfüllen, deshalb sind es globale Ebenen.

Alle Ebenen verwenden dieselbe Welt-/Kameratransformation und die vorhandenen
Anhangmessungen. Einblendende Gruppeninhalte behalten ihre Alpha-Werte. Passive
Knotenvorschauen bleiben selbständig. Keine Änderung an Körpermaßen, Anschlüssen,
Culling, Persistenz oder Undo. Überdeckte Meldungen blockieren keine Körpergesten.

Echte Compose-Pixel-/Interaktionstests prüfen beide Kartenreihenfolgen, Auswahl,
Abwahl, Mehrfachauswahl/aktiven Knoten, verschiedene Zoomstufen, Schrift1/2 und
beide Themes. Details im aktiven Pie-ExecPlan.
