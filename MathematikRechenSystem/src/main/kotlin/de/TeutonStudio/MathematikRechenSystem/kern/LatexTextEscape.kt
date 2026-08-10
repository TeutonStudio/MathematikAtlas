package de.TeutonStudio.MathematikRechenSystem.kern

/** Escaped internen Bezeichnertext für kontrollierte LaTeX-Beschriftungen. */
internal fun String.latexText(): String =
    replace("\\", "").replace("_", "\\_").replace(" ", "\\ ")
