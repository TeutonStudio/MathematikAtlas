# ADR 0052: Zertifizierte Polynomlösungen

- Datum: 2026-10-12
- Status: angenommen

## Kontext
Der Gleichungslöser und der gemeinsame Langdruckdialog benötigen analytische und numerische Lösungen über R und C sowie für Matrixunbekannte. Kleine Residuen oder kleine Iterationsänderungen beweisen keine korrekten Dezimalziffern. Rangkriterien allein entscheiden keine quadratischen Matrixgleichungen.

## Entscheidung
Existenz, Vollständigkeit, Dimension und Zifferngenauigkeit werden getrennt modelliert. n bedeutet Abschneiden gegen null, je reeller Komponente. Rationale Intervalle müssen vollständig in derselben Dezimalzelle liegen; exakte Dezimalgrenzen werden durch Einsetzen oder bei komplexen Wurzeln durch bewiesene Spiegelungssymmetrie behandelt.

Reelle rationale Polynome verwenden quadratfreie Zerlegung und Sturm-Isolation. Komplex-rationale Polynome verwenden exakte Rouché-Scheiben nach heuristischer Startwertsuche und rationaler Newton-Verfeinerung. Disjunkte Scheiben und die Anzahl verschiedener Wurzeln beweisen Vollständigkeit. Ein fehlgeschlagener hinreichender Vielfachheitstest bedeutet unbekannt, nicht Vielfachheit eins. Koordinatenboxen einer komplexen Scheibe sind Einschließungen und werden nicht selbst als eindeutige Isolationsregion bezeichnet.

Matrixunbekannte werden mit dem vorhandenen Variablenknoten als quadratische Matrizen geordneter skalarer Variablen erzeugt. Vorhandene Matrixoperationen erhalten die Multiplikationsreihenfolge. Matrix-/Vektorgleichheiten werden in gemeinsame polynomiale Komponenten zerlegt. Komplexe Systeme werden in reelle Komponenten aufgeteilt. Lineare Systeme liefern exakte affine Familien über Gauß. Nichtlineare Systeme liefern exakt geprüfte Mitglieder oder lokale Krawczyk-Nachweise. Globale Dimension und Vollständigkeit bleiben ohne weiteren Beweis offen. Rang des linearen Anteils wird nur als Diagnose ausgegeben.

Analytische Darstellungen bis Grad vier verwenden quadratische Formel, Cardano mit gekoppelten Zweigen und Ferrari. Höhere Grade bleiben exakt als Originalpolynom und zertifizierte Isolationsregion beschrieben. Eine fehlende Radikaldarstellung ist kein Beweis ihrer Unmöglichkeit.

Ein gemeinsamer Hintergrunddienst mit begrenztem Cache und kooperativem Abbruch betreibt die Numerik. Familienaufträge werden bei Eingabeänderungen abgebrochen und zusätzlich durch Anfrageversionen abgesichert. Compose enthält keine mathematische Lösungslogik. Parameter werden über bestehende Kartenaktionen gespeichert; Zertifikate und Ergebnisse bleiben flüchtig.

## Grenzen und Folgen
- n: 0 bis 100, Standard 6; Suchradius: 1 bis 1000, Standard 10.
- Matrixvariable: Dimension 1 bis 8; nichtlineare lokale Systemsuche: bis 16 reelle Unbekannte.
- Komplexe Startwertsuche: bis Grad 32. Präzisions-/Operationsbudgets können offen enden.
- Eigenwertnachweise für quadratische rational-komplexe Matrizen bis Dimension 8 verwenden Faddeev–LeVerrier und denselben komplexen Wurzellöser.
- Nicht unterstützte Koeffizienten, zusätzliche unentscheidbare Annahmen und unbestimmte Familien werden nicht durch erfundene vollständige Lösungen ersetzt.
- Keine neue Knotenart, Produktionsabhängigkeit oder Kartenformatversion.

## Quellen
- [RootFinding/Isolate](https://www.maplesoft.com/support/help/addons/view.aspx?L=E&path=RootFinding%2FIsolate): isolierende rationale Regionen.
- [A Test for Existence of Solutions to Nonlinear Systems](https://epubs.siam.org/doi/pdf/10.1137/0714040): verifizierbare Intervallnachweise.
