package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypId
import de.TeutonStudio.TypSystem.TypPrüfung

/** Die eine wiederverwendbare Zielmenge aller Prädikate. */
val WahrheitsMenge: MengenAusdruck = EndlicheMenge(
    setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)),
)

/**
 * Domänenneutrale Komponente einer Methodensignatur.
 *
 * Die stabile [id] ist Identität; [name] ist ausschließlich sichtbare Benennung.
 * Die [position] macht die Reihenfolge explizit. Mathematische Mengen gehören
 * ausdrücklich nicht in diesen Vertrag.
 */
data class MethodenKomponente(
    val id: String,
    val name: String,
    val position: Int,
    val typ: TypAusdruck,
) {
    init {
        require(id.isNotBlank()) { "Eine Methodenkomponente benötigt eine stabile ID." }
        require(position >= 0) { "Die Position einer Methodenkomponente darf nicht negativ sein." }
    }
}

/**
 * Neutrale semantische Signatur einer Methode.
 *
 * Argument- und Ergebnisstruktur sind symmetrisch. Intern ist der Methodenvertrag
 * dadurch immer `Tupel<A1,...,An> -> Tupel<R1,...,Rm>`, auch für 0 oder 1 Komponente.
 */
data class MethodenSignatur(
    val argumente: List<MethodenKomponente>,
    val ergebnisse: List<MethodenKomponente>,
) {
    init {
        prüfeKomponenten(argumente, "Argument")
        prüfeKomponenten(ergebnisse, "Ergebnis")
    }

    val argumentTupelTyp: TypAusdruck.Parameterisiert
        get() = TypAusdruck.Parameterisiert(
            MathematischeTypen.Tupel,
            argumente.sortedBy(MethodenKomponente::position).map(MethodenKomponente::typ),
        )

    val ergebnisTupelTyp: TypAusdruck.Parameterisiert
        get() = TypAusdruck.Parameterisiert(
            MathematischeTypen.Tupel,
            ergebnisse.sortedBy(MethodenKomponente::position).map(MethodenKomponente::typ),
        )

    private fun prüfeKomponenten(komponenten: List<MethodenKomponente>, rolle: String) {
        require(komponenten.map(MethodenKomponente::id).distinct().size == komponenten.size) {
            "$rolle-Komponenten benötigen eindeutige stabile IDs."
        }
        require(komponenten.map(MethodenKomponente::position).sorted() == komponenten.indices.toList()) {
            "$rolle-Komponenten benötigen lückenlose Positionen 0..n-1."
        }
    }
}

/** Mathematische Metadaten einer Argumentkomponente. */
data class MathematischeArgumentKomponente(
    val id: String,
    val name: String,
    val position: Int,
    val parameter: MethodenParameter,
    val definitionsMenge: MengenAusdruck,
)

/** Mathematische Metadaten einer Ergebniskomponente. */
data class MathematischeErgebnisKomponente(
    val id: String,
    val name: String,
    val position: Int,
    val zielMenge: MengenAusdruck,
)

/**
 * Zusätzliche mathematische Raum-/Mengensignatur einer Methode.
 *
 * Komponenten-Definitionsmengen und Zielmengen werden nicht mit ihren Typen
 * verwechselt. Der kanonische Argumentraum und der Zielraum sind stets Tupelräume.
 */
data class MathematischeMethodenSignatur(
    val argumente: List<MathematischeArgumentKomponente>,
    val ergebnisse: List<MathematischeErgebnisKomponente>,
    val effektiverDefinitionsRaum: MengenAusdruck? = null,
) {
    init {
        prüfePositionen(argumente.map { it.id to it.position }, "Argument")
        prüfePositionen(ergebnisse.map { it.id to it.position }, "Ergebnis")
    }

    val kanonischerArgumentRaum: Tupelraum
        get() = Tupelraum(
            argumente.sortedBy(MathematischeArgumentKomponente::position)
                .map(MathematischeArgumentKomponente::definitionsMenge),
        )

    /**
     * Tatsächlicher mathematischer Definitionsraum. Historische nullstellige Daten,
     * die `LeereMenge` als leeres kartesisches Produkt gespeichert haben, werden hier
     * einmalig semantisch auf `{()}` normalisiert.
     */
    val definitionsRaum: MengenAusdruck
        get() = when {
            argumente.isEmpty() && effektiverDefinitionsRaum == LeereMenge -> kanonischerArgumentRaum
            else -> effektiverDefinitionsRaum ?: kanonischerArgumentRaum
        }

    val zielRaum: Tupelraum
        get() = Tupelraum(
            ergebnisse.sortedBy(MathematischeErgebnisKomponente::position)
                .map(MathematischeErgebnisKomponente::zielMenge),
        )

    private fun prüfePositionen(komponenten: List<Pair<String, Int>>, rolle: String) {
        require(komponenten.map { it.first }.distinct().size == komponenten.size) {
            "$rolle-Komponenten benötigen eindeutige stabile IDs."
        }
        require(komponenten.map { it.second }.sorted() == komponenten.indices.toList()) {
            "$rolle-Komponenten benötigen lückenlose Positionen 0..n-1."
        }
    }
}

/**
 * Historische mathematische Argumentprojektion. Sie existiert nur, damit älterer
 * Mathematikcode während der Migration nicht wieder Mengen in [MethodenSignatur]
 * erzwingt.
 */
@Deprecated("Verwende MathematischeArgumentKomponente bzw. MethodenKomponente.")
data class MethodenArgument(
    val parameter: MethodenParameter,
    val werteVorrat: MengenAusdruck,
)

/**
 * Zentraler Legacy-Adapter der alten mengenbasierten Methodensignatur.
 *
 * Dieser Typ ist ausdrücklich NICHT der allgemeine Methodenvertrag. Neue Aufrufer
 * verwenden [MethodenSignatur] beziehungsweise [MathematischeMethodenSignatur].
 */
@Deprecated("Verwende neutraleMethodenSignatur() oder mathematischeMethodenSignatur().")
data class LegacyMathematischeMethodenSignatur(
    val argumente: List<MethodenArgument>,
    val zielMenge: MengenAusdruck,
    val effektiverWerteVorrat: MengenAusdruck? = null,
) {
    val werteVorrat: MengenAusdruck
        get() = effektiverWerteVorrat ?: when (argumente.size) {
            0 -> Tupelraum(emptyList())
            1 -> argumente.single().werteVorrat
            else -> Tupelraum(argumente.map(MethodenArgument::werteVorrat))
        }
}

/** Fachliche, ausschließlich abgeleitete Nutzerbegriffe unterhalb von Methode. */
enum class MethodenAlias(val anzeigeName: String) {
    Funktion("Funktion"),
    Abbildung("Abbildung"),
    Prädikat("Prädikat"),
}

/** Anzahl der geordneten Argumentkomponenten, ausdrücklich kein Dimensionsbegriff. */
val Methode.argumentAnzahl: Int
    get() = (this as? SignaturtragendeMethode)?.signatur?.argumente?.size
        ?: error("Die Methode '$name' stellt keine semantische Signatur bereit.")

/** Neutrale Signaturgrenze für neuen generischen Methoden-/Graphcode. */
fun Methode.neutraleMethodenSignatur(): MethodenSignatur =
    (this as? SignaturtragendeMethode)?.signatur
        ?: error("Die Methode '$name' stellt keine semantische Signatur bereit.")

/**
 * Quellkompatible mathematische Legacy-Projektion.
 *
 * Die alte Funktionsbezeichnung bleibt vorübergehend erhalten, weil zahlreiche
 * ausschließlich mathematische Prüfer sie verwenden. Generischer Code darf sie nicht
 * benutzen; der Architekturwächter behandelt diese Funktion als Migrationsgrenze.
 */
@Deprecated("Mathematische Legacy-Projektion; verwende neutraleMethodenSignatur() oder mathematischeMethodenSignatur().")
fun Methode.methodenSignatur(): LegacyMathematischeMethodenSignatur {
    val mathematisch = mathematischeMethodenSignatur()
    val legacyZiel = when (mathematisch.ergebnisse.size) {
        0 -> Tupelraum(emptyList())
        1 -> mathematisch.ergebnisse.single().zielMenge
        else -> mathematisch.zielRaum
    }
    val effektiverLegacyBereich = mathematisch.effektiverDefinitionsRaum?.let { bereich ->
        if (mathematisch.argumente.isEmpty() && bereich == LeereMenge) Tupelraum(emptyList()) else bereich
    }
    return LegacyMathematischeMethodenSignatur(
        argumente = mathematisch.argumente.map { argument ->
            MethodenArgument(argument.parameter, argument.definitionsMenge)
        },
        zielMenge = legacyZiel,
        effektiverWerteVorrat = effektiverLegacyBereich,
    )
}

/** Explizite Mathematikgrenze für Mengen-/Raumsemantik. */
fun Methode.mathematischeMethodenSignatur(): MathematischeMethodenSignatur =
    (this as? MathematischeSignaturtragendeMethode)?.mathematischeSignatur
        ?: error("Die Methode '$name' stellt keine mathematische Raum-/Mengensignatur bereit.")

/** Wendet ausschließlich mathematisch auswertbare Methoden vollständig an. */
fun Methode.wendeKanonischAn(argumente: Map<String, MathematischesObjekt>): MathematischesObjekt {
    val auswertbar = this as? MathematischAuswertbareMethode
        ?: error("Die Methode '$name' besitzt keine mathematische Auswertungs-Capability.")
    return auswertbar.wendeMathematischAn(argumente)
}

/** Prädikate werden nicht durch Entscheidbarkeit, sondern nur durch mathematische Semantik erkannt. */
fun Methode.istPrädikat(): Boolean {
    val mathematisch = this as? MathematischeMethode ?: return false
    return mathematisch.vorschrift is Aussage && mathematisch.zielMengeFür(mathematisch.ausgabeNamen.single()) == WahrheitsMenge
}

/** Funktionen verarbeiten und liefern ausschließlich Zahl-, Vektor-, Matrix- oder Tensorräume. */
fun Methode.istFunktion(): Boolean {
    val signatur = (this as? MathematischeSignaturtragendeMethode)?.mathematischeSignatur ?: return false
    return signatur.argumente.all { it.definitionsMenge.istFunktionalerRaum() } &&
        signatur.ergebnisse.all { it.zielMenge.istFunktionalerRaum() }
}

/** Abbildungen nehmen Mengenobjekte entgegen und liefern ein Mengenobjekt. */
fun Methode.istAbbildung(): Boolean {
    val mathematisch = this as? MathematischeMethode ?: return false
    return mathematisch.parameter.isNotEmpty() &&
        mathematisch.parameter.all { it.istMengenArgument() } &&
        mathematisch.vorschrift is MengenAusdruck
}

fun Methode.aliase(): Set<MethodenAlias> = buildSet {
    if (istFunktion()) add(MethodenAlias.Funktion)
    if (istAbbildung()) add(MethodenAlias.Abbildung)
    if (istPrädikat()) add(MethodenAlias.Prädikat)
}

fun Methode.aliasAnzeige(): String = buildList {
    add("Methode")
    addAll(aliase().map(MethodenAlias::anzeigeName))
}.joinToString(" · ")

private fun MethodenParameter.istMengenArgument(): Boolean = when (this) {
    is MengenParameter -> true
    is TypisiertesElement -> anschlussArt == "mathematik.menge" || anschlussArt.startsWith("mathematik.menge.")
    else -> false
}

private fun MengenAusdruck.istFunktionalerRaum(): Boolean = when (this) {
    is Vektorraum -> skalarMenge.istFunktionalerRaum()
    is Matrizenraum -> skalarMenge.istFunktionalerRaum()
    is Tensorraum -> elementMenge.istFunktionalerRaum()
    is Tupelraum -> komponenten.all { it.istFunktionalerRaum() }
    else -> istZahlenmenge()
}

/** Erkennt ausschließlich strukturell belegte Teilmengen der Hamilton-Quaternionen. */
fun MengenAusdruck.istZahlenmenge(): Boolean = when (this) {
    LeereMenge -> true
    is BeschraenkteZahlmenge -> FundamentaleZahlbereiche.istTeilbereich(
        traeger,
        FundamentalerZahlbereich.QUATERNION,
    )
    is ReellesIntervall -> true
    is EndlicheMenge -> elemente.all { it is ZahlAusdruck }
    is DefinierteMenge ->
        variablen.size == 1 && variablen.single().grundMenge.istZahlenmenge()
    is GefilterteMenge -> menge.istZahlenmenge()
    is Vereinigung -> mengen.isNotEmpty() && mengen.all { it.istZahlenmenge() }
    is Schnitt -> mengen.any { it.istZahlenmenge() }
    is MengenDifferenz -> links.istZahlenmenge()
    else -> fundamentalerZahlbereichOderNull()?.let { bereich ->
        FundamentaleZahlbereiche.istTeilbereich(
            bereich,
            FundamentalerZahlbereich.QUATERNION,
        )
    } == true
}

/** Semantische Eingangsprüfung statt immer weiterer Methoden-Anschlussunterarten. */
fun interface MethodenAnforderung {
    fun prüfe(methode: Methode): String?

    data object Beliebig : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? = null
    }

    data class Stelligkeit(val anzahl: Int) : MethodenAnforderung {
        init { require(anzahl >= 0) }
        override fun prüfe(methode: Methode): String? {
            val signatur = (methode as? SignaturtragendeMethode)?.signatur
                ?: return "Die Methode '${methode.name}' besitzt keine semantische Signatur."
            return if (signatur.argumente.size == anzahl) null
            else "Die Methode '${methode.name}' muss genau $anzahl Argumente besitzen."
        }
    }

    /** Allgemeine typbasierte Ergebnisanforderung ohne mathematische Mengen. */
    data class ErgebnisTyp(val typ: TypAusdruck) : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? {
            val signatur = (methode as? SignaturtragendeMethode)?.signatur
                ?: return "Die Methode '${methode.name}' besitzt keine semantische Signatur."
            val quellTyp = if (signatur.ergebnisse.size == 1) {
                signatur.ergebnisse.single().typ
            } else {
                signatur.ergebnisTupelTyp
            }
            return when (val ergebnis = MathematischeTypen.typSystem.prüfe(quellTyp, typ)) {
                TypPrüfung.Kompatibel -> null
                is TypPrüfung.Unbestimmt ->
                    "Der Ergebnistyp der Methode '${methode.name}' ist noch unbestimmt: ${ergebnis.grund}"
                is TypPrüfung.Inkompatibel ->
                    "Der Ergebnistyp der Methode '${methode.name}' ist nicht kompatibel: ${ergebnis.grund}"
            }
        }
    }

    /** Legacy-Adapter für bestehende Knotenparameter. */
    data class ErgebnisArt(val anschlussArt: String) : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? {
            val semantisch = ErgebnisTyp(TypAusdruck.Atom(TypId(anschlussArt))).prüfe(methode)
            if (semantisch == null) return null

            val mathematisch = methode as? MathematischeMethode
                ?: return semantisch
            val passt = when (anschlussArt) {
                "mathematik.objekt" -> true
                "mathematik.zahl" -> mathematisch.vorschrift is ZahlAusdruck
                "mathematik.aussage" -> mathematisch.istPrädikat()
                "mathematik.menge" -> mathematisch.vorschrift is MengenAusdruck
                "mathematik.vektor.spalte" -> mathematisch.vorschrift is SpaltenVektor
                "mathematik.vektor.zeile" -> mathematisch.vorschrift is ZeilenVektor
                "mathematik.matrix" -> mathematisch.vorschrift is Matrix
                "mathematik.tensor" -> mathematisch.vorschrift is Tensor
                else -> (mathematisch.vorschrift as? TypisiertesElement)?.anschlussArt == anschlussArt
            }
            return if (passt) null else
                "Die Methode '${methode.name}' liefert kein Ergebnis der Anschlussart '$anschlussArt'."
        }
    }

    data object Prädikat : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? =
            if (methode.istPrädikat()) null
            else "Die Methode '${methode.name}' erfüllt das Prädikatskriterium nicht."
    }

    /** Endlichstellige mathematische Methode mit ausschließlich numerischen Mengenräumen. */
    data object Zahlenfunktion : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? {
            val signatur = (methode as? MathematischeSignaturtragendeMethode)?.mathematischeSignatur
                ?: return "Die Methode '${methode.name}' besitzt keine mathematische Raum-/Mengensignatur."
            val nichtNumerischesArgument = signatur.argumente.withIndex().firstOrNull { (_, argument) ->
                !argument.definitionsMenge.istZahlenmenge()
            }
            return when {
                nichtNumerischesArgument != null -> {
                    val (index, argument) = nichtNumerischesArgument
                    "Das ${index + 1}. Argument '${argument.name}' der Methode '${methode.name}' besitzt mit " +
                        "${argument.definitionsMenge.zuLatex()} keinen nachgewiesenen Zahlenraum innerhalb von \\mathbb H."
                }
                signatur.ergebnisse.any { !it.zielMenge.istZahlenmenge() } ->
                    "Mindestens eine Zielmenge der Methode '${methode.name}' ist keine nachgewiesene Teilmenge von \\mathbb H."
                else -> null
            }
        }
    }

    data object Endomorphismus : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? {
            val signatur = (methode as? MathematischeSignaturtragendeMethode)?.mathematischeSignatur
                ?: return "Die Methode '${methode.name}' besitzt keine mathematische Raum-/Mengensignatur."
            return if (
                signatur.argumente.size == 1 && signatur.ergebnisse.size == 1 &&
                signatur.argumente.single().definitionsMenge == signatur.ergebnisse.single().zielMenge
            ) null else "Die Methode '${methode.name}' ist kein Endomorphismus."
        }
    }

    data class Alle(val anforderungen: List<MethodenAnforderung>) : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? =
            anforderungen.firstNotNullOfOrNull { it.prüfe(methode) }
    }
}
