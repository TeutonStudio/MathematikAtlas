package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Leitet eine sichere Zielmenge für einen auswertbaren Ausdruck ab.
 *
 * Die Inferenz ist absichtlich konservativ. Sie beschreibt einen Träger, der
 * alle möglichen Werte enthalten kann, nicht die exakte Bildmenge.
 */
fun inferiereZielmenge(
    ausdruck: MathematischesObjekt,
    werteVorräte: Map<String, MengenAusdruck> = emptyMap(),
    annahmen: Set<Aussage> = emptySet(),
): MengenAusdruck = when (ausdruck) {
    is ZahlAusdruck -> inferiereZahlenWertevorrat(ausdruck, werteVorräte, annahmen)
    is AllgemeinerParameter -> werteVorräte[ausdruck.name]
        ?: error("Für den allgemeinen Parameter '${ausdruck.name}' fehlt ein Wertevorrat.")
    is Aussage -> Wahrheitsmenge
    is MengenAusdruck -> inferiereElementMenge(ausdruck, werteVorräte, annahmen)
    is Tupel -> Tupelraum(ausdruck.elemente.map { inferiereZielmenge(it, werteVorräte, annahmen) })
    is SpaltenVektor -> Vektorraum(
        VektorOrientierung.Spalte,
        ausdruck.werte.size,
        maximaleZahlenGrundmenge(ausdruck.werte.map { inferiereZahlenWertevorrat(it, werteVorräte, annahmen) }),
    )
    is ZeilenVektor -> Vektorraum(
        VektorOrientierung.Zeile,
        ausdruck.werte.size,
        maximaleZahlenGrundmenge(ausdruck.werte.map { inferiereZahlenWertevorrat(it, werteVorräte, annahmen) }),
    )
    is Matrix -> Matrizenraum(
        ausdruck.zeilenAnzahl,
        ausdruck.spaltenAnzahl,
        maximaleZahlenGrundmenge(ausdruck.zeilen.flatten().map { inferiereZahlenWertevorrat(it, werteVorräte, annahmen) }),
    )
    is Funktion, is GebundeneFunktion, is Mächtigkeit ->
        error("Für ${ausdruck::class.simpleName} ist noch keine Zielmengeninferenz definiert.")
}

private val Wahrheitsmenge = EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)))

private fun inferiereElementMenge(
    menge: MengenAusdruck,
    werteVorräte: Map<String, MengenAusdruck>,
    annahmen: Set<Aussage>,
): MengenAusdruck = when (menge) {
    NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen, KomplexeZahlen, is BenannteMenge -> menge
    LeereMenge -> LeereMenge
    is EndlicheMenge -> if (menge.elemente.isEmpty()) LeereMenge else vereinige(menge.elemente.map {
        inferiereZielmenge(it, werteVorräte, annahmen)
    })
    is ReellesIntervall -> ReelleZahlen
    is Vereinigung -> vereinige(menge.mengen.map { inferiereElementMenge(it, werteVorräte, annahmen) })
    is Schnitt -> menge.grundMenge?.let { inferiereElementMenge(it, werteVorräte, annahmen) }
        ?: vereinige(menge.mengen.map { inferiereElementMenge(it, werteVorräte, annahmen) })
    is MengenDifferenz -> inferiereElementMenge(menge.links, werteVorräte, annahmen)
    is KartesischesProdukt -> Tupelraum(menge.mengen)
    is DefinierteMenge -> if (menge.variablen.size == 1) menge.variablen.single().grundMenge
        else Tupelraum(menge.variablen.map { it.grundMenge })
    is Abbild -> menge.methode.einzigeZielMenge
    is IterierteVereinigung -> menge.methode.grundMengeFürMengenAusgabe()
    is IterierterSchnitt -> menge.methode.grundMengeFürMengenAusgabe()
    is IteriertesKartesischesProdukt -> Folgenraum(menge.methode.grundMengeFürMengenAusgabe())
    is Tupelraum, is Folgenraum, is Vektorraum, is Matrizenraum -> menge
}
