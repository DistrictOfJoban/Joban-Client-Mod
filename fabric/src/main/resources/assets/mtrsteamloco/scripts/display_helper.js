
function DisplayHelper(cfg) {
    if (cfg === void 0) return;

    this.cfg = cfg;
    this.texture = null;
    this.ownsTexture = false;
    if (cfg.version === 1) {
        let renderType = cfg.renderType || "light";
        let dhCompat = DisplayHelperCompat.create();

        for (let slotCfg of cfg.slots) {
            let realUV = Array(4);
            realUV[0] = [slotCfg.texArea[0] / cfg.texSize[0],
                slotCfg.texArea[1] / cfg.texSize[1]];
            realUV[1] = [slotCfg.texArea[0] / cfg.texSize[0],
                (slotCfg.texArea[1] + slotCfg.texArea[3]) / cfg.texSize[1]];
            realUV[2] = [(slotCfg.texArea[0] + slotCfg.texArea[2]) / cfg.texSize[0],
                (slotCfg.texArea[1] + slotCfg.texArea[3]) / cfg.texSize[1]];
            realUV[3] = [(slotCfg.texArea[0] + slotCfg.texArea[2]) / cfg.texSize[0],
                slotCfg.texArea[1] / cfg.texSize[1]];

            let quadUV = [
                slotCfg.texArea[0] / cfg.texSize[0], // u1
                slotCfg.texArea[1] / cfg.texSize[1], // v1
                (slotCfg.texArea[0]+slotCfg.texArea[2]) / cfg.texSize[0], // u2
                (slotCfg.texArea[1]+slotCfg.texArea[3]) / cfg.texSize[1], // v2
            ];

            if (slotCfg.offsets === void 0) slotCfg.offset = [[0, 0, 0]];
            for (let offset of slotCfg.offsets) {
                for (let posCfg of slotCfg.pos) {
                    let quadDrawCall = QuadDrawCall.create()
                        .renderType(renderType.toUpperCase())
                        .corner1(new Vector3f(posCfg[0][0] + offset[0], posCfg[0][1] + offset[1], posCfg[0][2] + offset[2]))
                        .corner2(new Vector3f(posCfg[1][0] + offset[0], posCfg[1][1] + offset[1], posCfg[1][2] + offset[2]))
                        .corner3(new Vector3f(posCfg[2][0] + offset[0], posCfg[2][1] + offset[1], posCfg[2][2] + offset[2]))
                        .corner4(new Vector3f(posCfg[3][0] + offset[0], posCfg[3][1] + offset[1], posCfg[3][2] + offset[2]))
                        .uv(quadUV[0], quadUV[1], quadUV[2], quadUV[3]);
                    dhCompat.addQuad(quadDrawCall);
                }
            }
        }
        this.baseModel = dhCompat;
    } else {
        throw new Error("Unknown version: " + cfg.version);
    }
}

DisplayHelper.prototype.create = function(sharedTexture) {
    let instance = new DisplayHelper();
    if (this.cfg.version === 1) {
        if (sharedTexture !== void 0) {
            instance.texture = sharedTexture;
            instance.ownsTexture = false;
        } else {
            instance.texture = new GraphicsTexture(this.cfg.texSize[0], this.cfg.texSize[1]);
            instance.ownsTexture = true;
        }
        instance._graphics = instance.texture.graphics;

        instance.emptyTransform = instance._graphics.getTransform();
        instance.slotTransforms = {};
        for (let slotCfg of this.cfg.slots) {
            instance._graphics.transform(java.awt.geom.AffineTransform.getTranslateInstance(slotCfg.texArea[0], slotCfg.texArea[1]));
            if (slotCfg.paintingSize !== void 0) {
                instance._graphics.transform(java.awt.geom.AffineTransform.getScaleInstance(slotCfg.texArea[2] / slotCfg.paintingSize[0],
                    slotCfg.texArea[4] / slotCfg.paintingSize[1]));
            }
            instance.slotTransforms[slotCfg.name] = instance._graphics.getTransform();
            instance._graphics.setTransform(instance.emptyTransform);
        }

        instance.model = this.baseModel.copy(instance.texture.identifier);
    } else {
        throw new Error("Unknown version: " + cfg.version);
    }
    return instance;
}

DisplayHelper.prototype.upload = function() {
    if (this.ownsTexture) {
        this.texture.upload();
    }
}

DisplayHelper.prototype.close = function() {
    if (this.ownsTexture) {
        this.texture.close();
    }
}

DisplayHelper.prototype.graphics = function() {
    this._graphics.setTransform(this.emptyTransform);
    return this._graphics;
}

DisplayHelper.prototype.graphicsFor = function(slotName) {
    this._graphics.setTransform(this.slotTransforms[slotName]);
    return this._graphics;
}

DisplayHelper.prototype.drawCalls = function() {
    return this.model.getDrawCalls();
}