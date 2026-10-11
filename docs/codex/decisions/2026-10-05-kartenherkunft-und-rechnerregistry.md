# ADR: App-lokale Kartenherkunft und kanonische Rechneroperator-Registry

**Status:** angenommen
**Datum:** 2026-10-05

## Kontext

Die Kartenbibliothek konnte ausgelieferte, eigene und importierte Karten nicht
einheitlich filtern. Zugleich war der Vertrag vorhandener Zahlen-, Vektor-,
Matrix- und Tensoroperatoren auf mehrere Kataloge und Auswerter-Wrapper verteilt.

## Entscheidung

Kartenherkunft bleibt außerhalb von `KartenDaten`. Standardkartenprovenienz und
Freigabequellen bleiben kanonisch; `karten-herkunft.json` ergänzt ausschließlich
direkte Fremdimporte und deren Ordner. Importiert werden JSON, Freigabepakete und
`.matlas` durch denselben formatprüfenden Speicherpfad. Eine Journaltransaktion
sichert Karten, Sicherungen, Ordnung, Freigabequellen und Herkunft gemeinsam und
stellt einen nicht abgeschlossenen Import beim Start wieder her.

`RechnerOperatorRegistry` ist der gemeinsame Laufzeitvertrag der vier bestehenden
Rechnerfamilien. Sie beschreibt stabile Operator-ID, Familie, Signaturen und
Ports, Parameter, Fähigkeiten und Unterstützungsstatus. Ein abschließender
Dispatcher je Rechner-Knotenart validiert die ID und delegiert an den bereits
spezialisierten Fachauswerter. Unbekannte IDs bleiben persistiert und erzeugen
einen sichtbaren Fehlerzustand.

## Folgen

- Das neutrale Kartenformat und seine Formatversion bleiben unverändert.
- Standardkarten haben bei der Herkunft immer Vorrang; lokale Freigaben gelten
  als eigen, fremde Freigaben und direkte Datei-/Linkimporte als fremd.
- Neue Rechneroperatoren müssen in der Registry erscheinen; Suchindex und
  Dispatcher verwenden denselben Vertrag.
- Fachliche Mathematik bleibt in Rechenkern und Familienauswertern; die Registry
  wird nicht zu einem universellen Mathematikauswerter.
