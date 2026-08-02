# Beispielkarten und erste Erkundung

Die App legt beim ersten Start automatisch Beispielkarten an, **wenn der interne Kartenspeicher noch leer ist**. Bereits vorhandene oder importierte Karten werden dabei nicht überschrieben.

Die Karten sind einfache technische Einstiegspunkte. Sie ersetzen keine vollständigen mathematischen Lernmaterialien und bilden nicht den gesamten Funktionsumfang ab.

## Doppeln

**Aufbau:**

```text
Karteneingang x → Addition x + x → Kartenausgang doppelt
```

Die Karte zeigt öffentliche Kartenschnittstellen. Sie kann in anderen Karten als versionierter Gruppenknoten verwendet werden.

## Rechnen

**Aufbau:**

```text
2 ─┐
   ├→ Addition → Auswerten → Gruppenknoten „Doppeln“
3 ─┘
```

Die Karte kombiniert gewöhnliche Rechenknoten mit einer wiederverwendeten Karte. Aus `2 + 3` entsteht zunächst `5`; die Karte „Doppeln“ verarbeitet dieses Ergebnis weiter.

## Aussage

**Aufbau:**

```text
Variable x ─┐
            ├→ Gleichheit → Auswerten
Zahl 0 ─────┘
```

Die Karte demonstriert eine symbolische Aussage. Solange die Variable nicht konkret belegt ist, darf die Auswertung einen symbolischen oder unentscheidbaren Zustand behalten, statt einen Wahrheitswert zu erfinden.

## Mengen

**Aufbau:**

```text
{1,2,3} ─┐
          ├→ Vereinigung
{3,4,5} ─┘
```

Die erwartete Vereinigungsmenge ist `{1,2,3,4,5}`. Die Karte eignet sich zum Erkunden endlicher Mengen, typisierter Mengenausgänge und Mengenoperatoren.

## Zahl und Menge verbinden

Die Karte verbindet eine Zahl und eine endliche Menge mit einer allgemeinen Gleichheitsrelation. Sie dient vor allem zur Prüfung der typisierten Verbindungspfade zwischen allgemeinen mathematischen Objekten. Sie ist keine Empfehlung, fachlich unpassende Objekte wahllos zu vergleichen, obwohl Menschen dieses Verfahren außerhalb der Mathematik erstaunlich häufig anwenden.

## Weitere Erkundung

- Der Bereich **Konzepte** enthält vorhandene Definitions- und Erklärungskarten.
- Eine Definitionskarte kann als bearbeitbare Kopie geöffnet werden.
- Karten mit öffentlichen Ein- und Ausgängen können in anderen Karten als Gruppenknoten verwendet werden.
- Der Inspector zeigt konfigurierbare Parameter und Anschlussinformationen des ausgewählten Knotens.
- Fachliche Fehler werden an den betroffenen Knoten beziehungsweise Auswertungsergebnissen sichtbar.

Der tatsächliche Bestand an Konzeptkarten wächst mit der Entwicklung. Aussagen über einzelne Karten sollten deshalb immer gegen die aktuelle Anwendung geprüft werden.
