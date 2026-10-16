package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung
import de.TeutonStudio.KnotenKartenVerwalter.logik.findeAnschluss
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.AnschlussInfo
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.anschlussBasisInfo
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
    private var joinErgebnisse: Map<KnotenId, KnotenAuswertungsErgebnis> = emptyMap()
    val graphPrüfung = GraphPrüfung(anschlussArten, typSystem, ::effektiverMathematikAnschluss).apply {
        normalisiereKarte = { it.normalisiereJoinEingänge() }
        mehrfachDarstellung = { karte, knoten -> zeigeJoinKapsel(karte, knoten, joinErgebnisse) }
    }
    val vorlagen: List<KnotenVorlage> = KanonischerMathematikKnotenKatalog.alle()

    private val auswerterRegister = GesamterMathematikAuswerter.erzeugeRegister()
    private val auswerter = KartenAuswerter(
        register = auswerterRegister,
        normalisiereKarte = { it.normalisiereJoinEingänge() },
        kartenQuelle = kartenQuelle,
        nichtAuswertbareKnotenArten = nichtAuswertbareKnotenArten,
    )

    /** Keine Auswertung: Metadaten stammen aus denselben Verträgen wie die Graphprüfung. */
    fun anschlussInfoFür(karte: KartenDaten, ref: AnschlussVerweis): AnschlussInfo? {
        val aufgelöst = effektiverMathematikAnschluss(karte, ref) ?: karte.findeAnschluss(ref) ?: return null
        val port = aufgelöst.copy(art = graphPrüfung.effektiveArt(karte, ref),
            vertrag = aufgelöst.vertrag.copy(typ = graphPrüfung.effektiverTyp(karte, ref)))
        val info = anschlussBasisInfo(karte, ref, anschlussArten, port) { id ->
            when (id) {
                "typ.tupel" -> "Tupel"
                "typ.tupel.unendlich" -> "Unendliches Tupel"
                else -> anschlussArten.finde(AnschlussArtId(id))?.name ?: id
            }
        } ?: return null
        val knoten = karte.knoten.first { it.id == ref.knotenId }
        return if (port.name in auswerterRegister.optionaleMethodenEingänge(knoten)) {
            info.copy(eingabeHinweis = "Optionaler Methodeneingang.")
        } else info
    }

    fun auswerten(karte: KartenDaten): KartenAuswertungsErgebnis = auswerter.auswerten(karte).also { joinErgebnisse = it.knoten }

    fun verwerfeCache(knotenId: KnotenId) {
        auswerter.verwerfeCache(knotenId)
    }

    fun leereCache() {
        auswerter.leereCache()
    }
}
