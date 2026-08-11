package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.TypPrüfung
import de.TeutonStudio.MathematikRechenSystem.kern.*

/**
 * Brücke von der vorhandenen Mengen-/Methodensemantik zum neuen Port-Typkern.
 * Es wird nur Information abgeleitet, die strukturell belegt ist; unbekannte
 * benannte Mengen werden absichtlich nicht anhand ihrer Anzeige geraten.
 */
object MathematikTypResolver {
    private val typSystem = MathematikTypSystem()

    /** Typ eines Elements der angegebenen Menge, nicht der Menge als Objekt. */
    fun elementTyp(menge: MengenAusdruck): TypAusdruck {
        menge.fundamentalerZahlbereichOderNull()?.let { return zahlbereichTyp(it) }
        return when (menge) {
            LeereMenge -> TypAusdruck.Unbekannt
            is BeschraenkteZahlmenge -> zahlbereichTyp(menge.traeger)
            is ReellesIntervall -> MathematikTypen.reelleZahl
            is Vektorraum -> when (menge.orientierung) {
                VektorOrientierung.Spalte -> MathematikTypen.spaltenVektor(elementTyp(menge.skalarMenge), menge.dimension)
                VektorOrientierung.Zeile -> MathematikTypen.zeilenVektor(elementTyp(menge.skalarMenge), menge.dimension)
            }
            is Matrizenraum -> MathematikTypen.matrix(elementTyp(menge.skalarMenge), menge.zeilen, menge.spalten)
            is Tensorraum -> MathematikTypen.tensor(
                elementTyp(menge.elementMenge),
                menge.dimensionen.map { it.zuLatex() },
            )
            is Tupelraum -> TypAusdruck.Parameterisiert(TypKernIds.Tupel, menge.komponenten.map(::elementTyp))
            is KartesischesProdukt -> TypAusdruck.Parameterisiert(TypKernIds.Tupel, menge.mengen.map(::elementTyp))
            is DefinierteMenge -> {
                val typen = menge.variablen.map { elementTyp(it.grundMenge) }
                if (typen.size == 1) typen.single() else TypAusdruck.Parameterisiert(TypKernIds.Tupel, typen)
            }
            is GefilterteMenge -> elementTyp(menge.menge)
            is MengenDifferenz -> elementTyp(menge.links)
            is Vereinigung -> gemeinsameOberart(menge.mengen.map(::elementTyp))
            is Schnitt -> spezifischsterGemeinsamerTyp(menge.mengen.map(::elementTyp))
            is EndlicheMenge -> gemeinsameOberart(menge.elemente.map(::objektTyp))
            is Potenzmenge -> MathematikTypen.mengeVon(elementTyp(menge.grundMenge))
            is Abbildungsmenge -> MathematikTypen.methode(
                argumente = listOf(elementTyp(menge.definitionsMenge)),
                ziel = elementTyp(menge.zielMenge),
            )
            else -> TypAusdruck.Unbekannt
        }
    }

    fun mengenObjektTyp(menge: MengenAusdruck): TypAusdruck = MathematikTypen.mengeVon(elementTyp(menge))

    fun methodenTyp(methode: Methode): TypAusdruck {
        val signatur = runCatching { methode.methodenSignatur() }.getOrNull()
            ?: return TypAusdruck.Atom(MathematikTypen.Methode)
        return MathematikTypen.methode(
            argumente = signatur.argumente.map { elementTyp(it.werteVorrat) },
            ziel = elementTyp(signatur.zielMenge),
        )
    }

    fun objektTyp(objekt: MathematischesObjekt): TypAusdruck = when (objekt) {
        is RationaleZahl -> MathematikTypen.rationaleZahl
        is ZahlAusdruck -> MathematikTypen.zahl
        is Aussage -> MathematikTypen.aussage
        is MengenAusdruck -> mengenObjektTyp(objekt)
        is SpaltenVektor -> MathematikTypen.spaltenVektor
        is ZeilenVektor -> MathematikTypen.zeilenVektor
        is Matrix -> MathematikTypen.matrix
        is Tensor -> MathematikTypen.tensor
        is Tupel -> TypAusdruck.Parameterisiert(TypKernIds.Tupel, objekt.elemente.map(::objektTyp))
        else -> TypAusdruck.Unbekannt
    }

    private fun zahlbereichTyp(bereich: FundamentalerZahlbereich): TypAusdruck = when (bereich) {
        FundamentalerZahlbereich.NATUERLICH_POSITIV -> MathematikTypen.natürlicheZahl
        FundamentalerZahlbereich.NATUERLICH_MIT_NULL -> MathematikTypen.nichtnegativeGanzeZahl
        FundamentalerZahlbereich.GANZ -> MathematikTypen.ganzeZahl
        FundamentalerZahlbereich.RATIONAL -> MathematikTypen.rationaleZahl
        FundamentalerZahlbereich.REELL -> MathematikTypen.reelleZahl
        FundamentalerZahlbereich.KOMPLEX -> MathematikTypen.komplexeZahl
        FundamentalerZahlbereich.QUATERNION -> MathematikTypen.quaternionZahl
    }

    private fun gemeinsameOberart(typen: List<TypAusdruck>): TypAusdruck =
        typSystem.gemeinsameOberart(typen) ?: TypAusdruck.Unbekannt

    private fun spezifischsterGemeinsamerTyp(typen: List<TypAusdruck>): TypAusdruck {
        val bekannt = typen.filterNot { it == TypAusdruck.Unbekannt }.distinct()
        return bekannt.firstOrNull { kandidat ->
            bekannt.all { anderer -> typSystem.prüfe(kandidat, anderer) == TypPrüfung.Kompatibel }
        } ?: TypAusdruck.Unbekannt
    }
}
