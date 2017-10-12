/**
 * File ConnectionTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import org.junit.After;
import org.junit.Test;

/**
 * ConnectionTest
 *
 * @author coenvl
 * @version 0.1
 * @since May 12, 2017
 */
public class SmallTest {

    @Test
    public void doTest() {
        final String test = "tcp://123456:808";
        final String[] results = test.split("[:/]+");
        System.out.println(String.join("-", results));
    }

    @After
    public void stop() {
        System.out.println("Dit is ook mooi");
    }

}
