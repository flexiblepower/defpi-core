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
import lombok.ToString;

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
@ToString
public class InterfaceVersion implements Comparable<InterfaceVersion> {

    private String versionName = null;
    private String receivesHash = null;
    private String sendsHash = null;

    public boolean isCompatibleWith(final InterfaceVersion other) {
        return this.receivesHash.equals(other.getSendsHash()) && this.sendsHash.equals(other.getReceivesHash());
    }

    @Override
    public int compareTo(final InterfaceVersion o) {
        return this.versionName.compareTo(o.getVersionName());
    }

}
