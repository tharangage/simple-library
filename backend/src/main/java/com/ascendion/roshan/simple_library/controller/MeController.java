package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.dto.BorrowerResponse;
import com.ascendion.roshan.simple_library.service.BorrowerProvisioningService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The caller's own Borrower; created on the first call if the user has none yet (US-02.4). */
@RestController
@RequestMapping("/apis/v1/me")
public class MeController {

    private final BorrowerProvisioningService provisioningService;

    public MeController(final BorrowerProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    @GetMapping
    public BorrowerResponse me(@AuthenticationPrincipal final Jwt jwt) {
        return BorrowerResponse.from(provisioningService.getOrCreate(jwt));
    }
}
