package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.Rechnung

open class ReelleZahlen: RationaleZahlen() {
    override fun zuLatex(): String = "\\mathbb{R}"
    override fun istKommutativ(arg: Rechnung): Boolean = super.istKommutativ(arg)

    override fun istAssoziativ(arg: Rechnung): Boolean = super.istAssoziativ(arg)

    override fun istDistributiv(
        äußere: Rechnung,
        innere: Rechnung
    ): Boolean = super.istDistributiv(äußere, innere)

    override fun istDistributivInvers(
        äußere: Rechnung,
        innere: Rechnung
    ): Boolean = super.istDistributivInvers(äußere, innere)
}
