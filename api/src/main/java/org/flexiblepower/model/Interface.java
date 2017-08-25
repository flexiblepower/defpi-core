package org.flexiblepower.model;

import java.util.List;

import org.mongodb.morphia.annotations.Embedded;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@Embedded
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class Interface {

    private final String id;
    private final String name;
    private final String serviceId;

    @Embedded
    private final List<InterfaceVersion> interfaceVersions;

    private final boolean allowMultiple;
    private final boolean autoConnect;

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
