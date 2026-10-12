#!/usr/bin/env python3
"""Verbindet die Kotlin-Laufzeitinventur mit allen statischen Definitionsassets.

Inventur erzeugen:
MATLAS_DEFINITIONSAUDIT=/tmp/matlas-definitionen.tsv ./gradlew :app:testDebugUnitTest --rerun-tasks --tests '*DefinitionsInventarTest'
Bericht erzeugen:
python3 scripts/analysiere_definitionskarten.py /tmp/matlas-definitionen.tsv

Die Prioritäten sind eine explizite fachliche Heuristik, keine automatische
Behauptung der mathematischen Äquivalenz. Assetgraphen werden strukturell erkannt.
"""
from __future__ import annotations
import argparse
from collections import Counter
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
KNOTEN = ROOT / 'MathematikKnoten/src/main'

# Zerlegung, vorausgesetzte Bausteine, Voraussetzungen, Selbstreferenz, Priorität.
VORSCHLAEGE = {
 'zahl.kehrwert': ('1/a', 'Division, Konstante 1', 'a ≠ 0; bei Quaternionen Inversenseite beachten', 'frei bei primitiver Division', 'P2'),
 'zahl.quadrat': ('a·a', 'Multiplikation', 'gleicher Zahlbereich; Reihenfolge erhalten', 'frei; vorhandenen dynamischen Graph nutzen', 'vorhanden'),
 'zahl.kubik': ('(a·a)·a', 'Multiplikation', 'gleicher Zahlbereich; Reihenfolge erhalten', 'frei; vorhandenen dynamischen Graph nutzen', 'vorhanden'),
 'zahl.wurzel': ('a^(1/n) mit Zweigwahl', 'Potenz, Division', 'n ≠ 0; reeller oder komplexer Zweig ausdrücklich', 'nur wenn Potenz nicht über Wurzel definiert', 'P2'),
 'zahl.quadratwurzel': ('Wurzel(a,2)', 'allgemeine Wurzel, Konstante 2', 'nichtnegative reelle Hauptwurzel bzw. komplexer Hauptzweig', 'frei bei primitiver Wurzel', 'P2'),
 'zahl.kubikwurzel': ('Wurzel(a,3)', 'allgemeine Wurzel, Konstante 3', 'negative reelle Werte müssen reelle Kubikwurzel behalten', 'frei bei primitiver Wurzel', 'P2'),
 'zahl.logarithmus': ('ln(a)/ln(b)', 'ln, Division', 'reell a>0,b>0,b≠1; komplexe Zweige gesondert', 'ln muss Grundbaustein bleiben', 'P2'),
 'zahl.lb': ('log_2(a)', 'allgemeiner Logarithmus, 2', 'a>0 im reellen Fall', 'frei bei primitiver ln', 'P2'),
 'zahl.log10': ('log_10(a)', 'allgemeiner Logarithmus, 10', 'a>0 im reellen Fall', 'frei bei primitiver ln', 'P2'),
 'zahl.sin': ('sin_Grad ∘ q für reelles Bogenmaß', 'Übersetzer R→Grad, Grad-Sinus', 'Grad-Sinus geometrisch; komplexer Sinus separat', 'frei: Graddefinition referenziert keinen reellen Sinus', 'implementiert'),
 'zahl.cos': ('sin_Grad(q(x)+90°)', 'Winkeladdition, q, Grad-Sinus', 'reelles Bogenmaß; Winkeladdition fehlt noch als eigener Vertrag', 'frei bei primitiver Grad-Sinusdefinition', 'P2'),
 'zahl.betrag': ('Fall a≥0:a, sonst −a; komplex sqrt(Re²+Im²)', 'Fall, Ordnung, Negation; komplex Wurzel', 'reell/komplex getrennt; Quaternionnorm separat', 'Wurzel oder Norm nicht zyklisch definieren', 'P2'),
 'zahl.minimum': ('Faltung von Fall a≤b:a sonst b', 'Vergleich, Fall, Faltung', 'geordneter Bereich; leere Liste undefiniert', 'frei', 'P2'),
 'zahl.maximum': ('Faltung von Fall a≥b:a sonst b', 'Vergleich, Fall, Faltung', 'geordneter Bereich; leere Liste undefiniert', 'frei', 'P2'),
 'vektor.norm': ('sqrt(Σ conjugate(v_i)·v_i) im hermiteschen Fall', 'Indexzugriff, Konjugation, Produkt, Summe, Wurzel', 'Metrik prüfen: allgemeine Norm ist nicht immer euklidisch', 'Skalarprodukt und Norm nicht gegenseitig definieren', 'P2'),
 'vektor.radius': ('euklidische Norm des reellen Tupels', 'Komponenten, Quadrat, Summe, Wurzel', 'nichtleeres reelles Tupel; R¹ Betrag', 'frei bei primitiver Wurzel', 'P2'),
 'vektor.distanz': ('Norm(u−v) gemäß ausgewählter Metrik', 'Subtraktion, Norm bzw. L1/L∞-Faltung', 'gleiche Dimension; Metrikparameter erhalten', 'frei', 'P2'),
 'vektor.normalisierung': ('v/Norm(v)', 'Norm, Kehrwert, Skalarmultiplikation', 'v≠0; Normparameter erhalten', 'frei', 'P2'),
 'vektor.projektion': ('(<u,v>/<v,v>)·v mit vereinbarter Linearitätsseite', 'Skalarprodukt, Division, Skalarmultiplikation', 'v≠0; komplexe Konjugationskonvention beachten', 'frei', 'P2'),
 'vektor.skalarprodukt': ('Σ conjugate(u_i)·v_i oder festgelegte Gegenseite', 'Indexzugriff, Konjugation, Produkt, Summe', 'gleiche Dimension; links/rechtslinear deklarieren', 'Norm nicht als Voraussetzung verwenden', 'P2'),
 'matrix.spur': ('Σ_i A_ii', 'Diagonale, Summe', 'quadratische Matrix; Indexordnung erhalten', 'frei; vorhandenen Assetgraph wiederverwenden', 'P2'),
 'matrix.produkt': ('C_ij=Σ_k A_ik B_kj', 'Indexzugriff, Produkt, Summe, Konstruktor', 'innere Dimensionen gleich; Faktorordnung auch bei Methoden', 'frei', 'P2'),
 'matrix.transponieren': ('B_ij=A_ji', 'Indexzugriff, Konstruktor', 'Orientierung und Dimensionen tauschen; keine Konjugation', 'frei', 'P2'),
 'matrix.inverse': ('A·B=I eindeutig lösen', 'lineares Gleichungssystem, Einheitsmatrix', 'quadratisch und invertierbar; singulär undefiniert', 'Löser darf inverse Matrix nicht selbst voraussetzen', 'P3'),
}

def vorschlag(art: str, variante: str, name: str, status: str):
    op = variante.split(';')[0]
    if op.startswith('operator='): op = op.split('=', 1)[1]
    # Vorlagenparameter können vor operator weitere Felder besitzen.
    m = re.search(r'(?:^|;)operator=([^;]+)', variante)
    if m: op = m.group(1)
    if 'sinus.winkelraum=grad' in variante or variante == 'zahl.sin;grad':
        return ('y-Koordinate des Einheitskreispunkts', 'geometrischer Grundbaustein', 'Winkel modulo 360°; reell −1≤y≤1', 'kein reeller Sinus als Definitionsvoraussetzung', 'Grundbaustein')
    if op in VORSCHLAEGE: return VORSCHLAEGE[op]
    if op in {'zahl.addition', 'zahl.multiplikation', 'zahl.division', 'zahl.potenz', 'zahl.ln', 'zahl.exp'}:
        return ('bereichsspezifischer arithmetischer bzw. analytischer Grundbaustein', 'Körper-/Ringaxiome bzw. unabhängige analytische Konstruktion', 'Bereich, Nichtnullbedingungen und Zweige ausdrücklich', 'keine gegenseitige Rekursion mit abgeleiteten Operatoren', 'Grundbaustein')
    key = (art + ' ' + variante + ' ' + name).lower()
    if status == 'dynamischer Graph':
        return ('vorhandenen Definitionsgraph verwenden', 'im Graph ausgewiesene produktive Knoten', 'Signatur und Bereich der Variante prüfen', 'Graph besitzt keinen pauschalen Selbstbeweis', 'vorhanden')
    if any(x in key for x in ('svg', 'visualisier', 'karten', 'auswerten', 'allgemeinerparameter', 'variable', 'literal', 'zahlkonstante')):
        return ('Werkzeug/Quelle: Eingabe- oder Ausführungsvertrag dokumentieren', 'keine zusätzliche mathematische Zerlegung', 'Persistenz und Fehlerzustände', 'keine künstliche Selbstdefinition', 'Werkzeug')
    if op.startswith(('vektor.', 'matrix.', 'tensor.')) or any(x in key for x in ('tensor', 'vektor', 'matrix')):
        return ('indexweise Konstruktion bzw. Reduktion entsprechend Operatorvertrag', 'Indexzugriff, Konstruktor, skalare Operationen/Faltung', 'Dimensionen, Orientierung, Achsen und Reihenfolge; bei Lösungsknoten Eindeutigkeit', 'Algorithmus darf den zu definierenden Operator nicht aufrufen', 'P2' if any(x in key for x in ('addition','subtraktion','hadamard','skalieren','skalarmult','spur','diagonal','transpon')) else 'P3')
    if op.startswith(('menge.', 'aussage.')) or any(x in key for x in ('menge', 'praedikat', 'aussage', 'relation', 'quantor', 'logik')):
        return ('Mitgliedschaftsprädikat, Quantifikation oder Wahrheitstafel', 'Elementbeziehung, logische Primitive, explizite Grundmenge', 'gebundene Variablen, leere Mengen und unentscheidbare Fälle erhalten', 'Mitgliedschaft/Logik als Primitive; Definition nicht über denselben Mengenoperator', 'P3')
    if any(x in key for x in ('methode', 'integr', 'differ', 'ableit', 'grenz', 'limes', 'topolog', 'rand', 'tangent', 'orbit', 'mass')):
        return ('Signatur/Vorschrift oder charakterisierendes Prädikat mit Voraussetzungen', 'Methodenanwendung, Komposition, Quantoren bzw. Topologie', 'Existenz, Totalität, Konvergenz und Definitionsraum separat nachweisen', 'nicht denselben Analysisoperator im Definitionsgraph verwenden', 'P3')
    if op.startswith('zahl.'):
        return ('bereichsspezifische Grunddefinition oder Zerlegung in elementare Operationen', 'arithmetische/logische Primitive laut Operatorvertrag', 'Zahlbereich, Hauptzweig, Null- und Ganzzahligkeitsbedingungen', 'Primitive markieren; gegenseitige Definitionen ausschließen', 'P3')
    if 'uebersetzer' in key or 'tupel' in key or 'abflach' in key:
        return ('geordnete Indexabbildung bzw. rekursive Strukturübersetzung', 'Komponentenzugriff, Konstruktion, Bereichsübersetzung', 'heterogene Obermengen, leere Tupel und Strukturtypen erhalten', 'kein Aufruf desselben Übersetzermodus', 'P3')
    return ('axiomatische/strukturelle Definition; konkrete Zerlegung fachlich prüfen', 'die im bestehenden Vertrag benannten Grundbausteine', 'Objektsignatur und Voraussetzungen ausdrücklich', 'keine automatische Freigabe eines Selbstverweises', 'Grundbaustein prüfen')

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('inventar', type=Path)
    parser.add_argument('--ausgabe', type=Path, default=ROOT/'docs/codex/DEFINITIONSKARTEN_ANALYSE.md')
    args = parser.parse_args()
    rows = [line.split('\t') for line in args.inventar.read_text().splitlines() if line]
    assert all(len(row) == 5 for row in rows)
    katalog = (KNOTEN/'kotlin/de/TeutonStudio/MathematikKnoten/konzeptkarte/StatischeKonzeptKarten.kt').read_text()
    assets = []
    for block in re.findall(r'WissensKartenReferenz.Asset\((.*?)\n        \)', katalog, re.S):
        if 'rolle = WissensKartenRolle.Definition' not in block: continue
        filename = re.search(r'datei = "([^"]+)"', block).group(1)
        id_ = re.search(r'id = "([^"]+)"', block).group(1)
        path = KNOTEN/'assets/de/TeutonStudio/MathematikKnoten/konzeptkarte'/filename
        d = json.loads(path.read_text())
        arts = [k['art'] for k in d['knoten']]
        regel = 'konzept.regel' in arts
        selfref = id_.split('|')[0] in arts
        assets.append((id_,filename,regel,selfref,arts))
    counts = Counter(r[0] for r in rows)
    canonical = {r[1] for r in rows if r[0]=='kanonisch'}
    lines = ['# Definitionskarten: vollständige Bestandsanalyse', '', 'Stand: 2026-10-12, lokaler Arbeitsstand. Quellen sind der ausgeführte Kotlin-Resolver, der kanonische Katalog, sämtliche Definitionsvorlagen und registrierten Operatorsignaturen sowie die statischen JSON-Assets. Keine Gleichsetzung von „Graph ohne Regelknoten“ mit bewiesener Totalität.', '',
      '## Ergebnis und Reihenfolge', '',
      '- Implementiert: reeller Sinus als ausführbarer Graph R → Grad → sin_Grad → R. Die Grad-Sinusdefinition ist die geometrische Grunddefinition am Einheitskreis; dadurch entsteht kein Definitionszyklus.',
      '- P2: Logarithmusbasen, Kehrwert und Wurzelspezialisierungen; anschließend Betrag/min/max mit Bereichsbedingungen. Danach Vektornorm, Distanz, Normalisierung, Projektion und komponentenweise Operationen sowie Matrixspur, Transposition und indexiertes Produkt.',
      '- P3: Mengen-/Logikprädikate, Methoden und Analysis. Quantoren, Grundmengen, Existenz und Konvergenz müssen Teil der Karte bleiben. Inverse Matrix erst nach einem unabhängigen Löser.',
      '- Vorhandene Graphen wiederverwenden: Quadrat/Kubik dynamisch; außerdem die unten aufgelisteten historischen Graphassets. Werkzeuge und axiomatische Primitive erhalten Verträge statt künstlicher Zerlegungen.',
      '- Die folgenden weiteren Vorschläge sind analysiert, aber nicht implementiert. Familienweise Einträge benennen die noch erforderliche fachliche Spezifikation; sie sind keine Freigabe einer mathematischen Äquivalenz.', '',
      '## Abdeckung', '', f'{len(rows)} konfigurierte Einträge; {len(canonical)} unterschiedliche kanonische Knotenarten. Alle Varianten bleiben als eigene Zeilen sichtbar.', '',
      '| Quelle | Einträge |','|---|---:|']
    lines += [f'| {k} | {v} |' for k,v in sorted(counts.items())]
    lines += ['', f'Statische Definitionen: {len(assets)}; {sum(a[2] for a in assets)} mit Regelknoten, {sum(not a[2] for a in assets)} ohne Regelknoten. Diese Kennzahlen betreffen Assets; dynamische Definitionen stehen gesondert in der Matrix.', '',
      '## Vollständige Matrix', '', 'Statisch: nur artgleiche Assettreffer; historische, inzwischen konsolidierte Varianten stehen vollständig im Assetinventar. Der dynamische Resolver hat Vorrang. „statisch prüfen“ bedeutet keinen dynamischen Treffer, nicht zwingend eine fehlende Definition. P2/P3 sind Reihenfolgeempfehlungen. Jeder Vorschlag erfordert vor Umsetzung eigene Tests und mathematische Prüfung.', '',
      '| Quelle · Art · Variante | Bestand | Zerlegung | Grundbausteine | Voraussetzungen | Selbstreferenzprüfung | Priorität |',
      '|---|---|---|---|---|---|---|']
    def cell(s): return str(s).replace('|','\\|').replace('\n',' ')
    for source,art,variante,name,status in rows:
        matches=[a for a in assets if a[0].split('|')[0]==art]
        bestand=status + (f'; {len(matches)} Assetvarianten' if matches else '; kein artgleiches Asset')
        v=vorschlag(art,variante,name,status)
        lines.append('| '+' | '.join(map(cell,[f'{source}: {art} · {variante or name}',bestand,*v]))+' |')
    lines += ['', '## Vollständiges statisches Assetinventar', '', '„Selbstart“ meldet eine enthaltene Knotenart gleich der erklärten Art. Bei konfigurierbaren Rechnern sind dabei Operatorparameter zusätzlich zu prüfen; das ist ein Hinweis, kein pauschaler Zyklusbeweis.', '', '| Definition | Asset | Form | Selbstart | Verwendete Knotenarten |','|---|---|---|---|---|']
    for id_,filename,regel,selfref,arts in assets:
        link=f'../../MathematikKnoten/src/main/assets/de/TeutonStudio/MathematikKnoten/konzeptkarte/{filename}'
        lines.append('| '+' | '.join(map(cell,[id_,f'[{filename}]({link})','Regelkarte' if regel else 'Graph ohne Regelknoten','ja' if selfref else 'nein',', '.join(sorted(set(arts)))]))+' |')
    lines += ['', '## Reproduktion und Prüfgrenzen', '', 'Die Laufzeitinventur erzeugt `DefinitionsInventarTest` bei gesetztem `MATLAS_DEFINITIONSAUDIT`. `scripts/analysiere_definitionskarten.py` verbindet sie mit den echten Assets. Klassifizierung und Zerlegungsvorschläge stehen explizit im Skript und lassen sich gegen den Tabellenstand prüfen. Nicht endliche Parameterwerte, frei eingegebene Formeln und nutzereigene Karten sind keine endliche Registry und können deshalb nur durch ihre parametrischen Verträge abgedeckt werden.', '', 'Bestehende Definitionen können fachlich korrekt sein, ohne ausführbar zu sein. Ein vollständig ausgeführter Beispielgraph beweist seinerseits keine allgemeine Äquivalenz. Weitere Definitionen müssen dieselben Bereiche, Zweige, Reihenfolgen und Fehlerzustände wie der Produktknoten bewahren.', '']
    args.ausgabe.write_text('\n'.join(lines))
    print(f'{args.ausgabe}: {len(rows)} Inventareinträge, {len(assets)} Assets')

if __name__ == '__main__': main()
