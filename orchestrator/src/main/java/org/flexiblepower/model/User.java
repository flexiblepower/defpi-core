package org.flexiblepower.model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.annotation.JsonProperty;

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
public class User {

    @Id
    @JsonProperty("id")
    private final ObjectId id = null;

    @Getter
    @JsonProperty("username")
    private String name = null;

    private String password = null;

    @Setter
    private String email = null;

    @Setter
    private boolean admin = false;

    public User(final String userName, final String userPass) {
        this.name = userName;
        this.password = userPass;
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

}
