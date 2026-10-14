package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Standardmengen innerhalb der reellen Zahlengeraden. */
val INTERVALL_STANDARDMENGEN = listOf(
    MengenKonstante.R, MengenKonstante.Q, MengenKonstante.Z,
    MengenKonstante.N, MengenKonstante.N0, MengenKonstante.P,
)

internal fun KnotenAuswertungsKontext.intervallGrundmenge(): MengenAusdruck {
    eingänge["grundmenge"]?.let { eingang ->
        return eingang.objekt as? MengenAusdruck
            ?: error("Der Eingang „grundmenge“ benötigt eine Menge.")
    }
    val id = knoten.parameter["standardMenge"] ?: "R"
    val standard = MengenKonstante.vonId(id)
    require(standard != null && standard in INTERVALL_STANDARDMENGEN) {
        "Ungültige Intervall-Standardmenge: $id"
    }
    return standard.auswerten()
}

/** Die Vergleichsrelationen bleiben auch mit frei verbundenen Mengen reell definiert. */
internal fun intervallEinschränken(intervall: MengenAusdruck, grundmenge: MengenAusdruck): MengenAusdruck =
    when {
        intervall == LeereMenge || grundmenge == ReelleZahlen -> intervall
        grundmenge == LeereMenge -> LeereMenge
        else -> Schnitt(listOf(intervall, grundmenge))
    }
