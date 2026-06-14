package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.Rechnung

open class GanzeZahlen: NatürlicheZahlen() {
    override fun istKommutativ(arg: Rechnung): Boolean {
        TODO("Not yet implemented")
    }

    override fun istAssoziativ(arg: Rechnung): Boolean {
        TODO("Not yet implemented")
    }

    override fun istDistributiv(
        äußere: Rechnung,
        innere: Rechnung
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun istDistributivInvers(
        äußere: Rechnung,
        innere: Rechnung
    ): Boolean {
        TODO("Not yet implemented")
    }

}