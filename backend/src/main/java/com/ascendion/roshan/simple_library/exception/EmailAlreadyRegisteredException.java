package com.ascendion.roshan.simple_library.exception;

/**
 * The email already belongs to a Keycloak user or a borrower. Extends IllegalStateException so the
 * existing handler answers 409 with the message as the body.
 */
public class EmailAlreadyRegisteredException extends IllegalStateException {

    public static final String CODE = "EMAIL_ALREADY_REGISTERED";

    public EmailAlreadyRegisteredException() {
        super(CODE);
    }

    public EmailAlreadyRegisteredException(final Throwable cause) {
        super(CODE, cause);
    }
}
