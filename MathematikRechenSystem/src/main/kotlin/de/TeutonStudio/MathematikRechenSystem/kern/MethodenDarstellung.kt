package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Liefert die große Fallunterscheidungsdarstellung für Methoden, ohne Aufrufer an die
 * konkrete Implementierung [MathematischeMethode] zu koppeln.
 *
 * Konkrete mathematische Methoden besitzen eine strukturierte Signatur-/Termdarstellung;
 * andere Implementierungen des offenen [Methode]-Vertrags verwenden ihre kanonische
 * LaTeX-Darstellung.
 */
fun Methode.zuFallunterscheidungsLatex(): String =
    (this as? MathematischeMethode)?.zuFallunterscheidungsLatex() ?: zuLatex()
