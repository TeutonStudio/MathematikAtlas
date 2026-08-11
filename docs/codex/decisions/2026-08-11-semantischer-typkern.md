# ADR: Semantischer Typkern neben Anschlussarten

**Datum:** 2026-08-11  
**Status:** angenommen

## Kontext

`AnschlussDaten.art` erfüllte bisher gleichzeitig mehrere Aufgaben: sichtbare Anschlusskategorie, Farbauswahl, Unterartprüfung, dynamische Artinferenz und indirekt die fachliche Beschreibung des transportierten Werts. Das genügt für grobe Kategorien wie `mathematik.zahl` oder `mathematik.methode`, kann aber parametrisierte Methodensignaturen, Tupelkomponenten, Dimensionen, echte Oder-Typen und zusätzliche Struktur-/Axiomanforderungen nicht verlustfrei ausdrücken.

Die geplante Godot-Erweiterung benötigt dieselbe fachneutrale Graphinfrastruktur für weitere Typfamilien wie `Script`, `ScriptMethod`, `Node` und Godot-Klassen. Ein zweites paralleles Typ- und Verbindungssystem wäre deshalb eine dauerhafte Architekturverschuldung.

## Entscheidung

Der Karteneditor erhält einen fachneutralen semantischen Typkern. `AnschlussArt` bleibt als grobe, rückwärtskompatible Anschlusskategorie bestehen; der konkrete Wertvertrag liegt zusätzlich in `AnschlussDaten.vertrag`.

Der Vertrag besteht aus:

- einem `TypAusdruck`,
- null oder mehr `TypAnforderung`-Einträgen.

`TypAusdruck` unterstützt:

- `Beliebig`,
- `Unbekannt`,
- atomare Typen,
- parametrisierte Typen,
- Vereinigungs-/Oder-Typen,
- Typvariablen.

`Beliebig` und `Unbekannt` sind semantisch verschieden. Ein unbekannter Typ darf nicht dadurch automatisch an jeden spezifischen Zieltyp anschließbar werden.

Der neutrale `TypRegister` unterstützt mehrere Elternbeziehungen und parametrisierte Konstruktoren mit Varianz. `StandardTypSystem` entscheidet kompatibel, inkompatibel oder unbestimmt. `GraphPrüfung` behält während der Migration die bestehende `AnschlussArt`-Prüfung und führt danach zusätzlich die semantische Typprüfung aus.

Mathematische Typbeziehungen liegen nicht im fachneutralen Editor. `MathematikKnoten` registriert mathematische Typen und leitet sie aus den bereits vorhandenen `MengenAusdruck`- und `MethodenSignatur`-Modellen ab. Bestehende Karten und Katalogvorlagen werden konservativ aus den alten Anschlussverträgen angereichert; präzisere bereits vorhandene Typangaben werden nicht überschrieben.

Struktur-, Eigenschafts- und Axiomanforderungen werden als Anforderungen transportiert, nicht als künstliche Typunterklassen. Ihre fachliche Auswertung gehört in G0.3 und höhere Domänen.

Die Darstellung bleibt von der Semantik getrennt. `TypVisualDescriptor` und `TypVisualAuflöser` erlauben Orchestrator-artige kompakte Typmarken; mathematische Kurznotation wird in `MathematikKnoten` aufgelöst.

## Methodentypen

Ein Methodentyp wird als parametrisierter Typ mit zwei Argumenten dargestellt:

1. struktureller Argumenttyp,
2. Zieltyp.

Die im Atlas etablierte Tupelkonvention bleibt erhalten: Auch eine einstellige Methode verwendet strukturell ein Einertupel als Argumenttyp. Der Methodenkonstruktor ist im Argument kontravariant und im Ergebnis kovariant.

## Persistenz

Der fachneutrale Karten-JSON-Codec wird auf Formatversion 8 angehoben und serialisiert `AnschlussVertrag` sowie optionale `TypInferenzRegel`. Fehlende Felder aus Formatversion 7 und älter werden mit neutralen Defaults gelesen. Die mathematische Codec-Schicht ergänzt anschließend konservative mathematische Standardverträge.

Die `.matlas`-Containerformatversion bleibt davon unabhängig und unverändert.

## Alternativen

### `AnschlussArt` vollständig ersetzen

Verworfen. Dies würde bestehende Karten, dynamische Anschlussregeln, Farblogik und zahlreiche Vorlagen unnötig in einem einzigen Schritt migrieren.

### Weitere mathematische Anschlussarten anlegen

Verworfen. Typen wie `Methode<ℝ²,ℝ>` oder `Tupel<ℝ,ℂ>` bilden keinen sinnvollen einfachen Anschlussartenbaum und würden die bereits erfolgte Methodenkonsolidierung rückgängig machen.

### Typsemantik direkt in `MathematikRechenSystem` an den Graph koppeln

Verworfen. Der Rechenkern darf keine Karteneditor-Abhängigkeit erhalten. Die Übersetzung liegt deshalb in `MathematikKnoten`.

### Godot erhält später ein eigenes Typsystem

Verworfen. Damit entstünden zwei konkurrierende Kompatibilitäts- und Visualisierungsmechanismen im selben Graphen.

## Konsequenzen

- Bestehende Karten bleiben lesbar und behalten ihre Anschluss-IDs.
- Neu erzeugte mathematische Vorlagen besitzen sofort semantische Standardverträge.
- Präzise mathematische Typen können schrittweise pro Knoten oder Resolver ergänzt werden.
- Godot kann später zusätzliche neutrale Typ-IDs und Konstruktoren registrieren, ohne `GraphPrüfung` oder das Persistenzgrundmodell erneut zu ersetzen.
- G0.3 kann Topologien, Metriken und algebraische Strukturen über `TypAnforderung` auswerten, ohne sie als starre Untertypen zu missbrauchen.
