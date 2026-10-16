package de.TeutonStudio.MathematikAtlas

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext

internal enum class PieEbene { Geschlossen, Kategorien, Fächer, Definition, Ziehen, Anschluss, Weg }

/** Flüchtige Sitzung. Die einzige persistierbare Wahrheit bleibt die Editorkarte. */
@Stable
internal class PieZustand(private val atlas: AtlasZustand) {
    var ebene by mutableStateOf(PieEbene.Geschlossen); private set
    var anker by mutableStateOf(Offset.Zero); private set
    var versatz by mutableStateOf(Offset.Zero); private set
    private var ringVersatz = Offset.Zero
    var bedienHöhe by mutableFloatStateOf(0f); private set
    fun meldeBedienHöhe(höhe: Float) { bedienHöhe = höhe }
    var zoom by mutableFloatStateOf(1f); private set
    var kategorie by mutableStateOf<KonzeptKategorie?>(null); private set
    var einträge by mutableStateOf<List<PieEintrag>>(emptyList()); private set
    var definition by mutableStateOf<KnotenDaten?>(null); private set
    var größen by mutableStateOf<Map<String, GraphGröße>>(emptyMap()); private set
    private var konzepte = emptyList<KonzeptBibliothekEintrag>()
    private var kartenEinträge = emptyList<PieEintrag>()
    private var einfügeInteraktion: Long? = null
    private var griff = GraphPunkt.Zero
    private var vorherigeAuswahl = emptySet<KnotenId>()
    private var vorherGruppe: VisuelleGruppenId? = null
    private var vorherVerbindung: VerbindungsId? = null
    private var vorherAktiv: KnotenId? = null
    var verbindungsKontext by mutableStateOf<VerbindungsEinfügeKontext?>(null); private set
    var lädt by mutableStateOf(false); private set
    var filterFehler by mutableStateOf<String?>(null); private set
    var anschlussTreffer by mutableStateOf<List<VerbindungsEinfügeTreffer>>(emptyList()); private set
    var gewählterAnschluss by mutableStateOf<VerbindungsEinfügeTreffer?>(null); private set
    private var dialogBasis: KartenDaten? = null
    private var eingefügterKnoten: KnotenId? = null
    val anschlussbezogen get() = verbindungsKontext != null

    fun verbindungÖffnen(kontext: VerbindungsEinfügeKontext, punkt: Offset) {
        öffnen(punkt)
        verbindungsKontext = kontext
        einträge = emptyList(); ebene = PieEbene.Fächer; lädt = true
    }

    suspend fun ladeVerbindungsEinträge() {
        val kontext = verbindungsKontext ?: return
        val basisVorlagen = statischeKnotenVorlagen()
        val karten = kartenEinträge
        val prüfEditor = atlas.piePrüfEditor(kontext.basis)
        try {
            val ergebnis = withContext(Dispatchers.Default) {
                val kandidaten = KanonischerMathematikKnotenKatalog.anschlussVarianten(basisVorlagen).map {
                    PieEintrag(KanonischerMathematikKnotenKatalog.variantenKennung(it), it.kategorie, it)
                } + karten
                kandidaten.distinctBy { KanonischerMathematikKnotenKatalog.variantenKennung(it.vorlage) }.mapNotNull { e ->
                    coroutineContext.ensureActive()
                    val probe = e.vorlage.erzeuge(GraphPunkt.Zero)
                    val treffer = prüfEditor.verbindungsEinfügeTreffer(kontext,
                        kontext.basis.copy(knoten = kontext.basis.knoten + probe), probe)
                    if (treffer.isEmpty()) null else e.copy(mitÜbersetzer = treffer.none { it.direkt })
                }.sortedWith(compareBy({ it.gruppe }, { it.vorlage.name }, { it.id }))
            }
            if (verbindungsKontext == kontext && atlas.editor.karte == kontext.basis) {
                einträge = ergebnis; lädt = false
            } else if (verbindungsKontext == kontext) schließen()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (verbindungsKontext == kontext) {
                lädt = false; filterFehler = "Die passenden Knoten konnten nicht ermittelt werden."
            }
        }
    }

    private fun stelleVorherigeAuswahlWiederHer() {
        atlas.editor.verwerfeVerbindungsAuswahl()
        when {
            vorherGruppe != null -> atlas.editor.wähleGruppe(vorherGruppe!!)
            vorherVerbindung != null -> atlas.editor.wähleVerbindung(vorherVerbindung)
            else -> atlas.editor.stelleAuswahlWiederHer(vorherigeAuswahl, vorherAktiv)
        }
    }
    private fun besitztEinfügung() = einfügeInteraktion != null && einfügeInteraktion == atlas.editor.interaktionsKennung
    private fun wähleNachAblage() {
        val kontext = verbindungsKontext ?: return
        if (!besitztEinfügung()) { schließen(); return }
        val knoten = atlas.editor.karte.knoten.firstOrNull { it.id == eingefügterKnoten } ?: run { schließen(); return }
        anschlussTreffer = atlas.editor.verbindungsEinfügeTreffer(kontext, atlas.editor.karte, knoten)
        dialogBasis = atlas.editor.karte
        if (anschlussTreffer.isEmpty()) { verwerfeEinfügung(); filterFehler = "Die Verbindung ist nicht mehr möglich." }
        else if (anschlussTreffer.size == 1) wähleAnschluss(anschlussTreffer.single())
        else ebene = PieEbene.Anschluss
    }
    fun wähleAnschluss(treffer: VerbindungsEinfügeTreffer) {
        if (treffer !in anschlussTreffer) return
        gewählterAnschluss = treffer
        if (treffer.wege.size == 1) bestätigeWeg(0) else ebene = PieEbene.Weg
    }
    fun bestätigeWeg(index: Int) {
        val kontext = verbindungsKontext ?: return
        val treffer = gewählterAnschluss ?: return
        val basis = dialogBasis ?: return
        val id = eingefügterKnoten ?: return
        if (!besitztEinfügung() || !atlas.editor.bestätigeVerbindungsEinfügung(kontext, basis, id, treffer.anschluss, index)) {
            verwerfeEinfügung(); filterFehler = "Die Karte oder Verbindung hat sich geändert. Bitte erneut auswählen."
            return
        }
        einfügeInteraktion = null; eingefügterKnoten = null
        schließen(); atlas.speichereAktuell()
    }
    fun verwerfeEinfügung() {
        if (besitztEinfügung()) {
            atlas.editor.verwerfeKnotenZiehen()
            stelleVorherigeAuswahlWiederHer()
        }
        einfügeInteraktion = null; eingefügterKnoten = null
        anschlussTreffer = emptyList(); gewählterAnschluss = null; dialogBasis = null
        if (verbindungsKontext != null && atlas.editor.karte == verbindungsKontext?.basis) ebene = PieEbene.Fächer
        else schließen()
    }
    val sichtbar get() = ebene in setOf(PieEbene.Kategorien, PieEbene.Fächer, PieEbene.Definition)
    val aktiv get() = ebene != PieEbene.Geschlossen
    val zieht get() = ebene == PieEbene.Ziehen
    val position get() = anker + versatz

    fun öffnen(punkt: Offset) {
        schließen()
        atlas.schließeKnotenAuswahl()
        konzepte = KonzeptBibliothekRegister.erstelle(statischeKnotenVorlagen())
        kartenEinträge = pieKartenEinträge(atlas.karten.filter(atlas::karteIstAlsKnotenZulässig), atlas::kartenVorlage)
        anker = punkt; versatz = Offset.Zero; ringVersatz = Offset.Zero; zoom = 1f
        kategorie = null; größen = emptyMap(); ebene = PieEbene.Kategorien
    }
    fun für(k: KonzeptKategorie): List<PieEintrag> = if (k.id == "eigene-karten") kartenEinträge else pieKonzeptEinträge(konzepte, k.id)
    fun wählen(index: Int) {
        val k = KonzeptBibliothekRegister.kategorien.getOrNull(index) ?: return
        val liste = für(k)
        if (liste.isEmpty()) return
        ringVersatz = versatz
        // Beide Ebenen teilen denselben Tipppunkt, auch nach dem Verschieben.
        zoom = 1f; kategorie = k; einträge = liste; ebene = PieEbene.Fächer
    }
    fun verschieben(delta: Offset) { versatz += delta }
    fun transformieren(mittelpunkt: Offset, pan: Offset, faktor: Float) {
        if (ebene != PieEbene.Fächer) { verschieben(pan); return }
        val neu = transformierePie(position, zoom, mittelpunkt, pan, faktor)
        versatz = neu.position - anker; zoom = neu.zoom
    }
    fun meldeGröße(id: String, größe: GraphGröße) {
        if (größen[id] != größe) größen = größen + (id to größe)
    }
    fun definitionÖffnen(e: PieEintrag) {
        definition = e.vorlage.erzeuge(GraphPunkt.Zero); ebene = PieEbene.Definition
    }
    fun zurück() {
        when (ebene) {
            PieEbene.Definition -> { definition = null; ebene = PieEbene.Fächer }
            PieEbene.Anschluss, PieEbene.Weg -> verwerfeEinfügung()
            PieEbene.Fächer -> { if (anschlussbezogen) { schließen(); return }; versatz = ringVersatz; zoom = 1f; kategorie = null; ebene = PieEbene.Kategorien }
            else -> schließen()
        }
    }
    fun beginneZiehen(e: PieEintrag, zeiger: Offset, relativ: Offset, dichte: Float) {
        val ref = e.vorlage.kartenVerweis
        if (ref != null && atlas.speicher.lade(ref)?.let(atlas::karteIstAlsKnotenZulässig) != true) { schließen(); return }
        val ansicht = atlas.editor.darstellungsKarte.ansicht
        // Beim Übergang zur Karte gilt wieder deren Zoom. Der gegriffene lokale
        // Punkt bleibt unter dem Zeiger, auch nach eigenständigem Menüzoom.
        griff = GraphPunkt(relativ.x / (dichte * ansicht.zoom * zoom), relativ.y / (dichte * ansicht.zoom * zoom))
        val knoten = e.vorlage.erzeuge(pieWeltPosition(zeiger, griff, ansicht, dichte))
        vorherGruppe = atlas.editor.ausgewählteGruppe; vorherVerbindung = atlas.editor.ausgewählteVerbindung
        vorherigeAuswahl = atlas.editor.ausgewählteKnoten; vorherAktiv = atlas.editor.ausgewählterKnoten
        val kontext = verbindungsKontext
        if (kontext != null) {
            if (!atlas.editor.beginneVerbindungsEinfügeZiehen(knoten, kontext)) { schließen(); return }
        } else atlas.editor.beginneKnotenEinfügeZiehen(knoten)
        ebene = PieEbene.Ziehen
        eingefügterKnoten = knoten.id
        einfügeInteraktion = atlas.editor.interaktionsKennung
        größen[e.id]?.let { atlas.editor.meldeInhaltsminimum(knoten.id, it) }
    }
    fun ziehe(zeiger: Offset, dichte: Float) {
        if (zieht) atlas.editor.aktualisiereKnotenZiehen(pieWeltPosition(zeiger, griff, atlas.editor.darstellungsKarte.ansicht, dichte))
    }
    fun ablegen() {
        if (zieht && anschlussbezogen) {
            atlas.editor.pausiereVerbindungsEinfügeZiehen()
            wähleNachAblage(); return
        }
        if (zieht) atlas.editor.beendeKnotenZiehen()
        ebene = PieEbene.Geschlossen
        if (atlas.editor.interaktionsKennung != einfügeInteraktion) einfügeInteraktion = null
        atlas.speichereAktuell()
    }
    fun schließen() {
        if (einfügeInteraktion != null && einfügeInteraktion == atlas.editor.interaktionsKennung) {
            atlas.editor.verwerfeKnotenZiehen()
            stelleVorherigeAuswahlWiederHer()
        }
        einfügeInteraktion = null; eingefügterKnoten = null
        verbindungsKontext = null; lädt = false; filterFehler = null
        anschlussTreffer = emptyList(); gewählterAnschluss = null; dialogBasis = null
        definition = null; ebene = PieEbene.Geschlossen
    }
}
