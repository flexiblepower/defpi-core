package org.flexiblepower.exceptions;

/**
 * UserNotFoundException
 *
 * @version 0.1
 * @since Apr 13, 2017
 */
public class UserNotFoundException extends NotFoundException {

    private static final long serialVersionUID = -6841254000987456321L;

    /**
     * The message string stating that the nodepool is not found
     */
    public static final String USER_NOT_FOUND_MESSAGE = "User not found";

    /**
     * Create an exception with the default message that the user ws not found
     */
    public UserNotFoundException() {
        super(UserNotFoundException.USER_NOT_FOUND_MESSAGE);
    }

    /**
     * Create an exception with the default message that the user was not found with that name
     * 
     * @param userName the name of the user that could not be found
     */
    public UserNotFoundException(final String userName) {
        super(UserNotFoundException.USER_NOT_FOUND_MESSAGE + " with name " + userName);
    }

}
