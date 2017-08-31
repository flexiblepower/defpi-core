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
@SuppressWarnings("static-method")
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

    @Test
    public void testTypes() {
        final Map<String, String> valueMap = new HashMap<>();
        valueMap.put("bool", "True");
        valueMap.put("char", " ");
        valueMap.put("byte", "0x7F");
        valueMap.put("short", "32767");
        valueMap.put("int", "123");
        valueMap.put("long", "-100000000");
        valueMap.put("float", "2e-2");
        valueMap.put("double", "3.1415926535897932384");
        valueMap.put("string", "");

        final GeneratedConfig config = ServiceConfig.generateConfig(GeneratedConfig.class, valueMap);
        Assert.assertEquals(true, config.getBool());
        Assert.assertEquals(' ', config.getChar());
        Assert.assertEquals(127, config.getByte());
        Assert.assertEquals(Short.MAX_VALUE, config.getShort());
        Assert.assertEquals(123, config.getInt());
        Assert.assertEquals((long) -1e8, config.getLong());
        Assert.assertEquals((float) 0.02, config.getFloat(), 1e-3);
        Assert.assertEquals(Math.PI, config.getDouble(), 1e-10);
        Assert.assertEquals("", config.getString());
    }

    public static interface GeneratedConfig {

        boolean getBool();

        char getChar();

        byte getByte();

        short getShort();

        int getInt();

        long getLong();

        float getFloat();

        double getDouble();

        String getString();

        int getUpdateInterval();

        String getName();

        double getInexistentParameter();

        float incorrectlyNamedProperty();

        Map<String, String> getInvalidObjectType();
    }

}