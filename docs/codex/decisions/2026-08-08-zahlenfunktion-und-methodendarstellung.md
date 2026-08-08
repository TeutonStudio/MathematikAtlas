# ADR: Zahlenfunktionen über Signaturen prüfen und Methoden gemeinsam darstellen

## Status

Angenommen für `v2.28.2`, Issue #340.

## Kontext

Der Zahlenrechner soll neben Zahltermen auch endlichstellige Methoden verarbeiten, deren Argumenträume und Zielmenge innerhalb der Hamilton-Quaternionen liegen. Bislang besitzt der Rechenkern keine gemeinsame Zahlenfunktionsanforderung. Außerdem rendert `Methode.zuLatex()` nur eine Gleichung, während der Term-zu-Methode-Knoten eine eigene `cases`-Darstellung aus Signatur und Vorschrift zusammensetzt.

Die getrennten Darstellungswege würden bei Ableitungsfunktionen, punktweise gehobenen Operatoren und parametrisierten Integralen auseinanderlaufen. Eine neue Anschlussart `Zahlenfunktion` würde dagegen fachliche Semantik in die statische Grapharthierarchie duplizieren.

## Entscheidung

1. Eine Zahlenfunktion bleibt eine gewöhnliche `Methode` und wird durch `MethodenAnforderung.Zahlenfunktion` anhand ihrer `MethodenSignatur` geprüft.
2. Jeder einzelne Argumentraum sowie die Zielmenge müssen strukturell als Teilmenge von `\mathbb H` nachweisbar sein. Unbekannte benannte Mengen werden nicht über Namen oder LaTeX geraten.
3. Ein effektiver Gesamtbereich bleibt Teil der Signatur, ersetzt aber nicht die Prüfung der deklarierten Argumenträume.
4. `Methode` stellt Signatur und Vorschrift zentral in einer `cases`-Umgebung dar. Knotenrenderer verwenden diese Darstellung, statt sie erneut zusammenzusetzen.
5. Die erste totale Ableitungsfunktion heißt `f'`; partielle Ableitungsfunktionen und Differentiale behalten getrennte Typen und Notationen.

## Alternativen

- **Neue Anschlussart `mathematik.zahlenfunktion`:** verworfen, weil die Gültigkeit von der vollständigen Methodensignatur und nicht nur von einer statischen Anschlussart abhängt.
- **Nur einstellige Zahlfunktionen zulassen:** verworfen, weil insbesondere `\mathbb R^2\to\mathbb R` ein Kernfall ist.
- **Numerische Mengen anhand ihres LaTeX erkennen:** verworfen, weil sichtbare Darstellung keine fachliche Datenquelle sein darf.
- **Private Renderer je Knotentyp behalten:** verworfen, weil Ableitungs-, Integral- und punktweise Methodenausgaben sonst verschiedene Signaturdarstellungen entwickeln.

## Konsequenzen

- Zahlenfunktionsanschlüsse können später als semantische Mehrfachanforderung „Zahl oder passende Methode“ umgesetzt werden.
- Neue strukturierte Zahlmengen müssen ihre numerische Herkunft explizit oder über bekannte Mengenkonstruktionen offenlegen, bevor sie automatisch akzeptiert werden.
- Die zentrale Methodendarstellung betrifft alle direkten `Methode.zuLatex()`-Aufrufe und benötigt deshalb vollständige Regressionstests.
- Es entsteht kein neuer physischer Methoden- oder Knotentyp und keine Persistenzmigration durch diese Entscheidung allein.
