"""Validate release jars contain the mod and bridges, with no user resource pack."""
import argparse, json, re, zipfile
from pathlib import Path

def verify(path, version=None):
    required=['NativeApiSession','NativeContentDriver','NativeContentSnapshot','NativeDisplayHelper','NativeMatrixApplier',
              'NativeVehicleDriver','NativeSnapshot','NativeHost','NativeScriptManager']
    forbidden=[b'wr2a03',b'jslcd_',b'yanyang-jslcd-pack',b'wr2-a03','若益宛'.encode(), '鸿雷行书'.encode(),b'Sans Lao']
    with zipfile.ZipFile(path) as jar:
        names=jar.namelist(); assert jar.testzip() is None,path
        for name in names:
            assert not any(token in name.encode().lower() for token in forbidden),(path,name)
            if name.endswith(('.class','.json','.js','.md','.txt')):
                data=jar.read(name).lower()
                assert not any(token.lower() in data for token in forbidden),(path,name)
        for name in required: assert 'com/lx862/jcm/nativeapi/'+name+'.class' in names,(path,name)
        bridges=[name for name in names if name.startswith('assets/jcm/natives/') and name.endswith(('.dll','.so','.dylib'))]
        assert len(bridges)==4,(path,bridges)
        assert {name.split('/')[3] for name in bridges}=={'windows-x64','linux-x64','macos-x64','macos-arm64'}
        assert not any(name.startswith('assets/') and name.endswith(('.cpp','.hpp','.so','.dll','.dylib')) and name not in bridges for name in names)
        config=json.loads(jar.read('mtrscripting.mixins.json'))
        for mixin in ['GraphicsHolderMatrixMixin','GlStateTrackerAccessor','RenderEyeCandyMixin','RenderLiftsMixin','RenderVehiclesMixin']:
            assert 'mtrscripting.'+mixin in config['client'],(path,mixin)
            assert 'com/lx862/jcm/mixin/mtrscripting/'+mixin+'.class' in names,(path,mixin)
        data=jar.read('com/lx862/mtrscripting/mod/impl/mtr/vehicle/VehicleWrapper$Stop$RouteInterchange.class')
        assert b'getTransportMode' in data and b'org/mtr/core/data/TransportMode' in data
        metadata=json.loads(jar.read('build_metadata.json'))
        if version: assert metadata['version']==version,(path,metadata)
        assert re.fullmatch(r'1\.(17\.1|18\.2|19\.2|19\.4|20\.1|20\.4)',metadata['minecraftVersion']),metadata
    return {'jar':path.name,'version':metadata['version'],'minecraft':metadata['minecraftVersion'],'bridges':4,'resource_pack_scan':'passed'}

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('paths',nargs='+',type=Path);parser.add_argument('--version');args=parser.parse_args()
    print(json.dumps([verify(path,args.version) for path in args.paths],indent=2))
