# Vier Inspectorbereiche und freie Werkzeugdarstellung

Datum: 16.10.2026. Entscheidung aus der bestätigten Inspectorüberarbeitung.

Android und Desktop verwenden dieselben fachneutralen Abschnitts-, Zellen- und
Standarddatenbausteine. Die Reihenfolge lautet Standarddaten, Knotendaten,
Eingangsdaten, Ausgangsdaten; leere Bereiche entfallen. Bereiche beginnen offen,
Zellen geschlossen. Sitzungsschlüssel bestehen aus Karte, Knoten und stabiler
Teil-/Anschluss-ID. Klappen verändert weder Karte noch Undo.

Richtungsweite Optionen stehen in der ersten Zelle einer Richtung; individuelle
Optionen am tatsächlichen Anschluss. Ein Mehrfacheingang bleibt eine Zelle und
zeigt alle Beiträge. Der vorhandene Inspectorvertrag erhält kompatible Bereichs-
und Anschluss-Einstiegspunkte. Fachliche Konfiguration bleibt in vorhandenen
Konfigurationsfunktionen und wird über Konfigurationsplan/Kartenaktionen übernommen.

Hintergrund- und Rahmenfarben werden optional als `KnotenEigenschaft.Farbe` unter
`atlas.hintergrundfarbe` und `atlas.rahmenfarbe` gespeichert. Fehlen sie, gilt das
bisherige Theme. Auswahlmarkierung hat weiter Vorrang. Die vorhandene Eigenschaft
`atlas.darstellungsform` unterstützt zusätzlich `ellipse`, `dreieck` und `raute`.
`adapter` bleibt die historische Kapsel. Unbekannte Werte ergeben Rechteck.
Notizen geben die Formwahl frei; mathematische Knoten behalten ihre feste Form.

Kontur, Treffer, sichere Inhaltsfläche und Anschlusspositionen werden zentral
beschrieben. Diagnose-/Zeitanhänge bleiben außerhalb des Körpers. Neue Felder
oder ein neues JSON-Format sind nicht erforderlich; der Eigenschaftscodec bleibt
maßgeblich. Alternativen waren plattformspezifische Abschnittshüllen und eine
neue persistierte Inspectorstruktur; beide würden unnötige Zustandsduplikation
und zusätzliche Kompatibilitätsgrenzen schaffen.
