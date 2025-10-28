package com.ivotasevski.idempotency.exception;

public class DuplicateCompensationHandlerException extends RuntimeException {

    public DuplicateCompensationHandlerException(String message) {
        super(message);
    }
}
