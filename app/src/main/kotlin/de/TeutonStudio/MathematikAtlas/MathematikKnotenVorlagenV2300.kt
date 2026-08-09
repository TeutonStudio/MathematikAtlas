package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.VektorKonstruktorV2300Vorlagen

/**
 * App-seitiger Katalogadapter für v2.30.0. Er hält historische Typen ladbar,
 * entfernt sie aber aus dem Erstellen-Dialog und ersetzt das Zeile/Spalte-Paar
 * durch den kanonischen orientierbaren Vektorkonstruktor.
 */
internal fun alleMathematikKnotenVorlagen(): List<KnotenVorlage> {
    val basis = de.TeutonStudio.MathematikKnoten.alleMathematikKnotenVorlagen()
    return basis.filterNot { vorlage ->
        vorlage.art in setOf("mathematik.vektor", "mathematik.zeilenVektor")
    } + VektorKonstruktorV2300Vorlagen.standard
}
