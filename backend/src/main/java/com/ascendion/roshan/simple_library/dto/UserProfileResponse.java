package com.ascendion.roshan.simple_library.dto;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * The signed-in user's profile. The same shape for every user, whatever their roles:
 * the identity (name and email) comes from the token, {@code mobile} from the member's Borrower record (the token
 * does not carry it; {@code null} for a caller without a Borrower), and {@code borrower} holds what belongs only to
 * library members (Borrower id and how many books they hold). {@code borrower} is {@code null} for a caller who is
 * not a member, for example a desk-only admin. {@code roles} lists the library roles ({@code user}, {@code admin}) so
 * the web app can show or hide admin features; the backend still enforces every role rule. Only returned to the
 * owner, so the mobile number is included.
 */
public record UserProfileResponse(
        String userId,
        String firstname,
        String lastname,
        String email,
        List<String> roles,
        String mobile,
        BorrowerProfile borrower) {

    public static UserProfileResponse of(final Jwt jwt, final List<String> roles, final String mobile,
                                         final BorrowerProfile borrower) {
        return new UserProfileResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                jwt.getClaimAsString("email"),
                roles,
                mobile,
                borrower);
    }
}
