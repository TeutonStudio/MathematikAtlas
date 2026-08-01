package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.EndlicheMenge
import de.TeutonStudio.MathematikRechenSystem.kern.LeereMenge
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt

object EndlicheMengeAuswerter {
    fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val gelesen = leseEndlicheMengeKonfiguration(kontext.knoten)
        val fehler = linkedMapOf<String, String>()
        val elemente = linkedMapOf<MathematischesObjekt, EndlicheMengeEintrag>()
        var duplikate = 0

        gelesen.konfiguration.einträge.forEach { eintrag ->
            val auswertung = eintrag.auswerten()
            val objekt = auswertung.objekt
            when {
                auswertung.fehler != null -> fehler[eintrag.id] = auswertung.fehler
                objekt == null -> fehler[eintrag.id] = "Das Element konnte nicht erzeugt werden."
                objekt in elemente -> duplikate += 1
                else -> elemente[objekt] = eintrag
            }
        }

        val menge = if (elemente.isEmpty()) LeereMenge else EndlicheMenge(elemente.keys)
        val latex = if (elemente.isEmpty()) "\\varnothing" else elemente.keys
            .joinToString(prefix = "\\{", postfix = "\\}") { it.zuLatex() }
        val gemeinsameArt = gelesen.konfiguration.mitErkannterGemeinsamerArt().gemeinsameArt
            ?.let(::AnschlussArtId)
        val warnungen = when (duplikate) {
            0 -> emptyList()
            1 -> listOf("Ein doppeltes Element wurde zusammengeführt.")
            else -> listOf("$duplikate doppelte Elemente wurden zusammengeführt.")
        }
        val gesamtFehler = gelesen.fehler ?: when (fehler.size) {
            0 -> null
            1 -> fehler.values.single()
            else -> "${fehler.size} Mengenelemente sind ungültig."
        }

        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "menge" to BedingterWert(
                    objekt = menge,
                    latexDarstellung = latex,
                    elementArt = gemeinsameArt,
                ),
            ),
            fehler = gesamtFehler,
            elementFehler = fehler,
            warnungen = warnungen,
        )
    }
}
