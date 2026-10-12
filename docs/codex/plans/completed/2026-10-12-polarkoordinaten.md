# Kartesische und polare Tupelkoordinaten

## 1. Status
Implementiert, technisch geprüft und unabhängig freigegeben; keine Veröffentlichung, Versionsreservierung oder Commits beauftragt. Geräteabnahme bleibt offen.

## 2. Ziel und Nutzerwirkung
Bestehenden Übersetzer um kartesisch↔polar ergänzen, Vektorrechner um Radius,
Winkeltupel/Vorzeichen und kartesisches Tupel aus Radius und Winkeln erweitern.

## 3. Nicht-Ziele
Neue Knotentypen, Produktionsabhängigkeiten, symbolische Trigonometrie oder Releases.
Vorhandene lokale Änderungen an Rechnerschemata und IDE-Dateien bleiben erhalten.

## 4. Untersuchter Istzustand
`ÜbersetzerKnoten.kt` besitzt gespeicherte Ziele und stabile Ein-/Ausgangs-IDs.
`VektorRechner` und `RechnerOperatorDefinitionen` liefern Kern und Registryvertrag.
Android und Desktop verwenden gemeinsame mathematische Knoten und Codecs.

## 5. Fachliche und mathematische Semantik
Für n≥2: (r,φ,θ₁,…,θₙ₋₂), r≥0, φ∈[0°,360°), θᵢ∈[0°,180°].
φ=atan2(x₂,x₁), θₖ=atan2(‖(x₁,…,xₖ₊₁)‖,xₖ₊₂). Winkel in Grad.
Unbestimmte Teilwinkel am Ursprung/auf Achsen werden 0°. R¹: (|x|,sign(x)).
Negative Radiuswerte und Winkel außerhalb des Vertrags werden abgewiesen.
Numerische Bestimmung über den Domänenauswerter; nichtreelle und unbelegte
symbolische Komponenten liefern verständliche Fehler. Berechnung mit Double,
stabiler hypot-Norm und atan2; Ergebnisse sind Näherungen.
Grundlage: [Hyperspherical Coordinates, Rutgers](https://www.physics.rutgers.edu/~shapiro/618/lects/hypersph.pdf),
mit für dieses Projekt umgekehrter Achsen-/Winkelreihenfolge und Azimut zuerst.

## 6. Daten-, Node-, Handle- und Edge-Vertrag
Keine neue Knotenart. Ziele `polar`, `kartesisch`; Eigenschaft des vorhandenen
Ziels bleibt unverändert. Parameter `uebersetzer.r1` unterscheidet bei Rückübersetzung
(r,sign) von (r,φ), Standard false. Bestehende IDs bleiben beim Zielwechsel erhalten.
Rechner-IDs `vektor.radius`, `vektor.polarwinkel`, `vektor.kartesisch`.
Eingang `kartesisch`: Tupel; Rückrichtung: `radius` und `winkel` (Zahl oder Tupel).
Ausgänge: `radius` (Zahl), `winkel` (in R¹ Zahl, sonst Tupel), `kartesisch` (Tupel).
Der effektive Winkelanschluss wird bei bekannter Quelldimension spezialisiert.

## 7. Architekturentscheidungen
Gemeinsame Umrechnung `Polarkoordinaten` im plattformfreien Rechenkern.
Inspector speichert nur Konfiguration über bestehende Kartenaktionen.
ADR beschreibt den dauerhaften Koordinatenvertrag.

## 8. Betroffene Dateien und Symbole
Kern: Polarkoordinaten, VektorRechner, RechnerOperatorDefinitionen.
Knoten: Übersetzer, Vektorkonfiguration, effektive Anschlüsse und Formeldispatcher.
Plattformen: Übersetzerinspektoren und dynamische Definitionskarten.

## 9. Meilensteine
- [x] Bestand und Konvention mit Nutzer geklärt.
- [x] Kern und Registry angebunden; erste Kompilierung erfolgreich.
- [x] Integration, Regressionstests und Plattformprüfungen erfolgreich.
- [x] Unabhängige Verifikation ohne verbleibende Blocker; Abschlussdiff geprüft.

## 10. Konkrete Umsetzungsschritte
Umrechnungen zentral implementieren, Operatorrollen registrieren, Knoten auswerten,
Inspektoroptionen und Definitionen ergänzen, Graph-/Persistenztests ausführen.

## 11. Tests und Validierung
R¹, Quadranten, Pole, Ursprung, Dimensionen 2–32, extreme endliche Werte,
ungültige Eingaben; echte Gesamtauswerter, Verbindungen, JSON und Undo/Redo.
Gezielte Tests, vollständige JVM-Tests, Repository-/Kernprüfungen, Android-Build
und Desktopkompilierung. Geräteprüfung getrennt dokumentieren.

## 12. Persistenz und Migration
Bestehende Textparameter speichern R¹-Modus. Fehlender Parameter bedeutet false.
Neue Operator-IDs normalisieren über bestehende Registry. Kein Formatwechsel nötig.

## 13. Risiken und Rückfallstrategie
Gleitkommanäherungen sind keine exakten CAS-Ergebnisse. Außerhalb des Double-Bereichs
keine stillschweigende Konvertierung. Rückbau betrifft ausschließlich neue Ziele,
Operationen und deren Tests; alte Ziele und Operationen bleiben kompatibel.

## 14. Fortschritt
Kern, Registry, Knoten, Formelpfad und beide Plattformen integriert. Elf neue Tests bestehen; Formel- und Definitionsregressionen erweitert.

## 15. Entscheidungsprotokoll
2026-10-12: Nutzer wählt Standardradius, bestehende Knoten und R¹-Vorzeichen als Zahl,
Winkel ab R² als Tupel. 180° bleibt enthalten, um die negative Achse darzustellen.
Flache Übersetzer-Rückrichtung verwendet ausdrücklich bestätigten Plan mit R¹-Schalter.

## 16. Abweichungen vom ursprünglichen Plan
Keine fachlichen Abweichungen. Keine Versionsänderung oder Branch-/Releasearbeit. Zusätzliche Dimensionprojektion und Kettentest schließen das im unabhängigen Review gefundene Verkettungsrisiko. Manuelle Geräteabnahme mangels verbundenem Gerät offen.

## 17. Ergebnis und Verifikation
- `./gradlew test :app:assembleDebug :desktopApp:compileKotlin --console=plain`: erfolgreich; 1513 vorhandene JVM-/Desktop-Testresultate ohne Fehler oder übersprungene Tests (Kern 460, Knoten 604, App 263, Editor 119, Adapter 49, TypSystem 10, Desktop 8).
- `python3 scripts/pruefe_repository.py`: erfolgreich, einschließlich Architektur und Desktop-Whitelist.
- `python3 scripts/pruefe_releaseplan.py` und `python3 scripts/pruefe_versionsfolge.py`: erfolgreich für den vorgefundenen Versionsstand 3.1.1; diese Aufgabe verändert keine Release-Metadaten.
- `python3 scripts/pruefe_kern.py`: nicht ausführbar, separates `kotlinc` fehlt im PATH. Gradle-Kerntests bestehen.
- `git diff --check`: erfolgreich.
- Unabhängiger node_verifier: finale Freigabe nach ergänzter Dimensionsprojektion durch Übersetzer und Rückrechner; Kettentest besteht.
- ADB-Geräteliste leer: keine manuelle Android-Abnahme möglich. Keine manuelle Desktop-Inspectorprüfung; automatisierte vorhandene Desktoptests bestehen. Hardware-/Gestenprüfung ist eine ausdrücklich offene Abnahmegrenze, kein behaupteter Nachweis.
- Aktueller Branch `master`, unveränderter Basiscommit `ff020f06c5f6aad7e17cef0292cce0b16384917b`. Keine Commits, kein Staging, kein Push; daher keine neue Autor-/Committeridentität. Bereits vorhandene Änderungen bleiben erhalten.
