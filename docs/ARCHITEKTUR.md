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

## Erweiterung

Ein neues mathematisches Konzept benötigt gewöhnlich:

1. ein CAS-Objekt oder eine vorhandene CAS-Repräsentation,
2. eine `KnotenVorlage`,
3. einen `MathematikKnotenAuswerter`,
4. optional einen spezielleren Renderer und eine Anschlussart.

Es werden keine fremden Module zur Laufzeit geladen. Erweiterbarkeit bedeutet eine stabile, modulare Registrierungs-API beim Build.
