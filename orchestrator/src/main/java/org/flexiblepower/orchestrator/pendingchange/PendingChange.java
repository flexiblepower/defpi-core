/*-
 * #%L
 * dEF-Pi REST Orchestrator
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.flexiblepower.orchestrator.pendingchange;

import java.time.Duration;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

import lombok.Getter;
import lombok.Setter;

/**
 * PendingChange
 *
 * @version 0.1
 * @since Aug 7, 2017
 */
@Entity("PendingChange") // All subclasses must have the exact same annotation!
@Indexes({@Index(fields = @Field("state")), @Index(fields = @Field("runAt")), @Index(fields = @Field("obtainedAt"))})
public abstract class PendingChange {

    private static final long DEFAULT_MINIMUM_RETRY_INTERVAL_MILLISECONDS = Duration.ofSeconds(5).toMillis();
    private static final long DEFAULT_MAXIMUM_RETRY_INTERVAL_MILLISECONDS = Duration.ofHours(36).toMillis();
    private static final int DEFAULT_MAX_RETRY_COUNT = 1000;
    private static final int DEFAULT_DELAY_MILLISECONDS = 0;

    /**
     * Result enum is the outcome of the {@link PendingChange#execute()} function, which indicated the outcome of the
     * execution of the pending change.
     */
    public static enum Result {
        /**
         * Successfully executed the pending change, confirming its intended purpose.
         */
        SUCCESS,
        /**
         * The execution failed, but may succeed in another attempt.
         */
        FAILED_TEMPORARY,
        /**
         * The execution failed in a way that future attempts are no longer futile.
         */
        FAILED_PERMANENTLY
    }

    /**
     * The State enum is the state in which the pending change is currently in. There is a strict ordering, in that a
     * pending change can only go from new to failed_temporary to failed_permanently to finished, but may skip
     * intermediate states. It can never go back, for instance from failed_temporary to new.
     */
    public static enum State {
        /**
         * The pending change is new, has never been attempted. By definition if the state is new,
         * the count should equal 0
         */
        NEW,
        /**
         * The pending change has failed its latest attempt, but will try again in the future
         */
        FAILED_TEMPORARY,
        /**
         * The pending change has failed its latest attempt, and will not be re-attempted
         */
        FAILED_PERMANENTLY,
        /**
         * The pending change is finished. It has succeeded its last attempt, and will not be re-attempted
         */
        FINISHED
    }

    /**
     * The ObjectId of the PendingChange is used to store and retrieve it in MongoDB, and is unique for this object.
     */
    @Id
    @Getter
    protected ObjectId id;

    /**
     * The List of resources is a list of {@link ObjectId}s that refer to objects that are in some way dependent on this
     * pending change. They may point to users, processes, connections or nodes, and other pending changes that also
     * have this resource will not be executed simultaneously.
     */
    @Getter
    protected List<ObjectId> resources;

    @Getter
    private Date created;

    @Getter
    @Setter
    private Date runAt;

    /**
     * This field is used as a flag that some thread has obtained this pending change, and is working on executing it.
     * Other threads should not pick it up until it is released.
     */
    /*
     * TODO This is redundant with the implementation of the resource locks, and breaks when an orchestrator container
     * is restarted while one pending change was in a deadlock. i.e. the deadlock survives even a orchestrator restart.
     */
    Date obtainedAt;

    @Getter
    private ObjectId userId;

    @Getter
    private int count;

    @Getter
    @Setter
    private State state;

    /**
     * Default constructor for morphia
     */
    protected PendingChange() {

    }

    /**
     * Create a pending change owned by a specific user.
     *
     * @param userId The ID of the owner of the pending change
     */
    public PendingChange(final ObjectId userId) {
        final long now = System.currentTimeMillis();
        this.created = new Date(now);
        this.runAt = new Date(now + this.delayMs());
        this.obtainedAt = null;
        this.state = State.NEW;
        this.userId = userId;
        this.count = 0;
    }

    /**
     * @return A textual representation of what the pending change aims to achieve, only used during logging.
     */
    public abstract String description();

    /**
     * The time the PendingChange should wait before starting after being submitted. Default value is 0, overwrite if
     * this should be something else.
     *
     * @return number of milliseconds to wait before starting the task
     */
    @SuppressWarnings("static-method")
    protected long delayMs() {
        return PendingChange.DEFAULT_DELAY_MILLISECONDS;
    }

    /**
     * Time to wait before retrying after a temporary failure. Default value is 5000, overwrite if it should be
     * different.
     *
     * @return Time to wait before retrying in milliseconds.
     */
    protected long retryIntervalMs() {
        return Math.min(PendingChange.DEFAULT_MINIMUM_RETRY_INTERVAL_MILLISECONDS + (long) Math.pow(2, this.getCount()),
                PendingChange.DEFAULT_MAXIMUM_RETRY_INTERVAL_MILLISECONDS);
    }

    /**
     * The number of times the PendingChange should be attempted before being marked as failed permanently. Default
     * value is 1000, overwrite if it should be changed.
     *
     * @return Number of times to try before failing permanently.
     */
    @SuppressWarnings("static-method")
    protected int maxRetryCount() {
        return PendingChange.DEFAULT_MAX_RETRY_COUNT;
    }

    /**
     * Execute the pending change, try to update the system according to the type of pending change, and properties set
     * during creation.
     *
     * @return A Result enum indicating the success of the execution.
     */
    public abstract Result execute();

    /**
     * Increment the number of tries of the pending change. Should be called immediately after execution by the pending
     * change manager.
     */
    void incrementCount() {
        this.count += 1;
    }

}
