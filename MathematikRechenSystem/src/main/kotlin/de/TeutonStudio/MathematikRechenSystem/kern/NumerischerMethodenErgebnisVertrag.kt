package de.TeutonStudio.MathematikRechenSystem.kern

/** Eine deklarierte Ergebnismenge ist ein Vertrag, keine nachträgliche Umdeutung des Werts. */
internal fun prüfeNumerischeMethodenErgebnisBedingungen(
    name:String,
    ergebnisse:List<Pair<String,MengenAusdruck>>,
    ergebnis:Tupel,
    substitution:Map<String,MathematischesObjekt>,
    kontext:RechenKontext=RechenKontext(),
):Set<Aussage> {
    require(ergebnis.anzahl==ergebnisse.size) {"Die Ergebnisanzahl stimmt nicht mit der Methodensignatur überein."}
    return ergebnisse.mapIndexedNotNull {i,komponente ->
        val ziel=ersetze(komponente.second,substitution) as MengenAusdruck
        // Mengenfamilien interpretieren ihr Ziel als Grundmenge (ADR 2026-07-27).
        // Dieser Schutz betrifft ausschließlich den nachgewiesenen Zahlbereichsvertrag.
        if(ergebnis.elemente[i] !is ZahlAusdruck || runCatching {nachgewieseneZahlenGrundmenge(ziel)}.getOrNull()==null) return@mapIndexedNotNull null
        val bedingung=ElementBeziehung(ergebnis.elemente[i],ziel)
        val entscheidung=bedingung.entscheide(kontext)
        require(entscheidung.wahrheitswert!=Wahrheitswert.Lüge) {
            "Das Ergebnis '${komponente.first}' der Methode '$name' liegt außerhalb der deklarierten Zielmenge ${ziel.zuLatex()}."
        }
        bedingung.takeUnless {entscheidung.wahrheitswert==Wahrheitswert.Wahr}
    }.toSet()
}
