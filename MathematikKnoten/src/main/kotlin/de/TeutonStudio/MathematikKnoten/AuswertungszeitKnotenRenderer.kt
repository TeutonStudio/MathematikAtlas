package de.TeutonStudio.MathematikKnoten

import androidx.compose.runtime.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenMeldung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenMeldungsArt
import de.TeutonStudio.KnotenKartenVerwalter.daten.normalisierteMeldungen
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenAuswertungszeitFußzeile
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInteraktionsModus
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRenderer
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRendererAktionen
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis

/** Ergänzt jeden Renderer um dieselbe Laufzeitdiagnose, ohne seine Knotendarstellung zu verändern. */
fun KnotenRenderer.mitAuswertungszeit(
    ergebnisFür: (KnotenDaten) -> KnotenAuswertungsErgebnis?,
): KnotenRenderer {
    val basis = this
    return object : KnotenRenderer {
        override val interaktionsModus: KnotenInteraktionsModus get() = basis.interaktionsModus
        override val misstNatürlichenInhalt: Boolean get() = basis.misstNatürlichenInhalt
        @Composable override fun Mindestgröße(knoten: KnotenDaten) = basis.Mindestgröße(knoten)

        @Composable
        override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen) {
            var lokaleMeldungen by remember(knoten.id, knoten.art) { mutableStateOf(emptyList<KnotenMeldung>()) }
            val gemeinsameMeldungen = ergebnisFür(knoten).knotenMeldungen()
            SideEffect { aktionen.meldungenMelden((gemeinsameMeldungen + lokaleMeldungen).normalisierteMeldungen()) }
            basis.Inhalt(knoten, ausgewählt, object : KnotenRendererAktionen by aktionen {
                override fun meldungenMelden(meldungen: List<KnotenMeldung>) {
                    lokaleMeldungen = meldungen.normalisierteMeldungen()
                }
            })
        }

        @Composable
        override fun Fußzeile(knoten: KnotenDaten, ausgewählt: Boolean) {
            KnotenAuswertungszeitFußzeile(ergebnisFür(knoten)?.auswertungsDauerNanos, ausgewählt)
        }
    }
}

/** Gemeinsame Auswertungsdiagnosen, auch für direkt verwendete Standardrenderer. */
internal fun KnotenAuswertungsErgebnis?.knotenMeldungen(): List<KnotenMeldung> {
    val ergebnis = this ?: return emptyList()
    return buildList {
        ergebnis.fehler?.let { add(KnotenMeldung(it, KnotenMeldungsArt.Fehler)) }
        ergebnis.elementFehler.values.forEach { add(KnotenMeldung(it, KnotenMeldungsArt.Fehler)) }
        ergebnis.warnungen.forEach { add(KnotenMeldung(it, KnotenMeldungsArt.Warnung)) }
        ergebnis.ausgaben.values.forEach { ausgabe ->
            ausgabe.übersetzungen.aktuell?.hinweise()?.forEach { add(KnotenMeldung(it)) }
        }
    }.normalisierteMeldungen()
}
