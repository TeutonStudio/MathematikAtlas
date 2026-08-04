from pathlib import Path

pfad = Path("MathematikKnoten/src/test/kotlin/de/TeutonStudio/MathematikKnoten/AuswertenTest.kt")
text = pfad.read_text(encoding="utf-8")
alt_eingang = 'KnotenAuswertungsKontext(auswerten, mapOf("objekt" to BedingterWert(aussage)), RechenKontext()),'
neu_eingang = 'KnotenAuswertungsKontext(auswerten, mapOf("term" to BedingterWert(aussage)), RechenKontext()),'
alt_ausgang = 'val auswertung = assertIs<WahrheitsKonstante>(ergebnis.ausgaben.getValue("wert").objekt)'
neu_ausgang = 'val auswertung = assertIs<WahrheitsKonstante>(ergebnis.ausgaben.getValue("term").objekt)'
if text.count(alt_eingang) != 2:
    raise SystemExit(f"Erwartete alte Eingabestelle wurde {text.count(alt_eingang)} statt zweimal gefunden.")
if text.count(alt_ausgang) != 2:
    raise SystemExit(f"Erwartete alte Ausgabestelle wurde {text.count(alt_ausgang)} statt zweimal gefunden.")
text = text.replace(alt_eingang, neu_eingang)
text = text.replace(alt_ausgang, neu_ausgang)
pfad.write_text(text, encoding="utf-8")

Path("scripts/agent_fix_auswerten_tests.py").unlink()
Path(".github/workflows/build.yml").write_text('''name: Android-Build

on:
  push:
  pull_request:

jobs:
  pruefen:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: android-actions/setup-android@v3
      - name: Android SDK 36 installieren
        run: sdkmanager "platforms;android-36" "build-tools;35.0.0"
      - name: Architektur prüfen
        run: python3 scripts/pruefe_architektur.py
      - name: Bauen und testen
        run: ./gradlew --stacktrace test :app:assembleDebug
''', encoding="utf-8")
