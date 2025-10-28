package com.ivotasevski.idempotency.filter;

import com.ivotasevski.idempotency.action.Action;
import com.ivotasevski.idempotency.action.IdempotentAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotentEndpointRegistry implements InitializingBean {

    private final RequestMappingHandlerMapping handlerMapping;

    private final Map<String, Action> idempotentEndpoints = new HashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public void afterPropertiesSet() {
        handlerMapping.getHandlerMethods().forEach((mapping, handlerMethod) -> {
            Set<String> patterns = mapping.getPatternValues();
            if (handlerMethod.hasMethodAnnotation(IdempotentAction.class)) {
                IdempotentAction annotation = handlerMethod.getMethodAnnotation(IdempotentAction.class);
                patterns.forEach(p -> idempotentEndpoints.put(p, annotation.action()));
            }
        });

        idempotentEndpoints.forEach((path, key) ->
                log.info("Registered idempotent endpoint {} → {}", path, key));
    }

    public Optional<Action> getActionForPath(String path) {
        return idempotentEndpoints.entrySet().stream()
                .filter(e -> pathMatcher.match(e.getKey(), path))
                .map(Map.Entry::getValue)
                .findFirst();
    }
}