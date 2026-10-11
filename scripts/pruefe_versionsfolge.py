#!/usr/bin/env python3
"""Prüft den Git- und Pull-Request-Kontext der aktuellen Releaseversion."""

from __future__ import annotations

import json
import hashlib
import os
import re
import subprocess
import sys
import tomllib
from pruefe_releaseplan import prüfe_historie
from pathlib import Path

WURZEL = Path(__file__).resolve().parents[1]
PLAN_PFAD = WURZEL / "release" / "roadmap.toml"
FINALER_TITEL = re.compile(r"^v(\d+\.\d+\.\d+)$")


def fehler(text: str) -> None:
    print(f"Versionsfolge-Fehler: {text}", file=sys.stderr)
    raise SystemExit(1)


def git(*argumente: str, erforderlich: bool = True) -> str:
    ergebnis = subprocess.run(
        ["git", *argumente],
        cwd=WURZEL,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if erforderlich and ergebnis.returncode != 0:
        fehler(f"git {' '.join(argumente)} fehlgeschlagen: {ergebnis.stderr.strip()}")
    return ergebnis.stdout.strip()


def lade_plan() -> tuple[dict, dict]:
    with PLAN_PFAD.open("rb") as datei:
        plan = tomllib.load(datei)
    aktuelle_version = str(plan["current_version"])
    eintrag = next(
        (eintrag for eintrag in plan["releases"] if str(eintrag["version"]) == aktuelle_version),
        None,
    )
    if eintrag is None:
        fehler(f"Kein Releaseeintrag für {aktuelle_version}.")
    return plan, eintrag


def version_in_ref(ref: str) -> str | None:
    if not ref or set(ref) == {"0"}:
        return None
    inhalt = git("show", f"{ref}:release/roadmap.toml", erforderlich=False)
    if not inhalt:
        return None
    try:
        return str(tomllib.loads(inhalt)["current_version"])
    except (KeyError, tomllib.TOMLDecodeError):
        fehler(f"Releaseplan in Git-Ref {ref!r} ist ungültig.")


def lese_event() -> dict | None:
    pfad = os.environ.get("GITHUB_EVENT_PATH")
    if not pfad:
        return None
    event_pfad = Path(pfad)
    if not event_pfad.is_file():
        fehler(f"GITHUB_EVENT_PATH {pfad!r} existiert nicht.")
    return json.loads(event_pfad.read_text(encoding="utf-8"))


def dokumentationsänderung(basis: str) -> bool:
    dateien = git("diff", "--name-only", f"{basis}..HEAD").splitlines()
    return bool(dateien) and all(
        (name.startswith("docs/") and name.endswith(".md"))
        or re.fullmatch(r"README(?:_[A-Za-z]+)?\.md", name) is not None
        for name in dateien
    )


def prüfe_vorgänger(eintrag: dict, basis: str | None = None) -> None:
    vorgänger = eintrag.get("previous_release")
    if not vorgänger:
        return
    master = basis or git("rev-parse", "--verify", "origin/master", erforderlich=False) or "master"
    zeilen = git("log", "--first-parent", "--format=%H %s", master).splitlines()
    treffer = [zeile.split(" ", 1)[0] for zeile in zeilen if zeile.split(" ", 1)[1] == f"v{vorgänger}"]
    if len(treffer) != 1:
        fehler(f"Vorgänger v{vorgänger} muss genau einmal als finaler Titel auf der master-Hauptlinie liegen.")
    if subprocess.run(["git", "merge-base", "--is-ancestor", treffer[0], "HEAD"], cwd=WURZEL).returncode:
        fehler(f"Vorgänger v{vorgänger} ist kein Vorfahr des Arbeitsstands.")


def prüfe_versionsschritt(basis: str, version: str, eintrag: dict) -> None:
    alt = tuple(map(int, basis.split(".")))
    neu = tuple(map(int, version.split(".")))
    achse = eintrag.get("version_axis")
    erwartet = {"x": (alt[0], alt[1], alt[2] + 1), "y": (alt[0], alt[1] + 1, 0)}
    if achse == "M":
        if neu != (alt[0] + 1, 0, 0) or not str(eintrag.get("reason", "")).strip():
            fehler("Ein neuer Versionsraum benötigt den nächsten Major und eine ausdrückliche Roadmap-Begründung.")
    elif neu != erwartet.get(achse):
        fehler(f"Unzulässiger Versionsschritt {basis} -> {version} für Achse {achse!r}.")


def prüfe_knotentypen(basis: str, eintrag: dict) -> None:
    # Das Manifest wird zusätzlich vom tatsächlichen kanonischen JVM-Katalog geprüft.
    pfad = "release/knotentypen.json"
    aktuell = WURZEL / pfad
    if not aktuell.exists():
        fehler("Der überprüfbare kanonische Knotentypkatalog fehlt.")
    vorher = git("show", f"{basis}:{pfad}", erforderlich=False)
    if not vorher:
        baseline = (WURZEL / "release/knotentypen-v3.0.0.json").read_bytes()
        # Einmalige Baseline des veröffentlichten v3.0.0-Katalogs, nicht parallel mit neuen Typen ändern.
        if hashlib.sha256(baseline).hexdigest() != "104d7eab96154c87a846194074486940ead7507b0e2c5acfff2ef8e22b86f0fb":
            fehler("Die festgeschriebene v3.0.0-Katalogbasis wurde verändert.")
        vorher = baseline.decode("utf-8")
    neue = set(json.loads(aktuell.read_text(encoding="utf-8"))) - set(json.loads(vorher))
    angekündigt = set(eintrag.get("new_node_types", []))
    achse = eintrag.get("version_axis")
    if achse == "x" and neue:
        fehler("Neue erzeugbare Knotentypen sind in einer x-Version unzulässig: " + ", ".join(sorted(neue)))
    if achse == "y" and (not neue or neue != angekündigt):
        fehler("Tatsächliche neue Knotentypen müssen exakt der angekündigten y-Version entsprechen.")


def prüfe_basishistorie(plan: dict, basis: str) -> None:
    inhalt = git("show", f"{basis}:release/roadmap.toml")
    try:
        vorher = tomllib.loads(inhalt)
    except tomllib.TOMLDecodeError:
        fehler("Ungültiger Releaseplan der Basis.")
    prüfe_historie(vorher, plan)


def prüfe_pr(plan: dict, eintrag: dict, event: dict) -> None:
    pull_request = event.get("pull_request")
    if not isinstance(pull_request, dict):
        fehler("Pull-Request-Event enthält keine pull_request-Daten.")

    version = str(plan["current_version"])
    basis = str(pull_request.get("base", {}).get("ref", ""))
    kopf = str(pull_request.get("head", {}).get("ref", ""))
    titel = str(pull_request.get("title", ""))
    erwarteter_branch = str(eintrag.get("branch", ""))

    if basis != str(plan.get("default_branch", "master")):
        fehler(f"Pull Request zielt auf {basis!r} statt auf master.")

    basis_sha = str(pull_request.get("base", {}).get("sha", ""))
    if not basis_sha:
        fehler("Basis-SHA des Pull Requests fehlt.")
    ergebnis = subprocess.run(
        ["git", "merge-base", "--is-ancestor", basis_sha, "HEAD"],
        cwd=WURZEL,
    )
    if ergebnis.returncode != 0:
        fehler("Der Pull-Request-Branch enthält die angegebene PR-Basis nicht als Vorfahren.")

    subjects = git("log", "--format=%s", f"{basis_sha}..HEAD").splitlines()
    zusätzliche_finale = [subject for subject in subjects if FINALER_TITEL.fullmatch(subject)]
    if zusätzliche_finale:
        fehler(
            "Der Pull-Request-Branch enthält bereits finale Versionscommits: "
            + ", ".join(zusätzliche_finale)
        )

    prüfe_basishistorie(plan, basis_sha)
    basis_version = version_in_ref(basis_sha)
    if basis_version == version:
        if not dokumentationsänderung(basis_sha):
            fehler("Produktänderungen gegen master benötigen eine eigene Releaseversion.")
        print(f"Reiner Dokumentations-PR ohne Versionswechsel: v{version}.")
        return

    if basis_version is None or str(eintrag.get("previous_release")) != basis_version:
        fehler("previous_release muss der veröffentlichten Version der PR-Basis entsprechen.")
    prüfe_versionsschritt(basis_version, version, eintrag)
    prüfe_vorgänger(eintrag, basis_sha)
    prüfe_knotentypen(basis_sha, eintrag)
    if erwarteter_branch and kopf != erwarteter_branch:
        fehler(f"Release-PR-Branch {kopf!r} stimmt nicht mit {erwarteter_branch!r} überein.")
    if not titel.startswith(f"v{version}"):
        fehler(f"Release-PR-Titel muss mit v{version} beginnen.")


def prüfe_master_push(plan: dict, event: dict | None) -> None:
    version = str(plan["current_version"])
    branch = os.environ.get("GITHUB_REF_NAME", "")
    if branch and branch != str(plan.get("default_branch", "master")):
        return

    vorherige_version = None
    if isinstance(event, dict):
        vorherige_version = version_in_ref(str(event.get("before", "")))
    if vorherige_version is not None:
        basis = str(event.get("before", ""))
        prüfe_basishistorie(plan, basis)
        finale = [titel for titel in git("log", "--format=%s", f"{basis}..HEAD").splitlines() if FINALER_TITEL.fullmatch(titel)]
        erwartet = [] if vorherige_version == version else [f"v{version}"]
        if finale != erwartet:
            fehler("Das master-Pushintervall enthält unzulässige finale Versionstitel: " + ", ".join(finale))
    if vorherige_version == version:
        if not dokumentationsänderung(str(event.get("before", ""))):
            fehler("Produktänderungen auf master ohne neue Releaseversion sind unzulässig.")
        print(f"Reiner Dokumentationscommit auf master: v{version}.")
        return

    if vorherige_version is not None:
        _, eintrag = lade_plan()
        if str(eintrag.get("previous_release")) != vorherige_version:
            fehler("Der veröffentlichte Vorgänger passt nicht zum vorherigen master-Stand.")
        prüfe_versionsschritt(vorherige_version, version, eintrag)
        prüfe_vorgänger(eintrag, str(event.get("before", "")))
        prüfe_knotentypen(str(event.get("before", "")), eintrag)
    titel = git("log", "-1", "--format=%s")
    if titel != f"v{version}":
        fehler(f"HEAD auf master muss den finalen Titel v{version!s} besitzen, gefunden wurde {titel!r}.")


def main() -> None:
    if git("rev-parse", "--is-inside-work-tree", erforderlich=False) != "true":
        fehler("Die Versionsfolge benötigt einen Git-Checkout.")

    plan, eintrag = lade_plan()
    prüfe_vorgänger(eintrag)
    event = lese_event()
    event_name = os.environ.get("GITHUB_EVENT_NAME", "")

    if event_name == "pull_request":
        if event is None:
            fehler("Pull-Request-Prüfung ohne Eventdaten.")
        prüfe_pr(plan, eintrag, event)
    elif event_name == "push":
        prüfe_master_push(plan, event)
    else:
        branch = git("branch", "--show-current", erforderlich=False)
        erwarteter_branch = str(eintrag.get("branch", ""))
        if branch and erwarteter_branch and branch not in {erwarteter_branch, "master"}:
            print(
                f"Arbeitsbranch {branch!r} verwendet weiterhin die veröffentlichte Version "
                f"v{plan['current_version']}; Release-Metadaten werden erst im Releasebranch geändert."
            )

    print(f"Versionsfolge für v{plan['current_version']} erfolgreich geprüft.")


if __name__ == "__main__":
    main()
