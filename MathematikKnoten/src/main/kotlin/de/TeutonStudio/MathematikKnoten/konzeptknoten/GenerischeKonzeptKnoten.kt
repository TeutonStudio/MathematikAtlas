package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensEintrag

/**
 * Vollständiger Übergangsadapter für noch nicht in eine eigene Konzeptdatei
 * überführte Knotenvorlagen. Jeder bisherige Bibliothekseintrag behält dabei
 * seine historische stabile ID.
 */
object GenerischeKonzeptKnoten {
    fun erstelle(vorlagen: List<KnotenVorlage>): List<WissensEintrag> =
        vorlagen.map(::einzelnesVorlagenKonzept)
}
