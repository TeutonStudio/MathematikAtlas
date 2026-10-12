package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class KnotenAuswertungszeitFußzeileTest {
    @Test fun `Zeitfeld beginnt an den unteren Tangenten des Rechtecks`() {
        val form = RoundedCornerShape(12.dp)
        for (richtung in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            val outline = form.createOutline(Size(480f, 240f), richtung, Density(2f))
            val erwartet = 24f to 24f
            assertEquals(erwartet, unterkantenEinzüge(outline))
        }
    }

    @Test fun `Zeitfeld folgt Kapselradius und begrenzten Radien`() {
        val form = RoundedCornerShape(50)
        for ((größe, erwartet) in listOf(Size(240f, 80f) to 40f, Size(400f, 160f) to 80f, Size(60f, 100f) to 30f)) {
            assertEquals(erwartet to erwartet, unterkantenEinzüge(form.createOutline(größe, LayoutDirection.Ltr, Density(1f))))
        }
    }

    @Test fun `Auswertungsdauer wird kompakt formatiert`() {
        assertEquals("<1 µs", formatiereAuswertungsDauerNanos(999L))
        assertEquals("12,4 ms", formatiereAuswertungsDauerNanos(12_400_000L))
        assertEquals("1,24 s", formatiereAuswertungsDauerNanos(1_240_000_000L))
    }
}
