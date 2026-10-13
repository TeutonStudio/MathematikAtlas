# ADR 0053: Inhaltsminimum und indexbasierte Strukturdefinitionen

Datum: 2026-10-12. Status: Umsetzung und Verifikation laufen.

## Kontext
Gespeicherte Benutzermaße allein garantieren keine lesbaren Formeln, Anschlüsse oder Gruppeninhalte. Die Verbindungsebene überdeckte bisher die auswählbaren Gruppenköpfe. Strukturdefinitionen wurden als beschreibende Regelknoten statt als ausführbare Zerlegung gezeigt. Polarkoordinaten waren flach kodiert, obwohl Radius und Winkeltupel unterschiedliche Komponenten sind.

## Entscheidung
Renderer besitzen einen fachneutralen Mindestgrößenvertrag. Text-/Formelinhalte werden einmal in ihrer natürlichen Größe gemessen. Bei interaktiven Viewports werden Kopf, Fuß und Bedienung getrennt natürlich gemessen; die Zeichenfläche erhält danach einmal endliche Maße und den verbleibenden Platz. Das tatsächliche Mindestmaß wird über die Rendereraktionen an den Editor gemeldet. Schriftmaß und Anschlussanzahl ergänzen die Untergrenze. Die effektive Knotengröße ist das Maximum aus gespeichertem Benutzermaß und abgeleitetem Inhaltsminimum. Mindestmaße sind flüchtiger Layoutzustand, keine persistierten Kartenparameter. Die zentrale Gruppenprojektion liefert dieselbe Geometrie für Darstellung, Anschlüsse, Verbindungen und Treffer. Größere Benutzermaße bleiben erhalten. Die Diagnosefußzeile wird natürlich gemessen und innerhalb des effektiven Knotenrechtecks reserviert. Bei Kapseln berücksichtigt das Breitenminimum zusätzlich den Seitenradius für Inhalt und Fußzeile. Der frühere Außenfußzeilenvertrag wird damit bewusst ersetzt.

Aufgeklappte Gruppen werden von innen nach außen um ihre Mitglieder und Untergruppen erweitert, mit 24 dp Rand und Kopfbereich. Gruppentitel und Laufzeitdiagnose bestimmen zusätzlich die Kopfbreite. Der gesamte Gruppenkopf einschließlich Text und Bedienung wird gemeinsam mit dem Zoom skaliert. Verbindungen liegen unter den Gruppenköpfen, Knoten darüber; Klick wählt und Doppelklick öffnet die Gruppe. Kapselkontur bestimmt sichtbaren Rahmen und Trefferfläche gemeinsam.

Struktur→Indexmethode erweitert atlas.uebersetzer, ohne neue Knotenregistrierung. Indizes sind 1-basiert und achsenweise begrenzt. Verschachtelte Tupel behalten ihre innere Struktur. Indexzugriff und Methodenbindung gehören in den Rechenkern; Definitionskarten benutzen vorhandene Knotenfamilien. Produktdefinitionen müssen geordnete Komponentenprodukte und explizite Indexsummen enthalten und dürfen ihren zu definierenden Operator nicht selbst aufrufen.

Generierte Indexgraphen verwenden eine gemeinsame Frischebasis über die freien Namen sämtlicher ursprünglicher Operanden. Alle Indexübersetzer erhalten denselben Operanden-Kontext und wählen ein Präfix ohne Überschneidung mit Quellnamen. Explizite interne Rollenmarker lösen Kontraktionsindizes auf; fremde Methoden werden nicht über Namenssuffixe interpretiert. Damit bleiben symbolische Komponenten namens i, k oder p fachliche Quellvariablen.

Konjugation symbolischer Komponenten bleibt ein eigener substituierbarer Zahl-Ausdruck. Eine bloße LaTeX-Variable würde die Bindung an die Indexargumente verlieren. Das Skalarprodukt konjugiert gemäß vorhandener Linearitätskonvention auch bei aus den tatsächlichen Komponenten erschlossenem komplexem Zahlbereich; für reelle Komponenten ist Konjugation die Identität.

Determinanten werden als Permutations-Komponentenprodukte mit Vorzeichen und Indexsumme zerlegt. Inversen verwenden transponierte Kofaktoren und Division durch eine separat expandierte Determinante. Diese Definitionsgraphen rufen den ursprünglichen Matrixoperator nicht auf. Die Materialisierung besitzt explizite Budgets: höchstens Dimension 7 für Permutationen sowie 100000 Minor-Komponenten. Das Minorbudget kann eine Inverse bereits unterhalb der Permutationsgrenze begrenzen. Überschreitungen und singuläre Nenner liefern fachliche Diagnosen. Die neue symbolische Kofaktorauswertung wird bis Dimension 6 materialisiert. Die bisherige numerische Inversion rationaler Matrizen bleibt auch für größere Dimensionen verfügbar.

Matrixzerlegung ist ein Ziel des bestehenden Übersetzers. Richtung (Zeilen/Spalten) und Ausgabeform (orientierte Vektoren/Tupel) sind persistierte Parameter. Der historische Rechneroperator bleibt zur Kompatibilität auflösbar, wird aber nicht mehr zur Auswahl angeboten. Codec-Migrationen erhalten IDs und Verbindungen und normalisieren ausschließlich die Eingangsrolle matrix→wert. Dynamische Ausgänge behalten die historischen matrixZerlegen-IDs.

LaTeX-Einsetzungen übernehmen die vorhandene Klammerung von Matrizen, Vektoren und Tupeln. Zusätzliche notwendige Gruppierungen werden mit \left und \right geschrieben.

Polarkoordinaten ab R² werden als (r,(φ,θ,…)) ausgegeben. R¹ behält (r,Vorzeichen). Historische flache Tupel bleiben lesbar; Winkelwerte bleiben typisiert. Ungültige Eingaben sind fachliche Diagnosen.

## Alternativen
Persistierte automatische Größen würden Undo und Speichern mit Layoutänderungen belasten. Ein zweiter mathematischer Indexknotenkatalog würde bestehende Methoden-/Übersetzerverträge duplizieren. Flaches Polarformat verliert die fachliche Paarstruktur. Diese Alternativen werden verworfen.

## Konsequenzen
Layoutmessung muss von Zoom und Benutzermaßen unabhängig und stabil bleiben. Renderer mit begrenzten Viewports können freie Messung deaktivieren. Größen-, Auswahl-, Methoden-, Definitionsgraph- und Kompatibilitätstests werden getrennt geführt. Es entstehen weder neue Produktionsabhängigkeiten noch neue registrierte Knotentypen.

## Nachweise
Fortlaufender Nachweis: ../plans/active/2026-10-12-inhaltsminimum-indexdefinitionen.md. Finale Abnahme steht aus.
