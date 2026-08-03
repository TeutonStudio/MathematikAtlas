#!/usr/bin/env python3
from pathlib import Path

pfad = Path("MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/UniversellerZahlenRechnerKnoten.kt")
text = pfad.read_text(encoding="utf-8")

aenderungen = {
    'UniversellerZahlenOperator.QUADRAT -> unär { Potenz(it, RationaleZahl.von(2)) }':
        'UniversellerZahlenOperator.QUADRAT -> unär(erzeuge = { Potenz(it, RationaleZahl.von(2)) })',
    'UniversellerZahlenOperator.KUBIK -> unär { Potenz(it, RationaleZahl.von(3)) }':
        'UniversellerZahlenOperator.KUBIK -> unär(erzeuge = { Potenz(it, RationaleZahl.von(3)) })',
    '''UniversellerZahlenOperator.KUBIKWURZEL -> unär {
            Potenz(it, RationaleZahl.von(1, 3))
        }''':
        '''UniversellerZahlenOperator.KUBIKWURZEL -> unär(
            erzeuge = { Potenz(it, RationaleZahl.von(1, 3)) },
        )''',
    'Argument(it.imaginärteil, it.realteil)': 'Argument(it)',
}

for alt, neu in aenderungen.items():
    anzahl = text.count(alt)
    if anzahl != 1:
        raise SystemExit(f"Erwartete genau einen Treffer, gefunden {anzahl}: {alt!r}")
    text = text.replace(alt, neu)

pfad.write_text(text, encoding="utf-8")
