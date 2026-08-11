package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog

/**
 * Plattformneutrale Zusammensetzung der mathematischen Kartenlaufzeit.
 *
 * Android und Desktop sollen weder Anschlussregister noch Gesamtauswerter oder
 * Knotenkatalog unabhängig voneinander verdrahten. Plattformzustände behalten
 * Navigation, Dateisystem und UI, delegieren aber die gemeinsame Mathematik- und
 * Graphinfrastruktur an diese Laufzeit.
 */
class MathematikKartenLaufzeit(
    kartenQuelle: KartenQuelle = KartenQuelle { null },
    nichtAuswertbareKnotenArten: Set<KnotenArtId> = emptySet(),
) {
    val anschlussArten = AnschlussArtRegister(MathematikAnschlussArten.alle)
    val typSystem = erzeugeMathematikKartenTypSystem(anschlussArten)
    val graphPrüfung = GraphPrüfung(anschlussArten, typSystem)
    val vorlagen: List<KnotenVorlage> = KanonischerMathematikKnotenKatalog.alle()
        .map(KnotenVorlage::migriereSemantischeTypen)

    private val auswerter = KartenAuswerter(
        register = GesamterMathematikAuswerter.erzeugeRegister(),
        kartenQuelle = kartenQuelle,
        nichtAuswertbareKnotenArten = nichtAuswertbareKnotenArten,
    )

    fun auswerten(karte: KartenDaten): KartenAuswertungsErgebnis = auswerter.auswerten(karte)

    fun verwerfeCache(knotenId: KnotenId) {
        auswerter.verwerfeCache(knotenId)
    }

    fun leereCache() {
        auswerter.leereCache()
    }
}
