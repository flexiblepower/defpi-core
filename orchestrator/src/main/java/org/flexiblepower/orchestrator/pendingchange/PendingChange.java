/**
 * File PendingChange.java
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

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;

import lombok.Getter;
import lombok.Setter;

@Entity("PendingChange") // All subclasses must have the exact same annotation!
@Indexes({@Index(fields = @Field("state")), @Index(fields = @Field("runAt")), @Index(fields = @Field("obtainedAt"))})
public abstract class PendingChange {

    private static final int DEFAULT_MINIMUM_RETRY_INTERVAL_MILLISECONDS = 5000;
    private static final int DEFAULT_MAXIMUM_RETRY_INTERVAL_MILLISECONDS = 21600;
    private static final int DEFAULT_MAX_RETRY_COUNT = 1000;
    private static final int DEFAULT_DELAY_MILLISECONDS = 0;

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

    // Default constructor for morphia
    protected PendingChange() {

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
    @SuppressWarnings("static-method")
    public long delayMs() {
        return PendingChange.DEFAULT_DELAY_MILLISECONDS;
    }

    /**
     * Time to wait before retrying after a temporary failure. Default value is 5000, overwrite if it should be
     * different.
     *
     * @return Time to wait before retrying in milliseconds.
     */
    public long retryIntervalMs() {
        return Math.min(PendingChange.DEFAULT_MINIMUM_RETRY_INTERVAL_MILLISECONDS + (2 ^ this.getCount()),
                PendingChange.DEFAULT_MAXIMUM_RETRY_INTERVAL_MILLISECONDS);
    }

    /**
     * The number of times the PendingChange should be attempted before being marked as failed permanently. Default
     * value is 1000, overwrite if it should be changed.
     *
     * @return Number of times to try before failing permanently.
     */
    @SuppressWarnings("static-method")
    public int maxRetryCount() {
        return PendingChange.DEFAULT_MAX_RETRY_COUNT;
    }

    public abstract Result execute();

    public void incrementCount() {
        this.count += 1;
    }

}
