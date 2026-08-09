from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/MathematikAuswerter.kt"
text = path.read_text(encoding="utf-8")
old = '''                compareBy<Map.Entry<String, List<VariablenQuelle>>> { entry ->
                    entry.value.minOf { quelle -> k.topologischeReihenfolge[quelle.knotenId] ?: Int.MAX_VALUE }
                }.thenBy { entry -> entry.value.minOf { quelle -> quelle.knotenId.wert } },
'''
new = '''                compareBy<Map.Entry<String, List<VariablenQuelle>>> { entry ->
                    entry.value.minOf { quelle -> k.topologischeReihenfolge[quelle.knotenId] ?: Int.MAX_VALUE }
                }.thenBy { entry -> entry.value.minOf { quelle -> quelle.knotenId.wert } }
                    .thenBy { entry -> entry.value.minOf { quelle -> quelle.reihenfolge } },
'''
count = text.count(old)
if count != 1:
    raise RuntimeError(f"Erwartete genau eine Methodenparameter-Sortierung, gefunden: {count}")
path.write_text(text.replace(old, new, 1), encoding="utf-8")
print("Tupelargument-Reihenfolge explizit verankert")
