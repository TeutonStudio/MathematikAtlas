package de.TeutonStudio.MathematikRechenSystem.kern

/** Begrenzte Geometry-Nodes-Grundoperationen, ohne Rendering oder Editorzustand. */
object GeometrieOperationen {
    fun wuerfel(groesse: GeometrieVektor3 = GeometrieVektor3.Eins): ProzeduraleGeometrie {
        require(groesse.x >= 0 && groesse.y >= 0 && groesse.z >= 0)
        val p = listOf(-1 to -1,1 to -1,1 to 1,-1 to 1).let { xy -> listOf(-1,1).flatMap { z -> xy.map { (x,y) -> GeometrieVektor3(x*groesse.x/2,y*groesse.y/2,z*groesse.z/2) } } }
        val f = listOf(listOf(0,3,2,1),listOf(4,5,6,7),listOf(0,1,5,4),listOf(1,2,6,5),listOf(2,3,7,6),listOf(3,0,4,7))
        return ProzeduraleGeometrie(mesh=PolygonMesh(p,randKanten(f),f))
    }
    fun gitter(anzahlX: Int = 2, anzahlY: Int = 2, groesseX: Double = 1.0, groesseY: Double = 1.0): ProzeduraleGeometrie {
        require(anzahlX >= 2 && anzahlY >= 2 && groesseX.isFinite() && groesseY.isFinite() && groesseX >= 0 && groesseY >= 0)
        val n=anzahlX.toLong()*anzahlY
        val f=(anzahlX-1L)*(anzahlY-1L)
        require(n <= GeometrieGrenzen.PUNKTE && f <= GeometrieGrenzen.FLAECHEN && f*4 <= GeometrieGrenzen.ECKEN)
        require((anzahlX-1L)*anzahlY+(anzahlY-1L)*anzahlX <= GeometrieGrenzen.KANTEN)
        val p=List(n.toInt()) { i -> GeometrieVektor3(((i%anzahlX).toDouble()/(anzahlX-1)-0.5)*groesseX,((i/anzahlX).toDouble()/(anzahlY-1)-0.5)*groesseY,0.0) }
        val fl=List(f.toInt()) { i -> val a=(i/(anzahlX-1))*anzahlX+i%(anzahlX-1); listOf(a,a+1,a+anzahlX+1,a+anzahlX) }
        return ProzeduraleGeometrie(mesh=PolygonMesh(p,randKanten(fl),fl))
    }
    fun punkte(anzahl: Int, position: VektorFeld = VektorFeld.Konstant(GeometrieVektor3.Null)): ProzeduraleGeometrie {
        require(anzahl in 0..GeometrieGrenzen.PUNKTE)
        feldBudget(anzahl.toLong(),position)
        val auswertung=GeometrieFeldAuswertung()
        return ProzeduraleGeometrie(punktwolke=Punktwolke(List(anzahl) { auswertung.wert(position,GeometrieFeldKontext(GeometrieVektor3.Null,it)) }))
    }
    fun meshZuPunkten(geometrie: ProzeduraleGeometrie): ProzeduraleGeometrie {
        ohneInstanzen(geometrie)
        val anzahl=(geometrie.mesh?.punkte?.size ?: 0).toLong()+(geometrie.punktwolke?.punkte?.size ?: 0)
        require(anzahl <= GeometrieGrenzen.PUNKTE)
        return ProzeduraleGeometrie(punktwolke=Punktwolke(geometrie.mesh?.punkte.orEmpty()+geometrie.punktwolke?.punkte.orEmpty()))
    }
    fun verbinden(geometrien: List<ProzeduraleGeometrie>): ProzeduraleGeometrie {
        val budget=Budget()
        geometrien.forEach { budget.zaehlen(it); budget.instanzen += it.instanzen.size; budget.pruefen() }
        val p=ArrayList<GeometrieVektor3>(); val k=ArrayList<MeshKante>(); val f=ArrayList<List<Int>>(); val wolke=ArrayList<GeometrieVektor3>(); val instanzen=ArrayList<GeometrieInstanz>()
        geometrien.forEach { g ->
            g.mesh?.let { m -> val offset=p.size; p.addAll(m.punkte); k.addAll(m.kanten.map { MeshKante(it.a+offset,it.b+offset) }); f.addAll(m.flaechen.map { fl -> fl.map { it+offset } }) }
            wolke.addAll(g.punktwolke?.punkte.orEmpty()); instanzen.addAll(g.instanzen)
        }
        return ProzeduraleGeometrie(if(geometrien.any { it.mesh != null }) PolygonMesh(p,k,f) else null,if(geometrien.any { it.punktwolke != null }) Punktwolke(wolke) else null,instanzen)
    }
    fun transformieren(geometrie: ProzeduraleGeometrie, transformation: GeometrieTransformation): ProzeduraleGeometrie = ProzeduraleGeometrie(
        geometrie.mesh?.let { PolygonMesh(it.punkte.map(transformation::anwenden),it.kanten,it.flaechen) },
        geometrie.punktwolke?.let { Punktwolke(it.punkte.map(transformation::anwenden)) },
        geometrie.instanzen.map { it.copy(transformation=transformation*it.transformation) },
    )
    fun positionSetzen(geometrie: ProzeduraleGeometrie, auswahl: AuswahlFeld = AuswahlFeld.Konstant(true), position: VektorFeld = VektorFeld.Position, versatz: VektorFeld = VektorFeld.Konstant(GeometrieVektor3.Null)): ProzeduraleGeometrie {
        ohneInstanzen(geometrie)
        feldBudget((geometrie.mesh?.punkte?.size ?: 0).toLong()+(geometrie.punktwolke?.punkte?.size ?: 0),auswahl,position,versatz)
        val auswertung=GeometrieFeldAuswertung()
        fun verschieben(punkte: List<GeometrieVektor3>) = punkte.mapIndexed { i,p -> val kontext=GeometrieFeldKontext(p,i); if(auswertung.wert(auswahl,kontext)) auswertung.wert(position,kontext)+auswertung.wert(versatz,kontext) else p }
        return ProzeduraleGeometrie(geometrie.mesh?.let { PolygonMesh(verschieben(it.punkte),it.kanten,it.flaechen) },geometrie.punktwolke?.let { Punktwolke(verschieben(it.punkte)) })
    }
    fun aufPunktenInstanzieren(punkte: ProzeduraleGeometrie, instanz: ProzeduraleGeometrie, auswahl: AuswahlFeld = AuswahlFeld.Konstant(true), rotation: VektorFeld = VektorFeld.Konstant(GeometrieVektor3.Null), skalierung: VektorFeld = VektorFeld.Konstant(GeometrieVektor3.Eins)): ProzeduraleGeometrie {
        ohneInstanzen(punkte)
        require(instanz.tiefe < GeometrieGrenzen.TIEFE)
        feldBudget((punkte.mesh?.punkte?.size ?: 0).toLong()+(punkte.punktwolke?.punkte?.size ?: 0),auswahl,rotation,skalierung)
        val auswertung=GeometrieFeldAuswertung(); val ergebnis=ArrayList<GeometrieInstanz>()
        fun erzeugen(positionen: List<GeometrieVektor3>) { positionen.forEachIndexed { i,p ->
            val kontext=GeometrieFeldKontext(p,i)
            if(auswertung.wert(auswahl,kontext)) {
                require(ergebnis.size < GeometrieGrenzen.INSTANZEN) { "Zu viele Instanzen." }
                ergebnis.add(GeometrieInstanz(instanz,GeometrieTransformation.trs(p,auswertung.wert(rotation,kontext),auswertung.wert(skalierung,kontext))))
            }
        } }
        erzeugen(punkte.mesh?.punkte.orEmpty()); erzeugen(punkte.punktwolke?.punkte.orEmpty())
        return ProzeduraleGeometrie(instanzen=ergebnis)
    }
    /** Erst gesamte Expansion prüfen, dann Allokation. Gemeinsame Referenzen zählen je Vorkommen. */
    fun realisieren(geometrie: ProzeduraleGeometrie): ProzeduraleGeometrie {
        val budget=Budget()
        fun pruefen(g: ProzeduraleGeometrie, tiefe: Int) {
            require(tiefe <= GeometrieGrenzen.TIEFE)
            budget.besuche++; budget.zaehlen(g); budget.instanzen += g.instanzen.size; budget.pruefen()
            g.instanzen.forEach { pruefen(it.geometrie,tiefe+1) }
        }
        pruefen(geometrie,1)
        val p=ArrayList<GeometrieVektor3>(); val k=ArrayList<MeshKante>(); val f=ArrayList<List<Int>>(); val wolke=ArrayList<GeometrieVektor3>()
        var hatMesh=false; var hatWolke=false
        fun sammeln(g: ProzeduraleGeometrie, t: GeometrieTransformation) {
            g.mesh?.let { m -> hatMesh=true; val offset=p.size; p.addAll(m.punkte.map(t::anwenden)); k.addAll(m.kanten.map { MeshKante(it.a+offset,it.b+offset) }); f.addAll(m.flaechen.map { fl -> fl.map { it+offset } }) }
            g.punktwolke?.let { hatWolke=true; wolke.addAll(it.punkte.map(t::anwenden)) }
            g.instanzen.forEach { sammeln(it.geometrie,t*it.transformation) }
        }
        sammeln(geometrie,GeometrieTransformation.Identitaet)
        return ProzeduraleGeometrie(if(hatMesh) PolygonMesh(p,k,f) else null,if(hatWolke) Punktwolke(wolke) else null)
    }
    /** Konservative Vorabgrenze: auch abgewählte Punkte zählen alle angebotenen Felder. */
    private fun feldBudget(anzahl: Long, vararg felder: GeometrieFeld<*>) {
        require(anzahl*felder.sumOf { it.groesse.toLong() } <= GeometrieGrenzen.FELD_OPERATIONEN) { "Feld-Auswertungsbudget überschritten." }
    }
    private fun ohneInstanzen(g: ProzeduraleGeometrie) { require(g.instanzen.isEmpty()) { "Instanzen zuerst mit Realize Instances realisieren." } }
    private fun randKanten(f: List<List<Int>>) = f.flatMap { fl -> fl.indices.map { MeshKante(fl[it],fl[(it+1)%fl.size]) } }.distinct()
    private class Budget {
        var vertices=0L; var punkte=0L; var kanten=0L; var flaechen=0L; var ecken=0L; var instanzen=0L; var besuche=0L
        fun zaehlen(g: ProzeduraleGeometrie) {
            g.mesh?.let { vertices+=it.punkte.size; kanten+=it.kanten.size; flaechen+=it.flaechen.size; ecken+=it.flaechen.sumOf { f -> f.size.toLong() } }
            punkte+=g.punktwolke?.punkte?.size ?: 0
            pruefen()
        }
        fun pruefen() { require(vertices <= GeometrieGrenzen.PUNKTE && punkte <= GeometrieGrenzen.PUNKTE && kanten <= GeometrieGrenzen.KANTEN && flaechen <= GeometrieGrenzen.FLAECHEN && ecken <= GeometrieGrenzen.ECKEN && instanzen <= GeometrieGrenzen.INSTANZEN && besuche <= GeometrieGrenzen.INSTANZEN+1) { "Geometriebudget überschritten; Expansion wurde vorab abgebrochen." } }
    }
}
