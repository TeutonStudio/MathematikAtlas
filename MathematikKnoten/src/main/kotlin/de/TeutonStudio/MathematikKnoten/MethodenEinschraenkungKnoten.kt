package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val METHODEN_EINSCHRAENKUNG_KNOTEN_ART = "mathematik.methode.einschraenkung"

object MethodenEinschraenkungKnotenVorlagen {
    val Einschraenkung = KnotenVorlage(
        art = METHODEN_EINSCHRAENKUNG_KNOTEN_ART,
        name = "Methode einschränken",
        kategorie = "Grundlagen: Methoden",
        beschreibung = "Erzeugt f|_M für eine einstellige Methode. Der Wertevorrat wird M, die deklarierte Zielmenge bleibt unverändert.",
        standardGröße = GraphGröße(285f, 120f),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "methode",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Methode.id,
                reihenfolge = 0,
            ),
            AnschlussDaten(
                name = "menge",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Menge.id,
                reihenfolge = 1,
            ),
            AnschlussDaten(
                name = "methode",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Methode.id,
            ),
        ),
    )

    val alle = listOf(Einschraenkung)
}

internal fun MathematikAuswerterRegister.registriereMethodenEinschraenkungKnoten() {
    registriere(METHODEN_EINSCHRAENKUNG_KNOTEN_ART) { kontext ->
        kontext.werteMethodenEinschraenkungAus()
    }
}

private fun KnotenAuswertungsKontext.werteMethodenEinschraenkungAus(): KnotenAuswertungsErgebnis {
    val methode = eingänge["methode"]?.objekt as? Methode
        ?: return fehlerErgebnis("Die einzuschränkende Methode fehlt.")
    val menge = eingänge["menge"]?.objekt as? MengenAusdruck
        ?: return fehlerErgebnis("Die neue Definitionsmenge fehlt.")
    val einschraenkung = runCatching {
        schraenkeMethodeEin(methode, menge, rechenKontext)
    }.getOrElse { fehler ->
        return fehlerErgebnis(fehler.message ?: "Die Methode kann nicht auf diese Menge eingeschränkt werden.")
    }
    val annahmen = eingänge.values.flatMap { it.annahmen }.toSet() + einschraenkung.voraussetzungen
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "methode" to BedingterWert(
                objekt = einschraenkung.eingeschraenkteMethode,
                annahmen = annahmen,
                zielMenge = einschraenkung.eingeschraenkteMethode.zielMenge,
            ),
        ),
        warnungen = buildList {
            add("Wertevorrat: ${menge.zuLatex()}")
            add("Zielmenge bleibt: ${methode.zielMenge.zuLatex()}")
            if (einschraenkung.voraussetzungen.isNotEmpty()) {
                add("Die Teilmengenbeziehung bleibt als offene Voraussetzung erhalten.")
            }
        },
        eingänge = eingänge,
    )
}

private fun KnotenAuswertungsKontext.fehlerErgebnis(nachricht: String): KnotenAuswertungsErgebnis =
    KnotenAuswertungsErgebnis(
        ausgaben = emptyMap(),
        fehler = nachricht,
        eingänge = eingänge,
    )

fun KartenDaten.migriereMethodenEinschraenkungKnoten(): KartenDaten = copy(
    knoten = knoten.map { knoten ->
        if (knoten.art in setOf("mathematik.methodenEinschraenkung", "mathematik.restriktion")) {
            knoten.copy(
                art = METHODEN_EINSCHRAENKUNG_KNOTEN_ART,
                name = "Methode einschränken",
            )
        } else {
            knoten
        }
    },
)
