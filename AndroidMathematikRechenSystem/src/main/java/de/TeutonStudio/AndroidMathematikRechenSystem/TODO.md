1. Gesamtbewertung

Die grundlegende Idee ist sinnvoll:

Mathematische Objekte besitzen eine LaTeX-Darstellung.
Operatoren sind selbst mathematische Objekte.
Eine Vereinigung ist gleichzeitig ein Operator und eine Menge.
Eine Addition ist gleichzeitig eine Rechnung und eine Zahl.
Aus Operatoren soll ein symbolischer Ausdrucksbaum entstehen.
Dieser Ausdrucksbaum soll später aus der Knotenkarte erzeugt werden.

Damit bewegst du dich bereits in Richtung eines kleinen Computer-Algebra-Systems. Menschen nennen so etwas gern „ein paar Interfaces“, kurz bevor sie versehentlich Mathematica neu schreiben.

Der aktuelle Entwurf hat jedoch drei grundlegende Probleme:

Werte, Ausdrücke und Operatoren sind miteinander vermischt.
Mathematische Teilmengenbeziehungen werden über Kotlin-Vererbung abgebildet.
Es gibt noch keine Schicht, welche die Knotenkarte validiert, sortiert und auswertet.

Zusätzlich enthält der aktuelle Stand mehrere Kompilierfehler und fast überall nicht implementierte Methoden.

2. Aktuelle Struktur
   2.1 Basiskern
   MathematischesObjekt
   interface MathematischesObjekt {
   fun zuLatex(): String
   fun vereinfacht(): MathematischesObjekt
   }

Dieses Interface ist der gemeinsame Ursprung aller mathematischen Objekte. Es fordert:

eine Darstellung als LaTeX,
eine vereinfachte Form.

Das ist als Anfang verständlich, aber langfristig zu unspezifisch. vereinfacht() verliert beispielsweise den konkreten Rückgabetyp, weil immer nur MathematischesObjekt zurückkommt.

MathematischerOperator
interface MathematischerOperator : MathematischesObjekt

Der Operator fügt gegenwärtig keine Eigenschaften hinzu. Es gibt weder:

Anzahl oder Typ der Argumente,
Ergebnistyp,
Auswertungsfunktion,
Operator-ID,
algebraische Eigenschaften,
Fehlerprüfung.

Damit ist der Typ aktuell nur eine Markierung.

LaTeXOperator

Dieses Interface definiert zwei Zeichenketten:

val BINÄR_OPERATOR: String
val OPERATOR: String

Offen bleibt, wann der binäre und wann der allgemeine Operator verwendet wird und wie Klammern oder Operatorprioritäten behandelt werden.

Abbildung

Abbildung ist momentan ebenfalls nur eine leere Markierung. Definitionsmenge, Zielmenge und Anwendung fehlen vollständig.

2.2 Mengenlehre
Element

Ein Element muss seine „kleinste Obermenge“ kennen:

fun kleinsteOberMenge(): Menge<out Element>

Der Kommentar TODELETE deutet bereits an, dass dir diese Abhängigkeit verdächtig vorkommt. Zu Recht. Ein Wert sollte nicht selbst für die globale Klassifikation aller Mengen verantwortlich sein.

Menge
interface Menge<E : Element> : Element {
fun enthält(element: E): Boolean
}

Eine Menge wird damit immer selbst als Element behandelt. Mathematisch kann eine Menge Element einer anderen Menge sein, aber das sollte nicht automatisch für jede Menge gelten.

Außerdem liefert enthält nur Boolean. Für symbolische Mathematik werden mindestens drei Zustände benötigt:

enthalten,
nicht enthalten,
gegenwärtig nicht entscheidbar.

Beispielsweise kann bei einer symbolischen Variablen x oft nicht sofort entschieden werden, ob x ∈ ℕ gilt.

Mengenoperatoren

Die Hierarchie lautet derzeit:

MathematischerOperator
│
▼
MengenOperator<E>
│
├── Vereinigung<E>
└── binärMengenOperator<...>
└── binärKartesischeProdukt<...>

MengenOperator ist gleichzeitig:

Operator,
Menge,
Element.

Die Idee ist: Ein nicht vollständig ausgerechneter Ausdruck wie A ∪ B kann selbst als Menge behandelt werden. Das ist richtig. Allerdings sollte er dann ein Mengenausdruck sein, nicht gleichzeitig die Definition der Operation und deren Ergebnis.

Vereinigung besitzt bereits linke und rechte Eingaben, aber LaTeX, Vereinfachung, Enthaltensein und Typbestimmung sind sämtlich unimplementiert.

Das kartesische Produkt folgt demselben Muster. Lediglich kleinsteOberMenge() gibt bereits this zurück.

2.3 Zahlen und Rechnungen
Zahl

Eine Zahl ist ein Element und muss eine kleinste Obermenge bestimmen. Es existiert bisher aber keine konkrete Zahlenklasse, also beispielsweise kein:

GanzzahlWert,
RationalerWert,
DezimalWert,
KomplexerWert,
Variable.

Damit lassen sich gegenwärtig zwar Additionsobjekte konstruieren, aber keine tatsächlichen Zahlenwerte sinnvoll einsetzen.

Rechnung
interface Rechnung : MathematischerOperator, Zahl, Element {
fun istAsoziativ(): Boolean
fun istKommutativ(): Boolean
}

Eine Rechnung ist hier gleichzeitig eine Zahl. Als symbolischer Zahlenausdruck ist das nachvollziehbar.

Problematisch ist, dass Assoziativität und Kommutativität nicht allein vom Operator abhängen. Sie können vom betrachteten Zahlenbereich abhängen. Genau deshalb werden ähnliche Methoden später noch einmal in ZahlenMenge definiert. Damit existieren aktuell zwei konkurrierende Zuständigkeiten.

addition

Die Klasse soll endliche und indexierte Additionen unterstützen. Das ist eine gute spätere Zielsetzung, für einen ersten Stand aber unnötig komplex.

Aktuelle Probleme:

vararg arguments: List<Zahl> bedeutet mehrere Listen statt mehrere Zahlen.
Die Argumente werden nicht gespeichert.
Compose-Zustand wird direkt im Mathematikkern verwendet.
istAsoziativ() besitzt eine falsche Rückgabesignatur.
istKommutativ() fehlt.
Alle Kernfunktionen sind nicht implementiert.

Die Verwendung von SnapshotStateMap koppelt die Mathematikengine direkt an Compose. Der mathematische Kern sollte jedoch unabhängig von Android und UI ausführbar sein.

subtraktion

Hier ist als einzige Rechnung bereits eine LaTeX-Ausgabe vorhanden:

minuend - subtrahend

Die Berechnung der kleinsten Obermenge enthält dagegen mehrere nicht definierte Bezeichner und ungültige Aufrufe:

ReelleZahlen ist nicht importiert,
RelleZahlen ist falsch geschrieben,
arg: und ararg: sind keine gültigen Argumentausdrücke,
NatürlicheZahlen und GanzeZahlen werden wie Singleton-Objekte verwendet, sind aber Klassen.

Diese Datei ist momentan nicht kompilierbar.

multiplikation

Kommutativität und Assoziativität werden pauschal als wahr angegeben. Für gewöhnliche Zahlenbereiche ist das brauchbar, für Matrizen, Quaternionen oder Funktionen jedoch nicht allgemein richtig.

Daher sollte die Eigenschaft über eine Kombination aus Operator und Definitionsbereich abgefragt werden.

2.4 Vordefinierte Zahlenmengen

Aktuell wird folgende Klassenhierarchie angestrebt:

NatürlicheZahlen
└── GanzeZahlen
└── RationaleZahlen
├── ReelleZahlen
└── KomplexeZahlen

Das vermischt zwei völlig verschiedene Beziehungen:

Kotlin: „ist eine Instanz von“
Mathematik: „ist Teilmenge von“

Besonders sichtbar wird das bei:

class KomplexeZahlen : RationaleZahlen()

Das entspricht nicht der beabsichtigten Inklusionskette. Gemeint wäre:

ℕ ⊆ ℤ ⊆ ℚ ⊆ ℝ ⊆ ℂ

KomplexeZahlen müsste nach der aktuellen Logik wenigstens von ReelleZahlen erben. Besser ist jedoch, Mengeninklusion überhaupt nicht durch Vererbung abzubilden.

NatürlicheZahlen enthält bereits erste Tabellen für Kommutativität, Assoziativität und Distributivität. Der Ansatz ist brauchbar, enthält aber Duplikate und unvollständige Fälle. So wird dieselbe Kombination aus Subtraktion und Multiplikation zweimal geprüft.

2.5 Aussagenlogik
Aussage

Das Interface besitzt gegenwärtig keinen Wahrheitswert und keine Auswertungsfunktion.

Prädikat
interface Prädikat : MathematischerOperator, Aussage

Das ist mathematisch nicht sauber getrennt.

Ein Prädikat ist zunächst eine Abbildung:

Element → Aussage

Erst wenn das Prädikat auf ein Argument angewendet wird, entsteht eine Aussage.

Beispiel:

Prädikat:       „ist gerade“
Anwendung:      „4 ist gerade“
Wahrheitswert:  wahr

Prädikat sollte daher eine besondere Abbildung sein und nicht unmittelbar eine Aussage.

Konjunktion und Disjunktion

Die Operatorzeichen sind vertauscht:

konjunktion verwendet \vee, also Oder.
disjunktion verwendet \wedge, also Und.

Darüber hinaus werden die Konstruktorargumente nicht als Eigenschaften gespeichert. Dadurch kann keine der beiden Klassen ihre Argumente später auswerten oder rendern.

2.6 Tupel, Folgen und Netze
Dupel

Ein Paar aus linkem und rechtem Element ist grundsätzlich sinnvoll. Die Bezeichnung sollte aber eher Paar oder Tupel2 lauten.

Momentan wird das Paar als ElementOperator behandelt, obwohl es kein Operator ist, sondern ein zusammengesetzter Wert beziehungsweise Ausdruck.

Tupel

Tupel kapselt bereits eine Funktion Int → O. Der Typparameter idxM wird aber nicht verwendet. Zudem ist ein allgemeines Tupel normalerweise endlich und sollte direkt eine Liste von Elementen besitzen.

Folge und Netz

Beide Klassen sind bislang leer. Die Typparameter versuchen ihre Indexmengen über Interval<...> als Kotlin-Obergrenzen auszudrücken. Das wird schnell unnötig kompliziert und bietet der Laufzeit kaum Nutzen.

3. Empfohlene Zielarchitektur

Die Mathematikengine sollte von Android, Compose und der Darstellung der Knotenkarte unabhängig bleiben.

AndroidMathematikRechenSystem/
└── src/main/java/de/TeutonStudio/AndroidMathematikRechenSystem/
├── kern/
│   ├── typ/
│   ├── wert/
│   ├── ausdruck/
│   ├── operator/
│   ├── auswertung/
│   ├── vereinfachung/
│   ├── fehler/
│   └── rendering/
│
├── zahlen/
│   ├── werte/
│   ├── bereiche/
│   └── operatoren/
│
├── mengen/
│   ├── werte/
│   ├── ausdruecke/
│   └── operatoren/
│
├── logik/
│   ├── werte/
│   ├── ausdruecke/
│   └── operatoren/
│
├── abbildungen/
├── iterationen/
├── einheiten/
│
├── graph/
│   ├── modell/
│   ├── validierung/
│   ├── compiler/
│   ├── auswertung/
│   └── adapter/
│
└── serialisierung/
4. Zentrale Trennung: Typ, Wert, Ausdruck und Operator
   4.1 Mathematischer Typ

Der Typ beschreibt, was an einem Anschluss erlaubt ist.

sealed interface MatheTyp {
val id: String
}

data object BeliebigesObjektTyp : MatheTyp {
override val id = "objekt"
}

data object ZahlTyp : MatheTyp {
override val id = "zahl"
}

data object AussageTyp : MatheTyp {
override val id = "aussage"
}

data class MengenTyp(
val elementTyp: MatheTyp,
) : MatheTyp {
override val id = "menge<${elementTyp.id}>"
}

data class TupelTyp(
val komponenten: List<MatheTyp>,
) : MatheTyp {
override val id = "tupel<${komponenten.joinToString { it.id }}>"
}

Diese Typen können direkt den Anschlüssen deiner Knoten zugeordnet werden.

Beispiele:

Additionsknoten:
Eingang: Zahl, mehrfach
Ausgang: Zahl

Vereinigung:
Eingang links:  Menge<T>
Eingang rechts: Menge<T>
Ausgang:        Menge<T>

Konjunktion:
Eingang: Aussage, mehrfach
Ausgang: Aussage
4.2 Mathematischer Wert

Ein Wert ist ein tatsächlich bestimmtes mathematisches Objekt.

sealed interface MatheWert {
val typ: MatheTyp
}

Beispiele:

data class GanzzahlWert(
val wert: BigInteger,
) : MatheWert {
override val typ = ZahlTyp
}

data class RationalerWert(
val zaehler: BigInteger,
val nenner: BigInteger,
) : MatheWert {
override val typ = ZahlTyp
}

enum class Wahrheitswert : MatheWert {
WAHR,
FALSCH,
UNBESTIMMT;

    override val typ = AussageTyp
}

UNBESTIMMT ist wichtig. Ein symbolisches System darf fehlendes Wissen nicht einfach als false verkleiden. Das tun Menschen schon häufig genug.

4.3 Ausdruck

Ein Ausdruck ist entweder:

ein konstanter Wert,
eine Variable,
eine Operatoranwendung.
sealed interface Ausdruck<out T : MatheWert> {
val typ: MatheTyp
}

data class Konstante<T : MatheWert>(
val wert: T,
) : Ausdruck<T> {
override val typ = wert.typ
}

data class Variable(
val name: String,
override val typ: MatheTyp,
) : Ausdruck<MatheWert>

data class OperatorAnwendung(
val operatorId: String,
val argumente: List<Ausdruck<*>>,
override val typ: MatheTyp,
) : Ausdruck<MatheWert>

Damit ist eine Addition nicht selbst die Definition des Operators, sondern eine konkrete Anwendung:

Operator:    Addition
Argumente:   [2, x, 5]
Ausdruck:    2 + x + 5
4.4 Operatorbeschreibung
interface OperatorDefinition {
val id: String
val name: String
val eingänge: List<PortSignatur>
val ausgangsTyp: TypRegel
val arität: Arität

    fun validiere(
        argumente: List<Ausdruck<*>>,
        kontext: AuswertungsKontext,
    ): List<MatheFehler>

    fun auswerten(
        argumente: List<AuswertungsErgebnis>,
        kontext: AuswertungsKontext,
    ): AuswertungsErgebnis

    fun vereinfache(
        anwendung: OperatorAnwendung,
        kontext: VereinfachungsKontext,
    ): Ausdruck<*>
}
Arität
sealed interface Arität {
data class Fest(val anzahl: Int) : Arität
data class Bereich(val minimum: Int, val maximum: Int?) : Arität
}

Beispiele:

Subtraktion:       genau 2
Kartesisches Produkt: genau 2
Addition:          mindestens 2
Konjunktion:       mindestens 2
Negation:          genau 1
5. Algebraische Eigenschaften

Assoziativität und Kommutativität sollten nicht als Methoden auf jedem Rechnungsexemplar liegen.

Stattdessen:

data class AlgebraischeEigenschaften(
val assoziativ: Boolean?,
val kommutativ: Boolean?,
val neutralesElement: MatheWert?,
val absorbierendesElement: MatheWert?,
)

Die Eigenschaften werden abhängig von Operator und Bereich bestimmt:

interface AlgebraRegelwerk {
fun eigenschaften(
operatorId: String,
bereich: Zahlenbereich,
): AlgebraischeEigenschaften

    fun istDistributiv(
        äußererOperatorId: String,
        innererOperatorId: String,
        bereich: Zahlenbereich,
    ): Boolean?
}

Dabei bedeutet:

true: gilt,
false: gilt nicht,
null: nicht definiert oder derzeit unbekannt.

Das ist besser als ein TODO zur Laufzeit, das die Anwendung bei einer harmlosen mathematischen Frage in den Abgrund wirft.

6. Mengenmodell
   6.1 Mengen als Werte und Ausdrücke
   interface MatheMenge : MatheWert {
   val elementTyp: MatheTyp

   fun enthält(
   element: MatheWert,
   kontext: AuswertungsKontext,
   ): Entscheidungswert
   }

enum class Entscheidungswert {
JA,
NEIN,
UNBESTIMMT,
}

Konkrete endliche Menge:

data class EndlicheMenge(
val elemente: Set<MatheWert>,
override val elementTyp: MatheTyp,
) : MatheMenge {
override val typ = MengenTyp(elementTyp)
}

Symbolische Mengenoperation:

data class VereinigungsAusdruck(
val links: Ausdruck<*>,
val rechts: Ausdruck<*>,
override val typ: MatheTyp,
) : Ausdruck<MatheWert>
6.2 Zahlenbereiche als Singleton-Objekte
sealed interface Zahlenbereich : MatheMenge {
val symbol: String
}

data object NatuerlicheZahlen : Zahlenbereich
data object GanzeZahlen : Zahlenbereich
data object RationaleZahlen : Zahlenbereich
data object ReelleZahlen : Zahlenbereich
data object KomplexeZahlen : Zahlenbereich
data object GausscheZahlen : Zahlenbereich

Die Teilmengenbeziehung wird separat definiert:

object ZahlenbereichOrdnung {

    fun istTeilmenge(
        links: Zahlenbereich,
        rechts: Zahlenbereich,
    ): Boolean = when (links to rechts) {
        NatuerlicheZahlen to GanzeZahlen -> true
        NatuerlicheZahlen to RationaleZahlen -> true
        NatuerlicheZahlen to ReelleZahlen -> true
        NatuerlicheZahlen to KomplexeZahlen -> true

        GanzeZahlen to RationaleZahlen -> true
        GanzeZahlen to ReelleZahlen -> true
        GanzeZahlen to KomplexeZahlen -> true

        RationaleZahlen to ReelleZahlen -> true
        RationaleZahlen to KomplexeZahlen -> true

        ReelleZahlen to KomplexeZahlen -> true
        GausscheZahlen to KomplexeZahlen -> true

        else -> links == rechts
    }
}

Damit wird Mengeninklusion nicht länger durch Klassenvererbung missbraucht.

7. Aussagen und Prädikate
   Aussage
   sealed interface AussagenAusdruck : Ausdruck<Wahrheitswert>
   Prädikat
   interface Praedikat {
   val id: String
   val argumentTypen: List<MatheTyp>

   fun anwenden(
   argumente: List<Ausdruck<*>>,
   ): AussagenAusdruck
   }

Beispiel:

data class IstElementVon(
val element: Ausdruck<*>,
val menge: Ausdruck<*>,
) : AussagenAusdruck {
override val typ = AussageTyp
}

Konjunktion und Disjunktion sind keine Prädikate, sondern Operatoren auf Aussagen:

data class Konjunktion(
val argumente: List<AussagenAusdruck>,
) : AussagenAusdruck

data class Disjunktion(
val argumente: List<AussagenAusdruck>,
) : AussagenAusdruck

LaTeX-Zeichen:

Konjunktion:  \wedge beziehungsweise \bigwedge
Disjunktion:  \vee beziehungsweise \bigvee
8. LaTeX-Rendering

Die LaTeX-Erzeugung sollte nicht in jedem Wert selbst vollständig implementiert werden. Sonst verteilt sich Klammerlogik über dreißig Klassen und niemand weiß später, weshalb a - b - c plötzlich etwas anderes bedeutet.

interface AusdruckRenderer<R> {
fun rendere(
ausdruck: Ausdruck<*>,
kontext: RenderingKontext = RenderingKontext(),
): R
}
class LaTeXRenderer : AusdruckRenderer<String>

Der Renderer benötigt Operatorprioritäten:

enum class Prioritaet(val wert: Int) {
AUSSAGE(10),
VERGLEICH(20),
ADDITION(30),
MULTIPLIKATION(40),
POTENZ(50),
ATOM(100),
}

Dadurch kann er entscheiden, wann Klammern notwendig sind.

9. Auswertungsergebnis und Fehler

Eine Knotenauswertung darf nicht einfach TODO, null oder eine Exception zurückgeben.

sealed interface AuswertungsErgebnis {

    data class Berechnet(
        val wert: MatheWert,
    ) : AuswertungsErgebnis

    data class Symbolisch(
        val ausdruck: Ausdruck<*>,
    ) : AuswertungsErgebnis

    data class Fehlgeschlagen(
        val fehler: List<MatheFehler>,
    ) : AuswertungsErgebnis
}

Fehler sollten den verursachenden Knoten und Anschluss nennen:

sealed interface MatheFehler {
val nachricht: String

    data class FehlenderEingang(
        val knotenId: String,
        val anschlussId: String,
    ) : MatheFehler

    data class FalscherTyp(
        val knotenId: String,
        val anschlussId: String,
        val erwartet: MatheTyp,
        val erhalten: MatheTyp,
    ) : MatheFehler

    data class Zyklus(
        val knotenIds: List<String>,
    ) : MatheFehler

    data class DefinitionsbereichVerletzt(
        val knotenId: String,
        override val nachricht: String,
    ) : MatheFehler
}
10. Verbindung zur Knotenkarte

Das Rechensystem sollte nicht direkt von deinen Compose-Klassen Graph, Knoten, Anschluss oder Verbindung abhängen.

Stattdessen erhält es einen neutralen Snapshot.

data class RechenGraph(
val knoten: List<RechenKnoten>,
val kanten: List<RechenKante>,
)

data class RechenKnoten(
val id: String,
val operatorId: String,
val parameter: Map<String, SerialisierbarerWert>,
)

data class RechenKante(
val startKnotenId: String,
val startPortId: String,
val zielKnotenId: String,
val zielPortId: String,
)

Ein Adapter übersetzt deinen aktuellen Graphen:

interface RechenGraphAdapter<G> {
fun erzeugeSnapshot(graph: G): RechenGraph
}

Damit bleibt:

KnotenKartenVerwalter
│
│ erzeugt Snapshot
▼
RechenGraph
│
│ wird kompiliert
▼
Ausdrucksgraph
│
│ wird ausgewertet
▼
Knotenergebnisse
11. Ablauf der Knotenauswertung
    Schritt 1: Graphsnapshot erstellen

Der sichtbare Graph wird in ein unveränderliches Rechenmodell kopiert. Während einer Auswertung dürfen keine Compose-Zustände benötigt werden.

Schritt 2: Operatoren auflösen

Jeder Knoten besitzt eine stabile operatorId, beispielsweise:

zahl.konstante
zahl.addition
zahl.subtraktion
menge.vereinigung
menge.kartesisches-produkt
logik.konjunktion

Eine Registry löst diese IDs auf:

class OperatorRegistry(
operatoren: Collection<OperatorDefinition>,
)
Schritt 3: Anschlüsse validieren

Für jeden Knoten wird geprüft:

Sind alle Pflichtanschlüsse verbunden?
Hat ein einzelner Eingang höchstens eine eingehende Verbindung?
Ist die Anzahl variabler Eingänge gültig?
Passen Ausgangs- und Eingangstyp zusammen?
Existieren Operator und Port überhaupt?
Schritt 4: Zyklen erkennen

Normale Berechnungsgraphen müssen azyklisch sein.

A → B → C
▲     │
└─────┘

Ein solcher Zyklus muss als Fehler gemeldet werden.

Später können spezielle Knoten wie Iteration, Rekursion oder Fixpunkt Zyklen kontrolliert kapseln. Bis dahin sollte ein Zyklus nicht zu einer Stackoverflow-Lotterie werden.

Schritt 5: Topologisch sortieren

Die Knoten werden so geordnet, dass Eingaben immer vor ihren Verbrauchern ausgewertet werden.

Konstanten
↓
Teiloperationen
↓
Endergebnis
Schritt 6: Typen bestimmen

Bei generischen Knoten muss der konkrete Typ aus den Eingängen abgeleitet werden.

Beispiel:

Menge<Ganzzahl> ∪ Menge<Ganzzahl>
↓
Menge<Ganzzahl>

Dagegen:

Menge<Ganzzahl> ∪ Menge<Aussage>

führt zu einem Typfehler.

Schritt 7: Knoten auswerten

Jeder Knoten erhält die Ergebnisse seiner Vorgänger.

Ein Knoten kann liefern:

konkreten Wert,
symbolischen Ausdruck,
Fehler.
Schritt 8: Ergebnisse speichern
data class GraphAuswertung(
val knotenErgebnisse: Map<String, AuswertungsErgebnis>,
val endErgebnisse: List<AuswertungsErgebnis>,
)

Die UI kann anschließend jeden Knoten entsprechend darstellen:

Grau:   noch nicht ausgewertet
Grün:   erfolgreich
Gelb:   symbolisch/unbestimmt
Rot:    Fehler
12. Cache und inkrementelle Neuberechnung

Der Cache gehört in den Auswertungskontext, nicht in einen einzelnen Operator.

data class CacheSchluessel(
val knotenId: String,
val operatorId: String,
val eingangsHash: Int,
val parameterHash: Int,
)

Beim Verschieben eines Knotens muss nichts neu berechnet werden.

Beim Ändern einer Verbindung müssen nur neu ausgewertet werden:

der betroffene Zielknoten,
alle von ihm abhängigen Nachfolger.

Das ist deutlich effizienter als die gesamte Karte neu auszuwerten, nur weil ein Mensch eine Kante umgesteckt hat.

13. Empfohlene Migration der vorhandenen Klassen
    Aktuelle Klasse	Empfohlene Änderung
    MathematischesObjekt	Durch MatheWert und Ausdruck<T> ersetzen
    MathematischerOperator	Durch OperatorDefinition ersetzen
    LaTeXOperator	In Metadaten des Renderers verschieben
    Abbildung	Generisch als Definitionsmenge → Zielmenge definieren
    Element	Nicht mehr jede Typklassifikation selbst bestimmen lassen
    Menge<E>	In MatheMenge und MengenAusdruck aufteilen
    MengenOperator	Durch konkrete Operatordefinitionen ersetzen
    ElementOperator	Entfernen; Wert und Operator sind verschiedene Dinge
    Zahl	In Zahlenwert und Zahlenausdruck aufteilen
    Rechnung	Durch Zahlenoperatoren und Zahlenausdrücke ersetzen
    ZahlenMenge	Als Zahlenbereich plus Algebraregelwerk modellieren
    NatürlicheZahlen usw.	Als data object definieren
    Vereinigung	Als Operator plus OperatorAnwendung modellieren
    addition	AdditionOperator nennen und Argumente im Ausdruck speichern
    subtraktion	SubtraktionOperator, exakt zwei Eingänge
    multiplikation	MultiplikationOperator, variadische Eingänge
    Prädikat	Als Abbildung zu einer Aussage definieren
    konjunktion	KonjunktionOperator, Zeichen korrigieren
    disjunktion	DisjunktionOperator, Zeichen korrigieren
    Dupel	In Paar oder Tupel2 umbenennen
    Tupel	Als endliche Komponentenliste modellieren
    Folge	Indexmenge plus Bildungsgesetz speichern
    Netz	Erst nach funktionierendem Folgenmodell implementieren
    Einheit	Später als Dimension plus Umrechnung definieren
14. Vollständige Implementierungsliste
    Priorität 0: Projekt wieder kompilierbar machen
    Falsche Signatur von addition.istAsoziativ() korrigieren.
    addition.istKommutativ() ergänzen.
    Argumente der Addition tatsächlich speichern.
    SnapshotStateMap und sämtliche Compose-Abhängigkeiten aus dem Rechenkern entfernen.
    Fehlerhafte Bezeichner in subtraktion entfernen.
    Zahlenmengen nicht mehr wie Singleton-Objekte verwenden, solange sie Klassen sind.
    ReelleZahlen, GanzeZahlen und weitere fehlende Importe beziehungsweise Typen korrigieren.
    Operatorzeichen von Konjunktion und Disjunktion korrigieren.
    Nicht verwendete Importe entfernen.
    Klassen nach Kotlin-Konvention groß schreiben:
    Addition
    Subtraktion
    Multiplikation
    Konjunktion
    Disjunktion
    BinaererMengenOperator
    Für alle derzeitigen TODO()-Pfade entscheiden, ob sie implementiert oder zunächst als strukturierter Fehler behandelt werden.
    Priorität 1: Fundament des Rechensystems
    MatheTyp definieren.
    MatheWert definieren.
    Ausdruck<T> definieren.
    Konstante<T> definieren.
    Variable definieren.
    OperatorAnwendung definieren.
    OperatorDefinition definieren.
    OperatorRegistry definieren.
    Arität und PortSignatur definieren.
    AuswertungsErgebnis definieren.
    MatheFehler definieren.
    AuswertungsKontext definieren.
    VereinfachungsKontext definieren.
    LaTeX-Renderer mit Operatorprioritäten implementieren.
    Priorität 2: Minimales Zahlenmodell
    GanzzahlWert mit BigInteger.
    RationalerWert mit gekürztem Zähler und Nenner.
    Optional DezimalWert mit BigDecimal.
    AdditionOperator.
    SubtraktionOperator.
    MultiplikationOperator.
    Konstante Auswertung implementieren.
    Gemischte Zahlentypen vereinheitlichen.
    Ergebnisbereiche bestimmen.
    Division durch null als Fehler modellieren.
    Neutrale Elemente vereinfachen:
    x + 0 → x
    x · 1 → x
    Absorbierende Elemente:
    x · 0 → 0
    Konstante Teilausdrücke auswerten.
    Assoziative Ausdrücke abflachen:
    (a + b) + c → a + b + c
    Kommutative Argumente bei Bedarf kanonisch sortieren.
    Priorität 3: Zahlenbereiche und Mengen
    Zahlenbereiche als data object definieren.
    Teilmengenbeziehungen separat modellieren.
    Festlegen, ob 0 ∈ ℕ gelten soll.
    EndlicheMenge implementieren.
    MengenLiteral implementieren.
    VereinigungOperator implementieren.
    SchnittOperator implementieren.
    DifferenzOperator implementieren.
    KartesischesProduktOperator implementieren.
    Mitgliedschaftsprüfung implementieren.
    Entscheidungswert mit JA, NEIN, UNBESTIMMT.
    Mengen-LaTeX implementieren.
    Vereinfachungsregeln:
    A ∪ ∅ → A
    A ∪ A → A
    A ∩ A → A
    A \ ∅ → A
    ∅ × A → ∅
    Priorität 4: Aussagenlogik
    Wahrheitswert implementieren.
    AussagenAusdruck implementieren.
    KonjunktionOperator.
    DisjunktionOperator.
    NegationOperator.
    ImplikationOperator.
    AequivalenzOperator.
    Wahrheitswerttabellen implementieren.
    Symbolische Aussagen unterstützen.
    Prädikate als Abbildungen definieren.
    Vergleichsprädikate:
    Gleichheit,
    kleiner,
    größer,
    Element von,
    Teilmenge von.
    Später Quantoren:
    FürAlle,
    EsExistiert.
    Priorität 5: Graphintegration
    Neutralen RechenGraph definieren.
    Adapter vom vorhandenen Karten-Graphen implementieren.
    Operator-ID in Knotendaten speichern.
    Port-IDs stabil und serialisierbar machen.
    Mathematischen Typ an Anschlüssen hinterlegen.
    Graphvalidierung implementieren.
    Fehlende Eingänge erkennen.
    Mehrfachbelegung einzelner Eingänge erkennen.
    Falsche Verbindungstypen erkennen.
    Zyklenerkennung implementieren.
    Topologische Sortierung implementieren.
    Auswertung in Sortierreihenfolge implementieren.
    Pro Knoten ein Ergebnis speichern.
    Fehler an Knoten und Anschluss zurückmelden.
    Ergebnisknoten beziehungsweise Ausgabeknoten bestimmen.
    Inkrementelle Neuberechnung implementieren.
    Cache anhand von Eingangswerten und Parametern implementieren.
    Graphänderungen von rein visuellen Änderungen unterscheiden.
    Priorität 6: Abbildungen und Variablen
    Generische Abbildung mit Definitions- und Zieltyp definieren.
    Funktionsparameter und Bindungen implementieren.
    Variablenkontext implementieren.
    Funktionsanwendung als Ausdruck implementieren.
    Lambdaartige Abbildungen unterstützen.
    Komposition von Abbildungen.
    Identitätsabbildung.
    Injektivität, Surjektivität und Bijektivität als optionale Eigenschaften.
    Definitionsbereichsfehler behandeln.
    Priorität 7: Intervalle, Folgen und Netze
    Intervall mit unterer und oberer Grenze definieren.
    Offene und geschlossene Grenzen modellieren.
    Unendliche Grenzen unterstützen.
    Mitgliedschaft im Intervall implementieren.
    Paar und endliches Tupel implementieren.
    Folge als Abbildung von einer Indexmenge definieren.
    Bildungsgesetz speichern.
    Einzelnes Folgenglied auswerten.
    Endliche Teilfolge erzeugen.
    Summen- und Produktoperator über Folgen.
    Netz erst danach implementieren.
    Priorität 8: Einheiten
    Einheit von Java nach Kotlin überführen.
    Grunddimensionen definieren:
    Länge,
    Zeit,
    Masse,
    Stromstärke,
    Temperatur,
    Stoffmenge,
    Lichtstärke.
    Dimensionsexponenten speichern.
    Umrechnungsfaktoren definieren.
    Addition nur bei kompatiblen Einheiten erlauben.
    Multiplikation und Division von Einheiten.
    Zusammengesetzte Einheiten vereinfachen.
    Einheit in LaTeX rendern.
    Priorität 9: Tests

Mindestens folgende Testgruppen sind notwendig:

Typprüfung
Zahl darf an Zahleneingang angeschlossen werden.
Aussage darf nicht an Zahleneingang angeschlossen werden.
Generischer Mengentyp wird korrekt bestimmt.
Rechenoperationen
Addition ganzer Zahlen.
Subtraktion mit negativem Ergebnis.
Multiplikation rationaler Zahlen.
Vereinfachung neutraler Elemente.
Mengenlehre
Vereinigung endlicher Mengen.
Kartesisches Produkt.
Zahlenbereich-Inklusion.
Unbestimmbare Mitgliedschaft.
Aussagenlogik
Wahrheitswerttabellen.
Korrekte LaTeX-Zeichen.
Symbolische Aussagen.
Graph
Einfacher linearer Graph.
Verzweigter Graph.
Mehrere Endknoten.
Fehlender Eingang.
Typfehler.
Zyklus.
Nur abhängige Knoten werden neu ausgewertet.
Rendering
Operatorpriorität.
Klammerung.
Mengen.
Tupel.
Verschachtelte logische und arithmetische Ausdrücke.
15. Sinnvolle erste funktionsfähige Version

Für die erste tatsächlich nutzbare Version sollte der Umfang bewusst klein bleiben:

Ganze Zahlen als Konstanten.
Variablen.
Addition, Subtraktion und Multiplikation.
Endliche Mengen.
Vereinigung und kartesisches Produkt.
Wahr, falsch, Und und Oder.
LaTeX-Ausgabe.
Typisierte Knotenanschlüsse.
Graphvalidierung.
Topologische Knotenauswertung.
Fehleranzeige pro Knoten.
Einfache Vereinfachungsregeln.

Noch nicht in die erste Version gehören:

allgemeine reelle Zahlen,
komplexe Zahlenarithmetik,
Netze,
Grenzwerte,
Quantoren,
Einheiten,
automatische Beweisführung,
universelle Distributivitätsanalyse.

Der wichtigste nächste Schritt ist daher nicht, die bestehenden TODO()-Methoden einzeln auszufüllen. Zuerst müssen Wert, Ausdruck, Operator und Graphauswertung getrennt werden. Andernfalls entsteht eine wachsende Sammlung mathematischer Klassen, die alle voneinander erben, aber gemeinsam immer noch keine einzelne Knotenkarte zuverlässig berechnen können.