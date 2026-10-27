package com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.ante;

import com.lx862.mtrscripting.core.integration.VanillaTextWrapper;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.EyecandyCustomConfig;
import org.mtr.mapping.mapper.TextHelper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Consumer;

@Deprecated
public interface ConfigResponder {

    String key();
    void init(Map<String, String> map);
    List<EyecandyCustomConfig.Entry> getListEntries(Object dummy1, Object dummy2, Object dummy3);

    class TextField implements ConfigResponder {
        public Function<String, String> transformer = str -> str;
        public Function<String, Optional<VanillaTextWrapper>> errorSupplier = str -> Optional.empty();
        public Consumer<String> saveConsumer = str -> {};
        public String key;
        public String defaultValue;
        public VanillaTextWrapper name;
        public Function<String, Optional<VanillaTextWrapper[]>> tooltipSupplier = str -> Optional.empty();
        public boolean requireRestart = false;

        public TextField(String key, VanillaTextWrapper name, String defaultValue) {
            this.key = key;
            setName(name);
            setDefaultValue(defaultValue);
        }

        public TextField(String key, VanillaTextWrapper name, String defaultValue, Function<String, String> transformer, Function<String, Optional<VanillaTextWrapper>> errorSupplier, Consumer<String> saveConsumer, Function<String, Optional<VanillaTextWrapper[]>> tooltipSupplier, boolean requireRestart) {
            this.key = key;
            setName(name);
            setDefaultValue(defaultValue);
            setTransformer(transformer);
            setErrorSupplier(errorSupplier);
            setSaveConsumer(saveConsumer);
            setTooltipSupplier(tooltipSupplier);
            setRequireRestart(requireRestart);
        }
        @Override
        public void init(Map<String, String> map) {
            if (!map.containsKey(key)) {
                map.put(key, defaultValue);
            }
        }

        @Override
        public String key() {
            return key;
        }

        public TextField setTransformer(Function<String, String> transformer) {
            this.transformer = transformer;
            return this;
        }
        public TextField setErrorSupplier(Function<String, Optional<VanillaTextWrapper>> errorSupplier) {
            this.errorSupplier = errorSupplier;
            return this;
        }
        public TextField setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }
        public TextField setName(VanillaTextWrapper name) {
            this.name = name;
            return this;
        }
        public TextField setSaveConsumer(Consumer<String> saveConsumer) {
            this.saveConsumer = saveConsumer;
            return this;
        }
        public TextField setTooltipSupplier(Function<String, Optional<VanillaTextWrapper[]>> tooltipSupplier) {
            this.tooltipSupplier = tooltipSupplier;
            return this;
        }
        public TextField setRequireRestart(boolean requireRestart) {
            this.requireRestart = requireRestart;
            return this;
        }

        @Override
        public List<EyecandyCustomConfig.Entry> getListEntries(Object dummy1, Object dummy2, Object dummy3) {
            return Collections.singletonList(new EyecandyCustomConfig.Entry(key, name.impl(), EyecandyCustomConfig.Entry.Type.STRING, (str) -> {
                VanillaTextWrapper errorText = errorSupplier.apply(str).orElse(null);
                if(errorText == null) return new EyecandyCustomConfig.Entry.ValidationResult(null);
                return new EyecandyCustomConfig.Entry.ValidationResult(errorText.impl());
            }, defaultValue));
        }
    }

    class CycleToggle implements ConfigResponder {
        public List<String> values;
        public int defaultValue;
        public String key;
        public VanillaTextWrapper name;
        public Function<Integer, Optional<VanillaTextWrapper[]>> tooltipSupplier = str -> Optional.empty();
        public Consumer<Integer> saveConsumer = ind -> {};
        public boolean requireRestart = false;

        public CycleToggle(String key, VanillaTextWrapper name, int defaultValue, List<String> values) {
            this.key = key;
            setName(name);
            setDefaultValue(defaultValue);
            setValues(values);
        }

        public CycleToggle(String key, VanillaTextWrapper name, int defaultValue, List<String> values, Function<Integer, Optional<VanillaTextWrapper[]>> tooltipSupplier, Consumer<Integer> saveConsumer, boolean requireRestart) {
            this.key = key;
            setName(name);
            setDefaultValue(defaultValue);
            setValues(values);
            setTooltipSupplier(tooltipSupplier);
            setSaveConsumer(saveConsumer);
            setRequireRestart(requireRestart);
        }

        @Override
        public void init(Map<String, String> map) {
            if (!map.containsKey(key)) {
                map.put(key, values.get(0));
            }
        }

        @Override
        public String key() {
            return key;
        }

        public CycleToggle setDefaultValue(int defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public CycleToggle setName(VanillaTextWrapper name) {
            this.name = name;
            return this;
        }

        public CycleToggle setSaveConsumer(Consumer<Integer> saveConsumer) {
            this.saveConsumer = saveConsumer;
            return this;
        }

        public CycleToggle setTooltipSupplier(Function<Integer, Optional<VanillaTextWrapper[]>> tooltipSupplier) {
            this.tooltipSupplier = tooltipSupplier;
            return this;
        }

        public CycleToggle setRequireRestart(boolean requireRestart) {
            this.requireRestart = requireRestart;
            return this;
        }

        public CycleToggle setValues(List<String> values) {
            this.values = values;
            return this;
        }

        @Override
        public List<EyecandyCustomConfig.Entry> getListEntries(Object dummy1, Object dummy2, Object dummy3) {
            return Collections.emptyList();
        }
    }

    class BooleanToggle implements ConfigResponder {
        public String key;
        public VanillaTextWrapper name;
        public boolean defaultValue;
        public Function<Boolean, Optional<VanillaTextWrapper[]>> tooltipSupplier = str -> Optional.empty();
        public Consumer<Boolean> saveConsumer = bool -> {};
        public boolean requireRestart = false;

        public BooleanToggle(String key, VanillaTextWrapper name, boolean defaultValue) {
            this.key = key;
            setName(name);
            setDefaultValue(defaultValue);
        }


        public BooleanToggle(String key, VanillaTextWrapper name, boolean defaultValue, Function<Boolean, Optional<VanillaTextWrapper[]>> tooltipSupplier, Consumer<Boolean> saveConsumer, boolean requireRestart) {
            this.key = key;
            setName(name);
            setDefaultValue(defaultValue);
            setTooltipSupplier(tooltipSupplier);
            setSaveConsumer(saveConsumer);
            setRequireRestart(requireRestart);
        }

        public BooleanToggle setDefaultValue(boolean defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public BooleanToggle setName(VanillaTextWrapper name) {
            this.name = name;
            return this;
        }

        public BooleanToggle setSaveConsumer(Consumer<Boolean> saveConsumer) {
            this.saveConsumer = saveConsumer;
            return this;
        }

        public BooleanToggle setTooltipSupplier(Function<Boolean, Optional<VanillaTextWrapper[]>> tooltipSupplier) {
            this.tooltipSupplier = tooltipSupplier;
            return this;
        }

        public BooleanToggle setRequireRestart(boolean requireRestart) {
            this.requireRestart = requireRestart;
            return this;
        }

        @Override
        public void init(Map<String, String> map) {
            if (!map.containsKey(key)) {
                map.put(key, defaultValue + "");
            }
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public List<EyecandyCustomConfig.Entry> getListEntries(Object dummy1, Object dummy2, Object dummy3) {
            return Collections.singletonList(new EyecandyCustomConfig.Entry(key, name.impl(), EyecandyCustomConfig.Entry.Type.BOOLEAN, (str) -> new EyecandyCustomConfig.Entry.ValidationResult(null), String.valueOf(defaultValue)));
        }
    }

    class DoubleSlider implements ConfigResponder {
        public String key;
        public VanillaTextWrapper name;
        public double defaultValue;
        public double min;
        public double max;
        public int step;
        public Function<Double, Optional<VanillaTextWrapper[]>> tooltipSupplier = str -> Optional.empty();
        public Function<Double, VanillaTextWrapper> textGetter = str -> VanillaTextWrapper.literal(String.format("%.2f", str));
        public Consumer<Double> saveConsumer = num -> {
        };
        public boolean requireRestart = false;

        public DoubleSlider(String key, VanillaTextWrapper name, double defaultValue, double min, double max, int step, Function<Double, Optional<VanillaTextWrapper[]>> tooltipSupplier, Consumer<Double> saveConsumer, boolean requireRestart) {
            this.key = key;
            setName(name);
            setDefaultValue(defaultValue);
            setMin(min);
            setMax(max);
            setStep(step);
            setTooltipSupplier(tooltipSupplier);
            setSaveConsumer(saveConsumer);
        }

        public DoubleSlider(String key, VanillaTextWrapper name, double defaultValue, double min, double max, int step) {
            this.key = key;
            setName(name);
            setDefaultValue(defaultValue);
            setMin(min);
            setMax(max);
            setStep(step);
        }

        public DoubleSlider setDefaultValue(double defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public DoubleSlider setName(VanillaTextWrapper name) {
            this.name = name;
            return this;
        }

        public DoubleSlider setSaveConsumer(Consumer<Double> saveConsumer) {
            this.saveConsumer = saveConsumer;
            return this;
        }

        public DoubleSlider setTooltipSupplier(Function<Double, Optional<VanillaTextWrapper[]>> tooltipSupplier) {
            this.tooltipSupplier = tooltipSupplier;
            return this;
        }

        public DoubleSlider setRequireRestart(boolean requireRestart) {
            this.requireRestart = requireRestart;
            return this;
        }

        public DoubleSlider setMin(double min) {
            this.min = min;
            return this;
        }

        public DoubleSlider setMax(double max) {
            this.max = max;
            return this;
        }

        public DoubleSlider setStep(int step) {
            this.step = step;
            return this;
        }

        public DoubleSlider setTextGetter(Function<Double, VanillaTextWrapper> textGetter) {
            this.textGetter = textGetter;
            return this;
        }

        @Override
        public void init(Map<String, String> map) {
            if (!map.containsKey(key)) {
                map.put(key, defaultValue + "");
            }
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public List<EyecandyCustomConfig.Entry> getListEntries(Object dummy1, Object dummy2, Object dummy3) {
            return Collections.singletonList(new EyecandyCustomConfig.Entry(key, name.impl(), EyecandyCustomConfig.Entry.Type.DOUBLE, (str) -> {
                try {
                    double d = Double.parseDouble(str);
                    if(d < min) return new EyecandyCustomConfig.Entry.ValidationResult(TextHelper.literal("Smaller than min value " + min + "!"));
                    if(d > max) return new EyecandyCustomConfig.Entry.ValidationResult(TextHelper.literal("Bigger than max value " + max + "!"));
                    return new EyecandyCustomConfig.Entry.ValidationResult(null);
                } catch (NumberFormatException e) {
                    return new EyecandyCustomConfig.Entry.ValidationResult(TextHelper.literal("Not a double value!"));
                }
            }, String.valueOf(defaultValue)));
        }
    }
}