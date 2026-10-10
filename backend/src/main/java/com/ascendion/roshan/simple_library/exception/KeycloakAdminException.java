package com.ascendion.roshan.simple_library.exception;

/** Talking to the Keycloak Admin API failed. Answered as 500; the message never contains user data. */
public class KeycloakAdminException extends RuntimeException {

    public KeycloakAdminException(final String message) {
        super(message);
    }

    public KeycloakAdminException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
