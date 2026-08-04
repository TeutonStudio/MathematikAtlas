package de.TeutonStudio.MathematikKnoten.enzyklopädie

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage

@JvmInline
value class WissensId(val wert: String) {
    init { require(wert.isNotBlank()) { "Eine Wissens-ID darf nicht leer sein." } }
    override fun toString(): String = wert
}

@JvmInline
value class VariantenId(val wert: String) {
    init { require(wert.isNotBlank()) { "Eine Varianten-ID darf nicht leer sein." } }
    override fun toString(): String = wert
}

data class FachPfad(val segmente: List<String>) {
    init {
        require(segmente.isNotEmpty()) { "Ein Fachpfad benötigt mindestens ein Segment." }
        require(segmente.all(String::isNotBlank)) { "Fachpfadsegmente dürfen nicht leer sein." }
        require(segmente.size <= 3) { "Fachpfade dürfen höchstens drei Ebenen besitzen." }
    }

    val stabileId: String = segmente.joinToString("/")

    companion object {
        fun von(vararg segmente: String): FachPfad = FachPfad(segmente.toList())
    }
}

enum class WissensVerfügbarkeit { Verfügbar, Geplant, Historisch }
enum class WissensReifegrad { Entwurf, Geprüft, Zertifiziert }
enum class WissensBeziehungsArt {
    Voraussetzung,
    Verwendet,
    SpezialfallVon,
    Verallgemeinert,
    ÄquivalentZu,
    Erzeugt,
    VerwandtMit,
}

enum class WissensKartenRolle { Definition, Spezialfall, Beispiel, Äquivalenz }

data class WissensBeziehung(
    val ziel: WissensId,
    val art: WissensBeziehungsArt,
    val beschreibung: String? = null,
)

data class WissensQuelle(
    val titel: String,
    val referenz: String,
    val hinweis: String? = null,
) {
    init {
        require(titel.isNotBlank())
        require(referenz.isNotBlank())
    }
}

sealed interface WissensKartenReferenz {
    val id: String
    val rolle: WissensKartenRolle
    val primär: Boolean

    data class Asset(
        override val id: String,
        val datei: String,
        val formatVersion: Int,
        override val rolle: WissensKartenRolle,
        override val primär: Boolean = false,
    ) : WissensKartenReferenz {
        init {
            require(id.isNotBlank())
            require(datei.endsWith(".json")) { "Konzeptkarten-Assets müssen JSON-Dateien sein." }
            require(formatVersion > 0)
        }
    }

    data class Generator(
        override val id: String,
        val generatorId: String,
        override val rolle: WissensKartenRolle,
        override val primär: Boolean = false,
    ) : WissensKartenReferenz {
        init {
            require(id.isNotBlank())
            require(generatorId.isNotBlank())
        }
    }
}

data class WissensEintrag(
    val id: WissensId,
    val titel: String,
    val kurzbeschreibung: String,
    val fachPfade: Set<FachPfad>,
    val suchbegriffe: Set<String> = emptySet(),
    val aliase: Set<String> = emptySet(),
    val verfügbarkeit: WissensVerfügbarkeit = WissensVerfügbarkeit.Verfügbar,
    val reifegrad: WissensReifegrad = WissensReifegrad.Entwurf,
    val voraussetzungen: Set<WissensId> = emptySet(),
    val beziehungen: Set<WissensBeziehung> = emptySet(),
    val knotenArten: Set<KnotenArtId> = emptySet(),
    val varianten: Set<VariantenId> = emptySet(),
    val knotenVorlagen: List<KnotenVorlage> = emptyList(),
    val karten: List<WissensKartenReferenz> = emptyList(),
    val quellen: List<WissensQuelle> = emptyList(),
    val konventionen: Set<String> = emptySet(),
) {
    init {
        require(titel.isNotBlank()) { "$id benötigt einen Titel." }
        require(kurzbeschreibung.isNotBlank()) { "$id benötigt eine Kurzbeschreibung." }
        require(fachPfade.isNotEmpty()) { "$id benötigt mindestens einen Fachpfad." }
        require(suchbegriffe.none(String::isBlank)) { "$id besitzt einen leeren Suchbegriff." }
        require(aliase.none(String::isBlank)) { "$id besitzt einen leeren Alias." }
        require(knotenVorlagen.all { it.art in knotenArten }) {
            "$id enthält eine Knotenvorlage, deren Knotenart nicht im Wissenseintrag registriert ist."
        }
    }

    val alleSuchtexte: Set<String>
        get() = buildSet {
            add(titel)
            add(kurzbeschreibung)
            add(id.wert)
            addAll(suchbegriffe)
            addAll(aliase)
            fachPfade.forEach { add(it.stabileId) }
            knotenVorlagen.forEach { vorlage ->
                add(vorlage.name)
                add(vorlage.beschreibung)
                add(vorlage.kategorie)
                add(vorlage.art)
            }
        }

    val primäreDefinition: WissensKartenReferenz?
        get() = karten.singleOrNull { it.rolle == WissensKartenRolle.Definition && it.primär }
}
