/**
 * File User.java
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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User
 *
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Getter
@Entity
@NoArgsConstructor
public class User {

    private static final String SALT = "$salt@\"[3.c*%t<RBYA?,N\"2%[})X";

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId id;

    @Indexed(options = @IndexOptions(unique = true))
    private String username;

    private String password;

    @Setter
    private String passwordHash;

    @Setter
    @JsonIgnore
    private String authenticationToken;

    @Setter
    private String email = null;

    @Setter
    private boolean admin = false;

    public User(final String userName, final String userPass) {
        this.username = userName;
        this.password = userPass;
        this.setPasswordHash();
    }

    // public void setPassword(final String password) {
    // this.password = password;
    // this.setPasswordHash();
    // }

    public void setPasswordHash() {
        if (this.password != null) {
            this.passwordHash = User.computeUserPass(this.username, this.password);
            this.password = null;
        }
    }

    public void clearPasswordHash() {
        // For instance to export it;
        this.password = null;
        this.passwordHash = null;
    }

    @Override
    public String toString() {
        return "User [id=" + this.id + ", name=" + this.username + (this.email != null ? ", email=" + this.email : "")
                + (this.admin ? " (admin)" : "") + "]";
    }

    public static final String computeUserPass(final String name, final String password) {
        if ((name == null) || (password == null)) {
            throw new NullPointerException("Name and password must both be set to compute password hash");
        }
        return User.md5(password + name + User.SALT);
    }

    public static final String md5(final String password) {
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(password.getBytes(), 0, password.length());
            return new BigInteger(1, mDigest.digest()).toString(16);
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
