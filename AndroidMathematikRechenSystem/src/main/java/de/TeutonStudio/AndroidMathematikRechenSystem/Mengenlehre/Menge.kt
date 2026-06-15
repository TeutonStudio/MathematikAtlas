package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre

interface Menge<E: Element>: Element {
    public fun enthält(element: Element): Boolean? {
        if (element !is E) return false

    }

    // TODO
}