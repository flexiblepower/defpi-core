package org.flexiblepower.model;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Service
 *
 * @author coenvl
 * @version 0.1
 * @since Mar 30, 2017
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {

	private String name;

	/**
	 * Def-pi interfaces
	 */
	private Set<Interface> interfaces;

	// /**
	// * Volume mappings
	// */
	// private Set<String> mappings;
	//
	// /**
	// * Physical ports to open
	// */
	// private Set<String> ports;

	private String registry;

	private String repository;

	private String id;

	// private String tag;

	private String version;

	private Date created;

	private Map<Architecture, String> tags;

	public Service(String name, Set<Interface> interfaces, String fullname, String version, Date created) {
		this.name = name;
		this.interfaces = interfaces;

		final int pReg = fullname.indexOf('/');
		final int pTag = fullname.indexOf(':', pReg);
		final int pHash = fullname.indexOf('@', pTag);

		this.registry = fullname.substring(0, pReg);
		this.id = fullname.substring(pReg + 1, pTag);
		String tag = fullname.substring(pTag + 1, pHash);
		this.tags.put(getArchitectureFromTag(tag), tag);

		this.version = version;

		this.created = created;
	}

	@JsonIgnore
	public String getFullImageName(Architecture architecture) {
		return this.registry + "/" + this.repository + "/" + getImageName() + ":" + this.tags.get(architecture);
	}

	@JsonIgnore
	public String getImageName() {
		return this.id.split(":")[0];
	}

	public static Architecture getArchitectureFromTag(String tag) {
		if (tag.endsWith("-arm")) {
			return Architecture.ARM;
		} else if (tag.endsWith("-x86")) {
			return Architecture.X86;
		} else if (!tag.contains("-")) {
			// 64bit is default
			return Architecture.X86_64;
		} else {
			// There is an architecture defined, but we don't know it
			return Architecture.UNKNOWN;
		}
	}

	/**
	 * @param interfaceId
	 */
	public final Interface getInterface(final String interfaceId) {
		for (final Interface itf : this.interfaces) {
			if (itf.getId().equals(interfaceId)) {
				return itf;
			}
		}
		return null;
	}

}
