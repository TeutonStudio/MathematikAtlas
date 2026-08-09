# ADR: Kanonischer Knotenkatalog und explizite Registrierungsphasen

## Status

Angenommen am 2026-08-09 für `v2.32.1`.

## Kontext

Der sichtbare Mathematikknoten-Katalog wurde auf Android zuletzt app-seitig nachbearbeitet, insbesondere für die Vektorkonsolidierung ab v2.30.0. Desktop bezog dagegen direkt den Basiskatalog aus `MathematikKnoten`. Gleichzeitig orchestrierte `GesamterMathematikAuswerter` eine lange, reihenfolgeabhängige Folge fachlich heterogener Registrierungsfunktionen aus einer Datei unter dem Geometrie-Verzeichnis.

Damit bestanden zwei Risiken:

1. Android und Desktop konnten unterschiedliche erzeugbare Knotenmengen sehen.
2. Die semantisch relevante Reihenfolge von Basisregistrierungen und späteren Verfeinerungen war nur implizit durch die Reihenfolge eines großen Blocks definiert.

## Entscheidung

1. `MathematikKnoten` besitzt einen kanonischen sichtbaren Katalog als einzige fachliche Quelle für erzeugbare mathematische Knoten.
2. Plattformmodule dürfen diesen Katalog filtern oder um echte Plattformwerkzeuge ergänzen, aber keine zweite mathematische Ersetzungslogik pflegen.
3. Historische lade-kompatible Vorlagen bleiben getrennt von der sichtbaren Erzeugung erhalten.
4. Auswerterregistrierungen werden als benannte, geordnete Pakete beziehungsweise Phasen modelliert. Nachgelagerte Wrapper und Kompatibilitätsverfeinerungen bleiben explizit am Ende der Reihenfolge.
5. Versionsspezifische Implementierungsnamen dürfen für Binär-/Quellkompatibilität bestehen bleiben, werden aber hinter kanonischen Fassaden versteckt und nicht in neuen App-Pfaden verwendet.

## Alternativen

### Plattformkataloge getrennt lassen

Verworfen, weil dieselbe mathematische Domäne auf Android und Desktop nicht unterschiedliche sichtbare Knotenquellen besitzen sollte.

### Alle Registrierungen sofort auf ein vollständig deklaratives Pluginmodell umstellen

Vertagt. Die aktuelle Registry enthält bewusst ersetzende Wrapper. Eine vollständige Umstellung ohne vorbereitende Trennung würde Semantik und Refactor unnötig vermischen.

### Gleichzeitig Kotlin Multiplatform einführen

Vertagt. Die aktuelle Kotlin-/AGP-Kombination ist für den KMP-Android-Library-Weg nicht freigegeben; ein Toolchain-Downgrade wird nicht als Nebenwirkung dieses Refactors akzeptiert.

## Konsequenzen

- Android und Desktop verwenden denselben mathematischen Erstellen-Katalog.
- Neue Knotenkonsolidierungen werden in `MathematikKnoten` umgesetzt und nicht in `app` nachgebessert.
- Die Registrierungsreihenfolge wird testbar und fachlich benannt.
- Die Desktop-Shadowmodule bleiben vorerst technische Schuld, sind aber nicht länger Quelle unterschiedlicher Knotenkataloge.
- Ein späterer KMP-/Shared-UI-Umbau kann auf einer bereits zentralisierten fachlichen Architektur aufsetzen.
