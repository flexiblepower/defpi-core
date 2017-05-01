/**
 * File RegistryConnectorTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.io.IOException;
import java.util.Set;

import org.flexiblepower.model.Interface;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.Gson;

/**
 * RegistryConnectorTest
 *
 * @author coenvl
 * @version 0.1
 * @since May 1, 2017
 */
public class RegistryConnectorTest {

    private final Gson gson = new Gson();

    @Test
    public void testParse() throws JsonParseException, JsonMappingException, IOException {
        final String str = "[{\"name\":\"Echo Interface\",\"cardinality\":1,\"autoConnect\":false,\"subscribeHash\":\"eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252\",\"publishHash\":\"eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252\"}]";

        System.out.println(str);

        final Set<Object> set = this.gson.fromJson(str, Set.class);

        for (final Object o : set) {
            final Interface a = this.gson.fromJson(this.gson.toJson(o), Interface.class);
            System.out.println(a);
        }
    }

}
