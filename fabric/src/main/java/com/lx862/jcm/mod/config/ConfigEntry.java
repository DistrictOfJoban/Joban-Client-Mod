package com.lx862.jcm.mod.config;

import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.MutableText;

public enum ConfigEntry {
    DISABLE_RENDERING("disable_rendering", Boolean.class,false, TextUtil.translatable(TextCategory.GUI, "config.listview.title.disable_rendering"), TextUtil.literal("This disables the rendering of all JCM Blocks")),
    USE_CUSTOM_FONT("custom_font", Boolean.class,false, TextUtil.translatable(TextCategory.GUI, "config.listview.title.custom_font"), TextUtil.literal("Use custom font if available")),
    DEBUG_MODE("debug_mode", Boolean.class,false, TextUtil.translatable(TextCategory.GUI, "config.listview.title.debug_mode"), TextUtil.literal("This enables debug mode, usually used by developer or to troubleshoot issues")),
    NEW_TEXT_RENDERER("new_text_renderer", Boolean.class,false, TextUtil.translatable(TextCategory.GUI, "config.listview.title.new_text_rendering"), TextUtil.literal("This enables a new texture-atlas based font rendering technique which may improve performance and quality, but might be unstable"));

    private Object value;
    private final Object defaultValue;
    private final MutableText name;
    private final String keyName;
    private final MutableText description;
    private final Class<?> targetClass;

    ConfigEntry(String keyName, Class<?> targetClass, Object defaultValue, MutableText name, MutableText description) {
        this.keyName = keyName;
        this.name = name;
        this.description = description;
        this.targetClass = targetClass;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public void set(Object newValue) {
        this.value = newValue;
    }

    public MutableText getTitle() {
        return name;
    }

    public MutableText getDescription() {
        return description;
    }

    public String getKeyName() {
        return keyName;
    }

    public int getInt() {
        return (int)value;
    }

    public String getString() {
        return (String)value;
    }

    public boolean getBool() {
        return (boolean)value;
    }

    public boolean is(Class<?> cls) {
        return targetClass == cls;
    }
    public void reset() {
        set(defaultValue);
    }
}
