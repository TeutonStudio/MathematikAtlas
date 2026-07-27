from pathlib import Path

path = Path('.github/scripts/apply_v2_1_15.py')
text = path.read_text(encoding='utf-8')

old_anchor = '''        )
    }
    is Gleichheit -> Gleichheit(ersetze(objekt.links, bindungen), ersetze(objekt.rechts, bindungen))
''',
'''
new_anchor = '''        )
    }
    is FallAusdruck -> FallAusdruck(
        wahr = ersetze(objekt.wahr, bindungen),
        aussage = ersetze(objekt.aussage, bindungen),
        lüge = ersetze(objekt.lüge, bindungen),
    )
    is Gleichheit -> Gleichheit(ersetze(objekt.links, bindungen), ersetze(objekt.rechts, bindungen))
''',
'''
if text.count(old_anchor) != 1:
    raise RuntimeError(f'Alter Anker wurde {text.count(old_anchor)}-mal gefunden.')
text = text.replace(old_anchor, new_anchor, 1)

old_result = '''    is GefilterteMenge -> filtereMenge(
        ersetze(objekt.menge, bindungen) as MengenAusdruck,
        ersetze(objekt.methode, bindungen) as Funktion,
    )
    is Gleichheit -> Gleichheit(ersetze(objekt.links, bindungen), ersetze(objekt.rechts, bindungen))
''',
'''
new_result = '''    is GefilterteMenge -> filtereMenge(
        ersetze(objekt.menge, bindungen) as MengenAusdruck,
        ersetze(objekt.methode, bindungen) as Funktion,
    )
    is FallAusdruck -> FallAusdruck(
        wahr = ersetze(objekt.wahr, bindungen),
        aussage = ersetze(objekt.aussage, bindungen),
        lüge = ersetze(objekt.lüge, bindungen),
    )
    is Gleichheit -> Gleichheit(ersetze(objekt.links, bindungen), ersetze(objekt.rechts, bindungen))
''',
'''
if text.count(old_result) != 1:
    raise RuntimeError(f'Neuer Anker wurde {text.count(old_result)}-mal gefunden.')
text = text.replace(old_result, new_result, 1)
path.write_text(text, encoding='utf-8')
