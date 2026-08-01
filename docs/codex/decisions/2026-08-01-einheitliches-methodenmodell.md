# ADR: Einheitliches Methodenmodell

- Datum: 2026-08-01
- Status: angenommen
- Issues: #67, Grundlage für #64

## Kontext

Der Rechenkern modelliert methodenartige Objekte derzeit unter dem Namen `Funktion`. Gleichzeitig existieren mehrere Anschlussarten für allgemeine, zahlen-, aussagen-, mengen- und vektorwertige Funktionen. Diese Anschlussarten vermischen Graphkompatibilität mit mathematischer Klassifikation und erschweren Kartenmethoden mit dynamischen Signaturen.

Fachlich soll `Methode` der einzige allgemeine Begriff sein. `Funktion`, `Abbildung` und `Prädikat` sind aus Wertevorrat, Zielmenge und Vorschrift abgeleitete Bezeichnungen für Nutzer, keine getrennten Laufzeitklassen und keine getrennten produktiven Anschlussarten.

## Entscheidung

1. `Methode` ist der kanonische Fachname des bestehenden einzigen Laufzeittyps. Ein Typealias erhält vorerst die Quellkompatibilität zu `Funktion`; es entsteht keine zweite Klasse.
2. Eine Methode besitzt eine geordnete Parameterliste. Der Wertevorrat ist der Tupelraum der argumentweisen Wertevorräte. Bei null Argumenten gilt im Atlas `Wertevorrat = ∅`.
3. Eine Methode liefert fachlich genau ein Ergebnis in genau einer Zielmenge. Historische benannte Mehrfachausgaben werden in stabiler Reihenfolge als ein Tupel und eine Tupelzielmenge interpretiert. Kartenmethoden erzeugen dieses Tupel bereits beim Export.
4. Für jedes Element des deklarierten Wertevorrats muss die Anwendung ein Ergebnis liefern. Eine unbekannte oder unentscheidbare Aussage ist ein Ergebnis und kein Auswertungsfehler.
5. `Funktion`, `Abbildung` und `Prädikat` werden zentral und schreibgeschützt aus der Methodensemantik berechnet.
6. `mathematik.methode` ist die einzige produktive Methodenanschlussart. Die bisherigen `mathematik.funktion.*`-IDs bleiben nur zum Laden registriert und werden idempotent unter Erhalt aller Instanz- und Verbindungs-IDs migriert.
7. Einschränkungen bestimmter Knoten werden über `MethodenAnforderung` am konkreten Methodenobjekt geprüft, nicht über neue Anschlussunterarten.
8. Die kanonische Wahrheitsmenge liegt einmal im Rechenkern. Entscheidbarkeit ist kein Klassifikationskriterium für Prädikate.

## Aliasregeln

- **Funktion:** Alle Argument- und Ergebnisräume gehören zu Zahlen, Vektoren, Matrizen oder Tensoren. Gemischte Signaturen innerhalb dieser Familien sind zulässig.
- **Abbildung:** Die Argumentobjekte sind Mengen und das Ergebnisobjekt ist eine Menge.
- **Prädikat:** Die Vorschrift ist eine Aussage und die Zielmenge ist exakt `{Wahr, Lüge}`.

Mehrere Aliase dürfen gleichzeitig zutreffen. Die UI zeigt stets `Methode` und ergänzt die berechneten Aliase.

## Konsequenzen

- Neue Knoten und Karten verwenden nur `mathematik.methode`.
- Alte Karten bleiben ohne Verlust ladbar; ihre Anschlüsse werden normalisiert.
- Bestehender Quellcode kann schrittweise von `Funktion` auf `Methode` umgestellt werden, ohne ein paralleles Modell zu erzeugen.
- Statische Anschlusskompatibilität wird gröber. Präzise mathematische Anforderungen müssen deshalb im zuständigen Auswerter geprüft und als fachlicher Fehler ausgegeben werden.
- Issue #64 baut auf dieser Entscheidung auf und ergänzt Prädikatsauflösung, Darstellung, Namensprüfung und Wahrheitstabellen.

## Verworfene Alternativen

- Eigene Klassen für Funktion, Abbildung und Prädikat: erzeugen redundanten Zustand und fehleranfällige Konvertierungen.
- Dauerhafte spezialisierte Methodenanschlüsse: bilden dynamische Signaturen nur unvollständig ab.
- Mehrere unabhängige Rückgabewerte: widersprechen der festgelegten einen Zielmenge; Tupel modellieren dasselbe strukturiert.
- Entscheidbarkeit als Prädikatsmerkmal: würde gültige symbolische und unentscheidbare Aussagen fälschlich als Fehler behandeln.
