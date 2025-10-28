package com.ivotasevski.idempotency.exception;

public class MissingCompensationHandlerException extends RuntimeException {

    public MissingCompensationHandlerException(String message) {
        super(message);
    }
}
