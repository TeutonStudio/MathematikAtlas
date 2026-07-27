# Öffentliche Karten-Schnittstellen nach Name

Status: abgeschlossen

## Ziel und Nutzerwirkung

Karten-Eingang und Karten-Ausgang zeigen im Inspector den öffentlichen Namen und Typ. Beide besitzen genau den Anschluss `wert`. Eine daraus erzeugte wiederverwendbare Karte stellt pro Richtung genau einen Anschluss für jeden unterschiedlichen öffentlichen Namen bereit.

## Vertrag

- Name: `parameter["name"]`, bei leerem oder fehlendem Wert der bestehende Knotenname.
- Typ: `AnschlussDaten.art` des einzigen Anschlusses `wert`.
- Karten-Eingang: ein Ausgang `wert`; Karten-Ausgang: ein Eingang `wert`.
- Bei mehrfach vorkommendem Namen bleibt pro Richtung der zuerst gelesene Schnittstellenknoten maßgeblich; Ein- und Ausgänge werden getrennt behandelt.

## Migration und Auswirkungen

Der frühere Ausgang `zielmenge` wird beim Öffnen gespeicherter Karten entfernt, einschließlich seiner Kanten. Dadurch entfällt die automatische Erzeugung von Methodenkarten, die diesen zweiten Anschluss benötigte. Der reguläre Gruppenknoten und seine Auswertung bleiben erhalten.

## Meilensteine

- [x] Ein-Anschluss-Vertrag, Migration und Gruppenableitung implementieren. Evidenz: die Vorlage und die Lade-Migration lassen am Karten-Ausgang nur `wert` bestehen.
- [x] Inspector, Auswertung und Tests anpassen. Evidenz: der Inspector bearbeitet Name und Typ; Tests decken Ein-Anschluss-Vertrag, Namens-Eindeutigkeit und Migration ab.
- [x] Vollständige Tests, Debug-Build und Diff prüfen. Evidenz: `./gradlew test :app:assembleDebug` erfolgreich mit JDK 17.

## Ergebnis und Verifikation

- Öffentliche Karten-Eingänge und -Ausgänge besitzen jeweils genau einen typisierten Anschluss `wert` und den editierbaren öffentlichen Namen `parameter["name"]`.
- Die Gruppenvorlage erzeugt Ein- und Ausgänge getrennt und behält je Richtung nur den ersten Schnittstellenknoten eines öffentlichen Namens.
- Alte `zielmenge`-Anschlüsse an Karten-Ausgängen samt Kanten werden beim Öffnen entfernt. Die frühere automatische Methodenvorlage entfällt dadurch.
- Erfolgreich ausgeführt: `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew test :app:assembleDebug`.
