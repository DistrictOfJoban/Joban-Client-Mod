"""Compile and exercise production stop matching in GitHub Actions."""

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
    classes = args.build.resolve() / 'route-stops-test' / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    java_home = args.java_home or os.environ.get('JAVA_HOME')

    def tool(name):
        return str(Path(java_home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if java_home else name

    sources = [
        repo / 'fabric/src/main/java/com/lx862/mtrscripting/mod/network/packet/data/RouteStopCursor.java',
        Path(__file__).with_name('java') / 'RouteStopCursorTest.java',
        repo / 'fabric/src/main/java/com/lx862/mtrscripting/mod/impl/mtr/vehicle/RoutePlatformMatcher.java',
        Path(__file__).with_name('java') / 'RoutePlatformMatcherTest.java',
    ]
    subprocess.run([tool('javac'), '--release', '17', '-encoding', 'UTF-8', '-d', str(classes),
                    *map(str, sources)], check=True)
    subprocess.run([tool('java'), '-cp', str(classes),
                    'com.lx862.mtrscripting.mod.network.packet.data.RouteStopCursorTest'], check=True)
    subprocess.run([tool('java'), '-cp', str(classes),
                    'com.lx862.mtrscripting.mod.impl.mtr.vehicle.RoutePlatformMatcherTest'], check=True)


if __name__ == '__main__':
    main()
