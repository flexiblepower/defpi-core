package org.flexiblepower.create_service_maven_plugin;

import java.util.List;
import java.util.Map;

public class Service {

    private String name;
    private Map<String, Descriptor> descriptors;
    private List<Interface> interfaces;
    private List<String> mappings;
    private List<String> ports;

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Map<String, Descriptor> getDescriptors() {
        return this.descriptors;
    }

    public void setDescriptors(final Map<String, Descriptor> descriptors) {
        this.descriptors = descriptors;
    }

    public List<Interface> getInterfaces() {
        return this.interfaces;
    }

    public void setInterfaces(final List<Interface> interfaces) {
        this.interfaces = interfaces;
    }

    public List<String> getMappings() {
        return this.mappings;
    }

    public void setMappings(final List<String> mappings) {
        this.mappings = mappings;
    }

    public List<String> getPorts() {
        return this.ports;
    }

    public void setPorts(final List<String> ports) {
        this.ports = ports;
    }

    @Override
    public String toString() {
        return "Service [name=" + this.name + ", descriptors=" + this.descriptors + ", interfaces=" + this.interfaces
                + "]";
    }
}
