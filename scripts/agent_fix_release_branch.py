from pathlib import Path

roadmap = Path("release/roadmap.toml")
text = roadmap.read_text(encoding="utf-8")
alt = 'branch = "agent/auswerten-standardwerte"'
neu = 'branch = "release/v2.25.2-auswerten-aufloesen"'
if text.count(alt) != 1:
    raise SystemExit(f"Erwartete alte Branchangabe wurde {text.count(alt)} statt einmal gefunden.")
roadmap.write_text(text.replace(alt, neu, 1), encoding="utf-8")

Path("scripts/agent_fix_release_branch.py").unlink()
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
