package org.flexiblepower.orchestrator.pendingchange;

import java.util.Date;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Entity("PendingChange") // All subclasses must have the exact same annotation!
public abstract class PendingChange implements Runnable {

    public static enum Result {
        SUCCESS,
        FAILED_TEMPORARY,
        FAILED_PERMANENTLY
    }

    public static enum State {
        NEW,
        FAILED_TEMPORARY,
        FAILED_PERMANENTLY,
        FINISHED
    }

    @Id
    protected ObjectId id;

    private Date created;

    private long retryIntervalMs;

    private Date runAt;

    Date obtainedAt;

    private ObjectId userId;

    private int count;

    @Getter
    private State state;

    public PendingChange() {

    }

    public PendingChange(final long delayMs, final long retryIntervalMs, final ObjectId userId) {
        final long now = System.currentTimeMillis();
        this.created = new Date(now);
        this.retryIntervalMs = retryIntervalMs;
        this.runAt = new Date(now + delayMs);
        this.obtainedAt = null;
        this.state = State.NEW;
        this.userId = userId;
        this.count = 0;
    }

    public abstract String description();

    public abstract Result execute();

    @Override
    public void run() {
        Result result;
        try {
            result = this.execute();
        } catch (final Throwable e) {
            PendingChange.log.error("Error while executing PendingChange, marking it as failed permanently", e);
            result = Result.FAILED_PERMANENTLY;
        }
        this.count += 1;
        switch (result) {
        case FAILED_PERMANENTLY:
            this.state = State.FAILED_PERMANENTLY;
            break;
        case FAILED_TEMPORARY:
            this.state = State.FAILED_TEMPORARY;
            this.runAt = new Date(this.runAt.getTime() + this.retryIntervalMs);
            break;
        case SUCCESS:
            this.state = State.FINISHED;
            break;
        default:
            break;
        }
        PendingChangeManager.getInstance().update(this);
    }

}
