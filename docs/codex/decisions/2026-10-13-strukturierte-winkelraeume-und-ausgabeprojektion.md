# Strukturierte Winkelräume, Produktabtastung und Ausgabeprojektion

Status: angenommen; Umsetzung im Releasekandidaten v3.2.0.

## Kontext

Winkelkomponenten gehören zu einem gemeinsamen Argument in W_n = {−1,+1} × [0°,180°)^n. Eine Methode mit einem Tupelargument kann trotzdem beliebig viele kontinuierliche Samplingachsen besitzen. Die bisherige Beschränkung auf höchstens drei öffentliche Variablen und die direkte Auswertung einer Termvorschrift verloren diese Struktur.

## Entscheidung

`VariablenQuelle.StrukturQuellenBindung` rekonstruiert gemeinsame Identität, sichtbaren Parameternamen, Gesamtwertebereich und Komponentenpfade aus Knotendaten. Term→Methode bindet eine Struktur einmal, mit kollisionsfreien Namen einschließlich zusätzlicher Parameterquellen und Annahmen. Koordinatentupel werden als ein kanonisches Ergebnis gepackt; historische explizite Mehrfachausgaben erhalten ihre Namen. Die öffentliche Definitionsmenge eines einzelnen strukturierten Arguments bleibt dessen Gesamtwertebereich.

Polarübersetzer speichern `uebersetzer.polarKonvention`. Neue Konfigurationen verwenden SIGNED, historisch eindeutige Winkeltupelquellen werden so migriert; andere historische Konfigurationen erhalten CLASSIC. Die Auswertung wählt ihre Konvention nicht anhand zufälliger Metadaten. Numerische und symbolische signierte Richtungen verwenden dieselbe Rekursion. Typisierte Winkel werden nur am ausdrücklichen W_n-Argumentvertrag in Gradrepräsentanten normalisiert. Variable, Richtung und Konvertierung erlauben gemeinsam bis zu 64 Winkel.

`StrukturierteBildmengen` zerlegt Produktdomänen in einen Packungsbaum und Samplingachsen. Öffentliche Argumentzahl und Achsenzahl bleiben getrennt. Der bestehende Methodenaufruf erhält vollständig gepackte Werte, keine implizit abgeflachten Komponenten. Endliche Intervalle werden vollständig verwendet, unabhängig von der Kamera. Innerhalb des Budgets entsteht ein Produktgitter; passt schon ein Raster mit zwei Werten je kontinuierlicher Achse nicht, entsteht eine deterministische Halton-Approximation. Für jeden kontinuierlichen Stützpunkt werden sämtliche diskreten Zweige besucht. Linien und Dreiecke werden nur innerhalb eines einzelnen Zweigs erzeugt; Halton ergibt eine gekennzeichnete Punktapproximation.

`VisualisierungsKonfiguration.ausgabeKoordinaten` speichert zwei oder drei unterschiedliche 1-basierte reelle Ausgabekoordinaten. Sie beeinflussen den Samplingcache. Der gesamte Ergebniswert wird vor der Projektion ausgewertet; ausgeblendete undefinierte Komponenten bleiben Fehler. Dies gilt auch für die Auswahl öffentlicher Ausgangsnamen. Ohne ausdrückliche Projektion bleibt ein Ergebnis oberhalb R³ projektionsbedürftig. Die mathematische Bildmenge wird nie durch die Anzeigeprojektion ersetzt.

## Folgen und Grenzen

Der fachneutrale Editor kennt weder Winkel noch Projektionen. Alle neuen Daten liegen in vorhandenen serialisierbaren Knotenparametern oder Eigenschaften; keine neue Knotenart, Produktionsabhängigkeit oder JSON-Formatversion. Laufzeitbindungen und Samplingdaten werden nicht gespeichert. Nicht unterstützte Produktfaktoren und zu große vollständig diskrete Domänen liefern konkrete Diagnosen; es werden keine Zweige stillschweigend ausgelassen. Höhere kontinuierliche Dimensionen sind numerische Approximationen, kein geometrischer Vollständigkeitsbeweis.

## Nachweis

`WinkelraumMethodenTest` prüft Strukturbindung, Zusatzquellen, Namenskollisionen und Migration. `WinkelraumSamplingTest` prüft W₀, getrennte Halbkreise, alle Kugelachsen, DF und die Ellipsoidgleichung, Projektion, Halton bis Dimension 65, Variation aller inneren Achsen, JSON/Kopieren/Undo/Redo, vollständige Domänenprüfung und Abbruch. Bestehende skalare adaptive Visualisierungswege bleiben erhalten.
