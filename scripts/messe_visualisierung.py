#!/usr/bin/env python3
"""Opt-in-Gerätemessung mit isolierter App und temporären historischen Testquellen."""
import argparse
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
PAKET = "de.TeutonStudio.MathematikKnoten.visualisierung"

def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--basis", default="c4b23c69", help="Git-Stand des ursprünglichen Renderers")
    parser.add_argument("--bericht", type=Path, default=Path("/tmp/visualisierung-messung.json"))
    args = parser.parse_args()
    revision = git("rev-parse", "--verify", "--end-of-options", args.basis + "^{commit}")
    sdk = next(line.split("=", 1)[1] for line in (ROOT / "local.properties").read_text().splitlines() if line.startswith("sdk.dir="))
    adb = str(Path(sdk) / "platform-tools/adb")
    with tempfile.TemporaryDirectory(prefix="atlas-visualisierung-") as temp:
        directory = Path(temp)
        quelle = "MathematikKnoten/src/main/kotlin/de/TeutonStudio/MathematikKnoten/visualisierung"
        for name in git("ls-tree", "-r", "--name-only", revision, quelle).splitlines():
            if not name.endswith(".kt"):
                continue
            text = git("show", revision + ":" + name).replace(PAKET, PAKET + ".baseline")
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
   dsl.defaultConfig.applicationId = 'de.TeutonStudio.MathematikAtlas.pruefung'
   dsl.defaultConfig.testApplicationId = 'de.TeutonStudio.MathematikAtlas.pruefung.test'
   dsl.sourceSets.getByName('androidTest').java.srcDir(''' + json.dumps(str(directory)) + ''')
  }
 }
}
''')
        subprocess.run([str(ROOT / "gradlew"), ":app:connectedDebugAndroidTest", "-I", str(init),
                        "-Pandroid.testInstrumentationRunnerArguments.class=de.TeutonStudio.MathematikAtlas.VisualisierungsLeistungsTest",
                        "-Pandroid.testInstrumentationRunnerArguments.atlas.measureVisualization=true", "--offline"], cwd=ROOT, check=True)
        log = subprocess.check_output([adb, "logcat", "-d", "-s", "AtlasVisualisierungMessung:I", "*:S"], text=True)
        for line in reversed(log.splitlines()):
            if ': {"geraet"' in line:
                bericht = json.loads(line[line.index("{"):])
                bericht["baselineCommit"] = revision
                args.bericht.write_text(json.dumps(bericht, indent=2, ensure_ascii=False) + "\n")
                print(f"Messbericht: {args.bericht}")
                return
        raise RuntimeError("Kein vollständiger Messbericht im Gerätelog gefunden.")

if __name__ == "__main__":
    main()
