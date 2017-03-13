package org.flexiblepower.create_service_maven_plugin;

public class Descriptor {

    private String url = null;
    private String file = "";
    private boolean upload = false;
    private String source = "";

    public Descriptor() {

    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getFile() {
        return this.file;
    }

    public void setFile(final String file) {
        this.file = file;
    }

    public boolean isUpload() {
        return this.upload;
    }

    public void setUpload(final boolean upload) {
        this.upload = upload;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(final String source) {
        this.source = source;
    }
}
