package com.ascendion.roshan.simple_library.dto;

import com.ascendion.roshan.simple_library.entity.Borrower;

import java.time.LocalDateTime;

/**
 * API representation of a borrower. Keeps the JPA entity out of the HTTP layer;
 * the JSON fields are the same as the entity used to produce.
 */
public record BorrowerResponse(
        String id,
        String firstname,
        String lastname,
        String email,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate) {

    public static BorrowerResponse from(final Borrower borrower) {
        return new BorrowerResponse(
                borrower.getId(),
                borrower.getFirstname(),
                borrower.getLastname(),
                borrower.getEmail(),
                borrower.getCreatedDate(),
                borrower.getLastModifiedDate());
    }
}
