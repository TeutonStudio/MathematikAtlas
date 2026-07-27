# ADR: Allgemeine Funktionsparameter für Abbildungen

## Entscheidung

`Funktion` verwendet `FunktionsParameter` als gemeinsamen Parametertyp. Die bestehende `Variable` bleibt ein numerischer `FunktionsParameter`; `AllgemeinerParameter` modelliert einen Parameter für beliebige mathematische Objekte.

## Begründung

Das Bild einer Menge unter einer Funktion ist nicht auf Zahlen beschränkt. Eine bloße Erweiterung des Graph-Handles hätte unzulässige Methoden erst zur Auswertung scheitern lassen. Der gemeinsame Parametertyp ermöglicht allgemeine, einwertige Abbildungen und isoliert zugleich die weiterhin numerischen Operationen.

## Konsequenzen

- `mathematik.abbild.methode` hat den allgemeinen Typ `Funktion`.
- Eine endliche Bildmenge wird durch Bindung jedes `MathematischesObjekt`-Elements berechnet.
- Iteration, Matrixerzeugung, Komposition und Methodenanalysis fordern weiterhin explizit numerische `Variable`-Parameter und Zahl-Ausgaben, soweit fachlich erforderlich.
- Der Editor bietet `mathematik.allgemeinerParameter` mit einer der vorhandenen Grundmengen `N`, `Z`, `Q`, `R`, `C` an.
