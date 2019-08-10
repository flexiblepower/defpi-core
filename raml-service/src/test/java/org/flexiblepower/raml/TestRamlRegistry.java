/**
 * File TestRamlRegistry.java
 *
 * Copyright 2019 FAN
 */
package org.flexiblepower.raml;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

/**
 * TestRamlRegistry
 *
 * @version 0.1
 * @since Aug 10, 2019
 */
public class TestRamlRegistry {

    @Test
    public void patternTest() {
        Pattern p = RamlResourceRegistry.MethodRegistry.getPattern("test");
        Assert.assertEquals("test", p.pattern());
        p = RamlResourceRegistry.MethodRegistry.getPattern("test/{id}/nogiets/{version}/zoiets");
        Assert.assertTrue(p.matcher("test/2-._0~/nogiets/hoihoi.apx#2/zoiets").matches());
    }
}
