package com.ivotasevski.idempotency.exception;

public class PermanentException extends RuntimeException {

    public PermanentException(String message) {
        super(message);
    }
}
