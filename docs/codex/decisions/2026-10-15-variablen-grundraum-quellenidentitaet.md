# Variablen erben ihren Grundraum und behalten eine Quellenidentität

- Datum: 2026-10-15
- Status: angenommen

## Kontext
Eine Variable aus R×R wurde bisher aus zwei unabhängigen Komponentenvariablen aufgebaut. Der Übersetzer erzeugte daraus zwei Methodenargumente; der Mengenfilter verlangt dagegen ein Elementargument. Die Inspectorprojektion des Methodenaufrufs verändert keine logischen Argumente. Ein zweiter Konfigurationsweg für Variablenstruktur und Grundraum konnte zudem widersprüchliche Zustände erzeugen.

## Entscheidung
Der angeschlossene Grundraum bestimmt Elementtyp, Struktur, Dimension und Komponentenbereiche. Neue Variablen benötigen diesen Anschluss. Historische implizite Bereiche werden beim Laden einmalig in explizite Mengenquellen überführt. Die Kennzeichnung `variable.grundraumVertrag` verhindert, dass absichtlich entfernte Verbindungen beim nächsten Laden wiederhergestellt werden. Ein vorhandener Grundraum hat Vorrang vor alten Inspectorparametern.

Jede Variable besitzt eine stabile Quellenbindung `${knotenId}:grundraum-element`. Skalare Zahl- und Winkelvariablen tragen diese als `bindungsId`; strukturierte Elemente verwenden eine interne Wurzel mit `StrukturQuellenBindung`. Komponenten sind symbolische Zugriffe auf die Wurzel. `termAlsMethode` normalisiert die öffentliche Benennung anhand der Quellenidentität. Unabhängige gleichnamige Quellen bleiben getrennte Argumente.

Der Gleichungslöser darf eine Matrix lokal in skalare Unbekannte überführen. Diese Aufteilung verändert weder die Quellenvariable noch die logische Methodensignatur. Allgemeine Methoden behalten genau ein Matrix- bzw. Tupelargument pro Quellenvariable.

Methodenausgangsverträge stammen aus tatsächlichen Signaturen. Methodeneingänge zeigen die Signatur ihrer verbundenen Quelle durch `symbolAnschluss`, ohne ihre deklarierte Kompatibilitätsanforderung zu verengen. Nach fehlender Grundraumeingabe bleibt der letzte erfolgreiche Variablenausgangsvertrag erhalten; die Auswertungsdiagnose verhindert eine Berechnung. Fehlende Eingaben dürfen keine bestehenden Folgeverbindungen löschen.

## Alternativen
- Alle Tupel grundsätzlich zu skalaren Argumenten zerlegen: widerspricht der Elementsemantik von Mengenfiltern und verliert verschachtelte Strukturen.
- Den Filter automatisch auf beliebig viele Argumente erweitern: verschleiert den Unterschied zwischen einem Tupel und mehreren unabhängigen Parametern.
- Struktur im Inspector unabhängig vom Grundraum konfigurieren: erzeugt zwei konkurrierende Quellen der Wahrheit.

## Folgen
Bestehende Knotenarten und Kartenformat 9 bleiben erhalten. Die Migration nutzt Mengenquellen und bei zusammengesetzten historischen Bereichen den bestehenden gebundenen Formelrechner. Winkelparameter erhalten optionale Anzeige- und Bindungsfelder mit kompatiblen Defaults; der Wertcodec liest den historischen Tag weiterhin und ergänzt einen Tag für angereicherte Winkelparameter. Keine neue Produktionsabhängigkeit. Automatische Namen verwenden das bestehende semantische Prädikatskriterium; individuelle Namen bleiben erhalten. Tests müssen ihre Grundräume explizit bereitstellen.
