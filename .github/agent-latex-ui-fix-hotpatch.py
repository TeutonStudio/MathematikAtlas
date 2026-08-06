from pathlib import Path

pfad = Path(".github/agent-latex-ui-fix.py")
text = pfad.read_text(encoding="utf-8")
ersetzungen = {
    "val linkeKlammer": "val linksKlammer",
    "val rechteKlammer": "val rechtsKlammer",
    "$linkeKlammer": "$linksKlammer",
    "$rechteKlammer": "$rechtsKlammer",
}
for alt, neu in ersetzungen.items():
    anzahl = text.count(alt)
    if anzahl != 1:
        raise SystemExit(f"Reparaturskript: erwartete genau einen Treffer für {alt!r}, gefunden: {anzahl}")
    text = text.replace(alt, neu)
pfad.write_text(text, encoding="utf-8")
