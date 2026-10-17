#!/usr/bin/env python3
"""Opt-in-Gerätemessung mit isolierter App und temporären historischen Testquellen."""
import argparse
import hashlib
import uuid
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
PAKET = "de.TeutonStudio.MathematikKnoten.visualisierung"

def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()

def berichte_aus_log(log, kennung):
    """Nur vollständige Renderer-/Filterberichte derselben Instrumentierung lesen."""
    berichte = {}
    for line in log.splitlines():
        for tag in ("AtlasVisualisierungMessung", "AtlasFilterMessung"):
            marker = tag + ": "
            if marker not in line:
                continue
            try:
                bericht = json.loads(line.split(marker, 1)[1])
            except json.JSONDecodeError:
                continue
            if bericht.get("laufKennung") == kennung and "geraet" in bericht:
                berichte[tag] = bericht
    if len(berichte) != 2:
        raise RuntimeError("Kein vollständiges Renderer-/Filterberichtspaar mit aktueller Laufkennung; Gerätelog möglicherweise gekürzt")
    bericht = berichte["AtlasVisualisierungMessung"]
    bericht["filterMessung"] = berichte["AtlasFilterMessung"]
    return bericht

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--basis", default="c4b23c69", help="Git-Stand des ursprünglichen Renderers")
    parser.add_argument("--bericht", type=Path, default=Path("/tmp/visualisierung-messung.json"))
    parser.add_argument("--basis-quellen", type=Path, help="Gesicherter Visualisierungs-Quellordner eines uncommitteten Ausgangsstands")
    parser.add_argument("--wiederholungen", type=int, default=3, help="Anzahl identischer Geräteläufe (Standard: 3)")
    args = parser.parse_args()
    if args.wiederholungen < 1:
        parser.error("Mindestens ein Messlauf erforderlich")
    if args.basis_quellen is not None and not args.basis_quellen.is_dir():
        parser.error("Der Ausgangsquellordner fehlt")
    revision = None if args.basis_quellen else git("rev-parse", "--verify", "--end-of-options", args.basis + "^{commit}")
    sdk = next(line.split("=", 1)[1] for line in (ROOT / "local.properties").read_text().splitlines() if line.startswith("sdk.dir="))
    adb = str(Path(sdk) / "platform-tools/adb")
    with tempfile.TemporaryDirectory(prefix="atlas-visualisierung-") as temp:
        directory = Path(temp)
        quelle = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/visualisierung"
        if args.basis_quellen:
            quellen = [(str(p.relative_to(args.basis_quellen)), p.read_text()) for p in sorted(args.basis_quellen.rglob("*.kt"))]
        else:
            quellen = [(name, git("show", revision + ":" + name)) for name in git("ls-tree", "-r", "--name-only", revision, quelle).splitlines() if name.endswith(".kt")]
        hashes = {name: hashlib.sha256(text.encode()).hexdigest() for name, text in quellen}
        for name, original in quellen:
            text = original.replace(PAKET, PAKET + ".baseline")
            # Nur expliziter Cast wegen der zusätzlichen Testmodulgrenze, keine Semantikänderung.
            if name.endswith("/VisualisierungsSampler.kt"):
                text = text.replace("abbild.menge.mengen", "(abbild.menge as KartesischesProdukt).mengen")
            (directory / Path(name).name).write_text(text + "\n")
        (directory / "BaselineDetailRenderer.kt").write_text('''package de.TeutonStudio.MathematikKnoten.visualisierung.baseline.ui
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
class BaselineDetailRenderer(private val ergebnisFür: (KnotenDaten)->KnotenAuswertungsErgebnis?) : KnotenRenderer {
 @Composable override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen) {
  VisualisierungsDetailansicht(knoten,ergebnisFür(knoten),{aktionen.eigenschaftenErsetzen(it)},Modifier.fillMaxSize())
 }
}
''')
        init = directory / "messung.init.gradle"
        init.write_text('''allprojects { p ->
 if (p.path == ':app') p.plugins.withId('com.android.application') {
  p.extensions.getByName('androidComponents').finalizeDsl { dsl ->
   dsl.defaultConfig.applicationId = 'de.TeutonStudio.MathematikAtlas.visualoptimierung'
   dsl.defaultConfig.testApplicationId = 'de.TeutonStudio.MathematikAtlas.visualoptimierung.test'
   dsl.sourceSets.getByName('androidTest').java.srcDir(''' + json.dumps(str(directory)) + ''')
  }
 }
}
''')
        läufe = []
        for nummer in range(args.wiederholungen):
            kennung = str(uuid.uuid4())
            subprocess.run([str(ROOT / "gradlew"), ":app:connectedDebugAndroidTest", "-I", str(init),
                            "-Pandroid.testInstrumentationRunnerArguments.class=de.TeutonStudio.MathematikAtlas.VisualisierungsLeistungsTest",
                            "-Pandroid.testInstrumentationRunnerArguments.atlas.measureVisualization=true",
                            "-Pandroid.testInstrumentationRunnerArguments.atlas.measureRun=" + kennung,
                            "--offline"], cwd=ROOT, check=True)
            # UTP entfernt die isolierte App nach dem Test; deren Cache ist dann
            # nicht mehr über run-as erreichbar. Beide Logberichte müssen vollständig
            # und durch dieselbe Laufkennung eindeutig zugeordnet sein.
            log = subprocess.check_output([adb, "logcat", "-d", "-s",
                "AtlasVisualisierungMessung:I", "AtlasFilterMessung:I", "*:S"], text=True)
            bericht = berichte_aus_log(log, kennung)
            läufe.append(bericht)
            ergebnis = dict(läufe[-1], baselineCommit=revision, baselineQuellenSha256=hashes, läufe=läufe)
            args.bericht.parent.mkdir(parents=True, exist_ok=True)
            args.bericht.write_text(json.dumps(ergebnis, indent=2, ensure_ascii=False) + "\n")
            print(f"Messbericht {nummer + 1}/{args.wiederholungen}: {args.bericht}")

if __name__ == "__main__":
    main()
