# Term zu Methode – typisierte Zielmenge

Status: `[x] abgeschlossen`

## Ziel

Die Zielmenge von `mathematik.termZuMethode` wird vollständig aus Term und Wertebereich konstruiert; die Inspector-Auswahl entfällt.

## Umsetzung

- Der Rechenkern leitet Zielmengen für Zahlen, Aussagen, Mengen, Tupel, Vektoren und Matrizen ab.
- Neue Mengenmodelle repräsentieren Tupel-, variable Folgen-, Vektor- und Matrizenräume.
- Allgemeine Parameter speichern einen rekursiven typisierten Wertebereich als `KnotenEigenschaft`.
- Migrationen entfernen alte Zielmengenparameter und überführen alte numerische allgemeine Parameterwertebereiche.

## Verifikation

- Erfolgreich: `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew :MathematikRechenSystem:test :MathematikKnoten:test :app:testDebugUnitTest`.
- Erfolgreich: `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew test :app:assembleDebug`.
- Erfolgreich: `python3 scripts/pruefe_repository.py` und `git diff --check`.

## Risiken

Die Inferenz ist bewusst konservativ und berechnet keine exakte Bildmenge. Zielmengen für Funktions- und Mächtigkeitsobjekte sind nicht Teil dieses Vertrags.
