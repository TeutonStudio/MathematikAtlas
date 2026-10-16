# Kanonischer Erstellen-Katalog und diagnostische Migrationsgrenzen

Datum: 2026-10-17. Status: M0 implementiert und lokal verifiziert; Release-CI ausstehend.

## Kontext

Android ergänzte den gemeinsamen mathematischen Katalog erneut um rohe Mengenraum-
und Geometrievorlagen. Dadurch fehlten sieben gültige Raumtypen auf Desktop und
acht historische Geometrierelationen wurden auf Android wieder erzeugbar.
Die bisherigen Enthaltensein-Tests erkannten diese Abweichung nicht.
Historische Prädikatmigration konnte überzählige Ports entfernen und ihre
Rollenänderungen nicht vollständig in Standards/Kartenfallbacks fortführen.

## Entscheidung

Alle erzeugbaren mathematischen Vorlagen stammen aus
KanonischerMathematikKnotenKatalog. Plattformen ergänzen nur eigene Werkzeuge und
Gruppen. Das Release-Manifest entspricht dem gemeinsamen mathematischen Katalog
zuzüglich ausdrücklich deklarierter Plattformtypen; Tests prüfen Gleichheit.
Geometrierelationen bleiben über vorkonfigurierte Prädikate suchbar.

Eindeutige Prädikatmigration bewahrt Nutzername, Port- und Verbindungsidentitäten,
Verträge und Reihenfolge und remappt Standards und Kartenfallbacks gemeinsam.
Zusätzliche/doppelte/mehrfach belegte oder inkompatibel typisierte Ports,
ungeklärte Typableitungen und kollidierende Quellen bleiben im Originalknoten
mit Migrationsdiagnose erhalten. Nachfolgende Normalisierung darf diese Daten
nicht bereinigen. Die Diagnose sperrt fachliche Auswertung. Rollen werden ausschließlich anhand
kanonischer Namen oder ausdrücklich bekannter Legacy-Aliase migriert;
Layoutpositionen begründen keine semantische Rolle.

Der Mathematikadapter bietet dafür eine generische registrierbare Auswertungssperre;
der Gesamtauswerter liefert die fachliche Diagnose. Sie gilt vor allen Graphdispatchzweigen einschließlich vorgegebener Ausgaben
und Kartenreferenzen sowie bei direktem Registeraufruf. Keine neue Parallelregistry.

## Alternativen

Erneutes plattformspezifisches Filtern verworfen: mehrere veränderbare Wahrheiten.
Stilles positionsbasiertes Kürzen verworfen: Daten- und Verbindungsverlust.
Historische Mathematik als Fallback verworfen: Nutzer verlangt explizite Ablehnung.

## Konsequenzen

M0 enthält 136 gemeinsame und 138 vollständige Androidtypen statt 129/146.
Kein neuer Typ-Schlüssel, Kartenformat10 bleibt. Alte Graphkarten sind von der
bewussten Ablehnung alter Mathematik-Wire-Versionen getrennt zu behandeln.
Originaldaten bleiben speicher-/exportierbar. Die bestehende Diagnoseeigenschaft
rechnerMigrationDiagnose wird für diese Konsolidierungsgrenze wiederverwendet.

## Nachweise

KatalogMigrationSicherheitTest prüft Erzeugung, alle acht geometrischen Alttypen,
Rollen-/Quellenremapping, stabile Verbindungen, Ablehnung und Codecphasen.
ReleaseKnotenKatalogTest, ReleaseAppKnotenKatalogTest und
ReleaseDesktopKnotenKatalogTest prüfen vollständige Katalogkonsistenz.
Befehle/Ergebnisse stehen im aktiven Gesamt-ExecPlan.
