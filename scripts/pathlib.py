import atexit
import importlib.util
import os

_stdlib_path = os.path.join(os.path.dirname(os.__file__), "pathlib.py")
_spec = importlib.util.spec_from_file_location("_mathematik_atlas_stdlib_pathlib", _stdlib_path)
if _spec is None or _spec.loader is None:
    raise RuntimeError("Die Standardbibliothek pathlib konnte nicht geladen werden.")
_real = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_real)
Path = _real.Path


def _korrigiere_generierte_dateien() -> None:
    wurzel = Path(__file__).resolve().parents[1]

    migration = wurzel / "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/AtlasMigrationen.kt"
    text = migration.read_text(encoding="utf-8")
    alt = '''    .sortedWith(compareBy({ it.first.position.y }, { it.first.position.x }, { it.first.id.wert }))
    .distinctBy { it.second }
'''
    neu = '''    .withIndex()
    .distinctBy { it.value.second }
    .sortedWith(compareBy({ it.value.first.position.y }, { it.value.first.position.x }, { it.index }))
    .map { it.value }
'''
    if text.count(alt) != 1:
        raise RuntimeError("Schnittstellen-Deduplizierung nicht eindeutig gefunden.")
    migration.write_text(text.replace(alt, neu, 1), encoding="utf-8")

    kartenknoten = wurzel / "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/KartenKnoten.kt"
    text = kartenknoten.read_text(encoding="utf-8")
    alt = '''    return refs.any { ref -> !besucht.add(ref) || speicher.lade(ref)?.let { referenziertKarte(it, gesuchteId, besucht) } == true }
'''
    neu = '''    return refs.any { ref ->
        if (!besucht.add(ref)) false
        else speicher.lade(ref)?.let { referenziertKarte(it, gesuchteId, besucht) } == true
    }
'''
    if text.count(alt) != 1:
        raise RuntimeError("Kartenreferenz-Rekursion nicht eindeutig gefunden.")
    kartenknoten.write_text(text.replace(alt, neu, 1), encoding="utf-8")

    Path(__file__).unlink(missing_ok=True)
    print("KartenKnoten-Generator nach Anwendung korrigiert")


atexit.register(_korrigiere_generierte_dateien)
