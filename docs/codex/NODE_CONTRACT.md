# Vertrag für Knotentypen

Jeder neue oder wesentlich geänderte mathematische Knotentyp muss die folgenden Punkte ausdrücklich definieren.

## 1. Identität

- stabiler Art- oder Registry-Schlüssel,
- menschenlesbarer Name,
- Kategorie im Vorlagenkatalog,
- Daten- oder Kartenformatänderung, falls persistiert,
- eindeutige `KnotenId` pro Instanz.

Der Art-Schlüssel darf nicht aus einem übersetzten UI-Label abgeleitet werden.

## 2. Fachliche Bedeutung

Beschreibe:

- welches mathematische Objekt oder welchen Vorgang der Knoten repräsentiert,
- welche Voraussetzungen gelten,
- welches Ergebnis erzeugt wird,
- welche Fälle undefiniert, unbekannt oder ungültig sind,
- ob der Knoten symbolisch, exakt, näherungsweise, visuell oder kombiniert arbeitet.

## 3. Knotendaten

Trenne mindestens:

- persistierte Konfiguration in `parameter` oder `eigenschaften`,
- persistierte Anschlüsse und Kartenverweise,
- abgeleitete Auswertungsergebnisse,
- kurzlebigen Compose- und Interaktionszustand.

Persistierte Daten müssen durch `KartenJson` darstellbar, validierbar und rückwärtskompatibel lesbar sein. Laufzeitobjekte gehören nicht in `KnotenDaten`.

## 4. Anschlüsse

Für jeden Anschluss:

| Feld | Bedeutung |
|---|---|
| Instanz-ID | stabile `AnschlussId`, auf die Verbindungen verweisen |
| Name | fachlicher Schlüssel für Auswerter und Eingabesammlung |
| Richtung | `Eingang`, `Ausgang` oder bewusst `Neutral` |
| Kante | Platzierung am Knoten |
| Anschlussart | `AnschlussArtId` und Hierarchie für Kompatibilität |
| Kardinalität | genau eins, optional, mehrfach oder dynamisch erweiterbar |
| Reihenfolge | fachlich relevant oder nur visuell |
| Erweiterbarkeit | ob weitere Anschlüsse aus diesem Vertrag entstehen dürfen |
| Typabhängigkeit | gegebenenfalls `artFolgtEingang` |
| Fehlermodus | Verhalten bei fehlender oder inkompatibler Eingabe |

Anschluss-IDs dürfen nicht aus Position, Übersetzung oder sichtbarem Label abgeleitet werden. Strukturänderungen müssen bestehende IDs und damit vorhandene Verbindungen erhalten, soweit die fachliche Bedeutung bestehen bleibt.

## 5. Verbindungsverhalten

Definiere:

- wann eine Verbindung nach Richtung und Anschlussarthierarchie zulässig ist,
- wie belegte Eingänge behandelt werden,
- ob und wie dynamische Anschlüsse entstehen,
- ob die Änderung mit dem azyklischen Kartenmodell vereinbar ist,
- wie Änderungen propagiert werden,
- wie inkompatible gespeicherte Verbindungen behandelt werden,
- welches Undo/Redo-Verhalten erwartet wird.

## 6. Auswertung

Definiere:

- Eingabewerte oder Eingabeausdrücke nach Anschlussnamen,
- fachliches Ergebnis,
- Auswertungszeitpunkt,
- Verhalten des Ergebnis-Caches,
- deterministische Reihenfolge,
- Fehler- und Entscheidungszustände,
- Verhalten bei partiellen Eingaben,
- Verhalten innerhalb eines Gruppenknotens.

Die mathematische Semantik liegt im `MathematikRechenSystem` oder im zuständigen `MathematikKnotenAuswerter`, nicht im Compose-Renderer.

## 7. Darstellung

Definiere:

- Titel und kompakte Bedeutung,
- vom fachlichen Objekt erzeugte LaTeX- oder Textdarstellung,
- nativen Compose-Renderer und dessen unterstützten Formelteilumfang,
- Anschlüsse und ihre Platzierung,
- Fehler- und Ladeanzeige,
- Auswahlzustand,
- minimale und optionale Größe,
- `KnotenInteraktionsModus`,
- Interaktionen innerhalb des Knotens.

Die Darstellung darf keine zweite fachliche Semantik pflegen. Eine Rendervereinfachung muss als Darstellung erkennbar bleiben und darf das Auswertungsergebnis nicht verändern.

## 8. Inspector

Definiere:

- editierbare Felder,
- verwendeten Datenträger (`parameter` oder typisierte `eigenschaften`),
- Datentyp und Validierung,
- Standardwerte,
- unmittelbare oder bestätigte Übernahme,
- verwendete `KartenAktion`,
- Rückwirkung auf Anschlüsse, Auswertung und Persistenz,
- Verhalten bei ungültiger Eingabe.

Der Inspector schreibt keinen unabhängigen Schattenzustand, der von `KnotenDaten` abweichen kann.

## 9. Registrierung und Erzeugung

Definiere:

- Eintrag oder Erweiterung in `MathematikKnotenVorlagen.alle`,
- Auswerterregistrierung im vorhandenen `MathematikAuswerterRegister`,
- gegebenenfalls neue Anschlussart im vorhandenen Register,
- Renderer-Zuordnung im bestehenden Pfad,
- Kategorie und Suchverhalten in der Knotenauswahl.

Kein Knotentyp führt ein paralleles Register ein.

## 10. Persistenz und Migration

Definiere:

- gespeicherte Parameter und Eigenschaften,
- Defaultwerte für ältere Daten,
- Migration bei Anschluss- oder Strukturänderung,
- Verhalten bei unbekanntem Knotentyp,
- Kopier- und Duplizierverhalten,
- stabile Anschlussreferenzen nach Laden,
- Auswirkungen auf `formatVersion`, sofern tatsächlich erforderlich.

## 11. Tests

Mindestens prüfen:

- Erzeugung mit Standarddaten,
- fachlich gültige Auswertung,
- fehlende und inkompatible Eingaben,
- mathematische Grenzfälle,
- stabile Anschluss-IDs und Reihenfolge,
- Vorlagen- und Auswerterregistrierung,
- Inspector- oder Kartenaktionsänderung,
- JSON-Roundtrip, sofern Persistenz besteht,
- Migration, sofern ein Schema geändert wurde,
- Rendering oder Interaktion, sofern die bestehende Compose-Testumgebung dies unterstützt.

## 12. Abnahmekriterien

Abnahmekriterien sind beobachtbar und binär formuliert. Beispiele:

- „Ein Ausgang der Anschlussart `mathematik.menge` kann mit dem Eingang `indexmenge` verbunden werden.“
- „Nach Speichern und Laden bleiben Knotenart, Konfiguration und Anschlussreferenzen erhalten.“
- „Eine leere Indexmenge liefert das fachlich definierte neutrale Element oder einen expliziten undefinierten Zustand.“
- „Eine Inspectoränderung erzeugt genau einen Undo-Schritt.“

Nicht ausreichend:

- „Der Knoten funktioniert.“
- „Die UI sieht ordentlich aus.“