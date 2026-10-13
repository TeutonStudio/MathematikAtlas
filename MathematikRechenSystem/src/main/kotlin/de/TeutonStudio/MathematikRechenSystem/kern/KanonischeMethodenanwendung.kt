package de.TeutonStudio.MathematikRechenSystem.kern

/** Tupelergebnis mit den noch offenen fachlichen Voraussetzungen. */
data class MathematischeAnwendung(
    val ergebnis: Tupel,
    val bedingungen: Set<Aussage>,
)

/** Prüft Komponenten UND den gemeinsamen Definitionsraum, nicht nur die Anzahl. */
fun MathematischeMethode.prüfeDefinitionsBedingungen(argumente: Tupel): Set<Aussage> {
    require(argumente.anzahl == parameter.size) { "Falsche Argumentanzahl für '$name'." }
    parameter.forEachIndexed { i, p ->
        if (p is MethodenVariable) {
            val wert = argumente.elemente[i] as? SignaturtragendeMethode ?: error("Das Argument '${p.name}' benötigt einen Methodenwert.")
            require(MathematischeTypen.typSystem.prüfe(wert.signatur.typAusdruck, p.signatur.typAusdruck) !is de.TeutonStudio.TypSystem.TypPrüfung.Inkompatibel) {
                "Die Methodensignatur des Arguments '${p.name}' ist inkompatibel."
            }
        }
    }
    val bindungen = parameter.mapIndexed { i, p -> (p.bindungsId ?: p.name) to argumente.elemente[i] }.toMap()
    val komponenten = if (effektiverWerteVorrat != null) emptyList() else parameter.mapIndexedNotNull { i, p ->
        werteVorräte[p.name]?.let { ElementBeziehung(argumente.elemente[i], ersetze(it, bindungen) as MengenAusdruck) }
    }
    val gemeinsam = effektiverWerteVorrat?.let {
        val normalisiert = mathematischeSignatur.definitionsRaum
        ElementBeziehung(argumente, ersetze(normalisiert, bindungen) as MengenAusdruck)
    }
    return (komponenten + listOfNotNull(gemeinsam) + voraussetzungen.map { ersetze(it, bindungen) }).toSet()
}

fun MathematischeMethode.wendeMitBedingungenAn(
    argumente: Tupel,
    kontext: RechenKontext = RechenKontext(),
): MathematischeAnwendung {
    require(argumente.anzahl == parameter.size) { "Falsche Argumentanzahl für '$name'." }
    val bindungen = parameter.mapIndexed { i, p -> (p.bindungsId ?: p.name) to argumente.elemente[i] }.toMap()
    val bedingungen = prüfeDefinitionsBedingungen(argumente) + aufgerufeneVoraussetzungen(bindungen, kontext)
    val entscheidungen = bedingungen.associateWith { it.entscheide(kontext) }
    require(entscheidungen.values.none { it.wahrheitswert == Wahrheitswert.Lüge }) {
        "Die Argumente liegen außerhalb des Definitionsraums der Methode '$name'."
    }
    return MathematischeAnwendung(
        wendeMathematischAlsTupelAn(argumente),
        entscheidungen.filterValues { it.wahrheitswert != Wahrheitswert.Wahr }.keys,
    )
}

/** Neue Aufrufer binden ausschließlich anhand der Signaturidentitäten. */
fun MathematischeMethode.wendeMitIdentitätenAn(
    bindungen: Map<String, MathematischesObjekt>,
    kontext: RechenKontext = RechenKontext(),
): MathematischeAnwendung {
    val ids = parameter.map { argumentIdentitäten.getValue(it.name) }
    require(bindungen.keys == ids.toSet()) { "Die Argumentbindungen stimmen nicht mit den Signaturidentitäten überein." }
    return wendeMitBedingungenAn(Tupel(ids.map(bindungen::getValue)), kontext)
}

/** Partielle Bindung erzeugt eine Methode; alle verbleibenden Identitäten bleiben bestehen. */
fun MathematischeMethode.bindeIdentitäten(bindungen: Map<String, MathematischesObjekt>): MathematischeMethode {
    val nachId = parameter.associateBy { argumentIdentitäten.getValue(it.name) }
    require(bindungen.keys.all { it in nachId }) { "Unbekannte Argumentidentität." }
    val substitution = bindungen.mapKeys { (id, _) -> nachId.getValue(id).let { it.bindungsId ?: it.name } }
    val gebundeneNamen = bindungen.keys.map { nachId.getValue(it).name }.toSet()
    val rest = parameter.filterNot { it.name in gebundeneNamen }
    // Ein gemeinsamer Raum wird als Voraussetzung der ursprünglichen Tupelstruktur erhalten.
    val ursprünglicheArgumente = Tupel(parameter.map { substitution[it.bindungsId ?: it.name] ?: it })
    val domäne = effektiverWerteVorrat?.let { ElementBeziehung(ursprünglicheArgumente, mathematischeSignatur.definitionsRaum) }
    return copy(
        parameter = rest,
        übersetzungen = übersetzungen.alsHerkunft(),
        vorschrift = ersetze(vorschrift, substitution),
        zielMenge = ersetze(zielMenge, substitution) as MengenAusdruck,
        werteVorräte = werteVorräte.filterKeys { it !in gebundeneNamen }.mapValues { ersetze(it.value, substitution) as MengenAusdruck },
        effektiverWerteVorrat = null,
        voraussetzungen = (voraussetzungen + listOfNotNull(domäne) + aufgerufeneVoraussetzungen(substitution, RechenKontext())).map { ersetze(it, substitution) }.toSet() +
            parameter.filter { it.name in gebundeneNamen }.mapNotNull { p ->
                werteVorräte[p.name]?.let { ElementBeziehung(substitution.getValue(p.bindungsId ?: p.name), ersetze(it, substitution) as MengenAusdruck) }
            },
    )
}
