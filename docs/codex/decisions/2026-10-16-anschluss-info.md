# Anschlussinfos und Verbindungsgesten

Datum: 2026-10-16. Entscheidung aus dem bestätigten Anschlussinfo-Plan.

Maus- und Stift-Hover ohne Kontakt zeigen nach 500 ms eine nicht fokussierende,
auf 320 dp begrenzte Kurzinfo. Ein primärer Tipp/Klick zeigt eine rein lesende
Detailansicht im gemeinsamen AtlasBestätigungsDialog. Langes Berühren und
Rechtsklick behalten das Kontextmenü; Ziehen behält den bestehenden Graphpfad
mit Snap, Neuverdrahtung, Adapterauswahl und atomarem Undo/Redo.

Der neutrale Editor besitzt ein abgeleitetes Infomodell und einen optionalen
Provider pro Instanz. Die gemeinsame MathematikKartenLaufzeit liefert Typnamen,
Beschreibungen und effektive Verträge aus bestehenden Registern. Sie startet
keine Auswertung. Fehlende Pflichtangaben werden nicht als Pflicht interpretiert;
explizit optionale Methoden erhalten nur einen Optional-Hinweis, da eine
Kartenmethode den Identitätsfallback übersteuern kann.

Originalreferenzen aus der Gruppenprojektion identifizieren den fachlichen Port.
Ein Join-Leitungsgriff führt zusätzlich die Verbindungs-ID seines Beitrags; die
Ansicht markiert diese Verbindung. Kartenwechsel, Entfernen des Ports oder des
getroffenen Beitrags schließen die zugehörige Ansicht. Hover/Dialogzustand wird
weder persistiert noch in Undo/Redo aufgenommen.

Die Kurzinfo liegt außerhalb skalierter Knoten. Drücken, Scrollen, Zeigerverlassen,
Verbindungsdrag und Eingabesperre entfernen sie. Während eines Drags bleiben
bestehende fachliche Zielhinweise zuständig. Der gemeinsame Dialograhmen behandelt
Escape bei erlaubtem Zurück-Abschluss ausdrücklich auch als Compose-Tastenevent.

Alternative: den bisherigen Zwei-Tipp-Verbindungsmodus parallel beibehalten.
Verworfen, da ein einfacher Tipp ausdrücklich die Information öffnen soll.
Die Zustands-APIs bleiben für bestehende Aufrufer und Drag-Abschluss verfügbar.
