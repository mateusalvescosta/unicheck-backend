package com.unicheck.exception;

/** Indica que um valor que precisa ser unico, como RA ou email, ja esta em uso. */
public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException(String message) {
        super(message);
    }

}
