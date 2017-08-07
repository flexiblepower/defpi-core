/**
 * File ChangeManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator.pendingchange;

import org.flexiblepower.connectors.MongoDbConnector;

/**
 * ChangeManager
 *
 * @author wilco
 * @version 0.1
 * @since Aug 7, 2017
 */
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
                    pc.run();
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

    private PendingChange getNext() {
        return this.db.getNextPendingChange();
    }

}
