from pathlib import Path

skript = Path(__file__).with_name("bootstrap_v2111.py")
if skript.exists():
    text = skript.read_text(encoding="utf-8")
    alt = '''    .sortedWith(compareBy({ it.first.position.y }, { it.first.position.x }, { it.first.id.wert }))
    .distinctBy { it.second }
'''
    neu = '''    .distinctBy { it.second }
    .sortedWith(compareBy({ it.first.position.y }, { it.first.position.x }, { it.first.id.wert }))
'''
    if text.count(alt) != 1:
        raise RuntimeError("Die Schnittstellen-Deduplizierung konnte nicht eindeutig korrigiert werden.")
    skript.write_text(text.replace(alt, neu, 1), encoding="utf-8")

Path(__file__).unlink(missing_ok=True)
