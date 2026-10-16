# Methodenquelle und typbewahrende Identität

Datum: 2026-10-15

Status: umgesetzt und fachlich sowie technisch geprüft; Android-Laufzeitgrenze im zugehörigen ExecPlan dokumentiert.

## Kontext

Die neue Methodenquelle soll analog zur Mengenquelle eingebaute und eigene Quellen anbieten. Der vorhandene neutrale Methode-Vertrag ist ein AtlasWert; mathematische Signaturen, Domänen und symbolische Auswertung sind zusätzliche Capabilities. Eine allgemein verwendbare Identität darf deshalb keine künstliche mathematische Universalmenge einführen.

## Entscheidung

Ein regulärer Quellknoten mathematik.methode wählt zwischen Identität, konfigurierter Knotenvorlage/Rechneroperator und versionsfester Karte. Er liefert genau einen Methodenwert, dessen eigene Signatur mehrere Argumente und Ergebnisse besitzen kann. Vorlagen und Operatoren stammen aus den bestehenden Katalogen und Registern. Konkrete Knoteninstanzen werden nicht referenziert.

Die allgemeine Identität hat ein Argument und ein Ergebnis derselben Typvariable. Eine explizite ID-basierte Aufruf-Capability liefert den ursprünglichen AtlasWert unverändert zurück. Der Adapter bewahrt seine BedingterWert-Metadaten und konkretisiert den Ergebnistyp anhand des Arguments. Mit einer tatsächlichen angeschlossenen Menge A wird die Identität auf A beschränkt; falsche Mitgliedschaft ist ein Fehler, unentscheidbare Mitgliedschaft eine Bedingung.

Typenbeschränkte Identität ist ebenfalls eine neutrale Methode; insbesondere bedeutet ein Zahlenanschluss keine feste reelle Domäne. Nachweislich inkompatible konkrete Argumenttypen werden im Kern zurückgewiesen. Mengenbeschränkte Varianten verwenden zum Elementtyp passende Parameter mit expliziten Bindungs-IDs, auch für Aussagen und Mengen; freie gleichnamige Symbole in A bleiben dadurch frei. Identitätsmetadaten werden nur bei tatsächlich unveränderter Identitätsvorschrift durchgereicht, nicht allein aufgrund erhaltener Komponenten-IDs einer abgeleiteten Methode.

Knotenquellen verwenden den vorhandenen Kartenmethodenexport über einen transienten Ein-Knoten-Graphen. Eignung setzt erfolgreiche symbolische Auswertung und gültige Signaturen voraus. Stabile Argument-/Ergebnisidentitäten kommen aus ursprünglichen Ports beziehungsweise Kartengrenzknoten. Transiente Exporte dürfen keine Cacheeinträge realer Karten verdrängen oder wiederverwenden.

Methodenstandards werden ausdrücklich im fachlichen Auswerter-/Operatorvertrag deklariert. Echte Verbindungen haben Vorrang vor konfigurierten Kartenmethoden; erst danach greift Identität bei explizit optionalen Eingängen. Fehler einer angeschlossenen Quelle werden nicht verdeckt. Bestehende Pflichtanschlüsse bleiben Pflichtanschlüsse.

Explizite Anschluss-Signaturen müssen zum einstellig typgleichen Ein-/Ergebnis der Identität passen. Ein Standardwert besitzt keine Graphkante und kann deshalb nicht allein durch die gewöhnliche Kantenprüfung validiert werden; der Adapter prüft seinen Vertrag vor der Injektion.

## Alternativen

- Reelle Zahlen als feste Standarddomäne: würde Mengen, Tupel und andere Typen ohne fachlichen Grund ausschließen.
- FehlendeObermenge oder erfundene Universalmenge: würde unvollständige Typinformation als reale Mengenbehauptung ausgeben.
- Identität an jedem unverbundenen Methodeneingang: würde bestehende Pflichtverträge stillschweigend ändern.
- Eigenständiger Auswertungs-/Operatorenkatalog: würde Semantik und Register duplizieren.

## Konsequenzen

Allgemeine und mathematische Methodenauswertung behalten explizite Capability-Grenzen. Der Aufrufpfad muss die neue Identität unterstützen; Typvariablen allein bedeuten im bestehenden TypSystem keine vollständige Unifikation. Quellenkonfigurationen werden als Daten gespeichert, Laufzeitmethoden neu berechnet. Die neue Art erfordert eine y-Version; Identität benötigt keinen zusätzlichen Knotentyp.

Nachweis und offene Abnahmen: [ExecPlan](../plans/active/2026-10-15-methodenknoten.md).
