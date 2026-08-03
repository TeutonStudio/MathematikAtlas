#!/usr/bin/env python3
from pathlib import Path

pfad = Path("release/roadmap.toml")
text = pfad.read_text(encoding="utf-8")
text = text.replace('current_series = "2.20.x"', 'current_series = "2.21.x"', 1)
text = text.replace('current_version = "2.20.1"', 'current_version = "2.21.0"', 1)

version = 'version = "2.21.0"'
if version not in text:
    text = text.rstrip() + '''

[[releases]]
version = "2.21.0"
title = "Universeller Zahlenrechner und automatische Migration"
roadmap = "v2.21.x Zahlenrechner und CAS-Grundlage"
status = "released"
previous_release = "2.20.1"
branch = "agent/v2.21.0-universeller-zahlenrechner"
kind = "feature"
version_axis = "y"
new_node_types = ["mathematik.zahlenrechner"]
reason = "Führt den einzigen erzeugbaren Zahlenrechner-Knotentyp mit persistierten Operatorzuständen, Zahlbereichsdefinitionen, Rechenregeln und verlustfreier Migration der historischen Zahl-zu-Zahl-Rechnerknoten ein."
''' + "\n"

pfad.write_text(text, encoding="utf-8")
