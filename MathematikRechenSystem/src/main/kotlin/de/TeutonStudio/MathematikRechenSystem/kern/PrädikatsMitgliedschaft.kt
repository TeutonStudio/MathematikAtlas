package de.TeutonStudio.MathematikRechenSystem.kern

/** Membership in constructed solution sets uses the same original domain checks as solving. */
internal fun entscheidePrädikatsMitgliedschaft(
    element: MathematischesObjekt,
    menge: DefinierteMenge,
    kontext: RechenKontext,
): AussageErgebnis {
    val werte = if (menge.variablen.size == 1) listOf(element) else (element as? Tupel)?.elemente
        ?: return AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt, "Eine Tupelbelegung ist erforderlich.")
    if (werte.size != menge.variablen.size) return AussageErgebnis(Wahrheitswert.Lüge,
        EntscheidungsStatus.Widerlegt, "Die Tupeldimension stimmt nicht mit dem Lösungsraum überein.")
    val bindungen = menge.variablen.mapIndexed { i, v -> v.variable.name to werte[i] }.toMap()
    val bereich = Konjunktion(menge.variablen.mapIndexed { i, v -> ElementBeziehung(werte[i], v.grundMenge) }).entscheide(kontext)
    if (bereich.wahrheitswert == Wahrheitswert.Lüge) return bereich
    val bedingung = ersetze(menge.bedingung, bindungen)
    val lage = prüfeDefinitionslage(bedingung, kontext)
    if (lage.wahrheitswert == Wahrheitswert.Lüge) return AussageErgebnis(Wahrheitswert.Lüge,
        EntscheidungsStatus.Widerlegt, "Die Belegung liegt außerhalb des Definitionsbereichs.")
    if (lage.wahrheitswert == null) return AussageErgebnis(null, EntscheidungsStatus.Unbekannt,
        "Die Definitionslage der Belegung ist noch offen.")
    val e = bedingung.entscheide(kontext)
    return if (bereich.wahrheitswert == Wahrheitswert.Wahr || e.wahrheitswert == Wahrheitswert.Lüge) e
        else AussageErgebnis(null, EntscheidungsStatus.Unbekannt, "Die Bereichszugehörigkeit ist noch offen.")
}

internal fun entscheideBeschränkteMitgliedschaft(
    element: MathematischesObjekt,
    menge: BeschraenkteZahlmenge,
    kontext: RechenKontext,
): AussageErgebnis {
    val wert = element as? ZahlAusdruck ?: return AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt)
    return Konjunktion(listOfNotNull(ElementBeziehung(element, menge.traeger.alsMenge()),
        menge.untereGrenze?.let { Vergleich(it, if (menge.untereGrenzeEnthalten) VergleichsArt.KleinerGleich else VergleichsArt.Kleiner, wert) },
        menge.obereGrenze?.let { Vergleich(wert, if (menge.obereGrenzeEnthalten) VergleichsArt.KleinerGleich else VergleichsArt.Kleiner, it) }
    )).entscheide(kontext)
}
