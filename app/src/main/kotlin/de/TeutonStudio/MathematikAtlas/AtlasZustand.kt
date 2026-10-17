package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.MathematikRechenSystem.kern.effektiverWerteVorrat
import de.TeutonStudio.MathematikRechenSystem.kern.werteVorräte
import de.TeutonStudio.MathematikRechenSystem.kern.zielMenge

import android.content.Context
import androidx.compose.runtime.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.VerbindungsDragZielHinweis
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikAtlas.speicher.AktiveKarteSpeicher
import de.TeutonStudio.MathematikAtlas.speicher.KartenJson
import de.TeutonStudio.MathematikAtlas.speicher.KartenSpeicher
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikKnoten.abstraktion.AbstraktionsKontext
import de.TeutonStudio.MathematikKnoten.abstraktion.AbstraktionsRegister
import de.TeutonStudio.MathematikKnoten.abstraktion.AbstraktionsVorschlag
import de.TeutonStudio.MathematikKnoten.katalog.OperatorKnotenSuchindex
import de.TeutonStudio.MathematikKnoten.visualisierung.ui.VisualisierungsKnotenRenderer
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import java.io.File
import kotlinx.coroutines.*
import de.TeutonStudio.MathematikAtlas.knotenauswahl.*

@Stable
class AtlasZustand(context: Context) {
    internal val pie = PieZustand(this)
    val knotenNutzung=de.TeutonStudio.MathematikAtlas.speicher.InstallationsKnotenNutzung.für(context)
    var standardSitzung: String? by mutableStateOf(null)
        private set
    var nutzungUnterbrochen by mutableStateOf(false)
        private set
    var standardTab by mutableStateOf("Alle")
        private set
    private var suchHerkunft=SuchtextHerkunft.OHNE_FILTER
    private var standardAuftrag: KnotenAuswahlAuftrag?=null
    private var ergebnisCache: StandardKnotenErgebnis?=null
    private var ergebnisSchluessel: List<Any?>?=null
    private val rechenScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var rechenAuftrag: Job? = null
    private var rechenGeneration = 0L
    private var rechenMomentaufnahme: KartenDaten? = null

    val speicher = KartenSpeicher(context)
    private val aktiveKarteSpeicher = AktiveKarteSpeicher(File(context.filesDir, "MathematikAtlas"))
    fun anschlussInfoFür(karte: KartenDaten, ref: AnschlussVerweis) = laufzeit.anschlussInfoFür(karte, ref)

    private val laufzeit = MathematikKartenLaufzeit(
        kartenQuelle = KartenQuelle(speicher::lade),
        nichtAuswertbareKnotenArten = KartenWerkzeugVorlagen.nichtAuswertbareArten,
    )
    var ausstehendeKonfiguration by mutableStateOf<KnotenDaten?>(null)
        private set

    fun vorschauKonfiguration(kandidat: KnotenDaten): KnotenErsetzungsAuswirkung =
        planeKonfiguration(editor.karte, kandidat, graphPrüfung, laufzeit::auswerten).auswirkung

    fun beantrageKonfiguration(kandidat: KnotenDaten) {
        if (vorschauKonfiguration(kandidat).trenntVerbindungen) ausstehendeKonfiguration = kandidat
        else übernehmeKonfiguration(kandidat)
    }

    fun bestätigeKonfiguration() {
        ausstehendeKonfiguration?.let(::übernehmeKonfiguration)
        ausstehendeKonfiguration = null
    }

    fun verwerfeKonfiguration() { ausstehendeKonfiguration = null }

    internal fun übernehmeKonfiguration(kandidat: KnotenDaten) {
        val plan = planeKonfiguration(editor.karte, kandidat, graphPrüfung, laufzeit::auswerten)
        val umordnung = editor.karte.istReineAnschlussUmordnung(plan.karte)
        editor.führeAus(KartenAktion.KartenInhaltErsetzen(plan.karte), dynamischeEingängeErhalten = umordnung)
    }

    /** Kandidaten werden zuerst rein berechnet; Übernahme und Kantentrennung sind ein Undo-Schritt. */
    fun konfiguriereKnoten(aktion: KartenAktion) {
        val kandidat = editor.karte.wendeAn(aktion)
        kandidat.knoten.firstOrNull { neu -> editor.karte.knoten.any { it.id == neu.id && it != neu } }
            ?.let(::beantrageKonfiguration)
    }

    fun konfiguriereFesteEingänge(id: KnotenId, anzahl: Int) {
        val probe = KartenEditorZustand(editor.karte, graphPrüfung)
        probe.setzeFesteEingangAnzahl(id, anzahl)
        probe.karte.knoten.firstOrNull { it.id == id }?.let(::beantrageKonfiguration)
    }

    val anschlussArten = laufzeit.anschlussArten
    private val graphPrüfung = laufzeit.graphPrüfung

    var karten by mutableStateOf<List<KartenDaten>>(emptyList())
        private set
    var brotkrumen by mutableStateOf<List<KartenVerweis>>(emptyList())
        private set
    var auswertung by mutableStateOf(KartenAuswertungsErgebnis(emptyMap(), emptyList()))
        private set
    var linkerBereich by mutableStateOf(VerwaltungsBereich.Karten)
    var knotenAuswahlPosition by mutableStateOf<GraphPunkt?>(null)
    var knotenAuswahlStart by mutableStateOf<AnschlussVerweis?>(null)
    var suchText by mutableStateOf("")
        private set
    private var ausgewählteKnotenKategorie by mutableStateOf<String?>(null)

    val editor: KartenEditorZustand
    private var letzterGespeicherterStand: KartenDaten

    init {
        installiereStandardkarten(context, speicher)
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
        val start = aktiveKarteSpeicher.lade()
            ?.let { id -> karten.firstOrNull { it.id == id } }
            ?: karten.firstOrNull()
            ?: KartenDaten(name = "Neue Karte")
        if (start.id !in karten.map(KartenDaten::id).toSet()) aktiveKarteSpeicher.löschen()
        editor = KartenEditorZustand(start, graphPrüfung)
        val verbindungsPlaner = ÜbersetzerVerbindungsPlaner(graphPrüfung)
        editor.verbindungsPlaner = verbindungsPlaner::vorschläge
        editor.verbindungÜberAdapterMöglich = verbindungsPlaner::möglich
        editor.öffneGruppe(aktiveKarteSpeicher.ladeGruppe())
        letzterGespeicherterStand = start
        brotkrumen = if (karten.any { it.id == start.id }) {
            listOf(KartenVerweis(start.id, start.version))
        } else {
            emptyList()
        }
        MengenKnotenKartenQuelle.installieren(this)
        MethodenQuellenKarten.installieren(this)
        MengenOperatorInspektorRegistrierung.installieren()
        VerbindungsDragZielHinweis.installiere { dragZustand, ziel ->
            methodenSignaturFürDragZiel(dragZustand, ziel)
        }
        werteAus()
    }

    val aktuelleKarte get() = editor.karte
    val ausgewählterKnoten get() = editor.karte.knoten.firstOrNull { it.id == editor.ausgewählterKnoten }

    fun aktualisiereAuswertung() { werteAus() }

    fun analysiereAbstraktionen(): List<AbstraktionsVorschlag> = AbstraktionsRegister().analysiere(
        AbstraktionsKontext(
            karte = editor.karte,
            auswertung = auswertung,
            graphPrüfung = graphPrüfung,
            auswerten = laufzeit::auswerten,
        ),
    )

    fun zeigeAbstraktion(vorschlag: AbstraktionsVorschlag) {
        editor.stelleAuswahlWiederHer(vorschlag.vorherKnoten, vorschlag.wurzelKnoten)
    }

    /** Validiert einen möglicherweise veralteten Dialogvorschlag unmittelbar vor der atomaren Anwendung. */
    fun ersetzeDurchAbstraktion(vorschlag: AbstraktionsVorschlag): String? {
        val vorschau = editor.karte.vorschauTeilgraphErsetzen(vorschlag.ersetzungsPlan, graphPrüfung)
        if (!vorschau.istGültig) return vorschau.fehler.joinToString(" ")
        editor.führeAus(KartenAktion.TeilgraphErsetzen(vorschlag.ersetzungsPlan))
        werteAus()
        return null
    }

    /** Lädt den sichtbaren Kartenbestand aus dem Speicher, ohne die aktuell geöffnete Karte zu wechseln. */
    fun ladeKartenNeu() {
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
        if (aktuelleKarte.id !in karten.map(KartenDaten::id).toSet()) {
            karten.firstOrNull()?.let(::öffne) ?: aktiveKarteSpeicher.löschen()
        }
    }

    fun berechneKnotenCacheNeu(knotenId: KnotenId) {
        laufzeit.verwerfeCache(knotenId)
        werteAus()
    }

    fun öffne(karte: KartenDaten, alsUnterkarte: Boolean = false) {
        pie.schließen()
        if (karte.id != editor.karte.id) {
            sichereAktuellVorUnterbrechung()
        }
        val aktualisiert = aktualisiereAssoziativeKnoten(karte)
        editor.ersetzeKarte(aktualisiert)
        letzterGespeicherterStand = aktualisiert
        aktiveKarteSpeicher.speichere(aktualisiert.id)
        brotkrumen = if (alsUnterkarte) brotkrumen + KartenVerweis(aktualisiert.id, aktualisiert.version) else listOf(KartenVerweis(aktualisiert.id, aktualisiert.version))
        werteAus()
    }

    fun öffne(verweis: KartenVerweis, alsUnterkarte: Boolean = true) {
        speicher.lade(verweis)?.let { öffne(it, alsUnterkarte) }
    }

    fun geheZuBrotkrume(index: Int) {
        pie.schließen()
        val ziel = brotkrumen.getOrNull(index) ?: return
        speicher.lade(ziel)?.let {
            val aktualisiert = aktualisiereAssoziativeKnoten(it)
            brotkrumen = brotkrumen.take(index + 1)
            editor.ersetzeKarte(aktualisiert)
            letzterGespeicherterStand = aktualisiert
            werteAus()
        }
    }

    private var importLäuft = false

    fun speichereAktuell() {
        if (importLäuft || editor.interaktionAktiv) return
        aktiveKarteSpeicher.speichere(editor.karte.id, editor.geöffneteGruppe)
        if (editor.verbindungsStart != null || editor.karte == letzterGespeicherterStand) return
        val gespeichert = speicher.speichere(editor.karte)
        if (gespeichert.version != editor.karte.version) {
            editor.ersetzeKarte(gespeichert, historieLeeren = false)
            brotkrumen = brotkrumen.dropLast(1) + KartenVerweis(gespeichert.id, gespeichert.version)
        }
        letzterGespeicherterStand = gespeichert
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
    }

    /** Verwirft ausschließlich die flüchtige Verbindungsvorschau und sichert danach den Kartenstand. */
    fun sichereAktuellVorUnterbrechung() {
        pie.schließen()
        editor.brecheVerbindungsVorschauAb()
        speichereAktuell()
    }

    fun öffneBearbeitbareKopie(vorlage: KartenDaten) {
        val basisName = "${vorlage.name} – Kopie"
        val vorhandeneNamen = karten.mapTo(mutableSetOf()) { it.name }
        var name = basisName
        var nummer = 2
        while (name in vorhandeneNamen) {
            name = "$basisName $nummer"
            nummer += 1
        }
        val gespeichert = speicher.speichere(vorlage.alsNeueKarte(name))
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
        linkerBereich = VerwaltungsBereich.Karten
        öffne(gespeichert)
    }

    fun öffneKnotenAuswahl(position: GraphPunkt, start: AnschlussVerweis? = null) {
        beendeStandardSitzung(SitzungsAbschluss.ABBRUCH)
        standardTab="Alle"
        knotenAuswahlPosition = position
        knotenAuswahlStart = start
        sichereKnotenKategorieAuswahl()
    }

    fun schließeKnotenAuswahl() {
        if(standardAuftrag==null) beendeStandardSitzung(SitzungsAbschluss.ABBRUCH)
        knotenAuswahlPosition = null
        knotenAuswahlStart = null
    }

    fun fügeKnotenEin(vorlage: KnotenVorlage, position: GraphPunkt) {
        val knoten = vorlage.erzeuge(position)
        val start = knotenAuswahlStart
        editor.führeAus(KartenAktion.KnotenEinfügen(knoten))
        if (start != null) {
            editor.beginneVerbindung(start)
            val ziel = knoten.anschlüsse.asSequence().map { AnschlussVerweis(knoten.id, it.id) }.firstOrNull(editor::kompatibelMitStart)
            if (ziel != null) editor.anschlussAngeklickt(ziel)
        }
        editor.wähleKnoten(knoten.id)
        schließeKnotenAuswahl()
    }

    /** Fügt ein Tupel und dessen Umwandlung zu einem orientierten Vektor als eine zusammenhängende Knotenfolge ein. */
    fun fügeTupelVektorEin(spalte: Boolean, position: GraphPunkt) {
        val tupel = MathematikKnotenVorlagen.Tupel.erzeuge(position)
        val umwandlung = konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(position + GraphPunkt(270f, 0f)),if (spalte) "spalte" else "zeile")
        val tupelAusgang = tupel.anschlüsse.firstOrNull { it.name == "tupel" } ?: return
        val umwandlungsEingang = umwandlung.anschlüsse.firstOrNull { it.name == "wert" } ?: return
        val vorläufigeKarte = editor.karte.copy(knoten = editor.karte.knoten + listOf(tupel, umwandlung))
        val interneVerbindung = verbindungWennErlaubt(
            vorläufigeKarte,
            AnschlussVerweis(tupel.id, tupelAusgang.id),
            AnschlussVerweis(umwandlung.id, umwandlungsEingang.id),
        ) ?: return
        val karteMitInternerVerbindung = vorläufigeKarte.copy(verbindungen = vorläufigeKarte.verbindungen + interneVerbindung)
        val externeVerbindung = knotenAuswahlStart?.let { start ->
            tupel.anschlüsse.asSequence()
                .map { AnschlussVerweis(tupel.id, it.id) }
                .mapNotNull { ziel -> verbindungWennErlaubt(karteMitInternerVerbindung, start, ziel) }
                .firstOrNull()
        }
        if (knotenAuswahlStart != null && externeVerbindung == null) return
        editor.beginneInteraktion()
        editor.führeAus(KartenAktion.KnotenEinfügen(tupel), mitHistorie = false)
        editor.führeAus(KartenAktion.KnotenEinfügen(umwandlung), mitHistorie = false)
        editor.führeAus(KartenAktion.VerbindungEinfügen(interneVerbindung), mitHistorie = false)
        externeVerbindung?.let { editor.führeAus(KartenAktion.VerbindungEinfügen(it), mitHistorie = false) }
        editor.beendeInteraktion()
        editor.wähleKnoten(umwandlung.id)
        schließeKnotenAuswahl()
    }

    fun kannTupelVektorEinfügen(): Boolean = istKompatibelMitOffenerVerbindung(MathematikKnotenVorlagen.Tupel)

    fun neueKarte() {
        val karte = speicher.speichere(KartenDaten(name = "Neue Karte ${karten.size + 1}"))
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
        öffne(karte)
    }

    fun importiere(text: String) {
        val karte = speicher.importiere(text)
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
        öffne(karte)
    }

    fun importiere(container: ByteArray) {
        val karte = speicher.importiere(container)
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
        öffne(karte)
    }

    /** Validierung und Dateitransaktion laufen außerhalb des Compose-Threads. */
    suspend fun importiereImHintergrund(container: ByteArray) {
        check(!importLäuft) { "Ein Import läuft bereits." }
        sichereAktuellVorUnterbrechung()
        importLäuft = true
        val ergebnis = try {
            withContext(Dispatchers.IO) {
                val wurzel = speicher.importiere(container)
                wurzel to speicher.liste().map(::aktualisiereAssoziativeKnoten)
            }
        } finally {
            importLäuft = false
        }
        karten = ergebnis.second
        öffne(ergebnis.first)
    }

    /** Übernimmt bearbeitetes Karten-JSON. Referenzierte Versionen werden vom Speicher automatisch fortgeschrieben. */
    fun übernehmeJson(text: String): String? {
        val gelesen = runCatching { KartenJson.lese(text) }.getOrElse { fehler ->
            return "Ungültiges JSON: ${fehler.message ?: fehler::class.simpleName}"
        }
        gelesen.validierungsFehler()?.let { return it }
        val gespeichert = runCatching { speicher.speichere(gelesen) }.getOrElse { fehler ->
            return "Die Karte konnte nicht gespeichert werden: ${fehler.message ?: fehler::class.simpleName}"
        }
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
        öffne(gespeichert)
        return null
    }

    fun archiviereAktuell() {
        speicher.archiviere(editor.karte)
        karten = speicher.liste().map(::aktualisiereAssoziativeKnoten)
        karten.firstOrNull()?.let(::öffne) ?: aktiveKarteSpeicher.löschen()
    }

    fun renderer() = MathematikKnotenRenderer(
        ergebnisFür = { knoten -> auswertung.knoten[knoten.id] },
        beiKnotenKlick = { knoten -> editor.wähleKnoten(knoten.id) },
        beiKnotenDoppelklick = { knoten -> knoten.kartenVerweis?.let(::öffne) },
    )

    fun rendererFür(knoten: KnotenDaten) = (when {
        knoten.art == NOTIZ_KNOTEN_ART -> NotizKnotenRenderer
        knoten.art.startsWith("konzept.") -> KonzeptDokumentationsRenderer
        knoten.art == "mathematik.visualisierung" -> VisualisierungsKnotenRenderer { daten -> auswertung.knoten[daten.id] }
        knoten.art == GEOMETRY_NODES_PREFIX+"preview" -> GeometryNodesVorschauRenderer { daten -> auswertung.knoten[daten.id] }
        knoten.art == "mathematik.geometrie.visualisierung" -> GeometrieVisualisierungsKnotenRenderer { daten -> auswertung.knoten[daten.id] }
        else -> renderer()
    }).mitAuswertungszeit { daten -> auswertung.knoten[daten.id] }

    fun setzeSuchText(text: String) {
        if(text==suchText) return
        val sid=standardSitzung
        if(text.isEmpty() && sid!=null) knotenNutzung.protokollieren(sid,standardErgebnis().zustand,SuchAnlass.ZURUECKSETZEN)
        suchText = text
        suchHerkunft=if(text.isBlank()) SuchtextHerkunft.ZURUECKGESETZT else SuchtextHerkunft.GETIPPT
        sichereKnotenKategorieAuswahl()
        if(sid!=null) {
            val z=standardErgebnis().zustand
            knotenNutzung.aktualisieren(sid,z)
            if(text.isBlank()) knotenNutzung.protokollieren(sid,z,SuchAnlass.ZURUECKSETZEN)
        }
    }

    fun knotenKategorien(): List<String> = suchbareVorlagen().map { it.kategorie }.distinct()

    fun sichtbareVorlagen(): List<KnotenVorlage> = suchbareVorlagen().filter { vorlage ->
        val suchePasst = suchText.isBlank() ||
            vorlage.name.contains(suchText, ignoreCase = true) ||
            vorlage.kategorie.contains(suchText, ignoreCase = true) ||
            vorlage.beschreibung.contains(suchText, ignoreCase = true)
        suchePasst && istKompatibelMitOffenerVerbindung(vorlage)
    }

    fun aktiveKnotenKategorie(): String? = ausgewählteKnotenKategorie?.takeIf { kategorie -> sichtbareVorlagen().any { it.kategorie == kategorie } }

    fun wähleKnotenKategorie(kategorie: String?) {
        ausgewählteKnotenKategorie = kategorie?.takeIf { gewählt -> sichtbareVorlagen().any { it.kategorie == gewählt } }
    }

    private fun sichereKnotenKategorieAuswahl() {
        if (aktiveKnotenKategorie() == null) ausgewählteKnotenKategorie = null
    }

    private fun suchbareVorlagen(): List<KnotenVorlage> = suchbareEinträge().map { it.vorlage!! }

    private fun suchbareEinträge(): List<StandardKnotenEintrag> {
        val basis = alleKnotenVorlagen()
        val operatorTreffer = if (suchText.isBlank()) emptyList() else OperatorKnotenSuchindex.sucheMitMetadaten(suchText, basis)
        return standardVorlagenEinträge(basis,operatorTreffer)
    }

    fun standardErgebnis(): StandardKnotenErgebnis {
        val key=listOf(suchText,suchHerkunft,standardTab,knotenAuswahlStart,editor.karte,karten)
        if(ergebnisSchluessel==key) return checkNotNull(ergebnisCache)
        val start=knotenAuswahlStart
        val art=start?.let { r -> editor.karte.knoten.firstOrNull { it.id==r.knotenId }?.anschlüsse?.firstOrNull { it.id==r.anschlussId } }?.let { "${it.richtung}:${it.art.wert}" }
        val neu=standardKnotenErgebnis(suchbareEinträge(),suchText,suchHerkunft,standardTab,::istKompatibelMitOffenerVerbindung,kannTupelVektorEinfügen(),kannMengendefinitionEinfügen(),kannFaltungEinfügen(),art)
        ergebnisSchluessel=key;ergebnisCache=neu;return neu
    }
    fun beginneStandardSitzung(): String? {
        if(nutzungUnterbrochen || knotenAuswahlPosition==null) return null
        standardSitzung?.let { return it }
        suchHerkunft=if(suchText.isBlank()) SuchtextHerkunft.OHNE_FILTER else SuchtextHerkunft.UEBERNOMMEN
        val sid=nutzungsId();standardSitzung=sid
        knotenNutzung.öffnen(sid,if(knotenAuswahlStart==null) "freie-standardliste" else "anschluss-standardliste",BuildConfig.VERSION_NAME,standardErgebnis().zustand)
        return sid
    }
    fun wähleStandardTab(tab: String) {
        if(tab==standardTab) return
        standardSitzung?.let { knotenNutzung.protokollieren(it,standardErgebnis().zustand,SuchAnlass.TABWECHSEL) }
        standardTab=tab
        standardSitzung?.let { knotenNutzung.protokollieren(it,standardErgebnis().zustand,SuchAnlass.TABWECHSEL) }
    }
    fun beendeStandardSitzung(grund: SitzungsAbschluss) {
        val sid=standardSitzung ?: return
        knotenNutzung.schließen(sid,standardErgebnis().zustand,grund);standardSitzung=null
    }
    fun unterbrecheKnotenNutzung() {
        nutzungUnterbrochen=true;beendeStandardSitzung(SitzungsAbschluss.UNTERBROCHEN);knotenNutzung.sichern()
    }
    fun setzeKnotenNutzungFort() { nutzungUnterbrochen=false }

    /** Nur dieser explizite Standarddialogpfad zählt Entscheidungen; allgemeine Einfügefunktionen bleiben ungezählt. */
    fun wähleStandardEintrag(sitzung: String,eintrag: StandardKnotenEintrag,position: GraphPunkt,anzeige: StandardKnotenErgebnis) {
        if(standardSitzung!=sitzung || knotenAuswahlPosition==null || standardAuftrag!=null) return
        val rang=anzeige.zustand.angebote.indexOfFirst { it.schluessel==eintrag.schluessel }+1
        if(rang<1) return
        val auftrag=KnotenAuswahlAuftrag(nutzungsId(),sitzung,eintrag.schluessel,rang,anzeige.zustand,eintrag.vorlage?.kartenVerweis?.version)
        val vorher=editor.karte.knoten.mapTo(hashSetOf()) { it.id }
        var übernommen=false
        standardAuftrag=auftrag
        try {
            if(!eintrag.einfügbar) return
            when(eintrag.aktion) {
                StandardSammelAktion.FALTUNG -> fügeFaltungEin(position)
                StandardSammelAktion.MENGENDEFINITION -> fügeMengendefinitionEin(position)
                StandardSammelAktion.TUPEL_SPALTE -> fügeTupelVektorEin(true,position)
                StandardSammelAktion.TUPEL_ZEILE -> fügeTupelVektorEin(false,position)
                null -> fügeKnotenEin(checkNotNull(eintrag.vorlage),position)
            }
            übernommen=true
        } finally {
            val neu=editor.karte.knoten.filter { it.id !in vorher }.map { ErzeugterKnotenTyp(it.art,stabileKnotenVariante(it)) }
            val erfolg=übernommen && neu.isNotEmpty()
            knotenNutzung.ergebnis(auftrag,erfolg,neu)
            standardAuftrag=null
            if(erfolg || knotenAuswahlPosition==null) {
                knotenNutzung.schließen(sitzung,auftrag.zustand,if(erfolg) SitzungsAbschluss.AUSWAHL else SitzungsAbschluss.ABBRUCH)
                standardSitzung=null
            }
        }
    }

    private fun alleKnotenVorlagen(): List<KnotenVorlage> =
        statischeKnotenVorlagen() + gruppenVorlagen()

    private fun gruppenVorlagen(): List<KnotenVorlage> = karten.asSequence()
        .filter { it.id != editor.karte.id && !it.archiviert && !referenziertKarte(it, editor.karte.id, mutableSetOf()) }
        .map { kartenVorlage(it) }
        .toList()

    private fun referenziertKarte(karte: KartenDaten, gesuchteId: KartenId, besucht: MutableSet<KartenVerweis>): Boolean {
        val refs = karte.knoten.mapNotNull { it.kartenVerweis }
        if (refs.any { it.kartenId == gesuchteId }) return true
        return refs.any { ref -> if (!besucht.add(ref)) false else speicher.lade(ref)?.let { referenziertKarte(it, gesuchteId, besucht) } == true }
    }

    internal fun piePrüfEditor(basis: KartenDaten): KartenEditorZustand = KartenEditorZustand(basis, graphPrüfung).also {
        val planer = ÜbersetzerVerbindungsPlaner(graphPrüfung)
        it.verbindungsPlaner = planer::vorschläge
        it.verbindungÜberAdapterMöglich = planer::möglich
    }

    private fun istKompatibelMitOffenerVerbindung(vorlage: KnotenVorlage): Boolean {
        val start = knotenAuswahlStart ?: return true
        val probe = vorlage.erzeuge(GraphPunkt.Zero)
        val probeKarte = editor.karte.copy(knoten = editor.karte.knoten + probe)
        return probe.anschlüsse.any { anschluss -> graphPrüfung.prüfe(probeKarte, start, AnschlussVerweis(probe.id, anschluss.id)) is VerbindungsPrüfung.Erlaubt }
    }

    private fun verbindungWennErlaubt(karte: KartenDaten, erster: AnschlussVerweis, zweiter: AnschlussVerweis): VerbindungDaten? {
        if (graphPrüfung.prüfe(karte, erster, zweiter) !is VerbindungsPrüfung.Erlaubt) return null
        val (von, zu) = graphPrüfung.normalisiere(karte, erster, zweiter) ?: return null
        return VerbindungDaten(von = von, zu = zu)
    }

    private fun methodenSignaturFürDragZiel(
        dragZustand: KartenEditorZustand,
        ziel: AnschlussVerweis,
    ): String? {
        val start = dragZustand.verbindungsStart ?: return null
        if (!dragZustand.kompatibelMitStart(ziel)) return null
        val (von, _) = graphPrüfung.normalisiere(dragZustand.karte, start, ziel) ?: return null
        val quellKnoten = dragZustand.karte.knoten.firstOrNull { it.id == von.knotenId } ?: return null
        val quellAnschluss = quellKnoten.anschlüsse.firstOrNull { it.id == von.anschlussId } ?: return null
        val methode = auswertung.knoten[quellKnoten.id]
            ?.ausgaben
            ?.get(quellAnschluss.name)
            ?.objekt as? Methode
            ?: return null
        val effektiverWerteVorrat = methode.effektiverWerteVorrat
        val werteVorrat = when {
            methode.werteVorräte.size == 1 -> methode.werteVorräte.values.single().zuLatex()
            methode.werteVorräte.size > 1 -> methode.werteVorräte.values.joinToString(" × ") { it.zuLatex() }
            effektiverWerteVorrat != null -> effektiverWerteVorrat.zuLatex()
            else -> "W"
        }
        return "$werteVorrat → ${methode.zielMenge.zuLatex()}"
    }

    private fun KartenDaten.validierungsFehler(): String? {
        if (id.wert.isBlank()) return "Die Karten-ID darf nicht leer sein."
        if (name.isBlank()) return "Der Kartenname darf nicht leer sein."
        if (version < 1) return "Die Kartenversion muss mindestens 1 sein."
        if (!ansicht.zoom.isFinite() || ansicht.zoom <= 0f) return "Der Ansichtszoom muss eine positive Zahl sein."
        if (!ansicht.verschiebung.x.isFinite() || !ansicht.verschiebung.y.isFinite()) return "Die Ansichtsposition enthält keine gültigen Zahlen."

        val knotenIds = knoten.map { it.id }
        if (knotenIds.size != knotenIds.toSet().size) return "Knoten-IDs müssen innerhalb einer Karte eindeutig sein."
        if (knoten.any { it.id.wert.isBlank() || it.art.isBlank() || it.name.isBlank() }) {
            return "Jeder Knoten benötigt eine ID, eine Art und einen Namen."
        }
        if (knoten.any { !it.position.x.isFinite() || !it.position.y.isFinite() || !it.größe.breite.isFinite() || !it.größe.höhe.isFinite() || it.größe.breite <= 0f || it.größe.höhe <= 0f }) {
            return "Knotenpositionen und -größen müssen gültige positive Maße enthalten."
        }
        if (knoten.any { k -> k.anschlüsse.map { it.id }.let { ids -> ids.size != ids.toSet().size } }) {
            return "Anschluss-IDs müssen innerhalb eines Knotens eindeutig sein."
        }

        val verbindungsIds = verbindungen.map { it.id }
        if (verbindungsIds.size != verbindungsIds.toSet().size) return "Verbindungs-IDs müssen eindeutig sein."
        val anschlussVerweise = knoten.flatMap { k -> k.anschlüsse.map { AnschlussVerweis(k.id, it.id) } }.toSet()
        if (verbindungen.any { it.von !in anschlussVerweise || it.zu !in anschlussVerweise }) {
            return "Mindestens eine Verbindung verweist auf einen nicht vorhandenen Anschluss."
        }
        return null
    }

    private fun aktualisiereAssoziativeKnoten(karte: KartenDaten): KartenDaten =
        migriereAssoziativeKnoten(migriereAussagenOperatoren(karte)).migriereKonsolidierteKnoten()

    fun beendeBerechnungen() { unterbrecheKnotenNutzung();rechenScope.cancel() }

    private fun synchronisierteKarte(karte: KartenDaten, ergebnis: KartenAuswertungsErgebnis): KartenDaten {
        val restriktion = synchronisiereRestriktionsAnschlüsse(karte, ergebnis)
        val bild = synchronisiereBildmengenAnschlüsse(restriktion, ergebnis)
        val zerlegt = synchronisiereStrukturZerleger(bild, ergebnis, graphPrüfung)
        return synchronisiereMethodenAufrufe(zerlegt, ergebnis, graphPrüfung)
    }

    private fun werteAus() {
        val moment = editor.karte
        if (moment == rechenMomentaufnahme && rechenAuftrag?.isActive == true) return
        val generation = ++rechenGeneration
        rechenAuftrag?.cancel()
        rechenMomentaufnahme = moment
        if (moment.knoten.any { besitztPrädikatsRechenweg(it.art) || it.kartenVerweis != null } && moment.knoten.none { it.art.startsWith("konzept.") }) {
            auswertung = KartenAuswertungsErgebnis(emptyMap(), emptyList())
            rechenAuftrag = rechenScope.launch {
                val berechnet = withContext(Dispatchers.Default) {
                    MathematikKartenLaufzeit(kartenQuelle = KartenQuelle(speicher::lade),
                        nichtAuswertbareKnotenArten = KartenWerkzeugVorlagen.nichtAuswertbareArten).auswerten(moment)
                }
                if (generation != rechenGeneration || editor.karte != moment) return@launch
                val synchronisiert = synchronisierteKarte(moment, berechnet)
                if (synchronisiert == moment) auswertung = berechnet else {
                    val auswahl = editor.ausgewählteKnoten
                    val aktiv = editor.ausgewählterKnoten
                    editor.ersetzeKarte(synchronisiert, historieLeeren = false)
                    editor.stelleAuswahlWiederHer(auswahl, aktiv)
                    werteAus()
                }
            }
            return
        }
        if (editor.karte.knoten.any { it.art.startsWith("konzept.") }) {
            auswertung = KartenAuswertungsErgebnis(emptyMap(), emptyList())
            return
        }

        val ersteAuswertung = laufzeit.auswerten(editor.karte)
        val mitRestriktionsAnschlüssen = synchronisiereRestriktionsAnschlüsse(editor.karte, ersteAuswertung)
        val mitBildmengenAnschlüssen = synchronisiereBildmengenAnschlüsse(mitRestriktionsAnschlüssen, ersteAuswertung)
        val mitAuflösern = synchronisiereStrukturZerleger(mitBildmengenAnschlüssen, ersteAuswertung, graphPrüfung)
        val synchronisiert = synchronisiereMethodenAufrufe(mitAuflösern, ersteAuswertung, graphPrüfung)
        if (synchronisiert == editor.karte) {
            auswertung = ersteAuswertung
            return
        }

        val auswahl = editor.ausgewählteKnoten
        val aktiverKnoten = editor.ausgewählterKnoten
        editor.ersetzeKarte(synchronisiert, historieLeeren = false)
        editor.stelleAuswahlWiederHer(auswahl, aktiverKnoten)
        laufzeit.leereCache()
        auswertung = laufzeit.auswerten(editor.karte)
    }
}
