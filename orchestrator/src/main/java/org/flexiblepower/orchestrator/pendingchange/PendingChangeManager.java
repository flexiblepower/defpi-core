/**
 * File ChangeManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator.pendingchange;

import java.util.Date;

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
public class PendingChangeManager {

    private static PendingChangeManager instance;
    private final MongoDbConnector db;
    private final Object waitLock = new Object();

    private PendingChangeManager() {
        this.db = MongoDbConnector.getInstance();
        new Thread(() -> {
            while (true) {
                final PendingChange pc = this.db.getNextPendingChange();
                if (pc == null) {
                    // Nothing to do, take a break...
                    synchronized (this.waitLock) {
                        try {
                            this.waitLock.wait(5000);
                        } catch (final InterruptedException e) {
                            // Don't care
                        }
                    }
                } else {
                    this.runPendingChange(pc);
                }
            }
        }).start();
    }

    public synchronized static PendingChangeManager getInstance() {
        if (PendingChangeManager.instance == null) {
            PendingChangeManager.instance = new PendingChangeManager();
        }
        return PendingChangeManager.instance;
    }

    public void submit(final PendingChange pendingChange) {
        this.db.save(pendingChange);
        synchronized (this.waitLock) {
            this.waitLock.notifyAll();
        }
    }

    public void update(final PendingChange pendingChange) {
        pendingChange.obtainedAt = null;
        switch (pendingChange.getState()) {
        case FAILED_PERMANENTLY:
            this.db.save(pendingChange);
            break;
        case FAILED_TEMPORARY:
            this.submit(pendingChange);
            break;
        case FINISHED:
            // Awesome!
            this.db.delete(pendingChange);
            break;
        case NEW:
            // Wut?
            break;
        default:
            // Wut?
            break;
        }

    }

    private void runPendingChange(final PendingChange pc) {
        PendingChangeManager.log.debug("Running PendingChange of type " + this.getClass().getSimpleName());
        Result result;
        try {
            result = pc.execute();
        } catch (final Throwable e) {
            PendingChangeManager.log.error("Error while executing PendingChange of type "
                    + this.getClass().getSimpleName() + ", marking it as failed permanently", e);
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
                pc.setRunAt(new Date(pc.getRunAt().getTime() + pc.retryIntervalMs()));
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

}
