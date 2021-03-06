/*-
 * #%L
 * dEF-Pi API
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
package org.flexiblepower.model;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The user is the object that represents the owner of a process the corresponding connections, and/or private nodes. A
 * user can login to the orchestrator interface and view its process to list/add or remove them.
 * <p>
 * A user can be an administrator in which case he can see all processes, including those of other administrators. Also,
 * as an administrator the user can perform operations to manage the dEF-Pi environment.
 *
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Must be generated for Morphia
public class User {

    private static final String SALT = "$salt@\"[3.c*%t<RBYA?,N\"2%[})X";

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId id;

    @Indexed(options = @IndexOptions(unique = true))
    private String username;

    private transient String password;

    @Setter
    private String passwordHash;

    @Setter
    @JsonIgnore
    @Deprecated
    private String authenticationToken;

    @Setter
    private String email = null;

    @Setter
    private boolean admin = false;

    /**
     * Create a user with the provided username and password. Note that the password will not be stored in the database,
     * as it is transient, only the hash is stored persistently. All authorization operations will have to be performed
     * on the password hash.
     *
     * @param userName The name of the user
     * @param userPass The password the user can identify himself with.
     */
    public User(final String userName, final String userPass) {
        this.username = userName;
        this.password = userPass;
        this.setPasswordHash();
    }

    /**
     * Update the user's password hash if the password is not equal to null. i.e. this function can only be used from
     * the constructor, or if the user is created using reflection and has a password field.
     */
    public void setPasswordHash() {
        if (this.password != null) {
            this.passwordHash = User.computeUserPass(this.username, this.password);
            this.password = null;
        }
    }

    /**
     * Clear the password and the hash from the User before we serialize it. This may also be done using JSON
     * annotations but this way we can know for sure. This is used for instance when listing users via REST
     */
    public void clearPasswordHash() {
        this.password = null;
        this.passwordHash = null;
    }

    @Override
    public String toString() {
        return "User [id=" + this.id + ", name=" + this.username + (this.email != null ? ", email=" + this.email : "")
                + (this.admin ? " (admin)" : "") + "]";
    }

    /**
     * Compute the user password hash based on a combination of the user name and a password. This is a one-way
     * function, meaning that given the name and password we can compute the hash, but it is impossible to do the
     * reverse.
     * <p>
     * This function uses the SHA256 algorithm to compute hashes.
     *
     * @param name The user name
     * @param password The user password
     * @return The hash that can be checked.
     */
    public static final String computeUserPass(final String name, final String password) {
        if ((name == null) || (password == null)) {
            throw new NullPointerException("Name and password must both be set to compute password hash");
        }
        return User.sha256(password + name + User.SALT);
    }

    private static String sha256(final String password) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(password.getBytes());
            return new BigInteger(1, md.digest()).toString(16);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof User) {
            final User otherUser = (User) other;
            if ((this.id == null) && (otherUser.id == null)) {
                return this.username.equals(otherUser.username);
            } else {
                return ((User) other).id == this.id;
            }
        } else {
            return super.equals(other);
        }
    }

    @Override
    public int hashCode() {
        if (this.id != null) {
            return this.id.hashCode();
        } else {
            return this.username.hashCode();
        }
    }

}
