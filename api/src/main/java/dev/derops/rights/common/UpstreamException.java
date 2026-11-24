package dev.derops.rights.common;

/** The external metadata service failed or timed out. */
public class UpstreamException extends RuntimeException {
    public UpstreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
