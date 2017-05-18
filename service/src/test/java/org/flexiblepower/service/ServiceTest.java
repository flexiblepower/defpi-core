/**
 * File ServiceTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.Serializable;
import java.util.Properties;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ServiceTest
 *
 * @author coenvl
 * @version 0.1
 * @since May 12, 2017
 */
public class ServiceTest implements Service {

    private static final Logger log = LoggerFactory.getLogger(ServiceTest.class);

    @Test
    public void runTest() {
        final ServiceManager mgr = new ServiceManager(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#resumeFrom(java.lang.Object)
     */
    @Override
    public void resumeFrom(final Serializable state) {
        ServiceTest.log.info("ResumeFrom is called!");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#init(java.util.Properties)
     */
    @Override
    public void init(final Properties props) {
        ServiceTest.log.info("Init is called!");

    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#modify(java.util.Properties)
     */
    @Override
    public void modify(final Properties props) {
        ServiceTest.log.info("Modify is called!");

    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#suspend()
     */
    @Override
    public Serializable suspend() {
        ServiceTest.log.info("Suspend is called!");
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#terminate()
     */
    @Override
    public void terminate() {
        ServiceTest.log.info("Terminate is called!");

    }

}
