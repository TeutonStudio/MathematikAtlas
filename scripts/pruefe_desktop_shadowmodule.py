#!/usr/bin/env python3
from pathlib import Path
import sys

wurzel = Path(__file__).resolve().parents[1]

# Übergangs-Whitelist bis GitHub-Issue #395 die Shadowmodule durch echte
# gemeinsame Android-/Desktop-Module ersetzt. Neue relative Produktionsquellen
# sind ausdrücklich verboten; technische Schuld soll wenigstens nicht züchten.
erwartet = {
    (
        Path("KnotenKartenVerwalterDesktop/build.gradle.kts"),
        'kotlin.srcDir("../KnotenKartenVerwalter/src/main/kotlin")',
    ),
    (
        Path("MathematikKartenAdapterDesktop/build.gradle.kts"),
        'kotlin.srcDir("../MathematikKartenAdapter/src/main/kotlin")',
    ),
    (
        Path("MathematikKnotenDesktop/build.gradle.kts"),
        'kotlin.srcDir("../MathematikKnoten/src/main/kotlin")',
    ),
    (
        Path("MathematikKnotenDesktop/build.gradle.kts"),
        'resources.srcDir("../MathematikKnoten/src/main/assets")',
    ),
}

gefunden: set[tuple[Path, str]] = set()
for build in wurzel.glob("*/build.gradle.kts"):
    relativ = build.relative_to(wurzel)
    for zeile in build.read_text(encoding="utf-8").splitlines():
        bereinigt = zeile.strip()
        if '.srcDir("../' in bereinigt:
            gefunden.add((relativ, bereinigt))

unerwartet = gefunden - erwartet
fehlend = erwartet - gefunden

if unerwartet or fehlend:
    print("Desktop-Shadowmodul-Prüfung fehlgeschlagen:")
    for datei, zeile in sorted(unerwartet, key=lambda eintrag: (str(eintrag[0]), eintrag[1])):
        print(f"- neue relative Produktionsquelle: {datei}: {zeile}")
    for datei, zeile in sorted(fehlend, key=lambda eintrag: (str(eintrag[0]), eintrag[1])):
        print(f"- erwartete Übergangsquelle fehlt oder wurde verändert: {datei}: {zeile}")
    print("Die Übergangs-Whitelist darf nur im Zuge von #395 verkleinert, nicht erweitert werden.")
    sys.exit(1)

print("Desktop-Shadowmodule entsprechen exakt der begrenzten Übergangs-Whitelist aus #395.")
