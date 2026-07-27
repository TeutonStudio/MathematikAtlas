from pathlib import Path

path = Path(".github/scripts/apply_v2_1_15.py")
lines = path.read_text(encoding="utf-8").splitlines(keepends=True)

start = next(
    index
    for index, line in enumerate(lines)
    if line == "replace_once(\n"
    and index + 2 < len(lines)
    and "Funktionen.kt" in lines[index + 1]
    and "DefinierteMenge" in "".join(lines[index:index + 20])
)
end = next(
    index
    for index in range(start + 1, len(lines))
    if lines[index] == "replace_once(\n"
)

replacement = """replace_once(
    \"MathematikRechenSystem/src/main/kotlin/de/TeutonStudio/MathematikRechenSystem/kern/Funktionen.kt\",
    '''    is DefinierteMenge -> {
        val gebundeneNamen = objekt.variablen.map { it.variable.name }.toSet()
        val freieBindungen = bindungen - gebundeneNamen
        objekt.copy(
            variablen = objekt.variablen.map { it.copy(grundMenge = ersetze(it.grundMenge, freieBindungen) as MengenAusdruck) },
            bedingung = ersetze(objekt.bedingung, freieBindungen),
        )
    }
    is FallAusdruck -> FallAusdruck(
        wahr = ersetze(objekt.wahr, bindungen),
        aussage = ersetze(objekt.aussage, bindungen),
        lüge = ersetze(objekt.lüge, bindungen),
    )
    is Gleichheit -> Gleichheit(ersetze(objekt.links, bindungen), ersetze(objekt.rechts, bindungen))
''',
    '''    is DefinierteMenge -> {
        val gebundeneNamen = objekt.variablen.map { it.variable.name }.toSet()
        val freieBindungen = bindungen - gebundeneNamen
        objekt.copy(
            variablen = objekt.variablen.map { it.copy(grundMenge = ersetze(it.grundMenge, freieBindungen) as MengenAusdruck) },
            bedingung = ersetze(objekt.bedingung, freieBindungen),
        )
    }
    is GefilterteMenge -> filtereMenge(
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
)

""".splitlines(keepends=True)

lines[start:end] = replacement
text = "".join(lines)
replacements = {
    r'        return "\\left\\{${parameter.zuLatex()}\\in${menge.zuLatex()}\\mid ${bedingung.zuLatex()}\\right\\}"':
        r'        return "\\\\left\\\\{${parameter.zuLatex()}\\\\in${menge.zuLatex()}\\\\mid ${bedingung.zuLatex()}\\\\right\\\\}"',
    r'                MathematikAnschlussArten.Menge.id -> BenannteMenge("mengen_$name", "\\mathcal{P}(\\mathcal{U})")':
        r'                MathematikAnschlussArten.Menge.id -> BenannteMenge("mengen_$name", "\\\\mathcal{P}(\\\\mathcal{U})")',
    r'                else -> BenannteMenge("werte_$name", "\\mathcal{W}_{${name}}")':
        r'                else -> BenannteMenge("werte_$name", "\\\\mathcal{W}_{${name}}")',
}
for old, new in replacements.items():
    if text.count(old) != 1:
        raise RuntimeError(f"Escape-Zeile wurde {text.count(old)}-mal gefunden: {old}")
    text = text.replace(old, new, 1)
path.write_text(text, encoding="utf-8")
