package com.ivotasevski.idempotency.action.compensation;

import com.ivotasevski.idempotency.action.Action;
import com.ivotasevski.idempotency.exception.DuplicateCompensationHandlerException;
import com.ivotasevski.idempotency.exception.MissingCompensationHandlerException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CompensationHandlerRegistry {

    private final Map<Action, CompensationHandler> handlers = new HashMap<>();

    public CompensationHandlerRegistry(List<CompensationHandler> handlers) {
        handlers.forEach(handler -> {
            if (this.handlers.containsKey(handler.getSupportedAction())) {
                throw new DuplicateCompensationHandlerException("Duplicate compensation handler for action: " + handler.getSupportedAction() + " found.");
            }
            this.handlers.put(handler.getSupportedAction(), handler);
        });
    }

    public CompensationHandler getHandler(Action action) {
        CompensationHandler handler = handlers.get(action);
        if (handler == null) {
            throw new MissingCompensationHandlerException("No handler for type: " + action);
        }
        return handler;
    }
}
