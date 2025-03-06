package io.jenkins.plugins.ycptg.exception;

public class LoginFailed extends RuntimeException {

    /**
     * Exception for login errors.
     * @param cause - message error
     */
    public LoginFailed(final String cause) {
        super(cause);
    }
}
