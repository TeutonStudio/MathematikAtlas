# Platzschaffen nach Drag-Einfügung

Datum: 2026-10-16. Status: angenommen.

Beim Ablegen eines unverbundenen Knotens auf einer kompatiblen Verbindung bleibt
seine erreichte Position fest. Ein fachneutraler Layoutplan berechnet anhand
effektiver Körper-/Anhanggeometrie die notwendigen horizontalen Positionsdeltas.
Quelle und Ziel öffnen beidseitig 32 Graph-Einheiten Platz. Ausgelöste Kollisionen
werden nach außen weitergegeben; entfernte bestehende Überlappungen bleiben.
Gruppen bewegen sich als Blöcke, aktive Einfügecontainer erweitern ihre Rahmen.

Verdrahtung und Endpositionen werden zusammen als Karteninhalt übernommen und
mit der ursprünglichen Drag-Geste in einem Undo-Schritt gespeichert. Mehrdeutige
Anschlusspaare werden zuerst bestätigt und gegen den unveränderten Graph geprüft;
erst danach wird die aktuelle gemessene Geometrie für das Layout verwendet.

Die 200-ms-Animation interpoliert ausschließlich die gemeinsame Darstellung der
Gruppenprojektion. Verbindungen, Anschlüsse, Rahmen und Minimap folgen derselben
Geometrie. Es gibt weder animierte Zwischenstände in der Kartenhistorie noch
zusätzliche mathematische Auswertungen pro Frame. Neue Bearbeitung beendet den
Übergang am bereits gespeicherten Endstand.

Alternativen waren persistente Animationsschritte, ein globales Auto-Layout und
getrennte Animationen pro Composable. Sie würden Historie beziehungsweise
Layoutkontrolle verändern oder Verbindungen von den Körpern entkoppeln.
Die reine Platzberechnung läuft einmal bei Übernahme (quadratische Paarprüfung
im schlechtesten Fall); pro Animationsframe werden nur Positions-/Rahmenwerte
abgeleitet. Kein Kartenformat, öffentliche Aufrufsignatur oder Abhängigkeit ändert
sich. Der vorhandene Einfügedialog verwendet weiterhin seinen bisherigen Pfad.
