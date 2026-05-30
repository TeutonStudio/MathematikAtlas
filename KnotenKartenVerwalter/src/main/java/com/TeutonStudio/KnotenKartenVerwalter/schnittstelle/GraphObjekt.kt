package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Grundklasse für ein Objekt, dass auf einem KartenGraph erscheint
sealed interface GraphObjekt {
    @Composable
    fun zuComposable(modifier: Modifier = Modifier)
    // Der Inhalt des Fensters, dass bei Längerhaten geöffnet wird
    fun erhalteKontextFenster() = Unit
    // Auf dem Graph wird von einem Anschluss aus gezogen
    fun planeVerbindung(a: Anschluss) = Unit
    // Auf dem Graph wird eine gezogene Verbindung auf einem Anschluss dieses Knoten losgelassen
    // von ist dabei der Anschluss von dem gezogen wurde und nach der auf dem fallen gelassen wurde
    fun erstelleVerbindung(von: Anschluss, zu: Anschluss) = Unit

}
