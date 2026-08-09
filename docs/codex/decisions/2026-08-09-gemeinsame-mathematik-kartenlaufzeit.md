# ADR: Gemeinsame Mathematik-Kartenlaufzeit

## Status

Angenommen als dritte Architekturphase des noch unveröffentlichten `v2.32.1`-Refactors.

## Kontext

Android und Desktop verwendeten bereits dieselben mathematischen Module, setzten ihre Laufzeit jedoch getrennt zusammen. Beide erzeugten Anschlussartenregister, Graphprüfung, Gesamtauswerter und Cache unabhängig voneinander. Nach Zentralisierung von Knotenkatalog und Kartenmigration blieb diese Verdrahtung die nächste unnötige Plattformdopplung.

## Entscheidung

`MathematikKartenLaufzeit` in `MathematikKnoten` wird die kanonische Zusammensetzung für:

- `AnschlussArtRegister(MathematikAnschlussArten.alle)`,
- `GraphPrüfung`,
- `KartenAuswerter` mit `GesamterMathematikAuswerter`,
- kanonischen Mathematikknoten-Katalog,
- Auswertungs-Cachezugriffe.

Android- und Desktop-Zustände verwenden diese Laufzeit, behalten aber ihre plattformspezifische Navigation, Persistenz, Meldungen und UI-Orchestrierung.

## Gründe

Die gemeinsame Laufzeit verhindert Plattformdrift, ohne die App-Zustände künstlich zusammenzulegen. Insbesondere bleibt die zusätzliche Android-Synchronisierung dynamischer Restriktions- und Methodenanschlüsse dort, wo ihr Verhalten bereits etabliert ist.

## Konsequenzen

Neue mathematische Auswerter-, Anschlussarten- oder Katalogkonfigurationen werden in der gemeinsamen Mathematikschicht ergänzt. Plattformzustände dürfen keinen zweiten `KartenAuswerter` oder ein separates mathematisches Anschlussregister aufbauen.

## Nicht entschieden

Ein vollständig gemeinsamer Android-/Desktop-Workspace inklusive UI-Zustand sowie die physische Ablösung der Desktop-Shadowmodule bleiben getrennte Vorhaben. Dafür ist eine kompatible gemeinsame Plattform-Toolchain Voraussetzung.
