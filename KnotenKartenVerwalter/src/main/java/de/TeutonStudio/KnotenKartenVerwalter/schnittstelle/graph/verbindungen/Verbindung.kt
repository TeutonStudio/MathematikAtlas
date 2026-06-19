package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen

//typealias Kante = AnschlussGraphDaten.AnschlussKante

/*sealed class Verbindung(
    override val graph: Graph,
    override val daten: VerbindungDaten,
): GraphVerbindungObjekt<VerbindungDaten> {
    override var layoutCoordinates: MutableState<LayoutCoordinates?> = mutableStateOf(null)

    *//**
     * Erstellt das Kontextfenster dieser Verbindung.
     * Es wird von der Karte an der übergebenen Bildschirmposition geöffnet.
     *
     * @param pos Position des Kontextfensters im Bildschirmkoordinatenraum
     *//*
    @Composable
    public override fun KontextFenster(pos: BildschirmPosition) {
        Box(modifier = Modifier.offset { pos }.padding(vertical = 4.dp)) {
            Card() {
                Column(Modifier.padding(5.dp),horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("id: ${daten.id}",Modifier.scale(.9f),Color.Gray)
                    Text("löschen",Modifier.clickable() { graph.karte.vernichteVerbindung(this@Verbindung) })
                }
            }
        }
    }
}*/
