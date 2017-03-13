package org.flexiblepower.create_service_maven_plugin;

public class Type {

    private String descriptor;
    private String type;
    private Descriptor descriptorObject;

    public String getDescriptor() {
        return this.descriptor;
    }

    public void setDescriptor(final String descriptor) {
        this.descriptor = descriptor;
    }

    public String getType() {
        return this.type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public Descriptor getDescriptorObject() {
        return this.descriptorObject;
    }

    public void setDescriptorObject(final Descriptor descriptorObject) {
        this.descriptorObject = descriptorObject;
    }

}
