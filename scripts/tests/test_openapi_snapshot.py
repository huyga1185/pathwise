"""Tests for scripts/openapi_snapshot.py. Run: python3 -m unittest discover -s scripts/tests"""
import io
import json
import sys
import tempfile
import unittest
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import openapi_snapshot  # noqa: E402

DOCUMENT = {"openapi": "3.1.0", "servers": [{"url": "http://localhost"}],
            "paths": {"/b": {}, "/a": {"get": {"parameters": [{"name": "z"}, {"name": "a"}]}}},
            "info": {"title": "PathWise API"}}


class NormalizeTest(unittest.TestCase):
    def test_normalize_whenGenerated_dropServersSortKeysKeepArrayOrder(self):
        text = openapi_snapshot.normalize(json.dumps(DOCUMENT))
        document = json.loads(text)
        self.assertNotIn("servers", document)
        self.assertEqual(list(document), ["info", "openapi", "paths"])
        self.assertEqual(list(document["paths"]), ["/a", "/b"])
        self.assertEqual([p["name"] for p in document["paths"]["/a"]["get"]["parameters"]], ["z", "a"])
        self.assertTrue(text.endswith("}\n"))

    def test_normalize_whenAppliedTwice_unchanged(self):
        once = openapi_snapshot.normalize(json.dumps(DOCUMENT))
        self.assertEqual(openapi_snapshot.normalize(once), once)

    def test_normalize_whenNotOpenApi_raise(self):
        with self.assertRaises(ValueError):
            openapi_snapshot.normalize('{"swagger": "2.0"}')


class CommandsTest(unittest.TestCase):
    def setUp(self):
        self.dir = Path(tempfile.mkdtemp())
        self.generated = self.dir / "generated.json"
        self.snapshot = self.dir / "api" / "openapi.json"
        self.generated.write_text(json.dumps(DOCUMENT), encoding="utf-8")

    def run_main(self, command):
        out, err = io.StringIO(), io.StringIO()
        with redirect_stdout(out), redirect_stderr(err):
            code = openapi_snapshot.main([command, "--generated", str(self.generated), "--snapshot", str(self.snapshot)])
        return code, out.getvalue(), err.getvalue()

    def test_check_whenSnapshotMissing_fail(self):
        code, _, err = self.run_main("check")
        self.assertEqual(code, 1)
        self.assertIn("missing", err)

    def test_writeThenCheck_whenGeneratedUnchanged_pass(self):
        self.assertEqual(self.run_main("write")[0], 0)
        self.assertEqual(self.run_main("check")[0], 0)
        self.assertIn("unchanged", self.run_main("write")[1])

    def test_check_whenSnapshotEditedByHand_failWithDiffAndFix(self):
        self.run_main("write")
        self.snapshot.write_text(self.snapshot.read_text().replace("PathWise API", "Edited"), encoding="utf-8")
        code, _, err = self.run_main("check")
        self.assertEqual(code, 1)
        self.assertIn("sync_openapi.sh", err)
        self.assertIn('-    "title": "Edited"', err)

    def test_check_whenBackendChanged_fail(self):
        self.run_main("write")
        changed = dict(DOCUMENT, paths={**DOCUMENT["paths"], "/c": {}})
        self.generated.write_text(json.dumps(changed), encoding="utf-8")
        self.assertEqual(self.run_main("check")[0], 1)

    def test_write_whenGeneratedMissing_fail(self):
        self.generated.unlink()
        self.assertEqual(self.run_main("write")[0], 1)


if __name__ == "__main__":
    unittest.main()
