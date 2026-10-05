package de.TeutonStudio.MathematikKnoten.abstraktion

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.TeilgraphErsetzungsPlan
import de.TeutonStudio.KnotenKartenVerwalter.logik.vorschauTeilgraphErsetzen
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

object PolynomAbstraktionsRegel : AbstraktionsRegel {
    private val historischeRechenArten = setOf(
        "mathematik.addition", "mathematik.multiplikation", "mathematik.potenz",
        "mathematik.quadrat", "mathematik.kubik",
    )

    override fun finde(kontext: AbstraktionsKontext): List<AbstraktionsVorschlag> = buildList {
        kontext.graph.ausgangZuWert.forEach { (wurzelAusgang, wurzelWert) ->
            val ausdruck = wurzelWert.objekt as? ZahlAusdruck ?: return@forEach
            ausdruck.freieVariablen().sortedBy { it.name }.forEach { variable ->
                val zerlegung = zerlegeAlsPolynom(ausdruck, variable) ?: return@forEach
                if (zerlegung.koeffizienten.size < 2) return@forEach
                baueVorschlag(kontext, wurzelAusgang, wurzelWert, zerlegung)?.let(::add)
            }
        }
    }

    private fun baueVorschlag(
        kontext: AbstraktionsKontext,
        wurzelAusgang: AnschlussVerweis,
        wurzelWert: BedingterWert,
        zerlegung: PolynomZerlegung,
    ): AbstraktionsVorschlag? {
        val karte = kontext.karte
        val index = kontext.graph
        val alleVorgänger = index.vorgängerAb(wurzelAusgang.knotenId)
        val variableQuelle = findeQuelle(index, alleVorgänger, zerlegung.variable) ?: return null
        val koeffizientenQuellen = zerlegung.koeffizienten.map { koeffizient ->
            if (koeffizient == RationaleZahl.Null) null else findeQuelle(index, alleVorgänger, koeffizient)
        }
        if (koeffizientenQuellen.zip(zerlegung.koeffizienten).any { (quelle, wert) -> quelle == null && wert != RationaleZahl.Null }) return null

        val geschützteWurzeln = buildSet {
            add(variableQuelle.knotenId)
            koeffizientenQuellen.filterNotNull().forEach { add(it.knotenId) }
        }
        val geschützteKnoten = geschützteWurzeln.flatMapTo(linkedSetOf()) { index.vorgängerAb(it) }
        val entfernteKnoten = alleVorgänger.filterTo(linkedSetOf()) { id ->
            karte.knoten.firstOrNull { it.id == id }?.let(::istPolynomRechenknoten) == true
        } - geschützteKnoten
        if (wurzelAusgang.knotenId !in entfernteKnoten) return null
        val entfernteDaten = karte.knoten.filter { it.id in entfernteKnoten }

        val externeZwischennutzung = karte.verbindungen.any { verbindung ->
            verbindung.von.knotenId in entfernteKnoten &&
                verbindung.zu.knotenId !in entfernteKnoten &&
                verbindung.von != wurzelAusgang
        }
        if (externeZwischennutzung) return null

        val links = entfernteDaten.minOf { it.position.x }
        val oben = entfernteDaten.minOf { it.position.y }
        val tupelBasis = MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt(links, oben)).copy(
            parameter = MathematikKnotenVorlagen.Tupel.standardParameter +
                mapOf("festeEingänge" to zerlegung.koeffizienten.size.toString()),
        )
        val tupel = konfiguriereTupel(tupelBasis, TUPEL_EINZEL_EINGABEN)
        val polynom = konfiguriereErweitertenZahlenRechner(
            ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt(links + 300f, oben)),
            ErweiterterZahlenOperator.POLYNOM,
        )
        val nullKnoten = if (koeffizientenQuellen.any { it == null }) {
            MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt(links - 220f, oben)).copy(
                parameter = MathematikKnotenVorlagen.Zahl.standardParameter + ("wert" to "0"),
            )
        } else null
        val nullAusgang = nullKnoten?.ausgang("wert")
        val neueKnoten = listOfNotNull(nullKnoten, tupel, polynom)
        val tupelEingänge = tupel.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }
        val neueVerbindungen = mutableListOf<VerbindungDaten>()
        zerlegung.koeffizienten.indices.forEach { indexKoeffizient ->
            neueVerbindungen += VerbindungDaten(
                von = koeffizientenQuellen[indexKoeffizient] ?: nullAusgang ?: return null,
                zu = AnschlussVerweis(tupel.id, tupelEingänge[indexKoeffizient].id),
            )
        }
        neueVerbindungen += VerbindungDaten(
            von = tupel.ausgang("tupel"),
            zu = polynom.eingang("koeffizienten"),
        )
        neueVerbindungen += VerbindungDaten(
            von = variableQuelle,
            zu = polynom.eingang("argument"),
        )
        val polynomAusgang = polynom.ausgang("wert")
        karte.verbindungen.filter { it.von == wurzelAusgang && it.zu.knotenId !in entfernteKnoten }.forEach { grenze ->
            neueVerbindungen += grenze.copy(von = polynomAusgang)
        }

        val plan = TeilgraphErsetzungsPlan.aus(karte, entfernteKnoten, neueKnoten, neueVerbindungen)
        if (plan.ersparnis <= 0) return null
        val probe = karte.vorschauTeilgraphErsetzen(plan, kontext.graphPrüfung).karte ?: return null
        val probeAuswertung = kontext.auswerten(probe)
        val nachher = probeAuswertung.knoten[polynom.id]?.ausgaben?.get("wert") ?: return null
        if (!gleicheSchnittstelle(wurzelWert, nachher, zerlegung)) return null

        val stabileKnoten = entfernteKnoten.map { it.wert }.sorted().joinToString(",")
        return AbstraktionsVorschlag(
            id = "polynom:${wurzelAusgang.knotenId.wert}:${zerlegung.variable.name}:$stabileKnoten",
            titel = "Polynom erkennen",
            beschreibung = "${entfernteKnoten.size} vorhandene Knoten werden durch ${neueKnoten.size} Knoten ersetzt.",
            wurzelKnoten = wurzelAusgang.knotenId,
            vorherLatex = wurzelWert.objekt.zuLatex(),
            nachherLatex = polynomAusKoeffizienten(zerlegung.koeffizienten, zerlegung.variable).zuLatex(),
            vorherSchnittstelle = "Term → Term",
            nachherSchnittstelle = "Term → Term",
            sicherheit = AbstraktionsSicherheit.STRUKTURELL_BEWIESEN,
            ersetzungsPlan = plan,
        )
    }

    private fun istPolynomRechenknoten(knoten: KnotenDaten): Boolean {
        if (knoten.art in historischeRechenArten) return true
        if (knoten.art != ZAHLENRECHNER_ART) return false
        return UniversellerZahlenOperator.vonId(knoten.parameter[ZAHLENRECHNER_OPERATOR]) in setOf(
            UniversellerZahlenOperator.ADDITION,
            UniversellerZahlenOperator.MULTIPLIKATION,
            UniversellerZahlenOperator.POTENZ,
            UniversellerZahlenOperator.QUADRAT,
            UniversellerZahlenOperator.KUBIK,
        )
    }

    private fun findeQuelle(
        index: TeilgraphIndex,
        kandidaten: Set<KnotenId>,
        objekt: MathematischesObjekt,
    ): AnschlussVerweis? = index.ausgangZuWert.entries
        .asSequence()
        .filter { (ref, wert) -> ref.knotenId in kandidaten && wert.objekt == objekt }
        .maxByOrNull { (ref, _) -> index.topologischeReihenfolge[ref.knotenId] ?: -1 }
        ?.key

    private fun gleicheSchnittstelle(
        vorher: BedingterWert,
        nachher: BedingterWert,
        zerlegung: PolynomZerlegung,
    ): Boolean {
        val nachherAusdruck = nachher.objekt as? ZahlAusdruck ?: return false
        if (zerlegeAlsPolynom(nachherAusdruck, zerlegung.variable)?.koeffizienten != zerlegung.koeffizienten) return false
        return vorher.annahmen == nachher.annahmen &&
            kompatibleOptionaleSemantik(vorher.zielMenge, nachher.zielMenge) &&
            kompatibleOptionaleSemantik(vorher.werteVorrat, nachher.werteVorrat) &&
            vorher.reelleVariablen == nachher.reelleVariablen &&
            vorher.variablenQuellen == nachher.variablenQuellen &&
            vorher.elementArt == nachher.elementArt &&
            vorher.symbolischeMethode == nachher.symbolischeMethode
    }

    /** Fehlende Legacy-Metadaten sind unbekannt; zwei vorhandene Verträge müssen dagegen identisch sein. */
    private fun kompatibleOptionaleSemantik(vorher: MathematischesObjekt?, nachher: MathematischesObjekt?): Boolean =
        vorher == null || nachher == null || vorher == nachher

    private fun KnotenDaten.eingang(name: String) = AnschlussVerweis(
        id,
        requireNotNull(anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Eingang && it.name == name }).id,
    )

    private fun KnotenDaten.ausgang(name: String) = AnschlussVerweis(
        id,
        requireNotNull(anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang && it.name == name }).id,
    )
}
