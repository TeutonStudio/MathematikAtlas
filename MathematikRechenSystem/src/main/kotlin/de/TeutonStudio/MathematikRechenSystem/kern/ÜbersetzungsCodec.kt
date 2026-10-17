package de.TeutonStudio.MathematikRechenSystem.kern

import java.io.*
import java.util.Base64

/** Explizite Datenformate; alle Rekursionen teilen die Tiefengrenze des mathematischen Wertcodecs. */
object ÜbersetzungsCodec {
    private fun grenze(t: Int) { require(t <= 128) { "Übersetzungsdaten sind zu tief verschachtelt." } }
    private fun n(d: DataInputStream) = d.readInt().also { require(it in 0..100_000) { "Ungültige Komponentenanzahl." } }
    private fun text(d: DataOutputStream, s: String) = FormelKontextCodec.text(d, s)
    private fun text(d: DataInputStream) = FormelKontextCodec.text(d)
    private fun wert(d: DataOutputStream, w: MathematischesObjekt, t: Int) = MathematischerWertCodec.schreibeIntern(d, w, t + 1)
    private fun wert(d: DataInputStream, t: Int) = MathematischerWertCodec.liesIntern(d, t + 1)
    private fun optional(d: DataOutputStream, w: MathematischesObjekt?, t: Int) { d.writeBoolean(w != null); w?.let { wert(d, it, t) } }
    private fun optional(d: DataInputStream, t: Int) = if (d.readBoolean()) wert(d, t) else null
    private fun texte(d: DataOutputStream, s: List<String>) { require(s.size <= 100_000); d.writeInt(s.size); s.forEach { text(d, it) } }
    private fun texte(d: DataInputStream) = List(n(d)) { text(d) }
    private fun map(d: DataOutputStream, m: Map<String, String>) { d.writeInt(m.size); m.forEach { (k,v) -> text(d,k); text(d,v) } }
    private fun map(d: DataInputStream): Map<String, String> = List(n(d)) { text(d) to text(d) }.also { require(it.map { p -> p.first }.distinct().size == it.size) }.toMap()
    private fun aussagen(d: DataOutputStream, a: Collection<Aussage>, t: Int) { d.writeInt(a.size); a.forEach { wert(d,it,t) } }
    private fun aussagen(d: DataInputStream, t: Int) = List(n(d)) { wert(d,t) as Aussage }

    internal fun form(d: DataOutputStream, f: ÜbersetzungsStruktur, t: Int) {
        grenze(t); text(d,f.art.name); text(d,f.blattTyp); d.writeInt(f.dimensionen.size); f.dimensionen.forEach(d::writeInt)
        d.writeInt(f.kinder.size); f.kinder.forEach { form(d,it,t+1) }
    }
    internal fun form(d: DataInputStream, t: Int): ÜbersetzungsStruktur {
        grenze(t); val art = StrukturArt.valueOf(text(d)); val typ = text(d); val dim = List(n(d)) { d.readInt() }
        return ÜbersetzungsStruktur(art, List(n(d)) { form(d,t+1) }, dim, typ)
    }
    internal fun plan(d: DataOutputStream, p: StrukturRückführungsplan, t: Int) {
        grenze(t); d.writeInt(p.version); form(d,p.quelle,t+1); d.writeInt(p.quellPfade.size)
        p.quellPfade.forEach { pfad -> d.writeInt(pfad.size); pfad.forEach(d::writeInt) }
    }
    internal fun plan(d: DataInputStream, t: Int): StrukturRückführungsplan {
        grenze(t); val version = d.readInt(); val quelle = form(d,t+1)
        return StrukturRückführungsplan(quelle,List(n(d)) { List(n(d)) { d.readInt() } },version)
    }
    internal fun kontext(d: DataOutputStream, k: ÜbersetzungsKontext, t: Int) {
        grenze(t); optional(d,k.definitionsRaum,t); optional(d,k.werteBereich,t)
        d.writeBoolean(k.komponentenRäume != null); k.komponentenRäume?.let { r -> d.writeInt(r.size); r.forEach { wert(d,it,t) } }
        aussagen(d,k.annahmen,t); d.writeInt(k.quellen.size)
        k.quellen.forEach { q -> text(d,q.id); text(d,q.name); optional(d,q.bereich,t); text(d,q.rolle)
            d.writeInt(q.komponentenPfad.size); q.komponentenPfad.forEach(d::writeInt); d.writeBoolean(q.symbolId != null); q.symbolId?.let { text(d,it) } }
        texte(d,k.argumentReihenfolge); text(d,k.datenGenauigkeit.name)
        optional(d,k.bezugsRaum,t); optional(d,k.anker,t); optional(d,k.verschiebung?.let { Tupel(it) },t)
    }
    internal fun kontext(d: DataInputStream, t: Int): ÜbersetzungsKontext {
        grenze(t); val domain = optional(d,t) as MengenAusdruck?; val range = optional(d,t) as MengenAusdruck?
        val teile = if (d.readBoolean()) List(n(d)) { wert(d,t) as MengenAusdruck } else null
        val annahmen = aussagen(d,t).toSet()
        val quellen = List(n(d)) { val id = text(d); val name = text(d); val bereich = optional(d,t) as MengenAusdruck?
            val rolle = text(d); val pfad = List(n(d)) { d.readInt() }; val symbol = if(d.readBoolean()) text(d) else null
            ÜbersetzungsQuelle(id,name,bereich,rolle,pfad,symbol) }
        return ÜbersetzungsKontext(domain,range,teile,annahmen,quellen,texte(d), ÜbersetzungsGenauigkeit.valueOf(text(d)),
            optional(d,t) as EuklidischerRaum?, optional(d,t) as Tupel?, (optional(d,t) as Tupel?)?.elemente?.map { it as ZahlAusdruck })
    }
    private fun status(d: DataOutputStream, s: NachweisStatus, t: Int) {
        when(s) {
            NachweisStatus.Nachgewiesen -> d.writeByte(0); NachweisStatus.Widerlegt -> d.writeByte(1)
            NachweisStatus.Unvollstaendig -> d.writeByte(2); NachweisStatus.Unentscheidbar -> d.writeByte(3)
            is NachweisStatus.Bedingt -> { d.writeByte(4); aussagen(d,s.bedingungen,t) }
        }
    }
    private fun status(d: DataInputStream, t: Int): NachweisStatus = when(d.readByte().toInt()) {
        0 -> NachweisStatus.Nachgewiesen; 1 -> NachweisStatus.Widerlegt; 2 -> NachweisStatus.Unvollstaendig
        3 -> NachweisStatus.Unentscheidbar; 4 -> NachweisStatus.Bedingt(aussagen(d,t)); else -> error("Unbekannter Nachweisstatus.")
    }
    internal fun zeugnis(d: DataOutputStream, z: ÜbersetzungsZeugnis, t: Int) {
        grenze(t); text(d,z.regelId); d.writeInt(z.regelVersion); form(d,z.quelle,t+1); form(d,z.ziel,t+1)
        kontext(d,z.quellKontext,t+1); optional(d,z.zielTräger,t); optional(d,z.bildRaum,t)
        map(d,z.parameter); map(d,z.identitätsZuordnung); aussagen(d,z.voraussetzungen,t)
        status(d,z.injektiv,t); status(d,z.surjektivAufTräger,t); status(d,z.informationserhaltung,t)
        d.writeBoolean(z.rückführungsplan != null); z.rückführungsplan?.let { plan(d,it,t+1) }
        text(d,z.genauigkeit.name); texte(d,z.verloreneInformationen); texte(d,z.diagnose)
        d.writeBoolean(z.ungeprüfteBehauptungen != null); z.ungeprüfteBehauptungen?.let {
            status(d,it.injektiv,t);status(d,it.surjektivAufTräger,t);status(d,it.informationserhaltung,t);text(d,it.genauigkeit.name)
        }
    }
    internal fun zeugnis(d: DataInputStream, t: Int): ÜbersetzungsZeugnis {
        grenze(t); val id = text(d); val version = d.readInt(); val quelle = form(d,t+1); val ziel = form(d,t+1)
        val k = kontext(d,t+1); val träger = optional(d,t) as MengenAusdruck?; val bild = optional(d,t) as MengenAusdruck?
        val p = map(d); val ids = map(d); val voraus = aussagen(d,t).toSet()
        val inj = status(d,t); val surj = status(d,t); val info = status(d,t)
        val rück = if(d.readBoolean()) plan(d,t+1) else null
        val genau = ÜbersetzungsGenauigkeit.valueOf(text(d)); val verluste = texte(d); val diagnosen = texte(d)
        val ungeprüft=if(d.readBoolean()) ÜbersetzungsBehauptungen(status(d,t),status(d,t),status(d,t),ÜbersetzungsGenauigkeit.valueOf(text(d))) else null
        return ÜbersetzungsZeugnis(id,version,quelle,ziel,k,träger,bild,p,ids,voraus,inj,surj,info,rück,genau,verluste,diagnosen,ungeprüft).prüfeÜbertragung()
    }
    internal fun metadaten(d: DataOutputStream, m: ÜbersetzungsMetadaten, t: Int = 0) {
        grenze(t); d.writeBoolean(m.aktuell != null); m.aktuell?.let { zeugnis(d,it,t+1) }
        d.writeInt(m.herkunft.size); m.herkunft.forEach { zeugnis(d,it,t+1) }
        d.writeBoolean(m.kontext != null); m.kontext?.let { kontext(d,it,t+1) }
    }
    internal fun metadaten(d: DataInputStream, t: Int = 0): ÜbersetzungsMetadaten {
        grenze(t); val aktuell = if(d.readBoolean()) zeugnis(d,t+1) else null
        val herkunft = List(n(d)) { zeugnis(d,t+1) }
        val kontext = if(d.readBoolean()) kontext(d,t+1) else null
        return ÜbersetzungsMetadaten(aktuell,herkunft,kontext)
    }
    internal fun bild(d: DataOutputStream, b: ÜbersetzungsBildraum, t: Int) {
        grenze(t); wert(d,b.quellRaum,t); text(d,b.regelId); d.writeInt(b.regelVersion)
        form(d,b.quellStruktur,t+1); form(d,b.zielStruktur,t+1); map(d,b.parameter)
        d.writeBoolean(b.rückführungsplan != null); b.rückführungsplan?.let { plan(d,it,t+1) }
    }
    internal fun bild(d: DataInputStream, t: Int): ÜbersetzungsBildraum {
        grenze(t); val q = wert(d,t) as MengenAusdruck; val id=text(d); val version=d.readInt()
        val quelle=form(d,t+1); val ziel=form(d,t+1); val p=map(d); val plan=if(d.readBoolean()) plan(d,t+1) else null
        return ÜbersetzungsBildraum(q,id,quelle,ziel,p,plan,version)
    }
    private fun encode(art: String, block: (DataOutputStream) -> Unit): String {
        val b = ByteArrayOutputStream(); DataOutputStream(b).use { it.writeInt(if(art=="wert") 2 else 1); text(it,art); block(it) }
        require(b.size() <= 16_000_000); return Base64.getUrlEncoder().withoutPadding().encodeToString(b.toByteArray())
    }
    private fun <T> decode(text: String, art: String, block: (DataInputStream,Int) -> T): T {
        require(text.length <= 24_000_000)
        return DataInputStream(ByteArrayInputStream(Base64.getUrlDecoder().decode(text))).use {
            val version=it.readInt()
            val erwartet = if (art == "wert") 2 else 1
            require(version == erwartet && text(it) == art) { "Nicht unterstütztes Übersetzungsdatenformat $version für '$art'; erwartet wird Version $erwartet." }
            block(it,version).also { _ -> require(it.available()==0) { "Unerwartete Restdaten." } }
        }
    }
    fun kodieren(z: ÜbersetzungsZeugnis) = encode("zeugnis") { zeugnis(it,z,0) }
    fun dekodiereZeugnis(text: String) = decode(text,"zeugnis") { d,_ -> zeugnis(d,0) }
    fun kodieren(p: StrukturRückführungsplan) = encode("plan") { plan(it,p,0) }
    fun dekodierePlan(text: String) = decode(text,"plan") { d,_ -> plan(d,0) }
    fun kodieren(w: ÜbertragenerMathematischerWert) = encode("wert") { wert(it,w.wert,0); metadaten(it,w.übersetzungen); it.writeBoolean(w.formel!=null);w.formel?.let { f -> text(it,FormelKontextCodec.kodieren(f)) } }
    fun dekodiereWert(text: String): ÜbertragenerMathematischerWert = decode(text,"wert") { d,version ->
        val w = wert(d,0); val m = metadaten(d)
        require(version == 2)
        val f=if(d.readBoolean()) FormelKontextCodec.dekodieren(text(d)) else null
        require(m.aktuell == null || m.aktuell.ziel == ÜbersetzungsStruktur.von(w)) { "Zeugnis und Ergebnisstruktur widersprechen sich." }
        ÜbertragenerMathematischerWert(w,m,f)
    }
}

/** Unbekannte Regeln bleiben lesbar; ihre Behauptungen werden nicht als Nachweis benutzt. */
fun ÜbersetzungsZeugnis.prüfeÜbertragung(): ÜbersetzungsZeugnis {
    val zielId = regelId.removePrefix("uebersetzung.").substringBefore(':')
    val bekannt = regelId.startsWith("uebersetzung.") && ÜbersetzungsZiel.entries.any { it.id == zielId } && regelVersion == 1 && (':' !in regelId || zielId == "indexmethode")
    if (!bekannt || rückführungsplan?.version?.let { it != 1 } == true) return copy(
        injektiv = NachweisStatus.Unvollstaendig, surjektivAufTräger = NachweisStatus.Unvollstaendig,
        informationserhaltung = NachweisStatus.Unvollstaendig, genauigkeit = ÜbersetzungsGenauigkeit.OFFEN,
        diagnose = (diagnose + "Unbekannte Regel-/Planversion; gespeicherte Eigenschaften gelten nicht als nachgewiesen.").distinct(),
        ungeprüfteBehauptungen = ungeprüfteBehauptungen ?: ÜbersetzungsBehauptungen(injektiv,surjektivAufTräger,informationserhaltung,genauigkeit),
    )
    val quellIds = quellKontext.quellen.map { it.id }.toSet()
    require(quellKontext.quellen.all { it.id.isNotBlank() && it.name.isNotBlank() && it.komponentenPfad.all { i -> i > 0 } }) { "Ungültige Quellenreferenz im Zeugnis." }
    require(identitätsZuordnung.keys.all { it in quellIds } && identitätsZuordnung.values.all { it.isNotBlank() }) { "Unbekannte Identitätsreferenz im Zeugnis." }
    require(quellKontext.argumentReihenfolge.distinct().size == quellKontext.argumentReihenfolge.size) { "Doppelte Argumentidentität im Zeugnis." }
    if (rückführungsplan != null) {
        require(rückführungsplan.quelle == if (zielId == "rueckfuehrung") ziel else quelle) { "Quellstruktur und Rückführungsplan widersprechen sich." }
        val nodes = linkedMapOf<List<Int>, ÜbersetzungsStruktur>()
        fun besuche(f: ÜbersetzungsStruktur, p: List<Int>) { nodes[p]=f; f.kinder.forEachIndexed { i,k -> besuche(k,p+(i+1)) } }
        val planQuelle = if(zielId == "rueckfuehrung") ziel else quelle
        val planZiel = if(zielId == "rueckfuehrung") quelle else ziel
        besuche(planQuelle,emptyList())
        require(planZiel.art in setOf(StrukturArt.TUPEL,StrukturArt.ZEILE,StrukturArt.SPALTE))
        require(rückführungsplan.quellPfade.map { nodes[it] ?: error("Ungültiger Rückführungspfad.") } == planZiel.kinder) { "Zielstruktur und Rückführungspfade widersprechen sich." }
    }
    val regel = ÜbersetzungsZiel.vonId(zielId)
    val offen = NachweisStatus.Unvollstaendig
    var inj: NachweisStatus = offen
    var info: NachweisStatus = offen
    var surj: NachweisStatus = offen
    var genau = ÜbersetzungsGenauigkeit.OFFEN
    when (regel) {
        ÜbersetzungsZiel.TUPEL, ÜbersetzungsZiel.ZEILE, ÜbersetzungsZiel.SPALTE -> {
            require(rückführungsplan != null && quelle.art in setOf(StrukturArt.TUPEL, StrukturArt.ZEILE, StrukturArt.SPALTE))
            val art = when(regel) { ÜbersetzungsZiel.TUPEL -> StrukturArt.TUPEL; ÜbersetzungsZiel.ZEILE -> StrukturArt.ZEILE; else -> StrukturArt.SPALTE }
            require(ziel.art == art && ziel.kinder == quelle.kinder)
            require(ziel.art == StrukturArt.TUPEL || ziel.kinder.isNotEmpty() && ziel.kinder.all { it.art == StrukturArt.BLATT && it.blattTyp == "zahl" })
            require(rückführungsplan.quellPfade == quelle.kinder.indices.map { listOf(it+1) })
            inj = NachweisStatus.Nachgewiesen; info = NachweisStatus.Nachgewiesen; genau = ÜbersetzungsGenauigkeit.EXAKT
            // Recompute, never trust a serialized surjectivity claim on a restricted subspace.
            val komponenten = quellKontext.komponentenRäume ?: when(val r = quellKontext.werteBereich ?: quellKontext.definitionsRaum) {
                is Tupelraum -> r.komponenten; is Vektorraum -> List(r.dimension) { r.skalarMenge }; else -> null
            }
            val räume = listOfNotNull(quellKontext.werteBereich, quellKontext.definitionsRaum).distinct()
            if(komponenten != null && komponenten.distinct().size <= 1 && räume.size <= 1 &&
                (räume.singleOrNull() == Tupelraum(komponenten) || räume.singleOrNull() is Vektorraum && quellKontext.komponentenRäume == null)) surj = NachweisStatus.Nachgewiesen
        }
        ÜbersetzungsZiel.ABFLACHEN -> {
            require(quelle.art == StrukturArt.TUPEL && ziel.art == StrukturArt.TUPEL && rückführungsplan != null)
            val eineEbene = abflachenEineEbene(parameter)
            val pfade = mutableListOf<List<Int>>()
            fun öffne(f: ÜbersetzungsStruktur, p: List<Int>) {
                if(f.art == StrukturArt.TUPEL) f.kinder.forEachIndexed { i,k -> öffne(k,p+(i+1)) } else pfade += p
            }
            quelle.kinder.forEachIndexed { i,f -> val p = listOf(i+1)
                if(!eineEbene) öffne(f,p)
                else if(f.art in setOf(StrukturArt.TUPEL,StrukturArt.ZEILE,StrukturArt.SPALTE)) f.kinder.indices.forEach { pfade += p+(it+1) }
                else pfade += p
            }
            require(rückführungsplan.quellPfade == pfade) { "Der Plan passt nicht zur Abflachungsregel." }
            info = NachweisStatus.Nachgewiesen; genau = ÜbersetzungsGenauigkeit.EXAKT
        }
        ÜbersetzungsZiel.RÜCKFÜHRUNG -> {
            require(rückführungsplan != null && parameter["rueckfuehrung.regel"]?.substringBefore(':') in setOf("uebersetzung.tupel","uebersetzung.zeile","uebersetzung.spalte","uebersetzung.abflachen"))
            inj=NachweisStatus.Nachgewiesen; info=NachweisStatus.Nachgewiesen; genau=ÜbersetzungsGenauigkeit.EXAKT
        }
        ÜbersetzungsZiel.GRAD -> { genau=ÜbersetzungsGenauigkeit.EXAKT
            if(quellKontext.werteBereich==ReelleZahlen || quellKontext.definitionsRaum==ReelleZahlen) { inj=NachweisStatus.Widerlegt; info=NachweisStatus.Widerlegt }
        }
        ÜbersetzungsZiel.BOGENMASS -> { genau=ÜbersetzungsGenauigkeit.EXAKT
            if(quelle.blattTyp=="winkel") { inj=NachweisStatus.Nachgewiesen; info=NachweisStatus.Nachgewiesen }
        }
        ÜbersetzungsZiel.POLAR, ÜbersetzungsZiel.KARTESISCH -> genau=ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT
        ÜbersetzungsZiel.MITTELPUNKT, ÜbersetzungsZiel.OBJEKTPUNKT -> info=NachweisStatus.Widerlegt
        ÜbersetzungsZiel.METHODE -> if(ziel.blattTyp=="methode") genau=ÜbersetzungsGenauigkeit.EXAKT
        ÜbersetzungsZiel.INDEXMETHODE, ÜbersetzungsZiel.MATRIXZERLEGEN -> { genau=ÜbersetzungsGenauigkeit.EXAKT
            val variante=parameter["indexmethode.baustein"] ?: parameter["indexmethode.auswahl"] ?: parameter["indexmethode.achsenModus"]
            if(variante in setOf("schnitt","spur","nichtnullIndikator")) info=NachweisStatus.Unvollstaendig
        }
        else -> Unit
    }
    return copy(injektiv=inj, surjektivAufTräger=surj, informationserhaltung=info, genauigkeit=genau)
}
