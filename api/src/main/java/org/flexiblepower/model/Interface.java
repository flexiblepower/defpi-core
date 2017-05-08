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

	private String name = null;

	@Embedded
	private List<InterfaceVersion> interfaceVersions = null;

	private boolean allowMultiple = false;

	private boolean autoConnect = false;

}
