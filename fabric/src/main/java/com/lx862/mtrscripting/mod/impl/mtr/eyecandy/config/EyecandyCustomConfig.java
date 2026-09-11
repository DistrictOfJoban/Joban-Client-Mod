package com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config;

import java.util.List;
import java.util.regex.Pattern;

public class EyecandyCustomConfig {

    private final String description;
    private final List<Entry> entries;

    public EyecandyCustomConfig(String description, List<Entry> entries) {
        this.description = description;
        this.entries = entries;
    }

    public String description() {
        return this.description;
    }

    public List<Entry> entries() {
        return this.entries;
    }

    public record Entry(String id, String name, Type type, ValidationCallback validationCallback, String defaultValue) {
        public enum Type {
            STRING,
            INTEGER,
            DOUBLE,
            BOOLEAN
        }

        public record ValidationResult(String validationMessage) {
            public boolean success() {
                return this.validationMessage == null;
            }
        }

        @FunctionalInterface
        public interface ValidationCallback {
            ValidationResult validate(String string);

            class Regex implements ValidationCallback {
                private final String validationMessage;
                private final Pattern pattern;

                public Regex(String str, String validationMessage) {
                    this.pattern = Pattern.compile(str);
                    this.validationMessage = validationMessage;
                }

                @Override
                public ValidationResult validate(String string) {
                    return pattern.matcher(string).find() ? new ValidationResult(null) : new ValidationResult(validationMessage);
                }
            }
        }
    }
}
