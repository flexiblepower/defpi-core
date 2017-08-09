package org.flexiblepower.orchestrator.pendingchange;

import java.util.Date;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Entity("PendingChange") // All subclasses must have the exact same annotation!
public abstract class PendingChange {

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

    @Getter
    @Setter
    private Date runAt;

    Date obtainedAt;

    private ObjectId userId;

    @Getter
    private int count;

    @Getter
    @Setter
    private State state;

    public PendingChange() {

    }

    public PendingChange(final ObjectId userId) {
        final long now = System.currentTimeMillis();
        this.created = new Date(now);
        this.runAt = new Date(now + this.delayMs());
        this.obtainedAt = null;
        this.state = State.NEW;
        this.userId = userId;
        this.count = 0;
    }

    public abstract String description();

    public abstract long delayMs();

    public abstract long retryIntervalMs();

    public abstract int maxRetryCount();

    public abstract Result execute();

    public void incrementCount() {
        this.count += 1;
    }

}
