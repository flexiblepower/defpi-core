/**
 * File ConfigTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.util.HashMap;
import java.util.Map;

import org.flexiblepower.service.ConfigTest.GeneratedConfig;
import org.junit.Assert;
import org.junit.Test;

/**
 * ConfigTest
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 24 aug. 2017
 */
@Configurable(GeneratedConfig.class)
public class ConfigTest {

    @Test
    public void runConfigTest() {
        final Map<String, String> valueMap = new HashMap<>();
        valueMap.put("updateInterval", "7");
        valueMap.put("name", "Configuration");
        valueMap.put("invalidObjectType", "{\"key\":\"value\"}");

        final GeneratedConfig config = ServiceConfig.generateConfig(GeneratedConfig.class, valueMap);
        Assert.assertEquals("Configuration", config.getName());
        Assert.assertEquals(7, config.getUpdateInterval());

        try {
            config.incorrectlyNamedProperty();
            Assert.fail("Expected " + IllegalArgumentException.class);
        } catch (final Exception e) {
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
            Assert.assertEquals("Can only invoke 'getters'", e.getMessage());
        }

        try {
            config.getInexistentParameter();
            Assert.fail("Expected " + IllegalArgumentException.class);
        } catch (final Exception e) {
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
            Assert.assertEquals("Could not find method with name 'getInexistentParameter'", e.getMessage());
        }

        try {
            config.getInvalidObjectType();
            Assert.fail("Expected " + IllegalArgumentException.class);
        } catch (final Exception e) {
            Assert.assertEquals(IllegalArgumentException.class, e.getClass());
            Assert.assertEquals("Unable to return parameter of type 'interface java.util.Map'", e.getMessage());
        }
    }

    public static interface GeneratedConfig {

        int getUpdateInterval();

        String getName();

        double getInexistentParameter();

        float incorrectlyNamedProperty();

        Map<String, String> getInvalidObjectType();
    }

}
