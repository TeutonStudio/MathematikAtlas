# ADR: Abgeleitete Zielmenge für „Term zu Methode"

## Entscheidung

Bei `mathematik.termZuMethode` wird die effektive Zielmenge eines Zahlterms als kleinste gemeinsame Obermenge der im Inspector gewählten Grundmenge und des konservativ abgeleiteten Wertebereichs bestimmt. Die Ordnung ist ausschließlich `N ⊂ Z ⊂ Q ⊂ R ⊂ C`.

## Begründung

Ein frei gewähltes Ziel kann kleiner als die möglichen Werte des Terms sein, etwa bei einer komplexen Variablen oder einer Division über natürlichen Zahlen. Die zentrale Inferenz im Rechenkern bewahrt eine fachliche Wahrheit und verhindert abweichende Ableitungen in Inspector oder Renderer. Sie berechnet keine exakte Bildmenge, sondern eine sichere Obermenge.

## Konsequenzen

- Der gespeicherte Inspectorwert bleibt eine Untergrenze und Rückfallwert; gespeicherte Karten benötigen keine Migration.
- Nichtnumerische Terme behalten den Inspectorwert: Für Aussagen, Mengen, Vektoren und Matrizen gibt es noch keinen strukturierten gemeinsamen Zielmengenvertrag.
- Unsicher reelle oder partielle Zahloperationen werden konservativ nach `C` angehoben.
