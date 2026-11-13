"""Exercise the production shader-pass detector against independent optional APIs.

Each scenario runs in a fresh JVM so static method discovery is tested with
Iris present, absent, partially implemented, or failing during initialization.
No Minecraft runtime or resource-pack contents are needed.
"""
import argparse
import os
from pathlib import Path
import subprocess


FALLBACK = """package org.mtr.mapping.render.shader;
public final class ModShaderHandler {
    public static boolean liveShadow;
    public static int calls;
    public static boolean renderingShadows() { calls++; return liveShadow; }
}
"""
API = """package net.irisshaders.iris.api.v0;
public final class IrisApi {
    public static boolean pack, shadow, failPack, failShadow;
    public static IrisApi getInstance() { FACTORY }
    PACK_METHOD
    SHADOW_METHOD
}
"""
PACK = """public boolean isShaderPackInUse() {
    if (failPack) throw new IllegalStateException("pack failure"); return pack;
}"""
SHADOW = """public boolean isRenderingShadowPass() {
    if (failShadow) throw new IllegalStateException("pass failure"); return shadow;
}"""


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build', required=True, type=Path)
    parser.add_argument('--java-home', type=Path)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    work = args.build.resolve() / 'shader-pass-test'

    def tool(name):
        java_home = args.java_home or os.environ.get('JAVA_HOME')
        return str(Path(java_home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if java_home else name

    for scenario in ('full', 'no_pack', 'no_shadow', 'absent', 'broken_factory'):
        target = work / scenario
        classes = target / 'classes'
        classes.mkdir(parents=True, exist_ok=True)
        files = {
            'org/mtr/mapping/render/shader/ModShaderHandler.java': FALLBACK,
        }
        if scenario != 'absent':
            files['net/irisshaders/iris/api/v0/IrisApi.java'] = (API
                .replace('FACTORY', 'throw new IllegalStateException("factory failure");' if scenario == 'broken_factory' else 'return new IrisApi();')
                .replace('PACK_METHOD', '' if scenario == 'no_pack' else PACK)
                .replace('SHADOW_METHOD', '' if scenario == 'no_shadow' else SHADOW))
        sources = []
        for relative, content in files.items():
            path = target / 'src' / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding='utf8')
            sources.append(path)
        sources += [repo / 'fabric/src/main/java/com/lx862/jcm/mod/util/ShaderDetector.java',
                    repo / 'native/bench/java/ShaderPassTest.java']
        arguments = target / 'sources.txt'
        arguments.write_text('\n'.join('"' + str(path).replace('\\', '/') + '"' for path in sources), encoding='utf8')
        subprocess.run([tool('javac'), '-J-Dfile.encoding=UTF-8', '-encoding', 'UTF-8', '--release', '17',
                        '-d', str(classes), '@' + str(arguments)], check=True)
        subprocess.run([tool('java'), '-cp', str(classes), 'com.lx862.jcm.test.ShaderPassTest', scenario], check=True)


if __name__ == '__main__':
    main()
