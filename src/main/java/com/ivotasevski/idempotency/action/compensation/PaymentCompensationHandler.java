package com.ivotasevski.idempotency.action.compensation;

import com.ivotasevski.idempotency.action.Action;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentCompensationHandler implements CompensationHandler {

    @Override
    public Action getSupportedAction() {
        return Action.PAYMENT;
    }

    @Override
    public void handle(String idempotentKey) {
        log.info("Compensating action:{} for idempotent key: {}", getSupportedAction(), idempotentKey);

    }
}
