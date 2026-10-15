package de.TeutonStudio.MathematikRechenSystem.kern

import java.io.*
import java.util.Base64

/** Versionierter, explizit typisierter Wertcodec. Unbekannte Varianten werden nie als LaTeX ersetzt. */
object MathematischerWertCodec {
    fun kodieren(z: ÜbersetzungsZeugnis) = ÜbersetzungsCodec.kodieren(z)
    fun dekodierenZeugnis(text: String) = ÜbersetzungsCodec.dekodiereZeugnis(text)
    fun kodieren(p: StrukturRückführungsplan) = ÜbersetzungsCodec.kodieren(p)
    fun dekodierenRückführungsplan(text: String) = ÜbersetzungsCodec.dekodierePlan(text)
    fun kodieren(w: ÜbertragenerMathematischerWert) = ÜbersetzungsCodec.kodieren(w)
    fun dekodierenÜbertragenenWert(text: String) = ÜbersetzungsCodec.dekodiereWert(text)
    internal fun schreibeIntern(d: DataOutputStream, w: MathematischesObjekt, tiefe: Int) = wert(d,w,tiefe)
    internal fun liesIntern(d: DataInputStream, tiefe: Int): MathematischesObjekt = wert(d,tiefe)

    private const val VERSION = 3
    private const val MAX_ELEMENTE = 100_000
    private const val MAX_TIEFE = 128

    fun kodieren(wert: MathematischesObjekt): String = Base64.getUrlEncoder().withoutPadding().encodeToString(
        ByteArrayOutputStream().also { ausgabe -> DataOutputStream(ausgabe).use { d ->
            d.writeInt(VERSION)
            wert(d, wert, 0)
        } }.toByteArray(),
    )

    fun dekodieren(text: String): MathematischesObjekt {
        require(text.length <= 16_000_000) { "Wertdaten überschreiten die Größenbegrenzung." }
        return DataInputStream(ByteArrayInputStream(Base64.getUrlDecoder().decode(text))).use { d ->
            val version = d.readInt()
            require(version == VERSION) { "Nicht unterstützte mathematische Wertversion $version; erwartet wird Version $VERSION." }
            wert(d, 0, version).also { require(d.available() == 0) { "Unerwartete Restdaten im mathematischen Wert." } }
        }
    }

    private fun anzahl(d: DataInputStream): Int = d.readInt().also {
        require(it in 0..MAX_ELEMENTE) { "Ungültige Komponentenanzahl $it." }
    }
    private fun liste(d: DataOutputStream, werte: List<MathematischesObjekt>, tiefe: Int) {
        require(werte.size <= MAX_ELEMENTE)
        d.writeInt(werte.size)
        werte.forEach { wert(d, it, tiefe + 1) }
    }
    private fun liste(d: DataInputStream, tiefe: Int, version: Int): List<MathematischesObjekt> =
        List(anzahl(d)) { wert(d, tiefe + 1, version) }

    private fun wert(d: DataOutputStream, w: MathematischesObjekt, tiefe: Int) {
        require(tiefe <= MAX_TIEFE) { "Mathematischer Wert ist zu tief verschachtelt." }
        when (w) {
            is RationaleZahl -> { d.writeUTF("RationaleZahl"); d.writeUTF(w.zähler.toString()); d.writeUTF(w.nenner.toString()) }
            is Variable -> { d.writeUTF("Variable"); d.writeUTF(w.name); d.writeUTF(w.latex); d.writeBoolean(w.bindungsId != null); w.bindungsId?.let(d::writeUTF) }
            is AllgemeinerParameter -> { d.writeUTF("AllgemeinerParameter"); d.writeUTF(w.name); d.writeUTF(w.latex); d.writeBoolean(w.bindungsId != null); w.bindungsId?.let(d::writeUTF) }
            is MathematischeKonstante -> { d.writeUTF("MathematischeKonstante"); d.writeUTF(w.name); d.writeUTF(w.latex) }
            is BenannteMenge -> { d.writeUTF("BenannteMenge"); d.writeUTF(w.name); d.writeUTF(w.latex) }
            is Tupel -> { d.writeUTF("Tupel"); liste(d, w.elemente, tiefe) }
            is SpaltenVektor -> { d.writeUTF("SpaltenVektor"); liste(d, w.werte, tiefe) }
            is ZeilenVektor -> { d.writeUTF("ZeilenVektor"); liste(d, w.werte, tiefe) }
            is Tupelraum -> { d.writeUTF("Tupelraum"); liste(d, w.komponenten, tiefe) }
            is KartesischerPotenzraum -> { d.writeUTF("KartesischerPotenzraum"); wert(d, w.grundmenge, tiefe + 1); wert(d, w.dimension, tiefe + 1) }
            is KartesischesProdukt -> { d.writeUTF("KartesischesProdukt"); liste(d, w.mengen, tiefe) }
            is Vereinigung -> { d.writeUTF("Vereinigung"); liste(d, w.mengen, tiefe) }
            is Konjunktion -> { d.writeUTF("Konjunktion"); liste(d, w.aussagen, tiefe) }
            is Disjunktion -> { d.writeUTF("Disjunktion"); liste(d, w.aussagen, tiefe) }
            is EndlicheMenge -> { d.writeUTF("EndlicheMenge"); liste(d, w.elemente.toList(), tiefe) }
            is Addition -> { d.writeUTF("Addition"); liste(d, w.summanden, tiefe) }
            is Multiplikation -> { d.writeUTF("Multiplikation"); liste(d, w.faktoren, tiefe) }
            is Division -> { d.writeUTF("Division"); wert(d, w.dividend, tiefe + 1); wert(d, w.divisor, tiefe + 1) }
            is Potenz -> { d.writeUTF("Potenz"); wert(d, w.basis, tiefe + 1); wert(d, w.exponent, tiefe + 1) }
            is KomplexeZahl -> { d.writeUTF("KomplexeZahl"); wert(d, w.realteil, tiefe + 1); wert(d, w.imaginärteil, tiefe + 1) }
            is Logarithmus -> { d.writeUTF("Logarithmus"); wert(d, w.basis, tiefe + 1); wert(d, w.argument, tiefe + 1) }
            is Gleichheit -> { d.writeUTF("Gleichheit"); wert(d, w.links, tiefe + 1); wert(d, w.rechts, tiefe + 1) }
            is Selbigkeit -> { d.writeUTF("Selbigkeit"); wert(d, w.links, tiefe + 1); wert(d, w.rechts, tiefe + 1) }
            is Ungleichheit -> { d.writeUTF("Ungleichheit"); wert(d, w.links, tiefe + 1); wert(d, w.rechts, tiefe + 1) }
            is Äquivalenz -> { d.writeUTF("Äquivalenz"); wert(d, w.links, tiefe + 1); wert(d, w.rechts, tiefe + 1) }
            is Adjunktion -> { d.writeUTF("Adjunktion"); wert(d, w.links, tiefe + 1); wert(d, w.rechts, tiefe + 1) }
            is Implikation -> { d.writeUTF("Implikation"); wert(d, w.voraussetzung, tiefe + 1); wert(d, w.folgerung, tiefe + 1) }
            is MengenDifferenz -> { d.writeUTF("MengenDifferenz"); wert(d, w.links, tiefe + 1); wert(d, w.rechts, tiefe + 1) }
            is TeilmengenBeziehung -> { d.writeUTF("TeilmengenBeziehung"); wert(d, w.links, tiefe + 1); wert(d, w.rechts, tiefe + 1) }
            is Sinus -> { d.writeUTF("Sinus"); wert(d, w.argument, tiefe + 1) }
            is Cosinus -> { d.writeUTF("Cosinus"); wert(d, w.argument, tiefe + 1) }
            is ArcSinus -> { d.writeUTF("ArcSinus"); wert(d, w.argument, tiefe + 1) }
            is ArcCosinus -> { d.writeUTF("ArcCosinus"); wert(d, w.argument, tiefe + 1) }
            is Exponentialfunktion -> { d.writeUTF("Exponentialfunktion"); wert(d, w.argument, tiefe + 1) }
            is NatürlicherLogarithmus -> { d.writeUTF("NatürlicherLogarithmus"); wert(d, w.argument, tiefe + 1) }
            is Wurzel -> { d.writeUTF("Wurzel"); wert(d, w.argument, tiefe + 1) }
            is Betrag -> { d.writeUTF("Betrag"); wert(d, w.argument, tiefe + 1) }
            is Konjugierte -> { d.writeUTF("Konjugierte"); wert(d, w.argument, tiefe + 1) }
            is Negation -> { d.writeUTF("Negation"); wert(d, w.aussage, tiefe + 1) }
            is Potenzmenge -> { d.writeUTF("Potenzmenge"); wert(d, w.grundMenge, tiefe + 1) }
            is Folgenraum -> { d.writeUTF("Folgenraum"); wert(d, w.elementMenge, tiefe + 1) }
            is GradWinkel -> { d.writeUTF("GradWinkel"); wert(d, w.grad, tiefe + 1) }
            is WahrheitsKonstante -> { d.writeUTF("WahrheitsKonstante"); d.writeBoolean(w.wert) }
            is ElementBeziehung -> { d.writeUTF("ElementBeziehung"); wert(d, w.element, tiefe + 1); wert(d, w.menge, tiefe + 1) }
            is Vergleich -> { d.writeUTF("Vergleich"); wert(d, w.links, tiefe + 1); d.writeUTF(w.art.name); wert(d, w.rechts, tiefe + 1) }
            is ReellesIntervall -> { d.writeUTF("ReellesIntervall"); wert(d, w.links, tiefe + 1); d.writeBoolean(w.linksOffen); wert(d, w.rechts, tiefe + 1); d.writeBoolean(w.rechtsOffen) }
            is Schnitt -> { d.writeUTF("Schnitt"); liste(d, w.mengen, tiefe); d.writeBoolean(w.grundMenge != null); w.grundMenge?.let { wert(d, it, tiefe + 1) } }
            is Matrix -> { d.writeUTF("Matrix"); d.writeInt(w.zeilen.size); w.zeilen.forEach { liste(d, it, tiefe) } }
            is Tensor -> { d.writeUTF("Tensor"); d.writeInt(w.dimensionen.size); w.dimensionen.forEach(d::writeInt); liste(d, w.werte, tiefe) }
            is Vektorraum -> { d.writeUTF("Vektorraum"); d.writeUTF(w.orientierung.name); d.writeInt(w.dimension); wert(d, w.skalarMenge, tiefe + 1) }
            is Matrizenraum -> { d.writeUTF("Matrizenraum"); d.writeInt(w.zeilen); d.writeInt(w.spalten); wert(d, w.skalarMenge, tiefe + 1) }
            is DefinierteMenge -> { d.writeUTF("DefinierteMenge"); d.writeInt(w.variablen.size); w.variablen.forEach { wert(d, it.variable, tiefe + 1); wert(d, it.grundMenge, tiefe + 1) }; wert(d, w.bedingung, tiefe + 1) }
            is MethodenVariable -> {
                d.writeUTF("MethodenVariable"); d.writeUTF(w.name); d.writeUTF(w.bindungsId)
                FormelKontextCodec.signatur(d, w.signatur)
            }
            is StrukturellerMethodenAufruf -> {
                d.writeUTF("MethodenAufruf"); wert(d, w.methode, tiefe + 1); wert(d, w.argumente, tiefe + 1)
            }
            is MethodenZahlKomponente -> {
                d.writeUTF("MethodenZahlKomponente"); wert(d, w.aufruf, tiefe + 1); d.writeInt(w.index)
            }
            is MethodenWertKomponente -> {
                d.writeUTF("MethodenWertKomponente"); wert(d, w.aufruf, tiefe + 1); d.writeInt(w.index)
                FormelKontextCodec.signatur(d, w.signatur)
            }
            is Abbildungsmenge -> {
                d.writeUTF("Abbildungsmenge"); wert(d, w.zielMenge, tiefe + 1); wert(d, w.definitionsMenge, tiefe + 1)
            }
            is MathematischeMethode -> {
                require(w.bereichsanpassung == null) { "Bereichsanpassungs-Herkunft benötigt einen eigenen Codec." }
                d.writeUTF("MathematischeMethode"); d.writeUTF(w.name)
                liste(d, w.parameter, tiefe); wert(d, w.vorschrift, tiefe + 1); wert(d, w.zielMenge, tiefe + 1)
                d.writeInt(w.werteVorräte.size); w.werteVorräte.forEach { (k, v) -> d.writeUTF(k); wert(d, v, tiefe + 1) }
                d.writeInt(w.ausgabeNamen.size); w.ausgabeNamen.forEach(d::writeUTF)
                d.writeBoolean(w.effektiverWerteVorrat != null); w.effektiverWerteVorrat?.let { wert(d, it, tiefe + 1) }
                listOf(w.argumentIdentitäten, w.ergebnisIdentitäten).forEach { m ->
                    d.writeInt(m.size); m.forEach { (k, v) -> d.writeUTF(k); d.writeUTF(v) }
                }
                liste(d, w.voraussetzungen.toList(), tiefe)
                ÜbersetzungsCodec.metadaten(d, w.übersetzungen, tiefe + 1)
            }
            is Tensorraum -> { d.writeUTF("Tensorraum"); wert(d, w.elementMenge, tiefe + 1); liste(d, w.dimensionen, tiefe) }
            is EndlicheIndexMenge -> { d.writeUTF("EndlicheIndexMenge"); wert(d, w.obergrenze, tiefe + 1) }
            is FehlendeObermenge -> { d.writeUTF("FehlendeObermenge"); d.writeUTF(w.anschlussArt) }
            is OffeneDefinitionsBedingung -> { d.writeUTF("OffeneDefinitionsBedingung"); wert(d, w.objekt, tiefe + 1) }
            is ÜbersetzungsBildraum -> { d.writeUTF("ÜbersetzungsBildraum"); ÜbersetzungsCodec.bild(d,w,tiefe + 1) }
            is WinkelParameter -> { d.writeUTF("WinkelParameter"); d.writeUTF(w.name) }
            is WinkelAusBogenmass -> { d.writeUTF("WinkelAusBogenmass"); wert(d,w.argument,tiefe+1) }
            is WinkelBogenmass -> { d.writeUTF("WinkelBogenmass"); wert(d,w.winkel,tiefe+1); d.writeUTF(w.vertreter.name) }
            is SinusGrad -> { d.writeUTF("SinusGrad"); wert(d,w.winkel,tiefe+1) }
            is StrukturAchsenLaenge -> { d.writeUTF("StrukturAchsenLaenge"); wert(d,w.quelle,tiefe+1); d.writeInt(w.achse) }
            is IndexKomponente -> { d.writeUTF("IndexKomponente"); wert(d,w.quelle,tiefe+1); liste(d,w.indizes,tiefe) }
            is SymbolischeZahlKomponente -> { d.writeUTF("SymbolischeZahlKomponente"); wert(d,w.quelle,tiefe+1); wert(d,w.index,tiefe+1); liste(d,w.weitereIndizes,tiefe) }
            is EuklidischerRaum -> { d.writeUTF("EuklidischerRaum"); d.writeUTF(w.id); d.writeInt(w.dimension); d.writeUTF(w.axiomSystem.name) }
            is GeometrischesKoordinatensystem -> { d.writeUTF("GeometrischesKoordinatensystem"); wert(d,w.raum,tiefe+1); d.writeUTF(w.name) }
            is GeometrieDreieck -> { d.writeUTF("GeometrieDreieck"); liste(d,listOf(w.a,w.b,w.c,w.alpha,w.beta,w.gamma),tiefe); wert(d,w.raum,tiefe+1) }
            is TransformiertesGeometrieObjekt -> { d.writeUTF("TransformiertesGeometrieObjekt"); wert(d,w.original,tiefe+1); wert(d,w.struktur,tiefe+1); wert(d,w.raum,tiefe+1) }
            is GeometrieStruktur -> {
                d.writeUTF("GeometrieStruktur"); wert(d,w.raum,tiefe+1); d.writeUTF(w.darstellungsArt.name)
                d.writeInt(w.stufen.size); w.stufen.forEach { stufe ->
                    d.writeInt(stufe.dimension); d.writeInt(stufe.zellen.size); stufe.zellen.forEach { zelle ->
                        d.writeUTF(zelle.id); d.writeInt(zelle.dimension); d.writeInt(zelle.rand.size)
                        zelle.rand.forEach { d.writeUTF(it.zellId); d.writeInt(it.orientierung) }
                        d.writeBoolean(zelle.geometrie != null); zelle.geometrie?.let { wert(d,it,tiefe+1) }
                    }
                }
            }
            is Maximum -> { d.writeUTF("Maximum"); liste(d,w.operanden,tiefe) }
            is Minimum -> { d.writeUTF("Minimum"); liste(d,w.operanden,tiefe) }
            is GeometriePunkt -> { d.writeUTF("GeometriePunkt"); d.writeUTF(w.name); wert(d,w.raum,tiefe+1); d.writeBoolean(w.koordinaten != null); w.koordinaten?.let { wert(d,it,tiefe+1) } }
            is GeometrieGerade -> { d.writeUTF("GeometrieGerade"); wert(d,w.a,tiefe+1); wert(d,w.b,tiefe+1) }
            is GeometrieEbene -> { d.writeUTF("GeometrieEbene"); wert(d,w.a,tiefe+1); wert(d,w.b,tiefe+1); wert(d,w.c,tiefe+1) }
            is GeometrieStrecke -> { d.writeUTF("GeometrieStrecke"); wert(d,w.anfang,tiefe+1); wert(d,w.ende,tiefe+1) }
            is GeometrieStrahl -> { d.writeUTF("GeometrieStrahl"); wert(d,w.ursprung,tiefe+1); wert(d,w.richtungsPunkt,tiefe+1) }
            is GeometrieKreislinie -> { d.writeUTF("GeometrieKreislinie"); wert(d,w.mittelpunkt,tiefe+1); wert(d,w.randpunkt,tiefe+1) }
            is GeometrieWinkel -> { d.writeUTF("GeometrieWinkel"); wert(d,w.a,tiefe+1); wert(d,w.scheitel,tiefe+1); wert(d,w.c,tiefe+1); d.writeBoolean(w.orientiert) }
            is GeometriePolygon -> { d.writeUTF("GeometriePolygon"); liste(d,w.ecken,tiefe) }
            is GeometrieGruppe -> { d.writeUTF("GeometrieGruppe"); liste(d,w.objekte,tiefe) }
            is GeometrischeTrägermenge -> { d.writeUTF("GeometrischeTrägermenge"); wert(d,w.objekt,tiefe+1) }
            is KoordinatenBild -> { d.writeUTF("KoordinatenBild"); wert(d,w.objekt,tiefe+1); wert(d,w.koordinatensystem,tiefe+1) }
            is GeometrischeMengenRealisierung -> { d.writeUTF("GeometrischeMengenRealisierung"); wert(d,w.menge,tiefe+1); wert(d,w.raum,tiefe+1) }
            is VerschobeneKoordinatenMenge -> { d.writeUTF("VerschobeneKoordinatenMenge"); wert(d,w.original,tiefe+1); liste(d,w.verschiebung,tiefe) }
            is SymbolischerStrukturZugriff -> { d.writeUTF("SymbolischerStrukturZugriff"); wert(d,w.quelle,tiefe+1); wert(d,w.index,tiefe+1); d.writeUTF(w.art.name); d.writeInt(w.achse); d.writeUTF(w.ergebnisAnschlussArt); d.writeBoolean(w.ergebnisForm != null); w.ergebnisForm?.let { liste(d,it,tiefe) } }
            is TypisiertesElement -> { d.writeUTF("TypisiertesElement"); d.writeUTF(w.name); d.writeUTF(w.anschlussArt); d.writeUTF(w.latex); d.writeBoolean(w.strukturForm != null); w.strukturForm?.let { liste(d,it,tiefe) } }
            is StrukturErgebnisMenge -> { d.writeUTF("StrukturErgebnisMenge"); d.writeUTF(w.anschlussArt); d.writeBoolean(w.form != null); w.form?.let { liste(d,it,tiefe) }; d.writeUTF(w.bezeichnung) }
            is UnterraumMenge -> { d.writeUTF("UnterraumMenge"); d.writeInt(w.dimension); d.writeInt(w.richtungen.size); w.richtungen.forEach { liste(d,it,tiefe) }; d.writeBoolean(w.stuetzpunkt != null); w.stuetzpunkt?.let { liste(d,it,tiefe) } }
            Gradmenge -> d.writeUTF("Gradmenge")
            LeereMenge -> d.writeUTF("LeereMenge")
            else -> error("Kein verlustfreier Wertcodec für ${w.javaClass.simpleName} registriert.")
        }
    }

    private fun wert(d: DataInputStream, tiefe: Int, version: Int = VERSION): MathematischesObjekt {
        require(tiefe <= MAX_TIEFE) { "Mathematischer Wert ist zu tief verschachtelt." }
        return when (val art = d.readUTF()) {
            "RationaleZahl" -> RationaleZahl.von(java.math.BigInteger(d.readUTF()), java.math.BigInteger(d.readUTF()))
            "Variable" -> Variable(d.readUTF(), d.readUTF(), if (d.readBoolean()) d.readUTF() else null)
            "AllgemeinerParameter" -> AllgemeinerParameter(d.readUTF(), d.readUTF(), if (d.readBoolean()) d.readUTF() else null)
            "MathematischeKonstante" -> MathematischeKonstante(d.readUTF(), d.readUTF())
            "BenannteMenge" -> BenannteMenge(d.readUTF(), d.readUTF())
            "Tupel" -> Tupel(liste(d, tiefe, version).map { it as MathematischesObjekt })
            "SpaltenVektor" -> SpaltenVektor(liste(d, tiefe, version).map { it as ZahlAusdruck })
            "ZeilenVektor" -> ZeilenVektor(liste(d, tiefe, version).map { it as ZahlAusdruck })
            "Tupelraum" -> Tupelraum(liste(d, tiefe, version).map { it as MengenAusdruck })
            "KartesischerPotenzraum" -> KartesischerPotenzraum(wert(d, tiefe + 1, version) as MengenAusdruck, wert(d, tiefe + 1, version) as ZahlAusdruck)
            "KartesischesProdukt" -> KartesischesProdukt(liste(d, tiefe, version).map { it as MengenAusdruck })
            "Vereinigung" -> Vereinigung(liste(d, tiefe, version).map { it as MengenAusdruck })
            "Konjunktion" -> Konjunktion(liste(d, tiefe, version).map { it as Aussage })
            "Disjunktion" -> Disjunktion(liste(d, tiefe, version).map { it as Aussage })
            "EndlicheMenge" -> EndlicheMenge(liste(d, tiefe, version).toSet())
            "Addition" -> Addition.roh(liste(d, tiefe, version).map { it as ZahlAusdruck })
            "Multiplikation" -> Multiplikation.roh(liste(d, tiefe, version).map { it as ZahlAusdruck })
            "Division" -> Division(wert(d, tiefe + 1, version) as ZahlAusdruck, wert(d, tiefe + 1, version) as ZahlAusdruck)
            "Potenz" -> Potenz(wert(d, tiefe + 1, version) as ZahlAusdruck, wert(d, tiefe + 1, version) as ZahlAusdruck)
            "KomplexeZahl" -> KomplexeZahl(wert(d, tiefe + 1, version) as ZahlAusdruck, wert(d, tiefe + 1, version) as ZahlAusdruck)
            "Logarithmus" -> Logarithmus(wert(d, tiefe + 1, version) as ZahlAusdruck, wert(d, tiefe + 1, version) as ZahlAusdruck)
            "Gleichheit" -> Gleichheit(wert(d, tiefe + 1, version) as MathematischesObjekt, wert(d, tiefe + 1, version) as MathematischesObjekt)
            "Selbigkeit" -> Selbigkeit(wert(d, tiefe + 1, version) as MathematischesObjekt, wert(d, tiefe + 1, version) as MathematischesObjekt)
            "Ungleichheit" -> Ungleichheit(wert(d, tiefe + 1, version) as MathematischesObjekt, wert(d, tiefe + 1, version) as MathematischesObjekt)
            "Äquivalenz" -> Äquivalenz(wert(d, tiefe + 1, version) as Aussage, wert(d, tiefe + 1, version) as Aussage)
            "Adjunktion" -> Adjunktion(wert(d, tiefe + 1, version) as Aussage, wert(d, tiefe + 1, version) as Aussage)
            "Implikation" -> Implikation(wert(d, tiefe + 1, version) as Aussage, wert(d, tiefe + 1, version) as Aussage)
            "MengenDifferenz" -> MengenDifferenz(wert(d, tiefe + 1, version) as MengenAusdruck, wert(d, tiefe + 1, version) as MengenAusdruck)
            "TeilmengenBeziehung" -> TeilmengenBeziehung(wert(d, tiefe + 1, version) as MengenAusdruck, wert(d, tiefe + 1, version) as MengenAusdruck)
            "Sinus" -> Sinus(wert(d, tiefe + 1, version) as ZahlAusdruck)
            "Cosinus" -> Cosinus(wert(d, tiefe + 1, version) as ZahlAusdruck)
            "ArcSinus" -> ArcSinus(wert(d, tiefe + 1, version) as ZahlAusdruck)
            "ArcCosinus" -> ArcCosinus(wert(d, tiefe + 1, version) as ZahlAusdruck)
            "Exponentialfunktion" -> Exponentialfunktion(wert(d, tiefe + 1, version) as ZahlAusdruck)
            "NatürlicherLogarithmus" -> NatürlicherLogarithmus(wert(d, tiefe + 1, version) as ZahlAusdruck)
            "Wurzel" -> Wurzel(wert(d, tiefe + 1, version) as ZahlAusdruck)
            "Betrag" -> Betrag(wert(d, tiefe + 1, version) as ZahlAusdruck)
            "Konjugierte" -> Konjugierte(wert(d, tiefe + 1, version) as ZahlAusdruck)
            "Negation" -> Negation(wert(d, tiefe + 1, version) as Aussage)
            "Potenzmenge" -> Potenzmenge(wert(d, tiefe + 1, version) as MengenAusdruck)
            "Folgenraum" -> Folgenraum(wert(d, tiefe + 1, version) as MengenAusdruck)
            "GradWinkel" -> GradWinkel(wert(d, tiefe + 1, version) as ZahlAusdruck)
            "WahrheitsKonstante" -> WahrheitsKonstante(d.readBoolean())
            "ElementBeziehung" -> ElementBeziehung(wert(d, tiefe + 1, version), wert(d, tiefe + 1, version) as MengenAusdruck)
            "Vergleich" -> Vergleich(wert(d, tiefe + 1, version) as ZahlAusdruck, VergleichsArt.valueOf(d.readUTF()), wert(d, tiefe + 1, version) as ZahlAusdruck)
            "ReellesIntervall" -> ReellesIntervall(wert(d, tiefe + 1, version) as ZahlAusdruck, d.readBoolean(), wert(d, tiefe + 1, version) as ZahlAusdruck, d.readBoolean())
            "Schnitt" -> Schnitt(liste(d, tiefe, version).map { it as MengenAusdruck }, if (d.readBoolean()) wert(d, tiefe + 1, version) as MengenAusdruck else null)
            "Matrix" -> Matrix(List(anzahl(d)) { liste(d, tiefe, version).map { it as ZahlAusdruck } })
            "Tensor" -> Tensor(List(anzahl(d)) { d.readInt() }, liste(d, tiefe, version).map { it as ZahlAusdruck })
            "Vektorraum" -> Vektorraum(VektorOrientierung.valueOf(d.readUTF()), d.readInt(), wert(d, tiefe + 1, version) as MengenAusdruck)
            "Matrizenraum" -> Matrizenraum(d.readInt(), d.readInt(), wert(d, tiefe + 1, version) as MengenAusdruck)
            "DefinierteMenge" -> DefinierteMenge(List(anzahl(d)) { GebundeneMengenVariable(wert(d, tiefe + 1, version) as Variable, wert(d, tiefe + 1, version) as MengenAusdruck) }, wert(d, tiefe + 1, version) as Aussage)
            "MethodenVariable" -> MethodenVariable(d.readUTF(), d.readUTF(), FormelKontextCodec.signatur(d))
            "MethodenAufruf" -> StrukturellerMethodenAufruf(wert(d, tiefe + 1, version), wert(d, tiefe + 1, version) as Tupel)
            "MethodenWertKomponente" -> MethodenWertKomponente(wert(d, tiefe + 1, version) as StrukturellerMethodenAufruf, d.readInt(), FormelKontextCodec.signatur(d))
            "MethodenZahlKomponente" -> MethodenZahlKomponente(wert(d, tiefe + 1, version) as StrukturellerMethodenAufruf, d.readInt())
            "Abbildungsmenge" -> Abbildungsmenge(wert(d, tiefe + 1, version) as MengenAusdruck, wert(d, tiefe + 1, version) as MengenAusdruck)
            "MathematischeMethode" -> {
                val name = d.readUTF(); val parameter = liste(d, tiefe, version).map { it as MethodenParameter }
                val vorschrift = wert(d, tiefe + 1, version); val ziel = wert(d, tiefe + 1, version) as MengenAusdruck
                val räume = List(anzahl(d)) { d.readUTF() to (wert(d, tiefe + 1, version) as MengenAusdruck) }.toMap()
                val ausgaben = List(anzahl(d)) { d.readUTF() }
                val domäne = if (d.readBoolean()) wert(d, tiefe + 1, version) as MengenAusdruck else null
                val argumentIds = List(anzahl(d)) { d.readUTF() to d.readUTF() }.toMap()
                val ergebnisIds = List(anzahl(d)) { d.readUTF() to d.readUTF() }.toMap()
                val bedingungen = liste(d, tiefe, version).map { it as Aussage }.toSet()
                val übersetzungen = ÜbersetzungsCodec.metadaten(d, tiefe + 1)
                MathematischeMethode(name, parameter, vorschrift, ziel, räume, ausgaben, domäne,
                    argumentIdentitäten = argumentIds, ergebnisIdentitäten = ergebnisIds, voraussetzungen = bedingungen, übersetzungen = übersetzungen)
            }
            "Tensorraum" -> Tensorraum(wert(d, tiefe + 1, version) as MengenAusdruck, liste(d, tiefe, version).map { it as ZahlAusdruck })
            "EndlicheIndexMenge" -> EndlicheIndexMenge(wert(d, tiefe + 1, version) as ZahlAusdruck)
            "FehlendeObermenge" -> FehlendeObermenge(d.readUTF())
            "OffeneDefinitionsBedingung" -> OffeneDefinitionsBedingung(wert(d, tiefe + 1, version))
            "ÜbersetzungsBildraum" -> ÜbersetzungsCodec.bild(d,tiefe + 1)
            "WinkelParameter" -> WinkelParameter(d.readUTF())
            "WinkelAusBogenmass" -> WinkelAusBogenmass(wert(d,tiefe+1,version) as ZahlAusdruck)
            "WinkelBogenmass" -> WinkelBogenmass(wert(d,tiefe+1,version) as WinkelAusdruck, WinkelVertreter.valueOf(d.readUTF()))
            "SinusGrad" -> SinusGrad(wert(d,tiefe+1,version) as WinkelAusdruck)
            "StrukturAchsenLaenge" -> StrukturAchsenLaenge(wert(d,tiefe+1,version) as MathematischesObjekt, d.readInt())
            "IndexKomponente" -> IndexKomponente(wert(d,tiefe+1,version) as MathematischesObjekt, liste(d,tiefe,version).map { it as ZahlAusdruck })
            "SymbolischeZahlKomponente" -> SymbolischeZahlKomponente(wert(d,tiefe+1,version) as MathematischesObjekt, wert(d,tiefe+1,version) as ZahlAusdruck, liste(d,tiefe,version).map { it as ZahlAusdruck })
            "EuklidischerRaum" -> EuklidischerRaum(d.readUTF(), d.readInt(), GeometrischesAxiomSystem.valueOf(d.readUTF()))
            "GeometrischesKoordinatensystem" -> GeometrischesKoordinatensystem(wert(d,tiefe+1,version) as EuklidischerRaum, d.readUTF())
            "GeometrieDreieck" -> { val a=liste(d,tiefe,version).map { it as ZahlAusdruck }; require(a.size==6); GeometrieDreieck(a[0],a[1],a[2],a[3],a[4],a[5],wert(d,tiefe+1,version) as EuklidischerRaum) }
            "TransformiertesGeometrieObjekt" -> TransformiertesGeometrieObjekt(wert(d,tiefe+1,version) as GeometrischerAusdruck,wert(d,tiefe+1,version) as GeometrieStruktur,wert(d,tiefe+1,version) as EuklidischerRaum)
            "GeometrieStruktur" -> {
                val raum=wert(d,tiefe+1,version) as EuklidischerRaum; val art=StrukturDarstellungsArt.valueOf(d.readUTF())
                GeometrieStruktur(raum,List(anzahl(d)) {
                    val dimension=d.readInt()
                    ZellStufe(dimension,List(anzahl(d)) {
                        val id=d.readUTF();val dim=d.readInt();val rand=List(anzahl(d)) { OrientierterZellVerweis(d.readUTF(),d.readInt()) }
                        GeometrischeZelle(id,dim,rand,if(d.readBoolean()) wert(d,tiefe+1,version) as GeometrischerAusdruck else null)
                    })
                },art)
            }
            "Maximum" -> Maximum.roh(liste(d,tiefe,version).map { it as ZahlAusdruck })
            "Minimum" -> Minimum.roh(liste(d,tiefe,version).map { it as ZahlAusdruck })
            "GeometriePunkt" -> GeometriePunkt(d.readUTF(), wert(d,tiefe+1,version) as EuklidischerRaum, if(d.readBoolean()) wert(d,tiefe+1,version) as Tupel else null)
            "GeometrieGerade" -> GeometrieGerade(wert(d,tiefe+1,version) as GeometriePunkt, wert(d,tiefe+1,version) as GeometriePunkt)
            "GeometrieEbene" -> GeometrieEbene(wert(d,tiefe+1,version) as GeometriePunkt, wert(d,tiefe+1,version) as GeometriePunkt, wert(d,tiefe+1,version) as GeometriePunkt)
            "GeometrieStrecke" -> GeometrieStrecke(wert(d,tiefe+1,version) as GeometriePunkt, wert(d,tiefe+1,version) as GeometriePunkt)
            "GeometrieStrahl" -> GeometrieStrahl(wert(d,tiefe+1,version) as GeometriePunkt, wert(d,tiefe+1,version) as GeometriePunkt)
            "GeometrieKreislinie" -> GeometrieKreislinie(wert(d,tiefe+1,version) as GeometriePunkt, wert(d,tiefe+1,version) as GeometriePunkt)
            "GeometrieWinkel" -> GeometrieWinkel(wert(d,tiefe+1,version) as GeometriePunkt, wert(d,tiefe+1,version) as GeometriePunkt, wert(d,tiefe+1,version) as GeometriePunkt, d.readBoolean())
            "GeometriePolygon" -> GeometriePolygon(liste(d,tiefe,version).map { it as GeometriePunkt })
            "GeometrieGruppe" -> GeometrieGruppe(liste(d,tiefe,version).map { it as GeometrischerAusdruck })
            "GeometrischeTrägermenge" -> GeometrischeTrägermenge(wert(d,tiefe+1,version) as GeometrischerAusdruck)
            "KoordinatenBild" -> KoordinatenBild(wert(d,tiefe+1,version) as GeometrischerAusdruck, wert(d,tiefe+1,version) as GeometrischesKoordinatensystem)
            "GeometrischeMengenRealisierung" -> GeometrischeMengenRealisierung(wert(d,tiefe+1,version) as MengenAusdruck, wert(d,tiefe+1,version) as EuklidischerRaum)
            "VerschobeneKoordinatenMenge" -> VerschobeneKoordinatenMenge(wert(d,tiefe+1,version) as MengenAusdruck, liste(d,tiefe,version).map { it as ZahlAusdruck })
            "SymbolischerStrukturZugriff" -> SymbolischerStrukturZugriff(wert(d,tiefe+1,version),wert(d,tiefe+1,version) as ZahlAusdruck,StrukturZugriffsArt.valueOf(d.readUTF()),d.readInt(),d.readUTF(),if(d.readBoolean()) liste(d,tiefe,version).map { it as ZahlAusdruck } else null)
            "TypisiertesElement" -> TypisiertesElement(d.readUTF(),d.readUTF(),d.readUTF(),if(d.readBoolean()) liste(d,tiefe,version).map { it as ZahlAusdruck } else null)
            "StrukturErgebnisMenge" -> StrukturErgebnisMenge(d.readUTF(),if(d.readBoolean()) liste(d,tiefe,version).map { it as ZahlAusdruck } else null,d.readUTF())
            "UnterraumMenge" -> { val dimension=d.readInt(); val richtungen=List(anzahl(d)) { liste(d,tiefe,version).map { it as ZahlAusdruck } }; UnterraumMenge(richtungen,if(d.readBoolean()) liste(d,tiefe,version).map { it as ZahlAusdruck } else null,dimension) }
            "Gradmenge" -> Gradmenge
            "LeereMenge" -> LeereMenge
            else -> error("Unbekannter mathematischer Werttyp $art.")
        }
    }
}
