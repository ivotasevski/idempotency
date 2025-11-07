package com.ivotasevski.idempotency.filter;

import com.ivotasevski.idempotency.action.Action;
import com.ivotasevski.idempotency.action.IdempotentAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotentEndpointRegistry implements InitializingBean {

    private final RequestMappingHandlerMapping handlerMapping;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private Map<String, Map<HttpMethod, Action>> idempotentEndpoints;

    @Override
    public void afterPropertiesSet() {
        Map<String, Map<HttpMethod, Action>> endpoints = new HashMap<>();
        handlerMapping.getHandlerMethods().forEach((mapping, handlerMethod) -> {
            if (handlerMethod.hasMethodAnnotation(IdempotentAction.class)) {
                Set<String> patterns = mapping.getPatternValues();
                Set<RequestMethod> requestMethods = mapping.getMethodsCondition().getMethods();
                IdempotentAction annotation = handlerMethod.getMethodAnnotation(IdempotentAction.class);
                for (String pattern : patterns) {
                    endpoints
                            .computeIfAbsent(pattern, k -> new HashMap<>()) // ensure inner map exists
                            .putAll(requestMethods.stream()
                                    .collect(HashMap::new,
                                            (map, rm) -> map.put(HttpMethod.valueOf(rm.name()), annotation.action()),
                                            HashMap::putAll));
                }
            }
        });

        idempotentEndpoints = Map.copyOf(endpoints);

        idempotentEndpoints.forEach((path, methods) ->
                methods.forEach((method, action) ->
                        log.info("Registered idempotent endpoint {} {} → {}", method, path, action)));

    }

    public Optional<Action> getActionForPathAndMethod(String path, HttpMethod method) {
        return idempotentEndpoints.entrySet().stream()
                .filter(e -> pathMatcher.match(e.getKey(), path))
                .map(Map.Entry::getValue)
                .map(m -> m.get(method))
                .filter(Objects::nonNull)
                .findFirst();
    }
}