package com.ascendion.roshan.simple_library.dto;

import com.ascendion.roshan.simple_library.entity.Borrower;

/** What a new user gets back: no password, no mobile number. */
public record RegistrationResponse(String id, String firstName, String lastName, String email) {

    public static RegistrationResponse from(final Borrower borrower) {
        return new RegistrationResponse(
                borrower.getId(), borrower.getFirstname(), borrower.getLastname(), borrower.getEmail());
    }
}
