package com.ivotasevski.idempotency.exception;

public class TransientException extends RuntimeException {

    public TransientException(String message) {
        super(message);
    }
}
