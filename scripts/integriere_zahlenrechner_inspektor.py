#!/usr/bin/env python3
from pathlib import Path


def ersetze(pfad: str, alt: str, neu: str, anzahl: int = 1) -> None:
    datei = Path(pfad)
    text = datei.read_text(encoding="utf-8")
    treffer = text.count(alt)
    if treffer != anzahl:
        raise SystemExit(f"{pfad}: erwartet {anzahl} Treffer, gefunden {treffer}: {alt!r}")
    datei.write_text(text.replace(alt, neu, anzahl), encoding="utf-8")


inspektoren = "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/KnotenInspektoren.kt"
ersetze(
    inspektoren,
    "import de.TeutonStudio.MathematikKnoten.WertebereichKonfiguration\n",
    """import de.TeutonStudio.MathematikKnoten.WertebereichKonfiguration
import de.TeutonStudio.MathematikKnoten.ZAHLENRECHNER_ART
""",
)
ersetze(
    inspektoren,
    '''    fun anschlussArt(verweis: AnschlussVerweis, art: AnschlussArtId)
}''',
    '''    fun anschlussArt(verweis: AnschlussVerweis, art: AnschlussArtId)
    fun knoten(knoten: KnotenDaten)
}''',
)
ersetze(
    inspektoren,
    '''        "mathematik.matrixdiagonale" to MatrixdiagonaleInspektor,
        GeometrieTeilobjektTyp.Ecke.knotenArt to GeometrieTeilobjektInspektor,''',
    '''        "mathematik.matrixdiagonale" to MatrixdiagonaleInspektor,
        ZAHLENRECHNER_ART to ZahlenRechnerInspektor,
        GeometrieTeilobjektTyp.Ecke.knotenArt to GeometrieTeilobjektInspektor,''',
)

fenster = "app/src/main/kotlin/de/TeutonStudio/MathematikAtlas/KnotenInspektorFenster.kt"
ersetze(
    fenster,
    '''                        override fun anschlussArt(verweis: AnschlussVerweis, art: AnschlussArtId) {
                            zustand.editor.ändereAnschlussArt(verweis, art)
                        }
                    },''',
    '''                        override fun anschlussArt(verweis: AnschlussVerweis, art: AnschlussArtId) {
                            zustand.editor.ändereAnschlussArt(verweis, art)
                        }
                        override fun knoten(knoten: KnotenDaten) {
                            zustand.editor.führeAus(KartenAktion.KnotenErsetzen(knoten))
                        }
                    },''',
)
