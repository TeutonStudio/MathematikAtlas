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
 * Rollenbelegung ist ein neutraler Wertkanal. Ob eine Rolle eine mathematische Menge,
 * Aussage oder Methode verlangt, wird durch [BegriffsRolle.art] und die jeweilige
 * Prüfung validiert, nicht durch eine pauschale `MathematischesObjekt`-Schranke.
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
 * gespeichert, wenn die Widerlegung durch konkrete mathematische Werte entstanden ist.
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
