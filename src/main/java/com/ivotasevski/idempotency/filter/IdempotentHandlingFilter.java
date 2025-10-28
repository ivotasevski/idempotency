package com.ivotasevski.idempotency.filter;

import com.ivotasevski.idempotency.action.Action;
import com.ivotasevski.idempotency.domain.IdempotentRequestEntity;
import com.ivotasevski.idempotency.domain.IdempotentRequestStatus;
import com.ivotasevski.idempotency.repository.IdempotentRequestRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotentHandlingFilter implements Filter {

    private static final String RECORD_ID_HEADER = "X-Request-Id";
    private static final String IDEMPOTENT_ACTION_ATTR = "IdempotentAction";
    private static final String UNIQUE_IDEMPOTENCY_KEY_CONSTRAINT_NAME = "unique_gtw_idemp_x_request_id";

    // TODO: Make this configurable
    private static final int lock_ttl_seconds = 15;

    private final IdempotentRequestRepository idempotentRequestRepository;
    private final TransactionTemplate transactionTemplate;
    private final IdempotentEndpointRegistry idempotentEndpointRegistry;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        Action actionKey = idempotentEndpointRegistry.getActionForPath(httpServletRequest.getRequestURI())
                .orElse((Action) request.getAttribute(IDEMPOTENT_ACTION_ATTR));
        if (actionKey == null) {
            // non-idempotent action, skip filter logic
            chain.doFilter(request, response);
            return;
        }

        request.setAttribute(IDEMPOTENT_ACTION_ATTR, actionKey);

        var requestWrapper = getRequestWrapper(httpServletRequest);
        var responseWrapper = getResponseWrapper((HttpServletResponse) response);

        // Persist request only for original request
        // (skip Error dispatch when exception in original request is thrown)
        if (request.getDispatcherType() != DispatcherType.ERROR) {
            // persist request, or return immediate response if it exists
            if (!handleIdempotentRequest(actionKey, requestWrapper, responseWrapper)) {
                return;
            }
        }

        // Continue with the filter chain (controller, interceptors, etc.)
        chain.doFilter(requestWrapper, responseWrapper);

        // persist response
        handleResponse(requestWrapper, responseWrapper);

        // Finally copy the cached body back to the actual HttpServletResponse
        responseWrapper.copyBodyToResponse();
    }

    /*
        Return true if request was successfully saved, false if it was retrieved from cache and flushed in response.
     */
    private boolean handleIdempotentRequest(Action actionKey, ContentCachingRequestWrapper requestWrapper, ContentCachingResponseWrapper responseWrapper) throws IOException {

        String xRequestId = requestWrapper.getHeader(RECORD_ID_HEADER);

        try {
            transactionTemplate.executeWithoutResult(status -> {
                var record = new IdempotentRequestEntity();
                record.setXRequestId(xRequestId);
                record.setTrxId(UUID.randomUUID().toString());
                record.setStatus(IdempotentRequestStatus.IN_PROGRESS);
                record.setIdempotentAction(actionKey);
                // TODO: make these configurable
                record.setExpiredAt(Instant.now().plus(7, ChronoUnit.DAYS));
                record.setLockDeadline(Instant.now().plus(5, ChronoUnit.MINUTES));
                idempotentRequestRepository.save(record);
            });

        } catch (DataIntegrityViolationException e) {
            var cause = e.getCause();
            if (cause instanceof ConstraintViolationException) {
                var cvEx = (ConstraintViolationException) cause;
                if (cvEx.getConstraintName().equals(UNIQUE_IDEMPOTENCY_KEY_CONSTRAINT_NAME)) {
                    // The record exists. Based on the state, determine the response.
                    Pair<IdempotentRequestStatus, IdempotentRequestEntity> pair = transactionTemplate.execute(s -> {
                        // get record (lock it) and update if status is UNDEFINED
                        var record = idempotentRequestRepository.findByxRequestIdAndLockForUpdate(xRequestId).orElseThrow();
                        if (record.getStatus() == IdempotentRequestStatus.UNDEFINED) {
                            record.setLockDeadline(Instant.now().plus(lock_ttl_seconds, ChronoUnit.SECONDS));
                            record.setStatus(IdempotentRequestStatus.IN_PROGRESS);
                            idempotentRequestRepository.save(record);
                            return Pair.of(IdempotentRequestStatus.UNDEFINED, record);
                        }
                        return Pair.of(record.getStatus(), record);
                    });

                    // decide how to continue based on original request status
                    switch (pair.getFirst()) {
                        case IN_PROGRESS -> {
                            responseWrapper.setStatus(HttpStatus.ACCEPTED.value());
                        }
                        case UNDEFINED -> {
                            // rerun request completely
                            return true;
                        }
                        default -> {
                            IdempotentRequestEntity requestRecord = pair.getSecond();
                            responseWrapper.setStatus(requestRecord.getResponseCode());
                            // set headers
                            requestRecord.getResponseHeaders().forEach((k, v) ->
                                    v.forEach(value -> responseWrapper.addHeader(k, value)));
                            responseWrapper.getOutputStream().write(requestRecord.getResponseBody());
                        }
                    }
                    responseWrapper.copyBodyToResponse();
                    return false;
                }
            }
            throw e;
        }
        return true;
    }

    private void handleResponse(ContentCachingRequestWrapper requestWrapper, ContentCachingResponseWrapper responseWrapper) {

        String xRequestId = requestWrapper.getHeader(RECORD_ID_HEADER);

        // Collect headers
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (String name : responseWrapper.getHeaderNames()) {
            headers.put(name, new ArrayList<>(responseWrapper.getHeaders(name)));
        }
        if (responseWrapper.getContentType() != null) {
            headers.put("Content-Type", List.of(responseWrapper.getContentType()));
        }

        // Update DB
        transactionTemplate.executeWithoutResult(s -> {
            idempotentRequestRepository.findByxRequestIdAndLockForUpdate(xRequestId).ifPresent(entity -> {
                entity.setResponseCode(responseWrapper.getStatus());
                entity.setResponseBody(responseWrapper.getContentAsByteArray());
                entity.setResponseHeaders(headers);
                entity.setRequestHash(computeRequestHash(requestWrapper));
                entity.setStatus(determineStatusFromCode(responseWrapper.getStatus()));
                entity.setUpdatedAt(Instant.now());
                idempotentRequestRepository.save(entity);
            });
        });
    }

    private String computeRequestHash(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String body = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);

        // Include key headers (not all as some headers can affect the hash (Authorization, Cookie etc..)
        String contentType = request.getHeader("Content-Type");
        String idempotencyKey = request.getHeader(RECORD_ID_HEADER);

        String canonical = String.join(":",
                method,
                uri + (query != null ? "?" + query : ""),
                contentType != null ? contentType : "",
                idempotencyKey != null ? idempotencyKey : "",
                body
        );

        return DigestUtils.md5DigestAsHex(canonical.getBytes(StandardCharsets.UTF_8));
    }

    private ContentCachingRequestWrapper getRequestWrapper(HttpServletRequest request) {
        // wrap response so it can be read more than once
        if (request instanceof ContentCachingRequestWrapper) {
            return (ContentCachingRequestWrapper) request;
        }
        return new ContentCachingRequestWrapper(request);
    }

    private ContentCachingResponseWrapper getResponseWrapper(HttpServletResponse response) {
        // wrap response so it can be read more than once
        if (response instanceof ContentCachingResponseWrapper) {
            return (ContentCachingResponseWrapper) response;
        }
        return new ContentCachingResponseWrapper(response);
    }

    private IdempotentRequestStatus determineStatusFromCode(int statusCode) {
        HttpStatus status = HttpStatus.valueOf(statusCode);
        if (status.is2xxSuccessful()) {
            return IdempotentRequestStatus.SUCCESS;
        } else if (status.is4xxClientError()) {
            return IdempotentRequestStatus.PENDING_COMPENSATION;
        } else {
            return IdempotentRequestStatus.UNDEFINED;
        }
    }
}
