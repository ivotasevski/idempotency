package com.ivotasevski.idempotency.exception;

import com.ivotasevski.idempotency.action.Action;
import com.ivotasevski.idempotency.filter.IdempotentEndpointRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@RequiredArgsConstructor
@RestControllerAdvice
public class ExceptionHandlingAdvice {

    private final IdempotentEndpointRegistry idempotentEndpointRegistry;

    // There must be a general handler for uncaught exceptions. If not present, the filter might throw an exception
    // and Spring will trigger /error handling logic (invoke the filter again)
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, WebRequest request) {
        // handle permanent errors
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage());
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("error", ex.getClass().getSimpleName());
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));
        return problemDetail;
    }


    @ExceptionHandler(TransientException.class)
    public ProblemDetail handleTransientException(TransientException ex, WebRequest request) {

        // Extract HttpServletRequest
        HttpServletRequest httpRequest = ((ServletWebRequest) request).getRequest();

        Action action = idempotentEndpointRegistry.getActionForPathAndMethod(httpRequest.getRequestURI(), HttpMethod.valueOf(httpRequest.getMethod())).orElse(null);
        if (action != null) {
            // return 503 for idempotent actions
            // handle transient errors
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                    ex.getMessage());
            problemDetail.setTitle("Transient error");
            problemDetail.setProperty("error", ex.getClass().getSimpleName());
            problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));
            return problemDetail;

        } else {
            // 500 for non-idempotent ones
            return handleGenericException(ex, request);
        }


    }
}
