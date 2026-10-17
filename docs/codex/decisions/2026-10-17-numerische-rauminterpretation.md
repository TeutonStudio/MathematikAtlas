# Numerische Rauminterpretation der Visualisierung

Datum: 2026-10-17. Status: angenommen.

## Problem

Dimensionsableitung, direkte Mengenanzeige, Filter und Bildmengen hatten eigene
Typfalllisten. Daher scheiterte beispielsweise ℝ², während ℝ×ℝ funktionierte.
Diskrete Filter wurden auf einem kontinuierlichen Raster abgetastet und verloren
Punkte. Matrixprojektionen wurden angefordert, aber auch mit konfigurierten
Indizes abgelehnt. Nicht unterstützte Träger erschienen als unentscheidbare Fläche.

## Entscheidung

`NumerischerRaum` beschreibt geordnete reelle Blattkoordinaten, deren Grundmengen
und die strukturtreue Rückverpackung. Diese Laufzeitbeschreibung ist keine neue
mathematische Wertform und wird nicht persistiert. Sie liegt im gemeinsamen
Visualisierungsmodul, nicht im fachneutralen Editor.

- Kartesische Potenzen, Tupel, Produkte und verschachtelte Strukturen behalten
  ihre Tupelgrenzen. Auch ein Einertupel bleibt ein Tupelargument.
- Vektoren behalten ihre Orientierung. Matrizen verwenden Zeilenreihenfolge,
  Tensoren ihre vorhandene lineare Speicherreihenfolge. `matrixForm` bestimmt
  weiterhin den tatsächlichen Matrixwert gegenüber einem Tensorwert.
- Komplexe Skalare verwenden Re/Im in dieser Reihenfolge. Quaternionische
  Ergebnisse verwenden bei ausdrücklicher Projektion Re/i/j/k.
- Projektionen wählen erst aus dem vollständigen Wert aus. Endliche Mengen
  werden vor der Projektion ausgewertet; unabhängige Produkträume dürfen direkt
  auf ausgewählte Faktoren projiziert werden.
- Ein Filter wird durch Weglassen von Koordinaten nicht automatisch existenziell
  projiziert. Ausgelassene Blattkoordinaten benötigen feste Schnitte `k1`, `k2`,
  …, oder eine ausdrückliche Bildmengenabbildung. Fehlende Schnitte werden erklärt.
- Zusätzliche Bedingungen, etwa gaußsche Primzahleigenschaft, bleiben erhalten.
  Ein Produkt aus einem eingeschränkten Raum darf nicht als voller Produktraum
  zertifiziert werden. Unbewiesene zusätzliche Argumentbedingungen werden mit
  Diagnose ausgelassen, niemals als bewiesene Bildpunkte verwendet.
- Diskrete Achsen werden innerhalb des Fensters vollständig und vorab budgetiert
  aufgezählt. Große Produkte werden abgelehnt; einzelne Zweige werden nicht
  stillschweigend übersprungen. Primzahlentscheidungen stammen aus dem Kern.
- Sichere Produkthüllen für Mengenalgebra liefern Samplingachsen, ersetzen aber
  niemals die ursprüngliche Mitgliedschaft. Auch diskrete Differenzen als
  Produktfaktoren und gemischte diskrete/stetige Vereinigungen behalten ihre Zweige.
  Einschränkungen von Matrixräumen behalten die Matrixform auch verschachtelt
  innerhalb eines Tupelarguments.
- Ausbleibende Fenstertreffer beweisen keine mathematisch leere Grundmenge.

## Grenzen ohne willkürliche Ersatzinterpretation

Gradmenge und Restklassenring sind Quotienten, keine reellen Zahlenintervalle.
Ihre Rohdarstellung verlangt ausdrücklich eine Vertreter- oder Kreisabbildung.
Funktionenräume benötigen Auswertungsstellen, Folgenräume ausgewählte Glieder,
Potenzmengen eine numerische Abbildung ihrer Mengenelemente. Benannte abstrakte
Mengen brauchen ebenfalls eine angegebene Interpretation. Diese Diagnosen gelten
auch für Wahr-Filter; es wird keine irreführende unbekannte Fläche erzeugt.

Symbolische Dimensionen bleiben bedingt, bis sie gebunden sind. Ein Raum mit null
Koordinaten enthält das leere Tupel (), ist jedoch kein räumlicher Darstellungsraum.
Die Ablehnung seiner räumlichen Darstellung ist kein Leermengennachweis.

## Konfiguration und Kompatibilität

Vorhandene 1-basierte Projektionsindizes und feste Schnitte bleiben das gespeicherte
Format. Ein bis drei ausgewählte Koordinaten stellen die Dimension atomar auf
ℝ, ℝ²/ℂ oder ℝ³. Es entstehen keine neuen Knotentypen, Anschlussänderungen,
Produktionsabhängigkeiten oder Kartenmigrationen.

## Verworfene Alternativen

Unbedingtes Abflachen mathematischer Werte würde Signaturen und Orientierung
zerstören. Ein kontinuierliches Raster für ganze Zahlen verliert Zweige.
Automatische Vertreterwahl für Quotienten verschweigt die gewählte Abbildung.
Separate Typfalllisten pro Darstellungspfad würden dieselben Fehler erneut erlauben.

## Nachweise

`VisualisierungsTypVertragTest`, `KompakterRaumKartenRegressionTest` und die
Darstellungstests `VisualisierungsTypenGeraeteTest`/`VisualisierungsTypenDesktopTest`.
Ausführungsstand und Umgebungsgrenzen stehen im zugehörigen ExecPlan.
