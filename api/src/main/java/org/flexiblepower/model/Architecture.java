package org.flexiblepower.model;

public enum Architecture {
    UNKNOWN,
    X86_64,
    ARM;

    public static Architecture fromString(final String text) {
        for (final Architecture a : Architecture.values()) {
            if (text.toUpperCase().startsWith(a.name())) {
                return a;
            }
        }
        return Architecture.UNKNOWN;
    }
}
