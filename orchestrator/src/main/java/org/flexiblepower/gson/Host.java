package org.flexiblepower.gson;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Host {

    private String id;
    private String state;
    private String hostname;
    private String ip;
    private String user;
    private Map<String, String> labels;

    public Host(final String id, final String hostname, final String ip) {
        this.id = id;
        this.hostname = hostname;
        this.ip = ip;
        this.labels = new HashMap<>();
    }
}
