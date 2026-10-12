package de.TeutonStudio.MathematikAtlas

import androidx.compose.runtime.Composable
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft
import de.TeutonStudio.MathematikKnoten.GemeinsamerStrukturFormelBauerDialog
import de.TeutonStudio.MathematikRechenSystem.kern.*

@Composable
internal fun StrukturFormelBauerDialog(familie: StrukturFormelFamilie, start: FormelAusdruck?, quantorVariable: String,
    schließen: () -> Unit, übernehmen: (FormelAusdruck) -> Unit, startZeichnung: KnotenEigenschaft? = null,
    übernehmenMitZeichnung: ((FormelAusdruck, KnotenEigenschaft) -> Unit)? = null) =
    GemeinsamerStrukturFormelBauerDialog(familie, start, quantorVariable, startZeichnung, übernehmenMitZeichnung, schließen, übernehmen)
