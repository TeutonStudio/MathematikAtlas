# Gemeinsame Matrix-, Vektor- und Tensoroperationen

Datum: 2026-10-17. Status: M2 als v3.4.4/x integriert; Schlussabnahme nach enger separater Testreparatur v3.4.5/e6760b9a vollständig grün. Ursprünglicher Androidfehler bleibt historisch dokumentiert.

## Kontext

Spur, Matrixdiagonale, Vektorradius und Kreuzprodukt hatten eigene erzeugbare Typen,
obwohl bestehende Rechner dieselben Operationen anbieten. Die Diagonalmodi besaßen
zusätzliche Projektionsverträge. Doppelte Radius-/Normformeln belegten außerhalb
reeller Komponenten keine Norm; Methodenhebung verlor Bindungsidentitäten und
Voraussetzungen. Typverbreiternde Anschlussnormalisierung konnte semantische
Portverträge beim anschließenden Laden verwerfen.

## Entscheidung

Die vier Spezialtypen werden ausschließlich als konfigurierte Varianten vorhandener
Matrix- und Vektorrechner erzeugt. Historische Wissens-IDs bleiben auflösbar. Spur
verwendet den bestehenden Kernalgorithmus. Diagonaloperatoren lesen Rang zwei und
projizieren ausdrücklich in Tupel, Zeile oder Spalte. Nebendiagonalen rechteckiger
Matrizen bleiben rechts oben verankert; Erzeugung unterstützt Haupt und Neben.
Auslesen weist auf verworfene Nichtdiagonaleinträge oder rechteckige Form hin.

Reelle euklidische Norm, Kreuzproduktkomponenten und Basisvektorerzeugung haben je
einen Fachalgorithmus. Die Norm benötigt nachgewiesen reelle, definierte Komponenten.
Quellenbereiche folgen Bindungsidentitäten. Eingeschränkte Zahlbereiche liefern eine
konservative Grundmenge für Ergebnisinferenz; die präzise Definitionsmenge bleibt
unangetastet. Aus einem reellen zusammengesetzten Wert wird kein Bereich seiner
inneren Variablen rückgeschlossen. Typisierte Komponenten benötigen einen gültigen
Index und einen nachgewiesenen reellen Quellraum. Offene Definitionsbedingungen
werden strukturiert weitergegeben; widerlegte verhindern die Berechnung.

Das klassische Kreuzprodukt benötigt einen kommutativen Koeffizientenbereich und
Dimension drei. Historisch konfigurierte Zeilen-/Spaltenorientierung bleibt explizit
validiert. Die vorhandene freie Orientierung des kanonischen Rechners bleibt ohne
solche Auswahl erhalten. Die konjugierte Tensor-Norm bleibt ein getrennter Vertrag.

Transponieren behält seinen bestehenden Typ und delegiert gemeinsame Kernoperationen.
Vektortransposition wechselt Zeile/Spalte; eine Tensorachsenpermutation erhält den
Tensor, auch bei Rang eins. Beliebige zulässige Tensorpermutationen bleiben verfügbar.

Punktweise Methodenhebung verwendet Bindungs-IDs und genau ein logisches Ergebnis
pro Wertoperand. Ein tupelwertiges Einzelresultat bleibt eine Struktur. Bekannte
mathematische Wrapper verwenden die vorhandene Materialisierung; allgemeine
Capabilities ohne symbolische Vorschrift werden ausdrücklich abgelehnt. Alle
Definitionsräume und Voraussetzungen werden nach Angleichung geschnitten bzw.
übernommen. Neu entstehende Bedingungen gehören auch zur erzeugten Methode, sodass
Extrahieren und späterer Aufruf sie nicht verlieren.

## Migration und Bedienung

Die bestehende Codec-Pipeline normalisiert die vier Altarten vor späteren
Anschlussnormalisierungen, einschließlich eingebetteter Methodenquellen. Eindeutige
Rollen, Typen, Kardinalitäten und Modi sind Voraussetzung. IDs, Verbindungen,
Reihenfolgen, Defaults, feste Kartenverweise und typisierte Rollenreferenzen bleiben
erhalten. Mehrdeutige Knoten behalten ihre Originaldaten plus blockierende Diagnose.
Gleichoperatorige Normalisierung bewahrt migrierte semantische Portverträge.

Android und Desktop verwenden gemeinsame Optionen für Diagonalprojektion,
Diagonalart und Kreuzproduktorientierung. Änderungen gehen als vollständige
Knotenkonfiguration durch den vorhandenen KonfigurationsPlan: relevante
Verbindungsverluste werden vor Übernahme benannt, der Wechsel bleibt ein Undo-Schritt.
Unbekannte gespeicherte Werte werden sichtbar diagnostiziert.

Berechnete Werte übernehmen Voraussetzungen, Quellen und Übersetzungsherkunft.
Ein Zeugnis des Eingangswerts wird durch eine mathematische Änderung nicht zum
aktiven Zeugnis des Ergebnisses. Unveränderte Literale behalten gültige Zeugnisse.
Kartenformat und Mathematik-Wire-Payload bleiben unverändert.

## Evidenz und Grenzen

Gezielte Kern-, Methodenhebungs- und Codec-Regressionen sowie der vollständige lokale
Produktlauf (2.180 Tests, 0 Fehler, 1 bestehender Skip, Lint/APKs/Desktop) bestehen.
Die unabhängige Prüfung einschließlich tatsächlicher Einzelanschlussbelegung und
N₀-Grundmengeninferenz meldet keine weiteren Findings. Eine historische
Matrixdiagonalen-Definitionskarte bleibt unter test/resources/konsolidierung-m2
unverändert gesichert (SHA-256 763ee69d0e1eb2dce3bc7d504a7eb735fc6d1e9ea5ef14923324751a9f9fe16f).
Geräte-/RPM-Abnahme und Veröffentlichung werden dem freigegebenen SHA im aktiven Gesamtplan zugeordnet.


## M2-Abschluss über Testreparatur v3.4.5 (2026-10-17)

M2 bleibt historisch als v3.4.4 auf `ec152625926698f58e1ddd1d1a1d6420f8b0c6cc`
integriert; sein ursprünglicher Android-master-Lauf mit einem Suchresetfehler
wird nicht nachträglich als erfolgreich ausgegeben. Die enge Testreparatur ist
separat als v3.4.5 auf `e6760b9a41aaeb186dee7ee7c13254430cb47ee8` veröffentlicht
(PR #459, geprüfter Head `89a1097a5c09531772bccd63934b9fb66df9d0a9`).
Kein Produktionsverhalten, Katalog, Format oder Gate wurde geändert. Die genaue
ursprüngliche Fehlerursache bleibt unbewiesen.

Alle sechs Reparatur-PR-Gates sind erfolgreich. Das gesicherte
[Laufzeitartefakt](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38992259351)
belegt 7 Editor- und 51 App-Tests ohne Fehler oder Skips; der Resettest wurde
in 6,266 s erfolgreich ausgeführt. Sämtliche finalen master-Workflows am
Reparaturcommit sind durch die Root-CLI als vollständig erfolgreich bestätigt:
[Release-Guard](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38992803512),
[Linux/RPM/Persistenz](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38992803457),
[Android-Build](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38992803546),
[Kern/Android-Laufzeit](https://github.com/TeutonStudio/MathematikAtlas/actions/runs/38992803470).
Der konkrete zusätzliche master-Artefaktexport ist noch nicht gesichert; die
Workflow-Erfolge sind diesem finalen SHA zugeordnet. Lokale Werkzeug- und
Gerätesignaturgrenzen bleiben unverändert dokumentiert.

Lokale Arbeitscommits sind SamAI/SamAI verifiziert. Beide finalen GitHub-Squashcommits
haben Autor Alexander und Committer GitHub/web-flow und sind keine lokalen SamAI-Commits.
M3 ist anschließend als v3.4.6/x auf `samai/v3.4.6/konsolidierung-m3` ab exakt
`e6760b9a41aaeb186dee7ee7c13254430cb47ee8` reserviert; Reservierungscommit
`63acb023c1bc33b8e38016834f480563faee8e3d`, ausschließlich Roadmap, SamAI/SamAI.
M4–M7 bleiben unreserviert.
