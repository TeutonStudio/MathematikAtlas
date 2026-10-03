package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Liefert die große mathematische Fallunterscheidungsdarstellung, sofern die Methode
 * diese Capability besitzt. Domänenneutrale Methoden werden bewusst nur über ihren
 * Anzeigenamen projiziert; `zuLatex()` ist kein allgemeiner Methodenvertrag mehr.
 */
fun Methode.zuFallunterscheidungsLatex(): String =
    (this as? MathematischeMethode)?.zuFallunterscheidungsLatex() ?: name
