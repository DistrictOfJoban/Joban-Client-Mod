"""Static + behavioural regression for the two 2026-11-21 PSD fixes.

Fix 1 (Tianjin Metro suppression): the Tianjin Metro mod is a multi-loader
build whose published jars RELOCATE all classes - the Fabric jar under
``fabric.ziyue.tjmetro.*`` and the Forge jar under ``forge.ziyue.tjmetro.*`` -
while its GitHub sources use the un-relocated ``ziyue.tjmetro.*`` names. The
TianjinRenderRouteBaseMixin originally targeted the un-relocated names, which
never exist at runtime, so the mixin silently never applied and Tianjin PSD
walls kept rendering their original content behind script canvases. This test
asserts both mixin files (fabric + forge trees) and both JcmMixinPlugin files
agree on the RELOCATED target lists, per loader prefix, with no un-relocated
name anywhere.

Fix 2 (psd_glass_2 canvas overflow): PSDWindowBounds' "mostly-frame quad"
whole-extent shortcut must stay restricted to geometrically SMALL quads - a
majority-opaque full-height pane (MTR's second glass style: transparent window
above a solid LED panel, 70 of 128 sprite rows opaque) still carries a ~47%
window and must keep it. This test asserts the span guards are present in the
source and re-runs the scan rule on synthetic half-panel textures to verify
the derived window (old rule: none -> full-face fallback -> canvas overflows;
new rule: the real window).
"""
from pathlib import Path
import re
import sys


MIXIN_FILE = 'src/main/java/com/lx862/jcm/mixin/mtrscripting/TianjinRenderRouteBaseMixin.java'
PLUGIN_FILE = 'src/main/java/com/lx862/jcm/mixin/JcmMixinPlugin.java'
WINDOW_FILE = 'src/main/java/com/lx862/jcm/mod/scripting/psd/PSDWindowBounds.java'
RENDERER_FILE = 'src/main/java/com/lx862/jcm/mod/scripting/psd/PSDScriptRenderer.java'

EXPECTED_TIERS = [
    'RenderRouteBase',
    'RenderPSDTopTianjin',
    'RenderPSDTopTianjinBMT',
    'RenderAPGGlassTianjinBMT',
    'RenderAPGGlassTianjinJinjing',
    'RenderAPGGlassTianjinTRT',
]


def extract_mixin_targets(text):
    block = re.search(r'@Mixin\(targets\s*=\s*\{(.*?)\}\s*,\s*remap', text, re.S)
    if block is None:
        block = re.search(r'@Mixin\(targets\s*=\s*\{(.*?)\}', text, re.S)
    assert block is not None, 'cannot find @Mixin(targets = {...}) in mixin source'
    return re.findall(r'"([^"]+)"', block.group(1))


def extract_plugin_array(text, name):
    block = re.search(name + r'\s*=\s*\{(.*?)\};', text, re.S)
    assert block is not None, f'cannot find {name} in plugin source'
    return re.findall(r'"([^"]+)"', block.group(1))


def check(condition, message):
    if not condition:
        raise AssertionError(message)


def test_tianjin_targets(repo):
    for loader in ('fabric', 'forge'):
        mixin = (repo / loader / MIXIN_FILE).read_text(encoding='utf8')
        plugin = (repo / loader / PLUGIN_FILE).read_text(encoding='utf8')
        targets = extract_mixin_targets(mixin)

        check(len(targets) == 12, f'{loader}: expected 12 mixin targets, got {len(targets)}')
        for target in targets:
            check(target.startswith('fabric.ziyue.tjmetro.mod.render.')
                  or target.startswith('forge.ziyue.tjmetro.mod.render.'),
                  f'{loader}: target {target} is not a relocated (fabric./forge.) Tianjin class'
                     ' - un-relocated ziyue.tjmetro.* names never exist at runtime')
            check('ziyue.tjmetro.mod.render.' in target,
                  f'{loader}: target {target} is not a Tianjin render class')
        # No un-relocated CLASS-NAME literals anywhere in either file (the
        # 2026-11-21 bug). Literals like ".ziyue.tjmetro)" are prefix-less
        # log-concatenation suffixes, not class names.
        for name, text in (('mixin', mixin), ('plugin', plugin)):
            for quoted in re.findall(r'"([^"\n]*ziyue[^"\n]*)"', text):
                check(not quoted.startswith('ziyue.'),
                      f'{loader}/{name}: un-relocated ziyue.* class literal "{quoted}"')

        fabric_set = {t for t in targets if t.startswith('fabric.')}
        forge_set = {t for t in targets if t.startswith('forge.')}
        check(len(fabric_set) == 6 and len(forge_set) == 6,
              f'{loader}: expected 6 fabric + 6 forge targets, got {len(fabric_set)}+{len(forge_set)}')
        check({t[len('fabric.'):] for t in fabric_set} == {t[len('forge.'):] for t in forge_set},
              f'{loader}: fabric and forge target sets disagree')
        check({t.rsplit('.', 1)[1] for t in fabric_set} == set(EXPECTED_TIERS),
              f'{loader}: target renderer names do not match the expected Tianjin renderers')

        plugin_fabric = extract_plugin_array(plugin, 'TIANJIN_RENDER_TARGETS_FABRIC')
        plugin_forge = extract_plugin_array(plugin, 'TIANJIN_RENDER_TARGETS_FORGE')
        check(plugin_fabric == sorted(fabric_set) or set(plugin_fabric) == fabric_set,
              f'{loader}: plugin TIANJIN_RENDER_TARGETS_FABRIC != mixin fabric targets')
        check(set(plugin_forge) == forge_set,
              f'{loader}: plugin TIANJIN_RENDER_TARGETS_FORGE != mixin forge targets')

        # The plugin must key its per-loader probe off the target's own prefix.
        check('targetClassName.startsWith("forge.")' in plugin,
              f'{loader}: plugin presence probe must select the target set by loader prefix')
    print('Tianjin hook targets: relocated fabric./forge. names, mixin/plugin sets consistent')


def scan_window(alpha, whole_rule_span_limited):
    """Re-runs PSDWindowBounds.markOcclusion's rule set over a sprite.

    alpha: list of rows, each a list of alpha values (row 0 = texture top =
    block-face top). Returns the largest unoccluded [bottom, top) span in
    block space, or None when no span of MIN_WINDOW survives.
    """
    grid = 128
    occluded = [False] * grid
    frame_rows = total_rows = 0
    for y, row in enumerate(alpha):
        total_rows += 1
        frame = sum(1 for a in row if a >= 51)
        if row and frame * 2 >= len(row):
            frame_rows += 1
            # Texture row 0 = face v 0 = block-face TOP (y 1); v grows down.
            hi = 1 - y / len(alpha)
            lo = 1 - (y + 1) / len(alpha)
            for r in range(int(lo * grid), int(hi * grid)):
                if 0 <= r < grid:
                    occluded[r] = True
    span = 1.0  # a full-height pane quad
    if frame_rows * 2 >= total_rows and (not whole_rule_span_limited or span < 0.5):
        for r in range(grid):
            occluded[r] = True
    best = (0, -1)
    run_len = run_start = 0
    for r in range(grid):
        if not occluded[r]:
            if run_len == 0:
                run_start = r
            run_len += 1
            if run_len > best[1] - best[0] + 1:
                best = (run_start, r)
        else:
            run_len = 0
    if best[1] < 0 or (best[1] - best[0] + 1) / grid < 0.05:
        return None
    return best[0] / grid, (best[1] + 1) / grid


def test_half_panel_window(repo):
    source = (repo / 'fabric' / WINDOW_FILE).read_text(encoding='utf8')

    # The whole-extent shortcuts must carry the geometric span guards.
    check(re.search(r'frameRows \* 2 >= totalRows && quad\.yMax - quad\.yMin < 0\.5F', source) is not None,
          'markOcclusion whole-extent shortcut lost its quad.yMax - quad.yMin < 0.5F span guard')
    check(re.search(r'frameCols \* 2 >= totalCols && quad\.hMax - quad\.hMin < 0\.5F', source) is not None,
          'markOcclusionH whole-extent shortcut lost its quad.hMax - quad.hMin < 0.5F span guard')
    # The renderer still consumes the derived window (no full-face hardcode).
    renderer = (repo / 'fabric' / RENDERER_FILE).read_text(encoding='utf8')
    check('PSDWindowBounds.window' in renderer,
          'PSDScriptRenderer no longer derives windows from PSDWindowBounds')

    # Behavioural replay: psd_glass_2's half-panel design (128px texture -
    # transparent window above a solid LED panel). Sampled 16 columns wide.
    transparent = [0] * 16
    opaque = [255] * 16
    half_panel = [transparent] * 58 + [opaque] * 70  # 58/128 window rows
    old = scan_window(half_panel, whole_rule_span_limited=False)
    new = scan_window(half_panel, whole_rule_span_limited=True)
    check(old is None,
          'old unrestricted shortcut should erase the half-panel window (full-face fallback)')
    check(new is not None and new[0] > 0.4 and new[1] > 0.99,
          f'half-panel window should survive as the upper ~45%, got {new}')

    # Style-1 design (small bottom frame): both rules agree.
    style1 = [transparent] * 26 + [opaque] * 6
    old1 = scan_window(style1, whole_rule_span_limited=False)
    new1 = scan_window(style1, whole_rule_span_limited=True)
    check(old1 is not None and new1 is not None and abs(old1[0] - new1[0]) < 0.01,
          'small-frame style must be unaffected by the span guard')

    # A true small strip (separate frame element, 3px tall, 100% opaque,
    # y span 3/32 < 0.5): the whole-extent shortcut still applies to it.
    strip = [opaque] * 3
    strip_result = scan_window(strip, whole_rule_span_limited=False)
    check(strip_result is None,
          'fully-opaque 3px strip must stay fully occluded under the guarded rule too')
    print(f'Half-panel window scan: old=None (full-face overflow), new={new}; small strips stay occluded')


def main():
    repo = Path(__file__).resolve().parents[2]
    test_tianjin_targets(repo)
    test_half_panel_window(repo)
    print('PSD hook targets + half-panel window regressions passed')


if __name__ == '__main__':
    main()
