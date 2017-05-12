package org.flexiblepower.model;

public enum Architecture {
	UNKNOWN, X86, X86_64, ARM;

	public static Architecture fromString(final String text) {
		for (final Architecture a : Architecture.values()) {
			if (a.toString().equalsIgnoreCase(text)) {
				return a;
			}
		}
		return Architecture.UNKNOWN;
	}
}
