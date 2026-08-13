package de.TeutonStudio.MathematikRechenSystem.kern

/** Ergebnis einer strukturierten Bijektivitätsprüfung für die Umkehrmethode. */
enum class BijektivitaetsStatus {
    BEWIESEN,
    NICHT_INJEKTIV,
    NICHT_SURJEKTIV,
    UNBEKANNT,
}

/** Sichtbare Voraussetzung, wenn Bijektivität nicht aus dem aktuellen Modell folgt. */
data class BijektivitaetsVoraussetzung(val methode: Methode) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis = AussageErgebnis(
        wahrheitswert = null,
        status = EntscheidungsStatus.Unbekannt,
        begründung = "Die Bijektivität von '${methode.name}' ist im aktuellen Kontext nicht bewiesen.",
    )

    override fun zuLatex(): String = "\\operatorname{bijektiv}\\left(${methode.name}\\right)"
}

/**
 * Strukturierter inverser Methodenoperator für noch nicht entschiedene Bijektivität.
 * Er besitzt bereits exakt die vertauschte Tupel-Signatur, ist aber absichtlich nicht
 * mathematisch auswertbar, solange der notwendige Nachweis fehlt.
 */
data class BedingteInverseMethode(
    val original: MathematischeSignaturtragendeMethode,
    val voraussetzungen: Set<Aussage> = setOf(BijektivitaetsVoraussetzung(original)),
) : MathematischeSignaturtragendeMethode, MathematischesObjekt {
    override val name: String = "${original.name}^{⟨-1⟩}"
    override val mathematischeSignatur: MathematischeMethodenSignatur = original.mathematischeSignatur.invertiert()
    override fun zuLatex(): String = "${original.name}^{\\langle-1\\rangle}"
}

/** Exakt ausgewertete inverse Methode einer endlichen Bijektion. */
data class EndlicheInverseMethode(
    val original: MathematischAuswertbareMethode,
    private val inverseTabelle: Map<Tupel, Tupel>,
) : MathematischAuswertbareMethode {
    override val name: String = "${original.name}^{⟨-1⟩}"
    override val mathematischeSignatur: MathematischeMethodenSignatur = original.mathematischeSignatur.invertiert()

    override fun wendeMathematischAn(argumente: Map<String, MathematischesObjekt>): MathematischesObjekt {
        val eingang = Tupel(mathematischeSignatur.argumente.map { komponente ->
            argumente[komponente.name]
                ?: error("Für die inverse Methode '$name' fehlt das Argument '${komponente.name}'.")
        })
        val ergebnis = inverseTabelle[eingang]
            ?: error("Der Wert ${eingang.zuLatex()} liegt nicht im Definitionsraum der inversen Methode '$name'.")
        return when (ergebnis.elemente.size) {
            0 -> ergebnis
            1 -> ergebnis.elemente.single()
            else -> ergebnis
        }
    }

    fun wendeAlsTupelAn(argumente: Tupel): Tupel = inverseTabelle[argumente]
        ?: error("Der Wert ${argumente.zuLatex()} liegt nicht im Definitionsraum der inversen Methode '$name'.")

    override fun zuLatex(): String = "${original.name}^{\\langle-1\\rangle}"
}

sealed interface UmkehrMethodenErgebnis {
    val status: BijektivitaetsStatus

    data class Gueltig(
        val methode: EndlicheInverseMethode,
    ) : UmkehrMethodenErgebnis {
        override val status: BijektivitaetsStatus = BijektivitaetsStatus.BEWIESEN
    }

    data class Bedingt(
        val methode: BedingteInverseMethode,
    ) : UmkehrMethodenErgebnis {
        override val status: BijektivitaetsStatus = BijektivitaetsStatus.UNBEKANNT
    }

    data class Ungueltig(
        override val status: BijektivitaetsStatus,
        val begruendung: String,
    ) : UmkehrMethodenErgebnis {
        init { require(status == BijektivitaetsStatus.NICHT_INJEKTIV || status == BijektivitaetsStatus.NICHT_SURJEKTIV) }
    }
}

/**
 * Konstruiert `f^{⟨-1⟩}`. Endliche Definitions- und Zielräume werden vollständig und
 * beweisend geprüft. Für nicht vollständig materialisierbare Räume bleibt exakt eine
 * strukturierte Bijektivitätsvoraussetzung erhalten; es werden keine Stichproben als
 * Beweis missbraucht.
 */
fun umkehrMethode(methode: Methode): UmkehrMethodenErgebnis {
    val mathematisch = methode as? MathematischeSignaturtragendeMethode
        ?: error("Die Umkehrfunktion benötigt eine mathematische Raum-/Mengensignatur.")
    val auswertbar = methode as? MathematischAuswertbareMethode
        ?: return UmkehrMethodenErgebnis.Bedingt(BedingteInverseMethode(mathematisch))
    val signatur = mathematisch.mathematischeSignatur
    val definitionsTupel = materialisiereDefinitionsRaum(signatur) ?: return UmkehrMethodenErgebnis.Bedingt(
        BedingteInverseMethode(mathematisch),
    )
    val zielTupel = signatur.zielRaum.materialisiereTupelraum() ?: return UmkehrMethodenErgebnis.Bedingt(
        BedingteInverseMethode(mathematisch),
    )

    val bildZuUrbild = linkedMapOf<Tupel, Tupel>()
    for (argument in definitionsTupel) {
        val bindungen = signatur.argumente.mapIndexed { index, komponente ->
            komponente.name to argument.wertAnPosition(index)
        }.toMap()
        val roh = auswertbar.wendeMathematischAn(bindungen)
        val bild = normalisiereErgebnisTupel(roh, signatur.ergebnisse.size)
        val vorhanden = bildZuUrbild.putIfAbsent(bild, argument)
        if (vorhanden != null && vorhanden != argument) {
            return UmkehrMethodenErgebnis.Ungueltig(
                BijektivitaetsStatus.NICHT_INJEKTIV,
                "Die Methode '${methode.name}' bildet ${vorhanden.zuLatex()} und ${argument.zuLatex()} beide auf ${bild.zuLatex()} ab.",
            )
        }
    }

    val zielSet = zielTupel.toSet()
    val bildSet = bildZuUrbild.keys
    if (bildSet != zielSet) {
        val fehlend = (zielSet - bildSet).firstOrNull()
        return UmkehrMethodenErgebnis.Ungueltig(
            BijektivitaetsStatus.NICHT_SURJEKTIV,
            "Die Methode '${methode.name}' trifft die deklarierte Zielmenge nicht vollständig" +
                (fehlend?.let { "; beispielsweise fehlt ${it.zuLatex()}" } ?: "") + ".",
        )
    }

    return UmkehrMethodenErgebnis.Gueltig(EndlicheInverseMethode(auswertbar, bildZuUrbild))
}

private fun MathematischeMethodenSignatur.invertiert(): MathematischeMethodenSignatur =
    MathematischeMethodenSignatur(
        argumente = ergebnisse.mapIndexed { index, ergebnis ->
            val name = ergebnis.name.ifBlank { "y${index + 1}" }
            MathematischeArgumentKomponente(
                id = "inverse-argument:${ergebnis.id}",
                name = name,
                position = index,
                parameter = AllgemeinerParameter(name),
                definitionsMenge = ergebnis.zielMenge,
            )
        },
        ergebnisse = argumente.mapIndexed { index, argument ->
            MathematischeErgebnisKomponente(
                id = "inverse-ergebnis:${argument.id}",
                name = argument.name,
                position = index,
                zielMenge = argument.definitionsMenge,
            )
        },
    )

private fun materialisiereDefinitionsRaum(signatur: MathematischeMethodenSignatur): List<Tupel>? = when (val raum = signatur.definitionsRaum) {
    is Tupelraum -> raum.materialisiereTupelraum()
    is EndlicheMenge -> raum.elemente.sortedBy(::strukturellerSchlüssel).map { wert ->
        when {
            wert is Tupel && wert.anzahl == signatur.argumente.size -> wert
            signatur.argumente.size == 1 -> Tupel(listOf(wert))
            else -> return null
        }
    }
    else -> null
}

private fun Tupelraum.materialisiereTupelraum(): List<Tupel>? {
    if (komponenten.isEmpty()) return listOf(Tupel(emptyList()))
    val endliche = komponenten.map { it as? EndlicheMenge ?: return null }
    return endliche.fold(listOf(Tupel(emptyList()))) { bisher, menge ->
        val elemente = menge.elemente.sortedBy(::strukturellerSchlüssel)
        bisher.flatMap { prefix -> elemente.map { element -> Tupel(prefix.elemente + element) } }
    }
}

private fun normalisiereErgebnisTupel(wert: MathematischesObjekt, erwarteteAnzahl: Int): Tupel = when {
    erwarteteAnzahl == 0 -> {
        require(wert is Tupel && wert.anzahl == 0) { "Eine ergebnislose Methode muss () liefern." }
        wert
    }
    wert is Tupel && wert.anzahl == erwarteteAnzahl -> wert
    erwarteteAnzahl == 1 -> Tupel(listOf(wert))
    else -> error("Die Methode liefert keinen Ergebnistupel der erwarteten Länge $erwarteteAnzahl.")
}
