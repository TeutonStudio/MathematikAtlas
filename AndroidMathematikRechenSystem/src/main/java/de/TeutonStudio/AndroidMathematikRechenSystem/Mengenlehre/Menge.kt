package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre

interface Menge<E: Element>: Element {
    public fun enthält(element: E): Boolean

    // TODO
}