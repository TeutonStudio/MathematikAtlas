# Extremwert-Knoten (Maximum/Minimum)

Status: abgeschlossen

## Ziel und Nutzerwirkung

Der Einfügedialog bietet getrennte Vorlagen für Maximum und Minimum. Beide erzeugen den persistierten Typ `mathematik.extremwert` mit mindestens zwei dynamischen Zahl-Eingängen und einem Zahl-Ausgang. Die Auswertung ist auf nachweisbar reelle Eingaben beschränkt.

## Nicht-Ziele

Keine Umschaltung zwischen Maximum und Minimum im Inspector, keine Näherungsarithmetik und keine Ordnung komplexer Zahlen.

## Istzustand und Vertrag

`MathematikKnotenVorlagen`, `StandardMathematikAuswerter` und `AtlasZustand.aktualisiereAssoziativeKnoten` sind die bestehenden Erweiterungspunkte. Eingänge `a`, `b` sind Zahl-Eingänge, links, variadisch und nach Reihenfolge sortiert; Ausgang `wert` ist Zahl, rechts. Der Modus ist der serialisierbare Parameter `modus` mit `maximum` oder `minimum`.

## Fachliche Semantik

Die Kernfabriken benötigen mindestens zwei Argumente, flachen gleiche Extremwertausdrücke ab, vereinfachen ausschließlich rationale Eingaben exakt und behalten sonst einen symbolischen Ausdruck. Ein zentraler Runtime-Nachweis akzeptiert rationale Zahlen, `pi`, `e`, Variablen mit Wertebereich N/Z/Q/R und reellheitserhaltende Zusammensetzungen. Nicht nachweisbar reelle und komplexe Eingaben werden abgelehnt.

## Meilensteine und Fortschritt

- [x] Kern-Ausdrücke, Reellheitsmetadaten und Auswertung ergänzen. `Maximum`/`Minimum`, konservativer Reellheitsnachweis und Registry-Auswerter umgesetzt.
- [x] Vorlagen, Renderer/Inspector und assoziative Migration anbinden. Beide Katalogeinträge verwenden `mathematik.extremwert`; der Modus ist im Inspector nur lesbar.
- [x] Unit-Tests und Repository-Prüfungen ausführen. `./gradlew test :app:assembleDebug` sowie `python3 scripts/pruefe_repository.py` erfolgreich.
- [x] Unabhängige Verifikation dokumentieren und Plan abschließen. `node_verifier` hat den finalen Diff ohne offene blockierende oder hohe Befunde abgenommen.

## Persistenz und Migration

`modus`, `festeEingänge` und `operatorAnzeige` nutzen die bestehende JSON-Persistenz. Die assoziative Migration normalisiert Extremwertinstanzen auf mindestens zwei feste, erweiterbare Eingänge. Runtime-Reellheitsmetadaten werden nicht gespeichert.

## Entscheidungen

- 2026-07-27: Ein Typ mit zwei Vorlagen. Das bewahrt einen gemeinsamen Auswerter bei nutzerverständlichen Einfügeeinträgen.
- 2026-07-27: Inspector zeigt den Modus nur lesend. So bleiben Vorlagenname und Modus konsistent.

## Ergebnis und Verifikation

Die Tests decken exakte rationale Maxima/Minima einschließlich Gleichständen, symbolische reelle Variablen, komplexe und partielle Ablehnung, Registry/Handle-Vertrag, dynamische Eingänge mit Undo/Redo, JSON-Roundtrip und assoziative Migration ab. `./gradlew test --rerun-tasks`, `./gradlew :MathematikRechenSystem:test :MathematikKnoten:test :app:assembleDebug --rerun-tasks` sowie `python3 scripts/pruefe_repository.py` waren mit JDK 17 erfolgreich; `git diff --check` ist sauber. Kein Emulator-/Gerätetest wurde ausgeführt.
