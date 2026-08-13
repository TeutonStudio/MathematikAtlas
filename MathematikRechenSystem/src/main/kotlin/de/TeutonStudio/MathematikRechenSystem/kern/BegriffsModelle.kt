package de.TeutonStudio.MathematikRechenSystem.kern

/** Erwarteter mathematischer Vertrag einer stabil benannten Begriffsrolle. */
enum class BegriffsRollenArt { MENGE, METHODE, AUSSAGE, OBJEKT }

data class BegriffsRolle(
    val id: String,
    val name: String,
    val art: BegriffsRollenArt,
) {
    init { require(id.isNotBlank() && name.isNotBlank()) }
}

data class BegriffsAxiom(
    val id: String,
    val name: String,
    val beschreibungLatex: String,
) {
    init { require(id.isNotBlank() && name.isNotBlank() && beschreibungLatex.isNotBlank()) }
}

data class BegriffsBeispiel(
    val id: String,
    val name: String,
    val zertifikatReferenzen: Set<String>,
) {
    init { require(id.isNotBlank() && name.isNotBlank()) }
}

data class BegriffsSpezifikation(
    val id: String,
    val name: String,
    val rollen: List<BegriffsRolle>,
    val axiome: List<BegriffsAxiom>,
    val beispiele: List<BegriffsBeispiel> = emptyList(),
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(rollen.map { it.id }.distinct().size == rollen.size)
        require(axiome.map { it.id }.distinct().size == axiome.size)
        require(beispiele.map { it.id }.distinct().size == beispiele.size)
    }
}

/**
 * Rollenbelegung eines Begriffs. Methodenrollen sind Atlaswerte, nicht automatisch
 * mathematische Objekte; die Spezifikation bestimmt über [BegriffsRollenArt], welche
 * Capability für eine konkrete Rolle verlangt wird.
 */
data class BegriffsKandidat(
    val spezifikationId: String,
    val belegung: Map<String, AtlasWert>,
    val quellenIdentitaeten: Map<String, String> = emptyMap(),
    val zertifikatReferenzen: Set<String> = emptySet(),
) {
    init { require(spezifikationId.isNotBlank()) }
}

data class NachweisZeugnis(
    val axiomId: String,
    val status: NachweisStatus,
    val referenzen: Set<String> = emptySet(),
    val gegenbeispiel: Map<String, MathematischesObjekt> = emptyMap(),
)

private data class BegriffsAxiomAussage(
    val axiom: BegriffsAxiomPruefung,
) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis = when (axiom.status) {
        NachweisStatus.Nachgewiesen -> AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen, axiom.begruendung)
        NachweisStatus.Widerlegt -> AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt, axiom.begruendung)
        is NachweisStatus.Bedingt -> AussageErgebnis(null, EntscheidungsStatus.Unbekannt, axiom.begruendung)
        NachweisStatus.Unvollstaendig -> AussageErgebnis(null, EntscheidungsStatus.NichtAuswertbar, axiom.begruendung)
        NachweisStatus.Unentscheidbar -> AussageErgebnis(null, EntscheidungsStatus.Unentscheidbar, axiom.begruendung)
    }

    override fun zuLatex(): String = "\\operatorname{${axiom.name.replace(" ", "\\ ")}}"
}


/**
 * Maschinenlesbare Einzelprüfung eines Axioms. Ein Gegenbeispiel wird nur
 * gespeichert, wenn die Widerlegung durch konkrete Werte entstanden ist.
 */
data class BegriffsAxiomPruefung(
    val id: String,
    val name: String,
    val status: NachweisStatus,
    val begruendung: String,
    val gegenbeispiel: Map<String, MathematischesObjekt> = emptyMap(),
) {
    init {
        require(id.isNotBlank())
        require(name.isNotBlank())
    }
}

sealed interface BegriffsZeugnis {
    val begriffsId: String
    val skalarKoerper: MengenAusdruck?
}

data class VektorraumZeugnis(
    val traegerMenge: MengenAusdruck,
    val addition: Methode,
    val skalareMultiplikation: Methode,
    override val skalarKoerper: MengenAusdruck,
) : BegriffsZeugnis {
    override val begriffsId: String = VEKTORRAUM_BEGRIFF_ID
}

data class LineareAbbildungsZeugnis(
    val definitionsRaum: VektorraumZeugnis,
    val zielRaum: VektorraumZeugnis,
    val abbildung: Methode,
) : BegriffsZeugnis {
    override val begriffsId: String = LINEARE_ABBILDUNG_BEGRIFF_ID
    override val skalarKoerper: MengenAusdruck = definitionsRaum.skalarKoerper
}

data class BegriffsPruefung(
    val begriffsId: String,
    val begriffsName: String,
    val axiomPruefungen: List<BegriffsAxiomPruefung>,
    val status: NachweisStatus,
    val diagnosen: List<String> = emptyList(),
    val zeugnis: BegriffsZeugnis? = null,
    val spezifikation: BegriffsSpezifikation? = null,
    val kandidat: BegriffsKandidat? = null,
    val axiomAussagen: List<Aussage> = axiomPruefungen.map(::BegriffsAxiomAussage),
    val gesamtAussage: Aussage = Konjunktion(axiomAussagen),
    val nachweisZeugnisse: List<NachweisZeugnis> = axiomPruefungen.map { pruefung ->
        NachweisZeugnis(
            axiomId = pruefung.id,
            status = pruefung.status,
            gegenbeispiel = pruefung.gegenbeispiel,
        )
    },
) {
    init {
        require(begriffsId.isNotBlank())
        require(begriffsName.isNotBlank())
        require(axiomPruefungen.map { it.id }.distinct().size == axiomPruefungen.size)
    }
}

/**
 * Eine Begriffsauswertung bleibt eine gewöhnliche Aussage und kann deshalb mit
 * den bestehenden Aussagen-, Auswertungs- und Kartenpfaden verbunden werden.
 */
data class BegriffsAussage(
    val pruefung: BegriffsPruefung,
    val formelLatex: String,
) : Aussage {
    override fun entscheide(kontext: RechenKontext): AussageErgebnis = when (pruefung.status) {
        NachweisStatus.Nachgewiesen -> AussageErgebnis(
            Wahrheitswert.Wahr,
            EntscheidungsStatus.Bewiesen,
            pruefung.diagnosen.joinToString(" "),
        )
        NachweisStatus.Widerlegt -> AussageErgebnis(
            Wahrheitswert.Lüge,
            EntscheidungsStatus.Widerlegt,
            pruefung.diagnosen.joinToString(" "),
        )
        is NachweisStatus.Bedingt -> AussageErgebnis(
            null,
            EntscheidungsStatus.Unbekannt,
            pruefung.diagnosen.joinToString(" "),
        )
        NachweisStatus.Unvollstaendig -> AussageErgebnis(
            null,
            EntscheidungsStatus.NichtAuswertbar,
            pruefung.diagnosen.joinToString(" "),
        )
        NachweisStatus.Unentscheidbar -> AussageErgebnis(
            null,
            EntscheidungsStatus.Unentscheidbar,
            pruefung.diagnosen.joinToString(" "),
        )
    }

    override fun zuLatex(): String = formelLatex
}

const val VEKTORRAUM_BEGRIFF_ID = "lina.vektorraum"
const val LINEARE_ABBILDUNG_BEGRIFF_ID = "lina.lineareAbbildung"

val VEKTORRAUM_SPEZIFIKATION = BegriffsSpezifikation(
    id = VEKTORRAUM_BEGRIFF_ID,
    name = "Vektorraum",
    rollen = listOf(
        BegriffsRolle("menge", "Trägermenge", BegriffsRollenArt.MENGE),
        BegriffsRolle("addition", "Addition", BegriffsRollenArt.METHODE),
        BegriffsRolle("skalareMultiplikation", "Skalare Multiplikation", BegriffsRollenArt.METHODE),
    ),
    axiome = listOf(
        BegriffsAxiom("abschluss", "Abgeschlossenheit", "u+v\\in V"),
        BegriffsAxiom("assoziativ", "Assoziativität", "(u+v)+w=u+(v+w)"),
        BegriffsAxiom("nullvektor", "Nullvektor", "u+0_V=u"),
        BegriffsAxiom("inverses", "Additives Inverses", "u+(-u)=0_V"),
        BegriffsAxiom("kommutativ", "Kommutativität", "u+v=v+u"),
        BegriffsAxiom("distributivVektor", "Distributivität über Vektoraddition", "a(u+v)=au+av"),
        BegriffsAxiom("distributivSkalar", "Distributivität über Skalaraddition", "(a+b)u=au+bu"),
        BegriffsAxiom("skalarAssoziativ", "Assoziativität der Skalarmultiplikation", "(ab)u=a(bu)"),
        BegriffsAxiom("skalarEins", "Einselement des Körpers", "1_Ku=u"),
    ),
    beispiele = listOf(
        BegriffsBeispiel("kn", "K^n", setOf("satz.vektorraum.komponentenweise")),
        BegriffsBeispiel("matrizen", "Matrizenraum", setOf("satz.vektorraum.matrizen")),
        BegriffsBeispiel("polynome", "Polynomraum", setOf("satz.vektorraum.polynome")),
        BegriffsBeispiel("methoden", "Methodenraum", setOf("satz.vektorraum.punktweise")),
        BegriffsBeispiel("nullraum", "Nullvektorraum", setOf("satz.vektorraum.nullraum")),
    ),
)

val LINEARE_ABBILDUNG_SPEZIFIKATION = BegriffsSpezifikation(
    id = LINEARE_ABBILDUNG_BEGRIFF_ID,
    name = "Lineare Abbildung",
    rollen = listOf(
        BegriffsRolle("definitionsraum", "Definitionsraum", BegriffsRollenArt.AUSSAGE),
        BegriffsRolle("zielraum", "Zielraum", BegriffsRollenArt.AUSSAGE),
        BegriffsRolle("methode", "Methode", BegriffsRollenArt.METHODE),
    ),
    axiome = listOf(
        BegriffsAxiom("additiv", "Additivität", "f(u+v)=f(u)+f(v)"),
        BegriffsAxiom("homogen", "Homogenität", "f(au)=af(u)"),
    ),
    beispiele = listOf(
        BegriffsBeispiel("identitaet", "Identität", setOf("satz.linear.identitaet")),
        BegriffsBeispiel("nullabbildung", "Nullabbildung", setOf("satz.linear.nullabbildung")),
        BegriffsBeispiel("matrixabbildung", "Matrixabbildung", setOf("satz.linear.matrix")),
        BegriffsBeispiel("quadrat", "Gegenbeispiel x²", emptySet()),
    ),
)

