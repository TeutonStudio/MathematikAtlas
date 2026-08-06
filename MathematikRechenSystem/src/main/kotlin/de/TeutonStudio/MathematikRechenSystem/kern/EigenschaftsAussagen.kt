package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Technischer Unterstützungsstatus einer mathematischen Prüfung.
 *
 * Dieser Status ist ausdrücklich unabhängig vom Wahrheitswert. Eine noch nicht
 * implementierte Prüfung darf deshalb niemals als falsche Aussage erscheinen.
 */
enum class UnterstuetzungsStatus {
    IMPLEMENTIERT,
    MATHEMATISCH_NICHT_MOEGLICH,
    NOCH_NICHT_IMPLEMENTIERT,
}

/** Fachlicher Status einer Eigenschaftsaussage. */
enum class AussageStatus {
    BEWIESEN,
    WIDERLEGT,
    BEDINGT,
    UNENTSCHEIDBAR,
}

data class EigenschaftsDiagnose(
    val code: String,
    val nachricht: String,
    val voraussetzungen: List<String> = emptyList(),
) {
    init {
        require(code.isNotBlank())
        require(nachricht.isNotBlank())
        require(voraussetzungen.none(String::isBlank))
    }
}

/**
 * Strukturierte Aussage über eine Eigenschaft eines mathematischen Subjekts.
 *
 * [unterstuetzung] beschreibt die Fähigkeit der Anwendung. [aussageStatus]
 * beschreibt ausschließlich den mathematischen Erkenntnisstand.
 */
data class EigenschaftsAussage(
    val eigenschaftId: String,
    val eigenschaftLatex: String,
    val subjektLatex: String,
    val unterstuetzung: UnterstuetzungsStatus,
    val aussageStatus: AussageStatus,
    val diagnose: EigenschaftsDiagnose? = null,
    val kontextLatex: String? = null,
) : Aussage {
    init {
        require(eigenschaftId.isNotBlank())
        require(eigenschaftLatex.isNotBlank())
        require(subjektLatex.isNotBlank())
        require(unterstuetzung == UnterstuetzungsStatus.IMPLEMENTIERT || aussageStatus == AussageStatus.UNENTSCHEIDBAR) {
            "Nicht unterstützte Prüfungen dürfen keinen scheinbaren Wahrheitswert tragen."
        }
    }

    override fun entscheide(kontext: RechenKontext): AussageErgebnis = when (unterstuetzung) {
        UnterstuetzungsStatus.MATHEMATISCH_NICHT_MOEGLICH -> AussageErgebnis(
            wahrheitswert = null,
            status = EntscheidungsStatus.NichtAuswertbar,
            begründung = diagnose?.nachricht ?: "Die Eigenschaft ist für dieses Subjekt mathematisch nicht definiert.",
        )
        UnterstuetzungsStatus.NOCH_NICHT_IMPLEMENTIERT -> AussageErgebnis(
            wahrheitswert = null,
            status = EntscheidungsStatus.NichtAuswertbar,
            begründung = diagnose?.nachricht ?: "Die Prüfung ist noch nicht implementiert.",
        )
        UnterstuetzungsStatus.IMPLEMENTIERT -> when (aussageStatus) {
            AussageStatus.BEWIESEN -> AussageErgebnis(
                wahrheitswert = Wahrheitswert.Wahr,
                status = EntscheidungsStatus.Bewiesen,
                begründung = diagnose?.nachricht.orEmpty(),
            )
            AussageStatus.WIDERLEGT -> AussageErgebnis(
                wahrheitswert = Wahrheitswert.Lüge,
                status = EntscheidungsStatus.Widerlegt,
                begründung = diagnose?.nachricht.orEmpty(),
            )
            AussageStatus.BEDINGT -> AussageErgebnis(
                wahrheitswert = null,
                status = EntscheidungsStatus.Unbekannt,
                begründung = diagnose?.nachricht ?: "Die Aussage gilt nur unter zusätzlichen Voraussetzungen.",
            )
            AussageStatus.UNENTSCHEIDBAR -> AussageErgebnis(
                wahrheitswert = null,
                status = EntscheidungsStatus.Unentscheidbar,
                begründung = diagnose?.nachricht ?: "Die Eigenschaft ist im aktuellen Kontext nicht entscheidbar.",
            )
        }
    }

    override fun zuLatex(): String = buildString {
        append("\\operatorname{")
        append(eigenschaftLatex.replace(" ", "\\ "))
        append("}\\left(")
        append(subjektLatex)
        append("\\right)")
        kontextLatex?.takeIf(String::isNotBlank)?.let {
            append("_{\\left[").append(it).append("\\right]}")
        }
    }
}
