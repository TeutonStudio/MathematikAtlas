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
    is TypisiertesElement -> werteVorräte[ausdruck.name]
        ?: error(
            "Für das gebundene Element '${ausdruck.name}' der Anschlussart " +
                "'${ausdruck.anschlussArt}' ist keine Obermenge festgelegt.",
        )
    is Aussage -> Wahrheitsmenge
    is MengenAusdruck -> inferiereElementMenge(ausdruck, werteVorräte, annahmen)
    is FallAusdruck -> when (ausdruck.aussage.entscheide(RechenKontext(annahmen)).wahrheitswert) {
        Wahrheitswert.Wahr -> inferiereZielmenge(ausdruck.wahr, werteVorräte, annahmen + ausdruck.aussage)
        Wahrheitswert.Lüge -> inferiereZielmenge(ausdruck.lüge, werteVorräte, annahmen + Negation(ausdruck.aussage))
        null -> vereinige(listOf(
            inferiereZielmenge(ausdruck.wahr, werteVorräte, annahmen + ausdruck.aussage),
            inferiereZielmenge(ausdruck.lüge, werteVorräte, annahmen + Negation(ausdruck.aussage)),
        ))
    }
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
    is GeometrischerAusdruck -> BenannteMenge("geometrie_${ausdruck.raum.id}", "\\mathcal{G}(${ausdruck.raum.id})")
    is EuklidischerRaum -> BenannteMenge("euklidische_raeume", "\\mathfrak{E}")
    is GeometrischesKoordinatensystem -> BenannteMenge("koordinatensysteme_${ausdruck.raum.id}", "\\mathcal{K}(${ausdruck.raum.id})")
    is GeometrieStruktur -> BenannteMenge("geometriestrukturen_${ausdruck.raum.id}", "\\mathcal{C}(${ausdruck.raum.id})")
    is GeometrischeTransformation -> BenannteMenge("geometrietransformationen", "\\operatorname{Trans}_{G}")
    is Funktion, is GebundeneFunktion, is Mächtigkeit ->
        error("Für ${ausdruck::class.simpleName} ist noch keine Zielmengeninferenz definiert.")
}

private val Wahrheitsmenge = EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)))

private fun inferiereElementMenge(
    menge: MengenAusdruck,
    werteVorräte: Map<String, MengenAusdruck>,
    annahmen: Set<Aussage>,
): MengenAusdruck = when (menge) {
    NatürlicheZahlen, GanzeZahlen, RationaleZahlen, ReelleZahlen, KomplexeZahlen,
    Primzahlen, GaußscheGanzeZahlen, GaußschePrimzahlen, is BenannteMenge -> menge
    LeereMenge -> LeereMenge
    is EndlicheMenge -> if (menge.elemente.isEmpty()) LeereMenge else vereinige(menge.elemente.map {
        inferiereZielmenge(it, werteVorräte, annahmen)
    })
    is ReellesIntervall -> ReelleZahlen
    is Vereinigung -> vereinige(menge.mengen.map { inferiereElementMenge(it, werteVorräte, annahmen) })
    is Schnitt -> menge.grundMenge?.let { inferiereElementMenge(it, werteVorräte, annahmen) }
        ?: vereinige(menge.mengen.map { inferiereElementMenge(it, werteVorräte, annahmen) })
    is MengenDifferenz -> inferiereElementMenge(menge.links, werteVorräte, annahmen)
    is SymmetrischeDifferenz -> vereinige(listOf(
        inferiereElementMenge(menge.links, werteVorräte, annahmen),
        inferiereElementMenge(menge.rechts, werteVorräte, annahmen),
    ))
    is KartesischesProdukt -> Tupelraum(menge.mengen)
    is DefinierteMenge -> if (menge.variablen.size == 1) menge.variablen.single().grundMenge
        else Tupelraum(menge.variablen.map { it.grundMenge })
    is GefilterteMenge -> inferiereElementMenge(menge.menge, werteVorräte, annahmen)
    is MengenParameter -> error(
        "Für die Mengenvariable '${menge.name}' ist keine Element- oder Obermenge festgelegt.",
    )
    is PrädikatsMenge -> error(
        "Die Prädikatsmenge ${menge.zuLatex()} legt bewusst keine Obermenge fest. " +
            "Die aktuelle Operation benötigt jedoch eine.",
    )
    is FehlendeObermenge -> error(
        "Für die Anschlussart '${menge.anschlussArt}' ist keine Obermenge festgelegt.",
    )
    is Abbild -> menge.methode.einzigeZielMenge
    is IterierteVereinigung -> menge.methode.grundMengeFürMengenAusgabe()
    is IterierterSchnitt -> menge.methode.grundMengeFürMengenAusgabe()
    is IteriertesKartesischesProdukt -> Folgenraum(menge.methode.grundMengeFürMengenAusgabe())
    is MengenFallAusdruck -> when (menge.aussage.entscheide(RechenKontext(annahmen)).wahrheitswert) {
        Wahrheitswert.Wahr -> inferiereElementMenge(menge.wahr, werteVorräte, annahmen + menge.aussage)
        Wahrheitswert.Lüge -> inferiereElementMenge(menge.lüge, werteVorräte, annahmen + Negation(menge.aussage))
        null -> vereinige(listOf(
            inferiereElementMenge(menge.wahr, werteVorräte, annahmen + menge.aussage),
            inferiereElementMenge(menge.lüge, werteVorräte, annahmen + Negation(menge.aussage)),
        ))
    }
    is GeometrischeTrägermenge -> BenannteMenge("punkte_${menge.objekt.raum.id}", "\\mathcal{P}(${menge.objekt.raum.id})")
    is KoordinatenBild -> Tupelraum(List(menge.objekt.raum.dimension) { ReelleZahlen })
    is Tupelraum, is Folgenraum, is Vektorraum, is Matrizenraum,
    is Potenzmenge, is Abbildungsmenge, is Tensorraum, is ModuloZahlenraum -> menge
}
