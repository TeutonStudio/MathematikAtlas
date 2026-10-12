# 0046 – Visuelle Gruppen als Projektion desselben Graphen

Datum: 2026-10-12. Status: angenommen.

Gruppenfaltung und einzelne Gruppenansichten verwenden einen abgeleiteten Darstellungsgraphen. Der persistierte Graph und die mathematische Auswertung bleiben vollständig in der Hauptkarte. Jede projizierte Schnittstelle verweist auf einen echten Anschluss; Verbindungsprüfung und Änderungen erfolgen dort.

Alternativen: echte versionierte Unterkarten würden automatisches Extrahieren, Schnittstellenmigration und Synchronisierung benötigen; versteckte weiterhin komponierte Knoten sparen keine Layoutarbeit.

Gruppen werden anhand ausgeklappter Rahmen räumlich verschachtelt. Direkte Mitgliedschaften bleiben eindeutig. Eingeklappte Bereiche behalten ihre Mitglieder. Auflösen erhält Inhalt. Darstellungsknoten sind keine registrierten oder persistierten Knotentypen.

Konsequenzen: gemeinsame Undo-Historie und Außenwerte; kein unabhängiges Speichern einer Gruppenansicht. Projektion, Treffergeometrie, Minimap und Verbindungsvorschau müssen dieselben Anschlussabbildungen verwenden. Faltung spart Rendering, nicht notwendige Mathematikauswertung.
