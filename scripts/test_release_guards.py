#!/usr/bin/env python3
"""Regressionen der reinen Releaseentscheidungen; echte Git-PR-Fälle separat."""
import importlib.util
import unittest
from pathlib import Path
from unittest.mock import patch


def modul(name):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(name + ".py"))
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result

folge = modul("pruefe_versionsfolge")
plan = modul("pruefe_releaseplan")

class ReleaseGuardTest(unittest.TestCase):
    def test_nur_dokumentation_erlaubt(self):
        for dateien, erlaubt in [("docs/V3_ALPHA.md", True), ("README_ger.md", True), ("app/a.kt", False), ("docs/a.md\napp/a.kt", False), ("scripts/test.py", False), ("docs/a.json", False)]:
            with self.subTest(dateien=dateien), patch.object(folge, "git", return_value=dateien):
                self.assertEqual(erlaubt, folge.dokumentationsänderung("basis"))

    def test_keine_uebersprungenen_versionen(self):
        folge.prüfe_versionsschritt("3.0.0", "3.0.1", {"version_axis": "x"})
        folge.prüfe_versionsschritt("3.0.1", "3.1.0", {"version_axis": "y"})
        for version, achse in [("3.0.2", "x"), ("3.1.1", "y"), ("3.0.1", "y")]:
            with self.subTest(version=version), self.assertRaises(SystemExit):
                folge.prüfe_versionsschritt("3.0.0", version, {"version_axis": achse})

    def test_vorgaenger_nicht_aus_body_oder_seitenbranch(self):
        with patch.object(folge, "git", return_value="abc andere Nachricht mit v3.0.0"), self.assertRaises(SystemExit):
            folge.prüfe_vorgänger({"previous_release": "3.0.0"}, "basis")
        with patch.object(folge, "git", return_value="abc v3.0.0\ndef v3.0.0"), self.assertRaises(SystemExit):
            folge.prüfe_vorgänger({"previous_release": "3.0.0"}, "basis")

import json
import subprocess
import tempfile
from pathlib import Path
from unittest.mock import patch
import unittest

class GitEreignisTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix='matlas-release-guard-')
        self.wurzel = Path(self.tmp.name)
        self.samai = Path(__file__).resolve().with_name('samai-git.sh')
        self.git('init', '--initial-branch=samai/fixture')
        self.schreibe('release/roadmap.toml', 'current_version = "3.0.0"\n[[releases]]\nversion="3.0.0"\nstatus="released"\n')
        self.schreibe('release/knotentypen.json', '["alt"]')
        self.schreibe('app/produkt.kt', 'alt')
        self.schreibe('docs/alpha.md', 'alt')
        self.commit('v3.0.0')
        self.basis = self.git('rev-parse', 'HEAD')
        self.git('branch', 'master')
        self.patcher = patch.object(folge, 'WURZEL', self.wurzel)
        self.patcher.start()
    def tearDown(self):
        self.patcher.stop()
        self.tmp.cleanup()
    def git(self, *args):
        return subprocess.check_output(['git', *args], cwd=self.wurzel, stderr=subprocess.PIPE, text=True).strip()
    def schreibe(self, name, text):
        path = self.wurzel / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)
        self.git('add', name)
    def commit(self, title):
        subprocess.run(['bash', str(self.samai), 'commit', '-m', title], cwd=self.wurzel, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    def event(self):
        return {'pull_request': {'base': {'ref': 'master', 'sha': self.basis}, 'head': {'ref': 'samai/fixture'}, 'title': 'v3.0.1 Fixture'}}
    def plan(self, version, entry=None):
        releases = [{'version': '3.0.0', 'status': 'released'}]
        if version != '3.0.0': releases.append({'version': version, 'status': 'released', **(entry or {})})
        return {'current_version': version, 'releases': releases}
    def test_dokumentation_ohne_versionssprung(self):
        self.schreibe('docs/alpha.md', 'neu')
        self.commit('Dokumentation')
        folge.prüfe_pr(self.plan('3.0.0'), {}, self.event())
    def test_produkt_ohne_versionssprung_abgewiesen(self):
        self.schreibe('app/produkt.kt', 'neu')
        self.commit('Produkt')
        with self.assertRaises(SystemExit):
            folge.prüfe_pr(self.plan('3.0.0'), {}, self.event())
    def test_neue_typen_in_x_abgewiesen(self):
        self.schreibe('release/knotentypen.json', '["alt", "neu"]')
        self.commit('Knotentyp')
        with self.assertRaises(SystemExit):
            folge.prüfe_pr(self.plan('3.0.1'), {'previous_release': '3.0.0', 'version_axis': 'x'}, self.event())
    def test_y_entspricht_exakt_registrierten_typen(self):
        self.schreibe('release/knotentypen.json', '["alt", "neu"]')
        self.commit('Knotentyp')
        entry = {'previous_release': '3.0.0', 'version_axis': 'y', 'new_node_types': ['neu']}
        event = self.event()
        event['pull_request']['title'] = 'v3.1.0 Fixture'
        folge.prüfe_pr(self.plan('3.1.0', entry), entry, event)
        entry['new_node_types'] = ['anderer']
        with self.assertRaises(SystemExit):
            folge.prüfe_pr(self.plan('3.1.0', entry), entry, event)
    def test_vorgaenger_muss_auf_master_hauptlinie_liegen(self):
        self.schreibe('docs/alpha.md', 'Seitentitel')
        self.commit('v3.0.1')
        with self.assertRaises(SystemExit):
            folge.prüfe_vorgänger({'previous_release': '3.0.1'}, self.basis)


class HistorienTest(unittest.TestCase):
    def test_historische_eintraege_unveraenderlich(self):
        alt = {"current_version": "3.0.0", "releases": [{"version": "3.0.0", "status": "released", "title": "alt"}]}
        for releases in [[], [{"version": "3.0.0", "status": "released", "title": "umgeschrieben"}]]:
            with self.subTest(releases=releases), self.assertRaises(SystemExit):
                plan.prüfe_historie(alt, {"current_version": "3.0.0", "releases": releases})
        plan.prüfe_historie(alt, alt)

    def test_blockierte_reservierung_darf_nicht_umgangen_werden(self):
        alt = {"current_version": "3.0.0", "releases": [{"version": "3.0.1", "status": "blocked"}]}
        for status in ["planned", "released", "skipped"]:
            with self.subTest(status=status), self.assertRaises(SystemExit):
                plan.prüfe_historie(alt, {"current_version": "3.1.0", "releases": [{"version": "3.0.1", "status": status}]})
        with self.assertRaises(SystemExit):
            plan.prüfe_historie(alt, {"current_version": "3.0.0", "releases": []})
        plan.prüfe_historie(alt, {"current_version": "3.0.1", "releases": [{"version": "3.0.1", "status": "released"}]})

    def test_blockiert_sperrt_hoehere_veroeffentlichung(self):
        import copy
        x = copy.deepcopy(plan.lade_plan())
        x["releases"][-1]["status"] = "blocked"
        x["current_version"] = "3.1.0"
        x["releases"].append({"version": "3.1.0", "status": "released", "title": "Test", "roadmap": "Test", "previous_release": "3.0.0"})
        with self.assertRaises(SystemExit): plan.prüfe_plan(x)

class ZusätzlicheHistorieTest(unittest.TestCase):
    def test_keine_veroeffentlichung_oberhalb_current(self):
        import copy
        x = copy.deepcopy(plan.lade_plan())
        x['releases'].append({'version': '3.1.0', 'status': 'released', 'title': 'Erfunden', 'roadmap': 'Erfunden', 'previous_release': x['current_version']})
        with self.assertRaises(SystemExit): plan.prüfe_plan(x)

    def test_nur_zielversion_darf_neu_veroeffentlicht_werden(self):
        basis = {'current_version': '3.0.0', 'releases': [{'version': '3.0.0', 'status': 'released'}]}
        ziel = {'current_version': '3.0.1', 'releases': basis['releases'] + [{'version': '3.0.1', 'status': 'released'}]}
        plan.prüfe_historie(basis, ziel)
        for status in ['released', 'superseded', 'skipped']:
            with self.subTest(status=status), self.assertRaises(SystemExit):
                plan.prüfe_historie(basis, {**ziel, 'releases': ziel['releases'] + [{'version': '3.1.0', 'status': status}]})

class MasterPushTest(unittest.TestCase):
    setUp = GitEreignisTest.setUp
    tearDown = GitEreignisTest.tearDown
    git = GitEreignisTest.git
    schreibe = GitEreignisTest.schreibe
    commit = GitEreignisTest.commit
    plan = GitEreignisTest.plan
    def test_zusaetzlicher_versionstitel_im_dokumentationspush_verboten(self):
        self.schreibe('docs/alpha.md', 'eins'); self.commit('v9.9.9')
        self.schreibe('docs/alpha.md', 'zwei'); self.commit('Dokumentation')
        with self.assertRaises(SystemExit), patch.dict('os.environ', {'GITHUB_REF_NAME': 'master'}):
            folge.prüfe_master_push(self.plan('3.0.0'), {'before': self.basis})

if __name__ == "__main__": unittest.main()
