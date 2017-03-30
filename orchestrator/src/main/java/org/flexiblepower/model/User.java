package org.flexiblepower.model;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * User
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Getter
@AllArgsConstructor
@Entity
public class User {

    private static final String SALT = "$salt@\"[3.c*%t<RBYA?,N\"2%[})X";

    @Id
    @JsonProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id = null;

    @Getter
    @JsonProperty("username")
    @Indexed(options = @IndexOptions(unique = true))
    private String name = null;

    private String password = null;

    @Setter
    private String email = null;

    @Setter
    private boolean admin = false;

    public User() {
        // Empty user for morphia
    }

    public User(final String userName, final String userPass) {
        this.name = userName;
        this.password = User.computeUserPass(userName, userPass);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "User [id=" + this.id + ", name=" + this.name + (this.email != null ? ", email=" + this.email : "")
                + (this.admin ? " (admin)" : "") + "]";
    }

    public static final String computeUserPass(final String name, final String password) {
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

}
