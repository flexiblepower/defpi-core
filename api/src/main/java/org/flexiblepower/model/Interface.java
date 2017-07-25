package org.flexiblepower.model;

import java.util.List;

import org.mongodb.morphia.annotations.Embedded;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embedded
@AllArgsConstructor
@NoArgsConstructor
public class Interface {

	private String id;

	private String name = null;

	private String serviceId = null;

	@Embedded
	private List<InterfaceVersion> interfaceVersions = null;

	private boolean allowMultiple = false;

	private boolean autoConnect = false;

	public boolean isCompatibleWith(Interface other) {
		for (InterfaceVersion iv : interfaceVersions) {
			for (InterfaceVersion oiv : other.getInterfaceVersions()) {
				if (iv.getReceivesHash().equals(oiv.getSendsHash())
						&& iv.getSendsHash().equals(oiv.getReceivesHash())) {
					return true;
				}
			}
		}
		return false;
	}

}
