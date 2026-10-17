package de.TeutonStudio.MathematikRechenSystem.kern

/** Tupelergebnis mit den noch offenen fachlichen Voraussetzungen. */
data class MathematischeAnwendung(
    val ergebnis: Tupel,
    val bedingungen: Set<Aussage>,
)

/** Prüft Komponenten UND den gemeinsamen Definitionsraum, nicht nur die Anzahl. */
fun MathematischeMethode.prüfeDefinitionsBedingungen(argumente: Tupel): Set<Aussage> {
    require(argumente.anzahl == parameter.size) { "Falsche Argumentanzahl für '$name'." }
    prüfeMethodenArgumentSignaturen(parameter, argumente.elemente)
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
    val ergebnis=wendeMathematischAlsTupelAn(argumente)
    return MathematischeAnwendung(ergebnis,
        entscheidungen.filterValues {it.wahrheitswert!=Wahrheitswert.Wahr}.keys+
            prüfeNumerischeMethodenErgebnisBedingungen(name,ergebnisMengenVertrag,ergebnis,bindungen,kontext.copy(annahmen=kontext.annahmen+bedingungen)))
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

/**
 * Identitätsbasierter Zugang zur offenen Mathematik-Capability. Deren historische
 * namensbasierte Signatur bleibt ausschließlich an dieser Grenze erhalten.
 */
fun MathematischAuswertbareMethode.wendeMitIdentitätenAn(
    bindungen: Map<String, MathematischesObjekt>,
    kontext: RechenKontext = RechenKontext(),
): MathematischeAnwendung {
    val konkret = runCatching { alsMathematischeMethode("kanonische Anwendung") }.getOrNull()
    if (konkret != null) return konkret.wendeMitIdentitätenAn(bindungen, kontext)
    val s = mathematischeSignatur
    val bedingungen = prüfeDefinitionsBedingungen(bindungen, kontext)
    val entscheidungen = bedingungen.associateWith { it.entscheide(kontext) }
    require(entscheidungen.values.none { it.wahrheitswert == Wahrheitswert.Lüge }) { "Die Argumente liegen außerhalb des Definitionsraums der Methode '$name'." }
    val roh = wendeMathematischAn(s.argumente.associate { it.name to bindungen.getValue(it.id) })
    val ergebnis = if (s.ergebnisse.size == 1) Tupel(listOf(roh)) else roh as? Tupel
        ?: error("Mehrere mathematische Ergebnisse benötigen ein Tupel.")
    require(ergebnis.anzahl == s.ergebnisse.size) { "Die Ergebnisanzahl stimmt nicht mit der Methodensignatur überein." }
    val substitution=s.argumente.associate {(it.parameter.bindungsId ?: it.name) to bindungen.getValue(it.id)}
    return MathematischeAnwendung(ergebnis, entscheidungen.filterValues {it.wahrheitswert!=Wahrheitswert.Wahr}.keys+
        prüfeNumerischeMethodenErgebnisBedingungen(name,s.ergebnisse.map {it.name to it.zielMenge},ergebnis,substitution,kontext.copy(annahmen=kontext.annahmen+bedingungen)))
}

/** Prüft den Definitionsvertrag einer offenen Capability ohne sie auszuführen. */
fun MathematischAuswertbareMethode.prüfeDefinitionsBedingungen(bindungen: Map<String, MathematischesObjekt>, kontext: RechenKontext = RechenKontext()): Set<Aussage> {
    val s = mathematischeSignatur
    require(bindungen.keys == s.argumente.map { it.id }.toSet()) { "Die Argumentbindungen stimmen nicht mit den Signaturidentitäten überein." }
    val konkret = runCatching { alsMathematischeMethode("Definitionsprüfung") }.getOrNull()
    if (konkret != null) {
        val argumente = Tupel(s.argumente.map { bindungen.getValue(it.id) })
        val substitution = konkret.parameter.mapIndexed { i,p -> (p.bindungsId ?: p.name) to argumente.elemente[i] }.toMap()
        return konkret.prüfeDefinitionsBedingungen(argumente) + konkret.aufgerufeneVoraussetzungen(substitution, kontext)
    }
    require(bindungen.keys == s.argumente.map { it.id }.toSet()) { "Die Argumentbindungen stimmen nicht mit den Signaturidentitäten überein." }
    require(s.argumente.map { it.name }.distinct().size == s.argumente.size) { "Die namensbasierte Mathematik-Capability benötigt eindeutige Argumentnamen." }
    prüfeMethodenArgumentSignaturen(s.argumente.map { it.parameter }, s.argumente.map { bindungen.getValue(it.id) })
    val substitution = s.argumente.associate { (it.parameter.bindungsId ?: it.name) to bindungen.getValue(it.id) }
    val argumente = Tupel(s.argumente.map { bindungen.getValue(it.id) })
    val bedingungen = if (s.effektiverDefinitionsRaum != null) {
        setOf(ElementBeziehung(argumente, ersetze(s.definitionsRaum, substitution) as MengenAusdruck))
    } else s.argumente.map { ElementBeziehung(bindungen.getValue(it.id), ersetze(it.definitionsMenge, substitution) as MengenAusdruck) }.toSet()
    return bedingungen
}

/** Höhere Argumente benötigen unabhängig von der Mengennachweisbarkeit eine passende Signatur. */
private fun prüfeMethodenArgumentSignaturen(parameter: List<MethodenParameter>, argumente: List<MathematischesObjekt>) {
    parameter.forEachIndexed { i, p ->
        if (p is MethodenVariable) {
            val wert = argumente[i] as? SignaturtragendeMethode ?: error("Das Argument '${p.name}' benötigt einen Methodenwert.")
            require(MathematischeTypen.typSystem.prüfe(wert.signatur.typAusdruck, p.signatur.typAusdruck) !is de.TeutonStudio.TypSystem.TypPrüfung.Inkompatibel) {
                "Die Methodensignatur des Arguments '${p.name}' ist inkompatibel."
            }
        }
    }
}
