package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.dto.UserProfileResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The caller's own profile, read from the token. The same for every user. A caller with the role
 * {@code user} also gets their Borrower created on this first call, by {@code BorrowerProvisioningInterceptor} (US-02.4).
 */
@RestController
@RequestMapping("/apis/v1/me")
public class MeController {

    @GetMapping
    public UserProfileResponse me(@AuthenticationPrincipal final Jwt jwt) {
        return UserProfileResponse.from(jwt);
    }
}
