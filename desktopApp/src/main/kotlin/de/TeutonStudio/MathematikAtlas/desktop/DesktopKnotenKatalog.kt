package de.TeutonStudio.MathematikAtlas.desktop

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage

internal fun desktopKnotenVorlagen(): List<KnotenVorlage> =
    de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog.alle()
