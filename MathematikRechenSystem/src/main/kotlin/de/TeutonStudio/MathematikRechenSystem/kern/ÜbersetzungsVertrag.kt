package de.TeutonStudio.MathematikRechenSystem.kern

/** Einzige Liste der bestehenden expliziten Übersetzerziele. */
enum class ÜbersetzungsZiel(val id: String) {
    TUPEL("tupel"), ZEILE("zeile"), SPALTE("spalte"), METHODE("methode"), INDEXMETHODE("indexmethode"),
    MATRIXZERLEGEN("matrixzerlegen"), POLAR("polar"), KARTESISCH("kartesisch"), GRAD("grad"), BOGENMASS("bogenmass"),
    KOORDINATENMENGE("koordinatenmenge"), MITTELPUNKT("mittelpunkt"), OBJEKTPUNKT("objektpunkt"), GEOMETRIE("geometrie"),
    ABFLACHEN("abflachen"), RÜCKFÜHRUNG("rueckfuehrung");
    val regelVersion: Int get() = 1
    companion object {
        fun vonId(id: String) = entries.singleOrNull { it.id == id } ?: error("Unbekanntes Übersetzungsziel '$id'.")
        val knotenZiele: List<String> get() = entries.map { it.id }
    }
}

enum class ÜbersetzungsGenauigkeit { EXAKT, NUMERISCH_ANGENÄHERT, OFFEN }
enum class StrukturArt { BLATT, TUPEL, ZEILE, SPALTE, MATRIX, TENSOR }

data class ÜbersetzungsStruktur(
    val art: StrukturArt,
    val kinder: List<ÜbersetzungsStruktur> = emptyList(),
    val dimensionen: List<Int> = emptyList(),
    val blattTyp: String = "objekt",
) {
    init {
        require(dimensionen.all { it >= 0 })
        when(art) {
            StrukturArt.BLATT -> require(kinder.isEmpty() && dimensionen.isEmpty())
            StrukturArt.TUPEL -> require(dimensionen.isEmpty())
            StrukturArt.ZEILE, StrukturArt.SPALTE -> require(dimensionen.isEmpty() && kinder.isNotEmpty() && kinder.all { it.art == StrukturArt.BLATT && it.blattTyp == "zahl" })
            StrukturArt.MATRIX -> require(dimensionen.size == 2 && dimensionen.all { it > 0 } && dimensionen.fold(1L) { a,b -> a*b } == kinder.size.toLong())
            StrukturArt.TENSOR -> require(dimensionen.isNotEmpty() && dimensionen.all { it > 0 } && dimensionen.fold(1L) { a,b -> Math.multiplyExact(a,b.toLong()) } == kinder.size.toLong())
        }
    }
    companion object {
        fun von(wert: MathematischesObjekt): ÜbersetzungsStruktur = when (wert) {
            is Tupel -> ÜbersetzungsStruktur(StrukturArt.TUPEL, wert.elemente.map(::von))
            is ZeilenVektor -> ÜbersetzungsStruktur(StrukturArt.ZEILE, wert.werte.map(::von))
            is SpaltenVektor -> ÜbersetzungsStruktur(StrukturArt.SPALTE, wert.werte.map(::von))
            is Matrix -> ÜbersetzungsStruktur(StrukturArt.MATRIX, wert.zeilen.flatten().map(::von), listOf(wert.zeilenAnzahl, wert.spaltenAnzahl))
            is Tensor -> ÜbersetzungsStruktur(StrukturArt.TENSOR, wert.werte.map(::von), wert.dimensionen)
            else -> ÜbersetzungsStruktur(StrukturArt.BLATT, blattTyp = when (wert) {
                is ZahlAusdruck -> "zahl"; is WinkelAusdruck -> "winkel"; is MengenAusdruck -> "menge"
                is Aussage -> "aussage"; is Methode -> "methode"; is GeometrischerAusdruck -> "geometrie"; else -> "objekt"
            })
        }
    }
}

/** Ein Ausgangselement entspricht genau einem Quellpfad (mathematisch 1-basiert). */
data class StrukturRückführungsplan(
    val quelle: ÜbersetzungsStruktur,
    val quellPfade: List<List<Int>>,
    val version: Int = 1,
) {
    init {
        require(quellPfade.distinct().size == quellPfade.size && quellPfade.all { p -> p.all { it > 0 } })
    }
    fun rückführen(werte: List<MathematischesObjekt>): MathematischesObjekt {
        require(version == 1) { "Unbekannte Rückführungsplanversion." }
        require(werte.size == quellPfade.size) { "Rückführungsplan und Komponentenanzahl widersprechen sich." }
        val zuordnung = quellPfade.zip(werte).toMap()
        val benutzt = linkedSetOf<List<Int>>()
        fun baue(form: ÜbersetzungsStruktur, pfad: List<Int>, tiefe: Int): MathematischesObjekt {
            require(tiefe <= 128)
            zuordnung[pfad]?.let { wert ->
                require(ÜbersetzungsStruktur.von(wert) == form) { "Die Komponente an $pfad hat eine andere Struktur." }
                benutzt += pfad
                return wert
            }
            val kinder = form.kinder.mapIndexed { i, kind -> baue(kind, pfad + (i + 1), tiefe + 1) }
            return when (form.art) {
                StrukturArt.TUPEL -> Tupel(kinder)
                StrukturArt.ZEILE -> ZeilenVektor(kinder.map { it as? ZahlAusdruck ?: error("Zeile benötigt Zahlkomponenten.") })
                StrukturArt.SPALTE -> SpaltenVektor(kinder.map { it as? ZahlAusdruck ?: error("Spalte benötigt Zahlkomponenten.") })
                else -> error("Für die Struktur an $pfad fehlt eine vollständige Komponentenbelegung.")
            }
        }
        return baue(quelle, emptyList(), 0).also {
            require(benutzt == zuordnung.keys) { "Der Rückführungsplan enthält ungültige oder überlappende Pfade." }
        }
    }
    companion object {
        fun fürStruktur(wert: MathematischesObjekt) = StrukturRückführungsplan(ÜbersetzungsStruktur.von(wert),
            when (wert) {
                is Tupel -> wert.elemente.indices.map { listOf(it + 1) }
                is OrientierterVektor -> wert.werte.indices.map { listOf(it + 1) }
                else -> error("Kein Tupel oder Vektor.")
            })
        fun fürAbflachen(wert: Tupel, eineEbene: Boolean): StrukturRückführungsplan {
            val pfade = mutableListOf<List<Int>>()
            fun besuche(w: MathematischesObjekt, pfad: List<Int>) {
                if (w is Tupel) w.elemente.forEachIndexed { i, kind -> besuche(kind, pfad + (i + 1)) }
                else pfade += pfad
            }
            wert.elemente.forEachIndexed { i, w ->
                val pfad = listOf(i + 1)
                if (!eineEbene) besuche(w, pfad)
                else when (w) {
                    is Tupel -> w.elemente.indices.forEach { pfade += pfad + (it + 1) }
                    is OrientierterVektor -> w.werte.indices.forEach { pfade += pfad + (it + 1) }
                    else -> pfade += pfad
                }
            }
            return StrukturRückführungsplan(ÜbersetzungsStruktur.von(wert), pfade)
        }
    }
}

data class ÜbersetzungsQuelle(
    val id: String, val name: String, val bereich: MengenAusdruck?,
    val rolle: String = "wert", val komponentenPfad: List<Int> = emptyList(),
    val symbolId: String? = null,
)

data class ÜbersetzungsKontext(
    val definitionsRaum: MengenAusdruck? = null,
    val werteBereich: MengenAusdruck? = null,
    val komponentenRäume: List<MengenAusdruck>? = null,
    val annahmen: Set<Aussage> = emptySet(),
    val quellen: List<ÜbersetzungsQuelle> = emptyList(),
    val argumentReihenfolge: List<String> = emptyList(),
    val datenGenauigkeit: ÜbersetzungsGenauigkeit = ÜbersetzungsGenauigkeit.OFFEN,
    val bezugsRaum: EuklidischerRaum? = null,
    val anker: Tupel? = null,
    val verschiebung: List<ZahlAusdruck>? = null,
)

data class ÜbersetzungsBehauptungen(val injektiv: NachweisStatus, val surjektivAufTräger: NachweisStatus, val informationserhaltung: NachweisStatus, val genauigkeit: ÜbersetzungsGenauigkeit)

data class ÜbersetzungsZeugnis(
    val regelId: String,
    val regelVersion: Int = 1,
    val quelle: ÜbersetzungsStruktur,
    val ziel: ÜbersetzungsStruktur,
    val quellKontext: ÜbersetzungsKontext,
    val zielTräger: MengenAusdruck?,
    val bildRaum: MengenAusdruck?,
    val parameter: Map<String, String> = emptyMap(),
    val identitätsZuordnung: Map<String, String> = emptyMap(),
    val voraussetzungen: Set<Aussage> = emptySet(),
    val injektiv: NachweisStatus = NachweisStatus.Unvollstaendig,
    val surjektivAufTräger: NachweisStatus = NachweisStatus.Unvollstaendig,
    val informationserhaltung: NachweisStatus = NachweisStatus.Unvollstaendig,
    val rückführungsplan: StrukturRückführungsplan? = null,
    val genauigkeit: ÜbersetzungsGenauigkeit = ÜbersetzungsGenauigkeit.OFFEN,
    val verloreneInformationen: List<String> = emptyList(),
    val diagnose: List<String> = emptyList(),
    val ungeprüfteBehauptungen: ÜbersetzungsBehauptungen? = null,
) {
    fun fürAutomatikGeeignet(): Boolean = regelVersion == 1 && injektiv == NachweisStatus.Nachgewiesen &&
        informationserhaltung == NachweisStatus.Nachgewiesen && genauigkeit == ÜbersetzungsGenauigkeit.EXAKT &&
        verloreneInformationen.isEmpty() && voraussetzungen.all { it.entscheide().wahrheitswert == Wahrheitswert.Wahr }

    val effektiveGenauigkeit: ÜbersetzungsGenauigkeit get() = if (quellKontext.datenGenauigkeit == ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT)
        ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT else genauigkeit
    fun hinweise(): List<String> = buildList {
        add(when {
            informationserhaltung == NachweisStatus.Widerlegt -> "Informationsverlust"
            injektiv == NachweisStatus.Nachgewiesen && surjektivAufTräger == NachweisStatus.Nachgewiesen -> "Bijektive Übersetzung"
            injektiv == NachweisStatus.Nachgewiesen -> "Injektive Einbettung; Zielträger nicht als Bild nachgewiesen"
            informationserhaltung == NachweisStatus.Nachgewiesen -> "Mit Kontext rückführbar; Abbildungseigenschaften offen"
            else -> "Übersetzungseigenschaften offen"
        })
        if (effektiveGenauigkeit == ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT) add("Numerische Näherung; keine zertifizierte Fehlergrenze")
        if (genauigkeit == ÜbersetzungsGenauigkeit.OFFEN) add("Rechengenauigkeit offen")
        addAll(verloreneInformationen)
        addAll(diagnose)
        voraussetzungen.filter { it.entscheide().wahrheitswert != Wahrheitswert.Wahr }.forEach { add("Voraussetzung: ${it.zuLatex()}") }
    }
}

data class ÜbersetzungsMetadaten(val aktuell: ÜbersetzungsZeugnis? = null, val herkunft: List<ÜbersetzungsZeugnis> = emptyList(), val kontext: ÜbersetzungsKontext? = null) {
    fun alsHerkunft() = ÜbersetzungsMetadaten(herkunft = (herkunft + listOfNotNull(aktuell)).distinct())
    fun mit(zeugnis: ÜbersetzungsZeugnis, kontext: ÜbersetzungsKontext? = null) = ÜbersetzungsMetadaten(zeugnis, (herkunft + listOfNotNull(aktuell)).distinct(), kontext)
}

data class ÜbersetzungsAnfrage(
    val wert: MathematischesObjekt, val ziel: ÜbersetzungsZiel,
    val kontext: ÜbersetzungsKontext = ÜbersetzungsKontext(),
    val parameter: Map<String, String> = emptyMap(),
    val automatisch: Boolean = false,
)

sealed interface ÜbersetzungsErgebnis {
    data class Erfolg(val wert: MathematischesObjekt, val kontext: ÜbersetzungsKontext, val zeugnis: ÜbersetzungsZeugnis) : ÜbersetzungsErgebnis
    data class NichtDefiniert(val code: String, val beschreibung: String, val voraussetzungen: Set<Aussage> = emptySet()) : ÜbersetzungsErgebnis
}

/** Symbolisches exaktes Bild; der Zielträger allein enthält nicht alle ursprünglichen Einschränkungen. */
data class ÜbersetzungsBildraum(
    val quellRaum: MengenAusdruck,
    val regelId: String,
    val quellStruktur: ÜbersetzungsStruktur,
    val zielStruktur: ÜbersetzungsStruktur,
    val parameter: Map<String, String> = emptyMap(),
    val rückführungsplan: StrukturRückführungsplan? = null,
    val regelVersion: Int = 1,
) : MengenAusdruck {
    override fun zuLatex(): String = "\\operatorname{Bild}_{\\mathrm{Ü}}\\left(${quellRaum.zuLatex()}\\right)"
    fun enthält(wert: MathematischesObjekt, kontext: RechenKontext): AussageErgebnis {
        if (regelVersion != 1 || regelId !in setOf("uebersetzung.tupel", "uebersetzung.zeile", "uebersetzung.spalte", "uebersetzung.abflachen")) return AussageErgebnis(null, EntscheidungsStatus.Unbekannt, "Unbekannte Bildraum-Regelversion.")
        if (ÜbersetzungsStruktur.von(wert) != zielStruktur) return AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt, "Die Zielstruktur stimmt nicht überein.")
        val plan = rückführungsplan ?: return AussageErgebnis(null, EntscheidungsStatus.Unbekannt, "Die Bildzugehörigkeit ist symbolisch offen.")
        if (plan.quelle != quellStruktur || plan.version != 1) return AussageErgebnis(null, EntscheidungsStatus.Unbekannt, "Bildraum und Rückführungsplan sind nicht bestätigt.")
        val geprüft = runCatching { ÜbersetzungsZeugnis(regelId,regelVersion,quellStruktur,zielStruktur,
            ÜbersetzungsKontext(),null,null,parameter=parameter,rückführungsplan=plan).prüfeÜbertragung() }.getOrNull()
        if(geprüft?.informationserhaltung != NachweisStatus.Nachgewiesen) return AussageErgebnis(null, EntscheidungsStatus.Unbekannt, "Der Bildraumplan passt nicht zur Regel.")
        val komponenten = when (wert) { is Tupel -> wert.elemente; is OrientierterVektor -> wert.werte; else -> return AussageErgebnis(null, EntscheidungsStatus.Unbekannt) }
        val quelle = runCatching { plan.rückführen(komponenten) }.getOrElse { return AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt, it.message.orEmpty()) }
        return ElementBeziehung(quelle, quellRaum).entscheide(kontext)
    }
}

/** Selbsttragender Übertragungswert, ohne den mathematischen Laufzeitwert durch einen Wrapper zu ersetzen. */
data class ÜbertragenerMathematischerWert(val wert: MathematischesObjekt, val übersetzungen: ÜbersetzungsMetadaten = ÜbersetzungsMetadaten(), val formel: GebundeneFormel? = null)

fun FormelAusdruck.übersetzungsMetadaten(): ÜbersetzungsMetadaten = when (this) {
    is FormelAusdruck.Literal -> if (übersetzungen == ÜbersetzungsMetadaten()) (wert as? MathematischeMethode)?.übersetzungen ?: übersetzungen else übersetzungen
    is FormelAusdruck.Operation -> {
        val eingänge = argumente.map { it.ausdruck.übersetzungsMetadaten() }
        val herkunft = eingänge.flatMap { it.alsHerkunft().herkunft }.distinct()
        val angenähert = eingänge.any { it.kontext?.datenGenauigkeit == ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT ||
            it.aktuell?.effektiveGenauigkeit == ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT } ||
            herkunft.any { it.effektiveGenauigkeit == ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT }
        ÜbersetzungsMetadaten(herkunft = herkunft, kontext = if (angenähert)
            ÜbersetzungsKontext(datenGenauigkeit = ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT) else null)
    }
    else -> ÜbersetzungsMetadaten()
}
