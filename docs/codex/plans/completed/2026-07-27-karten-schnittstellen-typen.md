# Typauswahl für Karten-Ein- und Ausgänge

Status: abgeschlossen

## Ziel und Nutzerwirkung

Die beiden öffentlichen Schnittstellenknoten einer wiederverwendbaren Karte erhalten im Inspector eine Auswahl ihres fachlichen Anschluss-Typs. Die Auswahl verändert jeweils den Anschluss `wert`: beim Karten-Eingang dessen internen Ausgang, beim Karten-Ausgang dessen internen Eingang. Daraus abgeleitete Gruppenknoten übernehmen den bereits vorhandenen Anschluss-Typ.

## Nicht-Ziele

- Keine neuen mathematischen Anschlussarten.
- Die optionale Zielmenge des Karten-Ausgangs bleibt ein fester Mengen-Eingang.
- Keine Änderung des JSON-Formats: `AnschlussDaten.art` ist bereits persistiert.

## Untersuchte Ausgangslage

- `MathematikKnotenVorlagen` erzeugt beide Schnittstellen mit `wert: mathematik.objekt`.
- `KnotenInspektorRegister` besitzt bislang keinen Inspector für diese Arten.
- Gruppenvorlagen in `AtlasZustand` leiten ihre Typen bereits aus dem Anschluss `wert` ab.
- `GraphPrüfung` prüft Typkompatibilität beim Verbinden, jedoch nicht nach nachträglichen Anschlussänderungen.

## Vertrag und Entscheidung

Alle registrierten `MathematikAnschlussArten` sind im Inspector auswählbar. Bei einer Änderung behält `wert` seine stabile Anschluss-ID und Richtung, erhält aber die neue Art. Bestehende, durch die Änderung typinkompatible Verbindungen werden entfernt; die Änderung ist ein einzelner Undo/Redo-Schritt. Damit bleiben Graph und Persistenz konsistent.

## Betroffene Dateien

- `KnotenKartenVerwalter/.../logik/GraphPrüfung.kt`
- `KnotenKartenVerwalter/.../zustand/KartenEditorZustand.kt`
- `app/.../KnotenInspektoren.kt`
- Tests des neutralen Karteneditors

## Meilensteine und Fortschritt

- [x] Neutralen Typwechsel mit Bereinigung inkompatibler Kanten implementieren und testen. Evidenz: `GraphPrüfungTest.typwechselEntferntInkompatibleKantenUndIstRückgängigMachbar`.
- [x] Inspector für beide Schnittstellen registrieren und mit dem Editor verbinden. Evidenz: beide Typ-Schlüssel sind im vorhandenen Inspector-Register eingetragen.
- [x] Gradle-Tests, Debug-Build und Abschlussdiff ausführen. Evidenz: `./gradlew test :app:assembleDebug` erfolgreich mit JDK 17.

## Persistenz und Migration

Kein neues Feld und keine Migration erforderlich. Bestehende Karten besitzen bereits `wert` mit der gespeicherten Standardart `mathematik.objekt`; ältere JSON-Formate lesen Anschlüsse unverändert.

## Risiken und Rückfallstrategie

Ein engerer Typ kann bestehende Verbindungen ungültig machen. Diese werden absichtlich entfernt statt als ungültige Kanten gespeichert; Undo stellt sie samt vorherigem Typ wieder her.

## Entscheidungsprotokoll

- 2026-07-27: Der Anschluss-Typ bleibt die fachliche Wahrheit; kein paralleler Inspector-Parameter wird eingeführt. Dadurch übernehmen Gruppenknoten und Persistenz den Wert ohne Synchronisationsrisiko.

## Ergebnis und Verifikation

- Karten-Eingang und Karten-Ausgang stellen im Inspector eine Dropdown-Auswahl aller registrierten mathematischen Anschlussarten bereit.
- Die Auswahl schreibt in den bestehenden Anschluss `wert`; dessen stabile ID und Richtung bleiben erhalten. Inkompatible Kanten werden entfernt und durch Undo wiederhergestellt.
- Persistenz bleibt kompatibel, weil der Typ bereits als `AnschlussDaten.art` gespeichert wird; es gibt keine neue Migration.
- Erfolgreich ausgeführt: `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew :KnotenKartenVerwalter:test :app:compileDebugKotlin` sowie `JAVA_HOME=/home/alex/.gradle/jdks/eclipse_adoptium-17-amd64-linux.2 ./gradlew test :app:assembleDebug`.
