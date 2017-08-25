package org.flexiblepower.model;

import java.util.List;

import org.mongodb.morphia.annotations.Embedded;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Embedded
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Interface {

    private String id;
    private String name = null;
    private String serviceId = null;

    @Embedded
    private List<InterfaceVersion> interfaceVersions = null;

    private boolean allowMultiple = false;
    private boolean autoConnect = false;

    public boolean isCompatibleWith(final Interface other) {
        if (this.interfaceVersions != null) {
            for (final InterfaceVersion iv : this.interfaceVersions) {
                for (final InterfaceVersion oiv : other.getInterfaceVersions()) {
                    if (iv.isCompatibleWith(oiv)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public InterfaceVersion getInterfaceVersionByName(final String interfaceName) {
        if (this.interfaceVersions != null) {
            for (final InterfaceVersion iv : this.interfaceVersions) {
                if (interfaceName.equals(iv.getVersionName())) {
                    return iv;
                }
            }
        }
        return null;
    }

}
