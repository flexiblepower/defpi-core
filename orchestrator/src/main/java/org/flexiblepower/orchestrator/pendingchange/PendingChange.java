package org.flexiblepower.orchestrator.pendingchange;

import java.util.Date;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Entity("PendingChange") // All subclasses must have the exact same annotation!
@Indexes({@Index(fields = @Field("state")), @Index(fields = @Field("runAt")), @Index(fields = @Field("obtainedAt"))})
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
    @Getter
    protected ObjectId id;

    @Getter
    private Date created;

    @Getter
    @Setter
    private Date runAt;

    Date obtainedAt;

    @Getter
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

    /**
     * The time the PendingChange should wait before starting after being submitted. Default value is 0, overwrite if
     * this should be something else.
     *
     * @return number of milliseconds to wait before starting the task
     */
    public long delayMs() {
        return 0;
    }

    /**
     * Time to wait before retrying after a temporary failure. Default value is 5000, overwrite if it should be
     * different.
     *
     * @return Time to wait before retrying in milliseconds.
     */
    public long retryIntervalMs() {
        return Math.min(5000 + (2 ^ this.getCount()), 21600);
    }

    /**
     * The number of times the PendingChange should be attempted before being marked as failed permanently. Default
     * value is 1000, overwrite if it should be changed.
     *
     * @return Number of times to try before failing permanently.
     */
    public int maxRetryCount() {
        return 1000;
    }

    public abstract Result execute();

    public void incrementCount() {
        this.count += 1;
    }

}
