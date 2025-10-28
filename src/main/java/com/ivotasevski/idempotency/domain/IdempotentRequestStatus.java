package com.ivotasevski.idempotency.domain;

public enum IdempotentRequestStatus {
    IN_PROGRESS,
    UNDEFINED,
    SUCCESS,
    FAILURE,
    PENDING_COMPENSATION,
    IN_COMPENSATION
}
