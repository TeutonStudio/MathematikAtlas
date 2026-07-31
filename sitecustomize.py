from pathlib import Path

skript = Path("scripts/bootstrap_v2111.py")
if skript.exists():
    text = skript.read_text(encoding="utf-8")

    alte_reihenfolge = '''    .sortedWith(compareBy({ it.first.position.y }, { it.first.position.x }, { it.first.id.wert }))
    .distinctBy { it.second }
'''
    neue_reihenfolge = '''    .withIndex()
    .distinctBy { it.value.second }
    .sortedWith(compareBy({ it.value.first.position.y }, { it.value.first.position.x }, { it.index }))
    .map { it.value }
'''
    if text.count(alte_reihenfolge) != 1:
        raise RuntimeError("Die Schnittstellen-Deduplizierung konnte nicht eindeutig korrigiert werden.")
    text = text.replace(alte_reihenfolge, neue_reihenfolge, 1)

    alte_rekursion = '''    return refs.any { ref -> !besucht.add(ref) || speicher.lade(ref)?.let { referenziertKarte(it, gesuchteId, besucht) } == true }
'''
    neue_rekursion = '''    return refs.any { ref ->
        if (!besucht.add(ref)) false
        else speicher.lade(ref)?.let { referenziertKarte(it, gesuchteId, besucht) } == true
    }
'''
    if text.count(alte_rekursion) != 1:
        raise RuntimeError("Die Kartenreferenz-Rekursion konnte nicht eindeutig korrigiert werden.")
    text = text.replace(alte_rekursion, neue_rekursion, 1)

    skript.write_text(text, encoding="utf-8")
    print("Root-Hook v2.11.1 angewendet")

Path(__file__).unlink(missing_ok=True)
