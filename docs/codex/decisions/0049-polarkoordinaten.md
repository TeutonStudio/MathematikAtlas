# ADR 0049: Reelle Tupel in Hypersphärenkoordinaten

Datum: 2026-10-12. Status: angenommen.

## Entscheidung
Koordinatenumrechnungen liegen gemeinsam im Compose-freien Rechenkern.
Bestehende Übersetzerziele und Vektorrechneroperationen verwenden dieselbe Umsetzung.
Für n≥2 steht Azimut zuerst, gefolgt von Polarwinkeln zu x₃,…,xₙ.
Radius ist nichtnegativ; Gradintervalle sind [0,360) und [0,180].
Nicht bestimmte Teilwinkel werden kanonisch null.

R¹ verwendet Betrag und skalares Vorzeichen. Ein Winkeltupel mit einem Eintrag
bezeichnet stets R². Nur das flache Übersetzertupel (r,s) ist mehrdeutig und benötigt
für die Rückrichtung den gespeicherten R¹-Modus, Standard aus.

## Alternativen und Konsequenzen
Ein signierter Radius oder ein zusätzliches Koordinatenobjekt wurde zugunsten der
vom Nutzer gewünschten Tupel und der bestehenden Knoten verworfen. Winkelreihenfolge
ist explizit dokumentiert; ein weiterer Katalog oder Knotentyp ist nicht erforderlich.
Numerische Auswertung benötigt endliche reelle Komponenten. Resultate sind
Gleitkommanäherungen; unbestimmte Ausdrücke werden nicht als gültige Zahlen ausgegeben.
