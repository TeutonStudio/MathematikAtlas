package de.TeutonStudio.KnotenKartenVerwalter.logik

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand

sealed interface AtlasBefehl {
    data object Speichern : AtlasBefehl
    data object Rückgängig : AtlasBefehl
    data object Wiederholen : AtlasBefehl
    data object AuswahlLöschen : AtlasBefehl
    data object AllesAuswählen : AtlasBefehl
    data object AuswahlKopieren : AtlasBefehl
    data object AuswahlAusschneiden : AtlasBefehl
    data class AuswahlEinfügen(val position: GraphPunkt? = null) : AtlasBefehl
    data object AuswahlDuplizieren : AtlasBefehl
    data class AuswahlVerschieben(val delta: GraphPunkt) : AtlasBefehl
    data object AuswahlGruppieren : AtlasBefehl
    data object GruppierungAufheben : AtlasBefehl
    data object InhaltEinpassen : AtlasBefehl
    data object AuswahlZentrieren : AtlasBefehl
    data class ZoomSetzen(val faktor: Float) : AtlasBefehl
    data class ZoomÄndern(val faktor: Float) : AtlasBefehl
    data object InteraktionAbbrechen : AtlasBefehl
    data object KnotenAuswahlÖffnen : AtlasBefehl
    data object Umbenennen : AtlasBefehl
    data object SucheÖffnen : AtlasBefehl
}

enum class AtlasFokusBereich { TextEditor, Dialog, Inspektor, Katalog, Karte, Anwendung }

data class BefehlsKontext(
    val fokus: AtlasFokusBereich = AtlasFokusBereich.Karte,
    val editierbar: Boolean = true,
    val zeigerPosition: GraphPunkt? = null,
    val sichtbareMitte: GraphPunkt = GraphPunkt(160f, 120f),
    val anzeigeBreiteDp: Float = 0f,
    val anzeigeHöheDp: Float = 0f,
)

data class AtlasBefehlsMetadaten(val name: String, val tastenkürzel: String?)

data class KnotenAusschnitt(
    val knoten: List<KnotenDaten>,
    val verbindungen: List<VerbindungDaten>,
    val gruppen: List<VisuelleKnotenGruppeDaten>,
    val ursprung: GraphPunkt,
)

class AtlasZwischenablage {
    private var ausschnitt: KnotenAusschnitt? = null
    private var einfügeZähler: Int = 0

    fun istLeer(): Boolean = ausschnitt == null

    fun kopiere(karte: KartenDaten, ids: Set<KnotenId>): Boolean {
        val knoten = karte.knoten.filter { it.id in ids }
        if (knoten.isEmpty()) return false
        val ursprung = GraphPunkt(knoten.minOf { it.position.x }, knoten.minOf { it.position.y })
        ausschnitt = KnotenAusschnitt(
            knoten = knoten,
            verbindungen = karte.verbindungen.filter { it.von.knotenId in ids && it.zu.knotenId in ids },
            gruppen = karte.visuelleGruppen.mapNotNull { gruppe ->
                val enthalten = gruppe.knotenIds.intersect(ids)
                gruppe.takeIf { enthalten.size >= 2 }?.copy(knotenIds = enthalten)
            },
            ursprung = ursprung,
        )
        einfügeZähler = 0
        return true
    }

    fun erzeugeEinfügung(position: GraphPunkt): KnotenAusschnitt? {
        val quelle = ausschnitt ?: return null
        val versatz = GraphPunkt(24f * einfügeZähler, 24f * einfügeZähler)
        einfügeZähler += 1
        val knotenIds = quelle.knoten.associate { it.id to neueKnotenId() }
        val anschlussIds = quelle.knoten.flatMap { it.anschlüsse }.associate { it.id to neueAnschlussId() }
        val gruppenIds = quelle.gruppen.associate { it.id to neueVisuelleGruppenId() }
        val delta = position - quelle.ursprung + versatz
        val knoten = quelle.knoten.map { original ->
            original.copy(
                id = knotenIds.getValue(original.id),
                position = original.position + delta,
                anschlüsse = original.anschlüsse.map { it.copy(id = anschlussIds.getValue(it.id)) },
            )
        }
        val verbindungen = quelle.verbindungen.map { verbindung ->
            VerbindungDaten(
                von = AnschlussVerweis(
                    knotenIds.getValue(verbindung.von.knotenId),
                    anschlussIds.getValue(verbindung.von.anschlussId),
                ),
                zu = AnschlussVerweis(
                    knotenIds.getValue(verbindung.zu.knotenId),
                    anschlussIds.getValue(verbindung.zu.anschlussId),
                ),
            )
        }
        val gruppen = quelle.gruppen.map { gruppe ->
            gruppe.copy(
                id = gruppenIds.getValue(gruppe.id),
                knotenIds = gruppe.knotenIds.mapTo(linkedSetOf()) { knotenIds.getValue(it) },
                position = gruppe.position + delta,
            )
        }
        return KnotenAusschnitt(knoten, verbindungen, gruppen, position + versatz)
    }
}

class AtlasBefehlsAusführer(
    private val editor: KartenEditorZustand,
    private val speichern: () -> Unit = {},
    private val knotenAuswahlÖffnen: (GraphPunkt) -> Unit = {},
    private val umbenennen: () -> Unit = {},
    private val sucheÖffnen: () -> Unit = {},
    val zwischenablage: AtlasZwischenablage = AtlasZwischenablage(),
) {
    private var wiederholbareAktionAktiv = false

    fun beginneWiederholbareAktion() {
        if (!wiederholbareAktionAktiv) {
            editor.beginneInteraktion()
            wiederholbareAktionAktiv = true
        }
    }

    fun beendeWiederholbareAktion() {
        if (wiederholbareAktionAktiv) {
            editor.beendeInteraktion()
            wiederholbareAktionAktiv = false
        }
    }

    fun verschiebeAuswahlWiederholbar(delta: GraphPunkt, kontext: BefehlsKontext): Boolean {
        if (!istVerfügbar(AtlasBefehl.AuswahlVerschieben(delta), kontext)) return false
        beginneWiederholbareAktion()
        editor.verschiebeAuswahl(delta, mitHistorie = false)
        return true
    }

    fun metadaten(befehl: AtlasBefehl): AtlasBefehlsMetadaten = when (befehl) {
        AtlasBefehl.Speichern -> AtlasBefehlsMetadaten("Speichern", "Ctrl+S")
        AtlasBefehl.Rückgängig -> AtlasBefehlsMetadaten("Rückgängig", "Ctrl+Z")
        AtlasBefehl.Wiederholen -> AtlasBefehlsMetadaten("Wiederholen", "Ctrl+Shift+Z / Ctrl+Y")
        AtlasBefehl.AuswahlLöschen -> AtlasBefehlsMetadaten("Auswahl löschen", "Entf")
        AtlasBefehl.AllesAuswählen -> AtlasBefehlsMetadaten("Alles auswählen", "Ctrl+A")
        AtlasBefehl.AuswahlKopieren -> AtlasBefehlsMetadaten("Kopieren", "Ctrl+C")
        AtlasBefehl.AuswahlAusschneiden -> AtlasBefehlsMetadaten("Ausschneiden", "Ctrl+X")
        is AtlasBefehl.AuswahlEinfügen -> AtlasBefehlsMetadaten("Einfügen", "Ctrl+V")
        AtlasBefehl.AuswahlDuplizieren -> AtlasBefehlsMetadaten("Duplizieren", "Ctrl+D")
        is AtlasBefehl.AuswahlVerschieben -> AtlasBefehlsMetadaten("Auswahl verschieben", "Pfeiltasten")
        AtlasBefehl.AuswahlGruppieren -> AtlasBefehlsMetadaten("Visuell gruppieren", "Ctrl+G")
        AtlasBefehl.GruppierungAufheben -> AtlasBefehlsMetadaten("Gruppierung aufheben", "Ctrl+Shift+G")
        AtlasBefehl.InhaltEinpassen -> AtlasBefehlsMetadaten("Inhalt einpassen", "Home")
        AtlasBefehl.AuswahlZentrieren -> AtlasBefehlsMetadaten("Auswahl zentrieren", "F")
        is AtlasBefehl.ZoomSetzen -> AtlasBefehlsMetadaten("Zoom setzen", "0")
        is AtlasBefehl.ZoomÄndern -> AtlasBefehlsMetadaten("Zoom ändern", "+ / -")
        AtlasBefehl.InteraktionAbbrechen -> AtlasBefehlsMetadaten("Abbrechen", "Esc")
        AtlasBefehl.KnotenAuswahlÖffnen -> AtlasBefehlsMetadaten("Knoten auswählen", "N")
        AtlasBefehl.Umbenennen -> AtlasBefehlsMetadaten("Umbenennen", "F2")
        AtlasBefehl.SucheÖffnen -> AtlasBefehlsMetadaten("Suchen", "Ctrl+F")
    }

    fun istVerfügbar(befehl: AtlasBefehl, kontext: BefehlsKontext): Boolean {
        val karteFokussiert = kontext.fokus == AtlasFokusBereich.Karte
        val hatAuswahl = editor.ausgewählteKnoten.isNotEmpty() || editor.ausgewählteVerbindung != null
        return when (befehl) {
            AtlasBefehl.Speichern -> kontext.editierbar
            AtlasBefehl.Rückgängig -> karteFokussiert && editor.kannRückgängig()
            AtlasBefehl.Wiederholen -> karteFokussiert && editor.kannWiederholen()
            AtlasBefehl.AuswahlLöschen, AtlasBefehl.AuswahlDuplizieren,
            AtlasBefehl.AuswahlKopieren, AtlasBefehl.AuswahlAusschneiden,
            is AtlasBefehl.AuswahlVerschieben -> karteFokussiert && kontext.editierbar && hatAuswahl
            AtlasBefehl.AllesAuswählen -> karteFokussiert && editor.karte.knoten.isNotEmpty()
            is AtlasBefehl.AuswahlEinfügen -> karteFokussiert && kontext.editierbar && !zwischenablage.istLeer()
            AtlasBefehl.AuswahlGruppieren -> karteFokussiert && editor.ausgewählteKnoten.size >= 2
            AtlasBefehl.GruppierungAufheben -> karteFokussiert && editor.auswahlIstVisuellGruppiert()
            AtlasBefehl.InhaltEinpassen -> editor.karte.knoten.isNotEmpty() && kontext.anzeigeBreiteDp > 0f && kontext.anzeigeHöheDp > 0f
            AtlasBefehl.AuswahlZentrieren -> editor.ausgewählteKnoten.isNotEmpty()
            is AtlasBefehl.ZoomSetzen, is AtlasBefehl.ZoomÄndern -> karteFokussiert
            AtlasBefehl.InteraktionAbbrechen -> true
            AtlasBefehl.KnotenAuswahlÖffnen -> karteFokussiert && kontext.editierbar
            AtlasBefehl.Umbenennen -> karteFokussiert && editor.ausgewählterKnoten != null
            AtlasBefehl.SucheÖffnen -> true
        }
    }

    fun führeAus(befehl: AtlasBefehl, kontext: BefehlsKontext): Boolean {
        if (!istVerfügbar(befehl, kontext)) return false
        when (befehl) {
            AtlasBefehl.Speichern -> speichern()
            AtlasBefehl.Rückgängig -> editor.rückgängig()
            AtlasBefehl.Wiederholen -> editor.wiederholen()
            AtlasBefehl.AuswahlLöschen -> editor.löscheAuswahl()
            AtlasBefehl.AllesAuswählen -> editor.wähleAlleKnoten()
            AtlasBefehl.AuswahlKopieren -> zwischenablage.kopiere(editor.karte, editor.ausgewählteKnoten)
            AtlasBefehl.AuswahlAusschneiden -> {
                if (zwischenablage.kopiere(editor.karte, editor.ausgewählteKnoten)) editor.löscheAuswahl()
            }
            is AtlasBefehl.AuswahlEinfügen -> {
                val ziel = befehl.position ?: kontext.zeigerPosition ?: kontext.sichtbareMitte
                zwischenablage.erzeugeEinfügung(ziel)?.let { ausschnitt ->
                    editor.führeAus(KartenAktion.KnotenMehrfachEinfügen(ausschnitt.knoten, ausschnitt.verbindungen, ausschnitt.gruppen))
                    editor.stelleAuswahlWiederHer(ausschnitt.knoten.mapTo(linkedSetOf()) { it.id }, ausschnitt.knoten.lastOrNull()?.id)
                }
            }
            AtlasBefehl.AuswahlDuplizieren -> editor.dupliziereAuswahl()
            is AtlasBefehl.AuswahlVerschieben -> editor.verschiebeAuswahl(befehl.delta)
            AtlasBefehl.AuswahlGruppieren -> editor.gruppiereAuswahlVisuell()
            AtlasBefehl.GruppierungAufheben -> editor.hebeVisuelleGruppierungDerAuswahlAuf()
            AtlasBefehl.InhaltEinpassen -> editor.karte.ansichtFürInhalt(
                anzeigeBreiteDp = kontext.anzeigeBreiteDp,
                anzeigeHöheDp = kontext.anzeigeHöheDp,
                pufferDp = 40f,
            )?.let { editor.führeAus(KartenAktion.AnsichtÄndern(it), mitHistorie = false) }
            AtlasBefehl.AuswahlZentrieren -> zentriereAuswahl(kontext)
            is AtlasBefehl.ZoomSetzen -> setzeZoom(befehl.faktor, kontext.sichtbareMitte)
            is AtlasBefehl.ZoomÄndern -> setzeZoom(editor.karte.ansicht.zoom * befehl.faktor, kontext.zeigerPosition ?: kontext.sichtbareMitte)
            AtlasBefehl.InteraktionAbbrechen -> editor.brecheInteraktionAb()
            AtlasBefehl.KnotenAuswahlÖffnen -> knotenAuswahlÖffnen(kontext.zeigerPosition ?: kontext.sichtbareMitte)
            AtlasBefehl.Umbenennen -> umbenennen()
            AtlasBefehl.SucheÖffnen -> sucheÖffnen()
        }
        return true
    }

    private fun zentriereAuswahl(kontext: BefehlsKontext) {
        val auswahl = editor.karte.knoten.filter { it.id in editor.ausgewählteKnoten }
        if (auswahl.isEmpty()) return
        val mitte = GraphPunkt(
            (auswahl.minOf { it.position.x } + auswahl.maxOf { it.position.x + it.größe.breite }) / 2f,
            (auswahl.minOf { it.position.y } + auswahl.maxOf { it.position.y + it.größe.höhe }) / 2f,
        )
        val ansicht = editor.karte.ansicht
        editor.führeAus(
            KartenAktion.AnsichtÄndern(ansicht.copy(verschiebung = GraphPunkt(
                kontext.anzeigeBreiteDp / 2f - mitte.x * ansicht.zoom,
                kontext.anzeigeHöheDp / 2f - mitte.y * ansicht.zoom,
            ))),
            mitHistorie = false,
        )
    }

    private fun setzeZoom(zoom: Float, zentrum: GraphPunkt) {
        val alt = editor.karte.ansicht
        val neu = zoom.coerceIn(.25f, 3.5f)
        val faktor = neu / alt.zoom
        val verschiebung = GraphPunkt(
            zentrum.x - (zentrum.x - alt.verschiebung.x) * faktor,
            zentrum.y - (zentrum.y - alt.verschiebung.y) * faktor,
        )
        editor.führeAus(KartenAktion.AnsichtÄndern(AnsichtsFenster(verschiebung, neu)), mitHistorie = false)
    }
}

fun KartenDaten.ansichtFürInhalt(
    anzeigeBreiteDp: Float,
    anzeigeHöheDp: Float,
    pufferDp: Float,
): AnsichtsFenster? {
    val erster = knoten.firstOrNull() ?: return null
    if (anzeigeBreiteDp <= 0f || anzeigeHöheDp <= 0f) return null
    var links = erster.position.x
    var oben = erster.position.y
    var rechts = erster.position.x + erster.größe.breite
    var unten = erster.position.y + erster.größe.höhe
    knoten.drop(1).forEach { knoten ->
        links = minOf(links, knoten.position.x)
        oben = minOf(oben, knoten.position.y)
        rechts = maxOf(rechts, knoten.position.x + knoten.größe.breite)
        unten = maxOf(unten, knoten.position.y + knoten.größe.höhe)
    }
    val zoom = minOf(
        (anzeigeBreiteDp - 2f * pufferDp).coerceAtLeast(1f) / (rechts - links).coerceAtLeast(1f),
        (anzeigeHöheDp - 2f * pufferDp).coerceAtLeast(1f) / (unten - oben).coerceAtLeast(1f),
    ).coerceIn(.25f, 3.5f)
    val mitteX = (links + rechts) / 2f
    val mitteY = (oben + unten) / 2f
    return AnsichtsFenster(
        verschiebung = GraphPunkt(
            anzeigeBreiteDp / 2f - mitteX * zoom,
            anzeigeHöheDp / 2f - mitteY * zoom,
        ),
        zoom = zoom,
    )
}
