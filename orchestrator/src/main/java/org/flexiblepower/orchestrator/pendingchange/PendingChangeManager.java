/**
 * File ChangeManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator.pendingchange;

import java.util.Date;

import org.bson.types.ObjectId;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.orchestrator.pendingchange.PendingChange.Result;
import org.flexiblepower.orchestrator.pendingchange.PendingChange.State;

import lombok.extern.slf4j.Slf4j;

/**
 * ChangeManager
 *
 * @author wilco
 * @version 0.1
 * @since Aug 7, 2017
 */
@Slf4j
@SuppressWarnings("static-method")
public class PendingChangeManager {

    private static final int NUM_THREADS = 16;
    private static PendingChangeManager instance;
    protected final Object waitLock = new Object();

    private PendingChangeManager() {
        for (int i = 0; i < PendingChangeManager.NUM_THREADS; i++) {
            new Thread(new PendingChangeRunner()).start();
        }
    }

    public synchronized static PendingChangeManager getInstance() {
        if (PendingChangeManager.instance == null) {
            PendingChangeManager.instance = new PendingChangeManager();
        }
        return PendingChangeManager.instance;
    }

    public void submit(final PendingChange pendingChange) {
        MongoDbConnector.getInstance().save(pendingChange);
        synchronized (this.waitLock) {
            this.waitLock.notify();
        }
    }

    public void update(final PendingChange pendingChange) {
        pendingChange.obtainedAt = null;
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

    protected void runPendingChange(final PendingChange pc) {
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
        this.update(pc);
    }

    public PendingChange getPendingChange(final ObjectId objectId) {
        return MongoDbConnector.getInstance().get(PendingChange.class, objectId);
    }

    /**
     * @param pendingChangeId
     */
    public void deletePendingChange(final ObjectId pendingChangeId) {
        MongoDbConnector.getInstance().delete(PendingChange.class, pendingChangeId);
    }

    private class PendingChangeRunner implements Runnable {

        public PendingChangeRunner() {
            // TODO Auto-generated constructor stub
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
                final PendingChange pc = db.getNextPendingChange();
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

}
