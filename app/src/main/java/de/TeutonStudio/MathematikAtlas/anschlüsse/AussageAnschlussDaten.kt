package de.TeutonStudio.MathematikAtlas.anschlüsse

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.PullAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.RichtungsAnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.PullSystem

class AussageAnschlussDaten(
    id: String,
    kante: AnschlussKante,
    richtung: AnschlussRichtung,
): RichtungsAnschlussDaten(id,kante,richtung), PullAnschluss {
    override val cache: PullSystem.PullDaten<*> = AussageDaten()
    class AussageDaten: PullSystem.PullDaten<Aussage>() {
        override fun ausSpeicher(wert: String): Aussage {
            TODO("Not yet implemented")
        }

        override fun zuSpeicher(wert: Aussage): String {
            TODO("Not yet implemented")
        }

    }
}