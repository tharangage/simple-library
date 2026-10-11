package com.ascendion.roshan.simple_library.dto;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The signed-in user's profile. The same shape for every user, whatever their roles:
 * the identity comes from the token, and {@code borrower} holds the library details that belong only to
 * members (the Borrower record and how many books they hold). It is {@code null} for a caller who is not a
 * member, for example a desk-only admin.
 */
public record UserProfileResponse(
        String userId,
        String firstname,
        String lastname,
        String email,
        MyBorrowerResponse borrower) {

    public static UserProfileResponse of(final Jwt jwt, final MyBorrowerResponse borrower) {
        return new UserProfileResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                jwt.getClaimAsString("email"),
                borrower);
    }
}
