package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.AussageStatus
import de.TeutonStudio.MathematikRechenSystem.kern.EigenschaftsAussage
import de.TeutonStudio.MathematikRechenSystem.kern.EigenschaftsDiagnose
import de.TeutonStudio.MathematikRechenSystem.kern.EndlicheMenge
import de.TeutonStudio.MathematikRechenSystem.kern.LeereMenge
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.ReellesIntervall
import de.TeutonStudio.MathematikRechenSystem.kern.UnterstuetzungsStatus

private enum class TopologieModus {
    KANONISCH_REELL,
    DISKRET,
    INDISKRET,
    UNBEKANNT,
}

internal fun MathematikAuswerterRegister.registriereMengenEigenschaftsAuswertung() {
    registriere(MENGEN_EIGENSCHAFT_KNOTEN_ART) { kontext ->
        val menge = kontext.eingänge["menge"]?.objekt as? MengenAusdruck
            ?: error("Die zu prüfende Menge fehlt.")
        val kennung = kontext.knoten.parameter[EIGENSCHAFT_PARAMETER].orEmpty().trim().lowercase()
        val definition = when (kennung) {
            "offen" -> MathematischeEigenschaftRegister.Offen
            "abgeschlossen", "geschlossen" -> MathematischeEigenschaftRegister.Abgeschlossen
            "konvex", "konvexe-menge" -> MathematischeEigenschaftRegister.KonvexeMenge
            else -> MathematischeEigenschaftRegister.finde(kennung)
                ?: error("Unbekannte Mengeneigenschaft '$kennung'.")
        }
        val topologie = topologieModus(kontext.knoten.parameter)
        val umgebungsraum = kontext.knoten.parameter["umgebungsraum"]
            ?: kontext.knoten.parameter[EIGENSCHAFT_KONTEXT_PARAMETER]
            ?: "\\mathbb{R}"
        val istUmgebungsraum = kontext.knoten.parameter["istUmgebungsraum"].toBoolean()
        val affineStruktur = kontext.knoten.parameter["affineStruktur"] ?: "reell"

        val bewertung = when (definition) {
            MathematischeEigenschaftRegister.Offen -> topologischeBewertung(
                menge = menge,
                topologie = topologie,
                istUmgebungsraum = istUmgebungsraum,
                offen = true,
            )
            MathematischeEigenschaftRegister.Abgeschlossen -> topologischeBewertung(
                menge = menge,
                topologie = topologie,
                istUmgebungsraum = istUmgebungsraum,
                offen = false,
            )
            MathematischeEigenschaftRegister.KonvexeMenge -> konvexitätsBewertung(menge, affineStruktur)
            else -> MengenBewertung(
                unterstuetzung = UnterstuetzungsStatus.NOCH_NICHT_IMPLEMENTIERT,
                status = AussageStatus.UNENTSCHEIDBAR,
                code = "mengenbegriff-fehlt",
                nachricht = "Für diese Mengeneigenschaft ist noch kein Prüfer registriert.",
            )
        }

        val aussage = EigenschaftsAussage(
            eigenschaftId = definition.id,
            eigenschaftLatex = definition.adjektiv,
            subjektLatex = menge.zuLatex(),
            unterstuetzung = bewertung.unterstuetzung,
            aussageStatus = bewertung.status,
            diagnose = EigenschaftsDiagnose(
                code = bewertung.code,
                nachricht = bewertung.nachricht,
                voraussetzungen = bewertung.voraussetzungen,
            ),
            kontextLatex = umgebungsraum,
        )
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "aussage" to BedingterWert(
                    aussage,
                    kontext.eingänge.values.flatMap { it.annahmen }.toSet(),
                ),
            ),
            eingänge = kontext.eingänge,
            warnungen = if (bewertung.status in setOf(AussageStatus.BEDINGT, AussageStatus.UNENTSCHEIDBAR)) {
                listOf(bewertung.nachricht)
            } else emptyList(),
        )
    }
}

private data class MengenBewertung(
    val unterstuetzung: UnterstuetzungsStatus,
    val status: AussageStatus,
    val code: String,
    val nachricht: String,
    val voraussetzungen: List<String> = emptyList(),
)

private fun topologischeBewertung(
    menge: MengenAusdruck,
    topologie: TopologieModus,
    istUmgebungsraum: Boolean,
    offen: Boolean,
): MengenBewertung {
    val status = when (topologie) {
        TopologieModus.DISKRET -> AussageStatus.BEWIESEN
        TopologieModus.INDISKRET -> if (menge == LeereMenge || istUmgebungsraum) {
            AussageStatus.BEWIESEN
        } else {
            AussageStatus.WIDERLEGT
        }
        TopologieModus.KANONISCH_REELL -> if (offen) kanonischOffen(menge) else kanonischAbgeschlossen(menge)
        TopologieModus.UNBEKANNT -> AussageStatus.UNENTSCHEIDBAR
    }
    val unterstuetzung = if (topologie == TopologieModus.UNBEKANNT) {
        UnterstuetzungsStatus.NOCH_NICHT_IMPLEMENTIERT
    } else {
        UnterstuetzungsStatus.IMPLEMENTIERT
    }
    return MengenBewertung(
        unterstuetzung = unterstuetzung,
        status = status,
        code = "topologie-${topologie.name.lowercase()}-${if (offen) "offen" else "abgeschlossen"}",
        nachricht = when (topologie) {
            TopologieModus.DISKRET -> "In der diskreten Topologie ist jede Teilmenge offen und abgeschlossen."
            TopologieModus.INDISKRET -> "In der indiskreten Topologie sind nur die leere Menge und der gesamte Umgebungsraum offen und abgeschlossen."
            TopologieModus.KANONISCH_REELL -> "Die Aussage wurde relativ zur kanonischen Topologie des reellen Umgebungsraums geprüft."
            TopologieModus.UNBEKANNT -> "Die Topologie ist mathematisch relevant, wird vom aktuellen Prüfer aber noch nicht unterstützt."
        },
        voraussetzungen = listOf("Topologie", "Umgebungsraum"),
    )
}

private fun konvexitätsBewertung(
    menge: MengenAusdruck,
    affineStruktur: String,
): MengenBewertung {
    if (affineStruktur.isBlank() || affineStruktur.equals("keine", ignoreCase = true)) {
        return MengenBewertung(
            unterstuetzung = UnterstuetzungsStatus.MATHEMATISCH_NICHT_MOEGLICH,
            status = AussageStatus.UNENTSCHEIDBAR,
            code = "affine-struktur-fehlt",
            nachricht = "Mengenkonvexität benötigt eine affine Struktur; eine bloße Menge oder Topologie reicht nicht aus.",
            voraussetzungen = listOf("affine Struktur"),
        )
    }
    val status = when (menge) {
        LeereMenge, is ReellesIntervall -> AussageStatus.BEWIESEN
        is EndlicheMenge -> if (menge.elemente.size <= 1) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT
        else -> AussageStatus.UNENTSCHEIDBAR
    }
    return MengenBewertung(
        unterstuetzung = UnterstuetzungsStatus.IMPLEMENTIERT,
        status = status,
        code = "affine-konvexitaet",
        nachricht = if (status == AussageStatus.UNENTSCHEIDBAR) {
            "Die affine Struktur ist sichtbar, die Mengenbeschreibung erlaubt jedoch keinen exakten Segmentnachweis."
        } else {
            "Für alle geprüften Punkte bleibt das Verbindungssegment in der Menge."
        },
        voraussetzungen = listOf("affine Struktur", "sichtbarer Umgebungsraum"),
    )
}

private fun kanonischOffen(menge: MengenAusdruck): AussageStatus = when (menge) {
    LeereMenge -> AussageStatus.BEWIESEN
    is ReellesIntervall -> if (menge.linksOffen && menge.rechtsOffen) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT
    is EndlicheMenge -> if (menge.elemente.isEmpty()) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT
    else -> AussageStatus.UNENTSCHEIDBAR
}

private fun kanonischAbgeschlossen(menge: MengenAusdruck): AussageStatus = when (menge) {
    LeereMenge -> AussageStatus.BEWIESEN
    is ReellesIntervall -> if (!menge.linksOffen && !menge.rechtsOffen) AussageStatus.BEWIESEN else AussageStatus.WIDERLEGT
    is EndlicheMenge -> AussageStatus.BEWIESEN
    else -> AussageStatus.UNENTSCHEIDBAR
}

private fun topologieModus(parameter: Map<String, String>): TopologieModus = when (
    (parameter["topologie"] ?: parameter[EIGENSCHAFT_KONTEXT_PARAMETER] ?: "kanonisch").trim().lowercase()
) {
    "kanonisch", "kanonisch:r", "r", "reell", "automatisch" -> TopologieModus.KANONISCH_REELL
    "diskret" -> TopologieModus.DISKRET
    "indiskret", "trivial" -> TopologieModus.INDISKRET
    else -> TopologieModus.UNBEKANNT
}
