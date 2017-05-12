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
public class InterfaceVersion {

	private String versionName = null;

	private String receivesHash = null;

	private String sendsHash = null;

}
