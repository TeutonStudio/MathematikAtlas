package de.TeutonStudio.MathematikKnoten.konzeptknoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.enzyklopädie.VariantenId
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensEintrag
import de.TeutonStudio.MathematikKnoten.enzyklopädie.WissensId

internal interface ExpliziteKonzeptDatei {
    val id: WissensId
    val varianten: Set<VariantenId>
    fun erstelle(vorlagen: List<KnotenVorlage>): WissensEintrag
}

internal object ExpliziteKonzeptKnoten {
    val dateien: List<ExpliziteKonzeptDatei> = listOf(
        MethodenEigenschaftKonzeptDateiA53F6C21,
        AnalysisEigenschaftKonzeptDateiBE88390F,
        FolgenEigenschaftKonzeptDatei5D2C79C4,
        MethodenStelligkeitKonzeptDatei426F55A9,
        MengenEigenschaftKonzeptDatei9041F8B7,
        ZufallsvariableKonzeptDatei70790B00,
        OffeneMengeKonzeptDatei75C049CE,
        TensorrechnerKonzeptDatei64FF6386,
        TermZuMethodeKonzeptDateiEF3062BD,
        ZahlenrechnerKonzeptDateiD21B379B,
        ZahlkonstanteKonzeptDatei6894F709,
        AbbildKonzeptDateiC41EBE73,
        AdjunktionKonzeptDatei2E800AB4,
        AllgemeinerParameterKonzeptDatei5724C3EC,
        AussagesatzKonzeptDatei306805CB,
        AuswertenKonzeptDatei9491EE1A,
        LineareAbbildungUeberpruefenKonzeptDatei3CEB1BD9,
        SkalarproduktUeberpruefenKonzeptDatei0C3B9613,
        VektorraumUeberpruefenKonzeptDateiB8402D1B,
        CauchyKonzeptDateiF15AAD59,
        DarstellungsoptimierungKonzeptDatei64A92B1D,
        DifferenzKonzeptDateiC0B93A61,
        DimensionenKonzeptDatei19DF793F,
        DisjunktionKonzeptDatei5479BC07,
        DisjunktKonzeptDateiF321CB4A,
        EinheitsvektorSpalteKonzeptDatei146DB0AB,
        EinheitsvektorZeileKonzeptDateiDD16C0F1,
        EinzelmengeKonzeptDateiE95AFB12,
        ElementKonzeptDatei33CEB866,
        EndlicheMengeKonzeptDatei8A0B2B23,
        FallunterscheidungKonzeptDatei6CBC973B,
        GleichheitKonzeptDatei0253D494,
        AufloesenKonzeptDatei2E01E8D3,
        ImplikationKonzeptDatei5343B1FD,
        IterationKonzeptDateiE75010EC,
        IterierteAdjunktionKonzeptDatei8D382236,
        IterierteDisjunktionKonzeptDatei85044197,
        IterierteKonjunktionKonzeptDatei1011D7AF,
        IterierteVereinigungKonzeptDateiB56C9CB8,
        IterierterSchnittKonzeptDateiDCF5F882,
        IteriertesKartesischesProduktKonzeptDateiF534B8DE,
        KartenAusgangKonzeptDatei9D3A3582,
        KartenEingangKonzeptDatei9196A9B8,
        KartesischesProduktKonzeptDatei4F5DFA58,
        KompositionKonzeptDateiC88629E5,
        KonjunktionKonzeptDatei433D2483,
        KreuzproduktSpaltenKonzeptDateiC2A666D0,
        KreuzproduktZeilenKonzeptDatei4F9C6B31,
        LoesungsmengeKonzeptDatei1C6DDE5B,
        LuegeKonzeptDatei1389D14E,
        MatrixInvertierenKonzeptDateiA1A8093F,
        MatrixproduktKonzeptDatei0369ED39,
        MatrixdiagonaleKonzeptDatei6E2CD166,
        MatrixrechnerKonzeptDatei50E21D48,
        MatrixKonzeptDateiE3ACDC23,
        MengenfilterKonzeptDatei50031874,
        MethodeAufrufenKonzeptDatei0AB29116,
        AussagenmethodeAnwendenKonzeptDatei3E9CB1B7,
        MengenmethodeAnwendenKonzeptDatei1FAAA738,
        MethodeAllgemeinAnwendenKonzeptDateiFE4FE5CF,
        ZahlmethodeAnwendenKonzeptDatei98605300,
        MethodeDifferentierenKonzeptDatei6C112126,
        MethodeIntegrierenKonzeptDatei0D61CF49,
        MethodenZielmengeKonzeptDateiBD7F443F,
        MaechtigkeitKonzeptDateiD03E6986,
        NegationKonzeptDatei3ED8965D,
        GroesserOderGleichKonzeptDateiEA21E5BD,
        GroesserKonzeptDatei44D01B8F,
        KleinerOderGleichKonzeptDatei0B2CFA49,
        KleinerKonzeptDatei69B0A80F,
        ReellesIntervallKonzeptDatei5B81E0DC,
        SchnittKonzeptDateiDFA2CDE6,
        SpaltenmethodeDifferentierenKonzeptDateiE0E646C8,
        SpaltenmethodeIntegrierenKonzeptDatei1E68BC3A,
        SpurKonzeptDatei849FE507,
        TeilOderGleichmengeKonzeptDatei51777C6D,
        TeilmengeKonzeptDateiE2CF2C8C,
        TensorproduktKonzeptDatei21C3C57C,
        TransponierenKonzeptDateiC5B2771D,
        TupelZuSpalteKonzeptDateiFCF3E839,
        TupelZuZeileKonzeptDatei74C8B1FF,
        TupelKonzeptDatei7F7581E3,
        UngleichheitKonzeptDatei5EF34203,
        VariableKonzeptDatei75AF5028,
        RadiusSpalteKonzeptDatei0B34B00C,
        RadiusZeileKonzeptDatei63B931F8,
        VektorZuPolynomKonzeptDatei9DB43D81,
        VektorrechnerKonzeptDatei7D632CEB,
        SpaltenvektorKonzeptDateiF5C13360,
        VereinigungKonzeptDatei0C0E4F66,
        VisualisierungKonzeptDateiDBCD59C4,
        WahrKonzeptDateiC8BDC920,
        ZeilenmethodeDifferentierenKonzeptDatei0958FE38,
        ZeilenmethodeIntegrierenKonzeptDateiD04E8A09,
        ZeilenvektorKonzeptDateiCD2FAD60,
        QuivalenzKonzeptDatei8816493A,
        BerOderGleichmengeKonzeptDateiD9089303,
        BermengeKonzeptDatei45837512,
    )
}

internal fun List<KnotenVorlage>.nachVarianten(ids: Set<VariantenId>): List<KnotenVorlage> {
    if (ids.isEmpty()) return emptyList()
    val nachId = distinctBy(KnotenVorlage::stabileVariantenId)
        .associateBy(KnotenVorlage::stabileVariantenId)
    return ids.mapNotNull(nachId::get)
}
