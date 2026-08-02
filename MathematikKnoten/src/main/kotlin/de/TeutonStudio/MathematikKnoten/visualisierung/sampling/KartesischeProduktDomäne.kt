package de.TeutonStudio.MathematikKnoten.visualisierung.sampling

import de.TeutonStudio.MathematikRechenSystem.kern.KartesischesProdukt
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck

/**
 * Stabiler, modulinterner Zugriff nach einer Prüfung auf [KartesischesProdukt].
 *
 * Kotlin kann öffentliche Eigenschaften aus einem anderen Modul nicht über
 * mehrere Zugriffe hinweg smart-casten. Die Samplinglogik prüft den Typ vor
 * jedem Zugriff; diese Erweiterung hält den anschließenden Zugriff dennoch
 * auf die tatsächlich geprüfte Produktdomäne beschränkt.
 */
internal val MengenAusdruck.mengen
    get() = (this as KartesischesProdukt).mengen
