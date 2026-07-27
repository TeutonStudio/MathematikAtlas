# Vertrag für Node-Typen

Jeder neue oder wesentlich geänderte Node muss die folgenden Punkte ausdrücklich definieren.

## 1. Identität

- stabiler Node-Typ oder Registry-Schlüssel,
- menschenlesbarer Name,
- Kategorie,
- Daten-Schemaversion, falls persistiert,
- eindeutige Node-ID pro Instanz.

Der Typ-Schlüssel darf nicht aus einem übersetzten UI-Label abgeleitet werden.

## 2. Fachliche Bedeutung

Beschreibe:

- welches mathematische Objekt oder welchen Vorgang der Node repräsentiert,
- welche Voraussetzungen gelten,
- welches Ergebnis erzeugt wird,
- welche Fälle undefiniert oder ungültig sind,
- ob der Node rein symbolisch, numerisch, visuell oder kombiniert arbeitet.

## 3. Node-Daten

Trenne mindestens:

- persistierte Konfiguration,
- Verweise auf Eingaben,
- abgeleitete Ergebnisse,
- UI-spezifischen Laufzeitzustand.

Persistierte Daten müssen serialisierbar und validierbar sein.

## 4. Anschlüsse

Für jeden Handle:

| Feld | Bedeutung |
|---|---|
| stabile ID | unveränderlicher technischer Schlüssel innerhalb des Node-Typs |
| Richtung | Eingang oder Ausgang |
| fachlicher Typ | zum Beispiel Ausdruck, Zahl, Menge, Funktion, Relation oder Visualisierungsdaten |
| Kardinalität | genau eins, optional, mehrere oder variadisch |
| Ordnung | relevant oder irrelevant |
| Label | UI-Bezeichnung |
| Kompatibilität | erlaubte Quell- oder Zieltypen |
| Fehlermodus | Verhalten bei fehlender oder falscher Eingabe |

Handle-IDs dürfen nicht von Position, Übersetzung oder sichtbarem Label abhängen.

## 5. Edge-Verhalten

Definiere:

- wann eine Verbindung zulässig ist,
- ob mehrere Edges an einem Handle erlaubt sind,
- ob Zyklen zulässig sind,
- wie Änderungen propagiert werden,
- wie inkompatible gespeicherte Edges behandelt werden.

## 6. Auswertung

Definiere:

- Eingabewerte oder Eingabeausdrücke,
- Ergebnis,
- Auswertungszeitpunkt,
- Caching oder Memoisierung, falls relevant,
- deterministische Reihenfolge,
- Fehler- und Ladezustände,
- Verhalten bei partiellen Eingaben.

## 7. Darstellung

Definiere:

- Titel und kompakte Bedeutung,
- KaTeX-Ausgabe,
- Handles und ihre Platzierung,
- Fehleranzeige,
- Auswahlzustand,
- minimale und optionale Größe,
- Interaktion im Node selbst.

Die Node-Komponente darf keine zweite fachliche Semantik pflegen.

## 8. Inspector

Definiere:

- editierbare Felder,
- Datentyp und Validierung,
- Standardwerte,
- unmittelbare oder bestätigte Übernahme,
- Rückwirkung auf Handles, Auswertung und Persistenz,
- Verhalten bei ungültiger Eingabe.

## 9. Persistenz und Migration

Definiere:

- gespeichertes Schema,
- Defaultwerte für ältere Daten,
- Migration bei Strukturänderung,
- Verhalten bei unbekanntem Node-Typ,
- Kopier- und Duplizierverhalten,
- stabile Handle-Referenzen nach Laden.

## 10. Tests

Mindestens prüfen:

- Erzeugung mit Standarddaten,
- fachlich gültige Auswertung,
- fehlende und inkompatible Eingaben,
- mathematische Grenzfälle,
- stabile Handles,
- Registry-Zuordnung,
- Inspector-Änderung,
- Serialisierungs-Roundtrip, sofern Persistenz besteht,
- Migration, sofern Schema geändert wurde,
- Rendering eines relevanten Zustands, sofern die bestehende Testumgebung dies unterstützt.

## 11. Abnahmekriterien

Abnahmekriterien sind beobachtbar und binär formuliert. Beispiel:

- „Eine Edge vom Typ `Menge<T>` kann mit dem Handle `indexSet` verbunden werden.“
- „Nach Speichern und Laden bleiben Node-Typ, Konfiguration und Handle-Referenzen erhalten.“
- „Eine leere Indexmenge liefert das fachlich definierte neutrale Element oder einen expliziten undefinierten Zustand.“

Nicht ausreichend:

- „Der Node funktioniert.“
- „Die UI sieht ordentlich aus.“
