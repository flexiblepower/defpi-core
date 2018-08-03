/**
 * File PendingChangeManager.java
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
package org.flexiblepower.orchestrator.pendingchange;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.orchestrator.pendingchange.PendingChange.Result;
import org.flexiblepower.orchestrator.pendingchange.PendingChange.State;

import lombok.extern.slf4j.Slf4j;

/**
 * PendingChangeManager
 *
 * @version 0.1
 * @since Aug 7, 2017
 */
@Slf4j
@SuppressWarnings("static-method")
public class PendingChangeManager {

    private static final int NUM_THREADS = 16;
    private static PendingChangeManager instance;

    /**
     * The wait lock is used to wait for new incoming pending changes if there are no pending changes found.
     */
    final Object waitLock = new Object();

    /**
     * The locked resources are the resources that are currently "in use", so that no other pending changes with that
     * resource will be used.
     */
    final List<ObjectId> lockedResources = new LinkedList<>();

    /*
     * A private constructor as part of the singleton design pattern
     */
    private PendingChangeManager() {
        for (int i = 0; i < PendingChangeManager.NUM_THREADS; i++) {
            new Thread(new PendingChangeRunner()).start();
        }
    }

    /**
     * @return The singleton instance of the pending change manager
     */
    public static PendingChangeManager getInstance() {
        if (PendingChangeManager.instance == null) {
            PendingChangeManager.instance = new PendingChangeManager();
        }
        return PendingChangeManager.instance;
    }

    /**
     * Submit a pending change to the backend, so that it will be executed at some point in the future. At some point it
     * will be picked up by a runner and be attempted, until it eventually succeeds, or permanently fails.
     *
     * @param pendingChange The pending change to execute
     */
    public void submit(final PendingChange pendingChange) {
        MongoDbConnector.getInstance().save(pendingChange);
        synchronized (this.waitLock) {
            this.waitLock.notify();
        }
    }

    private void release(final PendingChange pendingChange) {
        pendingChange.obtainedAt = null;
        synchronized (this.lockedResources) {
            this.lockedResources.removeAll(pendingChange.getResources());
        }
        switch (pendingChange.getState()) {
        case FAILED_PERMANENTLY:
            MongoDbConnector.getInstance().save(pendingChange);
            break;
        case FAILED_TEMPORARY:
            this.submit(pendingChange);
            break;
        case FINISHED:
            // Awesome!
            MongoDbConnector.getInstance().delete(pendingChange);
            break;
        case NEW:
            // Wut?
            break;
        default:
            // Wut?
            break;
        }
    }

    /**
     * Execute the pending change. This function will only be called by the PendingChangeRunner and this function will
     * make sure any uncaught exceptions are handled appropriately, the try-count is incremented, the state is updated,
     * the retry period is maintained, and finally the pending change is released.
     *
     * @param pc The pending change to execute
     */
    void runPendingChange(final PendingChange pc) {
        PendingChangeManager.log.debug("Running PendingChange of type " + pc.getClass().getSimpleName());

        Result result;
        try {
            result = pc.execute();
        } catch (final Throwable e) {
            PendingChangeManager.log.error("Error while executing PendingChange of type "
                    + pc.getClass().getSimpleName() + ", marking it as failed permanently", e);
            result = Result.FAILED_PERMANENTLY;
        }
        pc.incrementCount();
        switch (result) {
        case FAILED_PERMANENTLY:
            pc.setState(State.FAILED_PERMANENTLY);
            break;
        case FAILED_TEMPORARY:
            if (pc.getCount() <= pc.maxRetryCount()) {
                pc.setState(State.FAILED_TEMPORARY);
                pc.setRunAt(new Date(System.currentTimeMillis() + pc.retryIntervalMs()));
            } else {
                pc.setState(State.FAILED_PERMANENTLY);
            }
            break;
        case SUCCESS:
            pc.setState(State.FINISHED);
            break;
        default:
            break;
        }
        this.release(pc);
    }

    /**
     * Retrieve the pending change from the database with the provided object id
     *
     * @param objectId The objectId to look fo
     * @return The pending change with the provided id, or {@code null} if no such pending change exists.
     */
    public PendingChange getPendingChange(final ObjectId objectId) {
        return MongoDbConnector.getInstance().get(PendingChange.class, objectId);
    }

    /**
     * @param pendingChange the pending change to delete from the database
     */
    public void deletePendingChange(final PendingChange pendingChange) {
        MongoDbConnector.getInstance().delete(pendingChange);
    }

    /**
     * Count all pending changes currently stored in the database
     *
     * @param filter A filter to count a specific filtered subset of pending changes, may be empty
     * @return The number of pending changes that match the filter
     */
    public int countPendingChanges(final Map<String, Object> filter) {
        return MongoDbConnector.getInstance().totalCount(PendingChange.class, filter);
    }

    /**
     * Clean up all pending changes that are either lingering or are in the FAILED_PERMANENTLY state, or inactive for a
     * long period of time. Also clean up all locked resources, so that pending changes that are held back because they
     * are in use, will be released.
     *
     * @return A String containing information about the results of the cleanup action meant for user interpretation
     */
    public String cleanPendingChanges() {
        final String ret = MongoDbConnector.getInstance().cleanPendingChanges();
        PendingChangeManager.log.info(ret);
        this.lockedResources.clear();
        return ret;
    }

    private class PendingChangeRunner implements Runnable {

        /*
         * Empty package private constructor for enclosing type
         */
        PendingChangeRunner() {
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            final MongoDbConnector db = MongoDbConnector.getInstance();
            while (true) {
                PendingChange pc;
                synchronized (PendingChangeManager.this.lockedResources) {
                    pc = db.getNextPendingChange(PendingChangeManager.this.lockedResources);
                    if (pc != null) {
                        PendingChangeManager.this.lockedResources.addAll(pc.getResources());
                    }
                }

                if (pc == null) {
                    // Nothing to do, take a break...
                    synchronized (PendingChangeManager.this.waitLock) {
                        try {
                            PendingChangeManager.this.waitLock.wait(5000);
                        } catch (final InterruptedException e) {
                            // Don't care
                        }
                    }
                } else {
                    PendingChangeManager.this.runPendingChange(pc);
                }
            }
        }

    }

    /**
     * List pending changes. It is possible to paginate, sort and filter all pending changes depending on the provided
     * arguments.
     *
     * @param page The page to view
     * @param perPage The amount of pending changes to view per page, and thus the maximum amount of pending changes
     *            returned
     * @param sortDir The direction to sort the pending changes
     * @param sortField The field to sort the pending changes
     * @param filter A key/value map of filters
     * @return A list all pending changes that match the filters, or a paginated subset thereof
     */
    public List<PendingChange> listPendingChanges(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final Map<String, Object> filter) {
        return MongoDbConnector.getInstance().list(PendingChange.class, page, perPage, sortDir, sortField, filter);
    }

}
