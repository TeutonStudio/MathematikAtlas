package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog

/**
 * App-Fassade für den plattformunabhängigen mathematischen Erstellen-Katalog.
 * App-spezifische Werkzeuge und Gruppenknoten werden erst in [AtlasZustand]
 * ergänzt; mathematische Konsolidierungsregeln gehören nicht in die App.
 */
internal fun alleMathematikKnotenVorlagen(): List<KnotenVorlage> =
    KanonischerMathematikKnotenKatalog.alle()
