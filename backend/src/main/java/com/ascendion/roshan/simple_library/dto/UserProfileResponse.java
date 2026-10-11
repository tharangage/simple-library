package com.ascendion.roshan.simple_library.dto;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The signed-in user's profile. The same shape for every user, whatever their roles:
 * the identity (name and email) comes from the token, and {@code borrower} holds only what belongs to library
 * members (Borrower id, mobile, how many books they hold). It is {@code null} for a caller who is not a
 * member, for example a desk-only admin.
 */
public record UserProfileResponse(
        String userId,
        String firstname,
        String lastname,
        String email,
        BorrowerProfile borrower) {

    public static UserProfileResponse of(final Jwt jwt, final BorrowerProfile borrower) {
        return new UserProfileResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                jwt.getClaimAsString("email"),
                borrower);
    }
}
