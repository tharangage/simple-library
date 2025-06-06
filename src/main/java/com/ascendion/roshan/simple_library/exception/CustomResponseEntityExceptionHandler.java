package com.ascendion.roshan.simple_library.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

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

    @ExceptionHandler({RuntimeException.class})
    ResponseEntity<Object> handleRuntime(final RuntimeException ex, final WebRequest request) {
        log.error("Handled RuntimeException", ex);
        return super.handleExceptionInternal(ex, "Internal server error",
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
