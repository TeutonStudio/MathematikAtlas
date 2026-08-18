package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypId
import de.TeutonStudio.TypSystem.TypPrüfung

/** Die eine wiederverwendbare Zielmenge aller mathematischen Prädikate. */
val WahrheitsMenge: MengenAusdruck = EndlicheMenge(
    setOf(WahrheitsKonstante(true), WahrheitsKonstante(false)),
)

/**
 * Domänenneutrale Komponente einer Methodensignatur.
 *
 * [id] ist die stabile Identität. [name] ist nur die sichtbare Benennung und darf
 * geändert werden, ohne die Komponente semantisch auszutauschen.
 */
data class MethodenKomponente(
    val id: String,
    val name: String,
    val position: Int,
    val typ: TypAusdruck,
) {
    init {
        require(id.isNotBlank()) { "Eine Methodenkomponente benötigt eine stabile ID." }
        require(name.isNotBlank()) { "Eine Methodenkomponente benötigt einen sichtbaren Namen." }
        require(position >= 0) { "Die Position einer Methodenkomponente darf nicht negativ sein." }
    }
}

/**
 * Domänenneutrale Methodensignatur. Argumente und Ergebnisse sind symmetrisch und
 * typseitig immer Tupel, einschließlich Tupel<> und Tupel<A>.
 */
data class MethodenSignatur(
    val argumente: List<MethodenKomponente>,
    val ergebnisse: List<MethodenKomponente>,
) {
    init {
        prüfeKomponenten("Argument", argumente)
        prüfeKomponenten("Ergebnis", ergebnisse)
    }

    val argumentTyp: TypAusdruck.Parameterisiert
        get() = tupelTyp(argumente.map(MethodenKomponente::typ))

    val ergebnisTyp: TypAusdruck.Parameterisiert
        get() = tupelTyp(ergebnisse.map(MethodenKomponente::typ))

    val typAusdruck: TypAusdruck.Parameterisiert
        get() = TypAusdruck.Parameterisiert(
            MathematischeTypen.Methode,
            listOf(argumentTyp, ergebnisTyp),
        )

    private fun prüfeKomponenten(rolle: String, komponenten: List<MethodenKomponente>) {
        require(komponenten.map { it.id }.distinct().size == komponenten.size) {
            "$rolle-Komponenten benötigen eindeutige stabile IDs."
        }
        require(komponenten.map { it.position } == komponenten.indices.toList()) {
            "$rolle-Komponenten müssen explizit und lückenlos in Signaturreihenfolge positioniert sein."
        }
    }
}

private fun tupelTyp(komponenten: List<TypAusdruck>): TypAusdruck.Parameterisiert =
    TypAusdruck.Parameterisiert(MathematischeTypen.Tupel, komponenten)

/** Mathematische Metadaten einer Argumentkomponente. */
data class MathematischeArgumentKomponente(
    val id: String,
    val name: String,
    val position: Int,
    val parameter: MethodenParameter,
    val definitionsMenge: MengenAusdruck,
) {
    init {
        require(id.isNotBlank())
        require(name.isNotBlank())
        require(position >= 0)
    }

    val typ: TypAusdruck
        get() = definitionsMenge.elementTypAusdruck()
}

/** Mathematische Metadaten einer Ergebniskomponente. */
data class MathematischeErgebnisKomponente(
    val id: String,
    val name: String,
    val position: Int,
    val zielMenge: MengenAusdruck,
) {
    init {
        require(id.isNotBlank())
        require(name.isNotBlank())
        require(position >= 0)
    }

    val typ: TypAusdruck
        get() = zielMenge.elementTypAusdruck()
}

/**
 * Zusätzliche mathematische Raum-/Mengensignatur einer Methode.
 *
 * Komponenten-Definitionsmengen bleiben von einem optionalen tatsächlichen
 * gemeinsamen Definitionsraum getrennt. Der kanonische Argument- und Zielraum ist
 * immer ein [Tupelraum], auch bei null oder einer Komponente.
 */
data class MathematischeMethodenSignatur(
    val argumente: List<MathematischeArgumentKomponente>,
    val ergebnisse: List<MathematischeErgebnisKomponente>,
    val effektiverDefinitionsRaum: MengenAusdruck? = null,
) {
    init {
        require(argumente.map { it.id }.distinct().size == argumente.size) {
            "Mathematische Argumentkomponenten benötigen eindeutige IDs."
        }
        require(ergebnisse.map { it.id }.distinct().size == ergebnisse.size) {
            "Mathematische Ergebniskomponenten benötigen eindeutige IDs."
        }
        require(argumente.map { it.position } == argumente.indices.toList()) {
            "Mathematische Argumentkomponenten müssen lückenlos positioniert sein."
        }
        require(ergebnisse.map { it.position } == ergebnisse.indices.toList()) {
            "Mathematische Ergebniskomponenten müssen lückenlos positioniert sein."
        }
    }

    val kanonischerArgumentRaum: Tupelraum
        get() = Tupelraum(argumente.map(MathematischeArgumentKomponente::definitionsMenge))

    val definitionsRaum: MengenAusdruck
        get() = effektiverDefinitionsRaum ?: kanonischerArgumentRaum

    val zielRaum: Tupelraum
        get() = Tupelraum(ergebnisse.map(MathematischeErgebnisKomponente::zielMenge))

    val typSignatur: MethodenSignatur
        get() = MethodenSignatur(
            argumente = argumente.map { argument ->
                MethodenKomponente(argument.id, argument.name, argument.position, argument.typ)
            },
            ergebnisse = ergebnisse.map { ergebnis ->
                MethodenKomponente(ergebnis.id, ergebnis.name, ergebnis.position, ergebnis.typ)
            },
        )

    fun zielMengeFür(idOderName: String): MengenAusdruck {
        val komponent = ergebnisse.singleOrNull { it.id == idOderName }
            ?: ergebnisse.singleOrNull { it.name == idOderName }
            ?: error("Die mathematische Signatur besitzt keine Ergebniskomponente '$idOderName'.")
        return komponent.zielMenge
    }
}

/** Fachliche, ausschließlich abgeleitete Nutzerbegriffe unterhalb von Methode. */
enum class MethodenAlias(val anzeigeName: String) {
    Funktion("Funktion"),
    Abbildung("Abbildung"),
    Prädikat("Prädikat"),
}

/** Anzahl der geordneten Argumentplätze. Sie ist ausdrücklich kein Dimensionsbegriff. */
val Methode.argumentAnzahl: Int
    get() = (this as? SignaturtragendeMethode)?.signatur?.argumente?.size
        ?: error("Die Methode '$name' stellt keine neutrale Signatur bereit.")

/** Allgemeine Signaturgrenze ohne mathematische Mengen. */
fun Methode.methodenSignatur(): MethodenSignatur =
    (this as? SignaturtragendeMethode)?.signatur
        ?: error("Die Methode '$name' stellt keine neutrale Methodensignatur bereit.")

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

/** Prädikate werden nur durch ihre mathematische Semantik erkannt. */
fun Methode.istPrädikat(): Boolean {
    val mathematisch = this as? MathematischeMethode ?: return false
    if (mathematisch.ausgabeNamen.size != 1 || mathematisch.vorschriftFür(mathematisch.ausgabeNamen.single()) !is Aussage) {
        return false
    }
    return mathematisch.mathematischeSignatur.ergebnisse.singleOrNull()?.zielMenge == WahrheitsMenge
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
        mathematisch.vorschriftFür(mathematisch.ausgabeNamen.singleOrNull() ?: return false) is MengenAusdruck
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

/** Semantische Methodenanforderungen, getrennt in neutrale und mathematische Prüfungen. */
fun interface MethodenAnforderung {
    fun prüfe(methode: Methode): String?

    data object Beliebig : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? = null
    }

    data class Stelligkeit(val anzahl: Int) : MethodenAnforderung {
        init { require(anzahl >= 0) }
        override fun prüfe(methode: Methode): String? {
            val signatur = (methode as? SignaturtragendeMethode)?.signatur
                ?: return "Die Methode '${methode.name}' besitzt keine neutrale Signatur."
            return if (signatur.argumente.size == anzahl) null
            else "Die Methode '${methode.name}' muss genau $anzahl Argumente besitzen."
        }
    }

    /** Prüft den neutralen Typ der kanonischen Ergebnistupelsignatur. */
    data class ErgebnisTyp(val typ: TypAusdruck) : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? {
            val signatur = (methode as? SignaturtragendeMethode)?.signatur
                ?: return "Die Methode '${methode.name}' besitzt keine neutrale Signatur."
            val quellTyp = if (signatur.ergebnisse.size == 1 && typ !is TypAusdruck.Parameterisiert) {
                signatur.ergebnisse.single().typ
            } else {
                signatur.ergebnisTyp
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

    /** Prüft eine vollständige neutrale Methode<Tupel<...>, Tupel<...>>-Signatur. */
    data class SignaturKompatibilität(val typ: TypAusdruck) : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? = when (
            val ergebnis = MathematischeTypen.typSystem.prüfe(methode.methodenSignatur().typAusdruck, typ)
        ) {
            TypPrüfung.Kompatibel -> null
            is TypPrüfung.Unbestimmt -> ergebnis.grund
            is TypPrüfung.Inkompatibel -> ergebnis.grund
        }
    }

    /** Legacy-Adapter für bestehende Knotenparameter. */
    data class ErgebnisArt(val anschlussArt: String) : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? {
            val semantisch = ErgebnisTyp(TypAusdruck.Atom(TypId(anschlussArt))).prüfe(methode)
            if (semantisch == null) return null

            val mathematisch = methode as? MathematischeMethode ?: return semantisch
            val ausgabe = mathematisch.ausgabeNamen.singleOrNull()?.let(mathematisch::vorschriftFür)
                ?: return semantisch
            val passt = when (anschlussArt) {
                "mathematik.objekt" -> true
                "mathematik.zahl" -> ausgabe is ZahlAusdruck
                "mathematik.aussage" -> mathematisch.istPrädikat()
                "mathematik.menge" -> ausgabe is MengenAusdruck
                "mathematik.vektor.spalte" -> ausgabe is SpaltenVektor
                "mathematik.vektor.zeile" -> ausgabe is ZeilenVektor
                "mathematik.matrix" -> ausgabe is Matrix
                "mathematik.tensor" -> ausgabe is Tensor
                else -> (ausgabe as? TypisiertesElement)?.anschlussArt == anschlussArt
            }
            return if (passt) null else
                "Die Methode '${methode.name}' liefert kein Ergebnis der Anschlussart '$anschlussArt'."
        }
    }

    data object Prädikat : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? =
            if (methode.istPrädikat()) null
            else "Die Methode '${methode.name}' erfüllt das mathematische Prädikatskriterium nicht."
    }

    /** Endlichstellige mathematische Methode mit ausschließlich numerischen Komponentenräumen. */
    data object Zahlenfunktion : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? = runCatching {
            val signatur = methode.mathematischeMethodenSignatur()
            val nichtNumerischesArgument = signatur.argumente.firstOrNull {
                !it.definitionsMenge.istZahlenmenge()
            }
            when {
                nichtNumerischesArgument != null ->
                    "Das Argument '${nichtNumerischesArgument.name}' der Methode '${methode.name}' besitzt mit " +
                        "${nichtNumerischesArgument.definitionsMenge.zuLatex()} keinen nachgewiesenen Zahlenraum innerhalb von \\mathbb H."
                signatur.ergebnisse.any { !it.zielMenge.istZahlenmenge() } ->
                    "Mindestens eine Zielmenge der Methode '${methode.name}' ist keine nachgewiesene Teilmenge von \\mathbb H."
                else -> null
            }
        }.getOrElse { fehler -> fehler.message ?: "Die mathematische Methodensignatur ist unvollständig." }
    }

    data object Endomorphismus : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? = runCatching {
            val signatur = methode.mathematischeMethodenSignatur()
            if (
                signatur.argumente.size == 1 &&
                signatur.ergebnisse.size == 1 &&
                signatur.argumente.single().definitionsMenge == signatur.ergebnisse.single().zielMenge
            ) null else "Die Methode '${methode.name}' ist kein Endomorphismus."
        }.getOrElse { it.message ?: "Die mathematische Methodensignatur ist unvollständig." }
    }

    data class Alle(val anforderungen: List<MethodenAnforderung>) : MethodenAnforderung {
        override fun prüfe(methode: Methode): String? =
            anforderungen.firstNotNullOfOrNull { it.prüfe(methode) }
    }
}
