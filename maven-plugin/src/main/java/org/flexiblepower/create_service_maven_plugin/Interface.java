package org.flexiblepower.create_service_maven_plugin;

import java.util.List;

public class Interface {

    private String name;
    private String classPrefix;
    private String descriptorClass;
    private int cardinality;
    private boolean autoConnect;
    private List<Type> subscribe;
    private List<Type> publish;

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getClassPrefix() {
        return this.classPrefix;
    }

    public void setClassPrefix(final String classPrefix) {
        this.classPrefix = classPrefix;
    }

    public String getDescriptorClass() {
        return this.descriptorClass;
    }

    public void setDescriptorClass(final String descriptorClass) {
        this.descriptorClass = descriptorClass;
    }

    public int getCardinality() {
        return this.cardinality;
    }

    public void setCardinality(final int cardinality) {
        this.cardinality = cardinality;
    }

    public boolean isAutoConnect() {
        return this.autoConnect;
    }

    public void setAutoConnect(final boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    public List<Type> getSubscribe() {
        return this.subscribe;
    }

    public void setSubscribe(final List<Type> subscribe) {
        this.subscribe = subscribe;
    }

    public List<Type> getPublish() {
        return this.publish;
    }

    public void setPublish(final List<Type> publish) {
        this.publish = publish;
    }

    @Override
    public String toString() {
        return "Service [name=" + this.name + ", classPrefix=" + this.classPrefix + ", cardinality=" + this.cardinality
                + ", autoConnect=" + this.autoConnect + ", subscribe=" + this.subscribe + ", publish=" + this.publish
                + "]";
    }

}
