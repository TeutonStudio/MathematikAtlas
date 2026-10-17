# Stabile Quellenidentitäten und ausdrückliche Wertebereiche

Datum: 2026-10-17. Status: M5 in Umsetzung, noch keine Produktfreigabe.

Variable, allgemeiner Parameter und nullstellige Wahrheitsquellen bleiben
unterschiedliche bestehende Knotentypen. Die gemeinsame Quellenhilfe leitet Typ,
Anschlussart, Form und Komponentenräume aus demselben fachlichen Grundraum ab.
Die bisherige logische Argumentidentität allgemeiner Parameter ist ihre Knoten-ID und bleibt exakt erhalten. Interne strukturierte Symbolnamen enthalten nur die Knoten-ID; sichtbare Namen stehen in LaTeX/Quellenmetadaten.
Die Variable materialisiert ihre vorhandenen strukturierten Zugriffe; der
allgemeine Parameter behält seine bisherige symbolische Repräsentation. Ein
Parameter wird dadurch nicht zur Lösungsvariable.

Bindungs- und Strukturquellenidentitäten werden aus der Knoten-ID abgeleitet.
Sichtbare Namen ändern diese Identitäten nicht. Gleichnamige Quellen werden bei
Methodenbildung und Auflösen lokal eindeutig benannt, über ihre IDs zugeordnet
und behalten ihre Rollen. Quellen ohne Strukturbindung reservieren ihren Namen
anhand ihrer tatsächlichen Identität. Umbenennen oder Portreihenfolge darf keine
Quellen zusammenführen; eine Kopie mit neuer Knoten-ID erhält eine neue Quelle.

## Wertebereiche und Altdaten

Eine vollständig fehlende Bereichseigenschaft bezeichnet weiterhin den
historischen Standard R. Dokumentierte fehlende optionale Felder bekannter Arten
behalten ihre historischen Defaults. Vorhandene falsche Feldtypen, unbekannte
Arten, Orientierungen, Versionen oder Felder und ungültige Bereiche werden
konkret diagnostiziert. Originalkonfiguration und Verbindungen bleiben erhalten;
die Auswertung ist bis zur ausdrücklichen Reparatur gesperrt. Aktive angeschlossene
Variable-Grundräume werden nicht durch alte inaktive Parameter eingeschränkt.

Historisch wurde eine ausdrücklich leere Tupelkomponentenliste als zwei reelle
Komponenten gelesen. Diese Konfiguration ist daher mehrdeutig und wird nicht
still in einen leeren Tupelraum umgedeutet. Neu geschriebene Bereichseigenschaften
tragen innerhalb der bestehenden Eigenschaft `version=1`; dort bezeichnet eine
leere Liste ausdrücklich das leere Tupel. Kartenformat 10 und Mathematik-Wire
bleiben unverändert. Gültige alte Bereichseigenschaften werden beim Laden nicht
umgeschrieben.

Android und Desktop verwenden denselben Bereichseditor. Er prüft stets den
vollständigen Konfigurationskandidaten, auch bei verschachtelten Änderungen und
an der Tiefengrenze. Ungültige Entwürfe werden nicht übernommen. Eine ausdrücklich
gewählte gültige Reparatur entfernt ausschließlich die eigene Bereichsdiagnose;
fremde Migrationsdiagnosen und gesonderte Quellenidentitätsdiagnosen bleiben erhalten. Doppelte IDs können nicht durch eine Bereichsreparatur entsperrt werden. Änderungen folgen dem bestehenden
Konfigurationsplan und dessen Undo-/Verlustvertrag.

## Numerische Korrektur und Vertragsgrenze

N bezeichnet positive natürliche Zahlen. Die Inferenz ordnet 0 korrekt N₀ zu;
positive ganze Zahlen bleiben in N, negative in Z. Auch der explizite
N₀-Divisionsbereich wird nicht mehr als N behauptet. Alte Methoden mit einem
numerischen Ergebnis außerhalb ihrer deklarierten numerischen Zielmenge werden
abgewiesen. Noch offene numerische Mitgliedschaften bleiben im kanonischen
Anwendungsergebnis erhalten. Die Zieldeklaration wird niemals still erweitert.

Diese Prüfung betrifft tatsächliche Zahlwerte mit nachgewiesenem numerischem
Zielbereich. Sie führt keinen neuen allgemeinen Ergebnisdomänenvertrag ein:
Mengenfamilien verwenden ihre deklarierte Zielmenge weiterhin als Grundmenge,
wie in [der bestehenden Entscheidung](2026-07-27-zielmenge-als-grundmenge.md)
festgelegt. Eine pauschale Mitgliedschaftsprüfung aller Ergebniswerte würde diesen
Vertrag brechen. Sonstige Ergebnisdomänen behalten ihre vorhandenen Prüfpfade.

## Strukturierte Bereiche und Formeln

Gebundene Formeln verwenden Quellen-IDs vor sichtbaren Namen. Eindeutige historische Namensaliase überschreiben keine fremde ID. Export, Wire-Rundlauf und Methodenbildung erhalten Tupel-, Vektor- und Matrixbereiche als ein logisches Argument beziehungsweise Ergebnis. Interne lokale Namen werden erst nach identitätsbasierter Zuordnung vergeben.

Die vorhandene Mengenmitgliedschaft prüft bei konkreten orientierten Vektoren Orientierung und Dimension, bei Matrizen Zeilen und Spalten sowie jeweils die Komponentenbereiche. Konkrete fremde Strukturen werden abgewiesen; allgemeine symbolische Strukturparameter und offene Komponentenbedingungen bleiben unentschieden. Ein Tensor zweiten Ranges wird dabei nicht automatisch zur Matrix. Die Inferenz von Achsenlängen verwendet wegen möglicher leerer Tupel ebenfalls N₀.

## Verifikation

Vollständige lokale Suite grün (3 min 26 s, 277 Tasks, 2237 Fälle/0 Fehler/1 vorhandener Skip), einschließlich 16 Quellen-, sechs Kern- und zwei nativer Desktop-UI-Regressionen. Zwei Android-Inspector-/Undo-Fälle kompiliert; isolierte Geräte- und Linux-RPM-Abnahme erfolgen SHA-bezogen in CI. Standalone-kotlinc fehlt, lokales jpackage unterstützt rpm nicht. Kein neuer Typ und keine neue Produktionsabhängigkeit.
