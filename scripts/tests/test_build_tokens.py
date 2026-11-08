"""Tests for scripts/build_tokens.py. Run: python3 -m unittest discover -s scripts/tests"""
import io
import json
import re
import sys
import tempfile
import unittest
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import build_tokens  # noqa: E402


def css_block(css: str, selector: str) -> str:
    start = css.index(selector + " {")
    return css[start:css.index("}", start)]


class RealTokensTest(unittest.TestCase):
    """The committed tokens.json produces complete themes."""

    @classmethod
    def setUpClass(cls):
        cls.tokens = build_tokens.load()
        cls.css = build_tokens.build_css(cls.tokens)
        cls.dart = build_tokens.build_dart(cls.tokens)

    def test_buildCss_whenRealTokens_everyColourAndShadowInBothThemes(self):
        light = css_block(self.css, ':root, [data-theme="light"]')
        dark = css_block(self.css, '[data-theme="dark"]')
        for token in self.tokens["color"]["tokens"] + self.tokens["shadow"]["tokens"]:
            values = build_tokens.theme_values(token, ["light", "dark"])
            self.assertIn(f"--{token['name']}: {values['light'].lower() if values['light'].startswith('#') else values['light']};", light)
            self.assertIn(f"--{token['name']}: {values['dark'].lower() if values['dark'].startswith('#') else values['dark']};", dark)

    def test_buildCss_whenRealTokens_spacingRadiusGradientAndFontPresent(self):
        root = css_block(self.css, "\n:root")
        for family in ("spacing", "radius", "gradient"):
            for token in self.tokens[family]["tokens"]:
                self.assertRegex(root, rf"--{re.escape(token['name'])}: [^;]+;")
        self.assertIn("--radius-lg: 16px;", root)
        self.assertIn("--font-sans:", root)

    def test_buildCss_whenRealTokens_oneClassPerTypeStyle(self):
        for group in self.tokens["type"]["groups"]:
            for style in group["styles"]:
                self.assertIn(f".pw-type-{style['name']} {{", self.css)
        self.assertIn("text-transform: uppercase;", self.css[self.css.index(".pw-type-label"):])

    def test_buildDart_whenRealTokens_lightAndDarkHaveEveryColour(self):
        for theme in ("light", "dark"):
            block = self.dart[self.dart.index(f"static const {theme} = PwColorTokens("):]
            block = block[:block.index(");")]
            for token in self.tokens["color"]["tokens"]:
                self.assertIn(f"    {build_tokens.camel(token['name'])}: Color(0x", block)
        self.assertIn("route: Color(0xFF0B6A5C)", self.dart)
        self.assertIn("static const double radiusLg = 16;", self.dart)
        self.assertIn("static const label = TextStyle(", self.dart)


class ParsingTest(unittest.TestCase):
    def test_parseColor_whenHexOrRgba_returnChannelsAndAlpha(self):
        self.assertEqual(build_tokens.parse_color("#0b6a5c"), (11, 106, 92, 1.0))
        self.assertEqual(build_tokens.parse_color("#fff"), (255, 255, 255, 1.0))
        self.assertEqual(build_tokens.parse_color("rgba(11, 106, 92, 0.10)"), (11, 106, 92, 0.1))

    def test_parseColor_whenNamedOrVar_raise(self):
        for value in ("red", "var(--route)", "#12345", "rgb(300, 0, 0)"):
            with self.subTest(value=value), self.assertRaises(build_tokens.TokenError):
                build_tokens.parse_color(value)

    def test_dartColor_whenAlpha_encodeAarrggbb(self):
        self.assertEqual(build_tokens.dart_color((11, 106, 92, 0.1)), "Color(0x1A0B6A5C)")

    def test_parseShadow_whenInsetOrMalformed_raise(self):
        for value in ("inset 0 1px 2px #000", "0 1px", "big"):
            with self.subTest(value=value), self.assertRaises(build_tokens.TokenError):
                build_tokens.parse_shadow(value)

    def test_dartGradient_when180deg_runTopToBottom(self):
        gradient = build_tokens.dart_gradient("linear-gradient(180deg, #000000 0%, #ffffff 100%)")
        self.assertIn("begin: Alignment(0, -1), end: Alignment(0, 1)", gradient)
        self.assertIn("stops: [0, 1]", gradient)

    def test_letterSpacing_whenEm_scaleByFontSize(self):
        self.assertAlmostEqual(build_tokens.parse_letter_spacing("0.04em", 12), 0.48)


class FileModesTest(unittest.TestCase):
    def setUp(self):
        self.dir = Path(tempfile.mkdtemp())
        self.web = self.dir / "web" / "tokens.css"
        self.mobile = self.dir / "mobile" / "tokens.g.dart"

    def run_main(self, *args):
        out, err = io.StringIO(), io.StringIO()
        with redirect_stdout(out), redirect_stderr(err):
            code = build_tokens.main(["--web-out", str(self.web), "--mobile-out", str(self.mobile), *args])
        return code, err.getvalue()

    def test_check_whenFilesWrittenFromSameTokens_pass(self):
        self.assertEqual(self.run_main()[0], 0)
        self.assertEqual(self.run_main("--check")[0], 0)

    def test_check_whenFileMissingOrEdited_fail(self):
        self.assertEqual(self.run_main("--check")[0], 1)
        self.run_main()
        self.web.write_text(self.web.read_text().replace("#0b6a5c", "#000000"))
        code, err = self.run_main("--check")
        self.assertEqual(code, 1)
        self.assertIn("out of date", err)

    def test_main_whenTokenInvalid_failWithoutWriting(self):
        tokens = json.loads(build_tokens.TOKENS.read_text(encoding="utf-8"))
        tokens["color"]["tokens"][0]["value"]["light"] = "red"
        bad = self.dir / "tokens.json"
        bad.write_text(json.dumps(tokens), encoding="utf-8")
        code, err = self.run_main("--tokens", str(bad))
        self.assertEqual(code, 1)
        self.assertIn("unsupported colour", err)
        self.assertFalse(self.web.exists())

    def test_load_whenDuplicateName_raise(self):
        tokens = json.loads(build_tokens.TOKENS.read_text(encoding="utf-8"))
        tokens["spacing"]["tokens"].append({"name": "route", "value": "4px"})
        bad = self.dir / "tokens.json"
        bad.write_text(json.dumps(tokens), encoding="utf-8")
        with self.assertRaises(build_tokens.TokenError):
            build_tokens.load(bad)


if __name__ == "__main__":
    unittest.main()
