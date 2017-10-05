/**
 * File ServiceExecutor.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ServiceExecutor
 *
 * @author coenvl
 * @version 0.1
 * @since Sep 27, 2017
 */
public class ServiceExecutor {

    private static final Logger log = LoggerFactory.getLogger(ServiceExecutor.class);
    private static ServiceExecutor instance;
    private final ExecutorService executor;

    public synchronized static ServiceExecutor getInstance() {
        if (ServiceExecutor.instance == null) {
            ServiceExecutor.instance = new ServiceExecutor();
        }
        return ServiceExecutor.instance;
    }

    private ServiceExecutor() {
        final ThreadFactory threadFactory = r -> new Thread(r, "dEF-Pi userThread " + ServiceMain.threadCount++);
        this.executor = Executors.newSingleThreadExecutor(threadFactory);
    }

    /**
     * @param object
     */
    public void submit(final Runnable task) {
        this.executor.submit(() -> {
            try {
                task.run();
            } catch (final Exception e) {
                ServiceExecutor.log.error("Error occured while executing user code: {}", e.getMessage());
                ServiceExecutor.log.trace(e.getMessage(), e);
            }
        });
    }

    /**
     * @param object
     */
    public <T> Future<T> submit(final Callable<T> task) {
        return this.executor.submit(() -> {
            try {
                return task.call();
            } catch (final Exception e) {
                ServiceExecutor.log.error("Error occured while executing user code: {}", e.getMessage());
                ServiceExecutor.log.trace(e.getMessage(), e);
                throw e;
            }
        });
    }

    public void shutDown() {
        this.executor.shutdownNow();
        ServiceExecutor.instance = null;
    }

}
