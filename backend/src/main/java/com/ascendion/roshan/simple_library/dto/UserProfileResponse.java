package com.ascendion.roshan.simple_library.dto;

import org.springframework.security.oauth2.jwt.Jwt;

/** The signed-in user's profile as the token states it. Same shape for every user, whatever their roles. */
public record UserProfileResponse(
        String userId,
        String firstname,
        String lastname,
        String email) {

    public static UserProfileResponse from(final Jwt jwt) {
        return new UserProfileResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                jwt.getClaimAsString("email"));
    }
}
