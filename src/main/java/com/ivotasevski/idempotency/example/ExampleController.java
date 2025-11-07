package com.ivotasevski.idempotency.example;

import com.ivotasevski.idempotency.action.Action;
import com.ivotasevski.idempotency.action.IdempotentAction;
import com.ivotasevski.idempotency.exception.PermanentException;
import com.ivotasevski.idempotency.exception.TransientException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/example")
public class ExampleController {

    @PostMapping("/2xx")
    @IdempotentAction(action = Action.PAYMENT)
    public Map<String, Object> success() {
        return Map.of("idempotentAction", UUID.randomUUID().toString());
    }

    @SneakyThrows
    @PostMapping("/2xx/long")
    @IdempotentAction(action = Action.PAYMENT)
    public Map<String, Object> longRunning2xx() {
        Thread.sleep(15000);
        return Map.of("idempotentAction", UUID.randomUUID().toString());
    }

    @SneakyThrows
    @PostMapping("/permanent")
    @IdempotentAction(action = Action.PAYMENT)
    public Map<String, Object> permanentError() {
        throw new PermanentException("This is intentional permanent exception");
    }

    @PostMapping("/transient")
    @IdempotentAction(action = Action.PAYMENT)
    public Map<String, Object> transientError() {
        throw new TransientException("This is intentional transient exception");
    }

    // this endpoint should not go through the filter as it is not annotated
    @PostMapping("/non-idempotent")
    public Map<String, Object> nonIdempotent() {
        return Map.of("normalAction", UUID.randomUUID().toString());
    }
}
