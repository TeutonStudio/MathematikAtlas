# Registrierte Darstellung und erhaltene numerische Genauigkeit

Datum: 2026-10-17. Status: M6 in Umsetzung; Laufzeitfreigabe offen.

`mathematik.darstellungsoptimierung` heißt sichtbar LaTeX-Darstellung. Sein
Auswerter gehört zum bestehenden mathematischen Basisregister und verändert
lediglich `latexDarstellung`. Objekt, Bedingungen, Quellen, Bereiche,
Komponenten, gebundene Formel und Übersetzungszeugnisse werden durchgereicht.
Der Kartenadapter besitzt dafür keinen typabhängigen Sonderdispatch mehr.
Er berücksichtigt den vorhandenen Vertrag `bezeugteAusgaben`: ausdrücklich
weitergereichte Anzeige und Kontext bleiben erhalten. Neue Berechnungen führen
Zeugnisse als Herkunft fort und übernehmen keine veralteten Bereichs-/Formdaten.

Die vorhandene `ÜbersetzungsKontext.datenGenauigkeit` trägt auch Genauigkeit von
Quellen ohne aktives Übersetzungszeugnis. Numerische Annäherung wird konservativ
durch Folgerechnungen und Export erhalten. Dafür entsteht weder ein künstliches
Übersetzungszeugnis noch ein neues Wire-Feld. Ohne weitergereichten Kontext
bleibt nur ein Genauigkeitskontext mit `NUMERISCH_ANGENÄHERT` erhalten.

Die konfigurierte Richtungsquelle mit null Winkeln liefert exakt ±1. Bei
mindestens einem Winkel verwendet ihr bestehender Algorithmus Double-Trigonometrie
und Normierung; seine rational gespeicherten Resultate werden ausdrücklich als
numerisch angenähert gekennzeichnet und mit einem Hinweis angezeigt. Auch bei
Spezialwinkeln ist diese konservative Kennzeichnung zulässig. Der Kern verwendet
dieselbe bestehende Obergrenze von 64 Winkeln/65 Dimensionen wie Knoten und
Winkelraum; die isolierte alte Grenze von 31 Winkeln war inkonsistent.

Sichtbare Namen präzisieren unveränderte Verträge: `mengenNorm` berechnet das
Maß μ(A), `vektorraum` erzeugt eine Tensorträgermenge erster Stufe ohne
Vektorraumaxiome, `cauchy` bezeichnet die Nichtstandard-Cauchyprüfung.
Persistierte Typ-IDs, historische Konzept-/Variantenreferenzen und Nutzernamen
bleiben erhalten; alte Suchbegriffe bleiben auffindbar. Keine neuen Typen,
Produktionsabhängigkeiten oder Karten-/Wire-Versionen.

Die Abnahme prüft Registrierung, Metadatenerhalt, Export, Bereichs-/Maß- und
Metrikverträge, stabile Referenzen und Richtungsgrenzen. Ergebnisse werden im
aktiven Konsolidierungsplan dem Release-SHA zugeordnet.


Literal-Snapshots beziehen ihren Kontext aus den fortgeführten Metadaten.
Operations-ASTs führen numerische Annäherung rekursiv aus Kindmetadaten fort,
auch wenn kein Übersetzungszeugnis existiert. Wire und erneute Formelauswertung
prüfen denselben Genauigkeitsstatus. Bloßer Genauigkeitstransport liest bestehende
Metadaten und erfordert keine neu inferierte Methodensignatur.


Lokale finale Suite: 2244 Fälle, 0 Fehler, 1 vorhandener Skip, 3 min 47 s/277 Tasks.
Sieben neue Regressionen und unabhängiger Nachreview ohne Blocker. Geräte- und
Linux-RPM-Abnahme bleiben vor Veröffentlichung SHA-bezogen erforderlich.
