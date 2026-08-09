package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Kanonisiert definierte Mengen nur dann, wenn ihre Bedingung ohne weitere
 * Belegung bereits entscheidbar ist.
 *
 * Damit bleiben echte Prädikate wie {x∈R | x≥0} strukturell erhalten, während
 * triviale Resultate wie {x∈R | 2≥0} nicht als unnötige Hülle weitergetragen
 * werden.
 */
fun normalisiereDefinierteMenge(
    menge: DefinierteMenge,
    kontext: RechenKontext = RechenKontext(),
): MengenAusdruck = when (menge.bedingung.entscheide(kontext).wahrheitswert) {
    Wahrheitswert.Wahr -> grundRaum(menge.variablen)
    Wahrheitswert.Lüge -> LeereMenge
    null -> menge
}

/** Normalisiert nur bekannte triviale Mengenhüllen; andere Mengentypen bleiben identisch. */
fun normalisiereMenge(
    menge: MengenAusdruck,
    kontext: RechenKontext = RechenKontext(),
): MengenAusdruck = when (menge) {
    is DefinierteMenge -> normalisiereDefinierteMenge(menge, kontext)
    else -> menge
}

private fun grundRaum(variablen: List<GebundeneMengenVariable>): MengenAusdruck = when (variablen.size) {
    1 -> variablen.single().grundMenge
    else -> kartesischesProdukt(variablen.map { it.grundMenge })
}
