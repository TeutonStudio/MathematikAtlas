package de.TeutonStudio.MathematikKnoten.visualisierung.sampling

import de.TeutonStudio.MathematikKnoten.visualisierung.modell.VisualisierungsKonfiguration
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Mengenalgebra bleibt im Kern exakt; nur der letzte Zeichenschritt verwendet Double. */
internal object ZahlengeradenNormalisierer {
    fun normalisiere(menge: MengenAusdruck, konfiguration: VisualisierungsKonfiguration): VisualisierungsDefinition {
        if (menge == RationaleZahlen) return VisualisierungsDefinition.NichtRäumlich("ℚ ist dicht, aber kein reelles Intervall.")
        val links = ReelleGrenze.Endlich(rationaleKoordinate(konfiguration.bereiche.x.minimum))
        val rechts = ReelleGrenze.Endlich(rationaleKoordinate(konfiguration.bereiche.x.maximum))
        val ergebnis = normalisiereExakteReelleMenge(menge, links.wert, rechts.wert, konfiguration.sampling.maximalesRasterBudget)
        if (ergebnis is ExakteMengenNormalisierung.Offen) return VisualisierungsDefinition.BedingtRäumlich(ergebnis.grund)
        val exakt = (ergebnis as ExakteMengenNormalisierung.Normalisiert).menge
        val fenster = ExaktesSegment(links, rechts, true, true)
        val punkte = mutableListOf<Double>()
        val intervalle = mutableListOf<VisualisierungsIntervall>()
        val hinweise = mutableListOf<String>()
        if (!exakt.vollständig) hinweise += "Die Darstellung ist auf den sichtbaren Bereich begrenzt."
        for (segment in exakt.segmente) {
            val sichtbar = segment.schnitt(fenster) ?: continue
            val von = (sichtbar.von as ReelleGrenze.Endlich).wert.zuDezimal(34).toDouble()
            val bis = (sichtbar.bis as ReelleGrenze.Endlich).wert.zuDezimal(34).toDouble()
            if (sichtbar.von == sichtbar.bis) punkte += von else {
                if (von == bis) hinweise += "Verschiedene exakte Grenzen fallen in der Zeichenauflösung zusammen."
                intervalle += VisualisierungsIntervall(von, bis, sichtbar.linksGeschlossen, sichtbar.rechtsGeschlossen,
                    linksAmFensterrand = segment.von < links, rechtsAmFensterrand = segment.bis > rechts)
            }
        }
        return VisualisierungsDefinition.Zahlengerade(punkte, intervalle, hinweise.distinct(),
            mathematischLeer = exakt.vollständig && exakt.segmente.isEmpty())
    }
}
