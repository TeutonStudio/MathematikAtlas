package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.Rechnung

open class RationaleZahlen: GanzeZahlen() {
    override fun zuLatex(): String = "\\mathbb{Q}"
}
