package de.TeutonStudio.TypSystem

import kotlin.test.*

class StandardTypSystemTest {
    private val objekt = TypId("objekt")
    private val zahl = TypId("zahl")
    private val n = TypId("N")
    private val z = TypId("Z")
    private val r = TypId("R")
    private val c = TypId("C")
    private val methode = TypId("methode")
    private val tupel = TypId("typ.tupel")

    private val eltern = mapOf(n to z, z to r, r to c, c to zahl, zahl to objekt, methode to objekt, tupel to objekt)
    private val system = StandardTypSystem(
        istAtomUntertyp = { von, erwartet ->
            var aktuell: TypId? = von
            val besucht = mutableSetOf<TypId>()
            var gefunden = false
            while (aktuell != null && besucht.add(aktuell)) {
                if (aktuell == erwartet) {
                    gefunden = true
                    break
                }
                aktuell = eltern[aktuell]
            }
            gefunden
        },
        konstruktoren = listOf(
            TypKonstruktorDefinition(tupel, standardVarianz = TypVarianz.Kovariant),
            TypKonstruktorDefinition(methode, listOf(TypVarianz.Kontravariant, TypVarianz.Kovariant)),
        ),
    )

    @Test
    fun `untertypen sind kovariant zu allgemeineren zieltypen`() {
        assertIs<TypPrüfung.Kompatibel>(
            system.prüfe(TypAusdruck.Atom(n), TypAusdruck.Atom(r)),
        )
        assertIs<TypPrüfung.Inkompatibel>(
            system.prüfe(TypAusdruck.Atom(r), TypAusdruck.Atom(n)),
        )
    }

    @Test
    fun `unbekannt ist nicht beliebig`() {
        assertIs<TypPrüfung.Unbestimmt>(
            system.prüfe(TypAusdruck.Unbekannt, TypAusdruck.Atom(r)),
        )
        assertIs<TypPrüfung.Kompatibel>(
            system.prüfe(TypAusdruck.Atom(r), TypAusdruck.Beliebig),
        )
        assertIs<TypPrüfung.Inkompatibel>(
            system.prüfe(TypAusdruck.Beliebig, TypAusdruck.Atom(r)),
        )
    }

    @Test
    fun `zielvereinigung akzeptiert eine passende alternative`() {
        val ziel = TypAusdruck.Vereinigung(listOf(TypAusdruck.Atom(r), TypAusdruck.Atom(tupel)))
        assertIs<TypPrüfung.Kompatibel>(system.prüfe(TypAusdruck.Atom(n), ziel))
    }

    @Test
    fun `quellvereinigung muss mit jeder zielmoeglichkeit kompatibel sein`() {
        val quelle = TypAusdruck.Vereinigung(listOf(TypAusdruck.Atom(n), TypAusdruck.Atom(tupel)))
        assertIs<TypPrüfung.Inkompatibel>(system.prüfe(quelle, TypAusdruck.Atom(r)))
    }

    @Test
    fun `vereinigung entfernt von obertyp ueberdeckte alternative`() {
        val normalisiert = system.normalisiere(
            TypAusdruck.Vereinigung(listOf(TypAusdruck.Atom(n), TypAusdruck.Atom(r))),
        )
        assertEquals(TypAusdruck.Atom(r), normalisiert)
    }

    @Test
    fun `variadische tupel sind komponentenweise kovariant`() {
        fun t(typen: List<TypId>) = TypAusdruck.Parameterisiert(
            tupel,
            typen.map(TypAusdruck::Atom),
        )

        assertIs<TypPrüfung.Kompatibel>(system.prüfe(t(listOf(r, c)), t(listOf(c, c))))
        assertIs<TypPrüfung.Inkompatibel>(system.prüfe(t(listOf(c, c)), t(listOf(r, c))))
        assertIs<TypPrüfung.Inkompatibel>(system.prüfe(t(listOf(r)), t(listOf(r, c))))
    }

    @Test
    fun `methoden sind im argument kontravariant und im ergebnis kovariant`() {
        fun f(domain: TypId, codomain: TypId) = TypAusdruck.Parameterisiert(
            methode,
            listOf(TypAusdruck.Atom(domain), TypAusdruck.Atom(codomain)),
        )

        assertIs<TypPrüfung.Kompatibel>(system.prüfe(f(r, n), f(n, r)))
        assertIs<TypPrüfung.Inkompatibel>(system.prüfe(f(n, r), f(r, n)))
    }

    @Test
    fun `vereinigung erzeugt gestreiften visual descriptor`() {
        val visual = TypAusdruck.Vereinigung(
            listOf(TypAusdruck.Atom(r), TypAusdruck.Atom(c)),
        ).zuVisualDescriptor { it.wert }

        assertEquals(TypVisualMuster.Gestreift, visual.muster)
        assertEquals("R ∨ C", visual.kurzLabel)
        assertEquals(listOf("R", "C"), visual.segmente.map(TypVisualSegment::label))
    }

    @Test
    fun `methodentyp wird als pfeilsignatur dargestellt`() {
        val visual = TypAusdruck.Parameterisiert(
            methode,
            listOf(TypAusdruck.Atom(r), TypAusdruck.Atom(c)),
        ).zuVisualDescriptor { it.wert }

        assertEquals("R → C", visual.kurzLabel)
        assertEquals(TypVisualMuster.Zusammengesetzt, visual.muster)
    }
}
