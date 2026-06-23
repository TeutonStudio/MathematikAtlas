package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.Rechnung

open class GanzeZahlen: NatürlicheZahlen() {
    override fun zuLatex(): String = "\\mathbb{Z}"
    override fun istKommutativ(arg: Rechnung): Boolean = arg !is de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.subtraktion

    override fun istAssoziativ(arg: Rechnung): Boolean = arg !is de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.subtraktion

    override fun istDistributiv(
        äußere: Rechnung,
        innere: Rechnung
    ): Boolean = äußere is de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.multiplikation &&
            innere !is de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.multiplikation

    override fun istDistributivInvers(
        äußere: Rechnung,
        innere: Rechnung
    ): Boolean = istDistributiv(äußere, innere)

}
