package org.flexiblepower.model;

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

	private String image;

	// private String tag;

	private String created;

	private Map<Architecture, String> tags;

	public Service(String name, Set<Interface> interfaces, String fullname, String created) {
		this.name = name;
		this.interfaces = interfaces;

		final int pReg = fullname.indexOf('/');
		final int pTag = fullname.indexOf(':', pReg);
		final int pHash = fullname.indexOf('@', pTag);

		this.registry = fullname.substring(0, pReg);
		this.image = fullname.substring(pReg + 1, pTag);
		String tag = fullname.substring(pTag + 1, pHash);
		this.tags.put(getArchitectureFromTag(tag), tag);

		this.created = created;
	}

	@JsonIgnore
	public String getFullImageName(Architecture architecture) {
		return this.registry + "/" + this.image + ":" + this.tags.get(architecture);
	}

	public static Architecture getArchitectureFromTag(String tag) {
		if (tag.endsWith("-arm")) {
			return Architecture.ARM;
		} else if (tag.endsWith("-x86")) {
			return Architecture.X86;
		} else {
			// This seems to be the default
			return Architecture.X86_64;
		}
	}

	/**
	 * @param interfaceName
	 */
	public final Interface getInterface(final String interfaceName) {
		for (final Interface itf : this.interfaces) {
			if (itf.getName().equals(interfaceName)) {
				return itf;
			}
		}
		return null;
	}

}
