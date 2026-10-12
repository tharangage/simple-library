package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.dto.BorrowerCreateRequest;
import com.ascendion.roshan.simple_library.dto.BorrowerResponse;
import com.ascendion.roshan.simple_library.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/apis/v1/borrowers")
public class BorrowerController {

    // The "current user" endpoint is planned (E01); the Location header points to it already.
    private static final URI ME = URI.create("/apis/v1/me");

    private final RegistrationService registrationService;

    public BorrowerController(final RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /** Public endpoint: a visitor creates their own account (open in {@code SecurityConfig}, no token yet). */
    @PostMapping
    public ResponseEntity<BorrowerResponse> register(@Valid @RequestBody final BorrowerCreateRequest request) {
        return ResponseEntity.created(ME)
                .body(BorrowerResponse.from(registrationService.register(request)));
    }
}
