#!/usr/bin/env python3
"""Gibt fehlgeschlagene Gradle/JUnit-Tests kompakt für GitHub Actions aus."""

from pathlib import Path
import xml.etree.ElementTree as ET

gefunden = False
for pfad in sorted(Path(".").glob("**/build/test-results/test/*.xml")):
    try:
        wurzel = ET.parse(pfad).getroot()
    except ET.ParseError:
        continue

    fehler = []
    for fall in wurzel.findall(".//testcase"):
        knoten = fall.find("failure")
        if knoten is None:
            knoten = fall.find("error")
        if knoten is None:
            continue
        fehler.append(
            (
                fall.get("classname", "?"),
                fall.get("name", "?"),
                knoten.get("message", ""),
                (knoten.text or "").strip(),
            )
        )

    if not fehler:
        continue

    gefunden = True
    print(f"::group::{pfad}")
    for klasse, name, nachricht, details in fehler:
        print(f"FAIL {klasse}.{name}")
        if nachricht:
            print(nachricht)
        if details:
            print(details[:6000])
        print()
    print("::endgroup::")

if not gefunden:
    print("Keine JUnit-XML-Fehlerberichte gefunden.")
