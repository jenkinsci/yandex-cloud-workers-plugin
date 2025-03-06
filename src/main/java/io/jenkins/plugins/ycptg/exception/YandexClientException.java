package io.jenkins.plugins.ycptg.exception;

public class YandexClientException extends RuntimeException {

    /**
     * Exception for client errors.
     * @param message - error message
     */
    public YandexClientException(final String message) {
        super(message);
    }
}
