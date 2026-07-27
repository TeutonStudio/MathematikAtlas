# Zielmenge als Grundmenge mengenwertiger Iterationsmethoden

Datum: 2026-07-27

## Entscheidung

Bei einer einwertigen mengenwertigen Methode wird die deklarierte Zielmenge als feste Grundmenge ihrer Mengenausgaben interpretiert. Sie wird nicht separat gespeichert.

Der Atlas liest `A : I -> G` dabei als Familie von Teilmengen von `G`; mathematisch präziser wäre `A : I -> P(G)`. Ein eigener Potenzmengen-Typ wird dafür nicht eingeführt.

Die Grundmenge muss von dem einzigen Iterationsparameter unabhängig sein. Deshalb ist `A(k) : I -> {1, ..., k}` als Methode für iterative Mengenoperatoren ungültig. Der leere Schnitt ergibt die abgeleitete Grundmenge.

## Alternativen

- Ein separates persistiertes Feld `grundMenge`.
- Eine dritte Grundmengen-Verbindung am Iterationsknoten.
- Ein Potenzmengen-Typ als Zielmenge.

## Begründung und Folgen

Alle Alternativen erzeugen eine zweite, potentiell abweichende Quelle der Wahrheit oder erweitern das Datenmodell unnötig. Validierung und Teilmengenprüfung erfolgen im Rechenkern; Adapter und Darstellung konsumieren ausschließlich die abgesicherte abgeleitete Grundmenge.
