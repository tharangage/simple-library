package com.ascendion.roshan.simple_library.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps exceptions to HTTP responses. Expected client errors (4xx) are logged at WARN/DEBUG
 * without a stack trace; only unexpected errors (500) are logged at ERROR with the stack trace.
 */
@ControllerAdvice
@Slf4j
public class CustomResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<Object> handleNotFound(final NotFoundException ex, final WebRequest request) {
        log.warn("Not found: {}", ex.getMessage());
        return super.handleExceptionInternal(ex, ex.getMessage(),
                new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Object> handleIllegalState(final IllegalStateException ex, final WebRequest request) {
        log.warn("Conflict: {}", ex.getMessage());
        return super.handleExceptionInternal(ex, ex.getMessage(),
                new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> handleDataIntegrity(final DataIntegrityViolationException ex, final WebRequest request) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getClass().getSimpleName());
        return super.handleExceptionInternal(ex, "Data integrity violation",
                new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    /** Another request holds the row lock (e.g. two people borrowing the same book at once). */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    ResponseEntity<Object> handleLockFailure(final PessimisticLockingFailureException ex, final WebRequest request) {
        log.warn("Lock conflict: {}", ex.getClass().getSimpleName());
        return super.handleExceptionInternal(ex, "Resource is being updated by another request, please retry",
                new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    /** Unknown property in a request parameter such as {@code ?sort=foo}. */
    @ExceptionHandler(PropertyReferenceException.class)
    ResponseEntity<Object> handlePropertyReference(final PropertyReferenceException ex, final WebRequest request) {
        log.debug("Invalid property reference in request");
        return super.handleExceptionInternal(ex, "Unknown property: " + ex.getPropertyName(),
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @Override
    @Nullable
    protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException ex,
                                                                  final HttpHeaders headers,
                                                                  final HttpStatusCode status,
                                                                  final WebRequest request) {
        log.debug("Validation failed for {} field(s)", ex.getBindingResult().getFieldErrorCount());
        final Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> String.valueOf(error.getDefaultMessage()),
                        (first, second) -> first,      // several constraints on one field: keep the first
                        LinkedHashMap::new));
        return this.handleExceptionInternal(ex, errors, headers, status, request);
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<Object> handleRuntime(final RuntimeException ex, final WebRequest request) {
        log.error("Unhandled exception", ex);
        return super.handleExceptionInternal(ex, "Internal server error",
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
