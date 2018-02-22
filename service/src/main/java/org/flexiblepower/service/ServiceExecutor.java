/**
 * File ServiceExecutor.java
 *
 * Copyright 2017 FAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flexiblepower.service;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ServiceExecutor
 *
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
        this.executor.shutdown();
        try {
            this.executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            this.executor.shutdownNow();
        }
        ServiceExecutor.instance = null;
    }

}
