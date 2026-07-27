# Architektur

## Abhängigkeitsrichtung

```text
app
├── MathematikKnoten
├── MathematikKartenAdapter
├── KnotenKartenVerwalter
└── MathematikRechenSystem

MathematikKnoten
├── MathematikKartenAdapter
├── KnotenKartenVerwalter
└── MathematikRechenSystem

MathematikKartenAdapter
├── KnotenKartenVerwalter
└── MathematikRechenSystem
```

Verboten sind insbesondere Abhängigkeiten des `MathematikRechenSystem` auf Android, Compose oder den Karteneditor sowie jede Mathematikabhängigkeit des `KnotenKartenVerwalter`.

## KnotenKartenVerwalter

Das persistierbare Modell verwendet ausschließlich eigene Typen wie `GraphPunkt`, `GraphGröße`, `KnotenDaten`, `AnschlussDaten` und `VerbindungDaten`. Compose-Typen treten erst in `schnittstelle` auf.

Anschlüsse besitzen:

- eine Richtung: `Neutral`, `Eingang` oder `Ausgang`,
- eine Knotenkante,
- eine hierarchische `AnschlussArt`,
- eine stabile ID und Reihenfolge.

Ein Eingang akzeptiert höchstens eine eingehende Verbindung. Der `GraphPrüfung` verhindert inkompatible Typen, gleichgerichtete Verbindungen und Zyklen. Neutrale Anschlüsse sind im allgemeinen Editor verfügbar, werden von den mitgelieferten Mathematikknoten jedoch nicht verwendet.

Der Editorzustand hält nur eine unveränderliche `KartenDaten`-Instanz. Aktionen erzeugen einen neuen Stand. Drag-Interaktionen werden zu einem Undo-Schritt zusammengefasst.

`KnotenDaten.eigenschaften` ergänzt die ältere String-Map `parameter` für typisierte Konfigurationen. `KnotenEigenschaft` kennt ausschließlich primitive, rekursiv zusammensetzbare Werte; insbesondere sind Compose-Typen ausgeschlossen. Bestehende Knoten verwenden `parameter` weiter, während Kamera, Achsen und Farben der Visualisierung über die Eigenschaftsmap und atomare Kartenaktionen gespeichert werden.

## MathematikRechenSystem

Der CAS-Kern verwendet unveränderliche Objekte. Assoziative Operationen sind variadisch:

```kotlin
Addition(listOf(a, b, c, d))
```

Fabriken normalisieren verschachtelte Additionen und Multiplikationen. Exakte rationale Zahlen bleiben exakt; Näherungen sind eine ausdrückliche Operation. Aussagen trennen den Wahrheitswert vom Entscheidungsstatus, einschließlich `Unentscheidbar`.

Öffentliche Umformungen liefern strukturierte `UmformungsSchritt`-Objekte. Bedingungen werden in einem `RechenKontext` weitergegeben. Fallknoten machen solche Bedingungen im Graph sichtbar, statt sie stillschweigend zu verschlucken.

## Adapter und Auswertung

`KartenAuswerter` verarbeitet einen azyklischen Graph topologisch. Jeder Knotenauswerter wird über ein Register anhand der stabilen Knotenart gefunden. Der Cache verwendet eine Signatur aus Knoteninhalt und Eingängen; unveränderte Teilgraphen werden wiederverwendet.

Ein Gruppenknoten enthält lediglich einen generischen `KartenVerweis`. Erst der Adapter interpretiert die referenzierte Karte mathematisch. Nicht gebundene Karten-Eingänge werden zu freien Funktionsparametern; teilweise gebundene Gruppen liefern eine teilweise gebundene Funktion.

`DefinierteMenge` beschreibt Mengen mit gebundenen Variablen, Grundmengen und einer `Aussage`. `freieVariablen()` entfernt gebundene Namen zentral rekursiv; die Substitution von Aussagen bleibt strukturerhaltend. Der Knoten „Lineare Gleichung lösen“ bleibt ein spezieller linearer Löser, während „Lösungsmenge“ eine Aussage ohne Lösungsalgorithmus als symbolische `DefinierteMenge` repräsentiert.

## Erweiterung

Ein neues mathematisches Konzept benötigt gewöhnlich:

1. ein CAS-Objekt oder eine vorhandene CAS-Repräsentation,
2. eine `KnotenVorlage`,
3. einen `MathematikKnotenAuswerter`,
4. optional einen spezielleren Renderer und eine Anschlussart.

Es werden keine fremden Module zur Laufzeit geladen. Erweiterbarkeit bedeutet eine stabile, modulare Registrierungs-API beim Build.

## Visualisierung

Die Visualisierung liegt zunächst abgegrenzt unter `MathematikKnoten/visualisierung`, weil dieses Modul bereits die zulässige Compose-Abhängigkeit besitzt und nicht auf `app` angewiesen ist. Sampling und numerische Aussageauswertung sind plattformneutrale Unterpakete; der Compose-Renderer startet sie entprellt auf `Dispatchers.Default` und persistiert nur Konfiguration, niemals Raster oder Punktwolken.

Der Visualisierungsknoten ist fachlich ein Durchreicher und verändert die eingehende Menge nicht. R² verwendet Vorzeichenwechsel für implizite Gleichheiten und Rasterpunkte für Ungleichungen. R³ verwendet derzeit eine toleranzbasierte Oberflächen- beziehungsweise Bereichspunktwolke mit einfacher orthografischer Projektion; ein späteres Marching-Cubes-Verfahren kann denselben Samplervertrag ersetzen. Renderer deklarieren ihren Interaktionsmodus: Visualisierungen sind nur an der Kopfzeile ziehbar, sodass Plotgesten und Knöpfe keine Knotenverschiebung auslösen.
