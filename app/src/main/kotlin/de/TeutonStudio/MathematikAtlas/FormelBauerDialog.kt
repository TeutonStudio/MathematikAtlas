package de.TeutonStudio.MathematikAtlas

import androidx.compose.runtime.Composable
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft
import de.TeutonStudio.MathematikKnoten.GemeinsamerFormelBauerDialog
import de.TeutonStudio.MathematikRechenSystem.kern.FormelAusdruck

/** Android stellt nur den Kontext und die atomare Übernahme bereit. */
@Composable
internal fun FormelBauerDialog(startLatex: String, schließen: () -> Unit, übernehmen: (String) -> Unit,
    startZeichnung: KnotenEigenschaft? = null, übernehmenMitZeichnung: ((FormelAusdruck, String, KnotenEigenschaft) -> Unit)? = null, startAusdruck: FormelAusdruck? = null) =
    GemeinsamerFormelBauerDialog(startLatex, startZeichnung, übernehmenMitZeichnung, schließen, übernehmen, startAusdruck)
