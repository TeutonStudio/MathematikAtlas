# v3 – Änderungsdokumentation

Diese Datei ist die fortlaufende Anforderungsliste für den Versionsraum **v3.y.x** des Mathematik Atlas. Sie konkretisiert die langfristige `ROADMAP.md`, ohne bereits eine bestimmte Unterversion oder einen Implementierungsbranch vorwegzunehmen.

Neue für v3 beschlossene Änderungen werden hier ergänzt, bevor sie in konkrete Releases und Implementierungspläne zerlegt werden.

## Übergeordneter v3-Rahmen

Der in der Roadmap festgelegte Schwerpunkt von v3 bleibt **Grafik, Auszeichnung und Dokumente** mit strukturierten Inhalten und Erzeugungspfaden für unter anderem SVG, TikZ, LaTeX, Mermaid und HTML.

Zusätzlich werden hier notwendige produktweite Änderungen festgehalten, die für die v3-Nutzung verbindlich sein sollen.

## Änderungen

### V3-001 – Geöffnete Karte über den App-Lebenszyklus erhalten

**Status:** geplant

Die aktuell geöffnete Karte muss Teil des dauerhaft wiederherstellbaren App-Zustands sein. Ein Wechsel des Android-Lebenszyklus darf den Nutzer nicht aus der Karte werfen oder stillschweigend eine andere Karte öffnen.

#### Anforderungen

- Beim Öffnen oder Wechseln einer Karte wird ihre stabile Karten-ID als aktuell geöffnete Karte gespeichert.
- Eine Bildschirmrotation beziehungsweise andere Activity-/Configuration-Neuerstellung lässt dieselbe Karte geöffnet.
- Wird die App minimiert und anschließend wieder in den Vordergrund geholt, bleibt dieselbe Karte geöffnet.
- Wird der App-Prozess während des Hintergrundzustands von Android verworfen und später neu erstellt, wird die zuletzt geöffnete Karte erneut geöffnet, sofern sie weiterhin existiert.
- Die Wiederherstellung darf keine neue Kopie der Karte erzeugen und die Karte nicht erneut als inhaltliche Änderung speichern.
- Ist die gespeicherte Karte nicht mehr vorhanden oder nicht lesbar, muss die App kontrolliert auf die Kartenübersicht zurückfallen, statt abzustürzen.

#### Abgrenzung

Diese Anforderung betrifft zunächst die **Identität der geöffneten Karte**. Flüchtige UI-Zustände innerhalb der Karte, etwa offene Dialoge, aktuelle Auswahl, Drag-Zustände oder Inspector-Fokus, gelten nicht automatisch als mitgespeichert und werden bei Bedarf als eigene V3-Anforderungen dokumentiert.

#### Abnahmekriterien

1. Karte A öffnen, Gerät drehen: Karte A bleibt geöffnet.
2. Karte A öffnen, App minimieren, App erneut öffnen: Karte A bleibt geöffnet.
3. Karte A öffnen, App in den Hintergrund schicken, Prozess neu erzeugen lassen: Karte A wird anhand ihrer stabilen ID wieder geöffnet.
4. Die Wiederherstellung erzeugt weder eine zweite Karteninstanz noch einen zusätzlichen inhaltlichen Undo-/Speicherschritt.
5. Ist Karte A zwischenzeitlich nicht mehr verfügbar, erscheint die Kartenübersicht ohne Absturz.
