package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import kotlin.test.*

class SemantischesTypSystemTest {
    private val objekt = TypId("test.objekt")
    private val komplex = TypId("test.komplex")
    private val reell = TypId("test.reell")
    private val ganz = TypId("test.ganz")
    private val methode = TypId("test.methode")

    private val system = StandardTypSystem(
        atomTypen = listOf(
            AtomTypDefinition(objekt),
            AtomTypDefinition(komplex, objekt),
            AtomTypDefinition(reell, komplex),
            AtomTypDefinition(ganz, reell),
            AtomTypDefinition(methode, objekt),
        ),
        konstruktoren = listOf(
            TypKonstruktorDefinition(TypKernIds.Tupel, standardVarianz = TypVarianz.Kovariant),
            TypKonstruktorDefinition(methode, listOf(TypVarianz.Kontravariant, TypVarianz.Kovariant)),
        ),
    )

    @Test fun `Unbekannt ist nicht Beliebig`() {
        assertEquals(TypPrüfung.Unbestimmt, system.prüfe(TypAusdruck.Unbekannt, TypAusdruck.Atom(reell)))
        assertEquals(TypPrüfung.Kompatibel, system.prüfe(TypAusdruck.Atom(reell), TypAusdruck.Beliebig))
    }

    @Test fun `Atomare Zahlhierarchie ist kovariant`() {
        assertEquals(TypPrüfung.Kompatibel, system.prüfe(TypAusdruck.Atom(ganz), TypAusdruck.Atom(reell)))
        assertIs<TypPrüfung.Inkompatibel>(system.prüfe(TypAusdruck.Atom(komplex), TypAusdruck.Atom(reell)))
    }

    @Test fun `Vereinigung entfernt redundante Untertypen`() {
        val normalisiert = system.normalisiere(
            TypAusdruck.Vereinigung(listOf(TypAusdruck.Atom(ganz), TypAusdruck.Atom(reell))),
        )
        assertEquals(TypAusdruck.Atom(reell), normalisiert)
    }

    @Test fun `Quellvereinigung muss mit allen Alternativen passen`() {
        val quelle = TypAusdruck.Vereinigung(listOf(TypAusdruck.Atom(ganz), TypAusdruck.Atom(komplex)))
        assertIs<TypPrüfung.Inkompatibel>(system.prüfe(quelle, TypAusdruck.Atom(reell)))
    }

    @Test fun `Tupel sind komponentenweise kovariant und behalten ihre Laenge`() {
        val enger = TypAusdruck.Parameterisiert(
            TypKernIds.Tupel,
            listOf(TypAusdruck.Atom(ganz), TypAusdruck.Atom(reell)),
        )
        val weiter = TypAusdruck.Parameterisiert(
            TypKernIds.Tupel,
            listOf(TypAusdruck.Atom(reell), TypAusdruck.Atom(komplex)),
        )
        val falscheLänge = TypAusdruck.Parameterisiert(
            TypKernIds.Tupel,
            listOf(TypAusdruck.Atom(reell)),
        )
        assertEquals(TypPrüfung.Kompatibel, system.prüfe(enger, weiter))
        assertIs<TypPrüfung.Inkompatibel>(system.prüfe(enger, falscheLänge))
    }

    @Test fun `Methoden sind in Argumenten kontra und im Ergebnis kovariant`() {
        val erwartet = TypAusdruck.Parameterisiert(
            methode,
            listOf(
                TypAusdruck.Parameterisiert(TypKernIds.Tupel, listOf(TypAusdruck.Atom(ganz))),
                TypAusdruck.Atom(reell),
            ),
        )
        val gegeben = TypAusdruck.Parameterisiert(
            methode,
            listOf(
                TypAusdruck.Parameterisiert(TypKernIds.Tupel, listOf(TypAusdruck.Atom(reell))),
                TypAusdruck.Atom(ganz),
            ),
        )
        assertEquals(TypPrüfung.Kompatibel, system.prüfe(gegeben, erwartet))
    }

    @Test fun `Parametrisierte Methode passt an groben Methodenport`() {
        val konkret = TypAusdruck.Parameterisiert(
            methode,
            listOf(
                TypAusdruck.Parameterisiert(TypKernIds.Tupel, listOf(TypAusdruck.Atom(reell))),
                TypAusdruck.Atom(reell),
            ),
        )
        assertEquals(TypPrüfung.Kompatibel, system.prüfe(konkret, TypAusdruck.Atom(methode)))
    }
}
