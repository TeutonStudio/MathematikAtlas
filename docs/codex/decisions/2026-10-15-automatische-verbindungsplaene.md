# Automatische Verbindungspläne, Drag-Transaktionen und Android-Vollbild

- Status: akzeptiert
- Datum: 2026-10-15
- Bezug: [ExecPlan](../plans/completed/2026-10-15-verbindungsbearbeitung-und-vollbild.md)

## Kontext

Direkt inkompatible Anschlüsse wurden bisher abgewiesen, obwohl konfigurierte vorhandene Übersetzer einen passenden Weg bilden können. Der Editor besitzt bereits atomare Kartenaktionen und gruppierte Gesten-Historie. Mathematik gehört weiterhin in MathematikKnoten; Android und Desktop teilen sich den neutralen Editor.

## Entscheidung

`VerbindungsVorschlag` beschreibt einen reinen, vollständigen Kartenkandidaten. Der Editor hält die Ausgangskarte und bietet einen oder mehrere Vorschläge an. Eine eindeutige Übersetzerkette wird direkt angewendet, mehrere gleich kurze Wege erscheinen im gemeinsamen scrollbaren Auswahldialog. Bei Abbruch bleiben Kanten unverändert; vor Bestätigung werden Snapshot und vollständige Verdrahtung erneut geprüft. Undo/Redo stellt die ganze Transaktion wieder her.

Der mathematische `ÜbersetzerVerbindungsPlaner` verwendet ausschließlich bestehende, über `konfiguriereÜbersetzer` erzeugte Knoten. Breiten-Suche findet alle kürzesten einfachen, typgeprüften Wege. Wiederholte Anschlussarten und unnötig lange Wege zu bereits erreichten Typzuständen werden vermieden. Direkte gültige Kanten haben Vorrang. Rückführung ohne Zeugnis, Objektpunktauswahl ohne Anker und rein semantische Polar-/Kartesischwechsel werden nicht automatisch erfunden. Bekannte unterschiedliche Komponentenanzahlen von Tupeln/Vektoren werden nicht durch gröber typisierte Umwege verschleiert. Datenabhängige Voraussetzungen prüfen weiterhin die bestehenden mathematischen Auswerter.

Die Vorschau auf vielen Ports verwendet bis zu 128 gespeicherte Ergebnisse nach aktuell aufgelösten Portverträgen und eine kleine Zwei-Knoten-Typkarte. Die Zyklusprüfung erfolgt trotzdem für den aktuellen echten Graphen. Eine synthetisch erlaubte Direktkante wird konservativ als möglich markiert, denn im echten Graph kann ein abhängiger Ausgang eine explizite Konversion benötigen. Erst beim Ablegen wird der vollständige Graph geplant und erneut geprüft. Alte belegte Zielkanten bleiben während der Zwischenplanung gültig und werden bei der abschließenden Kante ersetzt, damit abhängige Ausgänge nicht vorübergehend ihren Typ verlieren.

## Einfügen und Herausziehen

Ein zunächst unverbundener Knoten kann mit seiner Mitte auf eine Verbindung gezogen werden. Die tatsächliche Bézierkurve in Weltkoordinaten bestimmt das Ziel; die Linie wird hervorgehoben. Der Plan ersetzt sie durch Quelle → freier Eingang / Ausgang → bisheriges Ziel. Bei mehreren passenden Portpaaren erscheint die Auswahl. Bewegung und bestätigte Verdrahtung sind zusammen ein Undo-Schritt. Eine abgebrochene Portauswahl behält nur die Bewegung.

Herausziehen ist entsprechend der Nutzerentscheidung ausdrücklich aktiviert: Alt-Ziehen am Desktop oder Aktion „Aus Verbindung herausziehen“ für den nächsten Drag am Tablet. Die Aktion ist im Editor und in den Knoten-Kontextaktionen verfügbar. Normales Verschieben behält Verbindungen. Nur eine eindeutige belegte Eingangsquelle und ein benutzter Ausgang können überbrückt werden; Fan-out bleibt erhalten. Bei inkompatiblem Bypass oder mehreren Eingangsquellen werden keine Kanten getrennt. Die alten Kanten bleiben bei der Planung bis zur sicheren Ersetzung bestehen. Abgebrochene Gesten setzen die Bewegung zurück. Der ursprüngliche Down-Punkt bestimmt den Ziehbereich, auch bei schnellen Bewegungen über die Ziehschwelle hinaus.

## Android-Vollbild

MainActivity verwendet WindowCompat und WindowInsetsControllerCompat, zeichnet ohne Systemleisten-Padding und blendet Status-/Navigationsleiste aus. Fokus-Rückkehr aktiviert den immersiven Modus erneut; eine Randwischgeste kann die Systemleisten vorübergehend anzeigen. Aussparungen und Bildschirmtastatur behalten ihre Insets. Das ist kein Kioskmodus und ändert die Systemnavigation nicht dauerhaft.

## Alternativen und Konsequenzen

Keine automatische Fachlogik im neutralen Graphprüfer, kein neuer Konverterkatalog und keine neuen Knotentypen. Alle beliebig langen Übersetzungsumwege anzuzeigen wäre unübersichtlich und könnte semantisch unnötige Umdeutungen anbieten. Automatisches Herauslösen bei normalem Verschieben wurde ausdrücklich verworfen. Die vorhandenen Datenformate enthalten neue Übersetzer wie manuell eingefügte Knoten; keine Migration ist erforderlich. Vorschläge, Auswahl und Modus für den nächsten Drag werden nicht persistiert.

## Verifikation

Graph-/Historytests sichern Insert, Bypass, Fan-out, belegte Ports, inkompatible Typen, abhängige Ausgänge, Auswahlabbruch und veraltete Vorschläge ab. Mathematische Tests prüfen eindeutige/mehrdeutige/mehrgliedrige Ketten, Anschlussreihenfolge in beiden Ziehrichtungen, tatsächliche Übersetzung, JSON-Rundreise, Dimensionskonflikte, Vorschaucache und Zyklen. Desktop-Compose-Tests klicken Ports und Auswahl; echte Touch-/Mausgesten testen Einfügen und Herausziehen bei Zoom 0,5 und 1,5 einschließlich Alt und Undo. Ein Android-Instrumentierungstest für Systemleisten ist vorhanden; sein Ausführungsstatus sowie die finalen Prüfergebnisse stehen im ExecPlan.

Ein isolierter Diagnoselauf mit 221 Knoten/199 Kanten ergab etwa 24 ms für die Vorschau von 20 Zielen und 198 ms für die konkrete Planung; im parallel laufenden Gesamtprüflauf waren es 79 ms und 498 ms. Diese Messungen sind keine geräteunabhängigen Laufzeitlimits.
