package de.TeutonStudio.MathematikAtlas

import androidx.compose.runtime.*

/** Eine Auswahl bestätigt den gesamten noch offenen Einfügeentwurf. */
@Composable
internal fun PieVerbindungsDialoge(atlas: AtlasZustand) {
    val pie = atlas.pie
    if (pie.ebene !in setOf(PieEbene.Anschluss, PieEbene.Weg)) return
    val anschlussWahl = pie.ebene == PieEbene.Anschluss
    val optionen = if (anschlussWahl) pie.anschlussTreffer.map { t ->
        val port = atlas.editor.karte.knoten.firstOrNull { it.id == t.anschluss.knotenId }
            ?.anschlüsse?.firstOrNull { it.id == t.anschluss.anschlussId }
        (port?.name ?: "Anschluss") to "${atlas.anschlussInfoFür(atlas.editor.karte, t.anschluss)?.typ ?: t.typ}${if (t.direkt) "" else " · Mit Übersetzer"}"
    } else pie.gewählterAnschluss?.wege.orEmpty().map { it.titel to "" }
    de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.VerbindungsEinfügeAuswahlDialog(
        titel = if (anschlussWahl) "Anschluss auswählen" else "Verbindungsweg auswählen",
        optionen = optionen,
        abbrechen = pie::verwerfeEinfügung,
        übernehmen = { index -> if (anschlussWahl) pie.wähleAnschluss(pie.anschlussTreffer[index]) else pie.bestätigeWeg(index) },
    )
}
