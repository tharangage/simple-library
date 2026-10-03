package com.ascendion.roshan.simple_library.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class CustomResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<Object> handleNotFound(final NotFoundException ex, final WebRequest request) {
        log.error("Handled NotFoundException", ex);
        return super.handleExceptionInternal(ex, ex.getMessage(),
                new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler({IllegalStateException.class})
    ResponseEntity<Object> handleIllegalState(final IllegalStateException ex, final WebRequest request) {
        log.error("Handled IllegalStateException", ex);
        return super.handleExceptionInternal(ex, ex.getMessage(),
                new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler({DataIntegrityViolationException.class})
    ResponseEntity<Object> handleDataIntegrity(final DataIntegrityViolationException ex, final WebRequest request) {
        log.error("Handled DataIntegrityViolationException", ex);
        return super.handleExceptionInternal(ex, "Data integrity violation",
                new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @Nullable
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("Handled MethodArgumentNotValidException", ex);
        final Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return this.handleExceptionInternal(ex, errors, headers, status, request);
    }

    @ExceptionHandler({RuntimeException.class})
    ResponseEntity<Object> handleRuntime(final RuntimeException ex, final WebRequest request) {
        log.error("Handled RuntimeException", ex);
        return super.handleExceptionInternal(ex, "Internal server error",
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
