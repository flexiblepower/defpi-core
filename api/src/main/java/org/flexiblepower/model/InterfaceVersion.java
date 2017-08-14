/**
 * File InterfaceVersion.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import org.mongodb.morphia.annotations.Embedded;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * InterfaceVersion
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 * @see #Interface
 */
@Getter
@Embedded
@AllArgsConstructor
@NoArgsConstructor
public class InterfaceVersion implements Comparable<InterfaceVersion> {

	private String versionName = null;

	private String receivesHash = null;

	private String sendsHash = null;

	public boolean isCompatibleWith(InterfaceVersion other) {
		return receivesHash.equals(other.getSendsHash()) && sendsHash.equals(other.getReceivesHash());
	}

	@Override
	public int compareTo(InterfaceVersion o) {
		return versionName.compareTo(o.getVersionName());
	}

}
