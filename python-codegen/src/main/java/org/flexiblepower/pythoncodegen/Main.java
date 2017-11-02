/**
 * File Main.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.pythoncodegen;

import lombok.extern.slf4j.Slf4j;

/**
 * Main
 *
 * @author coenvl
 * @version 0.1
 * @since Oct 23, 2017
 */
@Slf4j
public class Main {

    public static void main(final String[] args) {
        final PythonCodegen codeGenerator = new PythonCodegen();
        try {
            codeGenerator.run();
        } catch (final Exception e) {
            Main.log.error(e.getMessage());
            Main.log.trace(e.getMessage(), e);
        }
    }

}
