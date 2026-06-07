package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition

// Grundklasse für ein Objekt, dass auf einem KartenGraph erscheint
sealed interface GraphObjekt {
    @Composable fun zuComposable(modifier: Modifier = Modifier.Companion)

    @Composable fun öffneKontext(pos: BildschirmPosition)
    // Der Inhalt des Fensters, dass bei Längerhaten geöffnet wird
    fun erhalteKontextFenster() = Unit
    // Der Inhalt des Inspectrs zu diesem Objekt
    fun erhalteInspectorFenster() = Unit

    // Auf dem Graph wird von einem Anschluss aus gezogen
    fun planeVerbindung(a: Anschluss) = Unit
    // Auf dem Graph wird eine gezogene Verbindung auf einem Anschluss dieses Knoten losgelassen
    // von ist dabei der Anschluss von dem gezogen wurde und nach der auf dem fallen gelassen wurde
    fun erstelleVerbindung(von: Anschluss, zu: Anschluss) = Unit

}
