# Teststrategie für Knoten

## Grundsatz

Teste die fachliche Bedeutung auf der niedrigsten sinnvollen Ebene. Compose-Tests ersetzen keine Tests der mathematischen Semantik, und ein erfolgreicher JVM-Test ersetzt keinen Laufzeittest einer Geste auf Emulator oder Gerät.

## 1. Domänentests

Für mathematische Operationen im `MathematikRechenSystem`:

- gültige Standardfälle,
- Grenzfälle,
- leere Eingaben,
- undefinierte Zustände,
- unbekannte und unentscheidbare Zustände,
- Typinkompatibilität,
- relevante algebraische Eigenschaften,
- exakte und deterministische Ausgabe.

Diese Tests müssen ohne Android und Compose ausführbar sein.

## 2. Karteneditor- und Graphvertragstests

Im `KnotenKartenVerwalter` prüfen:

- unveränderliche Kartenzustände nach Aktionen,
- Richtung und Anschlussart-Kompatibilität,
- stabile Anschluss- und Verbindungsreferenzen,
- Eingangskardinalität,
- Zyklusverhinderung,
- dynamische Anschlüsse,
- Auswahl und visuelle Gruppen,
- atomare Undo/Redo-Schritte.

Tests dieses Moduls dürfen keine mathematischen Knotenschlüssel oder app-spezifischen Parameterkonventionen voraussetzen.

## 3. Knotenvorlagen- und Auswertertests

Im `MathematikKnoten` und `MathematikKartenAdapter` prüfen:

- Standarddaten einer Vorlage,
- neue Anschluss-IDs pro Instanz,
- Registrierung im Vorlagenkatalog,
- Registrierung und Auflösung des Auswerters,
- Einsammeln der Eingänge anhand der Anschlussnamen,
- fachlich gültige Ausgabe,
- Fehlerzustände bei fehlenden oder falschen Eingaben,
- Cache- und Teilgraphverhalten, sofern betroffen.

## 4. Integrationstests

Prüfen:

- Verbindung kompatibler Knoten,
- Ablehnung inkompatibler Verbindungen,
- Ersetzung belegter Eingänge,
- Propagation von Änderungen,
- Inspector → Kartenaktion → Knotendaten → Auswertung,
- Laden und Wiederherstellen,
- Kopieren, Duplizieren und Löschen,
- Undo/Redo,
- Gruppenknoten und feste Kartenverweise.

## 5. Darstellungstests

Nur für relevantes Verhalten:

- Anschlüsse werden mit korrekter ID, Richtung und Position gerendert,
- nativer LaTeX- oder Formeltext entspricht den Daten,
- Fehlerzustand ist sichtbar,
- Inspector-Felder besitzen korrekte Werte und Validierung,
- `KnotenInteraktionsModus` trennt Inhaltsgesten vom Knotenziehen,
- Klick-, Halte-, Drag-, Transformations- und Kontextgesten lösen die vorgesehene Aktion aus,
- Semantik- und Accessibility-Beschreibungen sind vorhanden, soweit die Komponente sie anbietet.

Vermeide fragile Tests, die nur interne Compose-Hierarchien oder zufällige Layoutdetails festschreiben.

## 6. Persistenztests

Wenn Karten- oder Knotendaten gespeichert werden:

- JSON-Roundtrip,
- Laden älterer Formatversionen,
- Defaultwerte fehlender Felder,
- stabile Anschluss- und Verbindungsreferenzen,
- robuste Behandlung unbekannter Typen,
- Migration geänderter Anschlussstrukturen,
- Kartenversionsregeln bei Gruppenknoten,
- Ausschluss von Laufzeit- und Cachedaten.

## 7. Laufzeit- und Smoke-Tests

Für Änderungen an Compose-Interaktionen oder Android-Pfaden dokumentiere getrennt:

- JVM- und Unit-Teststatus,
- erfolgreichen APK-Build,
- Emulatorprüfung,
- Prüfung auf physischem Gerät.

Behaupte „funktionsfähig“ für eine neue Geste oder einen Dialog nur, wenn die Interaktion tatsächlich ausgeführt wurde. Kompilieren ist eine nützliche Eigenschaft, aber noch keine Benutzererfahrung.

## 8. Prüfbefehle

Codex bestimmt Befehle aus:

1. Gradle-Wrapper und `*.gradle.kts`,
2. Skripten unter `scripts/`,
3. vorhandenen Testquellen,
4. CI-Konfiguration.

Der derzeit übliche Prüfpfad ist:

```bash
python3 scripts/pruefe_repository.py
python3 scripts/pruefe_kern.py
./gradlew test
./gradlew :app:assembleDebug
```

Bevorzugte Reihenfolge:

1. gezielte Modul- oder Testklassentests,
2. vollständige JVM-Tests,
3. Repository- und Architekturprüfung,
4. Android-Lint, sofern im Bestand vorhanden,
5. Debug- oder Produktions-Build,
6. Compose-, Emulator- oder Geräteprüfung, sofern für die Änderung erforderlich und verfügbar.

Ein nicht vorhandener Task wird nicht erfunden. Ein wegen externer Umgebung nicht ausführbarer Test wird mit konkretem Grund dokumentiert.

## 9. Mindestabdeckung für einen neuen Knotentyp

- mindestens ein fachlicher Erfolgstest,
- mindestens ein ungültiger oder unvollständiger Zustand,
- Anschluss- und Registrierungsvertrag,
- Auswerterregistrierung,
- Inspectoränderung, falls vorhanden,
- Persistenz-Roundtrip, falls vorhanden,
- Migrationstest, falls das Schema geändert wurde,
- Darstellungstest bei spezialisiertem Renderer, soweit die vorhandene Umgebung dies unterstützt.