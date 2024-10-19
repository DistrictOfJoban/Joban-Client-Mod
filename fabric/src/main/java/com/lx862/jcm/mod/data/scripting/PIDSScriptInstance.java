package com.lx862.jcm.mod.data.scripting;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.scripting.base.ScriptInstance;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mtr.mapping.mapper.GraphicsHolder;

import java.util.ArrayList;
import java.util.List;

public class PIDSScriptInstance extends ScriptInstance {
    public List<PIDSComponent> components;

    public PIDSScriptInstance(Scriptable scope) {
        super(scope);
        this.components = new ArrayList<>();
    }

    public void execute(GraphicsHolder graphicsHolder, PIDSBlockEntity be) {
        execute(() -> {
            try {
                Context cx = Context.enter();
                cx.setLanguageVersion(Context.VERSION_ES6);
                ScriptContext ctx = new ScriptContext();
                PIDSScriptObject pidsContext = new PIDSScriptObject(graphicsHolder, be.getCustomMessages(), be.getRowHidden(), be.platformNumberHidden());
                Object renderFunction = scope.get("render", scope);
                Object[] renderParams = new Object[]{ctx, null, pidsContext};

                if(renderFunction instanceof Scriptable && renderFunction != Scriptable.NOT_FOUND) {
                    ((Function)renderFunction).call(cx, scope, scope, renderParams);
                }
                components.clear();
                components.addAll(pidsContext.getDrawCalls());
            } finally {
                Context.exit();
            }
        });
    }
}
