"""Compile and run the production PSD backing coordinate helper regression."""
import argparse
import os
from pathlib import Path
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build', required=True, type=Path)
    parser.add_argument('--java-home', type=Path)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    classes = args.build.resolve() / 'psd-backing-geometry-test'
    classes.mkdir(parents=True, exist_ok=True)

    def tool(name):
        java_home = args.java_home or os.environ.get('JAVA_HOME')
        return str(Path(java_home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if java_home else name

    subprocess.run([
        tool('javac'), '--release', '17', '-d', str(classes),
        str(repo / 'fabric/src/main/java/com/lx862/jcm/mod/scripting/psd/PSDBackingGeometry.java'),
        str(repo / 'native/bench/java/PSDBackingGeometryTest.java'),
    ], check=True)
    subprocess.run([tool('java'), '-cp', str(classes), 'com.lx862.jcm.mod.scripting.psd.PSDBackingGeometryTest'], check=True)


if __name__ == '__main__':
    main()
