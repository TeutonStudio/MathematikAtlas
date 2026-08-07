# Standardkarten

Die produktive Standardkartensammlung wird als deklaratives Paket unter

```text
app/src/main/assets/de/TeutonStudio/MathematikAtlas/standardkarten/
```

ausgeliefert.

## Vertrag

- `manifest.json` enthält stabile `sourceId`s, Quell-Hashes, Standardordner, Abhängigkeiten und benötigte Knotentypen.
- Jede Karte liegt als normales `KartenDatenJson` im aktuellen Kartenformat vor. Es existiert kein zweiter Graph-Codec.
- Beim ersten Installieren erhält jede Quellkarte eine lokale Karten-ID und wird danach wie gewöhnlicher Nutzerinhalt behandelt.
- Nutzeränderungen werden anhand des zuletzt installierten normalisierten Inhalts erkannt und durch Paketupdates nicht überschrieben.
- Gelöschte Standardkarten werden in der Provenienz als gelöscht geführt und nicht beim nächsten Start neu erzeugt.
- Der im Manifest angegebene Ordner ist nur die Erstplatzierung. Nutzerbewegungen werden nicht zurückgesetzt.
- Aufgabenkarten referenzieren wiederverwendbare Funktionskarten über normale Karten-Knoten.
- Jede ausgelieferte Karte besitzt mindestens einen anschlusslosen `karte.notiz`-Knoten mit einem statischen `Erwartetes Ergebnis`.
- Eine Karte wird nur ausgeliefert, wenn alle benötigten nativen Knotentypen vorhanden sind. Fehlende Mathematik wird nicht durch vorberechnete Ergebnisattrappen ersetzt.

## Prüfung

```bash
python3 scripts/pruefe_standardkarten.py
./gradlew test :app:assembleDebug
```

Die statische Prüfung kontrolliert Paketpfade, IDs, Handles, Edges, Abhängigkeiten, Quell-Hashes, Methodenausgänge der Funktionskarten und Soll-Ergebnis-Notizen. Zusätzlich führt `StandardKartenAuswertungTest` die produktiven Karten durch den echten Mathematik-Auswerter.

## Herkunft

Die erste produktive Fassung wurde ausgehend von `v2.27.0` aufgebaut und setzt die Lebenszyklus- und Inhaltsregeln aus #332 sowie die bereits implementierten Knotenkontrakte aus #333 und #334 voraus.
