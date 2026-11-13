"""Compile and run production script path confinement on GitHub runners."""
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
    work = args.build.resolve() / 'script-file-path-test'
    classes = work / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    sources = [repo / 'fabric/src/main/java/com/lx862/mtrscripting/core/util/ScriptFilePath.java',
               Path(__file__).with_name('java') / 'ScriptFilePathTest.java']
    argfile = work / 'sources.txt'
    argfile.write_text('\n'.join('"' + p.as_posix() + '"' for p in sources), encoding='utf-8')
    java_home = args.java_home or os.environ.get('JAVA_HOME')

    def tool(name):
        return str(Path(java_home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if java_home else name

    subprocess.run([tool('javac'), '-J-Dfile.encoding=UTF-8', '--release', '17', '-encoding', 'UTF-8', '-d', str(classes), '@' + str(argfile)], check=True)
    subprocess.run([tool('java'), '-cp', str(classes), 'com.lx862.mtrscripting.core.util.ScriptFilePathTest'], check=True)


if __name__ == '__main__':
    main()
