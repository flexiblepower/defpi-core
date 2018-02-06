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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ServiceExecutor is singleton object that the service library will use to schedule user code. This makes sure that all
 * user code is initially run in one single thread, unless the user specifically creates another thread.
 * <p>
 * Besides being a singleton object, it also provides functionality to make sure all exceptions in user code are caught
 * and logged, but do not interfere with the functionality of the service library.
 *
 * @version 0.1
 * @since Sep 27, 2017
 */
public class ServiceExecutor {

    private static final Logger log = LoggerFactory.getLogger(ServiceExecutor.class);
    private static ServiceExecutor instance;
    private final ExecutorService executor;

    /**
     * This is used to keep track of the threads created by the ServiceExecutor. It is incremented when a new thread is
     * created, which by design should only occur once! However, by keeping this counter we can show that this was
     * indeed the case by looking in the logs
     */
    private static int threadCount = 0;

    /**
     * @return the singleton instance of ServiceExecutor object.
     */
    public synchronized static ServiceExecutor getInstance() {
        if (ServiceExecutor.instance == null) {
            ServiceExecutor.instance = new ServiceExecutor();
        }
        return ServiceExecutor.instance;
    }

    private ServiceExecutor() {
        final ThreadFactory threadFactory = r -> new Thread(r, "dEF-Pi userThread " + ServiceExecutor.threadCount++);
        this.executor = Executors.newSingleThreadExecutor(threadFactory);
    }

    /**
     * Submit a Task to run as soon as possible.
     *
     * @param task the task to run.
     * @see ExecutorService#submit(Runnable)
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
     * Submit a value-returning task to run as soon as possible.
     *
     * @param task the Callable task to run.
     * @return a Future that may be used to obtain the result of the Callable object.
     * @see ExecutorService#submit(Callable)
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

    /**
     * Shuts down the ServiceExecutor and the corresponding ThreadPool. Also removes the reference to the singleton
     * instance.
     */
    public void shutDown() {
        this.executor.shutdownNow();
        ServiceExecutor.instance = null;
    }

}
