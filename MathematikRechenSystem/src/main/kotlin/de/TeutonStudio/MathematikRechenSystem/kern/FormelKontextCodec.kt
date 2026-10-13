package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.*
import java.io.*
import java.util.Base64

/** Gemeinsame versionierte Persistenz von AST, Quellen, Bindungen, Typen und Annahmen. */
object FormelKontextCodec {
    fun kodieren(formel: GebundeneFormel): String {
        require(FormelBindungsPruefer.pruefe(formel) == FormelBindungsPruefung.Gueltig) { "Ungültiger Formelkontext." }
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { d ->
            d.writeInt(1)
            text(d, StrukturFormelCodec.kodieren(formel.wurzel))
            d.writeInt(formel.quellen.size)
            formel.quellen.forEach { q ->
                text(d, q.id); text(d, q.anzeigeName); text(d, q.art.name); text(d, q.typ.name)
                d.writeBoolean(q.werteVorrat != null)
                q.werteVorrat?.let { text(d, MathematischerWertCodec.kodieren(it)) }
                typ(d, q.vollstaendigerTyp)
                text(d, q.rolle.name)
                d.writeBoolean(q.methodenSignatur != null)
                q.methodenSignatur?.let { signatur(d, it) }
            }
            d.writeInt(formel.variablenVorkommen.size)
            formel.variablenVorkommen.forEach { (v, q) -> text(d, v); text(d, q) }
            d.writeInt(formel.quantoren.size)
            formel.quantoren.forEach { q ->
                text(d, q.bindungsId); text(d, q.quantor.name); text(d, q.quelleId); text(d, q.geltungsbereichAusdruckId)
            }
            d.writeInt(formel.praedikate.size)
            formel.praedikate.forEach { p ->
                text(d, p.id); text(d, p.name); text(d, p.zielTyp.name)
                d.writeInt(p.argumentQuellenIds.size); p.argumentQuellenIds.forEach { text(d, it) }
            }
            d.writeInt(formel.annahmen.size)
            formel.annahmen.forEach { text(d, MathematischerWertCodec.kodieren(it)) }
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray())
    }

    fun dekodieren(kodiert: String): GebundeneFormel {
        require(kodiert.length <= 32_000_000)
        return DataInputStream(ByteArrayInputStream(Base64.getUrlDecoder().decode(kodiert))).use { d ->
            require(d.readInt() == 1) { "Unbekannte Formelkontext-Version." }
            val wurzel = StrukturFormelCodec.dekodieren(text(d))
            val quellen = List(anzahl(d)) {
                val id = text(d); val name = text(d)
                val art = FormelVariablenArt.valueOf(text(d)); val grob = FormelTyp.valueOf(text(d))
                val raum = if (d.readBoolean()) MathematischerWertCodec.dekodieren(text(d)) as MengenAusdruck else null
                val typ = typ(d); val rolle = FormelSymbolRolle.valueOf(text(d))
                val signatur = if (d.readBoolean()) signatur(d) else null
                FormelVariablenQuelle(id, name, art, grob, raum, typ, rolle, signatur)
            }
            val vorkommen = List(anzahl(d)) { text(d) to text(d) }
            require(vorkommen.map { it.first }.distinct().size == vorkommen.size) { "Doppelte Vorkommensbindung." }
            val quantoren = List(anzahl(d)) { FormelQuantorBindung(text(d), FormelQuantorArt.valueOf(text(d)), text(d), text(d)) }
            val praedikate = List(anzahl(d)) {
                val id = text(d); val name = text(d); val typ = FormelTyp.valueOf(text(d))
                FormelPraedikatsVertrag(id, name, List(anzahl(d)) { text(d) }, typ)
            }
            val annahmen = List(anzahl(d)) { MathematischerWertCodec.dekodieren(text(d)) as Aussage }.toSet()
            require(d.available() == 0) { "Unerwartete Formelkontext-Restdaten." }
            GebundeneFormel(wurzel, quellen, vorkommen.toMap(), quantoren, praedikate, annahmen).also {
                require(FormelBindungsPruefer.pruefe(it) == FormelBindungsPruefung.Gueltig) { "Ungültiger geladener Formelkontext." }
            }
        }
    }

    internal fun text(d: DataOutputStream, s: String) {
        val b = s.toByteArray(Charsets.UTF_8); require(b.size <= 16_000_000)
        d.writeInt(b.size); d.write(b)
    }
    internal fun text(d: DataInputStream): String {
        val n = d.readInt(); require(n in 0..16_000_000 && n <= d.available()) { "Ungültige Textlänge." }
        return ByteArray(n).also(d::readFully).toString(Charsets.UTF_8)
    }
    internal fun anzahl(d: DataInputStream): Int = d.readInt().also { require(it in 0..100_000) { "Ungültige Komponentenanzahl." } }

    internal fun signatur(d: DataOutputStream, s: MethodenSignatur) {
        listOf(s.argumente, s.ergebnisse).forEach { komponenten ->
            d.writeInt(komponenten.size)
            komponenten.forEach { k -> text(d, k.id); text(d, k.name); d.writeInt(k.position); typ(d, k.typ) }
        }
    }
    internal fun signatur(d: DataInputStream): MethodenSignatur {
        fun komponenten() = List(anzahl(d)) { MethodenKomponente(text(d), text(d), d.readInt(), typ(d)) }
        return MethodenSignatur(komponenten(), komponenten())
    }
    private fun typ(d: DataOutputStream, t: TypAusdruck, tiefe: Int = 0) {
        require(tiefe <= 128)
        when (t) {
            TypAusdruck.Beliebig -> d.writeByte(0)
            TypAusdruck.Unbekannt -> d.writeByte(1)
            is TypAusdruck.Atom -> { d.writeByte(2); text(d, t.id.wert) }
            is TypAusdruck.Literal -> { d.writeByte(3); text(d, t.wert) }
            is TypAusdruck.Variable -> { d.writeByte(4); text(d, t.id.wert) }
            is TypAusdruck.Parameterisiert -> {
                d.writeByte(5); text(d, t.konstruktor.wert); d.writeInt(t.argumente.size)
                t.argumente.forEach { typ(d, it, tiefe + 1) }
            }
            is TypAusdruck.Vereinigung -> {
                d.writeByte(6); d.writeInt(t.alternativen.size); t.alternativen.forEach { typ(d, it, tiefe + 1) }
            }
        }
    }
    private fun typ(d: DataInputStream, tiefe: Int = 0): TypAusdruck {
        require(tiefe <= 128)
        return when (val art = d.readByte().toInt()) {
            0 -> TypAusdruck.Beliebig
            1 -> TypAusdruck.Unbekannt
            2 -> TypAusdruck.Atom(TypId(text(d)))
            3 -> TypAusdruck.Literal(text(d))
            4 -> TypAusdruck.Variable(TypVariablenId(text(d)))
            5 -> TypAusdruck.Parameterisiert(TypId(text(d)), List(anzahl(d)) { typ(d, tiefe + 1) })
            6 -> TypAusdruck.Vereinigung(List(anzahl(d)) { typ(d, tiefe + 1) })
            else -> error("Unbekannter Typausdruck $art.")
        }
    }
}

/** Der Text ist die Darstellung, der Kontext die verlustfreie semantische Quelle. */
data class FormelLatexAustausch(val latex: String, val kontext: String)

fun FormelLatexCodec.exportiere(formel: GebundeneFormel): FormelLatexAustausch =
    FormelLatexAustausch(exportiere(formel.wurzel), FormelKontextCodec.kodieren(formel))

fun FormelLatexCodec.importiere(austausch: FormelLatexAustausch): GebundeneFormel =
    FormelKontextCodec.dekodieren(austausch.kontext).also {
        require(exportiere(it.wurzel) == austausch.latex) { "Formeltext und semantischer Kontext widersprechen sich." }
    }

fun FormelLatexCodec.importiereGebunden(latex: String, quellen: List<FormelVariablenQuelle>): GebundeneFormel {
    val import = importiere(latex, quellen)
    require(import is FormelLatexImportErgebnis.Erfolg) { (import as FormelLatexImportErgebnis.Fehler).nachricht }
    val vorkommen = linkedMapOf<String, String>()
    fun besuche(e: FormelAusdruck) {
        when (e) {
            is FormelAusdruck.Variable -> {
                val q = quellen.singleOrNull { it.anzeigeName == e.name } ?: error("Keine eindeutige Deklaration für '${e.name}'.")
                vorkommen[e.id] = q.id
            }
            is FormelAusdruck.Operation -> e.argumente.forEach { besuche(it.ausdruck) }
            else -> Unit
        }
    }
    besuche(import.ausdruck)
    return GebundeneFormel(import.ausdruck, quellen, vorkommen).also {
        require(FormelBindungsPruefer.pruefe(it) == FormelBindungsPruefung.Gueltig)
    }
}
