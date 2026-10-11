package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.dto.BorrowerCreateRequest;
import com.ascendion.roshan.simple_library.dto.BorrowerResponse;
import com.ascendion.roshan.simple_library.service.BorrowerProvisioningService;
import com.ascendion.roshan.simple_library.service.BorrowerService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apis/v1/borrowers")
public class BorrowerController {

    private final BorrowerService borrowerService;
    private final BorrowerProvisioningService provisioningService;

    public BorrowerController(final BorrowerService borrowerService,
                              final BorrowerProvisioningService provisioningService) {
        this.borrowerService = borrowerService;
        this.provisioningService = provisioningService;
    }

    /**
     * The caller's own Borrower, created on the first call (US-02.4). Role {@code user} only
     * ({@code SecurityConfig}); the web app calls it after login to learn its borrower id.
     */
    @GetMapping("/me")
    public BorrowerResponse me(@AuthenticationPrincipal final Jwt jwt) {
        return BorrowerResponse.from(provisioningService.getOrCreate(jwt));
    }

    @PostMapping
    public BorrowerResponse registerBorrower(@Valid @RequestBody final BorrowerCreateRequest borrowerCreateRequest) {
        return BorrowerResponse.from(borrowerService.registerBorrower(borrowerCreateRequest));
    }

}
