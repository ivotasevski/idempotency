package com.ivotasevski.idempotency.example;

import com.ivotasevski.idempotency.action.Action;
import com.ivotasevski.idempotency.action.IdempotentAction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
    @PostMapping("/4xx")
    @IdempotentAction(action = Action.PAYMENT)
    public Map<String, Object> e4xx() {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad Request");
    }

    @PostMapping("/5xx")
    @IdempotentAction(action = Action.PAYMENT)
    public Map<String, Object> e5xx() {
        throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Gateway Timeout");
    }

    // this endpoint should not go through the filter as it is not annotated
    @PostMapping("/non-idempotent")
    public Map<String, Object> nonIdempotent() {
        return Map.of("normalAction", UUID.randomUUID().toString());
    }
}
