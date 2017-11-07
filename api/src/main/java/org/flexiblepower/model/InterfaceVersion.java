/**
 * File InterfaceVersion.java
 *
 * Copyright 2017 FAN
 */
package org.flexiblepower.model;

import org.mongodb.morphia.annotations.Embedded;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * InterfaceVersion
 *
 * @version 0.1
 * @since 20 mrt. 2017
 * @see #Interface
 */
@Value
@Embedded
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class InterfaceVersion implements Comparable<InterfaceVersion> {

    private String versionName;
    private String receivesHash;
    private String sendsHash;

    public boolean isCompatibleWith(final InterfaceVersion other) {
        return this.receivesHash.equals(other.getSendsHash()) && this.sendsHash.equals(other.getReceivesHash());
    }

    @Override
    public int compareTo(final InterfaceVersion o) {
        return this.versionName.compareTo(o.getVersionName());
    }

}
