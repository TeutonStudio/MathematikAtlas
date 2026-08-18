package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.AussageStatus
import de.TeutonStudio.MathematikRechenSystem.kern.EigenschaftsAussage
import de.TeutonStudio.MathematikRechenSystem.kern.EigenschaftsDiagnose
import de.TeutonStudio.MathematikRechenSystem.kern.KomplexeZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.Matrizenraum
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.NatürlicheZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.GanzeZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.SpaltenVektor
import de.TeutonStudio.MathematikRechenSystem.kern.Tupelraum
import de.TeutonStudio.MathematikRechenSystem.kern.UnterstuetzungsStatus
import de.TeutonStudio.MathematikRechenSystem.kern.Vektorraum
import de.TeutonStudio.MathematikRechenSystem.kern.ZeilenVektor
import de.TeutonStudio.MathematikRechenSystem.kern.alsMathematischeMethode

/** Verbindliche Persistenzwerte aus #279 mit Migration der ersten Entwurfsnamen. */
private fun normalisierteFolgenEigenschaft(kennung: String): MathematischeEigenschaftDefinition? = when (kennung.trim().lowercase()) {
    "halbfolge", "einseitig", "einseitige-folge", "n0-folge", "unnatürlichestupel", "unnatuerlichestupel" ->
        MathematischeEigenschaftRegister.EinseitigeFolge
    "zweiseitig", "zweiseitige-folge", "z-folge", "bi-infinite" ->
        MathematischeEigenschaftRegister.ZweiseitigeFolge
    "reell", "reellwertig" -> MathematischeEigenschaftRegister.Reellwertig
    "komplex", "komplexwertig" -> MathematischeEigenschaftRegister.Komplexwertig
    "polynom", "polynomwertig" -> MathematischeEigenschaftRegister.Polynomwertig
    "vektor", "vektorwertig" -> MathematischeEigenschaftRegister.Vektorwertig
    else -> MathematischeEigenschaftRegister.finde(kennung)
}

internal fun MathematikAuswerterRegister.registriereFolgenUndSignaturEigenschaften() {
    registriere(FOLGEN_EIGENSCHAFT_KNOTEN_ART) { kontext ->
        val methode = kontext.eingänge["methode"]?.objekt as? Methode
            ?: error("Die zu prüfende Methode fehlt.")
        val kennung = kontext.knoten.parameter[EIGENSCHAFT_PARAMETER].orEmpty()
        val definition = normalisierteFolgenEigenschaft(kennung)
            ?: error("Unbekannte Folgeneigenschaft '$kennung'.")
        val vertrag = FolgenVertrag.von(methode)
        val wertArt = wertArtDefinition(methode)
        val wahr = when (definition) {
            MathematischeEigenschaftRegister.EinseitigeFolge -> vertrag.art == FolgenArt.Einseitig
            MathematischeEigenschaftRegister.ZweiseitigeFolge -> vertrag.art == FolgenArt.Zweiseitig
            else -> wertArt == definition
        }
        val aussage = EigenschaftsAussage(
            eigenschaftId = definition.id,
            eigenschaftLatex = definition.adjektiv,
            subjektLatex = methode.name,
            unterstuetzung = UnterstuetzungsStatus.IMPLEMENTIERT,
            aussageStatus = if (wahr) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT,
            diagnose = EigenschaftsDiagnose(
                code = "folgenvertrag-${vertrag.art.name.lowercase()}",
                nachricht = buildString {
                    append("Indexbereich: ")
                    append(vertrag.art.indexMenge.zuLatex())
                    append(", Zielmenge: ")
                    append(vertrag.zielMenge.zuLatex())
                    vertrag.indexRolle?.let { append(", Indexrolle: ${it.stabileId}") }
                    append('.')
                },
            ),
        )
        kontext.eigenschaftsAussageAusgabe(aussage)
    }

    registriere(METHODEN_STELLIGKEIT_KNOTEN_ART) { kontext ->
        val methode = kontext.eingänge["methode"]?.objekt as? Methode
            ?: error("Die zu prüfende Methode fehlt.")
        val kennung = kontext.knoten.parameter[EIGENSCHAFT_PARAMETER].orEmpty().lowercase()
        val erwartetEinstellig = when (kennung) {
            "einstellig", "univariat" -> true
            "mehrstellig", "multivariat" -> false
            else -> error("Unbekannte Stelligkeitseigenschaft '$kennung'.")
        }
        val ansicht = runCatching {
            ArgumentAnsicht.valueOf(
                kontext.knoten.parameter["argumentAnsicht"] ?: ArgumentAnsicht.EinzelArgumente.name,
            )
        }.getOrDefault(ArgumentAnsicht.EinzelArgumente)
        val signatur = MethodenSignaturAnsicht.von(methode, ansicht)
        val wahr = (signatur.stelligkeit == 1) == erwartetEinstellig
        val definition = if (erwartetEinstellig) MathematischeEigenschaftRegister.Einstellig
            else MathematischeEigenschaftRegister.Mehrstellig
        val aussage = EigenschaftsAussage(
            eigenschaftId = definition.id,
            eigenschaftLatex = definition.adjektiv,
            subjektLatex = methode.name,
            unterstuetzung = UnterstuetzungsStatus.IMPLEMENTIERT,
            aussageStatus = if (wahr) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT,
            diagnose = EigenschaftsDiagnose(
                code = "signatur-${signatur.stelligkeit}-${ansicht.name.lowercase()}",
                nachricht = "Die Methodensignatur besitzt ${signatur.stelligkeit} stabile Argumentrollen: " +
                    signatur.rollen.joinToString { "${it.position + 1}:${it.stabileId}" } + ".",
            ),
        )
        kontext.eigenschaftsAussageAusgabe(aussage)
    }
}

private fun wertArtDefinition(methode: Methode): MathematischeEigenschaftDefinition? {
    val mathematisch = runCatching { methode.alsMathematischeMethode("Folgen-Wertart") }.getOrNull() ?: return null
    return when {
        mathematisch.zielMenge in setOf(NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen) ->
            MathematischeEigenschaftRegister.Reellwertig
        mathematisch.zielMenge == KomplexeZahlen -> MathematischeEigenschaftRegister.Komplexwertig
        mathematisch.zielMenge is Vektorraum || mathematisch.zielMenge is Matrizenraum || mathematisch.zielMenge is Tupelraum ->
            MathematischeEigenschaftRegister.Vektorwertig
        mathematisch.vorschrift is SpaltenVektor || mathematisch.vorschrift is ZeilenVektor ->
            MathematischeEigenschaftRegister.Vektorwertig
        mathematisch.vorschrift::class.simpleName?.contains("Polynom", ignoreCase = true) == true ->
            MathematischeEigenschaftRegister.Polynomwertig
        else -> null
    }
}

private fun KnotenAuswertungsKontext.eigenschaftsAussageAusgabe(aussage: EigenschaftsAussage) =
    KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "aussage" to BedingterWert(
                aussage,
                eingänge.values.flatMap { it.annahmen }.toSet(),
            ),
        ),
        eingänge = eingänge,
    )
