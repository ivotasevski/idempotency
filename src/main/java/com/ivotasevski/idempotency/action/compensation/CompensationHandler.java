package com.ivotasevski.idempotency.action.compensation;

import com.ivotasevski.idempotency.action.Action;

public interface CompensationHandler {

    Action getSupportedAction();

    void handle(String idempotentKey);
}
