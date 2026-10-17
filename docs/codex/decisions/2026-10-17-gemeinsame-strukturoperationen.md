# Gemeinsame Matrix-, Vektor- und Tensoroperationen

Datum: 2026-10-17. Status: M2 in Umsetzung, v3.4.4/x reserviert; Veröffentlichung offen.

## Kontext

Spur, Matrixdiagonale, Vektorradius und Kreuzprodukt hatten eigene erzeugbare Typen,
obwohl bestehende Rechner dieselben Operationen anbieten. Die Diagonalmodi besaßen
zusätzliche Projektionsverträge. Doppelte Radius-/Normformeln belegten außerhalb
reeller Komponenten keine Norm; Methodenhebung verlor Bindungsidentitäten und
Voraussetzungen. Typverbreiternde Anschlussnormalisierung konnte semantische
Portverträge beim anschließenden Laden verwerfen.

## Entscheidung

Die vier Spezialtypen werden ausschließlich als konfigurierte Varianten vorhandener
Matrix- und Vektorrechner erzeugt. Historische Wissens-IDs bleiben auflösbar. Spur
verwendet den bestehenden Kernalgorithmus. Diagonaloperatoren lesen Rang zwei und
projizieren ausdrücklich in Tupel, Zeile oder Spalte. Nebendiagonalen rechteckiger
Matrizen bleiben rechts oben verankert; Erzeugung unterstützt Haupt und Neben.
Auslesen weist auf verworfene Nichtdiagonaleinträge oder rechteckige Form hin.

Reelle euklidische Norm, Kreuzproduktkomponenten und Basisvektorerzeugung haben je
einen Fachalgorithmus. Die Norm benötigt nachgewiesen reelle, definierte Komponenten.
Quellenbereiche folgen Bindungsidentitäten. Eingeschränkte Zahlbereiche liefern eine
konservative Grundmenge für Ergebnisinferenz; die präzise Definitionsmenge bleibt
unangetastet. Aus einem reellen zusammengesetzten Wert wird kein Bereich seiner
inneren Variablen rückgeschlossen. Typisierte Komponenten benötigen einen gültigen
Index und einen nachgewiesenen reellen Quellraum. Offene Definitionsbedingungen
werden strukturiert weitergegeben; widerlegte verhindern die Berechnung.

Das klassische Kreuzprodukt benötigt einen kommutativen Koeffizientenbereich und
Dimension drei. Historisch konfigurierte Zeilen-/Spaltenorientierung bleibt explizit
validiert. Die vorhandene freie Orientierung des kanonischen Rechners bleibt ohne
solche Auswahl erhalten. Die konjugierte Tensor-Norm bleibt ein getrennter Vertrag.

Transponieren behält seinen bestehenden Typ und delegiert gemeinsame Kernoperationen.
Vektortransposition wechselt Zeile/Spalte; eine Tensorachsenpermutation erhält den
Tensor, auch bei Rang eins. Beliebige zulässige Tensorpermutationen bleiben verfügbar.

Punktweise Methodenhebung verwendet Bindungs-IDs und genau ein logisches Ergebnis
pro Wertoperand. Ein tupelwertiges Einzelresultat bleibt eine Struktur. Bekannte
mathematische Wrapper verwenden die vorhandene Materialisierung; allgemeine
Capabilities ohne symbolische Vorschrift werden ausdrücklich abgelehnt. Alle
Definitionsräume und Voraussetzungen werden nach Angleichung geschnitten bzw.
übernommen. Neu entstehende Bedingungen gehören auch zur erzeugten Methode, sodass
Extrahieren und späterer Aufruf sie nicht verlieren.

## Migration und Bedienung

Die bestehende Codec-Pipeline normalisiert die vier Altarten vor späteren
Anschlussnormalisierungen, einschließlich eingebetteter Methodenquellen. Eindeutige
Rollen, Typen, Kardinalitäten und Modi sind Voraussetzung. IDs, Verbindungen,
Reihenfolgen, Defaults, feste Kartenverweise und typisierte Rollenreferenzen bleiben
erhalten. Mehrdeutige Knoten behalten ihre Originaldaten plus blockierende Diagnose.
Gleichoperatorige Normalisierung bewahrt migrierte semantische Portverträge.

Android und Desktop verwenden gemeinsame Optionen für Diagonalprojektion,
Diagonalart und Kreuzproduktorientierung. Änderungen gehen als vollständige
Knotenkonfiguration durch den vorhandenen KonfigurationsPlan: relevante
Verbindungsverluste werden vor Übernahme benannt, der Wechsel bleibt ein Undo-Schritt.
Unbekannte gespeicherte Werte werden sichtbar diagnostiziert.

Berechnete Werte übernehmen Voraussetzungen, Quellen und Übersetzungsherkunft.
Ein Zeugnis des Eingangswerts wird durch eine mathematische Änderung nicht zum
aktiven Zeugnis des Ergebnisses. Unveränderte Literale behalten gültige Zeugnisse.
Kartenformat und Mathematik-Wire-Payload bleiben unverändert.

## Evidenz und Grenzen

Gezielte Kern-, Methodenhebungs- und Codec-Regressionen sowie der vollständige lokale
Produktlauf (2.180 Tests, 0 Fehler, 1 bestehender Skip, Lint/APKs/Desktop) bestehen.
Die unabhängige Prüfung einschließlich tatsächlicher Einzelanschlussbelegung und
N₀-Grundmengeninferenz meldet keine weiteren Findings. Eine historische
Matrixdiagonalen-Definitionskarte bleibt unter test/resources/konsolidierung-m2
unverändert gesichert (SHA-256 763ee69d0e1eb2dce3bc7d504a7eb735fc6d1e9ea5ef14923324751a9f9fe16f).
Geräte-/RPM-Abnahme und Veröffentlichung werden dem freigegebenen SHA im aktiven Gesamtplan zugeordnet.
