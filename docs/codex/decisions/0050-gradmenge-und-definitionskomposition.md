# ADR 0050 – Eigene Gradmenge und ausführbare Sinuskomposition

Datum: 2026-10-12. Status: angenommen, lokaler Arbeitsstand.

## Kontext

Gradrepräsentanten waren bislang gewöhnliche reelle Zahlen mit einer Anzeige-/Umrechnungsoption. Damit ließ sich weder die Identifikation 360°=0° noch die Signatur eines Winkels ausdrücken. Tupel übersetzten ihre Werte korrekt, verloren jedoch gelieferte Komponentenobermengen zugunsten einer engeren Zahleninferenz.

## Entscheidung

- `Gradmenge` modelliert die additive Quotientengruppe R/(360 Z). Sie ist keine Teilmenge von R und kein mit üblicher reeller Multiplikation gebildeter Quotientenring. `WinkelAusdruck` und der Anschluss `mathematik.winkel` sind von Zahlen getrennt.
- `GradWinkel` vereinfacht und normalisiert rationale Vertreter exakt modulo 360; bekannte nichtreelle oder undefinierte Vertreter werden abgewiesen. Symbolische Parameter gelten innerhalb des Winkelvertrags als reell gebunden. `WinkelAusBogenmass` erhält symbolische Quotientenabbildungen. Substitution, freie Parameter, Vergleich, numerische Auswertung und Methodenkomposition benutzen diese Ausdrücke.
- Der bestehende Übersetzer erhält R→Grad und Grad→R. R bedeutet Bogenmaß. Der Rückweg wählt einen Hauptvertreter in [0,2π) oder [-π,π); der Inspector rendert dafür die bestehende `ReellesIntervall`-Mengennotation. q ist surjektiv; die Hauptvertreterauswahl ist eine Rechtsinverse, keine globale Bijektion mit R.
- Der bestehende Zahlenrechner erhält die Registry-Signatur `zahl.sin/grad`. Grad-Sinus ist geometrisch die y-Koordinate des Einheitskreispunkts. Die reelle Definitionskarte verbindet q mit Grad-Sinus und einem regulären Kartenausgang; sie referenziert keinen reellen Sinusknoten. Die numerische Implementierung darf den vorhandenen Sinusbackend verwenden. Historische Zahlen-/Gradoptionen und komplexe Auswertung bleiben erhalten.
- Polarwinkel werden als echte Winkel geliefert. Historische numerische Polarwinkel werden beim Rückweg weiterhin als Gradrepräsentanten akzeptiert. R¹ behält sein skalares Vorzeichen.
- Tupel behalten präzise Komponentenräume A₁×…×Aₙ. Die gemeinsame Grundmenge A ist deren Vereinigung; die Darstellung verwendet Aⁿ und bei Strukturübersetzungen Aⁿ → A^(1×n) bzw. A^(n×1). Nichtpersistierte `komponentenMengen` bewahren die Präzision auch über Vektoren und Abflachen hinweg. Strukturtypen bleiben verschieden; das leere Tupel besitzt den Raum {()}.

## Alternativen und Folgen

Ein Reell-Untertyp hätte Winkel weiterhin mit frei skalierbaren Zahlen verwechselt. Zusätzliche Knotentypen oder ein paralleles Definitionsregister sind unnötig. Neue gespeicherte Modi und die Wertebereichskonfiguration `winkel` nutzen vorhandene Knoten, Kartenaktionen und Codecs; die Kartenformatversion bleibt 9. Berechnete Winkel- und Komponentenmetadaten werden nach Laden neu ausgewertet.

Numerische Winkel bleiben Gleitkommanäherungen. Für negative Kleinstwinkel ist der positive Hauptvertreter nahe 2π nur eingeschränkt darstellbar; der symmetrische Hauptwert und der Sinusbackend bewahren deshalb den vorzeichenbehafteten kleinen Wert direkt. Die Definitionsinventur unterscheidet strukturelle Graphnachweise von allgemeiner mathematischer Äquivalenz. Weitere vorgeschlagene Definitionen werden gesondert umgesetzt.
