# Graphgespeiste endliche Mengen und unveränderte Standardkarten

Datum: 2026-10-17. Status: M4 in Umsetzung; Produkt-/Laufzeitfreigabe offen.

## Entscheidung

Der bestehende Typ `mathematik.endlicheMenge` erhält ausdrücklich gewählte
Eingabeformen `eingabeModus=lokal|graph`. Ohne Parameter gilt der historische
Lokalmodus mit unverändertem v2-/CSV-Datenvertrag. Im Graphmodus bestimmt
`endlicheMenge.anzahl` 0 bis 64 einzelne, geordnete Pflichtanschlüsse
`element1` bis `elementN`. Nur die ausdrücklich konfigurierte Anzahl 0 erzeugt
eine leere Menge ohne Elemente. Tupel, Mengen und mathematische Methoden sind
jeweils vollständige Elemente. Nichtmathematische Atlaswerte werden abgewiesen.

Alle Bedingungen, Quellidentitäten und Übersetzungsherkünfte werden vor der
mathematischen Duplikatentfernung gesammelt. Eine neu gebildete Menge übernimmt
kein aktives Element-Übersetzungszeugnis. Fremde Eingänge und Mehrfachbeiträge
werden abgewiesen; inaktive lokale Konfigurationen und Operatorparameter wirken
nicht auf Graphwerte oder Anschlussnormalisierung.

## Migration und Konfiguration

Einzelmenge wird Graphmodus mit einem Pflichtanschluss; Potenz- und
Abbildungsmenge werden vorhandene Mengenrechneroperatoren. Die strenge Migration
prüft gerichtete Rollen, Arten, Kardinalität, IDs und tatsächliche Einzelbelegung
vor allgemeinen Normalisierungen. Defaults, feste Kartenfallbacks und semantische
Rollenreferenzen werden gemeinsam umgesetzt. Nicht nachweisbar erhaltbare
Verträge bleiben im Original mit blockierender Diagnose erhalten.

Der Mengenausgang wird auch bei direkt geladenen kanonischen Graphknoten geprüft.
Widersprechende Arten, semantische Typen oder nicht nachgewiesene Ausgangstypregeln
sind Diagnosen, keine stillen Reparaturen. Allgemeine Mengenrechner behalten ihren
bestehenden Methodenhebungsvertrag; die strenge Migration normalisiert diesen
bereits im Dekodierungspfad, ohne gerichtete Originalreferenzen zu verlieren.

Die gemeinsame Konfigurationsvorschau entfernt aktive Referenzen auf entfallende
Graphrollen und benennt Verbindungen, Standardwerte und Kartenfallbacks vor der
Übernahme. Die bestehende Kartenaktion bildet einen Undo-Schritt. Inaktive lokale
Elementdaten bleiben erhalten. Android und Desktop verwenden dieselben
Eingabeform-/Anzahlsteuerungen.

## Standardkarten und Originalrevisionen

Ein vor M4 erfasstes M3-Format-10-Payload samt damaligem Inhaltshash belegt den
Originalhashpfad. Es wurde mit den damaligen Codecs erzeugt, nicht damals auf
einem Gerät installiert. Geräteprüfungen setzen diese Bytes als historische
Installation ein und führen den tatsächlichen Installer aus. Der historische
Provenienzhash wird dabei aus dem unveränderten Original mit dem fachneutralen
Android-Serializer und der damaligen Metadatennormalisierung rekonstruiert:
Android und JVM ordnen JSON-Schlüssel unterschiedlich. Der eingefrorene JVM-Hash
bleibt separate JVM-Evidenz; ein damals auf Android erfasster Hash liegt nicht vor.

Zur Erkennung einer unveränderten Installation gilt ausschließlich der exakte Hash der höchsten
physischen Originalrevision verwendet werden. Dazu werden ausschließlich die
bisherigen Laufzeitmetadaten neutralisiert und der fachneutrale Graphcodec
verwendet. Ein mathematisch migrierter Laufzeithash allein darf eine unveränderte
Altkarte nicht als Nutzeränderung einstufen. Eine beschädigte höchste Revision
führt niemals zur Provenienzprüfung einer älteren Ersatzrevision. Geänderte,
bereits markierte und gelöschte Nutzerkarten bleiben geschützt.

Offizielle Quellenupdates erhalten auch bereits kanonische, unreferenzierte
Vorgängerrevisionen und schreiben eine neue freie lokale Revision. Die
Paketquelltemplates behalten den bestehenden Format-7-/Version-1-Vertrag;
Kartenformat 10 und Mathematik-Wire-Version bleiben unverändert.

## Nachgewiesene ergänzende Korrektur

Die gezielten Tests lösten zuerst die Geometrieanschlussarten aus und deckten
einen bestehenden statischen Initialisierungszyklus auf: Mathematik.alle las
das noch nicht initialisierte Geometrie.alle. Der mathematische Gesamtkatalog
wird nun beim Zugriff zusammengesetzt. Stabile Arten/IDs und Kataloginhalt
bleiben unverändert; keine zusätzliche Registry oder Produktionsabhängigkeit.

## Abschließende Vertragspräzisierungen

Unbeschränkte Graph-Elementeingänge verwenden den vorhandenen Atlas-Wertanschluss
mit erlaubten mathematischen Objekt- und Methodenarten. Der neutrale Methodenport
ist kein Untertyp des mathematischen Objektports. Die Laufzeit-Capability
`mathematischesObjekt` nimmt mathematische Methoden als vollständige Elemente an
und weist Scriptmethoden ab. Explizite historische Typ-/Artbeschränkungen werden
nicht als unbeschränkte Standardports erweitert. Verdrahtbarkeit und tatsächliche
verbundene Auswertung werden getrennt geprüft.

Ausführbare Graphdefinitionen übernehmen Eingangsverträge und semantische IDs.
Ihr parametrisierter Ergebnistyp wird in den Singleton-Zwischenschritten geprüft;
Anforderungen an die gesamte Menge bleiben am Ergebnisrand. Noch nicht nachgewiesene
dynamische Portregeln führen zu einer konkreten Definitionsdiagnose. Beschädigte
Ausgangsverträge erzeugen auch beim Öffnen der Definition eine Diagnose.

Der aktuelle Mathematikhash ist ausdrücklich kein alternativer Unverändert-Nachweis:
Eine Originaländerung kann durch Migration wieder auf denselben Laufzeitgraphen
normalisiert werden. Die Regression mit einer rückmigrierten Nutzeränderung muss
weiter geschützt bleiben, selbst bei gleichem Mathematikhash.

Lokale Schlussabnahme: 2213/0/1 (bestehender Skip), Lint/APKs/Test-APKs/Desktop
vollständig grün; unabhängige Review ohne weitere Findings. CI und tatsächliche
Geräte-/Linux-RPM-Abnahme am Release-SHA stehen noch aus.
